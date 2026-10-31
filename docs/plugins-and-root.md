# 插件与 ROOT

AndroPaw 的两层管理能力：**插件**（模块）与 **ROOT**（超级用户）。两者都按模拟器（镜像）为单位，
主页面上各占一个标签页，运行中的模拟器里也能用和 AgentPaw 一样的小窗实时调整。

## 为什么这样实现

AndroPaw 的 guest 不是整机虚拟化：guest 的二进制通过 `qemu-arm`（2.3–4.4）或 `libaemu64`
（5.0+）直接跑在宿主内核上，`/system`、`/data` 就是镜像目录里的普通文件。因此：

- 没有真实的 `mount`、没有 SELinux、没有 overlayfs，也没有独立的 `magiskd`；
- 宿主对 guest 的文件树有完全读写权限，所以"安装到模拟器里"就是**写进 guest 自己的目录树**；
- "root" 不是真的换 uid（宿主上并没有可换的 root），而是一道**闸门**：`su` 是否存在、是否放行。

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

`core/RootManager.kt` + `res/raw/apw_su.sh`

- **开关**：`启用模拟器 ROOT 权限`。开 → 写入 `su`（`/data/adb/andropaw/bin/su`，并同时放到
  `/system/bin`、`/system/xbin`、`/sbin`、`/data/adb/ksu/bin`）与 `enabled` 标记；关 → 全部移除。
- **闸门**：`su` 从 `/proc` 找到真正的调用进程（连续几层 shell 会往上追），按包名在
  `/data/adb/andropaw/policy` 里查 `allow` / `deny`。首次申请的包名写进 `requests.log`，并最多
  等 60 秒——页面或小窗在这期间点"允许"，申请会直接继续执行。
- **页面**：KernelSU 超级用户页的样子：一行一个应用（图标 / 名称 / 包名 · uid / 开关），点开可
  允许、拒绝或忘记。图标和名称由宿主用自己的 `PackageManager` 直接读 guest 里的 apk
  （`core/GuestApps.kt`），所以机器没开机也能显示。
- 关机状态、开机状态都生效：文件直接写在 guest 树里，下一进程启动立刻能看到新的 `su`/策略。

## 与真实实现的差别（重要）

- **ROOT 不是提权**：宿主上没有 root 可提，`su` 放行的是"能执行命令"，不是"uid 变 0"。
  同时导出 `AEMU64_ID=0,0` / `DHD_UID=0`，让关心 `id` 的程序看到一致的结果。
- **没有 overlayfs / magiskd**：模块的 `system/` 是复制进树的（可逆），不是挂载；因此模块间的
  加载顺序、magisk 的 `post-fs-data` 早期时机都简化了。
- **Xposed 的方法级 Hook** 需要 guest 里存在 `bridge.jar`（放到 `/data/adb/xposed/bridge.jar`）；
  没有它，宿主只负责模块安装、启用停用与 `xposed_init` 登记。
- **重启生效**：system 覆盖与 apk 安装都在开机流程里完成，装完插件后需要重新启动模拟器。
