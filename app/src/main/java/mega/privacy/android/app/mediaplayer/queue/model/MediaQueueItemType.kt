package mega.privacy.android.app.mediaplayer.queue.model

/**
 * Enum class to represent the media queue item type.
 */
enum class MediaQueueItemType {
    /**
     * The previous item in the queue.
     */
    Previous,

    /**
     * The current playing item in the queue.
     */
    Playing,

    /**
     * The next item in the queue.
     */
    Next
}