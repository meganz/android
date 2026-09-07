package mega.privacy.android.feature.myaccount.presentation.achievements.info.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.feature.myaccount.presentation.achievements.info.model.AchievementsInfoUIState
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Screenshot tests for the achievements info screen, covering the states that surface each of its
 * shared strings: the awarded "result" paragraphs for the mobile, desktop and registration
 * achievements, the "days left" bonus-expiration title ([account_achievements_bonus_expiration_date]),
 * and the "Expired" title ([expired_label]). The "How it works" heading
 * ([title_achievement_invite_friends]) appears in every state.
 */
class AchievementsInfoScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MobileInstallAwarded() {
        OriginalTheme(isDark = false) {
            AchievementsInfoView(
                uiState = AchievementsInfoUIState(
                    awardId = -1,
                    achievementType = AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL,
                    achievementRemainingDays = 16,
                    awardStorageInBytes = 1L * 1024 * 1024 * 1024,
                    isAchievementAwarded = true,
                    isAchievementExpired = false,
                    isAchievementAlmostExpired = false,
                    durationInDays = 365,
                )
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun DesktopInstallAwarded() {
        OriginalTheme(isDark = false) {
            AchievementsInfoView(
                uiState = AchievementsInfoUIState(
                    awardId = -1,
                    achievementType = AchievementType.MEGA_ACHIEVEMENT_DESKTOP_INSTALL,
                    achievementRemainingDays = 30,
                    awardStorageInBytes = 2L * 1024 * 1024 * 1024,
                    isAchievementAwarded = true,
                    isAchievementExpired = false,
                    isAchievementAlmostExpired = false,
                    durationInDays = 365,
                )
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RegistrationExpired() {
        OriginalTheme(isDark = false) {
            AchievementsInfoView(
                uiState = AchievementsInfoUIState(
                    awardId = -1,
                    achievementType = AchievementType.MEGA_ACHIEVEMENT_WELCOME,
                    achievementRemainingDays = 0,
                    awardStorageInBytes = 5L * 1024 * 1024 * 1024,
                    isAchievementAwarded = true,
                    isAchievementExpired = true,
                    isAchievementAlmostExpired = true,
                    durationInDays = 365,
                )
            )
        }
    }
}
