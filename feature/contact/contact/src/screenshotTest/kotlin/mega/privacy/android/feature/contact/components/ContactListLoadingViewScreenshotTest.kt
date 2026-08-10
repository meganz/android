package mega.privacy.android.feature.contact.components

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

class ContactListLoadingViewScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactListLoadingViewDefault() {
        AndroidThemeForPreviews {
            ContactListLoadingView()
        }
    }
}
