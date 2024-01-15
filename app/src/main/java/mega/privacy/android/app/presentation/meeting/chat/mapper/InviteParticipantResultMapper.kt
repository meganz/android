package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.InviteContactToChatResult
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.exception.chat.ParticipantAlreadyExistsException
import javax.inject.Inject

/**
 * Mapper to convert a result list of inviting participants into a [InviteContactToChatResult].
 */
class InviteParticipantResultMapper @Inject constructor() {

    /**
     * Converts a result list of inviting participants into a [InviteContactToChatResult].
     * @param results List of results of inviting participants.
     * @return  [InviteContactToChatResult] of the operation.
     */
    operator fun invoke(results: List<Result<ChatRequest>>): InviteContactToChatResult {
        val errorCount = results.count { it.isFailure }
        val successCount = results.size - errorCount

        return when {
            errorCount == 1 -> {
                results.first { it.isFailure }.exceptionOrNull()?.let { throwable ->
                    if (throwable is ParticipantAlreadyExistsException) {
                        return InviteContactToChatResult.AlreadyExistsError
                    }
                }
                return InviteContactToChatResult.GeneralError
            }

            errorCount > 1 -> InviteContactToChatResult.SomeAddedSomeNot(successCount, errorCount)
            successCount == 1 -> InviteContactToChatResult.OnlyOneContactAdded
            else -> InviteContactToChatResult.MultipleContactsAdded(successCount)
        }
    }
}
