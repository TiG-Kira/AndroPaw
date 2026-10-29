package app.andropaw.openharmony

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.R
import app.andropaw.ui.AndroPawTheme
import app.andropaw.ui.ProvideNavigationEventOwner
import app.andropaw.ui.rememberGlassPageBackdrop
import app.andropaw.ui.standaloneContentPadding
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
 * OpenHarmony spaces: the "download → install → run" one-stop page.
 *
 * Storage split (deliberate): downloaded packages live in the download cache (OhCache) and the
 * archive is discarded right after extraction; the installed runtime and the spaces themselves
 * live in filesDir/openharmony — persistent, safe from cache clears. Spaces are also surfaced
 * on the emulator page ([OhHomeCard]) so both kinds of machines are managed side by side.
 */
class OhActivity : ComponentActivity() {
    override fun attachBaseContext(base: android.content.Context) =
        super.attachBaseContext(app.andropaw.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ProvideNavigationEventOwner {
                AndroPawTheme { OhScreen(onBack = { finish() }) }
            }
        }
    }
}

private class OhUiState {
    var runtimes by mutableStateOf<List<OhRuntimeRepository.OhRuntime>>(emptyList())
    var spaces by mutableStateOf<List<OhSpace>>(emptyList())
    var cacheBytes by mutableStateOf(0L)
    var freeBytes by mutableStateOf(0L)
    var dlLabel by mutableStateOf<String?>(null)
    var dlProgress by mutableStateOf(-1f)    // <0 = idle; 0..1 = downloading
    var installing by mutableStateOf(false)  // archive fetched: sha check + extraction running
    var error by mutableStateOf<String?>(null)
}

@Composable
private fun OhScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st = remember { OhUiState() }
    val glassPage = rememberGlassPageBackdrop()
    val scrollBehavior = MiuixScrollBehavior()
    val density = LocalDensity.current
    val systemNavBarsHeight = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        st.runtimes = OhRuntimeRepository.installed(ctx)
        st.spaces = OhSpaceStore.list(ctx)
        st.cacheBytes = OhCache.usedBytes(ctx)
        st.freeBytes = OhCache.deviceFreeBytes(ctx)
    }

    fun downloadAndInstall() {
        scope.launch {
            st.error = null
            try {
                val pkg = withContext(Dispatchers.IO) { OhCatalog.latest() }
                    ?: throw IllegalStateException("no runtime package published yet")
                withContext(Dispatchers.Main) {
                    st.dlProgress = 0f; st.installing = false; st.dlLabel = pkg.fileName
                }
                OhRuntimeRepository.ensureInstalled(ctx, pkg) { s ->
                    when (s) {
                        is OhDownloader.State.Downloading -> {
                            st.installing = false
                            st.dlProgress = s.progress
                            st.dlLabel = "${OhCache.formatBytes(s.downloaded)} / ${OhCache.formatBytes(s.total)}"
                        }
                        // archive complete: sha-256 + rename + extraction of a ~530 MB tar.xz
                        // follow — minutes of work the user has to see feedback for
                        is OhDownloader.State.Verifying -> {
                            st.dlProgress = 1f; st.installing = true; st.dlLabel = null
                        }
                        else -> {}
                    }
                }
                st.dlProgress = -1f; st.installing = false
                refresh()
            } catch (t: Throwable) {
                st.dlProgress = -1f; st.installing = false
                st.error = t.message ?: t.toString()
                refresh()
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            GlassTopAppBar(
                title = stringResource(R.string.oh_title),
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().then(glassPage.contentModifier)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = standaloneContentPadding(padding, bottom = systemNavBarsHeight + 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.oh_subtitle), fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }

            // ---- runtime: the download → install one-stop ---------------------------
            item {
                Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                    val installed = st.runtimes.firstOrNull()
                    Text(stringResource(R.string.oh_runtime), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    when {
                        st.dlProgress >= 0f -> {
                            Text(
                                stringResource(
                                    if (st.installing) R.string.oh_installing
                                    else R.string.oh_downloading, st.dlLabel ?: ""),
                                fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            if (st.installing) {
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
                                    Box(Modifier.fillMaxWidth(st.dlProgress.coerceIn(0f, 1f)).fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp)).background(MiuixTheme.colorScheme.primary))
                                }
                            }
                        }
                        st.error != null -> {
                            Text(stringResource(R.string.oh_failed, st.error!!), fontSize = 13.sp, color = Color(0xFFC62828))
                            Spacer(Modifier.height(8.dp))
                            TextButton(text = stringResource(R.string.oh_retry), onClick = { downloadAndInstall() })
                        }
                        installed != null -> {
                            Text("${installed.ohVersion} · ${installed.version} · ${OhCache.formatBytes(installed.sizeBytes)}",
                                fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            Spacer(Modifier.height(8.dp))
                            TextButton(text = stringResource(R.string.oh_check_updates), onClick = { downloadAndInstall() })
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
            }

            // ---- cache: usage + every cleanup path ----------------------------------
            item {
                Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                    Text(stringResource(R.string.oh_cache), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.oh_cache_usage,
                        OhCache.formatBytes(st.cacheBytes), OhCache.formatBytes(st.freeBytes)),
                        fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    Spacer(Modifier.height(8.dp))
                    OhActionsRow {
                        TextButton(text = stringResource(R.string.oh_clean_partial), onClick = {
                            OhCache.clearPartialDownloads(ctx); scope.launch { refresh() }
                        })
                        TextButton(text = stringResource(R.string.oh_clean_archives), onClick = {
                            OhCache.clearDownloads(ctx); scope.launch { refresh() }
                        })
                        TextButton(text = stringResource(R.string.oh_clean_runtimes), onClick = {
                            OhRuntimeRepository.uninstallAll(ctx); scope.launch { refresh() }
                        })
                    }
                }
            }

            // ---- spaces --------------------------------------------------------------
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.oh_spaces), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    if (st.runtimes.isNotEmpty()) {
                        TextButton(text = stringResource(R.string.oh_new_space), onClick = {
                            val rt = st.runtimes.first()
                            scope.launch(Dispatchers.IO) {
                                val space = OhSpaceStore.create(
                                    ctx, ctx.getString(R.string.oh_space_default, st.spaces.size + 1), rt)
                                refresh()
                                withContext(Dispatchers.Main) {
                                    ctx.startActivity(Intent(ctx, OhDisplayActivity::class.java).putExtra("space", space.id))
                                }
                            }
                        })
                    }
                }
            }
            if (st.spaces.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                        Text(stringResource(R.string.oh_spaces_empty), fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
            items(st.spaces, key = { it.id }) { space ->
                Card(modifier = Modifier.fillMaxWidth().clickable {
                    ctx.startActivity(Intent(ctx, OhDisplayActivity::class.java).putExtra("space", space.id))
                }, insideMargin = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0A59F7), Color(0xFF7B2FF7)))),
                            contentAlignment = Alignment.Center) {
                            Text("OH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(space.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("${space.displayVersion} · ${space.settings.displaySize} · ${space.settings.fps} Hz" +
                                    if (space.bootCount > 0) " · ${space.bootCount}×" else "",
                                fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1)
                        }
                        Box(Modifier.size(36.dp).clip(CircleShape)
                            .background(MiuixTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, stringResource(R.string.start),
                                tint = MiuixTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(36.dp).clip(CircleShape)
                            .background(MiuixTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = MiuixTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.clickable {
                                    OhSpaceStore.delete(ctx, space.id)
                                    scope.launch { refresh() }
                                })
                        }
                    }
                }
            }
        }
    }
}

