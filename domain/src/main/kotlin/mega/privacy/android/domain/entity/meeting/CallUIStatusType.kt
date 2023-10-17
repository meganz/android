package mega.privacy.android.domain.entity.meeting

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
     * None
     */
    None
}