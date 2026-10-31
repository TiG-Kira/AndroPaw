package app.andropaw.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Shield
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
import app.andropaw.core.RootApp
import app.andropaw.core.RootManager
import app.andropaw.core.RootState
import app.andropaw.core.VmPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The ROOT page: one emulator, a switch that gives it a `su`, and the apps that asked for it.
 *
 * The list is KernelSU's superuser page: one row per app — icon, name, package and id, a
 * switch on the right that answers the request. Rows come from the gate itself (the requests
 * `su` recorded inside the guest), so nothing is listed that did not really ask.
 */
@Composable
fun RootContent(images: List<GuestImage>, padding: PaddingValues) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var targetId by remember { mutableStateOf(app.andropaw.AppPrefs.rootTarget(ctx)) }
    val selected = images.firstOrNull { it.id == targetId } ?: images.firstOrNull()
    var state by remember { mutableStateOf(RootState()) }
    var detail by remember { mutableStateOf<RootApp?>(null) }
    var switching by remember { mutableStateOf(false) }

    LaunchedEffect(selected?.id) {
        if (selected == null) { state = RootState(); return@LaunchedEffect }
        while (true) {
            state = withContext(Dispatchers.IO) {
                runCatching { RootManager.pump(ctx, selected) }.getOrDefault(RootManager.state(ctx, selected.id))
            }
            delay(1_500)
        }
    }

    val choose: (GuestImage) -> Unit = { img ->
        targetId = img.id
        app.andropaw.AppPrefs.setRootTarget(ctx, img.id)
    }
    if (selected == null) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { EmulatorSelector(images, null, choose, badge = { rootBackendLine(it) }) }
            item { NoteCard(stringResource(R.string.root_no_emulator_hint)) }
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
        item { EmulatorSelector(images, selected, choose, badge = { rootBackendLine(it) }) }

        item {
            Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(0.dp)) {
                BasicComponent(
                    title = stringResource(R.string.root_enable),
                    summary = if (state.enabled) stringResource(R.string.root_status_on, rootBackendLine(selected))
                    else stringResource(R.string.root_status_off, rootBackendLine(selected)),
                    startAction = {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(if (state.enabled) MiuixTheme.colorScheme.primaryContainer
                                else MiuixTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Shield, null, Modifier.size(20.dp),
                                tint = if (state.enabled) MiuixTheme.colorScheme.onPrimaryContainer
                                else MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    },
                    endActions = {
                        Switch(checked = state.enabled, onCheckedChange = { on ->
                            if (switching) return@Switch
                            switching = true
                            state = state.copy(enabled = on)
                            scope.launch {
                                val next = withContext(Dispatchers.IO) {
                                    runCatching { RootManager.setEnabled(ctx, selected, on) }.getOrNull()
                                }
                                if (next != null) state = next
                                switching = false
                            }
                        })
                    },
                )
            }
        }

        item {
            ManagementSection(
                stringResource(R.string.root_apps),
                if (state.apps.isEmpty()) null
                else stringResource(R.string.root_apps_sub, state.apps.size, state.deniedCount),
            )
        }

        if (state.apps.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.root_empty), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.root_empty_sub), fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary, lineHeight = 18.sp)
                    }
                }
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(vertical = 6.dp)) {
                    state.apps.forEachIndexed { i, app ->
                        if (i > 0) HorizontalDivider(Modifier.padding(start = 68.dp))
                        RootAppRow(
                            paths = VmPaths(ctx, selected.id),
                            app = app,
                            onOpen = { detail = app },
                            onToggle = { allow ->
                                state = state.copy(apps = state.apps.map {
                                    if (it.pkg == app.pkg) it.copy(granted = allow, denied = !allow) else it
                                })
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        runCatching { RootManager.decide(ctx, selected, app.pkg, allow) }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        item { NoteCard(stringResource(R.string.root_note)) }
    }

    detail?.let { app ->
        RootAppDialog(
            app = state.apps.firstOrNull { it.pkg == app.pkg } ?: app,
            onDismiss = { detail = null },
            onDecide = { allow ->
                detail = null
                state = state.copy(apps = state.apps.map {
                    if (it.pkg == app.pkg) it.copy(granted = allow == true, denied = allow == false) else it
                })
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            if (allow == null) RootManager.forget(ctx, selected, app.pkg)
                            else RootManager.decide(ctx, selected, app.pkg, allow)
                        }
                    }
                }
            },
        )
    }
}

