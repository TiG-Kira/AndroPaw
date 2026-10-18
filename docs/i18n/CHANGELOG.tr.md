# AEmulator 0.0.0.3

[🇬🇧 English](../../CHANGELOG.md) · [🇷🇺 Русский](CHANGELOG.ru.md) · [🇺🇦 Українська](CHANGELOG.uk.md) · [🇩🇪 Deutsch](CHANGELOG.de.md) · [🇫🇷 Français](CHANGELOG.fr.md) · [🇪🇸 Español](CHANGELOG.es.md) · [🇧🇷 Português](CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](CHANGELOG.it.md) · [🇵🇱 Polski](CHANGELOG.pl.md) · **🇹🇷 Türkçe** · [🇸🇦 العربية](CHANGELOG.ar.md) · [🇮🇷 فارسی](CHANGELOG.fa.md) · [🇮🇳 हिन्दी](CHANGELOG.hi.md) · [🇮🇩 Indonesia](CHANGELOG.id.md) · [🇻🇳 Tiếng Việt](CHANGELOG.vi.md) · [🇨🇳 简体中文](CHANGELOG.zh-CN.md) · [🇯🇵 日本語](CHANGELOG.ja.md) · [🇰🇷 한국어](CHANGELOG.ko.md)

### Emülasyon
- Yeni yerel arm64 motoru: Android 5.0–17 ve GSI’lar (8–17) doğrudan telefonda çalışır; Android 2.3–4.4 QEMU ile çalışır.
- 64 bit yazılım desteği; içlerindeki 32 bit programlar qemu-arm ile çalışır.
- GSI’lar dahil tam Android 8 desteği.
- zygote ve mediaserver çok daha seyrek çöker (binder düzeltmeleri, system_server kilitlenmesi giderildi).
- `/system` misafir için salt okunurdur; önceden derlenmiş framework kodu artık kaybolmaz.
- Dalvik yazılımlarında daha hızlı ilk açılış (Galaxy S III: 104 sn → 59 sn).
- Çevirmen olmadan donanımsal 32 bit çalıştırma (AArch32) seçeneği; desteklenmezse QEMU kullanılır.
- SwiftShader üzerinden GLES 1/2/3.

### Yazılımlar ve üreticiler
- TouchWiz: `ro.tvout.enable=false`, `ro.SecEDS.enable=false` ve binder yamaları.
- Qualcomm yazılımları: `qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager` kaldırılır.
- Infinix (AOSP / XUI / XOS) ve Tecno (AOSP / HiOS) Android sürümüne göre tanınır.
- Sony: bildirim LED’i emülasyonu.
- MIUI, HTC Sense, TouchWiz, Xperia ve AOSP için düzeltmeler. Test edilenler: Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17.

### İçe aktarma
- Yeni biçimler: 7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`.

### Özellikler
- İnternet daha fazla yazılımda çalışıyor.
- Ses düzeltildi; mikrofon, kamera, ivmeölçer ve titreşim eklendi.
- Gezinme çubuğu: Geri, Ana ekran ve Son uygulamalar her yazılımda çalışır.
- Yerel ağ üzerinden ADB ve yerleşik ADB terminali.
- Çalışan sistemdeki üç nokta menüsü düzeltildi: ekran görüntüsü, ekran kaydı, VNC, SMS, uçak modu, APK yükleme ve dahası.
- Menü düğmesi çalışan sistemde taşınabilir.
- LED göstergesini gizleme seçeneği: yeni sistem varsayılanlarında ve her sistemde.
