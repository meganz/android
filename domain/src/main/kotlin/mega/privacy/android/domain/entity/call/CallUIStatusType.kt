package mega.privacy.android.domain.entity.call

/**
 * Status of the call UI
 */
enum class CallUIStatusType {
    /**
     * Waiting connection
     */
    WaitingConnection,

    /**
     * One to one
     */
    OneToOne,

    /**
     * Grid view
     */
    GridView,

    /**
     * Speaker view
     */
    SpeakerView,


    /**
     * PictureInPicture view
     */
    PictureInPictureView,

    /**
     * None
     */
    None
}
