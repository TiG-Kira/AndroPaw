package app.andropaw.ui

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.R
import app.andropaw.core.GuestImage
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The vendor's look for a machine card: a "screen" gradient that tells TouchWiz, MIUI, Sense, Xperia… apart at a
 * glance (artwork of the card, not UI chrome — the chrome around it stays on theme tokens).
 */
private fun skinColors(img: GuestImage): List<Color> {
    val s = (img.skin + " " + img.brand).lowercase()
    return when {
        "miui" in s || "hyperos" in s -> listOf(Color(0xFFFF8A3D), Color(0xFFE2462E))
        "touchwiz" in s || "one ui" in s || "samsung" in s -> listOf(Color(0xFF3D7BFF), Color(0xFF1B2F9E))
        "sense" in s || "htc" in s -> listOf(Color(0xFF7BC74D), Color(0xFF2E7D32))
        "xperia" in s || "sony" in s -> listOf(Color(0xFF8E7CFF), Color(0xFF3B2A8F))
        "coloros" in s || "realme" in s || "oppo" in s -> listOf(Color(0xFF2FD6A7), Color(0xFF0E7C68))
        "xos" in s || "xui" in s || "hios" in s || "infinix" in s || "tecno" in s -> listOf(Color(0xFF39C8F0), Color(0xFF1765B5))
        "motoblur" in s || "motorola" in s -> listOf(Color(0xFF5C6BC0), Color(0xFF263238))
        "lg" in s -> listOf(Color(0xFFE5486B), Color(0xFF8E1F3E))
        else -> listOf(Color(0xFF34C08E), Color(0xFF0B6E66))   // AOSP and the rest: Android green-teal
    }
}

/** A machine (a firmware with its own data and settings) as a little phone: tap to start, menu for the rest. */
@Composable
fun MachineCard(
    img: GuestImage,
    baseName: String?,
    modifier: Modifier = Modifier,
    running: Boolean = false,
    onStart: () -> Unit,
    onSettings: () -> Unit,
    onRename: () -> Unit,
    onNewMachine: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    Card(
        modifier = modifier,
        onClick = onStart,
        insideMargin = PaddingValues(0.dp),
        cornerRadius = 24.dp,
    ) {
        Box(
            Modifier.padding(8.dp).fillMaxWidth().aspectRatio(0.9f).clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(skinColors(img))),
        ) {
            Text(img.skin, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 14.dp, end = 48.dp))
            // the machine runs in the :vm process right now: tap the card to return to its screen
            if (running) {
                Row(
                    Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 40.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF34C08E)))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.vm_running_badge), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0B6E66))
                }
            }
            Box(Modifier.align(Alignment.TopEnd)) {
                GlassMenuButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.menu),
                    iconTint = Color.White,
                    items = listOf(
                        GlassMenuItem(stringResource(R.string.settings), Icons.Rounded.Tune) { onSettings() },
                        GlassMenuItem(stringResource(R.string.rename), Icons.Rounded.Edit) { onRename() },
                        GlassMenuItem(stringResource(R.string.new_machine), Icons.Rounded.ContentCopy) { onNewMachine() },
                        GlassMenuItem(stringResource(R.string.reset_data), Icons.Rounded.RestartAlt) { onReset() },
                        GlassMenuItem(stringResource(R.string.delete), Icons.Rounded.Delete) { onDelete() },
                    ),
                )
            }
            Column(Modifier.align(Alignment.Center).padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(img.release.substringBefore(' ').take(5), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("API ${img.api}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(10.dp).size(44.dp)
                    .clip(CircleShape).background(Color.White).clickable(onClick = onStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.start), tint = skinColors(img).last())
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp)) {
            Text(img.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    img.settings.let { "${it.width}×${it.height}" },
                    Formatter.formatShortFileSize(ctx, img.sizeBytes),
                    if (img.lastBootMs > 0) "${img.lastBootMs / 1000} s" else null,
                ).joinToString(" · "),
                fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1,
            )
            if (baseName != null) Text(stringResource(R.string.container_of, baseName), fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * "+": a FAB menu — import a firmware file, or open the firmware library, built from miuix
 * tokens (the Material3 *Expressive* `FloatingActionButtonMenu` the original used does not exist
 * on the Material3 1.3.x baseline Miuix 0.9.4 requires).
 */
@Composable
fun HomeFab(expanded: Boolean, onToggle: (Boolean) -> Unit, onImport: () -> Unit, onLibrary: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(visible = expanded) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FabButton(Icons.Rounded.Inventory2, stringResource(R.string.lib_title),
                    bg = MiuixTheme.colorScheme.secondaryContainer, fg = MiuixTheme.colorScheme.onSecondaryContainer,
                    onClick = { onToggle(false); onLibrary() })
                Spacer(Modifier.height(12.dp))
                FabButton(Icons.Rounded.FileOpen, stringResource(R.string.add_import),
                    bg = MiuixTheme.colorScheme.secondaryContainer, fg = MiuixTheme.colorScheme.onSecondaryContainer,
                    onClick = { onToggle(false); onImport() })
                Spacer(Modifier.height(12.dp))
            }
        }
        FabButton(Icons.Rounded.Close, stringResource(R.string.add_firmware),
            bg = MiuixTheme.colorScheme.primary, fg = MiuixTheme.colorScheme.onPrimary,
            swapIcon = !expanded, onClick = { onToggle(!expanded) })
    }
}

