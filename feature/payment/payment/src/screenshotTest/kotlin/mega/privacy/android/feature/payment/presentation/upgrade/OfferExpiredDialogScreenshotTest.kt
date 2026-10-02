package mega.privacy.android.feature.payment.presentation.upgrade

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot test for [OfferExpiredDialog]. It is shown from internal state of
 * [UpgradeAccountScreen] that no screen-level golden can reach, so it is covered on its own.
 */
class OfferExpiredDialogScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun OfferExpired() {
        AndroidThemeForPreviews {
            OfferExpiredDialog(onDismiss = {})
        }
    }
}
