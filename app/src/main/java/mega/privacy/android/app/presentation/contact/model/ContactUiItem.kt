package mega.privacy.android.app.presentation.contact.model

/**
 * Contact ui item
 *
 * @property nameOrEmail
 * @property contactStatus
 * @property avatar
 */
data class ContactUiItem(
    val nameOrEmail: String,
    val contactStatus: ContactStatus,
    val avatar: ContactAvatar,
)

