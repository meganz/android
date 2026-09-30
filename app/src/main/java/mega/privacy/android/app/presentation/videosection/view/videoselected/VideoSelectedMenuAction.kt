package mega.privacy.android.app.presentation.videosection.view.videoselected

import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithoutIcon
import mega.privacy.android.app.R

/**
 * Selection-mode actions offered by the video selection top bar.
 */
internal sealed interface VideoSelectedMenuAction : MenuAction {

    object SelectAll : MenuActionWithoutIcon(
        R.string.action_select_all,
        TEST_TAG_SELECT_ALL_ACTION,
    ), VideoSelectedMenuAction

    object ClearSelection : MenuActionWithoutIcon(
        R.string.action_unselect_all,
        TEST_TAG_CLEAR_SELECTION_ACTION,
    ), VideoSelectedMenuAction

    companion object {
        const val TEST_TAG_SELECT_ALL_ACTION = "video_selected_top_bar:select_all"
        const val TEST_TAG_CLEAR_SELECTION_ACTION = "video_selected_top_bar:clear_selection"
    }
}
