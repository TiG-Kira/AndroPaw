plugins {
    // Miuix 0.9.4's -android artifacts declare compileSdk 37 in their AAR metadata, which AGP 8.13
    // rejects ("requires ... compile against version 37 or later"). 9.1.0 is what Termux-Ultra uses.
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}
