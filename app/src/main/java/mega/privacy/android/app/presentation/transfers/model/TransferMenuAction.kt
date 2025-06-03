package mega.privacy.android.app.presentation.transfers.model

import mega.android.core.ui.model.TopAppBarAction
import mega.android.core.ui.model.TopAppBarActionString
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.R as iconPackR

/**
 * Transfer menu action.
 * All the actions which may be available in normal mode should be defined here.
 */
sealed interface TransferMenuAction : TopAppBarAction {

    /**
     * Resume transfers
     */
    data object Resume : TopAppBarActionString(
        iconRes = iconPackR.drawable.ic_play_medium_regular_outline,
        descriptionRes = R.string.action_play,
        testTag = TEST_TAG_RESUME_ACTION,
    ), TransferMenuAction

    /**
     * Pause transfers
     */
    data object Pause : TopAppBarActionString(
        iconRes = iconPackR.drawable.ic_pause_medium_regular_outline,
        descriptionRes = R.string.action_pause,
        testTag = TEST_TAG_PAUSE_ACTION,
    ), TransferMenuAction

    /**
     * More
     */
    data object More : TopAppBarActionString(
        iconRes = iconPackR.drawable.ic_more_vertical_medium_regular_outline,
        descriptionRes = mega.privacy.android.core.R.string.label_more,
        testTag = TEST_TAG_MORE_ACTION,
    ), TransferMenuAction

    /**
     * Select all
     */
    data object SelectAll : TopAppBarActionString(
        iconRes = iconPackR.drawable.ic_check_stack_medium_regular_outline,
        descriptionRes = R.string.action_select_all,
        testTag = TEST_TAG_SELECT_ALL_ACTION,
    ), TransferMenuAction

    /**
     * Cancel selected
     */
    data object CancelSelected : TopAppBarActionString(
        iconRes = iconPackR.drawable.ic_minus_circle_medium_regular_outline,
        descriptionRes = R.string.cancel_transfers,
        testTag = TEST_TAG_CANCEL_ACTION,
    ), TransferMenuAction

    /**
     * Clear selected
     */
    data object ClearSelected : TopAppBarActionString(
        iconRes = iconPackR.drawable.ic_eraser_medium_regular_outline,
        descriptionRes = R.string.general_clear,
        testTag = TEST_TAG_CLEAR_ACTION,
    ), TransferMenuAction

    companion object {
        /**
         * Test Tag resume transfers Action
         */
        const val TEST_TAG_RESUME_ACTION = "transfers_view:action_resume_transfers"

        /**
         * Test Tag pause transfers Action
         */
        const val TEST_TAG_PAUSE_ACTION = "transfers_view:action_pause_transfers"

        /**
         * Test Tag cancel all transfers Action
         */
        const val TEST_TAG_MORE_ACTION = "transfers_view:action_more"

        /**
         * Test Tag select all transfers Action
         */
        const val TEST_TAG_SELECT_ALL_ACTION = "transfers_view:action_select_all"

        /**
         * Test Tag cancel selected transfers Action
         */
        const val TEST_TAG_CANCEL_ACTION = "transfers_view:action_cancel_selected"

        /**
         * Test Tag clear selected transfers Action
         */
        const val TEST_TAG_CLEAR_ACTION = "transfers_view:action_clear_selected"
    }
}