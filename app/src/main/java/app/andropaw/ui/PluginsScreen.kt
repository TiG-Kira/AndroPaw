package app.andropaw.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.R
import app.andropaw.core.GuestImage
import app.andropaw.core.GuestVm
import app.andropaw.core.Plugin
import app.andropaw.core.PluginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The plug-in page: one emulator, its modules, and everything that can be done to them.
 *
 * Layout follows KernelSU's module page: the machine's framework on top, then one card of
 * module rows — icon, name, author and version, an enable switch and a chevron — then the
 * detail of whichever row was opened.
 */
@Composable
fun PluginsContent(images: List<GuestImage>, padding: PaddingValues) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var targetId by remember { mutableStateOf(app.andropaw.AppPrefs.pluginsTarget(ctx)) }
    val selected = images.firstOrNull { it.id == targetId } ?: images.firstOrNull()
    var plugins by remember { mutableStateOf(emptyList<Plugin>()) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmUninstall by remember { mutableStateOf<Plugin?>(null) }
    var version by remember { mutableStateOf(0) }

    LaunchedEffect(selected?.id, version) {
        if (selected == null) { plugins = emptyList(); return@LaunchedEffect }
        while (true) {
            plugins = runCatching { PluginManager.list(ctx, selected) }.getOrDefault(emptyList())
            delay(2_000)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val img = selected
        if (uri == null || img == null) return@rememberLauncherForActivityResult
        scope.launch {
            val res = runCatching { installPlugin(ctx, img, uri, null) }
            message = res.fold(
                onSuccess = { ctx.getString(R.string.plugins_installed_ok, it.name) },
                onFailure = { ctx.getString(R.string.plugins_failed, it.message ?: it.toString()) },
            )
            version++
        }
    }

    val detail = plugins.firstOrNull { it.id == detailId }
    if (detail != null && selected != null) {
        PluginDetail(
            img = selected,
            plugin = detail,
            padding = padding,
            onBack = { detailId = null },
            onChanged = { version++ },
            onUninstall = { confirmUninstall = detail },
        )
        UninstallDialog(
            plugin = confirmUninstall,
            onDismiss = { confirmUninstall = null },
            onConfirm = { p ->
                confirmUninstall = null
                scope.launch {
                    withContext(Dispatchers.IO) { runCatching { PluginManager.uninstall(ctx, selected, p) } }
                    detailId = null
                    version++
                }
            },
        )
        message?.let { MessageDialog(it) { message = null } }
        return
    }

    val choose: (GuestImage) -> Unit = { img ->
        targetId = img.id
        app.andropaw.AppPrefs.setPluginsTarget(ctx, img.id)
        version++
    }
    if (selected == null) {
        // nothing belongs to "no machine": the page is just the picker until one is chosen
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { EmulatorSelector(images, null, choose) }
            item { NoteCard(stringResource(R.string.plugins_no_emulator_hint)) }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { EmulatorSelector(images, selected, choose) }

        item {
            Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(0.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(MiuixTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Extension, null, Modifier.size(24.dp),
                            tint = MiuixTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(backendLine(selected), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.plugins_count, plugins.size),
                            fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    TextButton(text = stringResource(R.string.plugins_install), onClick = { picker.launch(arrayOf("*/*")) })
                }
            }
        }

        item {
            ManagementSection(stringResource(R.string.plugins_section_installed))
        }

        if (plugins.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.plugins_empty), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.plugins_empty_sub), fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(vertical = 6.dp)) {
                    plugins.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(Modifier.padding(start = 64.dp))
                        PluginRow(
                            plugin = p,
                            onOpen = { detailId = p.id },
                            onToggle = { on ->
                                plugins = plugins.map { if (it.id == p.id) it.copy(enabled = on) else it }
                                scope.launch {
                                    withContext(Dispatchers.IO) { runCatching { PluginManager.setEnabled(ctx, selected, p, on) } }
                                    version++
                                }
                            },
                        )
                    }
                }
            }
        }

        item { NoteCard(stringResource(R.string.plugins_note)) }
    }

    message?.let { MessageDialog(it) { message = null } }
}

