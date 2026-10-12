package mega.privacy.android.domain.entity.call

/**
 * The kind of call audio that the user is about to hear. The default output device depends on it.
 */
enum class CallAudioScenario {
    /**
     * A call that starts without local video. The user usually holds the phone to the ear.
     */
    AudioCall,

    /**
     * A call that starts with local video. The user usually looks at the screen.
     */
    VideoCall,

    /**
     * The meeting preview before the user joins, where the user checks the camera and the
     * microphone while looking at the screen.
     */
    MeetingOnboarding,
}
