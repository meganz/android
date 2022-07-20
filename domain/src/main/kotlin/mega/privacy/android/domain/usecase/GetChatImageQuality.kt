package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatImageQuality

/**
 * Gets chat image quality preference.
 *
 */
fun interface GetChatImageQuality {

    /**
     * Invoke.
     *
     * @return Chat image quality.
     */
    operator fun invoke(): Flow<ChatImageQuality>
}