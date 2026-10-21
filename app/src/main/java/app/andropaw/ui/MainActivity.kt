package app.andropaw.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.BuildConfig
import app.andropaw.R
import app.andropaw.core.GuestImage
import app.andropaw.update.AppUpdateManager
import app.andropaw.update.ReleaseInfo
import app.andropaw.update.UpdateState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBarState
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

const val ACTION_BOOT = "app.andropaw.BOOT"

class MainActivity : ComponentActivity() {
    private val model: LibraryModel by viewModels()

    /** Language this activity was created with: a change means its string resources are stale and need context recreation. */
    private var style = ""
    private fun styleOf(c: Context) = app.andropaw.AppPrefs.language(c)

    override fun attachBaseContext(base: Context) {
        style = styleOf(base)
        super.attachBaseContext(app.andropaw.AppPrefs.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        askPermissions()
        handleView(intent)
        setContent { ProvideNavigationEventOwner { AndroPawTheme { Library(model, onRestyle = { recreate() }) } } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleView(intent)
        if (styleOf(applicationContext) != style) recreate()
    }

    override fun onResume() {
        super.onResume()
        if (styleOf(applicationContext) != style) { recreate(); return }
        model.refresh()
    }

    private fun handleView(i: Intent?) {
        if (i?.action == Intent.ACTION_VIEW) {
            val u = i.data
            if (u != null) {
                model.import(u)
            } else {
                val p = i.getStringExtra("path") ?: i.getStringExtra("file")
                if (p != null) model.import(android.net.Uri.fromFile(java.io.File(p)))
            }
        }
        val importPath = i?.getStringExtra("import")
        if (importPath != null) {
            model.import(android.net.Uri.fromFile(java.io.File(importPath)))
        }
        // shortcuts and automation: am start -a app.andropaw.BOOT --es id <image>
        if (i?.action == ACTION_BOOT) i.getStringExtra("id")?.let { VmActivity.start(this, it, i.getBooleanExtra("recovery", false)) }
    }

    private fun askPermissions() {
        val want = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 33) want += Manifest.permission.POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT <= 32) want += Manifest.permission.WRITE_EXTERNAL_STORAGE
        val need = want.filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), 1)
    }
}

/**
 * The home screen: a glass shell over two pages — the emulator library and the settings.
 *
 * Both pages scroll under one [GlassTopAppBar] and share a single glass backdrop, captured by
 * [rememberGlassPageBackdrop] and handed to the bar and the [GlassNavigationBar].
 *
 * The floating [GlassNavigationBar] pads itself with the system navigation-bar inset, floored at
 * 12 dp: gesture bars keep their own height, three-button nav keeps its (taller) inset, and
 * devices without any nav bar still get the fallback clearance so the capsule never sits flush.
 */
