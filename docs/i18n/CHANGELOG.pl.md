# AEmulator 0.0.0.3

[🇬🇧 English](../../CHANGELOG.md) · [🇷🇺 Русский](CHANGELOG.ru.md) · [🇺🇦 Українська](CHANGELOG.uk.md) · [🇩🇪 Deutsch](CHANGELOG.de.md) · [🇫🇷 Français](CHANGELOG.fr.md) · [🇪🇸 Español](CHANGELOG.es.md) · [🇧🇷 Português](CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](CHANGELOG.it.md) · **🇵🇱 Polski** · [🇹🇷 Türkçe](CHANGELOG.tr.md) · [🇸🇦 العربية](CHANGELOG.ar.md) · [🇮🇷 فارسی](CHANGELOG.fa.md) · [🇮🇳 हिन्दी](CHANGELOG.hi.md) · [🇮🇩 Indonesia](CHANGELOG.id.md) · [🇻🇳 Tiếng Việt](CHANGELOG.vi.md) · [🇨🇳 简体中文](CHANGELOG.zh-CN.md) · [🇯🇵 日本語](CHANGELOG.ja.md) · [🇰🇷 한국어](CHANGELOG.ko.md)

### Emulacja
- Nowy natywny silnik arm64: Android 5.0–17 i obrazy GSI (8–17) działają bezpośrednio na telefonie; Android 2.3–4.4 przez QEMU.
- Obsługa 64-bitowych firmware; 32-bitowe programy w nich działają przez qemu-arm.
- Pełna obsługa Androida 8, w tym GSI.
- zygote i mediaserver padają znacznie rzadziej (poprawki bindera, usunięta blokada system_server).
- `/system` jest dla gościa tylko do odczytu, prekompilowany kod frameworka już nie ginie.
- Szybszy pierwszy start firmware Dalvik (Galaxy S III: 104 s → 59 s).
- Opcja sprzętowego wykonywania 32-bit (AArch32) bez translatora, w razie braku — QEMU.
- GLES 1/2/3 przez SwiftShader.

### Oprogramowanie i producenci
- TouchWiz: `ro.tvout.enable=false`, `ro.SecEDS.enable=false` i poprawki bindera.
- Firmware Qualcomm: usuwane są `qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager`.
- Infinix (AOSP / XUI / XOS) i Tecno (AOSP / HiOS) rozpoznawane według wersji Androida.
- Sony: emulacja diody powiadomień.
- Poprawki dla MIUI, HTC Sense, TouchWiz, Xperia i AOSP. Przetestowano na Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17.

### Import
- Nowe formaty: 7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`.

### Funkcje
- Internet działa w większej liczbie firmware.
- Naprawiony dźwięk; dodano mikrofon, aparat, akcelerometr i wibracje.
- Pasek nawigacji: Wstecz, Ekran główny i Ostatnie działają w każdym firmware.
- ADB w sieci lokalnej i wbudowany terminal ADB.
- Naprawione menu z trzema kropkami w uruchomionym systemie: zrzuty ekranu, nagrywanie ekranu, VNC, SMS, tryb samolotowy, instalacja APK i więcej.
- Przycisk menu można przesuwać w uruchomionym systemie.
- Opcja ukrycia wskaźnika LED: w ustawieniach domyślnych nowych systemów i dla każdego systemu.
