package app.andropaw.openharmony

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import top.yukonga.miuix.kmp.basic.BasicComponent
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
        setContent { ProvideNavigationEventOwner { AndroPawTheme { OhScreen(onBack = { finish() }) } } }
    }

    override fun onResume() {
        super.onResume()
        if (styleOf(applicationContext) != style) recreate()
    }
}

private class OhUiState {
    var runtimes by mutableStateOf<List<OhRuntimeRepository.OhRuntime>>(emptyList())
    var spaces by mutableStateOf<List<OhSpace>>(emptyList())
    var cacheBytes by mutableStateOf(0L)
    var freeBytes by mutableStateOf(0L)
    var dlLabel by mutableStateOf<String?>(null)
    var dlProgress by mutableStateOf(-1f)   // <0 = not downloading
    var error by mutableStateOf<String?>(null)
}

@Composable
private fun OhScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st = remember { OhUiState() }
    // this page lives outside the main tab host, so it captures its own glass backdrop
    val glassPage = rememberGlassPageBackdrop()
    val scrollBehavior = MiuixScrollBehavior()
    val density = LocalDensity.current
    val systemNavBarsHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

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
                withContext(Dispatchers.Main) { st.dlProgress = 0f; st.dlLabel = pkg.fileName }
                OhRuntimeRepository.ensureInstalled(ctx, pkg) { s ->
                    when (s) {
                        is OhDownloader.State.Downloading -> {
                            st.dlProgress = s.progress
                            st.dlLabel = "${OhCache.formatBytes(s.downloaded)} / ${OhCache.formatBytes(s.total)}"
                        }
                        is OhDownloader.State.Verifying -> { st.dlProgress = 1f; st.dlLabel = null }
                        else -> {}
                    }
                }
                st.dlProgress = -1f
                refresh()
            } catch (t: Throwable) {
                st.dlProgress = -1f
                st.error = t.message ?: t.toString()
                refresh()
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Box(modifier = Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                modifier = Modifier
                    .then(glassPage.contentModifier)
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = standaloneContentPadding(padding, bottom = systemNavBarsHeight + 26.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ---- runtime: the download → install one-stop ---------------------------
                item {
                    OhSection(stringResource(R.string.oh_runtime)) {
                        val installed = st.runtimes.firstOrNull()
                        when {
                            st.dlProgress >= 0f -> {
                                Text(stringResource(R.string.oh_downloading, st.dlLabel ?: ""), fontSize = 13.sp)
                                Spacer(Modifier.height(8.dp))
                                // plain linear bar: no miuix determinate-linear component to lean on
                                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                    .background(MiuixTheme.colorScheme.secondaryContainer)) {
                                    Box(Modifier.fillMaxWidth(st.dlProgress.coerceIn(0f, 1f)).height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)).background(MiuixTheme.colorScheme.primary))
                                }
                            }
                            st.error != null -> {
                                Text(stringResource(R.string.oh_failed, st.error!!), fontSize = 13.sp,
                                    color = MiuixTheme.colorScheme.error)
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
                    OhSection(stringResource(R.string.oh_cache)) {
                        Text(stringResource(R.string.oh_cache_usage,
                            OhCache.formatBytes(st.cacheBytes), OhCache.formatBytes(st.freeBytes)),
                            fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        Spacer(Modifier.height(8.dp))
                        // buttons wrap instead of crowding one line; rows keep their own gap
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
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
                    OhSection(
                        stringResource(R.string.oh_spaces),
                        subtitle = stringResource(R.string.oh_spaces_sub).takeIf { st.spaces.isNotEmpty() },
                    ) {
                        Column {
                            if (st.spaces.isEmpty()) {
                                Text(stringResource(R.string.oh_spaces_empty), fontSize = 13.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                            st.spaces.forEach { space ->
                                BasicComponent(
                                    title = space.name,
                                    summary = "${space.displayVersion} · ${space.settings.displaySize} · " +
                                        "${space.settings.fps} Hz" + if (space.bootCount > 0) " · ${space.bootCount}×" else "",
                                    startAction = { OhBadge(size = 36.dp, radius = 12.dp, text = 14.sp) },
                                    endActions = {
                                        Box(Modifier.size(34.dp).clip(CircleShape).clickable {
                                            OhSpaceStore.delete(ctx, space.id)
                                            scope.launch { refresh() }
                                        }, contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Delete, stringResource(R.string.delete),
                                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                        }
                                    },
                                    onClick = {
                                        ctx.startActivity(
                                            Intent(ctx, OhDisplayActivity::class.java).putExtra("space", space.id))
                                    },
                                )
                            }
                            if (st.runtimes.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    TextButton(text = stringResource(R.string.oh_new_space), onClick = {
                                        val rt = st.runtimes.first()
                                        scope.launch(Dispatchers.IO) {
                                            val space = OhSpaceStore.create(
                                                ctx, ctx.getString(R.string.oh_space_default, st.spaces.size + 1), rt)
                                            refresh()
                                            withContext(Dispatchers.Main) {
                                                ctx.startActivity(
                                                    Intent(ctx, OhDisplayActivity::class.java).putExtra("space", space.id))
                                            }
                                        }
                                    })
                                }
                            } else {
                                Text(stringResource(R.string.oh_runtime_missing), fontSize = 13.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A titled card section, matching the settings page's grouping. */
@Composable
private fun OhSection(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Column {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = if (subtitle == null) 8.dp else 0.dp))
        subtitle?.let {
            Text(it, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        }
        Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) { content() }
    }
}

/** The OpenHarmony mark, shared by the page rows and the emulator page card. */
@Composable
private fun OhBadge(size: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp,
                    text: androidx.compose.ui.unit.TextUnit) {
    Box(Modifier.size(size).clip(RoundedCornerShape(radius))
        .background(Brush.linearGradient(listOf(Color(0xFF0A59F7), Color(0xFF7B2FF7)))),
        contentAlignment = Alignment.Center) {
        Text("OH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = text)
    }
}

/**
 * The OpenHarmony section of the emulator page: OpenHarmony machines are managed on the same
 * grid as the Android ones — spaces start from here, and the runtime install / new-space flow
 * lives one tap away in [OhActivity].
 */
@Composable
fun OhHomeCard(
    spaces: List<OhSpace>,
    runtimeInstalled: Boolean,
    runningSpaceId: String?,
    onStartSpace: (String) -> Unit,
    onManage: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp), cornerRadius = 24.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OhBadge(size = 40.dp, radius = 13.dp, text = 14.sp)
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
                        OhBadge(size = 34.dp, radius = 11.dp, text = 12.sp)
                        Spacer(Modifier.width(10.dp))
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
