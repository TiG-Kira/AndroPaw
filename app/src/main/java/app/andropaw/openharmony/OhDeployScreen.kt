package app.andropaw.openharmony

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.R
import app.andropaw.ui.rememberGlassPageBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.glass.MiuixGlassIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * "Manage the OpenHarmony deployment" — the page behind the settings row.
 *
 * It used to be a page of its own with an entry of its own; that entry is gone (the emulator
 * page and the settings are where OpenHarmony is reached now) and what is left is the one thing
 * the other two pages cannot do: fetch the runtime package and unpack it.
 *
 * The spaces are deliberately not here any more: they are machines like any other, listed on
 * the emulator page and run from there.
 */
@Composable
fun OhDeployScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var runtimes by remember { mutableStateOf<List<OhRuntimeRepository.OhRuntime>>(emptyList()) }
    var cacheBytes by remember { mutableStateOf(0L) }
    var freeBytes by remember { mutableStateOf(0L) }
    var dlLabel by remember { mutableStateOf<String?>(null) }
    /** <0 = idle; 0..1 = downloading */
    var dlProgress by remember { mutableStateOf(-1f) }
    /** the archive is in: sha-256 and the extraction of a few hundred MB are running */
    var installing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        runtimes = OhRuntimeRepository.installed(ctx)
        cacheBytes = OhCache.usedBytes(ctx)
        freeBytes = OhCache.deviceFreeBytes(ctx)
    }

    fun downloadAndInstall() {
        scope.launch {
            error = null
            try {
                val pkg = withContext(Dispatchers.IO) { OhCatalog.latest() }
                    ?: throw IllegalStateException("no runtime package published yet")
                withContext(Dispatchers.Main) {
                    dlProgress = 0f; installing = false; dlLabel = pkg.fileName
                }
                OhRuntimeRepository.ensureInstalled(ctx, pkg) { s ->
                    when (s) {
                        // state writes are safe off the main thread (they are snapshot state)
                        is OhDownloader.State.Downloading -> {
                            installing = false
                            dlProgress = s.progress
                            dlLabel = "${OhCache.formatBytes(s.downloaded)} / ${OhCache.formatBytes(s.total)}"
                        }
                        is OhDownloader.State.Verifying -> { dlProgress = 1f; installing = true; dlLabel = null }
                        else -> {}
                    }
                }
                withContext(Dispatchers.Main) { dlProgress = -1f; installing = false }
                refresh()
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    dlProgress = -1f; installing = false
                    error = t.message ?: t.toString()
                }
                refresh()
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val glassPage = rememberGlassPageBackdrop()
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            GlassTopAppBar(
                title = stringResource(R.string.oh_deploy_title),
                backdrop = glassPage.backdrop,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixGlassIcons.ChevronBackward,
                            contentDescription = stringResource(R.string.back),
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.oh_deploy_sub), fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)

            // ---- runtime: the download → install one-stop ---------------------------
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                val installed = runtimes.firstOrNull()
                Text(stringResource(R.string.oh_runtime), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                when {
                    dlProgress >= 0f -> {
                        Text(
                            stringResource(
                                if (installing) R.string.oh_installing else R.string.oh_downloading,
                                dlLabel ?: ""),
                            fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        if (installing) {
                            // indeterminate sweep: the extraction has no per-byte progress,
                            // so keep the bar alive instead of parked at 100%
                            val tr = rememberInfiniteTransition(label = "install")
                            val sweep by tr.animateFloat(
                                -0.4f, 1f,
                                infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
                                label = "sweep")
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                .background(MiuixTheme.colorScheme.secondaryContainer)) {
                                Box(Modifier.fillMaxWidth(0.4f).fillMaxHeight()
                                    .graphicsLayer { translationX = sweep * size.width }
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MiuixTheme.colorScheme.primary))
                            }
                        } else {
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                .background(MiuixTheme.colorScheme.secondaryContainer)) {
                                Box(Modifier.fillMaxWidth(dlProgress.coerceIn(0f, 1f)).fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp)).background(MiuixTheme.colorScheme.primary))
                            }
                        }
                    }
                    error != null -> {
                        Text(stringResource(R.string.oh_failed, error!!), fontSize = 13.sp, color = Color(0xFFC62828))
                        Spacer(Modifier.height(8.dp))
                        TextButton(text = stringResource(R.string.oh_retry), onClick = { downloadAndInstall() })
                    }
                    installed != null -> {
                        Text("${installed.ohVersion} · ${installed.version} · ${OhCache.formatBytes(installed.sizeBytes)}",
                            fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            TextButton(text = stringResource(R.string.oh_check_updates), onClick = { downloadAndInstall() })
                            Spacer(Modifier.width(8.dp))
                            TextButton(text = stringResource(R.string.oh_clean_runtimes), onClick = {
                                OhRuntimeRepository.uninstallAll(ctx); scope.launch { refresh() }
                            })
                        }
                    }
                    else -> {
                        Text(stringResource(R.string.oh_runtime_missing), fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { downloadAndInstall() }) {
                            Text(stringResource(R.string.oh_download_install))
                        }
                    }
                }
            }

            // ---- cache --------------------------------------------------------------
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                Text(stringResource(R.string.oh_cache), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.oh_cache_usage,
                    OhCache.formatBytes(cacheBytes), OhCache.formatBytes(freeBytes)),
                    fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(text = stringResource(R.string.oh_clean_partial), onClick = {
                        OhCache.clearPartialDownloads(ctx); scope.launch { refresh() }
                    })
                    TextButton(text = stringResource(R.string.oh_clean_archives), onClick = {
                        OhCache.clearDownloads(ctx); scope.launch { refresh() }
                    })
                }
            }
        }
    }
}
