package mega.privacy.android.app.presentation.contact.link.dialog

import androidx.compose.ui.graphics.Color
import mega.privacy.android.domain.entity.contacts.ContactLinkQueryResult
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import java.io.File

/**
 * Contact link dialog UI state
 *
 * @property contactLinkQueryResult
 * @property avatarFile Avatar file if any
 * @property avatarColor
 * @property inviteContactResult
 */
data class ContactLinkDialogUiState(
    val contactLinkQueryResult: ContactLinkQueryResult? = null,
    val avatarFile: File? = null,
    val avatarColor: Color = Color.Unspecified,
    val inviteContactResult: Result<InviteContactRequest>? = null,
)
