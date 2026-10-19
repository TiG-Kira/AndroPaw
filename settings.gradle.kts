// --- GitHub Packages credentials ---------------------------------------------------------------
// Declared FIRST: a .kts script is compiled top-to-bottom, so pluginManagement (below) can only
// call helpers that are already defined.
//
// Credentials come from gradle.properties (project or ~/.gradle) or the environment — never
// committed. Put this in ~/.gradle/gradle.properties:
//   gpr.user=<github username>
//   gpr.token=<PAT with read:packages>
// …or export GPR_USER / GPR_TOKEN (what CI does). Without them the miuix-*-android artifacts
// return 401 and the build cannot resolve its UI dependencies.
fun props(name: String): String? {
    val files = listOf(
        settingsDir.resolve("gradle.properties"),
        java.io.File(System.getProperty("user.home"), ".gradle/gradle.properties"),
    )
    for (f in files) {
        if (!f.isFile) continue
        val p = java.util.Properties()
        runCatching { f.inputStream().use { p.load(it) } }
        p.getProperty(name)?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return System.getenv(name.uppercase().replace('.', '_'))?.takeIf { it.isNotBlank() }
}
fun gprUser(): String = props("gpr.user") ?: ""
fun gprToken(): String = props("gpr.token") ?: ""

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // MiuiX 0.9.4 ships the Android-only artifacts (miuix-*-android) here, not on Maven Central.
        // GitHub Packages requires a token with the `read:packages` scope even for public packages.
        maven { url = uri("https://maven.pkg.github.com/compose-miuix-ui/miuix"); credentials { username = gprUser(); password = gprToken() } }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.github.com/compose-miuix-ui/miuix"); credentials { username = gprUser(); password = gprToken() } }
    }
}
rootProject.name = "AndroPaw"
include(":app")
