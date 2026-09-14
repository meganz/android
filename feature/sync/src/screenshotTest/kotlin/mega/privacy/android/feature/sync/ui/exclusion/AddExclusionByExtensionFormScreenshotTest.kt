package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByExtensionFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionExtensionError

/**
 * The form body is pinned rather than the whole Add screen, whose chrome is not built yet.
 */
class AddExclusionByExtensionFormScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionByExtensionFormEmpty() {
        AndroidThemeForPreviews {
            Form(AddExclusionByExtensionFormState())
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionByExtensionFormWithOneExtension() {
        AndroidThemeForPreviews {
            Form(AddExclusionByExtensionFormState(value = "log"))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionByExtensionFormWithTwoExtensions() {
        AndroidThemeForPreviews {
            Form(AddExclusionByExtensionFormState(value = "mp4, zip"))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionByExtensionFormWithThreeExtensions() {
        AndroidThemeForPreviews {
            Form(AddExclusionByExtensionFormState(value = "log, mp4, mp3"))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionByExtensionFormWithInvalidError() {
        AndroidThemeForPreviews {
            Form(
                AddExclusionByExtensionFormState(
                    value = "my doc",
                    error = AddExclusionExtensionError.Invalid,
                )
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionByExtensionFormWithDuplicateError() {
        AndroidThemeForPreviews {
            Form(
                AddExclusionByExtensionFormState(
                    value = "log, mp3",
                    error = AddExclusionExtensionError.Duplicate("mp3"),
                )
            )
        }
    }

    @Composable
    private fun Form(state: AddExclusionByExtensionFormState) {
        AddExclusionByExtensionForm(
            state = state,
            onTypeOptionSelected = {},
            onValueChanged = {},
        )
    }
}
