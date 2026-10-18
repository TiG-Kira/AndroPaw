# AEmulator 0.0.0.3

[🇬🇧 English](../../CHANGELOG.md) · **🇷🇺 Русский** · [🇺🇦 Українська](CHANGELOG.uk.md) · [🇩🇪 Deutsch](CHANGELOG.de.md) · [🇫🇷 Français](CHANGELOG.fr.md) · [🇪🇸 Español](CHANGELOG.es.md) · [🇧🇷 Português](CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](CHANGELOG.it.md) · [🇵🇱 Polski](CHANGELOG.pl.md) · [🇹🇷 Türkçe](CHANGELOG.tr.md) · [🇸🇦 العربية](CHANGELOG.ar.md) · [🇮🇷 فارسی](CHANGELOG.fa.md) · [🇮🇳 हिन्दी](CHANGELOG.hi.md) · [🇮🇩 Indonesia](CHANGELOG.id.md) · [🇻🇳 Tiếng Việt](CHANGELOG.vi.md) · [🇨🇳 简体中文](CHANGELOG.zh-CN.md) · [🇯🇵 日本語](CHANGELOG.ja.md) · [🇰🇷 한국어](CHANGELOG.ko.md)

### Эмуляция
- Новый нативный arm64-движок: Android 5.0–17 и GSI (8–17) работают прямо на телефоне; Android 2.3–4.4 — через QEMU.
- Поддержка 64-битных прошивок; 32-битные программы в них работают через qemu-arm.
- Полная поддержка Android 8, включая GSI.
- zygote и mediaserver падают намного реже (исправления binder, устранена блокировка system_server).
- `/system` доступен гостю только для чтения, предкомпилированный код фреймворка больше не теряется.
- Первая загрузка Dalvik-прошивок быстрее (Galaxy S III: 104 с → 59 с).
- Опция аппаратного 32-битного исполнения (AArch32) без транслятора, при отсутствии — QEMU.
- GLES 1/2/3 через SwiftShader.

### Прошивки и производители
- TouchWiz: `ro.tvout.enable=false`, `ro.SecEDS.enable=false` и патчи binder.
- Прошивки Qualcomm: удаляются `qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager`.
- Infinix (AOSP / XUI / XOS) и Tecno (AOSP / HiOS) определяются по версии Android.
- Sony: эмуляция светового индикатора.
- Исправления для MIUI, HTC Sense, TouchWiz, Xperia и AOSP. Проверено на Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17.

### Импорт
- Новые форматы: 7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`.

### Функции
- Интернет работает в большем числе прошивок.
- Исправлен звук; добавлены микрофон, камера, акселерометр и вибрация.
- Панель навигации: «Назад», «Домой» и «Недавние» работают во всех прошивках.
- ADB по локальной сети и встроенный ADB-терминал.
- Исправлено меню «три точки» в запущенной системе: скриншоты, запись экрана, VNC, SMS, режим полёта, установка APK и другое.
- Кнопку меню можно перемещать в запущенной системе.
- Опция скрытия светового индикатора: в умолчаниях для новых систем и для каждой системы.
