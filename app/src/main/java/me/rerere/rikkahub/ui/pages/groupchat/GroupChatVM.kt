package me.rerere.rikkahub.ui.pages.groupchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.GroupChat
import me.rerere.rikkahub.data.model.GroupMessage
import me.rerere.rikkahub.data.repository.GroupChatRepository
import me.rerere.rikkahub.service.GroupChatService
import kotlin.uuid.Uuid

class GroupChatVM(
    private val groupChatId: Uuid,
    private val settingsStore: SettingsStore,
    private val groupChatRepository: GroupChatRepository,
    private val groupChatService: GroupChatService,
) : ViewModel() {

    val groupChat: StateFlow<GroupChat?> = groupChatRepository.getGroupChatFlow(groupChatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val messages: StateFlow<List<GroupMessage>> = groupChatRepository.getMessages(groupChatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generatingInfo: StateFlow<GroupChatService.GeneratingInfo> =
        groupChatService.generatingState
            .map { it[groupChatId] ?: GroupChatService.GeneratingInfo() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupChatService.GeneratingInfo())

    val assistants: StateFlow<List<Pair<Uuid, String>>> = settingsStore.settingsFlow
        .map { settings -> settings.assistants.map { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendMessage(text: String) {
        groupChatService.sendMessage(groupChatId, text)
    }

    fun stopGeneration() {
        groupChatService.stopGeneration(groupChatId)
    }

    fun updateGroupChat(groupChat: GroupChat) {
        viewModelScope.launch {
            groupChatRepository.updateGroupChat(groupChat)
        }
    }

    fun deleteGroupChat() {
        viewModelScope.launch {
            groupChatRepository.deleteGroupChat(groupChatId)
        }
    }

    fun clearMessages() {
        viewModelScope.launch {
            groupChatRepository.clearMessages(groupChatId)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            groupChatRepository.deleteMessage(messageId)
        }
    }
}