/** A circular floating action button on miuix tokens. */
@Composable
private fun FabButton(
    icon: ImageVector,
    contentDescription: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    swapIcon: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (swapIcon) Icons.Rounded.Add else icon, contentDescription, tint = fg)
    }
}

/**
 * The firmware library (VMOS-style): every imported firmware with "new machine", plus firmware files waiting in
 * Internal storage/andropaw/firmware and the import button — a miuix WindowDialog (bottom sheet on phones).
 */
@Composable
fun LibrarySheet(
    firmwares: List<GuestImage>,
    onDismiss: () -> Unit,
    onImportFile: () -> Unit,
    onImportFolder: (java.io.File) -> Unit,
    onNewMachine: (GuestImage) -> Unit,
) {
    var files by remember { mutableStateOf(emptyList<java.io.File>()) }
    DisposableEffect(Unit) {
        val t = Thread {
            runCatching { app.andropaw.core.FirmwareFolder.list() }.getOrDefault(emptyList()).let { files = it }
        }.apply { start() }
        onDispose { t.interrupt() }
    }
    MiuixSheet(
        show = true,
        title = stringResource(R.string.vmos_add_title),
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.add_container_sub), fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            // Quick Import Card
            Card(
                onClick = onImportFile,
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.FileOpen, null, tint = MiuixTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.add_import), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.add_import_sub), fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }

            // Section: Installed ROMs (VMOS-style)
            Text(stringResource(R.string.vmos_tab_roms), fontSize = 16.sp, fontWeight = FontWeight.Bold)

            if (firmwares.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                    Text(stringResource(R.string.lib_empty), fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            } else {
                for (fw in firmwares) {
                    Card(
                        onClick = { onNewMachine(fw) },
                        modifier = Modifier.fillMaxWidth(),
                        insideMargin = PaddingValues(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(skinColors(fw))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(fw.release.take(4), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(fw.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${fw.displayVersion} · ${fw.skin}", fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                            Button(onClick = { onNewMachine(fw) }) {
                                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.container_create))
                            }
                        }
                    }
                }
            }

            // Section: Firmware files in folder
            if (files.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.fw_folder_title), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.primary)
                for (f in files.take(12)) {
                    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.Inventory2, null, tint = MiuixTheme.colorScheme.secondary)
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${f.length() / 1_048_576} MB", fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                            TextButton(text = stringResource(R.string.fw_folder_import), onClick = { onImportFolder(f) })
                        }
                    }
                }
            }

            TextButton(text = stringResource(R.string.close), onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
