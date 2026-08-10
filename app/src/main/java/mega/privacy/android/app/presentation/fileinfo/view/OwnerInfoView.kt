package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.DefaultAvatarView
import mega.privacy.android.app.presentation.contact.view.UriAvatarView
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.app.presentation.preview.contactItemForPreviews
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary

/**
 * View to show Node's owner information
 * @param contactItem of the owner
 * @param modifier
 */
@Composable
internal fun OwnerInfoView(
    contactItem: ContactItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OwnerAvatar(contactItem = contactItem)
        Column {
            Row {
                val ownerName = contactItem.contactData.alias
                    ?: contactItem.contactData.fullName
                    ?: contactItem.email
                val text = buildAnnotatedString {
                    append(ownerName)
                    withStyle(SpanStyle(color = MaterialTheme.colors.textColorSecondary)) {
                        append(" (${stringResource(R.string.file_properties_owner)})")
                    }
                }
                Text(
                    modifier = Modifier.testTag(TEST_TAG_OWNER_NAME),
                    text = text,
                    maxLines = 1,
                    style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.textColorPrimary),
                )
                Image(
                    modifier = Modifier.testTag(TEST_TAG_OWNER_STATUS),
                    painter = painterResource(id = contactItem.status.iconRes(MaterialTheme.colors.isLight)),
                    contentDescription = "Contact status"
                )
            }
            Text(
                modifier = Modifier.testTag(TEST_TAG_OWNER_EMAIL),
                text = contactItem.email,
                maxLines = 1,
                style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.grey_alpha_038_white_alpha_038),
            )
        }
    }
}

@Composable
private fun OwnerAvatar(
    contactItem: ContactItem,
    modifier: Modifier = Modifier,
) {
    val areCredentialsVerified = contactItem.areCredentialsVerified
    Box(modifier = modifier) {
        val avatarModifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = if (areCredentialsVerified) 16.dp else 8.dp
            )
            .size(40.dp)
            .clip(CircleShape)
        val avatarUri = contactItem.contactData.avatarUri
        if (avatarUri != null) {
            UriAvatarView(modifier = avatarModifier, uri = avatarUri)
        } else {
            DefaultAvatarView(
                modifier = avatarModifier,
                color = Color(contactItem.defaultAvatarColor?.toColorInt() ?: -1),
                content = contactItem.getAvatarFirstLetter(),
            )
        }
        if (areCredentialsVerified) {
            Image(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                painter = painterResource(id = IconR.drawable.ic_contact_verified),
                contentDescription = "Verified user"
            )
        }
    }
}

/**
 * Preview for [OwnerInfoView]
 */
@CombinedTextAndThemePreviews
@Composable
private fun IncomeSharedInfoPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        OwnerInfoView(contactItemForPreviews)
    }
}