/** One module row, KernelSU-style: icon, name, version · author, switch, chevron. */
@Composable
private fun PluginRow(plugin: Plugin, onOpen: () -> Unit, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LetterAvatar(plugin.name, plugin.id)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plugin.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (plugin.version.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(plugin.version, fontSize = 11.sp, color = MiuixTheme.colorScheme.primary, maxLines = 1)
                }
            }
            Text(
                listOfNotNull(plugin.author.ifBlank { null }, plugin.description.ifBlank { null }).firstOrNull()
                    ?: plugin.dir,
                fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = plugin.enabled, onCheckedChange = onToggle)
    }
}

/** The whole page for one module: what it is, where it lives, and what can be done to it. */
@Composable
private fun PluginDetail(
    img: GuestImage,
    plugin: Plugin,
    padding: PaddingValues,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onUninstall: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var actionResult by remember { mutableStateOf<String?>(null) }
    var enabled by remember(plugin.id) { mutableStateOf(plugin.enabled) }

    Column(
        Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)
            .padding(top = padding.calculateTopPadding()),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, stringResource(R.string.back), Modifier.size(20.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(plugin.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = { on ->
                enabled = on
                scope.launch {
                    withContext(Dispatchers.IO) { runCatching { PluginManager.setEnabled(ctx, img, plugin, on) } }
                    onChanged()
                }
            })
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LetterAvatar(plugin.name, plugin.id, 56.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(plugin.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(plugin.version.ifBlank { null }, plugin.author.ifBlank { null })
                                    .joinToString(" · ").ifBlank { plugin.id },
                                fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    if (plugin.description.isNotBlank()) {
                        Spacer(Modifier.size(12.dp))
                        Text(plugin.description, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(vertical = 6.dp)) {
                    DetailRow(stringResource(R.string.plugins_backend_row), backendLine(img))
                    HorizontalDivider(Modifier.padding(start = 16.dp))
                    DetailRow(stringResource(R.string.plugins_size), android.text.format.Formatter
                        .formatShortFileSize(ctx, plugin.sizeBytes))
                    HorizontalDivider(Modifier.padding(start = 16.dp))
                    DetailRow(stringResource(R.string.plugins_path), plugin.dir)
                    HorizontalDivider(Modifier.padding(start = 16.dp))
                    DetailRow(
                        stringResource(R.string.plugins_installed),
                        if (plugin.installedAt > 0) java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(plugin.installedAt)) else "—",
                    )
                    if (plugin.apk != null) {
                        HorizontalDivider(Modifier.padding(start = 16.dp))
                        DetailRow(
                            stringResource(R.string.plugins_apk_row),
                            if (plugin.apkPending) stringResource(R.string.plugins_apk_pending) else "/${plugin.apk}",
                        )
                    }
                    if (plugin.xposedEntry != null) {
                        HorizontalDivider(Modifier.padding(start = 16.dp))
                        DetailRow(stringResource(R.string.plugins_entry), plugin.xposedEntry)
                    }
                    if (plugin.hasScripts) {
                        HorizontalDivider(Modifier.padding(start = 16.dp))
                        DetailRow(stringResource(R.string.plugins_scripts), "post-fs-data.sh · service.sh")
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(0.dp)) {
                    if (plugin.hasAction) {
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                scope.launch {
                                    val out = withContext(Dispatchers.IO) {
                                        runCatching { PluginManager.runAction(ctx, img, plugin) }
                                    }
                                    actionResult = out.getOrElse { it.message ?: it.toString() }
                                }
                            }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp),
                                tint = MiuixTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.plugins_action), fontSize = 15.sp)
                        }
                        HorizontalDivider(Modifier.padding(start = 16.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = onUninstall).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Delete, null, Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.plugins_uninstall), fontSize = 15.sp,
                            color = MiuixTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    actionResult?.let { MessageDialog(it) { actionResult = null } }
}

/**
 * The plug-in window inside a running machine: the same list and the same switches as the
 * page, sized to sit next to the guest's screen (like the AgentPaw chat window). Here an
 * install can also reach the running system, so a module's apk lands immediately.
 */
@Composable
fun PluginsOverlay(
    img: GuestImage?,
    vm: GuestVm?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var plugins by remember { mutableStateOf(emptyList<Plugin>()) }
    var version by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(img?.id, version) {
        if (img == null) return@LaunchedEffect
        while (true) {
            plugins = runCatching { PluginManager.list(ctx, img) }.getOrDefault(emptyList())
            delay(2_000)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || img == null) return@rememberLauncherForActivityResult
        scope.launch {
            val res = runCatching { installPlugin(ctx, img, uri, vm) }
            message = res.fold(
                onSuccess = { ctx.getString(R.string.plugins_installed_ok, it.name) },
                onFailure = { ctx.getString(R.string.plugins_failed, it.message ?: it.toString()) },
            )
            version++
        }
    }

    Card(modifier.width(320.dp).heightIn(max = 460.dp), insideMargin = PaddingValues(0.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Extension, null, Modifier.size(16.dp), tint = MiuixTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.tab_plugins), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Box(Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.close), Modifier.size(16.dp))
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(img?.let { backendLine(it) } ?: "", fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.weight(1f))
                Box(Modifier.size(30.dp).clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primaryContainer)
                    .clickable { picker.launch(arrayOf("*/*")) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.plugins_install), Modifier.size(17.dp),
                        tint = MiuixTheme.colorScheme.onPrimaryContainer)
                }
            }
            HorizontalDivider()
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                if (plugins.isEmpty()) {
                    item {
                        Text(stringResource(R.string.plugins_empty_sub), fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(12.dp))
                    }
                } else {
                    items(plugins, key = { it.id }) { p ->
                        Row(
                            Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LetterAvatar(p.name, p.id, 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(listOfNotNull(p.version.ifBlank { null }, p.author.ifBlank { null }).joinToString(" · "),
                                    fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Switch(checked = p.enabled, onCheckedChange = { on ->
                                plugins = plugins.map { if (it.id == p.id) it.copy(enabled = on) else it }
                                scope.launch {
                                    withContext(Dispatchers.IO) { runCatching { img?.let { PluginManager.setEnabled(ctx, it, p, on) } } }
                                    version++
                                }
                            })
                        }
                    }
                }
            }
        }
    }
    message?.let { MessageDialog(it) { message = null } }
}

