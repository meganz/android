import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

@Composable
fun TestView() {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(DSTokens.colors.button.brand)
    ) {
        MegaText(
            text = "Snowflake Test View",
            modifier = Modifier
                .size(100.dp)
                .background(DSTokens.colors.background.surface1),
            textColor = TextColor.Primary
        )
    }
}

@CombinedThemePreviews
@Composable
private fun TestViewPreview() {
    AndroidThemeForPreviews {
        TestView()
    }
}