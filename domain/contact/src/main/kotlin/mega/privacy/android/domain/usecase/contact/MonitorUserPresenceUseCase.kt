package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.contacts.UserPresence
import mega.privacy.android.domain.usecase.chat.MonitorUserChatStatusByHandleUseCase
import javax.inject.Inject

/**
 * Monitor the chat presence of a user, emitting the current status first and then every
 * status or last green update.
 *
 * The initial status is read from the chat presence config instead of a cached contact so it
 * also works for peers that are not contacts. The last green time is only pushed by the chat
 * server on request, so it is requested automatically whenever the status is Away or Offline
 * and is unknown until the first update arrives.
 */
class MonitorUserPresenceUseCase @Inject constructor(
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val monitorUserChatStatusByHandleUseCase: MonitorUserChatStatusByHandleUseCase,
    private val monitorUserLastGreenUpdatesUseCase: MonitorUserLastGreenUpdatesUseCase,
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase,
) {
    /**
     * Invoke.
     *
     * @param userHandle Handle of the user whose presence is monitored.
     * @return Flow of [UserPresence].
     */
    operator fun invoke(userHandle: Long): Flow<UserPresence> {
        val statusFlow = monitorUserChatStatusByHandleUseCase(userHandle)
            .onStart { emit(getUserOnlineStatusByHandleUseCase(userHandle)) }
            .onEach { status ->
                if (status == UserChatStatus.Away || status == UserChatStatus.Offline) {
                    requestUserLastGreenUseCase(userHandle)
                }
            }
        val lastGreenFlow = monitorUserLastGreenUpdatesUseCase(userHandle)
            .map<Int, Int?> { it }
            .onStart { emit(null) }
        return combine(statusFlow, lastGreenFlow) { status, lastGreen ->
            UserPresence(status = status, lastGreenMinutes = lastGreen)
        }
    }
}
