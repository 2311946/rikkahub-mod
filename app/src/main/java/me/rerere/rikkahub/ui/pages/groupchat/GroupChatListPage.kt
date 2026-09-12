package me.rerere.rikkahub.ui.pages.groupchat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.data.model.GroupChat
import me.rerere.rikkahub.data.model.GroupActivationStrategy
import me.rerere.rikkahub.data.model.GroupPersona
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowTurnBackward
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Message02
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatListPage(
    groupChats: List<GroupChat>,
    assistants: List<Pair<Uuid, String>>,
    assistantsWithConversations: List<AssistantWithConversations>,
    onNavigateToGroupChat: (Uuid) -> Unit,
    onCreateGroupChat: (GroupChat) -> Unit,
    onBack: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("群聊") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(HugeIcons.ArrowTurnBackward, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(HugeIcons.Add01, contentDescription = "创建群聊")
            }
        }
    ) { padding ->
        if (groupChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        HugeIcons.Message02,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "还没有群聊",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击右下角 + 创建一个吧",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groupChats) { gc ->
                    GroupChatItem(
                        groupChat = gc,
                        assistants = assistants,
                        onClick = { onNavigateToGroupChat(gc.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupChatDialog(
            assistants = assistants,
            onDismiss = { showCreateDialog = false },
            onCreate = { gc ->
                onCreateGroupChat(gc)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun GroupChatItem(
    groupChat: GroupChat,
    assistants: List<Pair<Uuid, String>>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                HugeIcons.Message02,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = groupChat.name.ifEmpty { "未命名群聊" },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val memberNames = if (groupChat.personas.isNotEmpty()) {
                    val names = groupChat.personas.take(3).map { it.name }
                    val suffix = if (groupChat.personas.size > 3) " 等${groupChat.personas.size}人" else ""
                    names.joinToString("、") + suffix
                } else {
                    val names = groupChat.memberIds
                        .take(3)
                        .mapNotNull { id -> assistants.find { it.first == id }?.second }
                    val suffix = if (groupChat.memberIds.size > 3) " 等${groupChat.memberIds.size}人" else ""
                    names.joinToString("、") + suffix
                }
                Text(
                    text = memberNames,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreateGroupChatDialog(
    assistants: List<Pair<Uuid, String>>,
    onDismiss: () -> Unit,
    onCreate: (GroupChat) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var personas by remember { mutableStateOf(listOf<GroupPersona>()) }
    var strategy by remember { mutableStateOf(GroupActivationStrategy.NATURAL) }
    var autoRounds by remember { mutableStateOf(3) }
    var autoDelay by remember { mutableStateOf(3) }

    val validAssistants = remember(assistants) {
        assistants.filter { it.second.isNotBlank() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建群聊") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("群名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("选人策略", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = strategy == GroupActivationStrategy.NATURAL,
                        onClick = { strategy = GroupActivationStrategy.NATURAL },
                        label = { Text("智能") }
                    )
                    FilterChip(
                        selected = strategy == GroupActivationStrategy.LIST,
                        onClick = { strategy = GroupActivationStrategy.LIST },
                        label = { Text("轮流") }
                    )
                    FilterChip(
                        selected = strategy == GroupActivationStrategy.POOLED,
                        onClick = { strategy = GroupActivationStrategy.POOLED },
                        label = { Text("随机") }
                    )
                }

                Text("自动接话轮数: $autoRounds", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = autoRounds.toFloat(),
                    onValueChange = { autoRounds = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8
                )

                Text("接话延迟: ${autoDelay}秒", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = autoDelay.toFloat(),
                    onValueChange = { autoDelay = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8
                )

                Text("选择助手（勾选加入群聊，至少2个角色）", style = MaterialTheme.typography.labelLarge)

                validAssistants.forEach { (assistantId, assistantName) ->
                    val hasPersona = personas.any { it.assistantId == assistantId }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hasPersona,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        personas = personas + GroupPersona(
                                            assistantId = assistantId,
                                            name = assistantName,
                                        )
                                    } else {
                                        personas = personas.filter { it.assistantId != assistantId }
                                    }
                                }
                            )
                            Text(
                                text = assistantName.ifBlank { "未命名助手" },
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val assistantPersonas = personas.filter { it.assistantId == assistantId }
                        assistantPersonas.forEach { persona ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 40.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = persona.name,
                                    onValueChange = { newName ->
                                        personas = personas.map {
                                            if (it.id == persona.id) it.copy(name = newName) else it
                                        }
                                    },
                                    label = { Text("角色名") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                if (assistantPersonas.size > 1) {
                                    IconButton(onClick = {
                                        personas = personas.filter { it.id != persona.id }
                                    }) {
                                        Icon(HugeIcons.Cancel01, contentDescription = "删除",
                                            modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        if (hasPersona) {
                            TextButton(
                                onClick = {
                                    personas = personas + GroupPersona(
                                        assistantId = assistantId,
                                        name = assistantName,
                                    )
                                },
                                modifier = Modifier.padding(start = 32.dp)
                            ) {
                                Text("+ 添加更多角色")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        GroupChat(
                            name = name.ifEmpty { "群聊" },
                            personas = personas,
                            activationStrategy = strategy,
                            autoChatRounds = autoRounds,
                            autoModeDelay = autoDelay
                        )
                    )
                },
                enabled = personas.size >= 2
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}