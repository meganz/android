package mega.privacy.android.feature.sync.ui.synclist

import mega.privacy.android.core.ui.model.MenuActionWithoutIcon
import mega.privacy.android.feature.sync.R

internal sealed interface SyncListMenuAction {
    object SyncOptions :
        MenuActionWithoutIcon(R.string.sync_menu_sync_options, SYNC_OPTIONS_TEST_TAG)

    object ClearSyncOptions :
        MenuActionWithoutIcon(R.string.sync_menu_clear_issues, CLEAN_SOLVED_ISSUES_TEST_TAG)
}