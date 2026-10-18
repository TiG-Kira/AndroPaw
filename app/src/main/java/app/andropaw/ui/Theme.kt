package app.andropaw.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

// HyperOS / MiuiX theme. Mirrors Termux-Ultra's KiTerminalTheme:
// ThemeController's colorSchemeMode is a read-only constructor argument, so switching the mode
// must rebuild the controller (remember(key)). Dynamic colour (Android 12+) uses MonetSystem;
// otherwise we honour the stored Light/Dark/System preference.
//
// NOTE: ColorSchemeMode is assumed to expose Light / Dark / System / MonetSystem (the four values
// KiTerminalTheme and the MiuiX source use). If a future MiuiX build renames them, adjust here.
@Composable
fun AndroPawTheme(forceDark: Boolean? = null, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    // The home screen stays alive while the settings screen flips the theme: follow the stored value.
    val rev = remember { mutableIntStateOf(0) }
    DisposableEffect(ctx) {
        val sp = app.andropaw.AppPrefs.prefs(ctx)
        val l = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> rev.intValue++ }
        sp.registerOnSharedPreferenceChangeListener(l)
        onDispose { sp.unregisterOnSharedPreferenceChangeListener(l) }
    }
    rev.intValue

    val dark = forceDark ?: when (app.andropaw.AppPrefs.theme(ctx)) {
        app.andropaw.AppPrefs.THEME_LIGHT -> false
        app.andropaw.AppPrefs.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    val colorSchemeMode = when {
        app.andropaw.AppPrefs.dynamicColor(ctx) && Build.VERSION.SDK_INT >= 31 -> ColorSchemeMode.MonetSystem
        dark -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.Light
    }

    MiuixTheme(
        controller = remember(colorSchemeMode) { ThemeController(colorSchemeMode = colorSchemeMode) },
        content = content,
    )
}

// Monospace style used by the guest screen / log views.
val Mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
