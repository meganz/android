package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Are sdk logs enabled
 *
 */
fun interface AreSdkLogsEnabled {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<Boolean>
}
