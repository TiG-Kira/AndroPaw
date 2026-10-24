# AndroPaw 代码审计与修复记录

日期：2026-10-24。基线：`ffa68103a0a19cf2bb7a4f6ad7f9802da911b833`。

已执行 `git pull --ff-only`，远端返回 Already up to date；未提交或推送本轮修改。

## 审计范围与可信度

本轮覆盖应用配置、固件导入与恢复镜像、虚拟机管理、ADB/VNC、日志、更新、截图/录屏及 Agent 的源码检查，扫描 native 源码危险操作，并执行 Android 编译、JVM 回归测试和 Lint。重点检查了外部输入、文件边界、内存分配、取消与资源释放。

这不是全部代码逐行验证或安全认证：预编译 JNI/引擎二进制没有逆向审计，未运行 native sanitizer、固件格式模糊测试及所有 ROM/Android 版本兼容性矩阵。本机 ADB 服务启动失败，无法完成手机上的实际交互、冷启动、帧率、峰值堆内存、后台保活与 OOM 复现。性能结论是源码层面的风险判断，不包含未经测量的性能数据。

## 按严重程度列出的修复

| 优先级 | 技术缺陷 | 用户体验与风险 | 修复/新增实现 |
|---|---|---|---|
| P1 高 | Importer 使用字符串前缀判断目录归属；ext4、ramdisk、TAR 目录和硬链接部分分支缺少校验 | 恶意固件可通过 `../root-other` 等路径越界写入同一应用可访问的文件，破坏镜像或应用数据 | 新增 `SafePaths`，以 canonical 路径组件判断归属；统一校验文件、目录、硬链接源与目的、ramdisk、权限遍历；拒绝越界目标 |
| P1 高 | RecoveryImage 同样存在前缀校验绕过；绝对符号链接可指向宿主文件系统 | 导入恢复镜像可能越界写入；后续操作可能跟随链接访问宿主路径 | 使用统一路径校验；将 guest 绝对链接转换为树内相对链接，拒绝越界目标 |
| P1 高 | ADB 包长度、sync 参数长度和 DATA 长度未经验证即分配数组；连接、stream、接收队列无界 | 连接到已开启的调试端口即可触发异常分配、线程或内存耗尽 | 包体上限 64 KiB，sync 路径上限 1024 字节，DATA 上限 64 KiB；最多 8 个连接、每连接 8 个 stream、每 stream 4 个排队包；异常退出也清理 stream 并唤醒等待者 |
| P1 高 | ADB sync 路径虽处理了 `..`，但可能通过已有符号链接越过 guest 或共享卡目录 | push/pull 可能读写树外的应用可访问文件 | sync 使用 canonical 目录边界校验 |
| P1 高 | VNC 未校验客户端 bpp，连接数无界，握手可永久占用线程 | 非法像素格式导致编码异常；大量连接放大帧缓存内存占用 | 只接受 8/16/32 bpp，校验通道移位与范围；最多 4 个客户端，握手超时 30 秒 |
| P1 高 | GuestLog.contains 将整个增长中的日志读入堆 | 启动诊断可能因大型日志出现高内存占用或 OOM | 改成 64 KiB 字符缓冲区流式搜索，并保留跨块匹配语义 |
| P2 中 | 更新下载不响应协程取消、无完整长度检查、连接未可靠关闭、APK 写入外部目录 | 取消后仍继续下载；网络中断留下残缺包；安装失败且难以判断原因 | 私有 cache 目录、唯一 `.part` 文件、循环检查取消、finally 清理和断连；检查响应与发布资产长度、APK 格式和包名，通过后重命名为 APK |
| P2 中 | 更新自动重定向缺少逐跳控制，进度回调过于频繁 | 下载链路不透明；大量小块回调增加 UI 更新负担 | 每跳要求 HTTPS，最多 5 次跳转，支持相对 Location；进度更新限制为约每 100 ms 一次 |
| P2 中 | 更新失败后缺少直接重试入口、空间不足需下载到失败才知道 | 用户需重新进入设置并重复操作 | 新增错误弹窗“重试”；阻止重复下载，下载前按发布大小检查空间；成功后清理旧 APK；稳定更新检查跳过 prerelease |
| P2 中 | 截图成功/失败后 Bitmap 未主动释放，PixelCopy 同步异常未处理，压缩 false 被当作成功 | 连续截图增加内存压力；偶发截图失败或假成功 | 保存结束、PixelCopy 失败或抛异常时回收截图 Bitmap；检查 PNG 压缩结果 |
| P2 中 | 导入日志只限制展示 50 条，底层 ArrayList 仍无限增长 | 长导入持续积累日志对象 | 改成最多 50 条的 ArrayDeque |
| P2 中 | AboutBgEffect 在 API 31/32 创建 API 33 RuntimeShader；FirmwareFolder 无保护调用 API 30 方法 | Android 12/12L 关于页可能崩溃；Android 8/9/10 固件目录列表被异常吞掉后一直为空 | 着色器入口改为 API 33 并标记类 API 要求；全文件访问检查只在 API 30+ 执行 |
| P3 低 | Manifest 重复声明前台特殊服务权限 | 合并告警、配置维护噪声 | 删除重复声明 |

