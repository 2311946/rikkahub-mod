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
import me.rerere.rikkahub.data.repository.GroupChatRepository
import kotlin.uuid.Uuid

class GroupChatListVM(
    private val settingsStore: SettingsStore,
    private val groupChatRepository: GroupChatRepository,
) : ViewModel() {

    val groupChats: StateFlow<List<GroupChat>> = groupChatRepository.getAllGroupChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assistants: StateFlow<List<Pair<Uuid, String>>> = settingsStore.settingsFlow
        .map { settings -> settings.assistants.map { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createGroupChat(groupChat: GroupChat) {
        viewModelScope.launch {
            groupChatRepository.insertGroupChat(groupChat)
        }
    }

    fun deleteGroupChat(id: Uuid) {
        viewModelScope.launch {
            groupChatRepository.deleteGroupChat(id)
        }
    }
}
