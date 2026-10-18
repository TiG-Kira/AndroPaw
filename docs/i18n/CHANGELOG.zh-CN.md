# AEmulator 0.0.0.3

[🇬🇧 English](../../CHANGELOG.md) · [🇷🇺 Русский](CHANGELOG.ru.md) · [🇺🇦 Українська](CHANGELOG.uk.md) · [🇩🇪 Deutsch](CHANGELOG.de.md) · [🇫🇷 Français](CHANGELOG.fr.md) · [🇪🇸 Español](CHANGELOG.es.md) · [🇧🇷 Português](CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](CHANGELOG.it.md) · [🇵🇱 Polski](CHANGELOG.pl.md) · [🇹🇷 Türkçe](CHANGELOG.tr.md) · [🇸🇦 العربية](CHANGELOG.ar.md) · [🇮🇷 فارسی](CHANGELOG.fa.md) · [🇮🇳 हिन्दी](CHANGELOG.hi.md) · [🇮🇩 Indonesia](CHANGELOG.id.md) · [🇻🇳 Tiếng Việt](CHANGELOG.vi.md) · **🇨🇳 简体中文** · [🇯🇵 日本語](CHANGELOG.ja.md) · [🇰🇷 한국어](CHANGELOG.ko.md)

### 模拟
- 全新原生 arm64 引擎：Android 5.0–17 与 GSI（8–17）直接在手机上运行；Android 2.3–4.4 通过 QEMU 运行。
- 支持 64 位固件；其中的 32 位程序通过 qemu-arm 运行。
- 完整支持 Android 8，包括 GSI。
- zygote 和 mediaserver 崩溃大幅减少（binder 修复，system_server 死锁已修复）。
- `/system` 对客户机只读，预编译的框架代码不再丢失。
- Dalvik 固件首次启动更快（Galaxy S III：104 秒 → 59 秒）。
- 新增无需翻译器的硬件 32 位执行（AArch32）选项，不支持时回退到 QEMU。
- 通过 SwiftShader 支持 GLES 1/2/3。

### 固件与厂商
- TouchWiz：`ro.tvout.enable=false`, `ro.SecEDS.enable=false`，以及 binder 补丁。
- Qualcomm 固件：移除 `qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager`。
- 按 Android 版本识别 Infinix（AOSP / XUI / XOS）和 Tecno（AOSP / HiOS）。
- Sony：模拟通知指示灯。
- 修复 MIUI、HTC Sense、TouchWiz、Xperia 与 AOSP。已在 Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17 上测试。

### 导入
- 新格式：7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`。

### 功能
- 更多固件可以联网。
- 修复声音；新增麦克风、相机、加速度计和振动。
- 导航栏：返回、主页和最近任务在所有固件中可用。
- 局域网 ADB 与内置 ADB 终端。
- 修复运行中系统的三点菜单：截图、录屏、VNC、短信、飞行模式、安装 APK 等。
- 运行中可以移动菜单按钮。
- 可隐藏 LED 指示灯：在新系统默认设置和每个系统的设置中。
