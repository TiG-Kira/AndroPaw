# Ohspace::Ui 显示桥协议（逆向自 oh-runtime libcomposer.z.so，符号未剥离）

> 逆向来源：`oh-runtime-1.0.0.tar.xz` 内 `system/lib64/libcomposer.z.so`
> （2026-10-30，llvm-objdump + dynsym，地址均指该 .so）。
> 宿主端参考：vendor APK `libmmspace_ui.so`（stripped，仅佐证）。

## 0. 总体架构

- **guest 是 LISTEN 方**：`OhspaceDisplayOutput::Start()` 调
  `Ohspace::Ui::Listen("/ohspace/run/display.sock", fd)`（0x192a0），
  然后起线程 accept（`Ui::CheckPeer` 校验 SO_PEERCRED uid == 自己的 uid，0x10124）。
- **宿主 App 是 CONNECT 方**：`Ui::Connect(path)`（0x10204），
  `socket(AF_UNIX, SOCK_SEQPACKET|SOCK_NONBLOCK|SOCK_CLOEXEC)` + connect + 同样的 uid 校验。
- 所有 socket 都是 **SOCK_SEQPACKET**（保留消息边界！不是 STREAM）。
- `Ui::Send/Receive` = sendmsg/recvmsg，flags `MSG_DONTWAIT(0x40)|MSG_NOSIGNAL(0x4000)`，
  fd 走 SCM_RIGHTS（`Ui::Send` 每条消息最多 4 个 fd）。
- guest 每收到 EAGAIN 由外层 poll 重试；`Ui::Notify(fd)` = 向 eventfd 写 1 唤醒。

## 1. 消息头 `Ohspace::Ui::Header`（16 字节，0x10900 Matches 解码）

```
offset 0x00  u32  kind        // Ohspace::Ui::Kind
offset 0x04  u32  len         // == 整条消息的字节数（含头）
offset 0x08  u64  session     // 会话号 == const.ohspace.display.session 参数值
```

`Matches(hdr, kind, len, session)` 三者全等才通过。

## 2. Kind 枚举（libcomposer 观测值）

| kind | 方向 | 长度 | 含义 |
|------|------|------|------|
| 1 | 宿主→guest | 24 | HELLO：头后 8 字节必须为 0（0x19db8 校验 [len-8..len)==0） |
| 2 | guest→宿主 | 64 | CONFIG 应答（见 §4） |
| 3 | 宿主→guest | 24 | POWER：payload u32@0x10 ∈ {0,1}（0x19e30），写 state+0x14 和 +0xbf |
| 6 | 宿主→guest | 24 | KEY_BACK（input.sock，见 §10） |
| 7 | 宿主→guest | 24 | KEY_HOME（input.sock，见 §10） |

> 触摸不走 display.sock（guest 端该连接只收 24 字节 kind1/3）。
> guest 内触摸验证函数 `Ui::ValidTouch`（0xfed4）存在但消费方在 MMI 栈
> （`system/bin/uinput`、`multimodalinput.cfg`），后备方案：经 runner 执行
> `uinput_inject`。待第二轮确认。

## 3. `Ohspace::Ui::Config`（ValidConfig 0xfe20 解码，≥32 字节）

```
0x00 u32 width      ∈ [320, 2560]
0x04 u32 height     ∈ [320, 2560]，且 width*height ≤ 4*1024*1024
0x08 u32 dpi        dpi < 120 或 dpi ≥ 641（641..∞ 合法；120..640 非法）
0x0c u32 fps        ∈ {30, 60, 90, 110, 120}（位图 0x1000_4000_0000_0001 | 特例 30）
0x10 u32 orientation 0 或 1；0=竖屏要求 width ≤ height，1=横屏要求 width ≥ height
0x18 u64 session     （State+0x18 被用作 Matches 的 session 参数）
```

guest 侧 Config 由 `const.ohspace.display.{width,height,dpi,fps,orientation,session,host_pid}`
系统参数构造（GetParameter）。参数来自 `/system/etc/param/*.para`——
**安装运行时时生成 ohspace.para 即可注入**。

