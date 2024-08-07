package mega.privacy.android.app.meeting.pip

/**
 * Ui state for PictureInPictureCallFragment
 * @property isVideoOn true if video is on
 * @property clientId current speaker id
 * @property peerId peer id
 * @property chatId chat id
 */
data class PictureInPictureCallUiState(
    val isVideoOn: Boolean = true,
    val clientId: Long = -1,
    val peerId: Long = -1,
    val chatId: Long = -1,
)
