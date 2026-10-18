# AEmulator 0.0.0.3

[🇬🇧 English](../../CHANGELOG.md) · [🇷🇺 Русский](CHANGELOG.ru.md) · [🇺🇦 Українська](CHANGELOG.uk.md) · [🇩🇪 Deutsch](CHANGELOG.de.md) · [🇫🇷 Français](CHANGELOG.fr.md) · [🇪🇸 Español](CHANGELOG.es.md) · [🇧🇷 Português](CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](CHANGELOG.it.md) · [🇵🇱 Polski](CHANGELOG.pl.md) · [🇹🇷 Türkçe](CHANGELOG.tr.md) · [🇸🇦 العربية](CHANGELOG.ar.md) · [🇮🇷 فارسی](CHANGELOG.fa.md) · [🇮🇳 हिन्दी](CHANGELOG.hi.md) · [🇮🇩 Indonesia](CHANGELOG.id.md) · [🇻🇳 Tiếng Việt](CHANGELOG.vi.md) · [🇨🇳 简体中文](CHANGELOG.zh-CN.md) · **🇯🇵 日本語** · [🇰🇷 한국어](CHANGELOG.ko.md)

### エミュレーション
- 新しいネイティブ arm64 エンジン：Android 5.0〜17 と GSI（8〜17）がスマホ上で直接動作。Android 2.3〜4.4 は QEMU で動作。
- 64 ビットファームウェアに対応。内部の 32 ビットプログラムは qemu-arm で動作。
- GSI を含む Android 8 に完全対応。
- zygote と mediaserver のクラッシュが大幅に減少（binder の修正、system_server のデッドロックを解消）。
- ゲストから `/system` は読み取り専用になり、プリコンパイル済みのフレームワークコードが失われなくなりました。
- Dalvik ファームウェアの初回起動を高速化（Galaxy S III：104 秒 → 59 秒）。
- トランスレーターを使わないハードウェア 32 ビット実行（AArch32）のオプション。非対応時は QEMU。
- SwiftShader による GLES 1/2/3。

### ファームウェアとメーカー
- TouchWiz：`ro.tvout.enable=false`, `ro.SecEDS.enable=false` と binder パッチ。
- Qualcomm ファームウェア：`qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager` を削除。
- Infinix（AOSP / XUI / XOS）と Tecno（AOSP / HiOS）を Android のバージョンで判別。
- Sony：通知 LED のエミュレーション。
- MIUI、HTC Sense、TouchWiz、Xperia、AOSP の修正。Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17 で動作確認。

### インポート
- 新しい形式：7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`。

### 機能
- より多くのファームウェアでインターネットが使えます。
- サウンドを修正。マイク、カメラ、加速度センサー、バイブレーションを追加。
- ナビゲーションバー：戻る、ホーム、履歴がすべてのファームウェアで動作。
- ローカルネットワーク経由の ADB と内蔵 ADB ターミナル。
- 起動中のシステムの三点メニューを修正：スクリーンショット、画面録画、VNC、SMS、機内モード、APK のインストールなど。
- 起動中のシステムでメニューボタンを移動できます。
- LED インジケーターを非表示にするオプション：新しいシステムの既定値と各システムの設定に。
