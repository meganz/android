package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The form body is pinned rather than the whole Add screen, whose chrome is not built yet.
 */
class AddExclusionByNameFormScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionByNameFormEmpty() {
        AndroidThemeForPreviews {
            Form(AddExclusionByNameFormState())
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionByNameFormWithTypeOnly() {
        AndroidThemeForPreviews {
            Form(AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Files))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionByNameFormWithNameEntered() {
        AndroidThemeForPreviews {
            Form(
                AddExclusionByNameFormState(
                    typeOption = SyncExclusionTypeOption.Folders,
                    condition = SyncExclusionCondition.BeginningWith,
                    value = "temp",
                )
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionByNameFormWithWildcardHelper() {
        AndroidThemeForPreviews {
            Form(
                AddExclusionByNameFormState(
                    typeOption = SyncExclusionTypeOption.FilesAndFolders,
                    condition = SyncExclusionCondition.MatchingWildcard,
                    value = "*.tmp",
                )
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionByNameFormWithDuplicateError() {
        AndroidThemeForPreviews {
            Form(
                AddExclusionByNameFormState(
                    typeOption = SyncExclusionTypeOption.Files,
                    condition = SyncExclusionCondition.MatchingExactly,
                    value = "notes.txt",
                    errorRes = sharedR.string.sync_exclusions_add_name_duplicate_error,
                )
            )
        }
    }

    @Composable
    private fun Form(state: AddExclusionByNameFormState) {
        AddExclusionByNameForm(
            state = state,
            onTypeOptionSelected = {},
            onConditionSelected = {},
            onValueChanged = {},
        )
    }
}
