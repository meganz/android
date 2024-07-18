package mega.privacy.android.domain.entity.call


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
    val isAudioOn: Boolean = false,
    val isVideoOn: Boolean = false,
    val isContact: Boolean = true,
    val isSpeaker: Boolean = false,
    val isGuest: Boolean = false,
)
