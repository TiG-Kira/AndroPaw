package app.andropaw.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * AndroPaw — miuix building blocks.
 *
 * Everything here sits on miuix / MiuixTheme tokens only; no Material3 widget survives in the
 * UI layer. This file replaces the former Material3 1.3 "compat shims": the Expressive components
 * are rebuilt on miuix primitives (WindowDialog for dialogs and bottom sheets, GlassDropdownPopup
 * for anchored menus, BasicComponent for list rows).
 */

/** Gap used by a "connected" button group. */
val ConnectedButtonSpace = 2.dp

/** Corner geometry for a connected button group: rounded on the outer edges only. */
fun connectedButtonShapes(index: Int, lastIndex: Int): RoundedCornerShape = when (index) {
    0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp)
    lastIndex -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp)
    else -> RoundedCornerShape(4.dp)
}

/**
 * One segment of a connected toggle group: filled with the primary container when checked,
 * a quiet surface otherwise. [content] receives the checked state so its text can follow.
 */
@Composable
fun ConnectedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    content: @Composable RowScope.(Boolean) -> Unit,
) {
    val bg = if (checked) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.surfaceContainer
    Box(
        modifier
            .background(bg, shape)
            .clip(shape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) { content(checked) }
    }
}

/** A small selectable chip (Material FilterChip replacement) on miuix tokens. */
@Composable
fun SelectChip(
    selected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = if (selected) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.surfaceContainer
    val fg = if (selected) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurface
    Box(
        modifier
            .background(if (enabled) bg else MiuixTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) fg else MiuixTheme.colorScheme.disabledOnSurface,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

/** One action of a miuix dialog. */
data class DialogAction(val label: String, val onClick: () -> Unit, val primary: Boolean = false, val destructive: Boolean = false)

@Composable
fun DialogButtonRow(actions: List<DialogAction>) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { a ->
            if (a.primary) {
                val container = if (a.destructive) MiuixTheme.colorScheme.errorContainer else MiuixTheme.colorScheme.primary
                val content = if (a.destructive) MiuixTheme.colorScheme.onErrorContainer else MiuixTheme.colorScheme.onPrimary
                Button(
                    onClick = a.onClick,
                    colors = ButtonDefaults.buttonColors(color = container, contentColor = content),
                ) { Text(a.label, color = content, fontWeight = FontWeight.SemiBold) }
            } else {
                TextButton(text = a.label, onClick = a.onClick)
            }
        }
    }
}

/**
 * How many popups currently ask the floating glass nav bar to sink off-screen.
 * Every dialog / bottom sheet registers itself here for the duration it is shown — the same
 * contract Termux-Ultra's MainNavBarHideEffect fulfils for its wrappers, so no new popup can
 * miss the behaviour. The home screen folds this into the bar's `visible`.
 */
object MainNavBarVisibility {
    var hideRequesters by mutableStateOf(0)
    val hidden: Boolean get() = hideRequesters > 0
}

/**
 * Registers [shown] with [MainNavBarVisibility] for as long as it is true.
 * Call from every popup wrapper — a popup that shows without this leaves the glass bar frozen
 * above it.
 */
@Composable
fun MainNavBarHideEffect(shown: Boolean) {
    androidx.compose.runtime.DisposableEffect(shown) {
        if (shown) MainNavBarVisibility.hideRequesters++
        onDispose { if (MainNavBarVisibility.hideRequesters > 0) MainNavBarVisibility.hideRequesters-- }
    }
}

/**
 * A HyperOS-style dialog on miuix [OverlayDialog] — the Termux-Ultra way. Unlike the
 * window-based [top.yukonga.miuix.kmp.window.WindowDialog], an overlay dialog renders inside
 * the same window, which keeps it under the glass popup layer and away from the
 * separate-window focus quirks that crashed the former dialogs. Bottom sheet on phones,
 * centred on large screens.
 *
 * [content] is placed in a 14dp-spaced column; put a [DialogButtonRow] last.
 */
@Composable
fun MiuixDialog(
    show: Boolean,
    title: String,
    summary: String? = null,
    onDismissRequest: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    MainNavBarHideEffect(show)
    OverlayDialog(
        show = show,
        title = title,
        summary = summary,
        onDismissRequest = onDismissRequest,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
    }
}

/**
 * A bottom sheet on miuix [OverlayBottomSheet] — the overlay twin of [MiuixDialog] for the
 * larger sheets (firmware library, per-machine settings). Same window as the app, so the
 * glass nav bar can sink under it and the backdrop layer stays intact.
 */
@Composable
fun MiuixSheet(
    show: Boolean,
    title: String,
    onDismissRequest: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    MainNavBarHideEffect(show)
    OverlayBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
    ) {
        content()
    }
}

/** One entry of [GlassMenuButton]. */
data class GlassMenuItem(
    val text: String,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * An icon button that drops a glass menu — the Termux-Ultra way (no Material popup survives).
 *
 * [backdrop] `null` keeps the panel's bloom stroke and shadow over an opaque fill, for surfaces
 * without a captured glass layer.
 */
@Composable
fun GlassMenuButton(
    icon: ImageVector,
    contentDescription: String?,
    items: List<GlassMenuItem>,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    iconTint: Color? = null,
) {
    var open by remember { mutableStateOf(false) }
    var boxOrigin by remember { mutableStateOf(Offset.Zero) }
    var iconRect by remember { mutableStateOf(Rect.Zero) }
    Box(modifier.onGloballyPositioned { boxOrigin = it.positionInWindow() }) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { open = true }
                .onGloballyPositioned { iconRect = it.boundsInWindow() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, tint = iconTint ?: MiuixTheme.colorScheme.onSurface)
        }
        if (open || iconRect != Rect.Zero) {
            val local = Rect(
                iconRect.left - boxOrigin.x, iconRect.top - boxOrigin.y,
                iconRect.right - boxOrigin.x, iconRect.bottom - boxOrigin.y,
            )
            GlassDropdownPopup(
                show = open,
                onDismissRequest = { open = false },
                anchorBounds = local,
                backdrop = backdrop,
            ) {
                items.forEach { item ->
                    GlassPopupItem(
                        text = item.text,
                        icon = item.icon,
                        enabled = item.enabled,
                        onClick = { open = false; item.onClick() },
                    )
                }
            }
        }
    }
}
