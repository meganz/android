package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.messages.InviteMultipleUsersAsContactResult
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import javax.inject.Inject

/**
 * Mapper for InviteMultipleUsersAsContactResult.
 *
 */
class InviteMultipleUsersAsContactResultMapper @Inject constructor() {

    /**
     * Maps a list of [InviteContactRequest] to [InviteMultipleUsersAsContactResult].
     *
     * @param resultSet list of [InviteContactRequest]
     * @return [InviteMultipleUsersAsContactResult]
     */
    operator fun invoke(resultSet: List<InviteContactRequest>): InviteMultipleUsersAsContactResult {
        val alreadySent = resultSet.count { it == InviteContactRequest.AlreadySent }
        val sent = resultSet.count { it == InviteContactRequest.Sent }
        val error = resultSet.count { it != InviteContactRequest.Sent }

        return if (alreadySent > 0) {
            InviteMultipleUsersAsContactResult.SomeAlreadyRequestedSomeSent(alreadySent, sent)
        } else if (error > 0) {
            InviteMultipleUsersAsContactResult.SomeFailedSomeSent(sent, error)
        } else {
            InviteMultipleUsersAsContactResult.AllSent(sent)
        }
    }
}