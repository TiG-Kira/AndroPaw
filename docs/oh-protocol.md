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
