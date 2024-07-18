package mega.privacy.android.feature.sync.ui.synclist

import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.android.feature.sync.R
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithoutIcon

/**
 * Sync menu action
 */
internal sealed interface SyncListMenuAction : MenuAction {

    /**
     * Add new sync
     */
    object AddNewSync : MenuActionWithoutIcon(
        descriptionRes = sharedResR.string.device_center_sync_add_new_syn_button_option,
        testTag = ADD_NEW_SYNC_ACTION_TEST_TAG,
    ), SyncListMenuAction

    /**
     * Clear resolved issues
     */
    object ClearSyncOptions : MenuActionWithoutIcon(
        descriptionRes = R.string.sync_menu_clear_issues,
        testTag = CLEAN_SOLVED_ISSUES_ACTION_TEST_TAG,
    ), SyncListMenuAction

    companion object {
        /**
         * Test Tag Add New Sync Action
         */
        const val ADD_NEW_SYNC_ACTION_TEST_TAG = "sync:action_add_new_sync_test_tag"

        /**
         * Test Tag Clear Resolved Issues Action
         */
        const val CLEAN_SOLVED_ISSUES_ACTION_TEST_TAG = "sync:action_clean_solved_issues_test_tag"
    }
}