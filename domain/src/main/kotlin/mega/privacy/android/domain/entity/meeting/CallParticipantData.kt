package mega.privacy.android.domain.entity.meeting


/**
 * Data class containing the main data of a contact.
 *
 * @property clientId               Client id.
 * @property isAudioOn              True, audio on. False, audio off.
 * @property isVideoOn              True, video on. False, video off.
 * @property isContact              True, if it is contact. False, if not.
 * @property isSpeaker              True, if it is speaker. False, if not.
 * @property isGuest                True, if it is guest. False, if not.
 */
data class CallParticipantData(
    val clientId: Long = -1L,
    var isAudioOn: Boolean = false,
    var isVideoOn: Boolean = false,
    var isContact: Boolean = true,
    var isSpeaker: Boolean = false,
    var isGuest: Boolean = false,
)