package mega.privacy.android.feature.sync.ui.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import javax.inject.Inject

internal class ResolutionActionTypeToResolutionNameMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    operator fun invoke(type: StalledIssueResolutionActionType): String {
        return when (type) {
            StalledIssueResolutionActionType.RENAME_ALL_ITEMS -> context.getString(R.string.sync_resolve_rename_all_items)
            StalledIssueResolutionActionType.MERGE_FOLDERS -> context.getString(R.string.sync_stalled_issue_merge_folders)
            StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE -> context.getString(R.string.sync_stalled_issue_choose_local_file)
            StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE -> context.getString(R.string.sync_stalled_issue_choose_remote_file)
            StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME -> context.getString(R.string.sync_stalled_issue_choose_latest_modified_time)
            else -> ""
        }
    }
}