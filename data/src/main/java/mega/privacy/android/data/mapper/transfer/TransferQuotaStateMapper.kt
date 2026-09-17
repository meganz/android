package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.TransferQuotaState
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * [TransferQuotaState] mapper from SDK int values
 */
class TransferQuotaStateMapper @Inject constructor() {
    /**
     * Invoke
     */
    operator fun invoke(state: Number): TransferQuotaState = when (state.toInt()) {
        MegaApiJava.TRANSFER_QUOTA_STATE_GREEN -> TransferQuotaState.Green
        MegaApiJava.TRANSFER_QUOTA_STATE_ORANGE -> TransferQuotaState.Orange
        MegaApiJava.TRANSFER_QUOTA_STATE_RED -> TransferQuotaState.Red
        else -> TransferQuotaState.Unknown
    }
}
