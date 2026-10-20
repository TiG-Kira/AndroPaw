package app.andropaw.agent

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.R
import app.andropaw.core.VmSession
import app.andropaw.ui.GlassMenuItem
import app.andropaw.ui.ProvideNavigationEventOwner
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.glass.MiuixGlassIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * AgentPaw for AndroPaw — 对话执行的进程级宿主。
 *
 * 与 KiTerminal-UX 的 Termux Agent（AgentChatSession）同一思路：执行协程挂在进程级
 * scope 上，切页面/关对话页都不会中断正在跑的那一轮；消息集合是 snapshot 状态，
 * 重新进入页面读到的是同一份对象。
 */
object AgentChatSession {
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile var generationJob: Job? = null
    @Volatile var cancelled: Boolean = false
    @Volatile var isExecuting: Boolean = false

    /** 当前对话的消息（含流式中的半截） */
    val messages = mutableStateListOf<Message>()
    /** 当前活跃对话 id（null：尚未打开任何对话） */
    @Volatile var activeId: String? = null
    /** 顶部状态行：正在执行什么 */
    val statusLine = mutableStateOf<String?>(null)

    fun open(ctx: Context, conversation: Conversation) {
        activeId = conversation.id
        messages.clear()
        // 旧文件里可能存着重复的 id（provider 复用 call_0 之类的 tool-call id），LazyColumn
        // 的 key 一见重复就抛异常——读入时统一补成唯一
        val seen = HashSet<String>()
        messages.addAll(conversation.messages.map { m ->
            if (seen.add(m.id)) m else m.copy(id = m.id + "#" + seen.size)
        })
    }

    /** 列表内唯一的消息 id：provider 会复用 tool-call id（call_0…），直接拿来当 key 必炸 */
    private fun freshId(base: String): String {
        var id = base.ifEmpty { "m" }
        var n = 0
        while (messages.any { it.id == id }) { n++; id = "$base~$n" }
        return id
    }

    fun close() {
        activeId = null
        messages.clear()
    }

