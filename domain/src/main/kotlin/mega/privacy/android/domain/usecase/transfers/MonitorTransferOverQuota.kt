package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.flow.Flow

/**
 * Monitor transfer over quota
 *
 */
fun interface MonitorTransferOverQuota {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<Boolean>
}