# zygote 退出码 137 排查与尝试修复

## 结论与证据范围

用户报告：各固件版本、各宿主机型均可能出现，启动、前台使用、后台返回均可能随机触发。

137 对应常见的 `128 + SIGKILL(9)` 退出表示。本仓库 native 引擎也使用 `128 + sig` 退出编码。仅凭这个数字不能认定是 OOM：宿主 LMK/电量管理、guest system_server 死亡后主动结束 zygote、宿主管理器主动重启，均需结合现场日志区分。本轮没有连接的 Android 设备（`adb devices` 为空），没有现场崩溃日志；以下是代码确认的缺陷与修复，不是对用户现场杀进程者的最终归因。

## 确认的问题与实现

1. **启动重试破坏依赖**：旧 watchdog 在启动期 zygote 137 后调用 `killGuestPrograms()`，结束所有 guest 服务并清空监控表，但只重新 spawn zygote。binder/SurfaceFlinger/netd 等服务不完整会阻止 system_server 恢复，产生黑屏或后续退出。改为保留其余核心服务，通过 `restartZygote()` 重建 framework，清理旧应用和 zygote/USAP socket。
2. **主副 zygote 混用恢复策略**：原先 zygote_secondary 死亡也会触发主 zygote 的重建及整套 guest 清理。现在副 zygote 只在主 zygote 存活时单独重试，最多三次，避免 32 位兼容问题拖垮主 framework。
3. **退出事件重复/新进程丢失**：旧代码按服务名查死进程，异步重建与末尾 `procs.remove(name)` 可交错，移除刚启动的新实例；未删除的旧死进程又可能被反复计数。新增 `GuestProcesses.takeExited()`，在锁内只取出一次已退出实例；若该名称已经是活进程则不处理，恢复后不再按名字无条件删除。
4. **快速恢复绕过失败计数**：旧快速重建分支在记录崩溃次数前 `continue`，使失败限频可能失效，且恢复成功只靠等待三秒就宣告。移除此分支，运行期崩溃统一经过原有时间窗口计数；正常启动完成属性才表示恢复完成。
5. **即时 PID 获取失效**：旧 spawn 使用反射 `Process.pid()` 并强转 Int；Android 上该公共 API 可能不存在，在返回 Long 的实现上也会转换失败。统一使用数字类型转换和兼容旧实现的 pid 字段回退，拒绝非正数及溢出 PID。
6. **Guest LMK 启动门控未生效**：`armed` 原先只写不读。现于启动/恢复期间关闭回收，收到 boot complete 后再启用。候选身份判断只读取 argv[0]，并要求合法包名形式；解释器 `.so` 路径和参数中的点号不再被误判为普通 app。
7. **重建误杀孤儿 native 服务**：旧 `killZygoteChildren()` 对镜像环境匹配的所有孤儿进程下手，可能涉及已 daemonize 的 native 服务。现将孤儿兜底限制为 system_server 与包名形式的 Java 进程，保留原有 app_process 路径识别。
8. **OOM 保护日志不可信**：旧代码把尝试数当成功数，拒绝写入后宣称存在未实现的 fallback，并可能每轮继续扫描 /proc。现只统计实际成功；只有 EPERM/EACCES 才标记权限拒绝，进程恰好退出不再永久禁用保护；明确记录非 root/宿主策略下没有 pin 成功，不声称绕过宿主 LMK。
9. **主动杀进程缺少可追踪来源**：设置向导、SurfaceFlinger 重启、冻结看门狗统一经过 `requestZygoteRestart()`，输出 `zygote-kill`、目标 PID 与 reason，并作为计划重启处理。

## 新增现场诊断

`zygote-exit` 记录服务名、退出码、此前是否完成启动、是否计划重启、宿主主动 SIGKILL 原因、宿主 oom_score_adj、Guest LMK 成功 kill 计数、MemAvailable/SwapFree，以及该服务日志末尾最多约 3 KB。

排查时将以下日志按时间对齐：

- `zygote-kill: ... reason=...`：说明宿主模拟器主动触发了重启，不应直接归因宿主 OOM。
- `memory: killed background process ...`：Guest LMK 的真实成功回收；失败不再增加计数。
- `oom: ... denied ... guest is NOT pinned`：说明宿主拒绝负 adj 保护；前台服务不等同所有子进程免杀。
- `zygote-exit: ...`、guest 主日志和 system_server 输出：判断是否先出现 Java fatal、watchdog 或核心服务故障。

内存/交换空间快照只是杀后证据，不能单独证明 OOM；宿主回收事件还需 logcat/lmkd 或系统级证据。

## 验证与限制

`:app:testStandardDebugUnitTest` 编译通过，18 项测试全部通过。新增四项覆盖 Long PID 与回退、退出只消费一次、过期退出通知不删除新活进程，以及解释器路径/参数不能伪装成 app 名称。

未做实机复现、后台往返压力测试或 OOM 注入；没有修改预编译 native 引擎。非 root 环境不能保证写入负 oom_score_adj，也不能保证宿主厂商电量策略不终止 guest。

建议实测矩阵：首次启动和恢复启动；分别触发主、副 zygote 退出；SurfaceFlinger 退出；前后台往返；低内存压力。重点确认主副 zygote 故障隔离、服务监控表仍保留新实例、其他核心服务未被恢复逻辑成批结束，并记录 137 次数及前后日志。

交付验证：基于最新 main（c72e067，0.5.9）执行 :app:testStandardDebugUnitTest :app:assembleStandardDebug，BUILD SUCCESSFUL。调试 APK：app/build/outputs/apk/standard/debug/app-standard-debug.apk。
