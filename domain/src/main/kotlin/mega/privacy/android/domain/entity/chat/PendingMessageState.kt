package mega.privacy.android.domain.entity.chat

/**
 * Pending message state.
 *
 * @param value
 */
enum class PendingMessageState(val value: Int) {

    /**
     * Preparing
     */
    PREPARING(0),

    /**
     * Preparing From File Explorer
     */
    PREPARING_FROM_EXPLORER(1),

    /**
     * Uploading
     */
    UPLOADING(2),

    /**
     * Attaching
     */
    ATTACHING(3),

    /**
     * Compressing
     */
    COMPRESSING(4),

    /**
     * Sent
     */
    SENT(20),

    /**
     * Error Uploading
     */
    ERROR_UPLOADING(-1),

    /**
     * Error Attaching
     */
    ERROR_ATTACHING(-2)
}