/** One app on the superuser list: icon, label, package and id, and the switch that answers it. */
@Composable
private fun RootAppRow(
    paths: VmPaths,
    app: RootApp,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val info = rememberAppInfo(paths, app.pkg)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppAvatar(info.icon, info.label, app.pkg)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(info.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (app.pending) {
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.root_pending), fontSize = 10.sp,
                        color = MiuixTheme.colorScheme.primary)
                }
            }
            Text(
                if (app.uid > 0) "${app.pkg} · ${app.uid}" else app.pkg,
                fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = app.granted, onCheckedChange = onToggle)
    }
}

/** One app's whole answer: what it asked for, and allow / deny / forget. */
@Composable
private fun RootAppDialog(app: RootApp, onDismiss: () -> Unit, onDecide: (Boolean?) -> Unit) {
    val ctx = LocalContext.current
    MiuixDialog(show = true, title = app.pkg, onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                when {
                    app.granted -> stringResource(R.string.root_granted)
                    app.denied -> stringResource(R.string.root_denied)
                    else -> stringResource(R.string.root_waiting)
                },
                fontSize = 13.sp, color = MiuixTheme.colorScheme.primary,
            )
            if (app.uid > 0) {
                Text(stringResource(R.string.root_uid, app.uid), fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            if (app.lastCommand.isNotBlank()) {
                Text(stringResource(R.string.root_last_command), fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text(app.lastCommand, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh,
                            androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .padding(8.dp))
            }
        }
        DialogButtonRow(listOf(
            DialogAction(stringResource(R.string.root_allow), onClick = { onDecide(true) }, primary = true),
            DialogAction(stringResource(R.string.root_deny), onClick = { onDecide(false) }),
            DialogAction(stringResource(R.string.root_forget), onClick = { onDecide(null) }),
        ))
    }
}

/**
 * The ROOT window inside a running machine: the switch and the apps that asked, so a request
 * that just came in can be answered without leaving the guest's screen.
 */
@Composable
fun RootOverlay(
    img: GuestImage?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(RootState()) }

    LaunchedEffect(img?.id) {
        if (img == null) return@LaunchedEffect
        while (true) {
            state = withContext(Dispatchers.IO) {
                runCatching { RootManager.pump(ctx, img) }.getOrDefault(RootManager.state(ctx, img.id))
            }
            delay(1_500)
        }
    }

    Card(modifier.width(320.dp).heightIn(max = 460.dp), insideMargin = PaddingValues(0.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Shield, null, Modifier.size(16.dp), tint = MiuixTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.tab_root), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (state.pendingCount > 0) {
                    Text(stringResource(R.string.root_pending_count, state.pendingCount), fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                }
                Box(Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.close), Modifier.size(16.dp))
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.root_enable), fontSize = 13.sp)
                    Text(
                        if (state.enabled) stringResource(R.string.root_enabled_short)
                        else stringResource(R.string.root_disabled_short),
                        fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Switch(checked = state.enabled, onCheckedChange = { on ->
                    state = state.copy(enabled = on)
                    scope.launch {
                        val next = withContext(Dispatchers.IO) {
                            runCatching { img?.let { RootManager.setEnabled(ctx, it, on) } }.getOrNull()
                        }
                        if (next != null) state = next
                    }
                })
            }
            HorizontalDivider()
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                if (state.apps.isEmpty()) {
                    item {
                        Text(stringResource(R.string.root_empty_sub), fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(12.dp), lineHeight = 17.sp)
                    }
                } else {
                    items(state.apps, key = { it.pkg }) { app ->
                        val paths = img?.let { VmPaths(ctx, it.id) }
                        val info = rememberAppInfo(paths, app.pkg)
                        Row(
                            Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppAvatar(info.icon, info.label, app.pkg, 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(info.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.pkg, fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Switch(checked = app.granted, onCheckedChange = { allow ->
                                state = state.copy(apps = state.apps.map {
                                    if (it.pkg == app.pkg) it.copy(granted = allow, denied = !allow) else it
                                })
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        runCatching { img?.let { RootManager.decide(ctx, it, app.pkg, allow) } }
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}
