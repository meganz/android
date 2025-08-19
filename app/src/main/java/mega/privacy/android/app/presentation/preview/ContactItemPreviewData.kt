package mega.privacy.android.app.presentation.preview

import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility

internal val contactItemForPreviews get() = contactItemForPreviews(-1)
internal fun contactItemForPreviews(id: Int) = ContactItem(
    handle = id.toLong(),
    email = "email$id@mega.io",
    contactData = ContactData(
        fullName = "Full name $id",
        alias = "Alias $id",
        avatarUri = null,
        userVisibility = UserVisibility.Unknown,
    ),
    defaultAvatarColor = "blue",
    visibility = UserVisibility.Visible,
    timestamp = 2345262L,
    areCredentialsVerified = false,
    status = UserChatStatus.Online,
    lastSeen = null,
    chatroomId = null,
)