    /** 发送一条用户消息并驱动一轮 agent 循环 */
    fun send(bridge: AgentVmBridge, config: LlmConfig, tools: List<AgentTool>, text: String) {
        if (isExecuting || text.isBlank()) return
        val ctx = bridge.ctx
        val conversationId = activeId ?: run {
            val c = AgentStore.newConversation(ctx)
            activeId = c.id
            c.id
        }
        cancelled = false
        isExecuting = true
        messages.add(Message(freshId("u" + System.currentTimeMillis()), MessageRole.USER, text.trim()))
        val streamingId = "a" + System.currentTimeMillis()
        messages.add(Message(streamingId, MessageRole.ASSISTANT, "", MessageStatus.STREAMING))
        var buffer = StringBuilder()

        fun persist() {
            val conv = AgentStore.get(ctx, conversationId) ?: Conversation(conversationId)
            AgentStore.save(ctx, conv.copy(
                title = AgentStore.titleFor(Conversation(conversationId, messages = messages.toList())),
                messages = messages.toList(),
                updatedAt = System.currentTimeMillis(),
            ))
        }

        generationJob = scope.launch {
            val agent = Agent(OpenAiCompatibleClient(), ToolRegistry(tools))
            try {
                agent.run(config, messages.filterNot { it.status == MessageStatus.STREAMING }.toList(), isCancelled = { cancelled })
                                    .collect { ev ->
                        when (ev) {
                            is AgentEvent.AssistantDelta -> {
                                buffer.append(ev.message.content)
                                val i = messages.indexOfFirst { it.id == streamingId }
                                if (i >= 0) messages[i] = messages[i].copy(content = buffer.toString(), status = MessageStatus.STREAMING)
                                statusLine.value = null
                            }
                            is AgentEvent.AssistantTurn -> {
                                val i = messages.indexOfFirst { it.id == streamingId }
                                if (i >= 0) messages[i] = ev.message.copy(id = streamingId)
                            }
                            is AgentEvent.ToolStarted -> {
                                statusLine.value = "running ${ev.call.name}…"
                                // 工具调用卡片：跟在这轮助手消息后面
                                messages.add(Message(
                                    freshId("t" + ev.call.id), MessageRole.TOOL, "", MessageStatus.STREAMING,
                                    toolCallId = ev.call.id,
                                ).let { it.copy(content = ev.call.name + " " + ev.call.arguments.take(200)) })
                            }
                            is AgentEvent.ToolFinished -> {
                                statusLine.value = null
                                val i = messages.indexOfFirst { it.toolCallId == ev.result.toolCallId }
                                // 截图结果整段 base64 不进 UI 也不进对话文件（几百 KB 且没法看）
                                val shown = ev.result.content.let { c ->
                                    if (c.contains("image_base64")) "[screenshot taken]" else c.take(4000)
                                }
                                val card = Message(
                                    freshId("t" + ev.result.toolCallId), MessageRole.TOOL, shown,
                                    if (ev.result.isError) MessageStatus.FAILED else MessageStatus.COMPLETE,
                                    toolCallId = ev.result.toolCallId,
                                )
                                if (i >= 0) messages[i] = card else messages.add(card)
                                persist()
                            }
                            is AgentEvent.Completed -> {
                                val i = messages.indexOfFirst { it.id == streamingId }
                                if (i >= 0) messages[i] = messages[i].copy(content = ev.message.content, status = MessageStatus.COMPLETE)
                                persist()
                            }
                            is AgentEvent.Failed -> {
                                val i = messages.indexOfFirst { it.id == streamingId }
                                if (i >= 0) messages[i] = messages[i].copy(
                                    content = ev.message.content.ifBlank { ev.message.error.orEmpty() },
                                    status = MessageStatus.FAILED, error = ev.message.error,
                                )
                                persist()
                            }
                            is AgentEvent.Cancelled -> {
                                val i = messages.indexOfFirst { it.id == streamingId }
                                if (i >= 0) messages[i] = messages[i].copy(status = MessageStatus.CANCELLED)
                            }
                        }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                val i = messages.indexOfFirst { it.id == streamingId }
                if (i >= 0) messages[i] = messages[i].copy(status = MessageStatus.CANCELLED)
                throw e
            } catch (e: Exception) {
                val i = messages.indexOfFirst { it.id == streamingId }
                if (i >= 0) messages[i] = messages[i].copy(
                    content = e.message ?: e::class.simpleName.orEmpty(),
                    status = MessageStatus.FAILED, error = e.message,
                )
                persist()
            } finally {
                isExecuting = false
                statusLine.value = null
                generationJob = null
                persist()
            }
        }
    }

    fun stop() {
        cancelled = true
        generationJob?.cancel()
    }
}

// ------------------------------------------------------------------ 对话列表页（AgentPaw 首页）

@Composable
fun AgentHomeContent(
    model: app.andropaw.ui.LibraryModel?,
    padding: PaddingValues,
    /**
     * The shell's top-bar scroll connection, applied to the list itself (Termux-Ultra's rule: the
     * connection belongs to the scrolling child). On a wrapping box it never got a thing to scroll
     * and the glass top bar stayed put.
     */
    modifier: Modifier = Modifier,
    onOpenConversation: (Conversation) -> Unit,
) {
    val ctx = LocalContext.current
    var conversations by remember { mutableStateOf(AgentStore.list(ctx)) }
    var runningName by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 轮询：从聊天页返回（或执行中新建）时列表自动跟上；顺带刷运行中机器名
    LaunchedEffect(Unit) {
        while (true) {
            conversations = AgentStore.list(ctx)
            val id = VmSession.runningId(ctx)
            runningName = id?.let { app.andropaw.core.ImageStore.get(ctx, it)?.name }
            delay(1500)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 运行中的机器横幅：agent 只作用于这台
        item {
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(
                        if (runningName != null) Color(0xFF34C08E) else MiuixTheme.colorScheme.onSurfaceVariantSummary))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            runningName?.let { stringResource(R.string.agent_attached, it) } ?: stringResource(R.string.agent_no_machine),
                            fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        )
                        Text(
                            if (runningName != null) stringResource(R.string.agent_attached_sub)
                            else stringResource(R.string.agent_no_machine_sub),
                            fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }

        if (conversations.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(20.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.agent_empty), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.agent_empty_sub), fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
        }

        items(conversations, key = { it.id }) { c ->
            val preview = c.messages.lastOrNull { it.content.isNotBlank() }?.content.orEmpty()
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenConversation(c) },
                insideMargin = PaddingValues(16.dp),
            ) {
                Column {
                    Text(AgentStore.titleFor(c), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        preview.ifBlank { stringResource(R.string.agent_no_messages) },
                        fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // A list of two or three chats is shorter than the screen and cannot be dragged at all, so
        // the top bar would never collapse: give the page some travel while it is still short.
        if (conversations.size < 5) item { Spacer(Modifier.fillParentMaxHeight(0.5f)) }
    }
}

// ------------------------------------------------------------------ 对话页

/** AgentPaw 标签页的新对话 FAB */
@Composable
fun AgentNewChatFab(onNewChat: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(CircleShape).background(MiuixTheme.colorScheme.primary).clickable(onClick = onNewChat),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, stringResource(R.string.agent_new_chat), tint = MiuixTheme.colorScheme.onPrimary)
    }
}

