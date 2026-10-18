# AEmulator 0.0.0.3

[🇬🇧 English](../../CHANGELOG.md) · [🇷🇺 Русский](CHANGELOG.ru.md) · [🇺🇦 Українська](CHANGELOG.uk.md) · [🇩🇪 Deutsch](CHANGELOG.de.md) · [🇫🇷 Français](CHANGELOG.fr.md) · [🇪🇸 Español](CHANGELOG.es.md) · [🇧🇷 Português](CHANGELOG.pt-BR.md) · [🇮🇹 Italiano](CHANGELOG.it.md) · [🇵🇱 Polski](CHANGELOG.pl.md) · [🇹🇷 Türkçe](CHANGELOG.tr.md) · [🇸🇦 العربية](CHANGELOG.ar.md) · [🇮🇷 فارسی](CHANGELOG.fa.md) · [🇮🇳 हिन्दी](CHANGELOG.hi.md) · [🇮🇩 Indonesia](CHANGELOG.id.md) · [🇻🇳 Tiếng Việt](CHANGELOG.vi.md) · [🇨🇳 简体中文](CHANGELOG.zh-CN.md) · [🇯🇵 日本語](CHANGELOG.ja.md) · **🇰🇷 한국어**

### 에뮬레이션
- 새 네이티브 arm64 엔진: Android 5.0–17과 GSI(8–17)가 휴대폰에서 직접 실행되며, Android 2.3–4.4는 QEMU로 실행됩니다.
- 64비트 펌웨어 지원. 그 안의 32비트 프로그램은 qemu-arm으로 실행됩니다.
- GSI를 포함한 Android 8 완전 지원.
- zygote와 mediaserver 충돌이 크게 줄었습니다(binder 수정, system_server 교착 해결).
- 게스트에서 `/system`은 읽기 전용이 되어 미리 컴파일된 프레임워크 코드가 더 이상 사라지지 않습니다.
- Dalvik 펌웨어 첫 부팅이 빨라졌습니다(Galaxy S III: 104초 → 59초).
- 번역기 없이 하드웨어 32비트(AArch32)로 실행하는 옵션, 미지원 시 QEMU 사용.
- SwiftShader를 통한 GLES 1/2/3.

### 펌웨어 및 제조사
- TouchWiz: `ro.tvout.enable=false`, `ro.SecEDS.enable=false` 및 binder 패치.
- Qualcomm 펌웨어: `qti_permissions.xml`, `com.qti.dpmframework.xml`, `com.qualcomm.location`, `qcrilmsgtunnel`, `dpmserviceapp`, `DiracAudioControlService`, `DiracManager` 제거.
- Infinix(AOSP / XUI / XOS)와 Tecno(AOSP / HiOS)를 Android 버전으로 인식.
- Sony: 알림 LED 에뮬레이션.
- MIUI, HTC Sense, TouchWiz, Xperia, AOSP 수정. Nexus 4, Galaxy Nexus, Galaxy S II/S III, Redmi 1S, Desire HD, One M7, Nexus 5X, Phh 9, AOSP GSI 17에서 테스트.

### 가져오기
- 새 형식: 7z, RAR, `.pac`, yaffs2, rfs, EROFS, `super.img`, APEX, `.tar.gz`.

### 기능
- 더 많은 펌웨어에서 인터넷이 작동합니다.
- 사운드 수정, 마이크·카메라·가속도계·진동 추가.
- 내비게이션 바: 뒤로, 홈, 최근 앱이 모든 펌웨어에서 작동.
- 로컬 네트워크 ADB와 내장 ADB 터미널.
- 실행 중인 시스템의 점 세 개 메뉴 수정: 스크린샷, 화면 녹화, VNC, SMS, 비행기 모드, APK 설치 등.
- 실행 중인 시스템에서 메뉴 버튼을 옮길 수 있습니다.
- LED 표시등 숨기기 옵션: 새 시스템 기본값과 시스템별 설정.
