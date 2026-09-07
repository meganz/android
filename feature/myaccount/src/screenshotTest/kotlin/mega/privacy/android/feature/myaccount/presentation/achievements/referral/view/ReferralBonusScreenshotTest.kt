package mega.privacy.android.feature.myaccount.presentation.achievements.referral.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.feature.myaccount.presentation.achievements.referral.model.ReferralBonusesUIState
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Screenshot test for the referral bonuses screen. The two rows surface both the "days left"
 * ([general_num_days_left]) and "Expired" ([expired_label]) states, and the toolbar shows the
 * [title_referral_bonuses] title.
 */
class ReferralBonusScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ReferralBonusList() {
        OriginalTheme(isDark = false) {
            ReferralBonusView(
                uiState = ReferralBonusesUIState(
                    awardedInviteAchievements = listOf(
                        referral(email = "alice@example.com", expirationInDays = 30L, expirationTimestampInSeconds = 100),
                        referral(email = "bob@example.com", expirationInDays = 0L, expirationTimestampInSeconds = 0),
                    )
                )
            )
        }
    }

    private fun referral(
        email: String,
        expirationInDays: Long,
        expirationTimestampInSeconds: Long,
    ) = ReferralBonusAchievements(
        contact = ContactItem(
            handle = email.hashCode().toLong(),
            email = email,
            contactData = ContactData(
                fullName = email.substringBefore('@').replaceFirstChar { it.uppercase() },
                alias = null,
                avatarUri = null,
                userVisibility = UserVisibility.Visible,
            ),
            defaultAvatarColor = "#4285F4",
            visibility = UserVisibility.Visible,
            timestamp = 1231231,
            areCredentialsVerified = true,
            status = UserChatStatus.Online,
            lastSeen = null,
            chatroomId = null,
        ),
        expirationInDays = expirationInDays,
        awardId = email.hashCode(),
        expirationTimestampInSeconds = expirationTimestampInSeconds,
        rewardedStorageInBytes = 51283102,
        rewardedTransferInBytes = 12381273,
        referredEmails = listOf(email),
    )
}
