package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Screenshot test for the plain/rich editing mode menu. The live menu is a popup, which the
 * screenshot engine cannot capture, so the rows are rendered in the same container the popup
 * gives them. Provides Weblate translators with the context for the menu's strings.
 */
class MarkdownEditModeMenuScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun EditModeMenu() {
        AndroidThemeForPreviews {
            Surface(
                shape = DSTokens.shapes.medium,
                color = DSTokens.colors.background.surface1,
                modifier = Modifier.padding(8.dp),
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    MarkdownEditModeMenuContent(isRichText = true, onSelectRichText = {})
                }
            }
        }
    }
}
