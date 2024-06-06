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
 * @param isRaisedHand              True if is raised hand. False if is lowered hand.
 * @param order                     Sort order based on raised hand order.
 */
data class CallParticipantData(
    val clientId: Long = -1L,
    val isAudioOn: Boolean = false,
    val isVideoOn: Boolean = false,
    val isContact: Boolean = true,
    val isSpeaker: Boolean = false,
    val isGuest: Boolean = false,
    val isRaisedHand: Boolean = false,
    val order: Int = Int.MAX_VALUE,
)
