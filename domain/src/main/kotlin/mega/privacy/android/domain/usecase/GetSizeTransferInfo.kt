package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.TransfersSizeInfo

/**
 * Get size transfer info
 */
fun interface GetSizeTransferInfo {
    /**
     * Invoke
     *
     * @return flow size transfer info
     */
    operator fun invoke(): Flow<TransfersSizeInfo>
}