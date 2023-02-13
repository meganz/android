package mega.privacy.android.domain.usecase.transfer

import kotlinx.coroutines.flow.Flow

/**
 * Monitor failed transfer
 *
 */
fun interface MonitorFailedTransfer {
    /**
     * Invoke
     *
     * @return the flow of true if has failed transfer otherwise false
     */
    operator fun invoke(): Flow<Boolean>
}