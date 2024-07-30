package mega.privacy.android.app.presentation.chat.model

import nz.mega.sdk.MegaChatMessage

/**
 * The state for media player opened error.
 *
 * @property message [MegaChatMessage]
 * @property position the message position
 * @property error Throwable
 */
data class MediaPlayerOpenedErrorState(
    val message: MegaChatMessage,
    val position: Int,
    val error: Throwable,
)