@Composable
fun Library(model: LibraryModel, onRestyle: () -> Unit) {
    val images by model.images.collectAsState()
    val imp by model.import.collectAsState()
    val ctx = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { model.import(it) } }
    var settingsFor by remember { mutableStateOf<GuestImage?>(null) }
    var deleteFor by remember { mutableStateOf<GuestImage?>(null) }
    var resetFor by remember { mutableStateOf<GuestImage?>(null) }
    var renameFor by remember { mutableStateOf<GuestImage?>(null) }
    var help by remember { mutableStateOf(false) }
    var addMenu by remember { mutableStateOf(false) }
    var library by remember { mutableStateOf(false) }
    var containerFrom by remember { mutableStateOf<GuestImage?>(null) }
    /** 0 = emulator page, 1 = AgentPaw page, 2 = settings page. */
    var tab by remember { mutableStateOf(0) }
    /** The AgentPaw chat page, drawn over the shell. */
    var agentChat by remember { mutableStateOf(false) }
    /**
     * A machine card's ⋮ menu: the image and where its icon sits, in root coordinates. The panel
     * itself is composed on the page box below — inside the card it was clipped by the card and
     * drawn under the neighbouring ones.
     */
    var cardMenu by remember { mutableStateOf<Pair<GuestImage, Rect>?>(null) }
    /** the page box's own place in root coordinates: the popup's anchor is handed over relative to it */
    var pageOrigin by remember { mutableStateOf(Offset.Zero) }
    /** where the shell overlay starts, for a machine card's menu anchor */
    var menuHostOrigin by remember { mutableStateOf(Offset.Zero) }
    val page = rememberGlassPageBackdrop()
    /** the AgentPaw execution channel: guest commands ride the QEMU path, no host-device control */
    val agentBridge = remember { app.andropaw.agent.AgentVmBridge(ctx.applicationContext, model) }
    LaunchedEffect(Unit) { app.andropaw.agent.ChatSession.attach(ctx.applicationContext, agentBridge) }

    // the machine the :vm process keeps running in the background (null: none), polled while the
    // home page is on screen — the card of that machine shows "running" and resumes it on tap
    var runningId by remember { mutableStateOf(app.andropaw.core.VmSession.runningId(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            runningId = app.andropaw.core.VmSession.runningId(ctx)
            kotlinx.coroutines.delay(1000)
        }
    }

    // Termux-Ultra floating glass nav-bar placement: the bar is inset 24 dp on both sides and
    // sits on top of the system navigation bars, floored at 12 dp (gesture bars keep their own
    // height, three-button nav its taller inset, and devices without any nav bar still get the
    // fallback clearance so the capsule never sits flush).
    val density = LocalDensity.current
    val systemNavBarsHeight = with(density) {
        maxOf(WindowInsets.navigationBars.getBottom(density), 12.dp.roundToPx()).toDp()
    }
    // The bar sinks off-screen while the IME is up or a popup asks for it, like Termux-Ultra's
    // default nav bar.
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val navBarVisible = !imeVisible && !MainNavBarVisibility.hidden

    // The glass top bar needs a ScrollBehavior to collapse and to keep its action-button pills
    // bare at rest: without one the bar reports "floating" all the time and the pills never
    // hide. The behaviour is remembered with stable pieces (Termux-Ultra's fix for the
    // collapse-snap being torn down on every recomposition).
    val topAppBarState = remember { TopAppBarState(0f, 0f, 0f) }
    val canScrollAlways = remember { { true } }
    val snapSpec = remember { androidx.compose.animation.core.spring<Float>(stiffness = 2500f) }
    val flingSpec = androidx.compose.animation.rememberSplineBasedDecay<Float>()
    val scrollBehavior = MiuixScrollBehavior(
        state = topAppBarState,
        canScroll = canScrollAlways,
        snapAnimationSpec = snapSpec,
        flingAnimationSpec = flingSpec,
    )

    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        if (app.andropaw.AppPrefs.autoCheckUpdates(ctx)) {
            val now = System.currentTimeMillis()
            if (now - app.andropaw.AppPrefs.lastUpdateCheck(ctx) > 3600_000L) {
                app.andropaw.AppPrefs.setLastUpdateCheck(ctx, now)
                val res = AppUpdateManager.checkLatestRelease(ctx)
                res.getOrNull()?.let { rel ->
                    if (AppUpdateManager.isNewerVersion(rel.versionName, BuildConfig.VERSION_NAME)) {
                        updateState = UpdateState.Available(rel)
                    }
                }
            }
        }
    }

    fun startDownload(rel: ReleaseInfo) {
        updateState = UpdateState.Downloading(rel, 0f, 0L, rel.sizeBytes)
        downloadJob = scope.launch {
            try {
                val file = AppUpdateManager.downloadApk(ctx, rel) { progress, downloaded, total ->
                    updateState = UpdateState.Downloading(rel, progress, downloaded, total)
                }
                updateState = UpdateState.ReadyToInstall(rel, file)
                AppUpdateManager.installApk(ctx, file)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    updateState = UpdateState.Error(e.localizedMessage ?: "Download error")
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            GlassTopAppBar(
                title = when (tab) {
                    0 -> stringResource(R.string.tab_emulator)
                    1 -> stringResource(R.string.tab_agent)
                    else -> stringResource(R.string.settings)
                },
                backdrop = page.backdrop,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (tab == 0) {
                        GlassIconButton(onClick = { help = true }) {
                            MiuixIcon(Icons.Rounded.Info, stringResource(R.string.help), tint = MiuixTheme.colorScheme.onSurface)
                        }
                    }
                },
            )
        },
        bottomBar = {
            // Floating glass bar, Termux-Ultra default: 24 dp side margins, bottom edge riding
            // the system navigation bars (floored at 12 dp). The glass shader pipeline needs
            // runtime shaders (API 33+); below that — and for the classic look — miuix's plain
            // NavigationBar takes over, like Termux-Ultra's fallback path.
            if (Build.VERSION.SDK_INT >= 33) {
                GlassNavigationBar(
                    items = listOf(
                        GlassNavigationItem(Icons.Rounded.Android, stringResource(R.string.tab_emulator)),
                        GlassNavigationItem(Icons.Rounded.AutoAwesome, stringResource(R.string.tab_agent)),
                        GlassNavigationItem(Icons.Rounded.Settings, stringResource(R.string.settings)),
                    ),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                    visible = navBarVisible,
                    backdrop = page.backdrop,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = systemNavBarsHeight),
                )
            } else {
                NavigationBar(
                    modifier = Modifier.padding(bottom = systemNavBarsHeight),
                ) {
                    NavigationBarItem(
                        icon = Icons.Rounded.Android,
                        label = stringResource(R.string.tab_emulator),
                        selected = tab == 0,
                        onClick = { tab = 0 },
                    )
                    NavigationBarItem(
                        icon = Icons.Rounded.AutoAwesome,
                        label = stringResource(R.string.tab_agent),
                        selected = tab == 1,
                        onClick = { tab = 1 },
                    )
                    NavigationBarItem(
                        icon = Icons.Rounded.Settings,
                        label = stringResource(R.string.settings),
                        selected = tab == 2,
                        onClick = { tab = 2 },
                    )
                }
            }
        },
        floatingActionButton = {
            when (tab) {
                0 -> HomeFab(expanded = addMenu, onToggle = { addMenu = it },
                    onImport = { pick.launch(arrayOf("*/*")) }, onLibrary = { library = true })
                // AgentPaw tab: no FAB — the conversation list's own "new chat" card is the
                // single creation path (the FAB used to create a second conversation on top)
            }
        },
    ) { pad ->
        // Pages run full height and pass under the glass bars — the Scaffold padding is spent as
        // scrollable content clearance, never as layout padding. That's what gives both bars
        // something to refract. The nestedScroll connection feeds the top bar's collapse.
        Box(
            Modifier
                .fillMaxSize()
                .then(page.contentModifier)
                // 页面底色放进 layerBackdrop 捕获层内部：底栏玻璃的材质是 SoftLight/HardLight
                // 混合，需要底下有实像素才能成像。没有 card 的页面捕获层是全透明的，
                // 混合层无物可混，底板层就渲染不出来。
                .background(MiuixTheme.colorScheme.surface)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .onGloballyPositioned { pageOrigin = it.boundsInRoot().topLeft },
        ) {
            if (tab == 0) {
                val full: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan =
                    { GridItemSpan(maxLineSpan) }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(148.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = pad.calculateTopPadding() + 8.dp,
                        bottom = pad.calculateBottomPadding() + 12.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = full) { StorageAccessCard() }
                    item(span = full) {
                        AnimatedVisibility(imp.active || imp.error != null || imp.done != null) {
                            ImportCard(imp, onCancel = model::cancelImport, onDismiss = model::dismissImport)
                        }
                    }
                    if (images.isEmpty() && !imp.active) item(span = full) { EmptyState(onHelp = { help = true }) }
                    items(images.size, key = { images[it].id }) { i ->
                        val img = images[i]
                        MachineCard(
                            img,
                            baseName = images.firstOrNull { it.id == img.baseId }?.name,
                            modifier = Modifier.animateItem(),
                            running = runningId == img.id,
                            onStart = { VmActivity.start(ctx, img.id) },
                            onMenu = { anchor -> cardMenu = img to anchor },
                        )
                    }
                }
            } else if (tab == 1) {
                app.andropaw.agent.AgentHomeContent(
                    model = model,
                    padding = PaddingValues(
                        top = pad.calculateTopPadding(),
                        bottom = pad.calculateBottomPadding(),
                    ),
                    // the top bar follows this page's own scrolling (see AgentHomeContent)
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    onOpenConversation = { id ->
                        app.andropaw.agent.ChatSession.open(ctx, id)
                        agentChat = true
                    },
                    onNewChat = { agentChat = true },
                )
            } else {
                SettingsContent(
                    onRestyle = onRestyle,
                    padding = PaddingValues(
                        top = pad.calculateTopPadding(),
                        bottom = pad.calculateBottomPadding(),
                    ),
                    onAbout = { ctx.startActivity(Intent(ctx, AboutActivity::class.java)) },
                )
            }

        }

        // 弹窗 / 底部 sheet 必须在 Scaffold 组合范围内：OverlayDialog / OverlayBottomSheet 走
        // LocalRootDialogStates 注册到 Scaffold 的 MiuixPopupHost，放在 Scaffold 之外只能拿到
        // 默认空表，宿主永远看不到这条状态，窗口也就弹不出来。
        settingsFor?.let { img ->
            SettingsSheet(img, onDismiss = { settingsFor = null }, onSave = { s -> model.updateSettings(img, s); settingsFor = null })
        }
        resetFor?.let { img ->
            MiuixDialog(
                show = true,
                title = stringResource(R.string.reset_title, img.name),
                summary = stringResource(R.string.reset_text),
                onDismissRequest = { resetFor = null },
            ) {
                DialogButtonRow(listOf(
                    DialogAction(stringResource(R.string.cancel), onClick = { resetFor = null }),
                    DialogAction(stringResource(R.string.reset_data), onClick = {
                        resetFor = null
                        if (model.vmRunning()) android.widget.Toast.makeText(ctx, R.string.reset_running, android.widget.Toast.LENGTH_LONG).show()
                        else model.resetData(img)
                    }, primary = true, destructive = true),
                ))
            }
        }
        deleteFor?.let { img ->
            MiuixDialog(
                show = true,
                title = stringResource(R.string.delete_title, img.name),
                summary = stringResource(R.string.delete_text, Formatter.formatShortFileSize(ctx, img.sizeBytes)),
                onDismissRequest = { deleteFor = null },
            ) {
                DialogButtonRow(listOf(
                    DialogAction(stringResource(R.string.cancel), onClick = { deleteFor = null }),
                    DialogAction(stringResource(R.string.delete), onClick = { model.delete(img); deleteFor = null },
                        primary = true, destructive = true),
                ))
            }
        }
        renameFor?.let { img ->
            var name by remember { mutableStateOf(img.name) }
            MiuixDialog(
                show = true,
                title = stringResource(R.string.name),
                onDismissRequest = { renameFor = null },
            ) {
                TextField(value = name, onValueChange = { name = it }, singleLine = true, label = "")
                DialogButtonRow(listOf(
                    DialogAction(stringResource(R.string.cancel), onClick = { renameFor = null }),
                    DialogAction(stringResource(R.string.save), onClick = {
                        model.rename(img, name.trim().ifEmpty { img.name }); renameFor = null
                    }, primary = true),
                ))
            }
        }
        if (help) HelpDialog(onDismiss = { help = false })
        if (library) LibrarySheet(images.filter { it.baseId.isEmpty() }, onDismiss = { library = false },
            onImportFile = { library = false; pick.launch(arrayOf("*/*")) },
            onImportFolder = { f -> library = false; model.import(android.net.Uri.fromFile(f)) },
            onNewMachine = { library = false; containerFrom = it })
        containerFrom?.let { src -> ContainerDialog(src, images, onDismiss = { containerFrom = null },
            onCreate = { from, name, copy, settings -> containerFrom = null; model.clone(from, name, copy, settings) }) }

        UpdateDialog(
            state = updateState,
            onDismiss = { updateState = UpdateState.Idle },
            onStartDownload = ::startDownload,
            onCancelDownload = {
                downloadJob?.cancel()
                updateState = UpdateState.Idle
            },
            onInstall = { file -> AppUpdateManager.installApk(ctx, file) },
            onOpenUrl = { url -> runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) } }
        )
    }

    // The AgentPaw chat page; its execution lives in the process-level session,
    // so leaving and re-entering never interrupts a running turn. It covers the whole shell:
    // a transparent page here left the conversation list showing through behind it.
    if (agentChat) {
        // the system back gesture closes the chat page, not the app
        BackHandler { agentChat = false }
        app.andropaw.agent.AgentChatScreen(
            bridge = agentBridge,
            onBack = { agentChat = false },
        )
    }

    // A machine card's menu, composed as a layer over the whole shell rather than inside the
    // page box: Scaffold draws its top bar, its floating nav bar and the FAB after its
    // content, and inside the grid the panel was clipped by its own card and covered by the
    // neighbouring ones.
    Box(Modifier.fillMaxSize().onGloballyPositioned { menuHostOrigin = it.boundsInRoot().topLeft }) {
            cardMenu?.let { (img, anchor) ->
                GlassDropdownPopup(
                    show = true,
                    onDismissRequest = { cardMenu = null },
                    anchorBounds = Rect(
                        anchor.left - menuHostOrigin.x, anchor.top - menuHostOrigin.y,
                        anchor.right - menuHostOrigin.x, anchor.bottom - menuHostOrigin.y,
                    ),
                    backdrop = page.backdrop,
                ) {
                    GlassPopupItem(text = stringResource(R.string.settings), icon = Icons.Rounded.Tune, onClick = { cardMenu = null; settingsFor = img })
                    GlassPopupItem(text = stringResource(R.string.rename), icon = Icons.Rounded.Edit, onClick = { cardMenu = null; renameFor = img })
                    GlassPopupItem(text = stringResource(R.string.new_machine), icon = Icons.Rounded.ContentCopy, onClick = { cardMenu = null; containerFrom = img })
                    GlassPopupItem(text = stringResource(R.string.reset_data), icon = Icons.Rounded.RestartAlt, onClick = { cardMenu = null; resetFor = img })
                    GlassPopupItem(text = stringResource(R.string.delete), icon = Icons.Rounded.Delete, onClick = { cardMenu = null; deleteFor = img })
                }
            }
    }

}

