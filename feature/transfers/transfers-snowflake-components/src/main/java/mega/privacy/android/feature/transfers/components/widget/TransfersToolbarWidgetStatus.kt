package mega.privacy.android.feature.transfers.components.widget

/**
 * TransfersToolbarWidgetView status
 */
enum class TransfersToolbarWidgetStatus {
    /**
     * There are transfers in progress
     */
    Transferring,

    /**
     * The transfers have been completed
     */
    Completed,

    /**
     * Transfers are paused
     */
    Paused,

    /**
     * Transfers are paused due to over quota issue
     */
    OverQuota,

    /**
     * There is some problem with transfers
     */
    Error,

    /**
     * There are no transfers
     */
    Idle;

    internal fun hasFinished() = this == Completed || this == Idle
}