@Composable
fun AgentChatScreen(
    bridge: AgentVmBridge,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val config = remember { AgentStore.config(ctx) }
    val tools = remember { apnpTools(bridge) }
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val messages = AgentChatSession.messages
    val executing = AgentChatSession.isExecuting

    val glassPage = app.andropaw.ui.rememberGlassPageBackdrop()
    val scrollBehavior = MiuixScrollBehavior()

    val toolsById = remember(messages) {
        messages.filter { it.toolCalls.isNotEmpty() }.flatMap { it.toolCalls }.associateBy { it.id }
    }

    // 自动滚到底
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length ?: 0) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // the more-menu: the trigger is a glass pill in the top bar, the panel itself is hosted on
    // this root box (a popup laid out inside the actions slot never had room to grow)
    var menuOpen by remember { mutableStateOf(false) }
    var menuIcon by remember { mutableStateOf(Rect.Zero) }
    var pageOrigin by remember { mutableStateOf(Offset.Zero) }

    // Opaque: the chat page is composed over the shell (its execution lives in the process-level
    // session), so it has to cover the page under it completely — left transparent, the
    // conversation list, the top bar and the FAB all show through and the page looks torn apart.
    // The dispatcher owner is provided again right here: some miuix popup hosts ask for it through
    // a path that missed the activity-level one and crashed outright.
    ProvideNavigationEventOwner {
    Box(
        Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
            .onGloballyPositioned { pageOrigin = it.boundsInRoot().topLeft },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            topBar = {
                GlassTopAppBar(
                    title = AgentStore.get(ctx, AgentChatSession.activeId ?: "")?.let { AgentStore.titleFor(it) }
                        ?: stringResource(R.string.agent_chat_title),
                    backdrop = glassPage.backdrop,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        GlassIconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixGlassIcons.ChevronBackward,
                                contentDescription = stringResource(R.string.back),
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    actions = {
                        GlassIconButton(
                            onClick = { menuOpen = true },
                            backdrop = glassPage.backdrop,
                            modifier = Modifier.onGloballyPositioned { menuIcon = it.boundsInRoot() },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.menu),
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .then(glassPage.contentModifier)
                    .padding(top = padding.calculateTopPadding()),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages, key = { it.id }) { m -> AgentBubble(m, toolsById) }
                }
                // 输入栏
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surface)
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.agent_input_hint),
                        maxLines = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    val canSend = input.isNotBlank() && !executing
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .background(if (canSend) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh)
                            .clickable(enabled = canSend || executing) {
                                if (executing) AgentChatSession.stop()
                                else if (input.isNotBlank()) {
                                    val text = input.trim()
                                    input = ""
                                    AgentChatSession.send(bridge, config, tools, text)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (executing) Icons.Rounded.Close else Icons.Rounded.Send,
                            stringResource(if (executing) R.string.agent_stop else R.string.agent_send),
                            tint = if (canSend) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        GlassDropdownPopup(
            show = menuOpen,
            onDismissRequest = { menuOpen = false },
            anchorBounds = menuIcon.translate(-pageOrigin.x, -pageOrigin.y),
            backdrop = glassPage.backdrop,
        ) {
            GlassPopupItem(
                text = stringResource(R.string.agent_menu_state),
                icon = Icons.Rounded.Info,
                enabled = !executing,
                onClick = {
                    menuOpen = false
                    scope.launch(Dispatchers.IO) {
                        val (_, out) = bridge.guestShell("getprop ro.build.version.release; getprop ro.product.model", 20_000)
                        AgentChatSession.messages.add(Message(
                            "info" + System.currentTimeMillis(), MessageRole.TOOL,
                            "Android " + out.trim(), MessageStatus.COMPLETE,
                        ))
                    }
                },
            )
            GlassPopupItem(
                text = stringResource(R.string.agent_menu_clear),
                icon = Icons.Rounded.Delete,
                enabled = !executing,
                onClick = {
                    menuOpen = false
                    AgentChatSession.activeId?.let { AgentStore.delete(ctx, it) }
                    AgentChatSession.messages.clear()
                    AgentChatSession.close()
                    onBack()
                },
            )
        }
    }
    }
}

