package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Are chat logs enabled
 *
 */
fun interface AreChatLogsEnabled {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<Boolean>
}