路径加固限制在解包与 sync 的具体文件访问入口；它不是整个 guest 的强安全沙箱，也没有解决运行中恶意进程同时改写链接的 TOCTOU 风险。

APK 长度、格式与包名检查不等于独立的密码学完整性证明；安装签名校验仍由 Android 安装器执行。

## 仍需优化或更新的模块

以下为真实遗留项或需进一步验证的风险，本轮没有声称修复。

| 优先级 | 模块与证据 | 用户影响 | 建议与完成条件 |
|---|---|---|---|
| P1 | `core/AdbServer.kt` 明确绑定 `0.0.0.0`，协议没有 AUTH；需要用户手动开启 | 开启后同网段可执行 guest shell、传文件或重启；不能把该入口当作已认证服务 | 增加授权配对/公钥认证或明确的本机与 LAN 两种模式，LAN 开启前展示访问范围，并做未授权连接拒绝测试 |
| P1 | `importer/BootImage.kt`、`Formats.kt`、`ErofsImage.kt` 等存在 `readBytes()` 和无界解压/分配 | 巨大或畸形固件仍可能 OOM；上述日志修复不能消除导入 OOM | 建立每格式的压缩输入、解压输出、字典、条目数和嵌套预算，优先流式或临时文件；增加畸形头、压缩炸弹、磁盘耗尽与取消测试 |
| P1 | guest/native 共享宿主应用权限与网络栈，包含可执行固件与预编译引擎 | 不可信固件的隔离强度不能等同系统虚拟机；单纯路径校验不足以建立安全边界 | 独立审计 syscall 路径映射、Binder/GL/网络 IPC；native fuzz + ASan/UBSan；明确支持的信任模型 |
| P2 | `agent/ChatSession.kt` 部分列表写入未统一加锁，persist 直接 writeText；切换会话可与生成交错 | 有并发修改、回复归属错误及写入中断丢失记录风险 | 会话状态统一交由单一 owner/Mutex；生成绑定会话 ID；原子写入及按需加载，增加切换/删除/生成并发测试 |
| P2 | `agent/LlmClient.kt` 非流式及错误响应整段 readText，读超时 300 秒 | 异常服务响应放大内存；停止生成可能等待网络读超时 | 限制响应和 SSE 行大小，使用可取消网络请求，增加断流、慢服务与取消延迟测试 |
| P2 | `agent/AgentStore.kt` API key 保存在普通私有 SharedPreferences；全局允许明文网络 | 私有存储并非加密存储；自定义 HTTP 端点可明文传输密钥/内容 | Keystore 加密并迁移旧 key；远程端点优先 HTTPS，保留 Ollama 本机 HTTP；增加迁移与异常恢复测试 |
| P2 | `core/TlsBridge.kt` start 直接返回 0，为空实现；README 宣称现代 TLS 代理 | 旧 guest 浏览器仍可能无法访问现代 HTTPS，用户易误以为代理已经启用 | 要么实现并验证 TLS 桥，要么在界面与文档明确功能未实现；不能仅凭开关存在判断支持 |
| P2 | `core/ScreenRecorder.kt` 编码器配置中途失败时未完整释放已创建资源，工作线程 Bitmap 未回收 | 多次启动失败可能占用 codec/Surface；录屏内存压力 | 明确各资源所有权，初始化失败反向释放；在真实设备验证编码器拒绝、旋转和后台切换 |
| P2 | `ui/LibraryModel.kt` 查询文件名仍在调用线程，取消与 importer 初始化之间存在窗口 | 慢 ContentProvider 可能短时卡 UI；极早取消可能未及时生效 | 查询移到 IO，取消状态覆盖整个操作，统一进度与完成状态；使用慢 Provider 做测试 |
| P2 | `core/ImageStore.kt` tmp.renameTo 返回值未检查 | 极端写入失败时保存结果不可见，用户可能以为设置已保存 | 使用 AtomicFile 或检查原子替换并向 UI 传播错误；覆盖失败与恢复测试 |
| P3 | `app/build.gradle.kts` Lint 设置 abortOnError=false；AGP 9.1.0 对 compileSdk 37 发出兼容性提示 | 构建成功不等于没有静态错误，依赖升级容易出现兼容问题 | 将一方代码的高风险 Lint 设为门禁，区分 vendored UI 基线；基于实际兼容矩阵更新构建工具，避免盲目升级 Compose/Miuix |
| P3 | `res/values*/strings.xml` 的多语言翻译不完整，Lint 有 65 个 MissingTranslation 条目 | Agent 与部分菜单回退到英文，语言体验不一致 | 按已声明支持语言补齐专业翻译，并在资源检查中建立门禁 |
| P3 | README 的版本/SDK 说明与当前 build.gradle.kts 不一致；targetSdk 28 是运行 guest 二进制的现有兼容约束 | 开发者按文档准备环境可能遇到构建问题；现代 Android 行为受旧 target 影响 | 同步构建文档；升级 target 前先设计可执行文件加载架构并做 ROM/系统矩阵验证 |

