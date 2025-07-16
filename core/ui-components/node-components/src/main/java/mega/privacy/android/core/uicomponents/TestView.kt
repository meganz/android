package mega.privacy.android.core.uicomponents

import androidx.compose.runtime.Composable
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor

@Composable
private fun TestView() {
    MegaText(
        text = "Test View",
        textColor = TextColor.Primary,
    )
}

@CombinedThemePreviews
@Composable
private fun TestViewPreview() {
    AndroidThemeForPreviews {
        TestView()
    }
}