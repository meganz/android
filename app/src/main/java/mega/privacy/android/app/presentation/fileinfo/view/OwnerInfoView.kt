package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.ContactAvatarVerified
import mega.privacy.android.app.presentation.contact.view.contactItemForPreviews
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.contacts.ContactItem

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
        ContactAvatarVerified(contactItem)
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

/**
 * Preview for [OwnerInfoView]
 */
@CombinedTextAndThemePreviews
@Composable
private fun IncomeSharedInfoPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        OwnerInfoView(contactItemForPreviews)
    }
}