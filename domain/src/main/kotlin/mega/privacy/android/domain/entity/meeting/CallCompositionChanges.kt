package mega.privacy.android.domain.entity.meeting

/**
 *  Call Composition changes
 */
enum class CallCompositionChanges {
    /**
     *  Peer removed
     */
    Removed,

    /**
     *  No composition change
     */
    NoChange,

    /**
     *  Peer added
     */
    Added,
}