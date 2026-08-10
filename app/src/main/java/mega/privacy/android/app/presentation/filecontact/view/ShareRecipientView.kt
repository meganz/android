package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.model.AvatarData
import java.io.File

/**
 * Renders a single [ShareRecipient] row using the shared `:shared:contact`
 * [ContactItemView]. Resolves the per-row presentational data (display name,
 * avatar, status, verified badge) up-front and passes the access-permission
 * label as the row subtitle.
 *
 * @param shareRecipient that will be shown
 * @param modifier
 * @param selected when true, replaces the avatar with the brand-coloured
 *   check tile used for multi-select states
 */
@Composable
internal fun ShareRecipientView(
    shareRecipient: ShareRecipient,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val displayName = when (shareRecipient) {
        is ShareRecipient.Contact -> shareRecipient.contactData.alias
            ?: shareRecipient.contactData.fullName ?: shareRecipient.email

        is ShareRecipient.NonContact -> shareRecipient.email
    }
    val status = when (shareRecipient) {
        is ShareRecipient.Contact -> shareRecipient.status.toContactItemStatus()
        is ShareRecipient.NonContact -> ContactItemStatus.Unknown
    }
    val avatarColor = when (shareRecipient) {
        is ShareRecipient.Contact -> Color(shareRecipient.defaultAvatarColor)
        is ShareRecipient.NonContact -> colorResource(R.color.red_600_red_300)
    }
    val avatar: AvatarData = when (shareRecipient) {
        is ShareRecipient.Contact -> shareRecipient.contactData.avatarUri
            ?.let { AvatarData.Image(file = File(it)) }
            ?: AvatarData.Initials(
                initials = shareRecipient.getAvatarFirstLetter(),
                avatarColor = avatarColor,
            )

        is ShareRecipient.NonContact -> AvatarData.Initials(
            initials = shareRecipient.getAvatarFirstLetter(),
            avatarColor = avatarColor,
        )
    }
    val permissionLabel = shareRecipient.permission.description()
        ?.let { stringResource(id = it) } ?: ""

    ContactItemView(
        displayName = displayName,
        statusText = permissionLabel,
        status = status,
        avatar = avatar,
        isVerified = shareRecipient.isVerified,
        modifier = modifier.testTag(SHARE_RECIPIENT_CONTACT_ITEM),
        selected = selected,
    )
}

private fun UserChatStatus.toContactItemStatus(): ContactItemStatus = when (this) {
    UserChatStatus.Online -> ContactItemStatus.Online
    UserChatStatus.Away -> ContactItemStatus.Away
    UserChatStatus.Busy -> ContactItemStatus.Busy
    UserChatStatus.Offline -> ContactItemStatus.Offline
    UserChatStatus.Invalid -> ContactItemStatus.Unknown
}

internal const val SHARE_RECIPIENT_CONTACT_ITEM = "share_recipient_view:contact_item"
