package app.andropaw.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.AppPrefs
import app.andropaw.BuildConfig
import app.andropaw.R
import app.andropaw.update.AppUpdateManager
import app.andropaw.update.ReleaseInfo
import app.andropaw.update.UpdateState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Project links — kept in sync across the app, README and the website. */
object Links {
    const val SITE = "https://andropaw.app"
    const val GITHUB = "https://github.com/uxazu/andropaw"
    const val CHANNEL = "https://t.me/andropaw"
    const val AUTHOR = "https://github.com/uxazu"
    const val ORIGINAL = "https://t.me/istratii_tech1"
    const val DONATE = "https://dalink.to/uxazu"
}

/**
 * The Settings tab of the home screen: appearance, defaults, updates, links and support.
 *
 * Rendered inside the glass shell's [GlassTopAppBar] + [GlassNavigationBar], so it carries no
 * top bar of its own — [padding] is the glass scaffold's content insets.
 */
@Composable
fun SettingsContent(
    onRestyle: () -> Unit,
    padding: PaddingValues,
) {
    val ctx = LocalContext.current
    var langDialog by remember { mutableStateOf(false) }
    var defaults by remember { mutableStateOf(AppPrefs.defaults(ctx)) }
    var theme by remember { mutableStateOf(AppPrefs.theme(ctx)) }
    var dynamic by remember { mutableStateOf(AppPrefs.dynamicColor(ctx)) }
    val scope = rememberCoroutineScope()
    var autoUpdates by remember { mutableStateOf(AppPrefs.autoCheckUpdates(ctx)) }
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    fun checkForUpdates() {
        updateState = UpdateState.Checking
        scope.launch {
            val res = AppUpdateManager.checkLatestRelease()
            res.fold(
                onSuccess = { rel ->
                    if (rel != null) {
                        if (AppUpdateManager.isNewerVersion(rel.versionName, BuildConfig.VERSION_NAME)) {
                            updateState = UpdateState.Available(rel)
                        } else {
                            updateState = UpdateState.UpToDate(BuildConfig.VERSION_NAME, rel.tagName)
                        }
                    } else {
                        updateState = UpdateState.UpToDate(BuildConfig.VERSION_NAME, "")
                    }
                },
                onFailure = { err ->
                    updateState = UpdateState.Error(err.localizedMessage ?: "Network error")
                }
            )
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

    fun open(url: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    fun copy(text: String) {
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("address", text))
        Toast.makeText(ctx, ctx.getString(R.string.as_copied), Toast.LENGTH_SHORT).show()
    }
    val langTag = AppPrefs.language(ctx)
    val langName = AppPrefs.LANGUAGES.firstOrNull { it.first == langTag }?.second?.ifEmpty { null } ?: stringResource(R.string.as_lang_system)

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AboutCard() }

        item {
            Section(stringResource(R.string.as_updates)) {
                Toggle(
                    stringResource(R.string.as_auto_updates),
                    stringResource(R.string.as_auto_updates_sub),
                    autoUpdates
                ) {
                    autoUpdates = it
                    AppPrefs.setAutoCheckUpdates(ctx, it)
                }
                Row_(
                    Icons.Rounded.SystemUpdate,
                    stringResource(R.string.as_check_updates),
                    stringResource(R.string.as_check_updates_sub, BuildConfig.VERSION_NAME)
                ) {
                    checkForUpdates()
                }
            }
        }

        item {
            Section(stringResource(R.string.as_appearance)) {
                Row_(Icons.Rounded.Translate, stringResource(R.string.as_language), langName) { langDialog = true }
                Text(stringResource(R.string.as_theme), fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp))
                val options = listOf(R.string.as_theme_system, R.string.as_theme_light, R.string.as_theme_dark)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(ConnectedButtonSpace)) {
                    options.forEachIndexed { i, res ->
                        ConnectedToggleButton(
                            checked = theme == i,
                            onCheckedChange = { if (theme != i) { theme = i; AppPrefs.setTheme(ctx, i) } },
                            shape = connectedButtonShapes(i, options.lastIndex),
                            modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) { checked ->
                            Text(stringResource(res), maxLines = 1, softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 13.sp,
                                color = if (checked) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurface)
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= 31) {
                    Toggle(stringResource(R.string.as_dynamic), stringResource(R.string.as_dynamic_sub), dynamic) {
                        dynamic = it; AppPrefs.setDynamicColor(ctx, it)
                    }
                }
            }
        }

        item {
            Section(stringResource(R.string.as_defaults), stringResource(R.string.as_defaults_sub)) {
                fun set(s: app.andropaw.core.VmSettings) { defaults = s; AppPrefs.setDefaults(ctx, s) }
                Toggle(stringResource(R.string.vs_gpu), stringResource(R.string.vs_gpu_sub), defaults.gpu) { set(defaults.copy(gpu = it)) }
                Toggle(stringResource(R.string.vs_jit), stringResource(R.string.vs_jit_sub), defaults.jit) { set(defaults.copy(jit = it)) }
                Toggle(stringResource(R.string.vs_proxy), stringResource(R.string.vs_proxy_sub), defaults.netProxy) { set(defaults.copy(netProxy = it)) }
                Toggle(stringResource(R.string.vs_nav), stringResource(R.string.vs_nav_sub), defaults.showNavBar) { set(defaults.copy(showNavBar = it)) }
                Toggle(stringResource(R.string.vs_awake), stringResource(R.string.vs_awake_sub), defaults.keepScreenOn) { set(defaults.copy(keepScreenOn = it)) }
                Toggle(stringResource(R.string.as_led), stringResource(R.string.as_led_sub), defaults.showLed) { set(defaults.copy(showLed = it)) }
            }
        }

        item {
            Section(stringResource(R.string.as_links)) {
                Row_(Icons.Rounded.Public, stringResource(R.string.as_site), "andropaw.app") { open(Links.SITE) }
                Row_(Icons.Rounded.Send, stringResource(R.string.as_channel), "@andropaw") { open(Links.CHANNEL) }
                Row_(Icons.Rounded.Person, stringResource(R.string.as_author), "uxazu") { open(Links.AUTHOR) }
                Row_(Icons.Rounded.History, stringResource(R.string.as_orig), "t.me/istratii_tech1") { open(Links.ORIGINAL) }
                Row_(Icons.Rounded.Code, stringResource(R.string.as_github), "github.com/uxazu/andropaw") { open(Links.GITHUB) }
            }
        }

        item {
            Section(stringResource(R.string.as_support)) {
                Row_(Icons.Rounded.Favorite, stringResource(R.string.as_donate), "dalink.to/uxazu") { open(Links.DONATE) }
            }
        }
    }

    if (langDialog) {
        MiuixDialog(
            show = true,
            title = stringResource(R.string.as_language),
            onDismissRequest = { langDialog = false },
        ) {
            LazyColumn {
                items(AppPrefs.LANGUAGES) { (tag, name) ->
                    BasicComponent(
                        title = name.ifEmpty { stringResource(R.string.as_lang_system) },
                        startAction = {
                            RadioButton(selected = tag == langTag, onClick = null)
                        },
                        onClick = {
                            langDialog = false
                            if (tag != langTag) { AppPrefs.setLanguage(ctx, tag); onRestyle() }
                        },
                    )
                }
            }
            DialogButtonRow(listOf(
                DialogAction(stringResource(R.string.cancel), onClick = { langDialog = false }),
            ))
        }
    }

    UpdateDialog(
        state = updateState,
        onDismiss = { updateState = UpdateState.Idle },
        onStartDownload = ::startDownload,
        onCancelDownload = {
            downloadJob?.cancel()
            updateState = UpdateState.Idle
        },
        onInstall = { file -> AppUpdateManager.installApk(ctx, file) },
        onOpenUrl = { url -> open(url) }
    )
}

@Composable
private fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.logo), null, Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("AndroPaw", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.as_version, BuildConfig.VERSION_NAME), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.as_about), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Section(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Column {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = if (subtitle == null) 8.dp else 0.dp))
        subtitle?.let {
            Text(it, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        }
        Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun Row_(icon: ImageVector, title: String, sub: String, mono: Boolean = false, onClick: () -> Unit) {
    BasicComponent(
        title = title,
        summary = sub,
        startAction = { Icon(icon, null, tint = MiuixTheme.colorScheme.primary) },
        onClick = onClick,
    )
}

/** A settings row with a miuix switch, used on this page and by the per-machine settings sheet. */
@Composable
internal fun Toggle(title: String, sub: String, on: Boolean, set: (Boolean) -> Unit) {
    BasicComponent(
        title = title,
        summary = sub,
        endActions = { Switch(checked = on, onCheckedChange = set) },
    )
}
