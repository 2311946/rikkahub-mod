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
import me.rerere.rikkahub.data.model.GroupSpeakerSelector
import me.rerere.rikkahub.data.model.SpeakerRecord
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowTurnBackward
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

data class GroupMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val speakerName: String = "",
    val speakerId: Uuid? = null,
    val colorIndex: Int = 0,
    val isGenerating: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatPage(
    groupChat: GroupChat,
    messages: List<GroupMessage>,
    isGenerating: Boolean,
    currentSpeaker: String,
    onSendMessage: (String) -> Unit,
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
                            Text(
                                text = "${groupChat.memberIds.size}位成员",
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = message.speakerName,
            style = MaterialTheme.typography.labelSmall,
            color = memberColors.getOrElse(message.colorIndex) { memberColors[0] },
            modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
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

@Composable
private fun GroupChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean
) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatSettingsSheet(
    groupChat: GroupChat,
    assistants: List<Pair<Uuid, String>>,
    onUpdateGroupChat: (GroupChat) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}