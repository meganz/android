package mega.privacy.android.feature.myaccount.presentation.achievements.invites.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.privacy.android.feature.myaccount.presentation.achievements.invites.model.InviteFriendsUIState
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Screenshot tests for the invite-friends achievement screen and its confirmation dialog, covering
 * each of the shared strings introduced by the migration: the invite-contacts button
 * ([invite_contacts]), the two explainer paragraphs ([first_paragraph_achievement_invite_friends]
 * and [second_paragraph_achievement_invite_friends]) on the screen, and the dialog's title
 * ([subtitle_confirmation_invite_friends]), the single- and multi-invite bodies
 * ([invite_sent_text] / [invite_sent_text_multi]) and its close button ([general_close]).
 */
class InviteFriendsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteFriendsScreen() {
        OriginalTheme(isDark = false) {
            InviteFriendsView(
                uiState = InviteFriendsUIState(
                    grantStorageInBytes = 5L * 1024 * 1024 * 1024,
                    durationInDays = 365,
                ),
                sentInvitesCount = null,
                onInviteClick = {},
                onConfirmationDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteSentConfirmationDialogSingle() {
        OriginalTheme(isDark = false) {
            InviteConfirmationDialog(
                isDialogVisible = true,
                description = sharedR.string.invite_sent_text,
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun InviteSentConfirmationDialogMulti() {
        OriginalTheme(isDark = false) {
            InviteConfirmationDialog(
                isDialogVisible = true,
                description = sharedR.string.invite_sent_text_multi,
                onDismiss = {},
            )
        }
    }
}
