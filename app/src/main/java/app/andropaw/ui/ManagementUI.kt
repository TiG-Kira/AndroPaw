package app.andropaw.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.R
import app.andropaw.core.GuestApps
import app.andropaw.core.GuestImage
import app.andropaw.core.PluginBackend
import app.andropaw.core.VmPaths
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.res.stringResource

/**
 * Shared pieces of the two management pages: the emulator they are pointed at, the avatar an
 * app or a plug-in gets when it has no icon, and the section header both pages use.
 */

/** "Android 13 · API 33", the line every row about a machine carries. */
@Composable
fun machineLine(img: GuestImage): String =
    stringResource(R.string.mgmt_android_version, img.release, img.api)

/** The framework this machine's generation loads plug-ins with, and what it covers. */
@Composable
fun backendLine(img: GuestImage): String {
    val b = PluginBackend.forApi(img.api)
    return stringResource(R.string.mgmt_backend_line, b.label, b.note)
}

/** The superuser implementation this machine's generation is rooted with. */
@Composable
fun rootBackendLine(img: GuestImage): String {
    val b = app.andropaw.core.RootBackend.forApi(img.api)
    return stringResource(R.string.mgmt_backend_line, b.label, b.note)
}

/**
 * The machine a management page works on: one row that opens the list of imported machines.
 *
 * Both pages start here — nothing is shown until a machine is chosen, because root and
 * plug-ins belong to exactly one machine at a time.
 */
@Composable
fun EmulatorSelector(
    images: List<GuestImage>,
    selected: GuestImage?,
    onSelect: (GuestImage) -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable (GuestImage) -> String = { PluginBackend.forApi(it.api).label },
) {
    var open by remember { mutableStateOf(false) }
    Card(modifier = modifier.fillMaxWidth(), insideMargin = PaddingValues(0.dp), onClick = { open = true }) {
        BasicComponent(
            title = selected?.name ?: stringResource(R.string.mgmt_no_emulator),
            summary = selected?.let { machineLine(it) } ?: stringResource(R.string.mgmt_no_emulator_sub),
            startAction = { AndroidAvatar() },
            endActions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    selected?.let {
                        Text(badge(it), fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.primary, maxLines = 1)
                        Spacer(Modifier.width(4.dp))
                    }
                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            },
            onClick = { open = true },
        )
    }
    if (open) {
        MiuixDialog(
            show = true,
            title = stringResource(R.string.mgmt_pick_emulator),
            onDismissRequest = { open = false },
        ) {
            if (images.isEmpty()) {
                Text(stringResource(R.string.mgmt_no_emulator_sub), fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                DialogButtonRow(listOf(DialogAction(stringResource(R.string.close), onClick = { open = false })))
            } else {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(images, key = { it.id }) { img ->
                        BasicComponent(
                            title = img.name,
                            summary = "${machineLine(img)} · ${badge(img)}",
                            startAction = { AndroidAvatar(36.dp) },
                            endActions = { RadioButton(selected = img.id == selected?.id, onClick = null) },
                            onClick = { open = false; onSelect(img) },
                        )
                    }
                }
                DialogButtonRow(listOf(DialogAction(stringResource(R.string.cancel), onClick = { open = false })))
            }
        }
    }
}

/** The Android mark, for rows that are about a machine rather than an app. */
@Composable
fun AndroidAvatar(size: Dp = 44.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f))
            .background(MiuixTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Android, null, Modifier.size(size * 0.55f),
            tint = MiuixTheme.colorScheme.onPrimaryContainer)
    }
}

/** An app or a plug-in with no icon: its initial on a colour derived from its name. */
@Composable
fun LetterAvatar(text: String, seed: String, size: Dp = 44.dp) {
    val hue = ((seed.hashCode() % 360) + 360) % 360
    val bg = Color.hsv(hue.toFloat(), 0.42f, 0.78f)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.trim().take(1).uppercase().ifBlank { "?" },
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

/** An app's real icon when the apk could be read, its initial otherwise. */
@Composable
fun AppAvatar(bitmap: Bitmap?, fallback: String, seed: String, size: Dp = 44.dp) {
    val img: ImageBitmap? = remember(bitmap) { bitmap?.asImageBitmap() }
    if (img != null) {
        Image(img, null, Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)))
    } else {
        LetterAvatar(fallback, seed, size)
    }
}

/**
 * Label + icon of a package inside [paths], read from its apk on the host.
 *
 * Reading an apk is disk work, so the row starts with the initial and is replaced when the
 * real label and icon arrive — a list of twenty apps must not block the frame that shows it.
 */
@Composable
fun rememberAppInfo(paths: VmPaths?, pkg: String): GuestApps.Info {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val fallback = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    var info by remember(paths?.root?.absolutePath, pkg) { mutableStateOf(GuestApps.Info(fallback, null)) }
    androidx.compose.runtime.LaunchedEffect(paths?.root?.absolutePath, pkg) {
        if (paths != null) {
            info = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { GuestApps.info(ctx, paths, pkg) }.getOrDefault(GuestApps.Info(fallback, null))
            }
        }
    }
    return info
}

/** A section caption above a group of rows. */
@Composable
fun ManagementSection(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = if (subtitle == null) 8.dp else 0.dp))
        subtitle?.let {
            Text(it, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        }
    }
}

/** A quiet paragraph inside a card: the note both pages end with. */
@Composable
fun NoteCard(text: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), insideMargin = PaddingValues(14.dp)) {
        Text(text, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            lineHeight = 18.sp, overflow = TextOverflow.Clip)
    }
}

/** Two values on one line, used by the plug-in detail rows. */
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurface)
        Spacer(Modifier.width(12.dp))
        Text(value, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.weight(1f, fill = false), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}
