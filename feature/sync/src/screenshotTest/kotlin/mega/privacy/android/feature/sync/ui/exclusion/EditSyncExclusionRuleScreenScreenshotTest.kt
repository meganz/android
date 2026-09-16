package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.EditSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption

/**
 * The Edit chrome: the title, the trash action and the Cancel/Save bar around a prefilled form.
 * The form body has its own tests, so only a prefilled state is kept here.
 */
class EditSyncExclusionRuleScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun EditSyncExclusionRuleScreenPrefilled() {
        AndroidThemeForPreviews {
            Screen(
                EditSyncExclusionRuleUiState(
                    byName = AddExclusionByNameFormState(
                        typeOption = SyncExclusionTypeOption.Folders,
                        condition = SyncExclusionCondition.Containing,
                        value = "temp",
                    ),
                )
            )
        }
    }

    @Composable
    private fun Screen(uiState: EditSyncExclusionRuleUiState) {
        EditSyncExclusionRuleScreen(
            uiState = uiState,
            onTypeOptionSelected = {},
            onConditionSelected = {},
            onValueChanged = {},
            onDelete = {},
            onSave = {},
            onDismiss = {},
        )
    }
}
