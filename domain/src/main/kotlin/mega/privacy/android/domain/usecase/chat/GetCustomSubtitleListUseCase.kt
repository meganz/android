package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.usecase.contact.GetParticipantFirstNameUseCase
import javax.inject.Inject

/**
 * Use case for getting a list of participants' names.
 * The purpose of the list is build a custom chat subtitle.
 *
 * @property getParticipantFirstNameUseCase [GetParticipantFirstNameUseCase]
 * @property loadUserAttributesUseCase [LoadUserAttributesUseCase]
 * @constructor Create empty Get custom subtitle list use case
 */
class GetCustomSubtitleListUseCase @Inject constructor(
    private val getParticipantFirstNameUseCase: GetParticipantFirstNameUseCase,
    private val loadUserAttributesUseCase: LoadUserAttributesUseCase,
) {

    /**
     * Invoke
     *
     * @param chatId Chat id.
     * @param participantsList List of participants' handles.
     * @param isPreviewMode True if it is in preview mode, false otherwise.
     * @return The list of participant's names.
     */
    suspend operator fun invoke(
        chatId: Long,
        participantsList: List<Long>,
        isPreviewMode: Boolean,
    ): List<String> =
        buildList {
            if (participantsList.isEmpty()) return@buildList
            val participantsCount = participantsList.size
            val pendingToGet = mutableListOf<Long>()
            val maxExceeded = participantsCount > MAX_NAMES_PARTICIPANTS
            val count =
                if (maxExceeded) MAX_NAMES_PARTICIPANTS.minus(1)
                else participantsCount.minus(1)

            for (i in 0..count) {
                getParticipantFirstNameUseCase(participantsList[i], false)?.let { add(it) }
                    ?: pendingToGet.add(participantsList[i])
            }

            if (pendingToGet.isNotEmpty()) {
                runCatching { loadUserAttributesUseCase(chatId, pendingToGet) }
                    .onSuccess {
                        pendingToGet.forEach { userHandle ->
                            getParticipantFirstNameUseCase(userHandle, false)?.let { add(it) }
                        }
                    }.onFailure { return@buildList }
            }

            if (maxExceeded) {
                val otherParticipantsCount = participantsCount.minus(if (isPreviewMode) 3 else 2)
                add(otherParticipantsCount.toString())
            }
        }

    companion object {
        private const val MAX_NAMES_PARTICIPANTS = 3
    }
}