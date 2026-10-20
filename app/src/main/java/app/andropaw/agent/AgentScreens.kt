package app.andropaw.agent

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.R
import app.andropaw.core.VmSession
import app.andropaw.ui.GlassMenuItem
import app.andropaw.ui.GlassMenuButton
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
import top.yukonga.miuix.kmp.glass.GlassIconButton
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
        messages.addAll(conversation.messages)
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
        messages.add(Message("u" + System.currentTimeMillis(), MessageRole.USER, text.trim()))
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
                                    "t${ev.call.id}", MessageRole.TOOL, "", MessageStatus.STREAMING,
                                    toolCallId = ev.call.id,
                                ).let { it.copy(content = ev.call.name + " " + ev.call.arguments.take(200)) })
                            }
                            is AgentEvent.ToolFinished -> {
                                statusLine.value = null
                                val i = messages.indexOfFirst { it.toolCallId == ev.result.toolCallId }
                                val card = Message(
                                    "t${ev.result.toolCallId}", MessageRole.TOOL, ev.result.content,
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

    // 自动滚到底
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length ?: 0) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Opaque: the chat page is composed over the shell (its execution lives in the process-level
    // session), so it has to cover the page under it completely — left transparent, the
    // conversation list, the top bar and the FAB all show through and the page looks torn apart.
    Box(
        Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
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
                        GlassMenuButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.menu),
                            items = buildList {
                                add(GlassMenuItem(
                                    stringResource(R.string.agent_menu_state), Icons.Rounded.Info,
                                    enabled = !executing,
                                ) {
                                    scope.launch(Dispatchers.IO) {
                                        val (code, out) = bridge.guestShell("getprop ro.build.version.release; getprop ro.product.model", 20_000)
                                        AgentChatSession.messages.add(Message(
                                            "info" + System.currentTimeMillis(), MessageRole.TOOL,
                                            "Android $out".trim(), MessageStatus.COMPLETE,
                                        ))
                                    }
                                })
                                add(GlassMenuItem(
                                    stringResource(R.string.agent_menu_clear), Icons.Rounded.Delete,
                                    enabled = !executing,
                                ) {
                                    AgentChatSession.activeId?.let { AgentStore.delete(ctx, it) }
                                    AgentChatSession.messages.clear()
                                    AgentChatSession.close()
                                    onBack()
                                })
                            },
                        )
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
                    items(messages, key = { it.id }) { m -> AgentBubble(m) }
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
    }
}

@Composable
private fun AgentBubble(m: Message) {
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
        m.isAssistant -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Card(
                modifier = Modifier.fillMaxWidth(0.86f),
                insideMargin = PaddingValues(12.dp),
            ) {
                if (m.content.isNotBlank()) Text(m.content, fontSize = 15.sp)
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
        else -> {
            // 工具调用卡片：紧凑、等宽、可折叠感
            val isShot = m.content.contains("image_base64")
            Card(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                insideMargin = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Column {
                    Text(
                        "⚙ " + (m.toolCallId ?: "tool"),
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (!isShot && m.content.isNotBlank()) {
                        Text(
                            m.content.take(600),
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 6, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (isShot) {
                        AsyncImage(
                            model = "data:image/jpeg;base64," + Regex("\"image_base64\"\\s*:\\s*\"([^\"]+)\"").find(m.content)?.groupValues?.get(1).orEmpty(),
                            contentDescription = "screenshot",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }
        }
    }
}
