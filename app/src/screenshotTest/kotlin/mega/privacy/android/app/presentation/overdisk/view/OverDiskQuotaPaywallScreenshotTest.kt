package mega.privacy.android.app.presentation.overdisk.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.presentation.overdisk.model.OverDiskQuotaPaywallUiState

/**
 * Baselines for [OverDiskQuotaPaywallScreen], one per body variant: no warning email yet,
 * a single warning email, and several of them.
 *
 * Every variant leaves the deadline unset so the countdown falls back to its static
 * "you have to upgrade" wording — a real deadline is rendered as the time left until it,
 * which would make the baseline change every day.
 */
class OverDiskQuotaPaywallScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun OverDiskQuotaPaywallScreenNoWarningDates() {
        AndroidThemeForPreviews {
            OverDiskQuotaPaywallScreen(
                uiState = uiState(warningTimestamps = emptyList()),
                onDismiss = {},
                onUpgrade = {},
                onLearnMore = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun OverDiskQuotaPaywallScreenSingleWarningDate() {
        AndroidThemeForPreviews {
            OverDiskQuotaPaywallScreen(
                uiState = uiState(warningTimestamps = listOf(FIRST_WARNING)),
                onDismiss = {},
                onUpgrade = {},
                onLearnMore = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun OverDiskQuotaPaywallScreenMultipleWarningDates() {
        AndroidThemeForPreviews {
            OverDiskQuotaPaywallScreen(
                uiState = uiState(
                    warningTimestamps = listOf(FIRST_WARNING, SECOND_WARNING, THIRD_WARNING),
                ),
                onDismiss = {},
                onUpgrade = {},
                onLearnMore = {},
            )
        }
    }

    private fun uiState(warningTimestamps: List<Long>) = OverDiskQuotaPaywallUiState(
        isLoading = false,
        email = "user@mega.co.nz",
        fileCount = 1280,
        usedStorage = 53_687_091_200L,
        warningTimestamps = warningTimestamps,
        deadlineTimestamp = -1,
    )

    private companion object {
        const val FIRST_WARNING = 1_700_000_000L
        const val SECOND_WARNING = 1_700_604_800L
        const val THIRD_WARNING = 1_701_209_600L
    }
}
