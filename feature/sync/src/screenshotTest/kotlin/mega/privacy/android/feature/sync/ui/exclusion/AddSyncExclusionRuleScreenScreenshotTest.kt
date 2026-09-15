package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionTab
import mega.privacy.android.feature.sync.ui.model.exclusion.AddSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption

/**
 * The chrome both tabs share, pinned once per tab. The form bodies have their own tests, so only
 * the states that change the chrome are kept here.
 */
class AddSyncExclusionRuleScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddSyncExclusionRuleScreenByNameTab() {
        AndroidThemeForPreviews {
            Screen(AddSyncExclusionRuleUiState())
        }
    }




    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddSyncExclusionRuleScreenBySizeTab() {
        AndroidThemeForPreviews {
            Screen(AddSyncExclusionRuleUiState(selectedTab = AddExclusionTab.BySize))
        }
    }

    @Composable
    private fun Screen(uiState: AddSyncExclusionRuleUiState) {
        AddSyncExclusionRuleScreen(
            uiState = uiState,
            onTabSelected = {},
            onTypeOptionSelected = {},
            onConditionSelected = {},
            onNameValueChanged = {},
            onExtensionValueChanged = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
