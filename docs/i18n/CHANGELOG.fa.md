# AEmulator 0.0.0.3

[🇬🇧 English](../../CHANGELOG.md) · [🇷🇺 Русский](CHANGELOG.ru.md) · [🇺🇦 Українська](CHANGELOG.uk.md) · [🇩🇪 Deutsch](CHANGELOG.de.md) · [🇫🇷 Français](CHANGELOG.fr.md) · [🇪🇸 Español](CHANGELOG.es.md) · [🇧🇷 Português](CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](CHANGELOG.it.md) · [🇵🇱 Polski](CHANGELOG.pl.md) · [🇹🇷 Türkçe](CHANGELOG.tr.md) · [🇸🇦 العربية](CHANGELOG.ar.md) · **🇮🇷 فارسی** · [🇮🇳 हिन्दी](CHANGELOG.hi.md) · [🇮🇩 Indonesia](CHANGELOG.id.md) · [🇻🇳 Tiếng Việt](CHANGELOG.vi.md) · [🇨🇳 简体中文](CHANGELOG.zh-CN.md) · [🇯🇵 日本語](CHANGELOG.ja.md) · [🇰🇷 한국어](CHANGELOG.ko.md)

<div dir="rtl">

### شبیه‌سازی
- موتور بومی جدید arm64: اندروید 5.0 تا 17 و GSIها (8 تا 17) مستقیم روی گوشی اجرا می‌شوند؛ اندروید 2.3 تا 4.4 از طریق QEMU.
- پشتیبانی از فرم‌ویرهای 64 بیتی؛ برنامه‌های 32 بیتی درون آن‌ها با qemu-arm اجرا می‌شوند.
- پشتیبانی کامل از اندروید 8، از جمله GSIها.
- zygote و mediaserver بسیار کمتر از کار می‌افتند (اصلاحات binder، رفع قفل‌شدن system_server).
- `/system` برای مهمان فقط‌خواندنی است و کد ازپیش‌کامپایل‌شده فریم‌ورک دیگر از دست نمی‌رود.
- بوت اول سریع‌تر فرم‌ویرهای Dalvik ‏(Galaxy S III: ‏104 ثانیه ← 59 ثانیه).
- گزینه اجرای سخت‌افزاری 32 بیتی (AArch32) بدون مترجم، با QEMU به‌عنوان جایگزین.
- GLES 1/2/3 از طریق SwiftShader.

### فرم‌ویرها و سازندگان
- TouchWiz: ‏`ro.tvout.enable=false`, `ro.SecEDS.enable=false` و وصله‌های binder.
- فرم‌ویرهای Qualcomm: حذف `qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager`.
- Infinix ‏(AOSP / XUI / XOS) و Tecno ‏(AOSP / HiOS) بر اساس نسخه اندروید شناسایی می‌شوند.
- Sony: شبیه‌سازی چراغ اعلان.
- اصلاحات برای MIUI، HTC Sense، TouchWiz، Xperia و AOSP. آزمایش‌شده روی Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17.

### وارد کردن
- قالب‌های جدید: 7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`.

### قابلیت‌ها
- اینترنت در فرم‌ویرهای بیشتری کار می‌کند.
- صدا اصلاح شد؛ میکروفون، دوربین، شتاب‌سنج و لرزش اضافه شد.
- نوار پیمایش: برگشت، خانه و اخیر در همه فرم‌ویرها کار می‌کنند.
- ADB روی شبکه محلی و ترمینال ADB داخلی.
- منوی سه‌نقطه در سیستم در حال اجرا اصلاح شد: اسکرین‌شات، ضبط صفحه، VNC، پیامک، حالت پرواز، نصب APK و موارد دیگر.
- دکمه منو در سیستم در حال اجرا جابه‌جا می‌شود.
- گزینه پنهان کردن نشانگر LED: در پیش‌فرض‌های سیستم‌های جدید و برای هر سیستم.

</div>
