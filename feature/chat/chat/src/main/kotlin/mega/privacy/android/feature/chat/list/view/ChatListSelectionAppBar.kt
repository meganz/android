package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import java.util.Locale

/**
 * Contextual top app bar shown while the chat list is in multi-select mode: the close action
 * exits selection, the title shows the selected [count], and the actions expose select-all and the
 * bulk actions overflow.
 *
 * @param count Number of currently selected chats.
 * @param isAllSelected Whether every selectable chat is already selected.
 * @param onSelectAllClick Called when the select-all action is pressed.
 * @param onCancelClick Called when selection mode is dismissed.
 * @param onMoreClick Called when the bulk actions overflow is pressed.
 * @param modifier [Modifier]
 */
@Composable
internal fun ChatListSelectionAppBar(
    count: Int,
    isAllSelected: Boolean,
    onSelectAllClick: () -> Unit,
    onCancelClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaTopAppBar(
        modifier = modifier.testTag(CHAT_LIST_SELECTION_BAR_TAG),
        navigationType = AppBarNavigationType.Close(onCancelClick),
        title = String.format(Locale.getDefault(), "%d", count),
        actions = buildList {
            if (!isAllSelected) add(CommonMenuAction.SelectAll)
            add(CommonMenuAction.More)
        },
        onActionPressed = { action ->
            when (action) {
                is CommonMenuAction.SelectAll -> onSelectAllClick()
                is CommonMenuAction.More -> onMoreClick()
                else -> Unit
            }
        },
    )
}

internal const val CHAT_LIST_SELECTION_BAR_TAG = "chat_list_screen:selection_bar"

@CombinedThemePreviews
@Composable
private fun ChatListSelectionAppBarPreview() {
    AndroidThemeForPreviews {
        ChatListSelectionAppBar(
            count = 3,
            isAllSelected = false,
            onSelectAllClick = {},
            onCancelClick = {},
            onMoreClick = {},
        )
    }
}
