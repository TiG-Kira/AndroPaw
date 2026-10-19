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

// NOTE: Gradle compiles the `pluginManagement` block as a *separate* script, so top-level helpers
// (gprUser/gprToken below) are NOT visible inside it — calling them there fails with
// "Unresolved reference". Plugins only ever come from google()/gradlePluginPortal(), so the
// GitHub Packages repo is declared in dependencyResolutionManagement only.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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