/**
 * A wrapping action row: buttons flow onto the next line when the width runs out,
 * instead of being squeezed into broken vertical text.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OhActionsRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

/**
 * The OpenHarmony section of the emulator page: OpenHarmony machines are managed on the same
 * grid as the Android ones — spaces start from here, and the runtime install / new-space flow
 * lives one tap away in [OhHomeCard].
 */
@Composable
fun OhHomeCard(
    spaces: List<OhSpace>,
    runtimeInstalled: Boolean,
    runningSpaceId: String?,
    onStartSpace: (String) -> Unit,
    onManage: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0A59F7), Color(0xFF7B2FF7)))),
                contentAlignment = Alignment.Center) {
                Text("OH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.oh_title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.oh_subtitle), fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            TextButton(text = stringResource(R.string.oh_manage), onClick = onManage)
        }
        Spacer(Modifier.height(10.dp))
        when {
            !runtimeInstalled -> {
                Text(stringResource(R.string.oh_runtime_missing), fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onManage) { Text(stringResource(R.string.oh_download_install)) }
            }
            spaces.isEmpty() -> {
                Text(stringResource(R.string.oh_spaces_empty), fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Spacer(Modifier.height(8.dp))
                TextButton(text = stringResource(R.string.oh_new_space), onClick = onManage)
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (space in spaces) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onStartSpace(space.id) }) {
                        Column(Modifier.weight(1f)) {
                            Text(space.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(space.displayVersion + if (space.id == runningSpaceId)
                                " · " + stringResource(R.string.vm_running_badge) else "",
                                fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                        Box(Modifier.size(34.dp).clip(CircleShape)
                            .background(MiuixTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, stringResource(R.string.start),
                                tint = MiuixTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    }
}
