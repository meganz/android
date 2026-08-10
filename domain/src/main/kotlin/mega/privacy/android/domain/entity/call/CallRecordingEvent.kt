package mega.privacy.android.domain.entity.call

/**
 * Call recording event
 *
 * @property isSessionOnRecording
 */
sealed interface CallRecordingEvent {
    val isSessionOnRecording: Boolean
    val chatId: Long

    /**
     * Not recording
     */
    data object NotRecording : CallRecordingEvent {
        override val isSessionOnRecording = false
        override val chatId = -1L
    }

    /**
     * Preexisting recording
     *
     * @property chatId
     */
    data class PreexistingRecording(
        override val chatId: Long,
    ) : CallRecordingEvent {
        override val isSessionOnRecording = true
    }

    /**
     * Recording
     *
     * @property chatId
     * @property participantRecording
     */
    data class Recording(
        override val chatId: Long,
        val participantRecording: String?,
    ) : CallRecordingEvent {
        override val isSessionOnRecording = true
    }

    /**
     * Recording ended
     *
     * @property chatId
     */
    data class RecordingEnded(
        override val chatId: Long,
    ) : CallRecordingEvent {
        override val isSessionOnRecording = false
    }

}