## 4. display.sock 消息流（OhspaceDisplayOutput::Read 0x19ce4）

1. 宿主 connect 成功后发：`{kind=1, len=24, session, pad8=0}`
2. guest 校验 session == Config.session，若 `state[0xa0]==null`（首个连接）：
   - `FrameRing::Create(Config)`（0x19eb0）
   - 回：`{kind=2, len=64, session, FrameInfo(48B), u32 ring_fd, u32 wake_eventfd}`
     + SCM_RIGHTS 附带 2 个 fd（0x19fbc）
   - eventfd = `eventfd(0, EFD_NONBLOCK|EFD_CLOEXEC)`，guest Publish 后写它通知宿主
3. 之后宿主可发 kind=3 (POWER)；guest 收到后 `Notify(eventfd)`
4. 日志：`OHSpace display connected: %ux%u at %u Hz`（0x7f57）

## 5. `Ohspace::Ui::FrameInfo`（FrameRing::Create 0xf1a4 解码，48 字节）

```
0x00  Config 前 32 字节原样拷贝
0x20  u32 stride      = (width*4 + 63) & ~63      // RGBA8888，64B 对齐
0x24  u32 pagesize    = sysconf(_SC_PAGESIZE)，须为 2 的幂且 ≥ 256
0x28  u64 ring_size   = round_down(stride*height + pagesize, pagesize)
```

## 6. FrameRing 共享内存布局（memfd，`memfd_create("ohspace-frame", MFD_CLOEXEC|MFD_ALLOW_SEALING)`）

- 总大小 = `3*ring_size + pagesize`（ftruncate 后 `fcntl(fd, F_ADD_SEALS, 7)`：
  SEAL_SEAL|SEAL_SHRINK|SEAL_GROW；宿主 Import 时用 F_GET_SEALS 校验 =7，fstat 须 S_IFREG）
- mmap(NULL, 3*ring+page, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0)
- **页 0 = 元数据**：
  - `+0x00 u64 latest`（release 写，stlr）：`(seq << 2) | slot`
  - `+0x40 + 64*i`：槽 i 描述符：`+0x00 u64 state`，`+0x08 u64 pts`
- **数据区**：`page + ring_size * slot`（i∈{0,1,2}），帧按行写：
  行 r 在 `data + r*stride`，每行拷 `width*4` 字节

## 7. 槽状态机（原子操作在 state 字上）

```
free(0) --writer CAS--> seq|bit63(0x8000…) --stlr--> seq      // 写完
seq     --reader CAS--> seq|bit62(0x4000…) --dtor CAS--> 0    // 读完释放
```

- 写者（Publish 0xf66c）：seq++；依次尝试槽 `seq%3, (seq+1)%3, (seq+2)%3`：
  `ldar state`，高 2 位非 0 则换下一槽；`ldaxr/stlxr` CAS 写 `seq|bit63`；
  按行 memcpy；`str pts, [desc+8]`；`stlr state=seq`；`stlr latest=(seq<<2)|slot`。
  三槽全忙 → 返回 false。
- 读者（Acquire 0xfb04）：`ldar latest`；要求 seq ≥ 1（latest ≥ 4）；
  slot=latest&3, seq=latest>>2；**必须 seq > 上次读的 seq**（否则 false=无新帧）；
  CAS `state: seq → seq|bit62`（失败=false）；ReadFrame={data=page+ring*slot, seq, pts, slot}；
  ReadFrame 析构时 CAS `state → 0`。
- `CopyLatest(dst, dst_pitch, &seq, &pts)` = Acquire + 按 dst_pitch 逐行拷贝 h 行 w*4。
  `dst_pitch ≥ width*4`。

## 8. VBlank / 刷新

- guest `Refresh()`（0x19934）：state[0xbf] 置 1 时调用 RegRefreshCallback 注册的
  回调（HWC pulse），随后 cond notify；state[0xbf]==0 时 `FrameRing::Clear(now)` +
  `Notify(eventfd)`。
