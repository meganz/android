package mega.privacy.android.feature.sync.ui.views

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The resolution list is the bottom sheet's content and the apply-to-all step is a dialog, so
 * they are rendered separately here — the same split the Nav3 destinations make.
 */
class StalledIssueResolutionScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun IssuesResolutionSheetContent() {
        AndroidThemeForPreviews {
            IssuesResolutionDialog(
                icon = IconPackR.drawable.ic_folder_medium_solid,
                conflictName = "Names would clash in the local folder",
                nodeName = "Competitors documentation",
                actions = listOf(
                    StalledIssueResolutionAction(
                        actionName = "Rename all items",
                        resolutionActionType = StalledIssueResolutionActionType.RENAME_ALL_ITEMS,
                    ),
                    StalledIssueResolutionAction(
                        actionName = "Merge folders",
                        resolutionActionType = StalledIssueResolutionActionType.MERGE_FOLDERS,
                    ),
                ),
                actionSelected = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ApplyToAllDialogWithOption() {
        AndroidThemeForPreviews {
            ApplyToAllDialog(
                fileName = "Competitors documentation",
                selectedAction = StalledIssueResolutionAction(
                    actionName = "Rename all items",
                    resolutionActionType = StalledIssueResolutionActionType.RENAME_ALL_ITEMS,
                ),
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                shouldShowApplyToAllOption = true,
            )
        }
    }

    /**
     * CHOOSE_LOCAL_FILE is the only covered path whose description carries an [A]…[/A] span, and
     * it is also the only one hitting the default confirm-button label.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ApplyToAllDialogSpannedDescription() {
        AndroidThemeForPreviews {
            ApplyToAllDialog(
                fileName = "Competitors documentation.pdf",
                selectedAction = StalledIssueResolutionAction(
                    actionName = "Choose local file",
                    resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE,
                ),
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                shouldShowApplyToAllOption = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ApplyToAllDialogCheckboxChecked() {
        AndroidThemeForPreviews {
            ApplyToAllDialog(
                title = "Choose the local file?",
                description = "The file on MEGA will be replaced with [A]Competitors documentation.pdf.[/A] The existing version will be moved to the SyncDebris folder.",
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                actionButtonStringRes = sharedR.string.general_dialog_choose_button,
                isApplyToAllChecked = true,
                onApplyToAllCheckedChange = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ApplyToAllDialogSingleIssue() {
        AndroidThemeForPreviews {
            ApplyToAllDialog(
                fileName = "Competitors documentation",
                selectedAction = StalledIssueResolutionAction(
                    actionName = "Merge folders",
                    resolutionActionType = StalledIssueResolutionActionType.MERGE_FOLDERS,
                ),
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                shouldShowApplyToAllOption = false,
            )
        }
    }
}
