package mega.privacy.android.feature.sync.ui.exclusions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.test.weblate.WeblateContentDescriptionScreenshot
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Translator reference for the shared `general_delete` string, which is only ever spoken and so
 * has no visual of its own. The bar below is the Edit exclusion chrome the action first ships on.
 */
class WeblateDeleteMenuActionScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun DeleteMenuActionContentDescription() {
        AndroidThemeForPreviews {
            WeblateContentDescriptionScreenshot(
                description = stringResource(sharedR.string.general_delete),
                tooltipAlignment = Alignment.TopEnd,
                tooltipOffset = DpOffset(x = (-8).dp, y = 52.dp),
            ) {
                Column {
                    MegaTopAppBar(
                        title = "Edit exclusion",
                        navigationType = AppBarNavigationType.Back {},
                        actions = listOf(CommonMenuAction.Delete),
                        onActionPressed = {},
                    )
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }
}