@Composable
private fun ContainerDialog(
    src: GuestImage,
    images: List<GuestImage>,
    onDismiss: () -> Unit,
    onCreate: (GuestImage, String, Boolean, app.andropaw.core.VmSettings) -> Unit
) {
    var from by remember { mutableStateOf(src) }
    var name by remember(from) { mutableStateOf("${from.name} (${images.count { it.baseId == from.id || it.baseId == from.baseId.ifEmpty { "-" } } + 2})") }
    var copy by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf(1) } // 0: Perf, 1: Balanced, 2: Lite
    var settings by remember(from) { mutableStateOf(from.settings) }
    val has32 = app.andropaw.core.CpuInfo.hasAArch32

    fun applyPreset(p: Int) {
        preset = p
        settings = when (p) {
            0 -> settings.copy(gpu = true, hwui = true, jit = true, lowRam = false, native32 = has32)
            1 -> settings.copy(gpu = true, hwui = true, jit = true, lowRam = false, native32 = false)
            else -> settings.copy(gpu = false, hwui = false, jit = true, lowRam = true, width = 480, height = 800, native32 = false)
        }
    }

    MiuixDialog(
        show = true,
        title = stringResource(R.string.vmos_add_title),
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Base ROM Card
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(12.dp)) {
                Text(stringResource(R.string.section_systems), fontSize = 12.sp, color = MiuixTheme.colorScheme.primary)
                Text("${from.name} · ${from.displayVersion}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(from.skin, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }

            // Name field
            TextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.name), singleLine = true)

            // Presets
            Text(stringResource(R.string.vmos_preset_title), fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurface)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SelectChip(selected = preset == 0, label = stringResource(R.string.vmos_preset_perf),
                    onClick = { applyPreset(0) }, modifier = Modifier.weight(1f))
                SelectChip(selected = preset == 1, label = stringResource(R.string.vmos_preset_balanced),
                    onClick = { applyPreset(1) }, modifier = Modifier.weight(1f))
                SelectChip(selected = preset == 2, label = stringResource(R.string.vmos_preset_lite),
                    onClick = { applyPreset(2) }, modifier = Modifier.weight(1f))
            }

            // Toggles
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { copy = !copy },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(state = ToggleableState(copy), onClick = { copy = !copy })
                Text(stringResource(R.string.container_copy_data), fontSize = 15.sp)
            }

            if (has32) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                        settings = settings.copy(native32 = !settings.native32)
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(state = ToggleableState(settings.native32), onClick = { settings = settings.copy(native32 = !settings.native32) })
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.vs_native32), fontSize = 15.sp)
                        Text(stringResource(R.string.vs_native32_sub), fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }

            Text(stringResource(R.string.container_hint), fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        DialogButtonRow(listOf(
            DialogAction(stringResource(R.string.cancel), onClick = onDismiss),
            DialogAction(stringResource(R.string.container_create), onClick = {
                onCreate(from, name.trim().ifEmpty { from.name }, copy, settings)
            }, primary = true),
        ))
    }
}