- HDI 层 `OhspaceHdiDevice::RegScreenVBlankCallback` / `RegRefreshCallback` 由
  render_service 注册；宿主端第一版可让 Publish 频率 = fps 定时器。

## 9. AndroPaw 宿主端实现要点（ohrt.c）

1. guest 起 init 后（或周期）`connect("<rootfs>/ohspace/run/display.sock", SEQPACKET)`；
   注意宿主侧用**真实路径**（不走 libaemu64 虚拟化），guest 侧路径被改写到 AEMU64_ROOT。
2. 发 `{kind=1, len=24, session=读到的参数值, pad=0}`；session 由 OHRT 写进
   `system/etc/param/ohspace.para`（安装时生成），两侧读同一值。
3. recv 64B CONFIG：解析 FrameInfo，收 2 个 fd（SCM_RIGHTS，cmsg）。
4. mmap memfd（校验 seals=7、size=3*ring+page），循环：
   poll(eventfd) → Acquire/CopyLatest → `ANativeWindow_lock` + memcpy（注意
   ANativeWindow 行距与 stride 的差异）→ `unlockAndPost`；
   记录 last_seq 传给 Acquire。
5. 触摸：第二阶段（MMI/uinput_inject 通道）。
6. 系统参数注入（安装时写 `system/etc/param/ohspace.para`）：
   `const.ohspace.display.width=...` 等六项 + session（随机 u64）+ host_pid。

## 10. 触摸/输入通道（libmmi-server.z.so `OhspaceInputAdapter` 逆向，2026-10-30）

**guest 的 MMI 服务器也是 LISTEN 方**：`OhspaceInputAdapter::Start()`（0x2fdae0）调
`Ui::Listen("/ohspace/run/input.sock")`（SOCK_SEQPACKET，同一套 Ohspace::Ui 传输层）。
宿主 connect 后即可注入触摸与按键。

### 消息流（`Read` 0x2fdf54）

| kind | 方向 | 长度 | 含义 |
|------|------|------|------|
| 1 | 宿主→guest | 24 | HELLO：guest 收到后 `OpenDevices()`（创建 "OHSpace Touchscreen" 等 uinput 设备）并回一条 24B kind1 应答；此后才接受触摸 |
| 5 | 宿主→guest | 216 | TOUCH：Header(16) + TouchMessage(200)，见下 |
| 6 | 宿主→guest | 24 | KEY_BACK：→ `Key(2)` = KEYCODE_BACK 单击 |
| 7 | 宿主→guest | 24 | KEY_HOME：→ `Key(1)` = KEYCODE_HOME 单击 |

**键码来源（`Key` 0x2fe27c 全量反汇编 + `.rodata` 0xbf3d0，2026-10-30 定案）**：

- `Key(int)` 的参数是 **OH KeyCode**，写进 `KeyEvent+0xa0`：kind7→1=KEYCODE_HOME、
  kind6→2=KEYCODE_BACK。`OpenDevices()` 成功后 MMI 还会自动注入一次 `Key(2802)`
  （KEYCODE_WAKEUP，作为初始唤醒信号——2802 远超 action 值域，反证参数是键码）。
- `.rodata 0xbf3d0` 的常量表 `{2, 3}` 按 `x9` 索引循环两遍写进 `KeyEvent+0xc0`
  （**action**：2=DOWN、3=UP——`KeyEvent::ActionToShortStr`（实体 0x3ea530）的跳转
  索引表 `{0,15,8,12}` 对应 4 个分支，act2→"K:D:"、act3→"K:U:"），同时
  `KeyItem.pressed = (action==2)`。即每收到一条 kind6/7，MMI 服务端**自动合成一次
  完整单击（DOWN+UP）**，宿主无需也不能分别发送按下/抬起。

消息体就是纯 Header（24B = 16B Header + 8B 零填充），**没有键码字段**——键码完全由
kind 编号决定。分发用的是 `memchr(msg, kind, 24)`（宽容解析，靠 Header 首字节）。
另：`KeyEvent+0x38`（deviceId）= `OpenDevices()` 打开的 "OHSpace Navigation" uinput 节点
的 fd（"OHSpace Touchscreen" fd 存 `this+0xc20`，Navigation fd 存 `this+0xc24`）。

