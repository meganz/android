package mega.privacy.android.app.presentation.login.model

/**
 * Data class representing a recovery key link
 *
 * @property link The recovery key link
 * @property recoveryKey The recovery key
 */
data class RkLink(
    val link: String,
    val recoveryKey: String,
)