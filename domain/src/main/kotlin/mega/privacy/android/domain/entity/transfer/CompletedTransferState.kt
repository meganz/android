package mega.privacy.android.domain.entity.transfer

/**
 * Completed transfer state
 */
enum class CompletedTransferState {

    /**
     * Transfer finished successfully or was cancelled.
     */
    Completed,

    /**
     * Transfer finished with an error.
     */
    Error,
}