### `Ohspace::Ui::TouchMessage`（`ValidTouch` 0xfed4 逐字段解出）

```
0x00  Header(16)   kind=5, len=216, session
0x10  u64 eventTime    必须 ≠ 0
0x18  u64 downTime     必须 ≥ [+0x20]
0x20  u64 time         （与 0x18 组成合法性对）
0x28  u32 action       ≤ 6，Android MotionEvent 枚举：
                        0=DOWN 1=UP 2=MOVE 3=CANCEL 4=OUTSIDE(非法)
                        5=POINTER_DOWN 6=POINTER_UP
                        action ∈ {0,1,2,5,6} 时要求 count>0 且**所有**
                        pointer id == activeId；action==3 恒合法
0x2c  u32 activeId
0x30  u32 count        ≤ 10
0x38  pointer[i]       16B/个 × 10（i < count）：
                        0x00 u32 id        ≤ 31，10 个内不得重复（位图查重）
                        0x04 f32 x         ∈ [0,1]（归一化），finite
                        0x08 f32 y         ∈ [0,1]，finite
                        0x0c f32 pressure  ∈ [0,1]，finite
```

坐标是**归一化 [0,1]**——宿主把窗口内触摸位置除以窗口宽高即得；guest 侧用
`const.ohspace.display.width/height` 换算回像素（mmi-server 直接读这两个参数）。

### 宿主实现要点

1. connect 路径：`<rootfs>/ohspace/run/input.sock`
2. 发 HELLO(24B) → 收 guest 的 24B kind1 应答（在此之前发的触摸会被丢弃）
3. 每个触摸事件组一条 216B 消息 send（SEQPACKET 单包）；MOVE 高频时每帧一条即可
4. uinput 设备由 guest 的 `mmi_uinput_service`/`OpenDevices` 管理，宿主不碰 /dev/uinput

## 11. GPU 直通（libohspace_gpu_client.so / vendor 三库逆向，2026-10-30）

> 早前推测的 "broker.sock" **不存在**。真实拓扑是三个 unix socket + 一个 goldfish
> pipe 兜底，传输层就是裸字节流（gfxstream 编码协议自带帧格式）。

### 11.1 双方组件

**guest 侧**（rootfs `system/lib64/chipset-sdk-sp/libohspace_gpu_client.so`，2.3MB）：
gfxstream guest（goldfish-opengl）定制连接器：

- `HostConnection::getOrCreate(VirtGpuCapset)` / `get()` / `rcEncoder()` / `gl2Encoder()`：
  标准 gfxstream guest API，单例按 PID 复位（fork 安全，`getPuid` 0xcd4dc）。
- `Ohspace::Gpu::Stream`：IOStream 的 socket 实现。构造
  `Stream(name, flags)`；`connectSocket()`（0x1581e4）对 `name` 做 `rfind('/')` 截出
  目录后 append `"gles.sock"`；`socket(AF_UNIX, SOCK_STREAM|CLOEXEC|NONBLOCK)` →
  `SocketConnect()`（0x158d88：非阻塞 connect + EAGAIN/EINPROGRESS poll 重试）。
- `SocketTransfer(fd, buf, len, isWrite)`（0x158f74）：**裸 send/recv**，
  `recv(..., MSG_DONTWAIT)` + poll 重试；无自定义帧头、**无 SCM_RIGHTS**。
- `processPipeInit()`（0x15877c）：`Stream::connect("pipe:GLProcessPipe")`（goldfish
  pipe 兜底，通用连接器 `connect(const char*)` 内 `strcmp(name, "pipe:opengles")`）→
  `writeFully(u32 100)` → `readFully(u64)` 返回 process handle。
