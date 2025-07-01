package mega.privacy.android.app.presentation.transfers.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.TopAppBarAction
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack

/**
 * Transfer menu action.
 * All the actions which may be available in normal mode should be defined here.
 */
sealed interface TransferMenuAction : TopAppBarAction {

    /**
     * Resume transfers
     */
    data object Resume : TransferTopAppBarActionString(
        descriptionRes = R.string.action_play,
        testTag = TEST_TAG_RESUME_ACTION,
    ), TransferMenuAction {
        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Play)
    }

    /**
     * Pause transfers
     */
    data object Pause : TransferTopAppBarActionString(
        descriptionRes = R.string.action_pause,
        testTag = TEST_TAG_PAUSE_ACTION,
    ), TransferMenuAction {
        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Pause)
    }

    /**
     * More
     */
    data object More : TransferTopAppBarActionString(
        descriptionRes = mega.privacy.android.core.R.string.label_more,
        testTag = TEST_TAG_MORE_ACTION,
    ), TransferMenuAction {
        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)
    }

    /**
     * Select all
     */
    data object SelectAll : TransferTopAppBarActionString(
        descriptionRes = R.string.action_select_all,
        testTag = TEST_TAG_SELECT_ALL_ACTION,
    ), TransferMenuAction {
        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.CheckStack)
    }

    /**
     * Cancel selected
     */
    data object CancelSelected : TransferTopAppBarActionString(
        descriptionRes = R.string.cancel_transfers,
        testTag = TEST_TAG_CANCEL_ACTION,
    ), TransferMenuAction {
        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.MinusCircle)
    }

    /**
     * Clear selected
     */
    data object ClearSelected : TransferTopAppBarActionString(
        descriptionRes = R.string.general_clear,
        testTag = TEST_TAG_CLEAR_ACTION,
    ), TransferMenuAction {
        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Eraser)
    }

    /**
     * Retry selected
     */
    data object RetrySelected : TransferTopAppBarActionString(
        descriptionRes = R.string.general_retry,
        testTag = TEST_TAG_RETRY_ACTION,
    ), TransferMenuAction {
        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.RotateCcw)
    }

    /**
     * Helper class to build Transfer TopAppBarAction
     * @property descriptionRes
     */
    abstract class TransferTopAppBarActionString(
        @StringRes val descriptionRes: Int,
        override val testTag: String,
    ) : TopAppBarAction {
        @Composable
        override fun getDescription() = stringResource(id = descriptionRes)
    }

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

        /**
         * Test Tag retry selected transfers Action
         */
        const val TEST_TAG_RETRY_ACTION = "transfers_view:action_retry_selected"
    }
}