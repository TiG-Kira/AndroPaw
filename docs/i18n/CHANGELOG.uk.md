# AEmulator 0.0.0.3

[🇬🇧 English](../../CHANGELOG.md) · [🇷🇺 Русский](CHANGELOG.ru.md) · **🇺🇦 Українська** · [🇩🇪 Deutsch](CHANGELOG.de.md) · [🇫🇷 Français](CHANGELOG.fr.md) · [🇪🇸 Español](CHANGELOG.es.md) · [🇧🇷 Português](CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](CHANGELOG.it.md) · [🇵🇱 Polski](CHANGELOG.pl.md) · [🇹🇷 Türkçe](CHANGELOG.tr.md) · [🇸🇦 العربية](CHANGELOG.ar.md) · [🇮🇷 فارسی](CHANGELOG.fa.md) · [🇮🇳 हिन्दी](CHANGELOG.hi.md) · [🇮🇩 Indonesia](CHANGELOG.id.md) · [🇻🇳 Tiếng Việt](CHANGELOG.vi.md) · [🇨🇳 简体中文](CHANGELOG.zh-CN.md) · [🇯🇵 日本語](CHANGELOG.ja.md) · [🇰🇷 한국어](CHANGELOG.ko.md)

### Емуляція
- Новий нативний arm64-рушій: Android 5.0–17 і GSI (8–17) працюють прямо на телефоні; Android 2.3–4.4 — через QEMU.
- Підтримка 64-бітних прошивок; 32-бітні програми в них працюють через qemu-arm.
- Повна підтримка Android 8, включно з GSI.
- zygote і mediaserver падають значно рідше (виправлення binder, усунено блокування system_server).
- `/system` доступний гостю лише для читання, попередньо скомпільований код фреймворку більше не втрачається.
- Перше завантаження Dalvik-прошивок швидше (Galaxy S III: 104 с → 59 с).
- Опція апаратного 32-бітного виконання (AArch32) без транслятора, за відсутності — QEMU.
- GLES 1/2/3 через SwiftShader.

### Прошивки та виробники
- TouchWiz: `ro.tvout.enable=false`, `ro.SecEDS.enable=false` і патчі binder.
- Прошивки Qualcomm: видаляються `qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager`.
- Infinix (AOSP / XUI / XOS) і Tecno (AOSP / HiOS) визначаються за версією Android.
- Sony: емуляція світлового індикатора.
- Виправлення для MIUI, HTC Sense, TouchWiz, Xperia і AOSP. Перевірено на Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17.

### Імпорт
- Нові формати: 7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`.

### Функції
- Інтернет працює в більшій кількості прошивок.
- Виправлено звук; додано мікрофон, камеру, акселерометр і вібрацію.
- Панель навігації: «Назад», «Додому» і «Останні» працюють у всіх прошивках.
- ADB у локальній мережі та вбудований ADB-термінал.
- Виправлено меню «три крапки» у запущеній системі: знімки екрана, запис екрана, VNC, SMS, режим польоту, встановлення APK тощо.
- Кнопку меню можна переміщати в запущеній системі.
- Опція приховування світлового індикатора: у типових налаштуваннях нових систем і для кожної системи.
