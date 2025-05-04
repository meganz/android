package mega.privacy.android.app.presentation.filecontact.model

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.domain.entity.user.UserVisibility

internal class ShareRecipientPreviewParameterProvider :
    PreviewParameterProvider<List<ShareRecipient>> {
    private val contact = ShareRecipient.Contact(
        handle = 1L,
        email = "contact@email.com",
        contactData = ContactData(
            fullName = "Contact Name",
            alias = "Contact Alias",
            avatarUri = null,
            userVisibility = UserVisibility.Visible,
        ),
        isVerified = true,
        permission = AccessPermission.READ,
        isPending = false,
        status = UserChatStatus.Online,
        defaultAvatarColor = 0,
    )

    private val nonContact = ShareRecipient.NonContact(
        email = "nonContact@email.com",
        permission = AccessPermission.READ,
        isPending = true,
    )

    override val values: Sequence<List<ShareRecipient>>
        get() = sequenceOf(
            emptyList(),
            listOf<ShareRecipient>(contact),
            listOf<ShareRecipient>(nonContact),
            listOf<ShareRecipient>(contact, nonContact),
        )
}