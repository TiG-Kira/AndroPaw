package app.andropaw.agent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Stop
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andropaw.R
import app.andropaw.core.VmSession
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import app.andropaw.ui.MiuixDialog
import app.andropaw.ui.ProvideNavigationEventOwner
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.glass.MiuixGlassIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val CardBorder = Color(0x33888888)
private val StatusSuccess = Color(0xFF30B94F)
private val StatusError = Color(0xFFF44336)

// ================================================================== 对话列表页

/** AgentPaw 对话列表（照搬 Ki 的 AiConversationManagementScreen：新建卡 + 会话卡 + 删除确认） */
@Composable
fun AgentHomeContent(
    model: app.andropaw.ui.LibraryModel?,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    onOpenConversation: (String) -> Unit,
    onNewChat: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val glassPage = app.andropaw.ui.rememberGlassPageBackdrop()
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableStateOf(0) }
    var runningName by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        val l: () -> Unit = { version++ }
        ChatSession.onChange(l)
        onDispose { ChatSession.offChange(l) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val id = VmSession.runningId(ctx)
            runningName = id?.let { app.andropaw.core.ImageStore.get(ctx, it)?.name }
            delay(1500)
        }
    }
    val ordered = remember(version) { ChatSession.conversations.sortedByDescending { it.updatedAt } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Box(Modifier.fillMaxSize().then(glassPage.contentModifier)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // 留白来自外壳 Scaffold（玻璃顶栏/底栏的让位），与本页内层 Scaffold 无关
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    // 运行状态横幅：AgentPaw 的能力挂在哪台机器上，一眼可见
                    StatusBanner(runningName)
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MiuixTheme.colorScheme.surface)
                            .border(0.5.dp, CardBorder, RoundedCornerShape(16.dp))
                            .clickable { ChatSession.startDraft(); onNewChat() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape)
                                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(20.dp), tint = MiuixTheme.colorScheme.primary)
                        }
                        Text(stringResource(R.string.agent_new_chat), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.onSurface)
                    }
                }
                items(ordered, key = { it.id }) { conv ->
                    val preview = conv.messages.lastOrNull()?.content?.lineSequence()?.firstOrNull().orEmpty().ifBlank { "…" }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MiuixTheme.colorScheme.surface)
                            .border(0.5.dp, CardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f).clickable { onOpenConversation(conv.id) }) {
                            Text(conv.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text(preview, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text("${conv.messages.size} · ${formatRelativeTime(conv.updatedAt)}", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                        GlassIconButton(onClick = { pendingDeleteId = conv.id }) {
                            Icon(Icons.Rounded.DeleteSweep, stringResource(R.string.delete), Modifier.size(20.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                }
            }
        }
    }
    if (pendingDeleteId != null) {
        MiuixDialog(
            show = true,
            title = stringResource(R.string.delete_conversation_title),
            summary = stringResource(R.string.delete_conversation_summary),
            onDismissRequest = { pendingDeleteId = null },
        ) {
            Row {
                TextButton(text = stringResource(R.string.cancel), onClick = { pendingDeleteId = null }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(16.dp))
                TextButton(text = stringResource(R.string.delete), onClick = { ChatSession.deleteConversation(ctx, pendingDeleteId!!); pendingDeleteId = null }, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ================================================================== 对话页

/** AgentPaw 对话页（照搬 Ki 的 AiChatScreen 骨架：玻璃顶栏 + 消息流 + 三态发送键 + 页面级更多菜单） */
@Composable
fun AgentChatScreen(
    bridge: AgentVmBridge,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    // 保底接线：无论从哪个入口进对话页，执行通道与上下文都已就绪（幂等）
    LaunchedEffect(bridge) { ChatSession.attach(ctx.applicationContext, bridge) }
    val glassPage = app.andropaw.ui.rememberGlassPageBackdrop()
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var menuOpen by remember { mutableStateOf(false) }
    var menuIcon by remember { mutableStateOf(Rect.Zero) }
    var pageOrigin by remember { mutableStateOf(Offset.Zero) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var version by remember { mutableStateOf(0) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        val l: () -> Unit = { version++ }
        ChatSession.onChange(l)
        onDispose { ChatSession.offChange(l) }
    }
    val messages = remember(version) { ChatSession.snapshot() }
    val isLoading = ChatSession.isLoading
    val isStreaming = ChatSession.isStreaming

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length ?: 0, messages.lastOrNull()?.reasoningContent?.length ?: 0) {
        if (messages.isNotEmpty()) listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
    }

    ProvideNavigationEventOwner {
        Box(
            Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background)
                .onGloballyPositioned { pageOrigin = it.boundsInRoot().topLeft },
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                topBar = {
                    GlassTopAppBar(
                        title = stringResource(R.string.agent_chat_title),
                        backdrop = glassPage.backdrop,
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            GlassIconButton(onClick = onBack) {
                                Icon(MiuixGlassIcons.ChevronBackward, stringResource(R.string.back), Modifier.size(24.dp), tint = MiuixTheme.colorScheme.onSurface)
                            }
                        },
                        actions = {
                            GlassIconButton(
                                onClick = { menuOpen = true },
                                backdrop = glassPage.backdrop,
                                modifier = Modifier.onGloballyPositioned { menuIcon = it.boundsInRoot() },
                            ) {
                                Icon(Icons.Rounded.MoreVert, stringResource(R.string.menu), Modifier.size(24.dp), tint = MiuixTheme.colorScheme.onSurface)
                            }
                        },
                    )
                },
            ) { pad ->
                Box(Modifier.fillMaxSize().then(glassPage.contentModifier)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .padding(top = pad.calculateTopPadding()),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                Column(Modifier.fillMaxSize()) {
                                    Spacer(Modifier.weight(1f))
                                    Text(stringResource(R.string.agent_empty), fontSize = 15.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.fillMaxWidth(), style = TextStyle(textAlign = TextAlign.Center))
                                    Spacer(Modifier.height(6.dp))
                                    Text(stringResource(R.string.agent_empty_sub), fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.fillMaxWidth(), style = TextStyle(textAlign = TextAlign.Center))
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        } else {
                            items(messages, key = { it.id }) { msg -> ChatBubble(msg) }
                            if (isLoading && messages.lastOrNull()?.role != "assistant") {
                                item { TypingIndicator() }
                            }
                        }
                        item { Spacer(Modifier.height(10.dp)) }
                    }

                    // 输入栏（三态发送键：就绪/流式中停止/加载中转圈）
                    Column(Modifier.align(Alignment.BottomCenter).imePadding().navigationBarsPadding()) {
                        HorizontalDivider()
                        Row(
                            Modifier.fillMaxWidth().background(MiuixTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
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
                            val canSend = input.text.isNotBlank() && !isLoading
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(
                                        when {
                                            isStreaming -> MiuixTheme.colorScheme.onSurface
                                            canSend -> MiuixTheme.colorScheme.primary
                                            else -> MiuixTheme.colorScheme.surfaceContainerHigh
                                        }
                                    )
                                    .clickable(enabled = canSend || isStreaming || isLoading) {
                                        if (isStreaming || isLoading) ChatSession.stopGeneration()
                                        else {
                                            val text = input.text.trim()
                                            if (text.isNotBlank()) { ChatSession.sendUserMessage(text); input = TextFieldValue("") }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isLoading && !isStreaming) {
                                    Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(size = 16.dp, strokeWidth = 2.dp) }
                                } else if (isStreaming) {
                                    Icon(Icons.Rounded.Stop, stringResource(R.string.agent_stop), Modifier.size(14.dp), tint = MiuixTheme.colorScheme.surface)
                                } else {
                                    Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.agent_send), Modifier.size(17.dp), tint = if (canSend) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                }
                            }
                        }
                    }
                }
            }

            // 玻璃更多菜单（挂页面根 Box：不被顶栏裁剪、锚点不错位）
            GlassDropdownPopup(
                show = menuOpen,
                onDismissRequest = { menuOpen = false },
                anchorBounds = menuIcon.translate(-pageOrigin.x, -pageOrigin.y),
                backdrop = glassPage.backdrop,
            ) {
                GlassPopupItem(text = stringResource(R.string.agent_menu_clear), onClick = { menuOpen = false; showClearConfirm = true })
            }

            if (showClearConfirm) {
                MiuixDialog(
                    show = true,
                    title = stringResource(R.string.agent_menu_clear),
                    summary = stringResource(R.string.clear_conversation_summary),
                    onDismissRequest = { showClearConfirm = false },
                ) {
                    Row {
                        TextButton(text = stringResource(R.string.cancel), onClick = { showClearConfirm = false }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(16.dp))
                        TextButton(text = stringResource(R.string.delete), onClick = {
                            ChatSession.activeId?.let { ChatSession.deleteConversation(ctx, it) }
                            showClearConfirm = false; onBack()
                        }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
    BackHandler { onBack() }
}

// ================================================================== 悬浮小窗（模拟器操作页）

/** 模拟器操作页的 AgentPaw 悬浮对话小窗：与整页共用 ChatSession，逻辑一致 */
@Composable
fun AgentChatOverlay(
    bridge: AgentVmBridge,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var version by remember { mutableStateOf(0) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        val l: () -> Unit = { version++ }
        ChatSession.onChange(l)
        onDispose { ChatSession.offChange(l) }
    }
    val messages = remember(version) { ChatSession.snapshot() }
    val isLoading = ChatSession.isLoading
    val isStreaming = ChatSession.isStreaming

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length ?: 0) {
        if (messages.isNotEmpty()) listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
    }

    ProvideNavigationEventOwner {
        Card(modifier.width(320.dp).heightIn(max = 460.dp), insideMargin = PaddingValues(0.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp), tint = MiuixTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.agent_chat_title), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Box(Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.close), Modifier.size(16.dp))
                    }
                }
                HorizontalDivider()
                LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (messages.isEmpty()) {
                        item { Text(stringResource(R.string.agent_empty_sub), fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
                    } else {
                        items(messages, key = { it.id }) { msg -> ChatBubble(msg) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
                    TextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), label = stringResource(R.string.agent_input_hint), maxLines = 3)
                    Spacer(Modifier.width(6.dp))
                    val canSend = input.text.isNotBlank() && !isLoading
                    Box(
                        Modifier.size(40.dp).clip(CircleShape)
                            .background(if (canSend) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh)
                            .clickable(enabled = canSend || isStreaming || isLoading) {
                                if (isStreaming || isLoading) ChatSession.stopGeneration()
                                else { val t = input.text.trim(); if (t.isNotBlank()) { ChatSession.sendUserMessage(t); input = TextFieldValue("") } }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (isStreaming || isLoading) Icons.Rounded.Stop else Icons.Rounded.ArrowUpward,
                            stringResource(if (isStreaming || isLoading) R.string.agent_stop else R.string.agent_send),
                            Modifier.size(18.dp),
                            tint = if (canSend) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

// ================================================================== 消息气泡与卡片

@Composable
private fun ChatBubble(msg: ChatMessage) {
    when {
        msg.role == "user" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Card(modifier = Modifier.widthIn(max = 300.dp), insideMargin = PaddingValues(12.dp)) {
                Text(parseMarkdown(msg.content), fontSize = 15.sp)
            }
        }
        msg.skillCard != null -> SkillCardView(msg.skillCard)
        else -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            if (!msg.reasoningContent.isNullOrBlank()) {
                ReasoningBlock(msg.reasoningContent, msg.reasoningDone)
                Spacer(Modifier.height(6.dp))
            }
            if (msg.content.isNotBlank() || msg.errorMessage != null) {
                Card(modifier = Modifier.fillMaxWidth(0.94f), insideMargin = PaddingValues(12.dp)) {
                    if (msg.content.isNotBlank()) Text(parseMarkdown(msg.content), fontSize = 15.sp)
                    if (msg.errorMessage != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(msg.errorMessage, fontSize = 12.sp, color = StatusError)
                    }
                }
            }
        }
    }
}

/** 深度思考卡（Ki ReasoningBlock：可折叠 + 进行中呼吸灯） */
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MiuixTheme.colorScheme.surface)
            .border(0.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded },
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Lightbulb, null, Modifier.size(15.dp).graphicsLayer { alpha = if (isDone) 1f else pulseAlpha },
                tint = if (isDone) MiuixTheme.colorScheme.onSurfaceVariantSummary else MiuixTheme.colorScheme.primary)
            Text(stringResource(if (isDone) R.string.agent_thought else R.string.agent_thinking),
                fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = if (isDone) MiuixTheme.colorScheme.onSurfaceVariantSummary else MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, null, Modifier.size(18.dp).graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        if (expanded) {
            Text(reasoning.trim(), fontSize = 12.sp, lineHeight = 18.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp))
        }
    }
}

/** 技能卡（Ki SkillCard：状态徽章圆 + 标题/状态 pill + 命令 meta + 等宽输出 + 截图预览） */
@Composable
private fun SkillCardView(card: SkillCardData) {
    val statusColor = when (card.status) {
        SkillStatus.RUNNING -> MiuixTheme.colorScheme.primary
        SkillStatus.COMPLETED -> StatusSuccess
        else -> StatusError
    }
    val statusText = when (card.status) {
        SkillStatus.RUNNING -> stringResource(R.string.agent_tool_running)
        SkillStatus.COMPLETED -> stringResource(R.string.agent_tool_done)
        else -> stringResource(R.string.agent_tool_failed)
    }
    Column(
        Modifier.fillMaxWidth(0.94f).clip(RoundedCornerShape(14.dp))
            .background(MiuixTheme.colorScheme.surface)
            .border(0.5.dp, CardBorder, RoundedCornerShape(14.dp)),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(19.dp), tint = statusColor)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(card.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.onSurface)
                    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(statusColor.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }
                }
                if (card.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(card.description, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        card.command?.takeIf { it.isNotBlank() }?.let { cmd ->
            HorizontalDivider()
            Text(cmd, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(12.dp), maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
        card.imageBase64?.let { b64 ->
            AsyncImage(
                model = "data:image/jpeg;base64,$b64",
                contentDescription = "screenshot",
                modifier = Modifier.fillMaxWidth().padding(12.dp).heightIn(max = 220.dp).clip(RoundedCornerShape(10.dp)),
            )
        }
        card.output?.takeIf { it.isNotBlank() && card.imageBase64 == null }?.let { out ->
            HorizontalDivider()
            val display = if (out.length > 400) out.take(400) + "\n…" else out
            Box(Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E1E1E)).padding(10.dp)) {
                Text(display, fontSize = 12.sp, lineHeight = 18.sp, color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace)
            }
        }
    }
}

/** 轻量 markdown（Ki parseMarkdown 的精简版）：**粗体** 与 `代码`、行首 # 去除 */
private fun parseMarkdown(text: String): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { li, line ->
        if (li > 0) append(10.toChar())
        var rest = line
        val t = line.trimStart()
        if (t.startsWith("#")) rest = t.trimStart('#').trim()
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
private fun TypingIndicator() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(size = 14.dp, strokeWidth = 2.dp) }
        Text("…", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

private fun formatRelativeTime(ts: Long): String {
    val d = System.currentTimeMillis() - ts
    return when {
        d < 60_000 -> "now"
        d < 3_600_000 -> "${d / 60_000}m"
        d < 86_400_000 -> "${d / 3_600_000}h"
        else -> "${d / 86_400_000}d"
    }
}

/** 运行状态横幅：哪台机器在跑（AgentPaw 的一切操作都落在它身上） */
@Composable
private fun StatusBanner(runningName: String?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(
                if (runningName != null) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MiuixTheme.colorScheme.surface
            )
            .border(0.5.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(
                if (runningName != null) MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MiuixTheme.colorScheme.surfaceContainerHigh
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(20.dp), tint = MiuixTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f)) {
            Text(
                if (runningName != null) stringResource(R.string.agent_attached, runningName)
                else stringResource(R.string.agent_no_machine),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (runningName != null) stringResource(R.string.agent_attached_sub)
                else stringResource(R.string.agent_no_machine_sub),
                fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