- 顶层 buffer API：`OhspaceGpuAllocate`（0xcde6c，`HostConnection::rcEncoder()
  ->colorBufferCreate`，格式 GL_RGBA 0x1908 / GL_RGB565 0x8d62，宽高 ≤ 32M 像素）、
  `OhspaceGpuPresent` / `OhspaceGpuBlank`（rcEncoder flush/post）、`OhspaceGpuUpload`、
  `RetainColorBuffer`（一次性 Stream → connect → writeFully → exportReference）。
  YUV 由 `Ohspace::Gpu::ConvertYuv` + `OhspaceBuffer::Layout` 处理。
- buffer 锁文件：`/ohspace/run/buffer-lock-%02x`。

**socket 拓扑**（guest 视角路径）：

| 路径 | 用途 |
|------|------|
| `/ohspace/gpu/command.sock` | HostConnection 主通道（rc + GL 命令流），`new Stream("/ohspace/gpu/command.sock", 0x4000)` |
| `/ohspace/gpu/gles.sock` | GLES 数据流（`connectSocket` 从通道名目录派生 `dir + "gles.sock"`） |
| `/ohspace/gpu/fault` | `StopSession` 故障通知 |
| `pipe:opengles` / `pipe:GLProcessPipe` | goldfish pipe 兜底（需 goldfish address space） |

**宿主侧**（vendor APK，`lib/arm64-v8a/`，AndroPaw 已提取到 `.cache/mmparts/lib/`）：

```
libmmspace_gpu_jni.so      → JNI 门面：Mmspace::Gpu::{ Start(const Configuration&),
                             Stop(), Healthy(), HasFrame(), ReadError(char*,size_t),
                             SetWindow(ANativeWindow*, w, h) }
libmmspace_gpu_renderer.so → rutabaga_gfx（Rust）+ kumquat server（Mesa virtio-gpu IPC）
                             + cross-domain；支持后端 gfxstream-gles / gfxstream-composer /
                             virgl / virgl2 / venus / cross-domain / drm / magma / rutabaga_2d；
                             kumquat 协议常量 kumquat_gpu_protocol_{ctrl_hdr,cmd_submit,
                             ctx_create,resource_create_blob,transfer_host_3d,get_capset,...}，
                             默认 socket `/tmp/kumquat-gpu-0`；memfd_create 共享资源
libmmspace_gfxstream.so    → gfxstream host backend（5MB）：
                             gfxstream_backend_{set_window,detach_window,
                             has_guest_posted_frame,reset_guest_posted_frame,set_vsync_hz}
```

### 11.2 端到端数据流

```
guest GL/EGL (GLESv3)
  → libohspace_gpu_client (gfxstream gl2_enc/gl_enc/renderControl_enc 编码)
  → /ohspace/gpu/command.sock + gles.sock (裸字节流)
  → 宿主 rutabaga_gfx (kumquat server 收流 → 解 kumquat_gpu_protocol)
  → gfxstream host backend (libmmspace_gfxstream) 解码 GL
  → 宿主 GPU 渲染 → gfxstream_backend_set_window(ANativeWindow) 上屏
```

### 11.3 AndroPaw 落地路径

1. 把 vendor APK 的三个 GPU 库（gpu_jni / gpu_renderer / gfxstream，共约 6.5MB）打进
   AndroPaw APK 的 `jniLibs/arm64-v8a/`，JNI 侧直接调 `Mmspace::Gpu::*`
   （Configuration 结构字段待逆向——renderer 内有 serde_json，可能是 JSON）。
2. `SetWindow(ANativeWindow*, w, h)` 接 OhDisplayActivity 的 Surface；`HasFrame()`
   轮询 guest posted frame。
3. rootfs 侧确保 `/ohspace/gpu/` 目录存在（socket 由 rutabaga 监听创建，guest client
   connect），`/tmp/kumquat-gpu-0` 与 `/ohspace/run/buffer-lock-*` 落在 guest 可见路径。
4. guest 侧无需任何改动：libohspace_gpu_client 已在 ROM 里，连接失败会静默回落
   （无 GPU 时 guest 仍可走软渲/合成器兜底）。
