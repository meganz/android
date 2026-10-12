package mega.privacy.android.app.meeting.activity

/**
 * Inputs for the in-call screen that arrive with the intent launching [MeetingActivity].
 *
 * A null value means the launcher did not supply that input, so the current state is kept.
 *
 * @property isMicEnabled       Whether the call starts with the microphone on
 * @property isCameraEnabled    Whether the call starts with the camera on
 * @property isSpeakerEnabled   Whether the call starts with the loudspeaker on
 * @property meetingName        Name of the meeting
 * @property meetingLink        Link of the meeting
 * @property guestFirstName     First name of a guest joining the meeting
 * @property guestLastName      Last name of a guest joining the meeting
 */
data class InCallInputs(
    val isMicEnabled: Boolean?,
    val isCameraEnabled: Boolean?,
    val isSpeakerEnabled: Boolean?,
    val meetingName: String?,
    val meetingLink: String?,
    val guestFirstName: String?,
    val guestLastName: String?,
) {
    override fun toString() =
        "InCallInputs(isMicEnabled=$isMicEnabled, isCameraEnabled=$isCameraEnabled, isSpeakerEnabled=$isSpeakerEnabled)"
}
