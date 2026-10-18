# AEmulator 0.0.0.3

**🇬🇧 English** · [🇷🇺 Русский](docs/i18n/CHANGELOG.ru.md) · [🇺🇦 Українська](docs/i18n/CHANGELOG.uk.md) · [🇩🇪 Deutsch](docs/i18n/CHANGELOG.de.md) · [🇫🇷 Français](docs/i18n/CHANGELOG.fr.md) · [🇪🇸 Español](docs/i18n/CHANGELOG.es.md) · [🇧🇷 Português](docs/i18n/CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](docs/i18n/CHANGELOG.it.md) · [🇵🇱 Polski](docs/i18n/CHANGELOG.pl.md) · [🇹🇷 Türkçe](docs/i18n/CHANGELOG.tr.md) · [🇸🇦 العربية](docs/i18n/CHANGELOG.ar.md) · [🇮🇷 فارسی](docs/i18n/CHANGELOG.fa.md) · [🇮🇳 हिन्दी](docs/i18n/CHANGELOG.hi.md) · [🇮🇩 Indonesia](docs/i18n/CHANGELOG.id.md) · [🇻🇳 Tiếng Việt](docs/i18n/CHANGELOG.vi.md) · [🇨🇳 简体中文](docs/i18n/CHANGELOG.zh-CN.md) · [🇯🇵 日本語](docs/i18n/CHANGELOG.ja.md) · [🇰🇷 한국어](docs/i18n/CHANGELOG.ko.md)

### Emulation
- New native arm64 engine: Android 5.0–17 and GSIs (8–17) run directly on the phone; Android 2.3–4.4 runs through QEMU.
- 64-bit firmware support; 32-bit programs inside them run through qemu-arm.
- Full Android 8 support, including GSIs.
- zygote and mediaserver crash much less often (binder fixes, system_server deadlock fixed).
- `/system` is read-only for the guest, so precompiled framework code is no longer lost.
- Faster first boot of Dalvik firmware (Galaxy S III: 104 s → 59 s).
- Option for hardware 32-bit execution (AArch32) without a translator, with QEMU as the fallback.
- GLES 1/2/3 through SwiftShader.

### Firmware and vendors
- TouchWiz: `ro.tvout.enable=false`, `ro.SecEDS.enable=false` and binder patches.
- Qualcomm firmware: removed `qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager`.
- Infinix (AOSP / XUI / XOS) and Tecno (AOSP / HiOS) detected by Android version.
- Sony: notification LED emulation.
- Fixes for MIUI, HTC Sense, TouchWiz, Xperia and AOSP. Tested on Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17.

### Import
- New formats: 7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`.

### Features
- Internet works in more firmware.
- Sound fixed; microphone, camera, accelerometer and vibration added.
- Navigation bar: Back, Home and Recents work on every firmware.
- ADB over the local network and a built-in ADB terminal.
- Three-dot menu in a running system fixed: screenshots, screen recording, VNC, SMS, airplane mode, APK install and more.
- The menu button can be moved in a running system.
- Option to hide the LED indicator: in defaults for new systems and per system.
