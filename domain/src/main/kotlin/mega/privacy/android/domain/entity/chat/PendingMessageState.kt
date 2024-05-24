package mega.privacy.android.domain.entity.chat

/**
 * Pending message state.
 *
 * @param value
 */
enum class PendingMessageState(val value: Int) {

    /**
     * The user just selected this file to be uploaded, it could be a content uri that needs to be copied to the cache folder and/or needs compression (video or image)
     */
    PREPARING(0),

    /**
     * The file is ready to be uploaded, it's accessible to the sdk (was copied to the cache folder if needed) and compressed if needed
     */
    READY_TO_UPLOAD(1),

    /**
     * The file is Uploading to cloud drive
     */
    UPLOADING(2),

    /**
     * The file is already uploaded and it's send to the sdk to be attached to the chat
     */
    ATTACHING(3),

    /**
     * The file needs compression or it's being compressed
     */
    COMPRESSING(4),

    /**
     * Sent
     */
    SENT(20),

    /**
     * Error Uploading the file to cloud drive
     */
    ERROR_UPLOADING(-1),

    /**
     * Error Attaching the node to the chat
     */
    ERROR_ATTACHING(-2)
}