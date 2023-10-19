package mega.privacy.android.data.mapper.chat

import mega.privacy.android.data.mapper.contact.UserChatStatusMapper
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import javax.inject.Inject

/**
 * Mapper to convert data into [OnlineStatus].
 */
internal class OnlineStatusMapper @Inject constructor(
    private val userChatStatusMapper: UserChatStatusMapper,
) {

    /**
     * Invoke.
     *
     * @param userHandle User handle.
     * @param status User chat status.
     * @param inProgress Whether the reported status is being set or it is definitive (only for your own changes).
     * @return [OnlineStatus]
     */
    operator fun invoke(userHandle: Long, status: Int, inProgress: Boolean) =
        OnlineStatus(userHandle, userChatStatusMapper(status), inProgress)
}