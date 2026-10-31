# 插件与 ROOT

AndroPaw 的两层管理能力：**插件**（模块）与 **ROOT**（超级用户）。两者都按模拟器（镜像）为单位，
主页面上各占一个标签页，运行中的模拟器里也能用和 AgentPaw 一样的小窗实时调整。

## 为什么这样实现

AndroPaw 的 guest 不是整机虚拟化：guest 的二进制通过 `qemu-arm`（2.3–4.4）或 `libaemu64`
（5.0+）直接跑在宿主内核上，`/system`、`/data` 就是镜像目录里的普通文件。因此：

- 没有真实的 `mount`、没有 SELinux、没有 overlayfs，也没有独立的 `magiskd`；
- 宿主对 guest 的文件树有完全读写权限，所以"安装到模拟器里"就是**写进 guest 自己的目录树**；
- "root" 给的是 guest 自己这一套（进程身份 / 文件系统 / 属性 / `su`），不是宿主内核的 root。

这两点决定了下面所有实现，也决定了能力的边界（见文末）。

## 后端选择

| Android 版本 | API | 后端 | 模块目录 |
| --- | --- | --- | --- |
| 4.x – 7.x | < 26 | Xposed | `/data/adb/xposed/<id>` |
| 8.0 – 12 | 26–31 | Magisk | `/data/adb/modules/<id>` |
| 12L+ | ≥ 32 | KernelSU | `/data/adb/modules/<id>` |

后端由镜像的 API 决定（`PluginBackend.forApi`），页面顶部显示当前后端。三者共用同一套
`module.prop`、`disable`、`remove` 标记，和真实的 Magisk / KernelSU 布局一致。

每个镜像还会拿到兼容命令行工具（写进 guest）：

- `/data/adb/andropaw/bin/ksud`、`/data/adb/ksu/bin/ksud` — `ksud module list|install|uninstall|enable|disable`
- `/data/adb/andropaw/bin/magisk`、`/data/adb/magisk/magisk` — `magisk --list|--install-module|--remove-module`
- `/data/adb/andropaw/bin/xposed` — `xposed list|enable|disable|bridge`

## 插件

`core/PluginManager.kt`

- **安装**：`module.prop` 的 zip，或一个 Xposed / 插件 apk。解包进 guest 的模块目录，写 `apw.json`
  （安装时间 / 后端 / apk 名）。带 `module.apk` 的模块会把这个 apk 一并装进 guest：机器在运行就
  立刻 `pm install`，没运行就标记为待装。
- **system 覆盖**：真实 Magisk 靠 overlayfs 把 `system/` 挂上去；这里没有 overlayfs，所以开机时把
  模块的 `system/` 逐文件复制进 guest 树。被覆盖的原文件先备份到模块自己的 `.apw-backup/`，
  复制清单记在 `.apw-overlay`——停用/卸载时据此还原，这是"systemless"能成立的关键。
- **开机脚本**：`post-fs-data.sh` 同步执行、`service.sh` 后台执行，由 `/data/adb/andropaw/bin/boot`
  驱动；guest 起来后由 `Management.onBoot` 调起，输出落在 `/data/adb/andropaw/boot.log`。
- **详情 / 启用停用 / 卸载 / action.sh**：对应 KernelSU 模块页的全部操作。
- **Xposed 注入点**：`PluginManager.xposedClasspath` 把 `bridge.jar` 与所有已启用模块的 apk 拼成
  classpath，`GuestVm.startService` 在启动 `zygote` 时注入 `CLASSPATH`。这样 zygote 及其 fork 出来
  的每个 app 进程都能加载模块的 `xposed_init` 入口。方法级 Hook 由 `bridge.jar`（Xposed 桥本身）
  提供，没有桥时只做安装与入口登记，不做注入。

## ROOT

`core/RootManager.kt` + `core/RootBackend.kt` + `res/raw/apw_su.sh`

ROOT 就是提权：开关打开后，这台模拟器在 guest 自己看来就是一台已 ROOT 的设备。guest 跑在宿主
内核上，所以"提权"落在四层上，缺一层都不完整：

1. **进程身份** —— init 起的服务本来就以 root 运行（`InitPlan` 对 uid=0 的服务不传 `DHD_UID`）；
   `su` 拉起的进程再导出 `AEMU64_ID=0,0` / `DHD_UID=0` / `DHD_GID=0`，从这一刻起它和它 exec
   出来的东西在 guest 里 `getuid()`、`id`、`/proc/self/status` 全都是 0。
