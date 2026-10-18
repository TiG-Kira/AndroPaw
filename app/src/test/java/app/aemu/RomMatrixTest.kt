package app.aemu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RomMatrixTest {

    private fun guessApi(rel: String?): Int = when {
        rel == null -> 19
        rel.startsWith("1.0") -> 1; rel.startsWith("1.1") -> 2; rel.startsWith("1.5") -> 3; rel.startsWith("1.6") -> 4
        rel.startsWith("2.0") -> 5; rel.startsWith("2.1") -> 7; rel.startsWith("2.2") -> 8; rel.startsWith("2.3") -> 10
        rel.startsWith("3.0") -> 11; rel.startsWith("3.1") -> 12; rel.startsWith("3.2") -> 13
        rel.startsWith("4.0") -> 15; rel.startsWith("4.1") -> 16; rel.startsWith("4.2") -> 17; rel.startsWith("4.3") -> 18
        rel.startsWith("4.4") -> 19; rel.startsWith("5.0") -> 21; rel.startsWith("5.1") -> 22
        rel.startsWith("6") -> 23; rel.startsWith("7.0") -> 24; rel.startsWith("7") -> 25
        rel.startsWith("8.0") -> 26; rel.startsWith("8.1") -> 27; rel.startsWith("8") -> 26
        rel.startsWith("9") -> 28; rel.startsWith("10") -> 29; rel.startsWith("11") -> 30
        rel.startsWith("12") -> 31; rel.startsWith("13") -> 33; rel.startsWith("14") -> 34
        rel.startsWith("15") -> 35; rel.startsWith("16") -> 36; rel.startsWith("17") -> 37
        else -> 19
    }

    private fun detectSkin(p: Map<String, String>, files: Set<String> = emptySet()): String {
        val man = (p["ro.product.manufacturer"] ?: p["ro.product.brand"] ?: "").lowercase()
        fun has(path: String) = files.contains(path)
        val brand = (p["ro.product.brand"] ?: "").lowercase()
        val api = p["ro.build.version.sdk"]?.toIntOrNull() ?: guessApi(p["ro.build.version.release"])
        return when {
            p.containsKey("ro.mi.os.version.name") || p.containsKey("ro.mi.os.version.code") -> "HyperOS " + (p["ro.mi.os.version.name"] ?: "")
            man.contains("infinix") || brand.contains("infinix") -> when { api >= 23 -> "XOS"; api >= 21 -> "XUI"; else -> "AOSP" }
            man.contains("tecno") || brand.contains("tecno") -> if (api >= 23) "HiOS" else "AOSP"
            p.containsKey("ro.build.version.oneui") || p.containsKey("ro.build.version.sep") -> "One UI"
            p.containsKey("ro.build.version.realmeui") -> "realme UI"
            p.containsKey("ro.build.version.oplusrom") -> "ColorOS"
            p.containsKey("ro.miui.ui.version.name") || p.containsKey("ro.miui.ui.version.code") || has("system/app/MiuiHome.apk") || has("system/app/MiuiHome") -> "MIUI " + (p["ro.miui.ui.version.name"] ?: "")
            p.containsKey("ro.cm.version") -> "CyanogenMod ${p["ro.cm.version"]?.substringBefore('-')}"
            p.containsKey("ro.lineage.version") -> "LineageOS"
            p["ro.build.display.id"]?.contains("Flyme", true) == true -> "Flyme"
            p.containsKey("ro.build.version.emui") -> "EMUI"
            p.containsKey("ro.build.version.opporom") -> "ColorOS"
            (p["ro.product.brand"] ?: "").lowercase() == "google" || p["ro.build.tags"]?.contains("release-keys") == true && man.contains("samsung") && !has("system/framework/twframework.jar") && !has("system/app/TwLauncher.apk") && !has("system/app/SecLauncher2.apk") -> "AOSP"
            has("system/framework/twframework.jar") || has("system/app/TwLauncher.apk") || has("system/app/TwLauncher") || has("system/app/SecLauncher2.apk") || man.contains("samsung") -> "TouchWiz"
            man.contains("htc") || has("system/framework/com.htc.framework.jar") || has("system/framework/HTCExtension.jar") -> "HTC Sense"
            man.contains("motorola") -> "MOTOBLUR"
            man.contains("lge") || man.contains("lg") -> "LG Optimus UI"
            man.contains("sony") || man.contains("semc") -> "Xperia UI"
            man.contains("asus") -> "ZenUI"
            man.contains("huawei") -> "EMUI"
            man.contains("lenovo") -> "Vibe UI"
            else -> "AOSP"
        }
    }

    @Test
    fun testApiGuessing() {
        assertEquals(1, guessApi("1.0"))
        assertEquals(8, guessApi("2.2.1"))
        assertEquals(10, guessApi("2.3.7"))
        assertEquals(16, guessApi("4.1.2"))
        assertEquals(19, guessApi("4.4.4"))
        assertEquals(21, guessApi("5.0.2"))
        assertEquals(23, guessApi("6.0.1"))
        assertEquals(26, guessApi("8.0.0"))
        assertEquals(28, guessApi("9.0"))
        assertEquals(30, guessApi("11"))
        assertEquals(34, guessApi("14"))
        assertEquals(37, guessApi("17"))
    }

    @Test
    fun testRomVendorMatrix() {
        // TouchWiz
        val tw = detectSkin(mapOf("ro.product.manufacturer" to "samsung", "ro.build.version.release" to "4.2.2"))
        assertEquals("TouchWiz", tw)

        // One UI
        val oneUi = detectSkin(mapOf("ro.product.manufacturer" to "samsung", "ro.build.version.oneui" to "50100"))
        assertEquals("One UI", oneUi)

        // MIUI
        val miui = detectSkin(mapOf("ro.miui.ui.version.name" to "V12"))
        assertEquals("MIUI V12", miui)

        // HyperOS
        val hyper = detectSkin(mapOf("ro.mi.os.version.name" to "1.0.1.0"))
        assertEquals("HyperOS 1.0.1.0", hyper)

        // HTC Sense
        val htc = detectSkin(mapOf("ro.product.manufacturer" to "HTC", "ro.build.version.release" to "4.0.4"))
        assertEquals("HTC Sense", htc)

        // Sony Xperia UI
        val sony = detectSkin(mapOf("ro.product.manufacturer" to "Sony", "ro.build.version.release" to "4.4.4"))
        assertEquals("Xperia UI", sony)

        // ColorOS
        val color = detectSkin(mapOf("ro.build.version.opporom" to "V7.1"))
        assertEquals("ColorOS", color)

        // Infinix rules: Android 4 -> AOSP, Android 5 -> XUI, Android 6+ -> XOS
        assertEquals("AOSP", detectSkin(mapOf("ro.product.brand" to "Infinix", "ro.build.version.release" to "4.4.2")))
        assertEquals("XUI", detectSkin(mapOf("ro.product.brand" to "Infinix", "ro.build.version.release" to "5.1")))
        assertEquals("XOS", detectSkin(mapOf("ro.product.brand" to "Infinix", "ro.build.version.release" to "7.0")))

        // Tecno rules: Android 4-5 -> AOSP, Android 6+ -> HiOS
        assertEquals("AOSP", detectSkin(mapOf("ro.product.brand" to "Tecno", "ro.build.version.release" to "4.4.2")))
        assertEquals("AOSP", detectSkin(mapOf("ro.product.brand" to "Tecno", "ro.build.version.release" to "5.0.2")))
        assertEquals("HiOS", detectSkin(mapOf("ro.product.brand" to "Tecno", "ro.build.version.release" to "6.0")))
    }

    @Test
    fun testQcomParkedComponents() {
        val qcomList = app.aemu.core.TreeFixer.QCOM_PARK
        assertTrue(qcomList.contains("system/etc/permissions/qti_permissions.xml"))
        assertTrue(qcomList.contains("system/etc/permissions/com.qti.dpmframework.xml"))
        assertTrue(qcomList.contains("system/priv-app/com.qualcomm.location"))
        assertTrue(qcomList.contains("system/priv-app/qcrilmsgtunnel"))
        assertTrue(qcomList.contains("system/priv-app/dpmserviceapp"))
        assertTrue(qcomList.contains("system/priv-app/DiracAudioControlService"))
        assertTrue(qcomList.contains("system/app/DiracManager"))
    }
}
