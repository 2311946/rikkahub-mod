package me.rerere.rikkahub.data.model

import kotlin.uuid.Uuid

object GroupSpeakerSelector {

    data class MemberInfo(
        val id: Uuid,
        val name: String,
        val talkativeness: Float = 0.5f,
        val assistantId: Uuid? = null,
    )

    fun pick(
        strategy: GroupActivationStrategy,
        members: List<MemberInfo>,
        enabledMembers: List<MemberInfo>,
        userInput: String = "",
        lastSpeakerId: Uuid? = null,
        allowSelfResponses: Boolean = false,
        speakerWeights: Map<Uuid, Int> = emptyMap(),
        speakerQueue: List<Uuid> = emptyList(),
        currentQueueIndex: Int = 0
    ): List<MemberInfo> {
        if (enabledMembers.isEmpty()) return emptyList()

        return when (strategy) {
            GroupActivationStrategy.NATURAL -> pickNatural(
                enabledMembers, userInput, lastSpeakerId, allowSelfResponses
            )
            GroupActivationStrategy.LIST -> pickList(
                enabledMembers, speakerQueue, currentQueueIndex
            )
            GroupActivationStrategy.MANUAL -> emptyList()
            GroupActivationStrategy.POOLED -> pickPooled(
                enabledMembers, speakerWeights, lastSpeakerId, allowSelfResponses
            )
        }
    }

    private fun pickNatural(
        members: List<MemberInfo>,
        userInput: String,
        lastSpeakerId: Uuid?,
        allowSelfResponses: Boolean
    ): List<MemberInfo> {
        val candidates = if (!allowSelfResponses && lastSpeakerId != null) {
            members.filter { it.id != lastSpeakerId }
        } else {
            members
        }
        if (candidates.isEmpty()) return members.take(1)

        val words = extractWords(userInput)
        val mentioned = candidates.filter { member ->
            words.any { word -> member.name.contains(word, ignoreCase = true) }
        }
        if (mentioned.isNotEmpty()) return mentioned

        val activated = candidates.filter { Math.random() < it.talkativeness }
        if (activated.isNotEmpty()) return activated.take(1)

        return listOf(candidates.random())
    }

    private fun pickList(
        members: List<MemberInfo>,
        queue: List<Uuid>,
        currentIndex: Int
    ): List<MemberInfo> {
        if (queue.isEmpty()) return members.take(1)
        val targetId = queue[currentIndex % queue.size]
        val member = members.find { it.id == targetId }
        return listOfNotNull(member)
    }

    private fun pickPooled(
        members: List<MemberInfo>,
        weights: Map<Uuid, Int>,
        lastSpeakerId: Uuid?,
        allowSelfResponses: Boolean
    ): List<MemberInfo> {
        val candidates = if (!allowSelfResponses && lastSpeakerId != null) {
            members.filter { it.id != lastSpeakerId }
        } else {
            members
        }
        if (candidates.isEmpty()) return members.take(1)

        val totalWeight = candidates.sumOf { weights[it.id] ?: 1 }
        var roll = (Math.random() * totalWeight).toInt()
        for (member in candidates) {
            roll -= (weights[member.id] ?: 1)
            if (roll < 0) return listOf(member)
        }
        return listOf(candidates.last())
    }

    private fun extractWords(input: String): List<String> {
        return input.split(Regex("[\\s,，。.!！?？@#]+"))
            .filter { it.length >= 2 }
    }
}