2. **文件系统** —— `AEMU_RO_SYSTEM=0`（`GuestRunner.rootEnabled`）：`/system` 从只读变可写，
   `mount -o rw,remount /system`、往 `/system/app` 里装东西都真的生效。
3. **属性** —— `ro.secure=0`、`ro.adb.secure=0`、`ro.debuggable=1`、`ro.build.tags=test-keys`、
   `service.adb.root=1` 合进属性区（`RootManager.props` → `GuestVm` 的 overrides）：adb 以 root
   起来，root 检测看到的是它期待的、可以持有 root 的设备。
4. **su 与那一代的产物** —— 按 API 分四代（`RootBackend.forApi`），每代都把它那一套布局摆在
   guest 里，模块和 root 检测找什么就有什么：

| Android 版本 | API | 后端 | 写进 guest 的东西 |
| --- | --- | --- | --- |
| 2.x – 3.x | < 14 | Superuser | `su` 于 `/system/xbin`、`/system/bin`、`/system/sbin`、`/sbin` |
| 4.x – 7.x | 14–25 | SuperSU | 上面全部 + `/su/bin/su`、`daemonsu`、`supolicy`、`install-recovery.sh` |
| 8.0 – 11 | 26–30 | Magisk | `/debug_ramdisk/su`、`/data/adb/magisk/{magisk,magiskd,magiskpolicy}`、`/sbin/magiskd` |
| 12+ | ≥ 31 | KernelSU | `ksud`（含 `ksud daemon`）、`/data/adb/ksu/bin/su`、`/data/adb/ksu/modules` |

- **su 怎么答复**：guest 只负责发问。`apw_su.sh` 把请求写进 `/data/adb/andropaw/ask/<pid>`，宿主
  （`RootManager.watch`，60 ms 轮询）从 `/proc/<pid>` 往上跳过 shell 层找到真正调用它的包名，记进
  `root.json`，再写 `answer/<pid>`。开关就是策略：开着答 `allow`，只有在这个页面上被单独收回过的
  包才 `deny`。宿主不在（机器在别的进程里跑、或已经停了）时按"已 ROOT"放行。
- **守护进程**：Magisk 有 `magiskd`、KernelSU 有 `ksud daemon`、SuperSU 有 `daemonsu`，都由
  `/data/adb/andropaw/bin/boot` 在开机后拉起（`res/raw/apw_rootd.sh`），`ps` 里能看到那一代该有的
  进程。2.x–3.x 没有守护进程的概念，`su` 二进制加 `ro.secure=0` 就是全部。
- **页面**：KernelSU 超级用户页的样子：一行一个应用（图标 / 名称 / 包名 · uid / 开关），点开可
  允许、拒绝或忘记。图标和名称由宿主用自己的 `PackageManager` 直接读 guest 里的 apk
  （`core/GuestApps.kt`），所以机器没开机也能显示。
- 关机状态、开机状态都生效：文件直接写在 guest 树里，下一进程启动立刻能看到新的 `su`/属性。

## 与真实实现的差别（重要）

- **提权的边界**：guest 与宿主共内核，"root"给的是 guest 自己这一套（init / framework / app /
  `id` / 文件权限）全按 root 呈现、`/system` 真的可写；宿主自身的权限边界不变——宿主没有 root
  可提，也不需要在有 root 的手机上才能跑。
- **没有 SELinux**：`supolicy` 是空操作（没有策略可打补丁），因此 `supolicy --live` 之类不会真的
  改到策略，也不会因此失败。
- **KernelSU 没有内核模块**：真实 KernelSU 是 GKI 内核模块，这里用 `ksud` + `su` + `/data/adb/ksu`
  在用户态复刻它的目录与命令；`ksud module install` 会提示改用 AndroPaw 页面安装。
- **没有 overlayfs**：模块的 `system/` 是复制进树的（可逆），不是挂载；因此模块间的加载顺序、
  magisk 的 `post-fs-data` 早期时机都简化了。
- **Xposed 的方法级 Hook** 需要 guest 里存在 `bridge.jar`（放到 `/data/adb/xposed/bridge.jar`）；
  没有它，宿主只负责模块安装、启用停用与 `xposed_init` 登记。
- **重启生效**：system 覆盖、属性与 apk 安装都在开机流程里完成，改完这些需要重新启动模拟器。
