package mega.privacy.android.feature.sync.domain.entity.progress

/** The SDK's last error of a transfer (getLastErrorExtended); the SDK never clears it. */
internal data class SyncTransferError(val code: Int, val value: Long)