/** the tool's icon, by its wire name (Termux-Ultra's per-kind icons) */
private fun toolIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector = when {
    name.startsWith("guest_shell") || name.startsWith("run_script") -> Icons.Rounded.Terminal
    name.startsWith("take_screenshot") -> Icons.Rounded.Screenshot
    name.startsWith("tap") || name.startsWith("swipe") || name.startsWith("long_press") -> Icons.Rounded.TouchApp
    name.startsWith("input_text") || name.startsWith("key_action") -> Icons.Rounded.Keyboard
    name.startsWith("launch_app") -> Icons.Rounded.Apps
    name.startsWith("open_deeplink") -> Icons.Rounded.Link
    name.startsWith("get_screen_state") -> Icons.Rounded.Info
    name.startsWith("wait") -> Icons.Rounded.Schedule
    name.startsWith("vm_power") -> Icons.Rounded.PowerSettingsNew
    name.startsWith("vm_reboot") -> Icons.Rounded.RestartAlt
    name.startsWith("vm_wipe") -> Icons.Rounded.DeleteForever
    name.startsWith("vm_install_recovery") -> Icons.Rounded.InstallMobile
    name.startsWith("vm_") -> Icons.Rounded.Android
    name.startsWith("web_search") -> Icons.Rounded.Search
    else -> Icons.Rounded.Build
}

/** 执行中 = primary、完成 = 绿、失败 = error（Eta 时间线节点的语义色） */
@Composable
private fun toolStatusColor(running: Boolean, failed: Boolean): Color =
    when {
        failed -> MiuixTheme.colorScheme.error
        running -> MiuixTheme.colorScheme.primary
        else -> Color(0xFF30B94F)
    }

/** 深度思考卡（Termux-Ultra ReasoningBlock：可折叠 + 进行中呼吸灯） */
@Composable
private fun ReasoningBlock(reasoning: String, isDone: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "thinking")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "thinkingAlpha",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MiuixTheme.colorScheme.surface)
            .clickable { expanded = !expanded },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.Lightbulb, null,
                Modifier.size(15.dp).graphicsLayer { alpha = if (isDone) 1f else pulseAlpha },
                tint = if (isDone) MiuixTheme.colorScheme.onSurfaceVariantSummary else MiuixTheme.colorScheme.primary,
            )
            Text(
                stringResource(if (isDone) R.string.agent_thought else R.string.agent_thinking),
                fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = if (isDone) MiuixTheme.colorScheme.onSurfaceVariantSummary else MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Rounded.ExpandMore, null,
                Modifier.size(18.dp).graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        if (expanded) {
            Text(
                reasoning.trim(),
                fontSize = 12.sp, lineHeight = 18.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            )
        }
    }
}

