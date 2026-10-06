package mega.privacy.android.feature.contact.list.model

/**
 * One-shot feedback messages shown by the contact list screen in a snackbar.
 */
enum class ContactListMessage {

    /**
     * The microphone permission required to start a call was denied.
     */
    MicrophonePermissionDenied,

    /**
     * A call cannot be started because the user is already participating in another call.
     */
    OngoingCall,
}
