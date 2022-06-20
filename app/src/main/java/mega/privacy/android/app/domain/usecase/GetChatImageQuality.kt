package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.ChatImageQuality

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