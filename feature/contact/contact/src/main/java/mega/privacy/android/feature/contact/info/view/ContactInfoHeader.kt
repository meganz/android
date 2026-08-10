package mega.privacy.android.feature.contact.info.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.contact.component.ContactStatusDot
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.shared.contact.components.ContactAvatar
import mega.privacy.android.shared.contact.components.getLastSeenString
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Header of the contact info screen: large centered avatar, name with presence indicator,
 * status/last-seen line, nickname (when set) and email.
 *
 * @param avatar
 * @param displayName
 * @param userChatStatus
 * @param lastSeenMinutes minutes since the contact was last seen, or null if unknown.
 * @param nickname nickname line, hidden when null.
 * @param email email line, hidden when null.
 * @param modifier
 */
@Composable
internal fun ContactInfoHeader(
    avatar: AvatarData,
    displayName: String,
    userChatStatus: UserChatStatus,
    lastSeenMinutes: Int?,
    nickname: String?,
    email: String?,
    modifier: Modifier = Modifier,
) {
    val status = userChatStatus.toContactItemStatus()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .testTag(CONTACT_INFO_HEADER_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ContactAvatar(
            avatar = avatar,
            displayName = displayName,
            isVerified = false,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .size(80.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MegaText(
                modifier = Modifier.testTag(CONTACT_INFO_HEADER_NAME_TAG),
                text = displayName,
                textColor = TextColor.Primary,
                style = AppTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ContactStatusDot(status)
        }
        statusText(status = status, lastSeenMinutes = lastSeenMinutes)?.let { statusText ->
            MegaText(
                modifier = Modifier.testTag(CONTACT_INFO_HEADER_STATUS_TAG),
                text = statusText,
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
            )
        }
        nickname?.let {
            MegaText(
                modifier = Modifier.testTag(CONTACT_INFO_HEADER_NICKNAME_TAG),
                text = it,
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        email?.let {
            MegaText(
                modifier = Modifier.testTag(CONTACT_INFO_HEADER_EMAIL_TAG),
                text = it,
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun statusText(status: ContactItemStatus, lastSeenMinutes: Int?): String? = when (status) {
    ContactItemStatus.Unknown -> null
    ContactItemStatus.Online -> stringResource(sharedR.string.online_status)
    ContactItemStatus.Away -> getLastSeenString(lastSeenMinutes)
        ?: stringResource(sharedR.string.away_status)

    ContactItemStatus.Busy -> stringResource(sharedR.string.busy_status)
    ContactItemStatus.Offline -> getLastSeenString(lastSeenMinutes)
        ?: stringResource(sharedR.string.offline_status)
}

internal fun UserChatStatus.toContactItemStatus(): ContactItemStatus = when (this) {
    UserChatStatus.Online -> ContactItemStatus.Online
    UserChatStatus.Away -> ContactItemStatus.Away
    UserChatStatus.Busy -> ContactItemStatus.Busy
    UserChatStatus.Offline -> ContactItemStatus.Offline
    UserChatStatus.Invalid -> ContactItemStatus.Unknown
}

internal const val CONTACT_INFO_HEADER_TAG = "contact_info_header"
internal const val CONTACT_INFO_HEADER_NAME_TAG = "contact_info_header:text_name"
internal const val CONTACT_INFO_HEADER_STATUS_TAG = "contact_info_header:text_status"
internal const val CONTACT_INFO_HEADER_NICKNAME_TAG = "contact_info_header:text_nickname"
internal const val CONTACT_INFO_HEADER_EMAIL_TAG = "contact_info_header:text_email"

@CombinedThemePreviews
@Composable
private fun ContactInfoHeaderOnlinePreview() {
    AndroidThemeForPreviews {
        ContactInfoHeader(
            avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
            displayName = "Alice Anderson",
            userChatStatus = UserChatStatus.Online,
            lastSeenMinutes = null,
            nickname = "Ally",
            email = "alice@example.com",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContactInfoHeaderOfflinePreview() {
    AndroidThemeForPreviews {
        ContactInfoHeader(
            avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
            displayName = "Bob Brown",
            userChatStatus = UserChatStatus.Offline,
            lastSeenMinutes = 90,
            nickname = null,
            email = "bob@example.com",
        )
    }
}