/** Copies a picked document into the cache and hands it to the store. */
private suspend fun installPlugin(ctx: Context, img: GuestImage, uri: Uri, vm: GuestVm?): Plugin {
    val suffix = when {
        ctx.contentResolver.getType(uri)?.contains("android.package") == true -> ".apk"
        uri.lastPathSegment?.endsWith(".apk", true) == true -> ".apk"
        else -> ".zip"
    }
    val tmp = java.io.File(ctx.cacheDir, "apw-plugin-${System.currentTimeMillis()}$suffix")
    try {
        withContext(Dispatchers.IO) {
            ctx.contentResolver.openInputStream(uri)?.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                ?: error("cannot open the picked file")
            PluginManager.install(ctx, img, tmp, vm)
        }.let { return it }
    } finally {
        runCatching { tmp.delete() }
    }
}

/** One-line result of whatever the page just did. */
@Composable
private fun MessageDialog(text: String, onDismiss: () -> Unit) {
    MiuixDialog(show = true, title = stringResource(R.string.plugins_result), onDismissRequest = onDismiss) {
        Text(text, fontSize = 13.sp, lineHeight = 19.sp)
        DialogButtonRow(listOf(DialogAction(stringResource(R.string.done), onClick = onDismiss, primary = true)))
    }
}

@Composable
private fun UninstallDialog(plugin: Plugin?, onDismiss: () -> Unit, onConfirm: (Plugin) -> Unit) {
    if (plugin == null) return
    MiuixDialog(
        show = true,
        title = stringResource(R.string.plugins_uninstall_q),
        summary = stringResource(R.string.plugins_uninstall_text, plugin.name),
        onDismissRequest = onDismiss,
    ) {
        DialogButtonRow(listOf(
            DialogAction(stringResource(R.string.plugins_uninstall), onClick = { onConfirm(plugin) },
                primary = true, destructive = true),
            DialogAction(stringResource(R.string.cancel), onClick = onDismiss),
        ))
    }
}
