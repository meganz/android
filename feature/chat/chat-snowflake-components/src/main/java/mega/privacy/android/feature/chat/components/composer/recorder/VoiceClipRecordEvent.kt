package mega.privacy.android.feature.chat.components.composer.recorder

/**
 * Event that the voice clip recorder sends when the recording changes.
 */
enum class VoiceClipRecordEvent {
    /**
     * The user pressed and held the record button. The caller must start to record.
     */
    Start,

    /**
     * The user slid up to lock the recording. The recording continues after the user releases
     * the button.
     */
    Lock,

    /**
     * The user cancelled the recording, or released the button too early. The caller must
     * discard the recording.
     */
    Cancel,

    /**
     * The user finished the recording. The caller must send the voice clip.
     */
    Finish,
}
