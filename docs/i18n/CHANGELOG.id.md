# AEmulator 0.0.0.3

[🇬🇧 English](../../CHANGELOG.md) · [🇷🇺 Русский](CHANGELOG.ru.md) · [🇺🇦 Українська](CHANGELOG.uk.md) · [🇩🇪 Deutsch](CHANGELOG.de.md) · [🇫🇷 Français](CHANGELOG.fr.md) · [🇪🇸 Español](CHANGELOG.es.md) · [🇧🇷 Português](CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](CHANGELOG.it.md) · [🇵🇱 Polski](CHANGELOG.pl.md) · [🇹🇷 Türkçe](CHANGELOG.tr.md) · [🇸🇦 العربية](CHANGELOG.ar.md) · [🇮🇷 فارسی](CHANGELOG.fa.md) · [🇮🇳 हिन्दी](CHANGELOG.hi.md) · **🇮🇩 Indonesia** · [🇻🇳 Tiếng Việt](CHANGELOG.vi.md) · [🇨🇳 简体中文](CHANGELOG.zh-CN.md) · [🇯🇵 日本語](CHANGELOG.ja.md) · [🇰🇷 한국어](CHANGELOG.ko.md)

### Emulasi
- Mesin arm64 native baru: Android 5.0–17 dan GSI (8–17) berjalan langsung di ponsel; Android 2.3–4.4 lewat QEMU.
- Dukungan firmware 64-bit; program 32-bit di dalamnya berjalan lewat qemu-arm.
- Dukungan penuh Android 8, termasuk GSI.
- zygote dan mediaserver jauh lebih jarang crash (perbaikan binder, deadlock system_server diperbaiki).
- `/system` hanya-baca bagi tamu, sehingga kode framework yang sudah dikompilasi tidak lagi hilang.
- Boot pertama firmware Dalvik lebih cepat (Galaxy S III: 104 dtk → 59 dtk).
- Opsi eksekusi 32-bit perangkat keras (AArch32) tanpa penerjemah, dengan QEMU sebagai cadangan.
- GLES 1/2/3 lewat SwiftShader.

### Firmware dan vendor
- TouchWiz: `ro.tvout.enable=false`, `ro.SecEDS.enable=false` dan patch binder.
- Firmware Qualcomm: `qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager` dihapus.
- Infinix (AOSP / XUI / XOS) dan Tecno (AOSP / HiOS) dikenali dari versi Android.
- Sony: emulasi LED notifikasi.
- Perbaikan untuk MIUI, HTC Sense, TouchWiz, Xperia, dan AOSP. Diuji pada Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17.

### Impor
- Format baru: 7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`.

### Fitur
- Internet berfungsi di lebih banyak firmware.
- Suara diperbaiki; mikrofon, kamera, akselerometer, dan getaran ditambahkan.
- Bilah navigasi: Kembali, Beranda, dan Terbaru berfungsi di semua firmware.
- ADB lewat jaringan lokal dan terminal ADB bawaan.
- Menu tiga titik di sistem yang berjalan diperbaiki: tangkapan layar, rekam layar, VNC, SMS, mode pesawat, pasang APK, dan lainnya.
- Tombol menu dapat dipindahkan di sistem yang berjalan.
- Opsi menyembunyikan indikator LED: di bawaan sistem baru dan per sistem.
