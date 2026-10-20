<div dir="rtl">

<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AndroPaw logo"/>

# AndroPaw

*Based on AEmulator and the original [SENSES emulator](https://github.com/Istratii-tech/SENSES)*

**شغّل برامج أندرويد الكلاسيكية — HTC Sense وTouchWiz وMIUI وAOSP — على هاتف حديث. بلا روت وبلا حاسوب.**

[![Version](https://img.shields.io/badge/version-0.2.0-3D5AFE?style=for-the-badge)](https://github.com/TiG-Kira/AndroPaw/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-3D5AFE?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/TiG-Kira/AndroPaw) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aemulatorofficial) [![Website](https://img.shields.io/badge/site-aemulator.gt.tc-111?style=for-the-badge)](https://aemulator.gt.tc)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · **🇸🇦 العربية** · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

يقلع AndroPaw نظام أندرويد 2.3–17 حقيقيًا مباشرة من ملف البرنامج الثابت: ملف ZIP للريكفري أو أرشيف Odin أو صورة مصنع Google أو GSI. تعمل البرامج الثابتة القديمة بمعمارية 32 بت (2.3–4.4) عبر QEMU معدّل، ويعمل أندرويد 5.0–17 مباشرة على أنوية arm64 في الهاتف؛ ويُحاكى binder النواة، وتُرسم الواجهة بمعالج رسومات الهاتف ويمر الصوت عبر نظام صوت أندرويد — كل ذلك داخل تطبيق عادي.

## ✨ المزايا

- استيراد كل الصيغ تقريبًا: ‏ZIP لـ CWM/TWRP، و‏Odin ‏`.tar.md5` من سامسونج، وصورة مصنع Google ‏`.tgz`، و`system.img`، وOTA ‏`system.new.dat.br`
- واجهات الشركات تعمل كما هي: HTC Sense وSamsung TouchWiz وMIUI وAOSP
- رسوميات عتادية عبر جسر GL، وصوت، ولمس ولمس متعدد، وشبكة مع وكيل TLS حديث
- مجلد بطاقة ذاكرة مشترك لملفات APK والموسيقى والصور
- واجهة Material 3 Expressive مع بطاقات بنمط Ki، بـ 18 لغة
- مجاني ومفتوح المصدر (GPL-3.0)
- AgentPaw — مساعد ذكاء اصطناعي مدمج يتصل بالجهاز قيد التشغيل ويمكنه استيراد البرنامج الثابت أو تغيير الإعدادات أو استعادة نظام متجمد (أي نقطة نهاية متوافقة مع OpenAI)

## 📱 قائمة البرامج الثابتة

قائمة البرامج الثابتة المُختبرة — مع الحالة والملاحظات وروابط التنزيل — موجودة في منتدانا. شارك نتائجك هناك واطرح أسئلتك واعثر على صور جديدة.

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/%D8%A7%D9%81%D8%AA%D8%AD%20%D8%A7%D9%84%D9%85%D9%86%D8%AA%D8%AF%D9%89-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="المنتدى"/></a>

</div>

## 🚀 البدء السريع

1. نزّل ملف APK من [Releases](https://github.com/TiG-Kira/AndroPaw/releases) وثبّته.
2. ابحث عن برنامج ثابت في [المنتدى](https://aeforum.uxazuu.space/) ونزّله إلى هاتفك.
3. افتح AndroPaw ← **إضافة برنامج ثابت** واختر الملف. يستغرق الاستيراد بضع دقائق.
4. اضغط **تشغيل**. الإقلاع الأول أبطأ: النظام يحسّن التطبيقات.
5. القائمة ⋮ للصوت وزر التشغيل والسجل؛ وزر ⚙️ للإعدادات واللغة.

## 📋 المتطلبات

- أندرويد 8.0+ على هاتف ARM بمعمارية 64 بت (arm64-v8a)
- نحو 1–3 غيغابايت لكل برنامج ثابت
- يُنصح بمعالج Snapdragon / Dimensity / Tensor حديث

## ⚙️ كيف يعمل

في أندرويد 2.3–4.4 تعمل كل عملية ضيف تحت QEMU معدّل بوضع المستخدم؛ وفي 5.0–17 تعمل برامج arm64 الخاصة بالبرنامج الثابت مباشرة، وتلتقط نواة محرك مستقلة لكل إصدار أندرويد استدعاءات النظام. يحل خادم binder محل برنامج تشغيل النواة، وينقل جسر GL استدعاءات OpenGL ES إلى معالج الرسومات، وتكيّف مكتبات صغيرة (HAL للصوت، وغلاف audio policy، وطبقة LD_PRELOAD) شفرة الشركات مع المحاكي. يقرأ المستورد البرنامج الثابت ويجد سكربتات init في صورة boot ويبني خطة تشغيل الخدمات.

## 🛠️ البناء من المصدر

تحتاج JDK 17 وAndroid SDK 36 وNDK r28. تُبنى مكتبات الضيف عبر `native/*/build.sh`.

```bash
git clone https://github.com/TiG-Kira/AndroPaw.git
cd AndroPaw
./gradlew assembleRelease
```

## 🙏 شكر وتقدير

نشأ AndroPaw من محاكيات HTC Desire HD وHTC One M7 التي صنعها [المطوّر الأصلي](https://t.me/istratii_tech1)، ولولا محركه ما وُجد هذا المشروع.

## 💙 ادعم المشروع

إن أعاد إليك AndroPaw هاتفًا أحببته، يمكنك دعم التطوير:

- 💳 [dalink.to/uxazu](https://dalink.to/uxazu)

## 🔗 روابط

- 🌐 الموقع: [aemulator.gt.tc](https://aemulator.gt.tc)
- 💬 المنتدى: [aeforum.uxazuu.space](https://aeforum.uxazuu.space/)
- 📣 قناة تيليجرام: [@aemulatorofficial](https://t.me/aemulatorofficial)
- 👤 المطوّر: [TiG-Kira](https://github.com/TiG-Kira)
- 🧬 المطوّر الأصلي: [t.me/istratii_tech1](https://t.me/istratii_tech1)

## 📄 الترخيص

GPL-3.0. أندرويد والعلامات التجارية والبرامج الثابتة ملك لأصحابها.

</div>
