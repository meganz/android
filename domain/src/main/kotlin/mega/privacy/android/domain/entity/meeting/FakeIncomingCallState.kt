package mega.privacy.android.domain.entity.meeting

/**
 * Types of fake call state
 * When the push notification has been received but the call has not yet been retrieved
 */
enum class FakeIncomingCallState {
    /**
     * Notification
     */
    Notification,

    /**
     * Screen
     */
    Screen,

    /**
     * Dismiss
     */
    Dismiss,

    /**
     * Remove
     */
    Remove
}