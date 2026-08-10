package mega.privacy.mobile.home.presentation.recents.view

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecentsOptionsBottomSheet(
    isVisible: Boolean,
    isHideRecentsEnabled: Boolean,
    onShowRecentActivity: () -> Unit,
    onHideRecentActivity: () -> Unit,
    onClearRecentActivity: () -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val optionsBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var clearConfirmationDialogVisible by rememberSaveable { mutableStateOf(false) }

    if (isVisible) {
        MegaModalBottomSheet(
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
            sheetState = optionsBottomSheetState,
            onDismissRequest = {
                onDismiss()
            },
        ) {
            RecentsOptionsBottomSheetContent(
                isHideRecentsEnabled = isHideRecentsEnabled,
                onShowRecentActivity = {
                    coroutineScope
                        .launch { optionsBottomSheetState.hide() }
                        .invokeOnCompletion {
                            onDismiss()
                            onShowRecentActivity()
                        }
                },
                onHideRecentActivity = {
                    coroutineScope
                        .launch { optionsBottomSheetState.hide() }
                        .invokeOnCompletion {
                            onDismiss()
                            onHideRecentActivity()
                        }
                },
                onClearRecentActivity = {
                    coroutineScope
                        .launch { optionsBottomSheetState.hide() }
                        .invokeOnCompletion {
                            onDismiss()
                            clearConfirmationDialogVisible = true
                        }
                }
            )
        }
    }

    if (clearConfirmationDialogVisible) {
        BasicDialog(
            modifier = Modifier.testTag(CLEAR_RECENT_DIALOG_TEST_TAG),
            title = stringResource(sharedR.string.home_recents_options_menu_clear),
            description = stringResource(sharedR.string.home_recents_clear_dialog_message),
            positiveButtonText = stringResource(id = sharedR.string.general_clear),
            negativeButtonText = stringResource(id = sharedR.string.general_dismiss_dialog),
            onPositiveButtonClicked = {
                onClearRecentActivity()
                clearConfirmationDialogVisible = false
            },
            onNegativeButtonClicked = {
                clearConfirmationDialogVisible = false
            }
        )
    }
}

@Composable
internal fun RecentsOptionsBottomSheetContent(
    isHideRecentsEnabled: Boolean,
    onShowRecentActivity: () -> Unit,
    onHideRecentActivity: () -> Unit,
    onClearRecentActivity: () -> Unit,
) {
    Column {
        if (isHideRecentsEnabled) {
            NodeActionListTile(
                text = stringResource(sharedR.string.home_recents_options_menu_show_activity),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Eye),
                onActionClicked = onShowRecentActivity,
                modifier = Modifier.testTag(SHOW_RECENT_MENU_ITEM_TEST_TAG)
            )
        } else {
            NodeActionListTile(
                text = stringResource(sharedR.string.home_recents_options_menu_hide_activity),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.EyeOff),
                onActionClicked = onHideRecentActivity,
                modifier = Modifier.testTag(HIDE_RECENT_MENU_ITEM_TEST_TAG)
            )
        }

        NodeActionListTile(
            text = stringResource(sharedR.string.home_recents_options_menu_clear),
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Eraser),
            onActionClicked = onClearRecentActivity,
            modifier = Modifier.testTag(CLEAR_RECENT_MENU_ITEM_TEST_TAG)
        )
    }
}

internal const val HIDE_RECENT_MENU_ITEM_TEST_TAG = "recents_widget:hide_recent_menu_item"
internal const val SHOW_RECENT_MENU_ITEM_TEST_TAG = "recents_widget:show_recent_menu_item"
internal const val CLEAR_RECENT_MENU_ITEM_TEST_TAG = "recents_widget:clear_recent_menu_item"
internal const val CLEAR_RECENT_DIALOG_TEST_TAG = "recents_widget:clear_recentdialog"

@CombinedThemePreviews
@Composable
private fun RecentsOptionsBottomSheetHidePreview() {
    AndroidThemeForPreviews {
        RecentsOptionsBottomSheet(
            isVisible = true,
            isHideRecentsEnabled = false,
            onShowRecentActivity = {},
            onHideRecentActivity = {},
            onClearRecentActivity = {},
            onDismiss = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentsOptionsBottomSheetShowPreview() {
    AndroidThemeForPreviews {
        RecentsOptionsBottomSheet(
            isVisible = true,
            isHideRecentsEnabled = true,
            onShowRecentActivity = {},
            onHideRecentActivity = {},
            onClearRecentActivity = {},
            onDismiss = {}
        )
    }
}

