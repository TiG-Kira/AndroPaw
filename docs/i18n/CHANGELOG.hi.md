# AEmulator 0.0.0.3

[🇬🇧 English](../../CHANGELOG.md) · [🇷🇺 Русский](CHANGELOG.ru.md) · [🇺🇦 Українська](CHANGELOG.uk.md) · [🇩🇪 Deutsch](CHANGELOG.de.md) · [🇫🇷 Français](CHANGELOG.fr.md) · [🇪🇸 Español](CHANGELOG.es.md) · [🇧🇷 Português](CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](CHANGELOG.it.md) · [🇵🇱 Polski](CHANGELOG.pl.md) · [🇹🇷 Türkçe](CHANGELOG.tr.md) · [🇸🇦 العربية](CHANGELOG.ar.md) · [🇮🇷 فارسی](CHANGELOG.fa.md) · **🇮🇳 हिन्दी** · [🇮🇩 Indonesia](CHANGELOG.id.md) · [🇻🇳 Tiếng Việt](CHANGELOG.vi.md) · [🇨🇳 简体中文](CHANGELOG.zh-CN.md) · [🇯🇵 日本語](CHANGELOG.ja.md) · [🇰🇷 한국어](CHANGELOG.ko.md)

### एमुलेशन
- नया नेटिव arm64 इंजन: Android 5.0–17 और GSI (8–17) सीधे फ़ोन पर चलते हैं; Android 2.3–4.4 QEMU से चलता है।
- 64-बिट फ़र्मवेयर का समर्थन; उनके अंदर के 32-बिट प्रोग्राम qemu-arm से चलते हैं।
- GSI सहित Android 8 का पूरा समर्थन।
- zygote और mediaserver अब बहुत कम क्रैश होते हैं (binder सुधार, system_server का अटकना ठीक)।
- `/system` गेस्ट के लिए केवल-पढ़ने योग्य है, इसलिए फ़्रेमवर्क का पहले से कंपाइल किया कोड अब नहीं खोता।
- Dalvik फ़र्मवेयर का पहला बूट तेज़ (Galaxy S III: 104 से → 59 से)।
- बिना ट्रांसलेटर के हार्डवेयर 32-बिट (AArch32) चलाने का विकल्प, न होने पर QEMU।
- SwiftShader के ज़रिए GLES 1/2/3।

### फ़र्मवेयर और निर्माता
- TouchWiz: `ro.tvout.enable=false`, `ro.SecEDS.enable=false` और binder पैच।
- Qualcomm फ़र्मवेयर: `qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager` हटाए जाते हैं।
- Infinix (AOSP / XUI / XOS) और Tecno (AOSP / HiOS) Android संस्करण से पहचाने जाते हैं।
- Sony: नोटिफ़िकेशन LED का एमुलेशन।
- MIUI, HTC Sense, TouchWiz, Xperia और AOSP के लिए सुधार। इन पर परीक्षण: Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17।

### इम्पोर्ट
- नए फ़ॉर्मैट: 7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`।

### सुविधाएँ
- अब ज़्यादा फ़र्मवेयर में इंटरनेट चलता है।
- ध्वनि ठीक; माइक्रोफ़ोन, कैमरा, एक्सेलेरोमीटर और वाइब्रेशन जोड़े गए।
- नेविगेशन बार: बैक, होम और हाल के ऐप हर फ़र्मवेयर में काम करते हैं।
- लोकल नेटवर्क पर ADB और बिल्ट-इन ADB टर्मिनल।
- चल रहे सिस्टम का तीन-बिंदु मेनू ठीक: स्क्रीनशॉट, स्क्रीन रिकॉर्डिंग, VNC, SMS, एयरप्लेन मोड, APK इंस्टॉल और बहुत कुछ।
- चल रहे सिस्टम में मेनू बटन को खिसकाया जा सकता है।
- LED संकेतक छिपाने का विकल्प: नए सिस्टम की डिफ़ॉल्ट सेटिंग में और हर सिस्टम के लिए।
