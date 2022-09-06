package mega.privacy.android.domain.entity

/**
 * Data class containing info about a text to import to MEGA.
 *
 * @property isUrl          True if the text to share is an Url.
 * @property subject        Subject of the text to share if any, empty otherwise.
 * @property fileContent    Content of the file to share.
 * @property messageContent Content of the message to share.
 */
data class ShareTextInfo(
    val isUrl: Boolean,
    val subject: String,
    val fileContent: String,
    val messageContent: String,
)
