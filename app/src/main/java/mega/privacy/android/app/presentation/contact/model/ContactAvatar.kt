package mega.privacy.android.app.presentation.contact.model

import androidx.compose.ui.graphics.Color

/**
 * Contact avatar
 */
sealed interface ContactAvatar {
    /**
     * Are credentials verified
     */
    val areCredentialsVerified: Boolean

    /**
     * Uri avatar
     *
     * @property uri
     * @property areCredentialsVerified
     */
    data class UriAvatar(val uri: String, override val areCredentialsVerified: Boolean) :
        ContactAvatar

    /**
     * Letter
     *
     * @property firstLetter
     * @property defaultAvatarColor
     * @property areCredentialsVerified
     * @constructor Create empty Letter
     */
    data class InitialsAvatar(
        val firstLetter: String,
        val defaultAvatarColor: Color,
        override val areCredentialsVerified: Boolean,
    ) : ContactAvatar
}