## 验证记录

- 新增 `SafePathsTest` 3 项：正常路径、穿越/同名前缀绕过、绝对 guest 链接转换与逃逸拒绝。
- 新增 `GuestLogTest` 2 项：跨 64 KiB 分界搜索、无日志处理；同时检查空查询与不存在关键词。
- 现有 ChatParser 6 项、RomMatrix 3 项，与新增项合计 14 项测试通过，0 失败、0 错误。
- 执行 `:app:testStandardDebugUnitTest`，源码与测试编译成功；测试报告位于 `app/build/reports/tests/testStandardDebugUnitTest/index.html`。
- `git diff --check` 使用 `core.whitespace=cr-at-eol`，保留原 MainActivity 的 CRLF，未发现内容空白错误。
- 执行 `:app:lintStandardDebug` 完成，任务成功不代表零问题：首轮报告为 108 Error、117 Warning、16 Hint；主要是缺失翻译 65 项、NewApi 41 项，另有可疑缩进及资源读取提示。已处理上述两处确认的 API 兼容缺陷与可疑缩进，其他 NewApi 条目需要结合调用方版本保护逐项甄别。最后一次完整 Lint 报告为 83 Error、118 Warning、16 Hint；剩余条目并未全部修复。报告位于 `app/build/reports/lint-results-standardDebug.html`。

## 建议的下一轮实机验收

固定同一设备/固件记录首次及二次启动耗时、导入耗时、PSS/Java/native 堆峰值、触摸响应和帧率；覆盖 2 GiB 以上固件、连续截图/录屏、网络中断、取消后立即重试、磁盘不足、前后台切换及不同 Android 版本。ADB/VNC 必须额外做异常报文与连接耗尽测试。

最终复核：最新源码 `:app:testStandardDebugUnitTest` BUILD SUCCESSFUL，14 项测试全部通过；`:app:lintStandardDebug` 任务完成。Lint 报告形成后，另对权限遍历跳过符号链接的收尾调整重新编译及运行了全部测试。
