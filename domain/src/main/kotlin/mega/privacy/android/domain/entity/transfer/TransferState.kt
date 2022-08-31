package mega.privacy.android.domain.entity.transfer

/**
 * Transfer state
 */
enum class TransferState {
    /**
     * State None refer MegaTransfer.STATE_NONE
     */
    STATE_NONE,

    /**
     * State Queued refer MegaTransfer.STATE_QUEUED
     */
    STATE_QUEUED,

    /**
     * State Active refer MegaTransfer.STATE_ACTIVE
     */
    STATE_ACTIVE,

    /**
     * State Paused refer MegaTransfer.STATE_PAUSED
     */
    STATE_PAUSED,

    /**
     * State Retrying refer MegaTransfer.STATE_RETRYING
     */
    STATE_RETRYING,

    /**
     * State Completing refer MegaTransfer.STATE_COMPLETING
     */
    STATE_COMPLETING,

    /**
     * State Completed refer MegaTransfer.STATE_COMPLETED
     */
    STATE_COMPLETED,

    /**
     * State Cancelled refer MegaTransfer.STATE_CANCELLED
     */
    STATE_CANCELLED,

    /**
     * State Failed refer MegaTransfer.STATE_FAILED
     */
    STATE_FAILED,
}