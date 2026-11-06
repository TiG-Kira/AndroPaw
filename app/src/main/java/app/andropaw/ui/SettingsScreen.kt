package app.andropaw.ui

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.andropaw.core.HostInfo
import app.andropaw.core.ProFeatures
import top.yukonga.miuix.kmp.basic.BasicComponent
import androidx.compose.material.icons.rounded.ChevronRight
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The Settings tab of the home screen: appearance and defaults. The updater, project links and
 * support now live on the About page.
 *
 * Rendered inside the glass shell's [GlassTopAppBar] + [GlassNavigationBar], so it carries no
 * top bar of its own — [padding] is the glass scaffold's content insets.
 */
@Composable
fun SettingsContent(
    onRestyle: () -> Unit,
    padding: PaddingValues,
    onAbout: () -> Unit = {},
) {
    val ctx = LocalContext.current
    var langDialog by remember { mutableStateOf(false) }
    var agentSettings by remember { mutableStateOf(false) }
    var phantomGuide by remember { mutableStateOf(false) }
    var proFeaturesDialog by remember { mutableStateOf(false) }
    var defaults by remember { mutableStateOf(AppPrefs.defaults(ctx)) }
    var theme by remember { mutableStateOf(AppPrefs.theme(ctx)) }
    var dynamic by remember { mutableStateOf(AppPrefs.dynamicColor(ctx)) }
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
        item { AboutCard(onClick = onAbout) }

        item {
            var autoRestart by remember { mutableStateOf(AppPrefs.hostKillAutoRestart(ctx)) }
            var guestStubs by remember { mutableStateOf(AppPrefs.guestStubsEnabled(ctx)) }
            var slimBoot by remember { mutableStateOf(AppPrefs.slimBoot(ctx)) }
            val pm = ctx.getSystemService(android.os.PowerManager::class.java)
            val batteryOk = pm?.isIgnoringBatteryOptimizations(ctx.packageName) == true
            val phantom = HostInfo.phantomState(ctx)
            val stateText = when (phantom) {
                HostInfo.PhantomState.NOT_APPLICABLE -> stringResource(R.string.host_phantom_na)
                HostInfo.PhantomState.DISABLED -> stringResource(R.string.host_phantom_off)
                HostInfo.PhantomState.ACTIVE -> stringResource(R.string.host_phantom_on)
                HostInfo.PhantomState.UNKNOWN -> stringResource(R.string.host_phantom_unknown)
            }
            Section(stringResource(R.string.host_compat_title), stringResource(R.string.host_compat_sub)) {
                ArrowPreference(
                    title = stringResource(R.string.host_phantom_title),
                    summary = stateText,
                    startAction = { Icon(Icons.Rounded.Tune, null, tint = MiuixTheme.colorScheme.primary) },
                    onClick = { phantomGuide = true },
                )
                ArrowPreference(
                    title = stringResource(R.string.host_battery_title),
                    summary = stringResource(if (batteryOk) R.string.host_battery_off else R.string.host_battery_on),
                    startAction = { Icon(Icons.Rounded.AutoAwesome, null, tint = MiuixTheme.colorScheme.primary) },
                    onClick = {
                        runCatching {
                            ctx.startActivity(Intent(
                                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                android.net.Uri.parse("package:${ctx.packageName}")))
                        }
                    },
                )
                Toggle(
                    stringResource(R.string.host_autorestart),
                    stringResource(R.string.host_autorestart_sub),
                    autoRestart,
                ) { autoRestart = it; AppPrefs.setHostKillAutoRestart(ctx, it) }
                Toggle(
                    stringResource(R.string.host_slim_boot),
                    stringResource(R.string.host_slim_boot_sub),
                    slimBoot,
                ) { slimBoot = it; AppPrefs.setSlimBoot(ctx, it) }
                Toggle(
                    stringResource(R.string.host_guest_stubs),
                    stringResource(R.string.host_guest_stubs_sub),
                    guestStubs,
                ) { guestStubs = it; AppPrefs.setGuestStubsEnabled(ctx, it) }
            }
        }

        item {
            var level by remember { mutableStateOf(ProFeatures.GuardLevel.of(AppPrefs.proGuardLevel(ctx))) }
            Section(stringResource(R.string.pro_title), stringResource(R.string.pro_sub)) {
                Text(stringResource(R.string.pro_guard_label), fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp))
                val levels = ProFeatures.GuardLevel.entries
                val labels = listOf(R.string.pro_guard_conservative, R.string.pro_guard_standard, R.string.pro_guard_aggressive)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(ConnectedButtonSpace)) {
                    levels.forEachIndexed { i, l ->
                        ConnectedToggleButton(
                            checked = level == l,
                            onCheckedChange = { level = l; AppPrefs.setProGuardLevel(ctx, l.key) },
                            shape = connectedButtonShapes(i, levels.lastIndex),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) { checked ->
                            Text(stringResource(labels[i]), maxLines = 1, softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 13.sp,
                                color = if (checked) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurface)
                        }
                    }
                }
                Text(stringResource(R.string.pro_guard_hint), fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp))
                ArrowPreference(
                    title = stringResource(R.string.pro_features_row),
                    summary = stringResource(R.string.pro_features_row_sub, ProFeatures.ALL.size),
                    startAction = { Icon(Icons.Rounded.Tune, null, tint = MiuixTheme.colorScheme.primary) },
                    onClick = { proFeaturesDialog = true },
                )
            }
        }

        item {
            Section(stringResource(R.string.agent_settings_group)) {
                ArrowPreference(
                    title = stringResource(R.string.agent_llm_row),
                    summary = stringResource(R.string.agent_settings_sub),
                    startAction = { Icon(Icons.Rounded.AutoAwesome, null, tint = MiuixTheme.colorScheme.primary) },
                    onClick = { agentSettings = true },
                )
            }
        }

        item {
            Section(stringResource(R.string.as_appearance)) {
                ArrowPreference(
                    title = stringResource(R.string.as_language),
                    summary = langName,
                    startAction = { Icon(Icons.Rounded.Translate, null, tint = MiuixTheme.colorScheme.primary) },
                    onClick = { langDialog = true },
                )
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
    }

    if (langDialog) {
        MiuixDialog(
            show = true,
            title = stringResource(R.string.as_language),
            onDismissRequest = { langDialog = false },
        ) {
            // Bounded height: an unbounded LazyColumn inside a sheet crashes at measure time.
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
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

    if (agentSettings) {
        app.andropaw.agent.AgentSettingsDialog(onDismiss = { agentSettings = false })
    }

    if (phantomGuide) {
        MiuixDialog(
            show = true,
            title = stringResource(R.string.host_phantom_guide_title),
            onDismissRequest = { phantomGuide = false },
        ) {
            Text(HostInfo.phantomGuide(), fontSize = 13.sp, lineHeight = 19.sp)
            DialogButtonRow(listOf(
                DialogAction(stringResource(R.string.host_copy_adb), onClick = {
                    val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as? android.content.ClipboardManager
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("adb", HostInfo.ADB_RAISE_LIMIT))
                    android.widget.Toast.makeText(ctx, R.string.host_copied,
                        android.widget.Toast.LENGTH_SHORT).show()
                }),
                DialogAction(stringResource(R.string.done), onClick = { phantomGuide = false }),
            ))
        }
    }

    if (proFeaturesDialog) {
        MiuixDialog(
            show = true,
            title = stringResource(R.string.pro_features_title),
            onDismissRequest = { proFeaturesDialog = false },
        ) {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(ProFeatures.ALL) { f ->
                    val state = stringResource(when (f.state) {
                        ProFeatures.State.BUILT_IN -> R.string.pro_state_built_in
                        ProFeatures.State.PLANNED -> R.string.pro_state_planned
                    })
                    BasicComponent(
                        title = "${stringResource(f.titleRes)} · $state",
                        summary = stringResource(f.summaryRes),
                    )
                }
            }
            DialogButtonRow(listOf(
                DialogAction(stringResource(R.string.done), onClick = { proFeaturesDialog = false }),
            ))
        }
    }

}

/**
 * The AndroPaw card that opens the About page.
 *
 * The whole card is the button: the ripple and the click area are the card's, not the row's,
 * which is what the card is for — the padding around the row used to be dead space that
 * neither lit up nor answered a tap. That also keeps the touch target at the card's full
 * height instead of the row's.
 */
@Composable
private fun AboutCard(onClick: () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(0.dp), onClick = onClick) {
        BasicComponent(
            title = "AndroPaw",
            summary = stringResource(R.string.as_version, BuildConfig.VERSION_NAME),
            startAction = {
                Image(painterResource(R.drawable.logo), null, Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)))
            },
            endActions = {
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            },
        )
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

/** A settings row with a miuix switch, used on this page and by the per-machine settings sheet. */
@Composable
internal fun Toggle(title: String, sub: String, on: Boolean, set: (Boolean) -> Unit) {
    BasicComponent(
        title = title,
        summary = sub,
        endActions = { Switch(checked = on, onCheckedChange = set) },
    )
}
