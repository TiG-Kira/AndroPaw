package app.andropaw.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * The standalone About page. Lives as its own activity (like Termux-Ultra's about screen is its
 * own page), so the main shell's glass backdrop stays untouched and system back just finishes.
 */
class AboutActivity : ComponentActivity() {
    /** Language this activity was created with: a change means its resources need recreation. */
    private var style = ""
    private fun styleOf(c: android.content.Context) = app.andropaw.AppPrefs.language(c)

    override fun attachBaseContext(base: android.content.Context) {
        style = styleOf(base)
        super.attachBaseContext(app.andropaw.AppPrefs.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AndroPawTheme { AboutScreen(onBack = { finish() }) } }
    }

    override fun onResume() {
        super.onResume()
        if (styleOf(applicationContext) != style) recreate()
    }
}
