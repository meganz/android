package mega.privacy.android.feature.contact.info.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Contact-related option rows of the contact info screen: nickname, verify credentials,
 * share contact and shared folders.
 *
 * @param nickname current nickname, or null when none is set (the row then offers to set one).
 * @param areCredentialsVerified drives the subtitle and verified icon of the credentials row.
 * @param showVerifyCredentials whether the verify credentials row is visible.
 * @param showShareContact whether the share contact row is visible.
 * @param showSharedFolders whether the shared folders row is visible.
 * @param sharedFoldersCount number of folders shared by the contact; the row is only clickable
 * when there is at least one.
 * @param onNicknameClick
 * @param onVerifyCredentialsClick
 * @param onShareContactClick
 * @param onSharedFoldersClick
 * @param modifier
 */
@Composable
internal fun ContactInfoOptionsSection(
    nickname: String?,
    areCredentialsVerified: Boolean,
    showVerifyCredentials: Boolean,
    showShareContact: Boolean,
    showSharedFolders: Boolean,
    sharedFoldersCount: Int,
    onNicknameClick: () -> Unit,
    onVerifyCredentialsClick: () -> Unit,
    onShareContactClick: () -> Unit,
    onSharedFoldersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag(CONTACT_INFO_OPTIONS_SECTION_TAG)) {
        FlexibleLineListItem(
            modifier = Modifier.testTag(CONTACT_INFO_NICKNAME_ROW_TAG),
            title = stringResource(
                if (nickname == null) {
                    sharedR.string.contact_info_set_nickname
                } else {
                    sharedR.string.contact_info_edit_nickname
                }
            ),
            subtitle = nickname,
            onClickListener = onNicknameClick,
        )
        if (showVerifyCredentials) {
            SubtleDivider()
            FlexibleLineListItem(
                modifier = Modifier.testTag(CONTACT_INFO_VERIFY_CREDENTIALS_ROW_TAG),
                title = stringResource(sharedR.string.contact_info_verify_credentials),
                subtitle = stringResource(
                    if (areCredentialsVerified) {
                        sharedR.string.contact_info_credentials_verified
                    } else {
                        sharedR.string.contact_info_credentials_not_verified
                    }
                ),
                trailingElement = if (areCredentialsVerified) {
                    {
                        Image(
                            painter = painterResource(iconPackR.drawable.ic_contact_verified),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag(CONTACT_INFO_CREDENTIALS_VERIFIED_ICON_TAG),
                        )
                    }
                } else {
                    null
                },
                onClickListener = onVerifyCredentialsClick,
            )
        }
        if (showShareContact) {
            SubtleDivider()
            OneLineListItem(
                modifier = Modifier.testTag(CONTACT_INFO_SHARE_CONTACT_ROW_TAG),
                text = stringResource(sharedR.string.contact_info_share_contact),
                onClickListener = onShareContactClick,
            )
        }
        if (showSharedFolders) {
            SubtleDivider()
            FlexibleLineListItem(
                modifier = Modifier.testTag(CONTACT_INFO_SHARED_FOLDERS_ROW_TAG),
                title = stringResource(sharedR.string.general_title_incoming_shares),
                subtitle = pluralStringResource(
                    sharedR.plurals.num_of_folders_with_parameter,
                    sharedFoldersCount,
                    sharedFoldersCount,
                ),
                enableClick = sharedFoldersCount > 0,
                onClickListener = onSharedFoldersClick,
            )
        }
    }
}

internal const val CONTACT_INFO_OPTIONS_SECTION_TAG = "contact_info_options_section"
internal const val CONTACT_INFO_NICKNAME_ROW_TAG = "contact_info_options_section:row_nickname"
internal const val CONTACT_INFO_VERIFY_CREDENTIALS_ROW_TAG =
    "contact_info_options_section:row_verify_credentials"
internal const val CONTACT_INFO_CREDENTIALS_VERIFIED_ICON_TAG =
    "contact_info_options_section:icon_credentials_verified"
internal const val CONTACT_INFO_SHARE_CONTACT_ROW_TAG =
    "contact_info_options_section:row_share_contact"
internal const val CONTACT_INFO_SHARED_FOLDERS_ROW_TAG =
    "contact_info_options_section:row_shared_folders"

@CombinedThemePreviews
@Composable
private fun ContactInfoOptionsSectionPreview() {
    AndroidThemeForPreviews {
        ContactInfoOptionsSection(
            nickname = "Ally",
            areCredentialsVerified = true,
            showVerifyCredentials = true,
            showShareContact = true,
            showSharedFolders = true,
            sharedFoldersCount = 3,
            onNicknameClick = {},
            onVerifyCredentialsClick = {},
            onShareContactClick = {},
            onSharedFoldersClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContactInfoOptionsSectionUnverifiedPreview() {
    AndroidThemeForPreviews {
        ContactInfoOptionsSection(
            nickname = null,
            areCredentialsVerified = false,
            showVerifyCredentials = true,
            showShareContact = false,
            showSharedFolders = false,
            sharedFoldersCount = 0,
            onNicknameClick = {},
            onVerifyCredentialsClick = {},
            onShareContactClick = {},
            onSharedFoldersClick = {},
        )
    }
}
