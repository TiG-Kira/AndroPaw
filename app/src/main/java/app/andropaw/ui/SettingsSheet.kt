package app.andropaw.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.R
import app.andropaw.core.CpuInfo
import app.andropaw.core.GuestImage
import app.andropaw.core.VmSettings
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

private data class Res(val label: String, val w: Int, val h: Int, val dpi: Int)

private val RESOLUTIONS = listOf(
    Res("480×800", 480, 800, 240),
    Res("540×960", 540, 960, 240),
    Res("720×1280", 720, 1280, 320),
)

private val RAM_STEPS = listOf(0, 256, 512, 768, 1024, 1536, 2048, 3072, 4096)

@Composable
fun SettingsSheet(img: GuestImage, onDismiss: () -> Unit, onSave: (VmSettings) -> Unit) {
    var s by remember { mutableStateOf(img.settings) }
    WindowDialog(
        show = true,
        title = stringResource(R.string.settings),
        summary = img.name,
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.vs_screen), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (s.legacyEngine && img.api < 14) {
                Text(stringResource(R.string.vs_fixed_2x), fontSize = 14.sp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(ConnectedButtonSpace), modifier = Modifier.fillMaxWidth()) {
                    RESOLUTIONS.forEachIndexed { i, r ->
                        ConnectedToggleButton(
                            checked = s.width == r.w && s.height == r.h,
                            onCheckedChange = { s = s.copy(width = r.w, height = r.h, density = r.dpi) },
                            shape = connectedButtonShapes(i, RESOLUTIONS.lastIndex),
                            modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                        ) { checked ->
                            Text(r.label, fontSize = 13.sp,
                                color = if (checked) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    NumField(stringResource(R.string.vs_width), s.width, Modifier.weight(1f)) { s = s.copy(width = it.coerceIn(240, 2160) and 0x7ffffffe) }
                    NumField(stringResource(R.string.vs_height), s.height, Modifier.weight(1f)) { s = s.copy(height = it.coerceIn(320, 3840) and 0x7ffffffe) }
                    NumField(stringResource(R.string.vs_dpi), s.density, Modifier.weight(1f)) { s = s.copy(density = it.coerceIn(96, 640)) }
                }
                Text(stringResource(R.string.vs_screen_hint), fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(top = 2.dp))
            }

            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.vs_perf), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Toggle(stringResource(R.string.vs_gpu), stringResource(R.string.vs_gpu_sub), s.gpu) { s = s.copy(gpu = it) }
            if (img.api >= 14) Toggle(stringResource(R.string.vs_hwui), stringResource(R.string.vs_hwui_sub), s.hwui) { s = s.copy(hwui = it) }
            Toggle(stringResource(R.string.vs_jit), stringResource(R.string.vs_jit_sub), s.jit) { s = s.copy(jit = it) }
            if (img.api >= 21) Toggle(stringResource(R.string.vs_fulldex), stringResource(R.string.vs_fulldex_sub), s.fullDexopt) { s = s.copy(fullDexopt = it) }
            if (img.api < 14) Toggle(stringResource(R.string.vs_legacy), stringResource(R.string.vs_legacy_sub), s.legacyEngine) { s = s.copy(legacyEngine = it) }
            val has32 = CpuInfo.hasAArch32
            Toggle(
                stringResource(R.string.vs_native32),
                if (has32) stringResource(R.string.vs_native32_sub) else stringResource(R.string.vs_native32_unsupported),
                s.native32 && has32
            ) { if (has32) s = s.copy(native32 = it) }
            val ramIdx = RAM_STEPS.indexOf(s.ramMb).coerceAtLeast(0)
            BasicComponent(
                title = stringResource(R.string.vs_ram),
                summary = if (s.ramMb == 0) stringResource(R.string.vs_ram_auto) else "${s.ramMb} MB",
                bottomAction = {
                    Slider(
                        value = ramIdx.toFloat(),
                        onValueChange = { s = s.copy(ramMb = RAM_STEPS[it.toInt()]) },
                        valueRange = 0f..RAM_STEPS.lastIndex.toFloat(),
                        steps = RAM_STEPS.size - 2,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                },
            )
            Toggle(stringResource(R.string.vs_lowram), stringResource(R.string.vs_lowram_sub), s.lowRam) { s = s.copy(lowRam = it) }
            Toggle(stringResource(R.string.vs_proxy), stringResource(R.string.vs_proxy_sub), s.netProxy) { s = s.copy(netProxy = it) }

            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.vs_radio_title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Toggle(stringResource(R.string.vs_radio), stringResource(R.string.vs_radio_sub), s.radio) { s = s.copy(radio = it) }
            if (s.radio) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextField(value = s.imei, onValueChange = { v -> s = s.copy(imei = v.filter(Char::isDigit).take(15)) },
                    label = "IMEI", singleLine = true, modifier = Modifier.weight(1f))
                TextButton(text = stringResource(R.string.vs_random), onClick = { s = s.copy(imei = VmSettings.randomImei()) })
            }

            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.vs_advanced), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            TextField(value = s.qemuArgs, onValueChange = { s = s.copy(qemuArgs = it) },
                label = stringResource(R.string.vs_qemu_args), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
            Text(stringResource(R.string.vs_qemu_args_sub), fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)

            Spacer(Modifier.height(4.dp))
            RecoverySection(img)

            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.vs_controls), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Toggle(stringResource(R.string.vs_nav), stringResource(R.string.vs_nav_sub), s.showNavBar) { s = s.copy(showNavBar = it) }
            if (s.showNavBar) NavBarSetup(s) { s = it }
            Toggle(stringResource(R.string.vs_awake), stringResource(R.string.vs_awake_sub), s.keepScreenOn) { s = s.copy(keepScreenOn = it) }
            Toggle(stringResource(R.string.as_led), stringResource(R.string.as_led_sub), s.showLed) { s = s.copy(showLed = it) }
            Toggle(stringResource(R.string.vs_single), stringResource(R.string.vs_single_sub), s.mtMode == 4) { s = s.copy(mtMode = if (it) 4 else 0) }

            Spacer(Modifier.height(8.dp))
            Button(onClick = { onSave(s) }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(stringResource(R.string.save)) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Which buttons the bar has (tap to add or remove; added ones go to the end) and how big it is. */
@Composable
internal fun NavBarSetup(s: VmSettings, set: (VmSettings) -> Unit) {
    val keys = s.navKeys.split(',').filter { it in VmSettings.NAV_ALL }
    androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (k in keys + VmSettings.NAV_ALL.filter { it !in keys }) {
            val on = k in keys
            SelectChip(selected = on, label = navKeyLabel(k),
                onClick = { set(s.copy(navKeys = (if (on) keys - k else keys + k).joinToString(","))) })
        }
    }
    Toggle(stringResource(R.string.vs_nav_compact), stringResource(R.string.vs_nav_compact_sub), s.navSize == 0) { set(s.copy(navSize = if (it) 0 else 1)) }
}

@Composable
internal fun navKeyLabel(k: String) = stringResource(when (k) {
    "back" -> R.string.back; "home" -> R.string.home; "recents" -> R.string.recents; "menu" -> R.string.menu
    "search" -> R.string.nav_search; "volup" -> R.string.vol_up; "voldown" -> R.string.vol_down
    "power" -> R.string.power; else -> R.string.nav_dpad
})

@Composable
private fun NumField(label: String, value: Int, modifier: Modifier, set: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    TextField(value = text, onValueChange = { v -> text = v.filter(Char::isDigit).take(4); text.toIntOrNull()?.let(set) },
        label = label, singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}

/** Recovery of this firmware: stock one from the import, or a custom recovery.img (TWRP, OrangeFox, CWM…). */
@Composable
private fun RecoverySection(img: GuestImage) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val paths = remember(img.id) { app.andropaw.core.VmPaths(ctx, img.id) }
    var status by remember { mutableStateOf(recoveryStatus(paths)) }
    var busy by remember { mutableStateOf(false) }
    val failed = stringResource(R.string.vs_recovery_failed)
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        Thread {
            val ok = runCatching {
                val data = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                app.andropaw.core.RecoveryImage.install(paths, data) {}
            }.getOrDefault(false)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                busy = false
                status = recoveryStatus(paths)
                if (!ok) android.widget.Toast.makeText(ctx, failed, android.widget.Toast.LENGTH_LONG).show()
            }
        }.start()
    }
    Text(stringResource(R.string.vs_recovery), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    BasicComponent(
        title = status ?: stringResource(R.string.vs_recovery_none),
        summary = stringResource(R.string.vs_recovery_sub),
        endActions = {
            TextButton(text = stringResource(R.string.vs_recovery_install), onClick = { pick.launch(arrayOf("*/*")) }, enabled = !busy)
        },
    )
}

private fun recoveryStatus(paths: app.andropaw.core.VmPaths): String? {
    if (!app.andropaw.core.RecoveryImage.installed(paths)) return null
    val dir = app.andropaw.core.RecoveryImage.dir(paths)
    val prop = runCatching { java.io.File(dir, "default.prop").readText() }.getOrDefault("")
    val twrp = Regex("""ro\.twrp\.version=(\S+)""").find(prop)?.groupValues?.get(1)
    return when {
        twrp != null -> "TWRP $twrp"
        java.io.File(dir, "twres").isDirectory -> "TWRP"
        java.io.File(dir, "sbin/orangefox.sh").exists() || prop.contains("orangefox", true) -> "OrangeFox"
        prop.contains("cwm", true) || java.io.File(dir, "res/images/icon_clockwork.png").exists() -> "ClockworkMod"
        else -> "Stock recovery"
    }
}
