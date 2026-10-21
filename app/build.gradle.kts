import java.util.Properties
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// подпись релиза: keystore.properties в корне (в git не попадает, см. .gitignore);
// без него релиз собирается неподписанным
val signProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val canSign = signProps.getProperty("storeFile")?.let { rootProject.file(it).isFile } == true

// storeType：keystore.properties 可显式指定；没指定就按魔数嗅探。
// 常见坑：从 IDE / macOS 钥匙串导出的私钥常常是「扩展名 .jks、内容却是 PKCS12」，
// 而 Gradle 的 signingConfig 默认按 JKS 解析，会在打包期抛
// "Failed to read key ... from store: Invalid keystore format"。
fun detectStoreType(file: File): String = runCatching {
    val head = ByteArray(4)
    val read = file.inputStream().use { it.read(head) }
    if (read >= 4 && head[0] == 0xFE.toByte() && head[1] == 0xED.toByte()) "jks" else "pkcs12"
}.getOrDefault("jks")
val signStoreType = signProps.getProperty("storeType")?.takeIf { it.isNotBlank() }
    ?: signProps.getProperty("storeFile")?.let { detectStoreType(rootProject.file(it)) }
    ?: "jks"

// Engine guest files: every kernel carries its own copy of most of them (473 MB, 110 MB of it different, all stored
// uncompressed because of their names). The APK gets each content once, compressed, as assets/blobs/<sha1>, plus
// assets/engines.idx ("path<TAB>sha1<TAB>size"); the app reads them through core/EngineAssets.kt
val engineSrc = file("src/main/assets/engines")
val engineOut = layout.buildDirectory.dir("generated/engineAssets")
val packEngines = tasks.register("packEngines") {
    inputs.dir(engineSrc)
    outputs.dir(engineOut)
    doLast {
        val o = engineOut.get().asFile
        o.deleteRecursively()
        File(o, "blobs").mkdirs()
        val idx = StringBuilder()
        engineSrc.walkTopDown().filter { it.isFile }.sortedBy { it.invariantSeparatorsPath }.forEach { f ->
            val sha = MessageDigest.getInstance("SHA-1").digest(f.readBytes()).joinToString("") { "%02x".format(it) }
            val b = File(o, "blobs/$sha")
            if (!b.exists()) f.copyTo(b)
            val rel = f.relativeTo(engineSrc).path.replace('\\', '/')
            idx.append("engines/$rel\t$sha\t${f.length()}\n")
        }
        File(o, "engines.idx").writeText(idx.toString())
    }
}
tasks.named("preBuild") { dependsOn(packEngines) }

android {
    namespace = "app.andropaw"
    // Miuix 0.9.4 (miuix-ui-android / miuix-preference-android) is built against android-37.
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        // 28: гостевые бинарники запускаются из каталога данных приложения (W^X для targetSdk>=29)
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 28
        versionCode = 38
        // 0.1.0 起恢复 semver 标准三段格式（此前是 0.0.0.x 四段）
        versionName = "0.2.29"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {
            dimension = "distribution"
            applicationId = "app.andropaw"
        }
        create("clone") {
            dimension = "distribution"
            applicationId = "app.andropaw.clone"
        }
        // a copy of this release that installs next to app.andropaw (a package name part cannot start with a digit)
        create("v0003") {
            dimension = "distribution"
            applicationId = "app.andropaw.v0003"
        }
    }

    signingConfigs {
        if (canSign) create("release") {
            storeFile = rootProject.file(signProps.getProperty("storeFile"))
            storeType = signStoreType
            storePassword = signProps.getProperty("storePassword")
            keyAlias = signProps.getProperty("keyAlias")
            keyPassword = signProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (canSign) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ""
            // тот же ключ, что у релиза: отладочная сборка ставится поверх без потери образов
            if (canSign) signingConfig = signingConfigs.getByName("release")
        }
    }

    sourceSets["main"].assets.setSrcDirs(listOf(engineOut))

    packaging {
        // исполняемые файлы движка лежат как lib*.so и должны распаковываться на диск
        jniLibs { useLegacyPackaging = true }
    }

    androidResources {
        noCompress += listOf("payload", "tar", "gz", "so", "img")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    lint { abortOnError = false; checkReleaseBuilds = false }
}

dependencies {
    testImplementation("org.json:json:20240303")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    // Compose — pinned to the versions Miuix 0.9.4 targets (Compose 1.8.x / Material3 1.3.x).
    // The previous compose-bom 2025.11.00 pulled Material3 1.5.0-alpha16; its expressive APIs are
    // binary-incompatible with Miuix and rendered a blank screen, so we pin explicitly.
    implementation("androidx.compose.ui:ui:1.8.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.3")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // MiuiX / HyperOS UI (Termux-Ultra look).
    // NOTE: the artifacts are the Android-only variants (-android) published to GitHub Packages
    // (https://maven.pkg.github.com/compose-miuix-ui/miuix), NOT the KMP artifacts on Maven Central.
    // GitHub Packages requires a token with the `read:packages` scope.
    // The glass layer (top.yukonga.miuix.kmp.glass) is vendored under app/src/main/java so it can be tuned.
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")        // basic + theme + layout + anim + overlay
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.4")      // Backdrop / LayerBackdrop for glass
    implementation("top.yukonga.miuix.kmp:miuix-shader-android:0.9.4")  // shader primitives the glass layer renders with
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")     // Miuix icon set
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4") // preference screens used by settings
    // miuix-glass popup controls need NavigationBackHandler / rememberNavigationEventState (1.1.2+).
    implementation("androidx.navigationevent:navigationevent-compose-android:1.1.2")
    // Liquid-glass primitives (Android-only) used by the vendored glass module.
    implementation("io.github.kyant0:backdrop-android:2.0.1") {
        exclude(group = "org.jetbrains.compose")
        exclude(group = "org.jetbrains.compose.ui")
        exclude(group = "org.jetbrains.compose.foundation")
    }
    implementation("io.github.kyant0:shapes-android:1.2.1") {
        exclude(group = "org.jetbrains.compose")
        exclude(group = "org.jetbrains.compose.ui")
        exclude(group = "org.jetbrains.compose.foundation")
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.coil-kt:coil-compose:2.7.0") // developer/contributor avatars on the About page
    implementation("org.tukaani:xz:1.10")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.brotli:dec:0.1.2")
    implementation("com.github.junrar:junrar:7.5.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

// sh: ./gradlew :app:copyReleaseApks -> release-apks/<version>/app.andropaw.apk and app.andropaw.0003.apk
tasks.register<Copy>("copyReleaseApks") {
    dependsOn("assembleStandardRelease", "assembleV0003Release")
    val v = android.defaultConfig.versionName
    from(layout.buildDirectory.dir("outputs/apk/standard/release")) {
        include("*.apk")
        rename { "app.andropaw.apk" }
    }
    from(layout.buildDirectory.dir("outputs/apk/v0003/release")) {
        include("*.apk")
        rename { "app.andropaw.0003.apk" }
    }
    into(rootProject.layout.projectDirectory.dir("release-apks/$v"))
}
