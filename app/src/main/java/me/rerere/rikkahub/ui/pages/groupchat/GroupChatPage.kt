package me.rerere.rikkahub.ui.pages.groupchat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.data.model.GroupChat
import me.rerere.rikkahub.data.model.GroupActivationStrategy
import me.rerere.rikkahub.data.model.GroupMessage
import me.rerere.rikkahub.data.model.GroupPersona
import me.rerere.rikkahub.ui.components.ui.TextAvatar
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowTurnBackward
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Forward02
import me.rerere.hugeicons.stroke.Setting07
import kotlin.uuid.Uuid

private val memberColors = listOf(
    Color(0xFF6200EE),
    Color(0xFFD32F2F),
    Color(0xFF1976D2),
    Color(0xFF388E3C),
    Color(0xFFF57C00),
    Color(0xFF7B1FA2),
    Color(0xFF00796B),
    Color(0xFFC2185B),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatPage(
    groupChat: GroupChat,
    messages: List<GroupMessage>,
    isGenerating: Boolean,
    currentSpeaker: String,
    streamingContent: String = "",
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(groupChat.name.ifEmpty { "群聊" })
                        if (isGenerating) {
                            Text(
                                text = "${currentSpeaker} 正在输入...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val memberCount = if (groupChat.personas.isNotEmpty()) {
                                groupChat.personas.size
                            } else {
                                groupChat.memberIds.size
                            }
                            Text(
                                text = "${memberCount}位成员",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(HugeIcons.ArrowTurnBackward, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(HugeIcons.Setting07, contentDescription = "群聊设置")
                    }
                }
            )
        },
        bottomBar = {
            GroupChatInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText.trim())
                        inputText = ""
                    }
                },
                onStop = onStopGeneration,
                isGenerating = isGenerating
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                if (message.isUser) {
                    UserMessageBubble(message = message)
                } else {
                    GroupMemberMessageBubble(message = message)
                }
            }
            if (isGenerating) {
                item(key = "typing_indicator") {
                    if (streamingContent.isNotEmpty()) {
                        StreamingMessageBubble(
                            speakerName = currentSpeaker,
                            content = streamingContent,
                        )
                    } else {
                        TypingIndicator(speakerName = currentSpeaker)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(message: GroupMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun GroupMemberMessageBubble(message: GroupMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        TextAvatar(
            text = message.speakerName,
            modifier = Modifier.size(32.dp),
            color = memberColors.getOrElse(message.colorIndex) { memberColors[0] }.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.speakerName,
                style = MaterialTheme.typography.labelSmall,
                color = memberColors.getOrElse(message.colorIndex) { memberColors[0] },
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (message.isGenerating) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator(speakerName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextAvatar(
            text = speakerName.ifEmpty { "?" },
            modifier = Modifier.size(32.dp),
            loading = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${speakerName.ifEmpty { "..." }} 正在输入",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                LinearProgressIndicator(
                    modifier = Modifier.width(24.dp).height(2.dp)
                )
            }
        }
    }
}

@Composable
private fun StreamingMessageBubble(
    speakerName: String,
    content: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        TextAvatar(
            text = speakerName,
            modifier = Modifier.size(32.dp),
            loading = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = speakerName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = content,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean
) {
    val showStop = isGenerating && inputText.isBlank()
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("说点什么...") },
                maxLines = 4,
                singleLine = false
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (showStop) {
                FilledIconButton(
                    onClick = onStop,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        HugeIcons.Cancel01,
                        contentDescription = "停止生成"
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(
                        HugeIcons.Forward02,
                        contentDescription = "发送",
                        tint = if (inputText.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatSettingsSheet(
    groupChat: GroupChat,
    assistants: List<Pair<Uuid, String>>,
    onUpdateGroupChat: (GroupChat) -> Unit,
    onClearMessages: () -> Unit,
    onDeleteGroupChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var editingPersona by remember { mutableStateOf<GroupPersona?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("群聊设置", style = MaterialTheme.typography.headlineSmall)

            var name by remember { mutableStateOf(groupChat.name) }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onUpdateGroupChat(groupChat.copy(name = it))
                },
                label = { Text("群名") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("选人策略", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = groupChat.activationStrategy == GroupActivationStrategy.NATURAL,
                    onClick = { onUpdateGroupChat(groupChat.copy(activationStrategy = GroupActivationStrategy.NATURAL)) },
                    label = { Text("智能") }
                )
                FilterChip(
                    selected = groupChat.activationStrategy == GroupActivationStrategy.LIST,
                    onClick = { onUpdateGroupChat(groupChat.copy(activationStrategy = GroupActivationStrategy.LIST)) },
                    label = { Text("轮流") }
                )
                FilterChip(
                    selected = groupChat.activationStrategy == GroupActivationStrategy.POOLED,
                    onClick = { onUpdateGroupChat(groupChat.copy(activationStrategy = GroupActivationStrategy.POOLED)) },
                    label = { Text("随机") }
                )
            }

            var rounds by remember { mutableStateOf(groupChat.autoChatRounds) }
            Text("自动接话轮数: $rounds", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = rounds.toFloat(),
                onValueChange = {
                    rounds = it.toInt()
                    onUpdateGroupChat(groupChat.copy(autoChatRounds = rounds))
                },
                valueRange = 1f..10f,
                steps = 8
            )

            if (groupChat.personas.isNotEmpty()) {
                PersonaManagementSection(
                    groupChat = groupChat,
                    assistants = assistants,
                    onUpdateGroupChat = onUpdateGroupChat,
                    onEditPersona = { editingPersona = it }
                )
            } else {
                LegacyMemberSection(
                    groupChat = groupChat,
                    assistants = assistants,
                    onUpdateGroupChat = onUpdateGroupChat
                )
            }

            HorizontalDivider()

            var showClearConfirm by remember { mutableStateOf(false) }
            var showDeleteConfirm by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(HugeIcons.Delete01, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("清除消息")
            }
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(HugeIcons.Delete01, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("删除群聊")
            }

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text("清除消息") },
                    text = { Text("确定要清除所有消息吗？此操作不可撤销。") },
                    confirmButton = {
                        TextButton(onClick = {
                            onClearMessages()
                            showClearConfirm = false
                        }) { Text("清除") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
                    }
                )
            }
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("删除群聊") },
                    text = { Text("确定要删除这个群聊吗？所有消息都会被删除，此操作不可撤销。") },
                    confirmButton = {
                        TextButton(onClick = {
                            onDeleteGroupChat()
                            showDeleteConfirm = false
                        }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    editingPersona?.let { persona ->
        EditPersonaDialog(
            persona = persona,
            onDismiss = { editingPersona = null },
            onSave = { updated ->
                val newPersonas = groupChat.personas.map {
                    if (it.id == updated.id) updated else it
                }
                onUpdateGroupChat(groupChat.copy(personas = newPersonas))
                editingPersona = null
            }
        )
    }
}

@Composable
private fun PersonaManagementSection(
    groupChat: GroupChat,
    assistants: List<Pair<Uuid, String>>,
    onUpdateGroupChat: (GroupChat) -> Unit,
    onEditPersona: (GroupPersona) -> Unit
) {
    Text("角色管理", style = MaterialTheme.typography.titleMedium)

    val personasByAssistant = groupChat.personas.groupBy { it.assistantId }

    assistants.forEach { (assistantId, assistantName) ->
        val thisAssistantPersonas = personasByAssistant[assistantId] ?: emptyList()
        if (thisAssistantPersonas.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = assistantName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    val newPersona = GroupPersona(
                        assistantId = assistantId,
                        name = assistantName,
                    )
                    onUpdateGroupChat(groupChat.copy(personas = groupChat.personas + newPersona))
                }) {
                    Text("添加角色")
                }
            }
            thisAssistantPersonas.forEach { persona ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = persona.enabled,
                        onCheckedChange = { enabled ->
                            val newPersonas = groupChat.personas.map {
                                if (it.id == persona.id) it.copy(enabled = enabled) else it
                            }
                            onUpdateGroupChat(groupChat.copy(personas = newPersonas))
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = persona.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onEditPersona(persona) }) {
                        Text("编辑")
                    }
                    IconButton(onClick = {
                        val newPersonas = groupChat.personas.filter { it.id != persona.id }
                        onUpdateGroupChat(groupChat.copy(personas = newPersonas))
                    }) {
                        Icon(
                            HugeIcons.Cancel01,
                            contentDescription = "删除",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }
    }

    // Assistants not yet in the group — offer to add personas from them
    val usedAssistantIds = personasByAssistant.keys
    val unusedAssistants = assistants.filter { it.first !in usedAssistantIds }
    if (unusedAssistants.isNotEmpty()) {
        Text("添加新助手的角色", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        unusedAssistants.forEach { (assistantId, assistantName) ->
            TextButton(onClick = {
                val newPersona = GroupPersona(
                    assistantId = assistantId,
                    name = assistantName,
                )
                onUpdateGroupChat(groupChat.copy(personas = groupChat.personas + newPersona))
            }) {
                Text("+ $assistantName")
            }
        }
    }
}

@Composable
private fun LegacyMemberSection(
    groupChat: GroupChat,
    assistants: List<Pair<Uuid, String>>,
    onUpdateGroupChat: (GroupChat) -> Unit
) {
    Text("成员管理", style = MaterialTheme.typography.titleMedium)
    assistants.forEach { (id, assistantName) ->
        val isMember = id in groupChat.memberIds
        val isDisabled = id in groupChat.disabledMemberIds
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isMember,
                onCheckedChange = { checked ->
                    val newIds = if (checked) {
                        groupChat.memberIds + id
                    } else {
                        groupChat.memberIds - id
                    }
                    onUpdateGroupChat(groupChat.copy(memberIds = newIds))
                }
            )
            Text(assistantName, modifier = Modifier.weight(1f))
            if (isMember) {
                TextButton(
                    onClick = {
                        val newDisabled = if (isDisabled) {
                            groupChat.disabledMemberIds - id
                        } else {
                            groupChat.disabledMemberIds + id
                        }
                        onUpdateGroupChat(groupChat.copy(disabledMemberIds = newDisabled))
                    }
                ) {
                    Text(if (isDisabled) "解除禁言" else "禁言")
                }
            }
        }
    }
}

@Composable
private fun EditPersonaDialog(
    persona: GroupPersona,
    onDismiss: () -> Unit,
    onSave: (GroupPersona) -> Unit
) {
    var name by remember { mutableStateOf(persona.name) }
    var systemPrompt by remember { mutableStateOf(persona.systemPrompt) }
    var talkativeness by remember { mutableStateOf(persona.talkativeness) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑角色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("角色名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("系统提示词（留空使用助手默认）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
                Text(
                    "话多程度: ${"%.1f".format(talkativeness)}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = talkativeness,
                    onValueChange = { talkativeness = it },
                    valueRange = 0f..1f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(persona.copy(
                    name = name,
                    systemPrompt = systemPrompt,
                    talkativeness = talkativeness
                ))
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}