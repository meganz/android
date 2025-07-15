package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import mega.privacy.android.core.R as appR
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon

/**
 * Sync menu action
 */
internal sealed interface SyncListMenuAction : MenuAction {

    object MoreActionMenu : MenuActionWithIcon, SyncListMenuAction {
        @Composable
        override fun getIconPainter() =
            painterResource(id = appR.drawable.ic_universal_more)

        @Composable
        override fun getDescription() = ""
        override val testTag: String = CLEAN_SOLVED_ISSUES_ACTION_TEST_TAG
    }


    companion object {
        /**
         * Test Tag Clear Resolved Issues Action
         */
        const val CLEAN_SOLVED_ISSUES_ACTION_TEST_TAG = "sync:action_clean_solved_issues_test_tag"
    }
}
