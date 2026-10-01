package mega.privacy.android.domain.entity.transfer.pending

/**
 * State of a pending transfer
 */
enum class PendingTransferState {
    /**
     * The transfer has not been sent to the SDK yet
     */
    NotSentToSdk,

    /**
     * There was an error starting the transfer
     */
    ErrorStarting,

    /**
     * The SDK is scanning the transfer
     */
    SdkScanning,

    /**
     * The SDK has scanned the transfer
     */
    SdkScanned,

    /**
     * The transfer has already been started
     */
    AlreadyStarted,
}