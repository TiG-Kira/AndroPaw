import java.util.Properties
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// подпись релиза: keystore.properties в корне (в git не попадает, см. .gitignore);
// без него релиз собирается неподписанным
val signProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val canSign = signProps.getProperty("storeFile")?.let { rootProject.file(it).isFile } == true

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
    namespace = "app.aemu"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        // 28: гостевые бинарники запускаются из каталога данных приложения (W^X для targetSdk>=29)
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 28
        versionCode = 3
        versionName = "0.0.0.3"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {
            dimension = "distribution"
            applicationId = "app.aemu"
        }
        create("clone") {
            dimension = "distribution"
            applicationId = "app.aemu.clone"
        }
        // a copy of this release that installs next to app.aemu (a package name part cannot start with a digit)
        create("v0003") {
            dimension = "distribution"
            applicationId = "app.aemu.v0003"
        }
    }

    signingConfigs {
        if (canSign) create("release") {
            storeFile = rootProject.file(signProps.getProperty("storeFile"))
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
            optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
            optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    lint { abortOnError = false; checkReleaseBuilds = false }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.11.00")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3:1.5.0-alpha16")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.tukaani:xz:1.10")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.brotli:dec:0.1.2")
    implementation("com.github.junrar:junrar:7.5.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

// sh: ./gradlew :app:copyReleaseApks -> release-apks/<version>/app.aemu.apk and app.aemu.0003.apk
tasks.register<Copy>("copyReleaseApks") {
    dependsOn("assembleStandardRelease", "assembleV0003Release")
    val v = android.defaultConfig.versionName
    from(layout.buildDirectory.dir("outputs/apk/standard/release")) {
        include("*.apk")
        rename { "app.aemu.apk" }
    }
    from(layout.buildDirectory.dir("outputs/apk/v0003/release")) {
        include("*.apk")
        rename { "app.aemu.0003.apk" }
    }
    into(rootProject.layout.projectDirectory.dir("release-apks/$v"))
}
