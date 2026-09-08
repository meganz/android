package mega.privacy.android.feature.myaccount.presentation.achievements.freetrial.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Screenshot tests for the MEGA Pass free-trial screen, covering the states the user can reach:
 * the "how it works" offer before the award is granted, the awarded state with days remaining,
 * and the awarded state once the trial has expired. All label strings are resolved from the
 * shared resources by the screen itself.
 */
class MegaPassFreeTrialScreenshotTest {

    private val storageInBytes = 5L * 1024 * 1024 * 1024

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NotYetAwarded() {
        OriginalTheme(isDark = isSystemInDarkTheme()) {
            MegaPassFreeTrialScreen(
                isReceivedAward = false,
                storageAmount = storageInBytes,
                awardStorageAmount = 0,
                durationInDays = 90,
                awardDaysLeft = null,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AwardedWithDaysLeft() {
        OriginalTheme(isDark = isSystemInDarkTheme()) {
            MegaPassFreeTrialScreen(
                isReceivedAward = true,
                storageAmount = storageInBytes,
                awardStorageAmount = storageInBytes,
                durationInDays = 90,
                awardDaysLeft = 30,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AwardedExpired() {
        OriginalTheme(isDark = isSystemInDarkTheme()) {
            MegaPassFreeTrialScreen(
                isReceivedAward = true,
                storageAmount = storageInBytes,
                awardStorageAmount = storageInBytes,
                durationInDays = 90,
                awardDaysLeft = 0,
            )
        }
    }
}
