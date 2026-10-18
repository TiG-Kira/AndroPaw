# AEmulator 0.0.0.3

[🇬🇧 English](../../CHANGELOG.md) · [🇷🇺 Русский](CHANGELOG.ru.md) · [🇺🇦 Українська](CHANGELOG.uk.md) · **🇩🇪 Deutsch** · [🇫🇷 Français](CHANGELOG.fr.md) · [🇪🇸 Español](CHANGELOG.es.md) · [🇧🇷 Português](CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](CHANGELOG.it.md) · [🇵🇱 Polski](CHANGELOG.pl.md) · [🇹🇷 Türkçe](CHANGELOG.tr.md) · [🇸🇦 العربية](CHANGELOG.ar.md) · [🇮🇷 فارسی](CHANGELOG.fa.md) · [🇮🇳 हिन्दी](CHANGELOG.hi.md) · [🇮🇩 Indonesia](CHANGELOG.id.md) · [🇻🇳 Tiếng Việt](CHANGELOG.vi.md) · [🇨🇳 简体中文](CHANGELOG.zh-CN.md) · [🇯🇵 日本語](CHANGELOG.ja.md) · [🇰🇷 한국어](CHANGELOG.ko.md)

### Emulation
- Neue native arm64-Engine: Android 5.0–17 und GSIs (8–17) laufen direkt auf dem Handy; Android 2.3–4.4 über QEMU.
- Unterstützung für 64-Bit-Firmware; 32-Bit-Programme darin laufen über qemu-arm.
- Volle Unterstützung für Android 8, einschließlich GSIs.
- zygote und mediaserver stürzen deutlich seltener ab (Binder-Korrekturen, system_server-Deadlock behoben).
- `/system` ist für das Gastsystem schreibgeschützt, vorkompilierter Framework-Code geht nicht mehr verloren.
- Schnellerer erster Start von Dalvik-Firmware (Galaxy S III: 104 s → 59 s).
- Option für native 32-Bit-Ausführung (AArch32) ohne Übersetzer, sonst QEMU.
- GLES 1/2/3 über SwiftShader.

### Firmware und Hersteller
- TouchWiz: `ro.tvout.enable=false`, `ro.SecEDS.enable=false` und Binder-Patches.
- Qualcomm-Firmware: entfernt werden `qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager`.
- Infinix (AOSP / XUI / XOS) und Tecno (AOSP / HiOS) werden nach Android-Version erkannt.
- Sony: Emulation der Benachrichtigungs-LED.
- Korrekturen für MIUI, HTC Sense, TouchWiz, Xperia und AOSP. Getestet auf Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17.

### Import
- Neue Formate: 7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`.

### Funktionen
- Internet funktioniert in mehr Firmware.
- Ton repariert; Mikrofon, Kamera, Beschleunigungssensor und Vibration hinzugefügt.
- Navigationsleiste: Zurück, Home und Letzte Apps funktionieren in jeder Firmware.
- ADB im lokalen Netzwerk und integriertes ADB-Terminal.
- Drei-Punkte-Menü im laufenden System repariert: Screenshots, Bildschirmaufnahme, VNC, SMS, Flugmodus, APK-Installation und mehr.
- Die Menütaste lässt sich im laufenden System verschieben.
- Option zum Ausblenden der LED-Anzeige: in den Standards für neue Systeme und pro System.