/** All-files access: the guest's shared folder lives in «Internal storage/AndroPaw». */
@Composable
private fun StorageAccessCard() {
    if (Build.VERSION.SDK_INT < 30) return
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(android.os.Environment.isExternalStorageManager()) }
    val life = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(life) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) granted = android.os.Environment.isExternalStorageManager()
        }
        life.lifecycle.addObserver(obs)
        onDispose { life.lifecycle.removeObserver(obs) }
    }
    if (granted) return
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(20.dp)) {
        Text(stringResource(R.string.storage_title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.storage_text), fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            runCatching {
                ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    android.net.Uri.parse("package:${ctx.packageName}")))
            }.onFailure { ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        }) { Text(stringResource(R.string.allow)) }
    }
}

@Composable
private fun EmptyState(onHelp: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier.size(112.dp).clip(CircleShape).background(MiuixTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Android, null, Modifier.size(56.dp), tint = MiuixTheme.colorScheme.onPrimaryContainer) }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.empty_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_text),
            fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        TextButton(text = stringResource(R.string.empty_help), onClick = onHelp)
    }
}

@Composable
private fun ImportCard(s: ImportState, onCancel: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (s.active) CircularProgressIndicator(size = 40.dp)
            else Icon(if (s.error != null) Icons.Rounded.Warning else Icons.Rounded.Android, null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(when { s.error != null -> stringResource(R.string.import_failed); s.active -> stringResource(R.string.import_running); else -> stringResource(R.string.import_done) },
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(s.file, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            s.error != null -> Text(s.error, fontSize = 14.sp)
            s.active -> {
                if (s.progress in 0f..1f) LinearProgressIndicator(progress = s.progress, modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(s.step, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            s.done != null -> {
                Text("${s.done.name} · ${s.done.displayVersion} · ${s.done.skin}", fontSize = 14.sp)
                s.done.warnings.forEach { Text("• $it", fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (s.active) TextButton(text = stringResource(R.string.cancel), onClick = onCancel)
            else TextButton(text = stringResource(R.string.ok), onClick = onDismiss)
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    MiuixDialog(
        show = true,
        title = stringResource(R.string.help_title),
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(stringResource(R.string.help_text), fontSize = 14.sp)
        }
        DialogButtonRow(listOf(DialogAction(stringResource(R.string.ok), onClick = onDismiss, primary = true)))
    }
}