/** 工具调用卡（Termux-Ultra SkillCard：状态徽章 + 标题 + 元信息 + 等宽输出） */
@Composable
private fun ToolCard(
    toolName: String,
    title: String,
    running: Boolean,
    failed: Boolean,
    meta: List<String>,
    output: String?,
    error: String?,
) {
    val statusColor = toolStatusColor(running, failed)
    val borderColor = MiuixTheme.colorScheme.outline.copy(alpha = 0.5f)
    Column(
        Modifier
            .fillMaxWidth(0.94f)
            .clip(RoundedCornerShape(14.dp))
            .background(MiuixTheme.colorScheme.surface)
            .border(0.5.dp, borderColor, RoundedCornerShape(14.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(toolIcon(toolName), null, Modifier.size(19.dp), tint = statusColor)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.onSurface)
                    Box(
                        Modifier.clip(RoundedCornerShape(999.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            stringResource(when {
                                running -> R.string.agent_tool_running
                                failed -> R.string.agent_tool_failed
                                else -> R.string.agent_tool_done
                            }),
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = statusColor,
                        )
                    }
                }
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        meta.first(), fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        val rest = meta.drop(1)
        if (rest.isNotEmpty()) {
            HorizontalDivider(color = borderColor)
            Column(Modifier.padding(12.dp)) {
                rest.forEach { line ->
                    Text(line, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontFamily = FontFamily.Monospace, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        output?.takeIf { it.isNotBlank() }?.let { out ->
            HorizontalDivider(color = borderColor)
            val display = if (out.length > 400) out.take(400) + "\n…" else out

            Box(
                Modifier.fillMaxWidth().padding(12.dp)
                    .clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E1E1E)).padding(10.dp),
            ) {
                Text(display, fontSize = 12.sp, lineHeight = 18.sp, color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace)
            }
        }
        error?.takeIf { it.isNotBlank() }?.let { err ->
            HorizontalDivider(color = borderColor)
            Text(
                err, fontSize = 12.sp, color = MiuixTheme.colorScheme.error,
                modifier = Modifier.padding(12.dp), maxLines = 4, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun argsMeta(arguments: String): List<String> = runCatching {
    val o = org.json.JSONObject(arguments.ifBlank { "{}" })
    o.keys().asSequence().take(4).map { k ->
        val v = o.opt(k).toString()
        "$k: ${if (v.length > 80) v.take(80) + "…" else v}"
    }.toList()
}.getOrDefault(emptyList())

/** 工具的展示名：wire 名 → 首字母大写的短名（够用即可，不引资源） */
private fun toolTitle(name: String): String =
    name.removePrefix("vm_").removePrefix("guest_").replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

/** 轻量 markdown：**粗体** 与 `代码`，其余原样——对话输出不再满屏星号 */
private fun markdownText(text: String): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { li, line ->
        if (li > 0) append(10.toChar())
        var rest = line
        val t = line.trimStart()
        if (t.startsWith("#")) { rest = t.trimStart('#').trim() }
        var i = 0
        while (i < rest.length) {
            when {
                rest.startsWith("**", i) -> {
                    val end = rest.indexOf("**", i + 2)
                    if (end > 0) { pushStyle(SpanStyle(fontWeight = FontWeight.Bold)); append(rest.substring(i + 2, end)); pop(); i = end + 2 }
                    else { append(rest[i]); i++ }
                }
                rest[i] == '`' -> {
                    val end = rest.indexOf('`', i + 1)
                    if (end > 0) { pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)); append(rest.substring(i + 1, end)); pop(); i = end + 1 }
                    else { append(rest[i]); i++ }
                }
                else -> { append(rest[i]); i++ }
            }
        }
    }
}

@Composable
private fun AgentBubble(m: Message, toolsById: Map<String, ToolCall> = emptyMap()) {
    // 截图观察消息：user 角色 + 带图
    if (m.isUser && m.images.isNotEmpty()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Card(
                modifier = Modifier.fillMaxWidth(0.7f),
                insideMargin = PaddingValues(6.dp),
            ) {
                Column {
                    AsyncImage(
                        model = "data:image/jpeg;base64,${m.images.first()}",
                        contentDescription = "screen",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    if (m.content.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(m.content, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
        }
        return
    }
    when {
        m.isUser -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Card(
                modifier = Modifier.fillMaxWidth(0.86f),
                insideMargin = PaddingValues(12.dp),
            ) {
                Text(m.content, fontSize = 15.sp)
            }
        }
        m.isAssistant -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            if (m.reasoning.isNotBlank()) {
                ReasoningBlock(m.reasoning, isDone = m.status != MessageStatus.STREAMING)
                Spacer(Modifier.height(6.dp))
            }
            // 已发出但还没有结果的调用画成"执行中"，结果的卡片跟着 TOOL 消息另画
            if (m.toolCalls.isNotEmpty() && m.status == MessageStatus.STREAMING) {
                m.toolCalls.forEach { call ->
                    ToolCard(call.name, toolTitle(call.name), running = true, failed = false,
                        meta = argsMeta(call.arguments), output = null, error = null)
                    Spacer(Modifier.height(6.dp))
                }
            }
            if (m.content.isNotBlank() || m.status == MessageStatus.STREAMING) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.86f),
                        insideMargin = PaddingValues(12.dp),
                    ) {
                        if (m.content.isNotBlank()) Text(markdownText(m.content), fontSize = 15.sp)
                        if (m.status == MessageStatus.STREAMING) {
                            Spacer(Modifier.height(4.dp))
                            Text("…", fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                        m.error?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, fontSize = 12.sp, color = MiuixTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        else -> {
            // 工具结果卡：名称取自发起调用的那条 assistant 消息
            val call = toolsById[m.toolCallId]
            ToolCard(
                call?.name ?: (m.toolCallId ?: "tool"),
                toolTitle(call?.name ?: "tool"),
                running = false,
                failed = m.status == MessageStatus.FAILED || m.content.startsWith("{\"error"),
                meta = call?.let { argsMeta(it.arguments) } ?: emptyList(),
                output = m.content.takeIf { !it.contains("image_base64") },
                error = m.error,
            )
        }
    }
}

/** 模拟器操作页的 AgentPaw 悬浮对话小窗：紧凑、半覆盖，不挡主操作区。执行与整页对话共用进程级会话。 */
@Composable
fun AgentChatOverlay(
    bridge: AgentVmBridge,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val config = remember { AgentStore.config(ctx) }
    val tools = remember { apnpTools(bridge) }
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val messages = AgentChatSession.messages
    val executing = AgentChatSession.isExecuting
    val toolsById = remember(messages) {
        messages.filter { it.toolCalls.isNotEmpty() }.flatMap { it.toolCalls }.associateBy { it.id }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length ?: 0) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    ProvideNavigationEventOwner {
        Card(
            modifier.width(320.dp).fillMaxHeight(0.62f),
            insideMargin = PaddingValues(0.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp), tint = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.agent_chat_title),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.close), Modifier.size(16.dp))
                    }
                }
                HorizontalDivider()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages, key = { it.id }) { m -> AgentBubble(m, toolsById) }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.agent_input_hint),
                        maxLines = 3,
                    )
                    Spacer(Modifier.width(6.dp))
                    val canSend = input.isNotBlank() && !executing
                    Box(
                        Modifier.size(40.dp).clip(CircleShape)
                            .background(if (canSend) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh)
                            .clickable(enabled = canSend || executing) {
                                if (executing) AgentChatSession.stop()
                                else if (input.isNotBlank()) {
                                    val text = input.trim()
                                    input = ""
                                    AgentChatSession.send(bridge, config, tools, text)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (executing) Icons.Rounded.Close else Icons.Rounded.Send,
                            stringResource(if (executing) R.string.agent_stop else R.string.agent_send),
                            tint = if (canSend) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
