package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import mega.privacy.android.feature.sync.ui.model.exclusion.labelRes
import mega.privacy.android.feature.sync.ui.views.SyncSingleSelectOption
import mega.privacy.android.feature.sync.ui.views.SyncSingleSelectSheetContent
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The sheet bodies are pinned rather than the modal sheets, which need a real window the screenshot
 * harness does not provide.
 */
class AddExclusionPickerSheetsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionTypeSheet() {
        AndroidThemeForPreviews {
            SyncSingleSelectSheetContent(
                title = stringResource(sharedR.string.sync_exclusions_add_type),
                options = SyncExclusionTypeOption.entries.map {
                    SyncSingleSelectOption(value = it, label = stringResource(it.labelRes))
                },
                selectedValue = SyncExclusionTypeOption.Files,
                onOptionSelected = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionConditionSheet() {
        AndroidThemeForPreviews {
            SyncSingleSelectSheetContent(
                title = stringResource(sharedR.string.sync_exclusions_add_condition),
                options = SyncExclusionCondition.entries.map {
                    SyncSingleSelectOption(value = it, label = stringResource(it.labelRes))
                },
                selectedValue = SyncExclusionCondition.MatchingWildcard,
                onOptionSelected = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionSizeConditionSheet() {
        AndroidThemeForPreviews {
            SyncSingleSelectSheetContent(
                title = stringResource(sharedR.string.sync_exclusions_add_size_condition_title),
                options = SyncSizeCondition.entries.map {
                    SyncSingleSelectOption(value = it, label = stringResource(it.labelRes))
                },
                selectedValue = SyncSizeCondition.NotBetween,
                onOptionSelected = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionSizeUnitSheet() {
        AndroidThemeForPreviews {
            SyncSingleSelectSheetContent(
                title = stringResource(sharedR.string.sync_exclusions_add_size_unit_title),
                options = SyncSizeUnit.entries.map {
                    SyncSingleSelectOption(value = it, label = stringResource(it.labelRes))
                },
                selectedValue = SyncSizeUnit.Megabytes,
                onOptionSelected = {},
            )
        }
    }
}
