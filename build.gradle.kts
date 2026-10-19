plugins {
    // Miuix 0.9.4's -android artifacts declare compileSdk 37 in their AAR metadata, which AGP 8.13
    // rejects ("requires ... compile against version 37 or later"). 9.1.0 is what Termux-Ultra uses.
    id("com.android.application") version "9.1.0" apply false
    // AGP 9 起 Kotlin 由 AGP 内置提供：再 apply `org.jetbrains.kotlin.android` 会直接
    // 报 "no longer required for Kotlin support since AGP 9.0" 并中止构建。Compose
    // 编译器插件仍需显式声明（Termux-Ultra 同样是只声明 plugin.compose）。
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}
