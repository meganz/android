package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.TransfersSizeInfo

/**
 * Monitor transfers size
 */
fun interface MonitorTransfersSize {
    /**
     * Invoke
     *
     * @return flow of TransfersSizeInfo
     */
    operator fun invoke(): Flow<TransfersSizeInfo>
}