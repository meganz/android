package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import kotlinx.coroutines.flow.Flow

/**
 * Use case for monitoring updates on calls
 */
fun interface MonitorChatCallUpdates {

    /**
     * Invoke.
     *
     * @return          Flow of [ChatCall].
     */
    operator fun invoke(): Flow<ChatCall>
}