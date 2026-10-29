package app.andropaw.openharmony

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.andropaw.R
import app.andropaw.ui.AndroPawTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The OpenHarmony screen: a fullscreen surface handed to the runner through OhEngine.
 * Leaving the screen stops the space — one space runs at a time, like the reference design.
 */
class OhDisplayActivity : ComponentActivity() {
    override fun attachBaseContext(base: android.content.Context) =
        super.attachBaseContext(app.andropaw.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val spaceId = intent.getStringExtra("space")
        if (spaceId == null) { finish(); return }
        setContent { AndroPawTheme { OhDisplay(spaceId, onBack = { finish() }) } }
    }

    override fun onDestroy() {
        if (isFinishing) OhEngine.stop()
        super.onDestroy()
    }
}

@Composable
private fun OhDisplay(spaceId: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf(OhEngine.state) }
    var failure by remember { mutableStateOf(OhEngine.failure) }

    DisposableEffect(Unit) {
        val stopState = OhEngine.onState { state = it }
        val stopLog = OhEngine.onLog { }
        onDispose { stopState(); stopLog() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                SurfaceView(c).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(h: SurfaceHolder) {
                            val space = OhSpaceStore.get(c, spaceId) ?: return
                            val rt = OhRuntimeRepository.get(c, space.runtimeVersion) ?: return
                            OhEngine.start(c, space, OhRuntimeRepository.dir(c, rt.version), h.surface)
                        }
                        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) {
                            OhEngine.surface(h.surface)
                        }
                        override fun surfaceDestroyed(h: SurfaceHolder) {
                            OhEngine.surface(null)
                        }
                    })
                }
            },
        )
        when (state) {
            OhEngine.State.PREPARING, OhEngine.State.BOOTING -> Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(if (state == OhEngine.State.PREPARING) R.string.oh_boot_preparing else R.string.oh_boot_booting),
                    fontSize = 15.sp, color = Color.White)
            }
            OhEngine.State.FAILED -> Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.oh_boot_failed), fontSize = 16.sp, color = Color(0xFFEF5350))
                Text(failure ?: "", fontSize = 13.sp, color = Color(0xFFB0B0B0))
            }
            else -> {}
        }
    }
}
