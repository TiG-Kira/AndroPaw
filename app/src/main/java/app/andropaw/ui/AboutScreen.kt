package app.andropaw.ui

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import app.andropaw.BuildConfig
import app.andropaw.R
import app.andropaw.update.AppUpdateManager
import app.andropaw.update.ReleaseInfo
import app.andropaw.update.UpdateState
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.glass.MiuixGlassIcons
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The About page, migrated from Termux-Ultra (KiTerminal-UX) and adapted to AndroPaw's
 * updater: breathing-gradient background (RuntimeShader on API 33+, Brush fallback below),
 * glass top bar with a chevron back button, and ArrowPreference cards for the developer,
 * the upstream project, updates, version info and the license.
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // 本页在主 tab 取景层之外，自建一层供玻璃顶栏折射页面内容
    val glassPage = rememberGlassPageBackdrop()
    val scrollBehavior = MiuixScrollBehavior()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val systemNavBarsHeight = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }

    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    val updateCtx = LocalContext.current
    fun checkForUpdates() {
        if (updateState is UpdateState.Checking || updateState is UpdateState.Downloading) return
        updateState = UpdateState.Checking
        scope.launch {
            val res = AppUpdateManager.checkLatestRelease(updateCtx.applicationContext)
            res.fold(
                onSuccess = { rel ->
                    if (rel != null && AppUpdateManager.isNewerVersion(rel.versionName, BuildConfig.VERSION_NAME)) {
                        updateState = UpdateState.Available(rel)
                    } else {
                        updateState = UpdateState.UpToDate(BuildConfig.VERSION_NAME, rel?.tagName ?: "")
                    }
                },
                onFailure = { err -> updateState = UpdateState.Error(err.localizedMessage ?: "Network error") },
            )
        }
    }

    fun startDownload(rel: ReleaseInfo) {
        updateState = UpdateState.Downloading(rel, 0f, 0L, rel.sizeBytes)
        downloadJob = scope.launch {
            try {
                val file = AppUpdateManager.downloadApk(context, rel) { progress, downloaded, total ->
                    updateState = UpdateState.Downloading(rel, progress, downloaded, total)
                }
                updateState = UpdateState.ReadyToInstall(rel, file)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    updateState = UpdateState.Error(e.localizedMessage ?: "Download error")
                }
            }
        }
    }

    // 呼吸渐变动画 (Termux-Ultra FeatureCenterCard 风格)
    val infiniteTransition = rememberInfiniteTransition(label = "breathingGradient")
    val gradientFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gradientBreathing",
    )

    val darkTheme = isSystemInDarkTheme()

    // 呼吸渐变颜色 (FeatureCenterCard 风格)
    val lightGradient = Brush.verticalGradient(
        colors = listOf(
            lerp(Color(0xFFFFE5EF), Color(0xFFF9DBE5), gradientFraction),
            lerp(Color(0xFFF7BAD1), Color(0xFFBCC1FF), gradientFraction),
            lerp(Color(0xFFA3A5F9), Color(0xFF8EAFFF), gradientFraction),
        )
    )
    val darkGradient = Brush.verticalGradient(
        colors = listOf(
            lerp(Color(0xFF330FE0), Color(0xFF934CBC), gradientFraction),
            lerp(Color(0xFF4C238C), Color(0xFF9E35AA), gradientFraction),
            lerp(Color(0xFF1C28D3), Color(0xFF0033C6), gradientFraction),
        )
    )

    // 滚动状态跟踪：头部卡片随滚动淡出
    val listState = rememberLazyListState()
    val headerHeightPx = with(density) { 100.dp.toPx() }
    var scrollFraction by remember { mutableStateOf(0f) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.offset ?: 0 }
            .collect { offset ->
                val firstIndex = listState.firstVisibleItemIndex
                scrollFraction = if (firstIndex == 0) {
                    (-offset.toFloat() / headerHeightPx).coerceIn(0f, 1f)
                } else {
                    1f
                }
            }
    }
    val headerAlphaAnim by animateFloatAsState(targetValue = 1f - scrollFraction, label = "headerAlpha")

    // Dual-track background: API 33+ RuntimeShader animated, older Brush fallback
    val useShaderBg = android.os.Build.VERSION.SDK_INT >= 33
    var bgController by remember { mutableStateOf<AboutBgEffect.ShaderController?>(null) }
    var bgDarkTheme by remember { mutableStateOf(darkTheme) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
    ) {
        if (useShaderBg) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (1f - scrollFraction) * (if (darkTheme) 0.5f else 1f) }
                    .blur(120.dp)
                    .zIndex(-1f),
                factory = { ctx ->
                    android.view.View(ctx).apply {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        val ctl = AboutBgEffect.createFor(this, darkTheme)
                        bgController = ctl
                        bgDarkTheme = darkTheme
                        ctl?.start()
                    }
                },
                update = { v ->
                    if (bgDarkTheme != darkTheme) {
                        bgController?.updateParams(AboutBgEffect.getParams(darkTheme))
                        bgDarkTheme = darkTheme
                    }
                },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (1f - scrollFraction) * (if (darkTheme) 0.5f else 1f) }
                    .background(if (darkTheme) darkGradient else lightGradient)
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                GlassTopAppBar(
                    title = stringResource(R.string.about_title),
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
                state = listState,
                modifier = Modifier
                    .then(glassPage.contentModifier)
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = standaloneContentPadding(padding, bottom = systemNavBarsHeight + 26.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                // 头部：logo + 应用名 + 版本
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = headerAlphaAnim }
                            .padding(top = 60.dp, bottom = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val headerFg = if (darkTheme) Color.White else Color(0xFF333333)
                        Image(
                            painter = painterResource(R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(100.dp),
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "AndroPaw",
                            style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Black, color = headerFg),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = TextStyle(fontSize = 16.sp, color = headerFg),
                        )
                    }
                }

                // 设备信息卡
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            InfoRow(stringResource(R.string.about_device_model), android.os.Build.MODEL)
                            InfoRow(stringResource(R.string.about_android_version), android.os.Build.VERSION.RELEASE)
                            InfoRow(stringResource(R.string.about_kernel), android.os.Build.DISPLAY)
                        }
                    }
                }

                // 开发者卡
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    val devUrl = stringResource(R.string.about_developer_url)
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        ArrowPreference(
                            title = stringResource(R.string.about_developer_name),
                            summary = stringResource(R.string.about_developer_handle),
                            onClick = { openUrl(context, devUrl) },
                            startAction = { AvatarBox("https://github.com/TiG-Kira.png") },
                        )
                    }
                }

                // 检查更新卡
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    val updateSummary = when (updateState) {
                        is UpdateState.Checking -> stringResource(R.string.update_checking)
                        is UpdateState.Available -> stringResource(R.string.update_available_title)
                        is UpdateState.Downloading -> stringResource(R.string.update_downloading)
                        is UpdateState.UpToDate -> stringResource(R.string.update_uptodate_title)
                        is UpdateState.Error -> stringResource(R.string.update_error_title)
                        else -> stringResource(R.string.as_check_updates_sub, BuildConfig.VERSION_NAME)
                    }
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        ArrowPreference(
                            title = stringResource(R.string.as_check_updates),
                            summary = updateSummary,
                            onClick = { checkForUpdates() },
                        )
                    }
                }

                // 版本信息卡
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                text = stringResource(R.string.about_version_info),
                                style = TextStyle(fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary),
                            )
                            Text(
                                text = BuildConfig.VERSION_NAME,
                                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.onSurface),
                            )
                            Text(
                                text = stringResource(R.string.about_app_version_label) + " · " +
                                    stringResource(R.string.about_upstream_info),
                                style = TextStyle(fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary),
                            )
                        }
                    }
                }

                // 许可证卡
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.about_license),
                                style = TextStyle(fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurface),
                            )
                            Text(
                                text = stringResource(R.string.about_license_text),
                                style = TextStyle(fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary),
                            )
                        }
                    }
                }
            }

            // The update dialogs are miuix overlays: they ride the Scaffold's overlay host, so they
            // have to be composed inside it — outside, the state lands in an empty table and the
            // window never opens.
            UpdateDialog(
                state = updateState,
                onDismiss = { updateState = UpdateState.Idle },
                onStartDownload = ::startDownload,
                onCancelDownload = {
                    downloadJob?.cancel()
                    updateState = UpdateState.Idle
                },
                onInstall = { file -> AppUpdateManager.installApk(context, file) },
                onOpenUrl = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) } },
            )
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
}

/** A 40dp round GitHub avatar — the Termux-Ultra developer-row style. */
@Composable
private fun AvatarBox(model: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Column(verticalArrangement = Arrangement.Center) {
        Text(
            text = value,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.onSurface),
        )
        Text(
            text = title,
            style = TextStyle(fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
