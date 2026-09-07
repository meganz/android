package mega.privacy.android.feature.myaccount.presentation.achievements.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import de.palm.composestateevents.consumed
import mega.privacy.android.feature.myaccount.presentation.achievements.model.AchievementsUIState
import mega.privacy.android.feature.myaccount.presentation.achievements.model.AwardAchievementExpirationStatus
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Screenshot tests for the achievements overview screen, covering the reward rows that surface its
 * moved shared strings: the unlocked-rewards header ([unlocked_rewards_title] / [unlocked_storage_title]
 * / the storage-quota units), the invite bonus, mobile/desktop install, signup bonus, and both free
 * trial rows.
 */
class AchievementsOverviewScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AchievementsOverviewWithReferrals() {
        OriginalTheme(isDark = false) {
            AchievementsOverviewView(
                uiState = AchievementsUIState(
                    achievementsOverview = null,
                    errorMessage = consumed(),
                    currentStorage = 1337000000000,
                    hasReferrals = true,
                    areAllRewardsExpired = false,
                    referralsStorage = 123,
                    referralsAwardStorage = 1024000000,
                    installAppStorage = 123,
                    installAppAwardDaysLeft = 0,
                    installAppAwardStorage = 12,
                    hasRegistrationAward = true,
                    registrationAwardDaysLeft = 0,
                    registrationAwardStorage = 12,
                    installDesktopStorage = 123,
                    installDesktopAwardDaysLeft = 10,
                    installDesktopAwardStorage = 0,
                    hasMegaVPNTrial = true,
                    hasMegaPassTrial = true,
                    megaPassTrialStorage = 1024 * 1024 * 1024,
                    megaPassTrialAwardStorage = 0,
                    megaVPNTrialStorage = 1024 * 1024 * 1024,
                    megaVPNTrialAwardStorage = 0,
                    referralsDurationInDays = 365,
                    installAppDurationInDays = 365,
                    installDesktopDurationInDays = 365,
                    megaPassTrialDurationInDays = 0,
                    megaVPNTrialDurationInDays = 365,
                    megaVPNTrialAwardDaysLeft = null,
                    megaPassTrialAwardDaysLeft = null,
                )
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AchievementsOverviewAwardedTrials() {
        OriginalTheme(isDark = false) {
            AchievementsOverviewView(
                uiState = AchievementsUIState(
                    achievementsOverview = null,
                    errorMessage = consumed(),
                    currentStorage = 13376969,
                    hasReferrals = false,
                    areAllRewardsExpired = false,
                    referralsStorage = 0,
                    referralsAwardStorage = 123000,
                    installAppStorage = null,
                    installAppAwardDaysLeft = 33,
                    installAppAwardStorage = 12,
                    hasRegistrationAward = false,
                    registrationAwardDaysLeft = 333,
                    registrationAwardStorage = 12,
                    installDesktopStorage = null,
                    installDesktopAwardDaysLeft = 14,
                    installDesktopAwardStorage = 12,
                    hasMegaVPNTrial = true,
                    hasMegaPassTrial = true,
                    megaPassTrialStorage = 5L * 1024 * 1024 * 1024,
                    megaPassTrialAwardStorage = 5L * 1024 * 1024 * 1024,
                    megaVPNTrialStorage = 5L * 1024 * 1024 * 1024,
                    megaVPNTrialAwardStorage = 5L * 1024 * 1024 * 1024,
                    referralsDurationInDays = 365,
                    installAppDurationInDays = 365,
                    installDesktopDurationInDays = 365,
                    megaPassTrialDurationInDays = 365,
                    megaVPNTrialDurationInDays = 365,
                    megaVPNTrialAwardDaysLeft = AwardAchievementExpirationStatus.Permanent,
                    megaPassTrialAwardDaysLeft = AwardAchievementExpirationStatus.Valid(100),
                )
            )
        }
    }
}
