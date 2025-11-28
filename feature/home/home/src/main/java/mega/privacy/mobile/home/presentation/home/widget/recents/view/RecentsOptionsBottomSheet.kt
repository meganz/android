package mega.privacy.mobile.home.presentation.home.widget.recents.view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.icon.pack.IconPack

// TODO: Add all strings to resources/transifex once confirmed
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecentsOptionsBottomSheet(
    isVisible: Boolean,
    isHideRecentsEnabled: Boolean,
    onShowRecentActivity: () -> Unit,
    onHideRecentActivity: () -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val optionsBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (isVisible) {
        MegaModalBottomSheet(
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
            sheetState = optionsBottomSheetState,
            onDismissRequest = {
                onDismiss()
            }
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
            )
        }
    }
}

@Composable
internal fun RecentsOptionsBottomSheetContent(
    isHideRecentsEnabled: Boolean,
    onShowRecentActivity: () -> Unit,
    onHideRecentActivity: () -> Unit,
) {
    if (isHideRecentsEnabled) {
        NodeActionListTile(
            text = "Show recent activity",
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Eye),
            onActionClicked = onShowRecentActivity,
            modifier = Modifier.testTag(SHOW_RECENT_MENU_ITEM_TEST_TAG)
        )
    } else {
        NodeActionListTile(
            text = "Hide recent activity",
            icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.EyeOff),
            onActionClicked = onHideRecentActivity,
            modifier = Modifier.testTag(HIDE_RECENT_MENU_ITEM_TEST_TAG)
        )
    }
}

internal const val HIDE_RECENT_MENU_ITEM_TEST_TAG = "recents_widget:hide_recent_menu_item"
internal const val SHOW_RECENT_MENU_ITEM_TEST_TAG = "recents_widget:show_recent_menu_item"

@CombinedThemePreviews
@Composable
private fun RecentsOptionsBottomSheetHidePreview() {
    AndroidThemeForPreviews {
        RecentsOptionsBottomSheet(
            isVisible = true,
            isHideRecentsEnabled = false,
            onShowRecentActivity = {},
            onHideRecentActivity = {},
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
            onDismiss = {}
        )
    }
}

