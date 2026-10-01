package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.usecase.contact.MonitorChatOnlineStatusUseCase
import javax.inject.Inject

/**
 * Monitor user chat status by handle use case
 *
 * @property monitorChatOnlineStatusUseCase [MonitorChatOnlineStatusUseCase]
 */
class MonitorUserChatStatusByHandleUseCase @Inject constructor(
    private val monitorChatOnlineStatusUseCase: MonitorChatOnlineStatusUseCase,
) {
    /**
     * Invoke
     *
     * @param userHandle User handle for monitoring their status.
     * @return Flow of [UserChatStatus].
     */
    operator fun invoke(userHandle: Long) =
        monitorChatOnlineStatusUseCase()
            .filter { onlineStatus -> onlineStatus.userHandle == userHandle }
            .map { it.status }
}
