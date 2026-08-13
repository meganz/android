package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.mobile.analytics.event.AndroidSyncChooseLatestModifiedTimeEvent
import mega.privacy.mobile.analytics.event.AndroidSyncChooseLocalFileEvent
import mega.privacy.mobile.analytics.event.AndroidSyncChooseRemoteFileEvent
import mega.privacy.mobile.analytics.event.AndroidSyncMergeFoldersEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRemoveDuplicatesAndRemoveRestEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRemoveDuplicatesEvent
import mega.privacy.mobile.analytics.event.AndroidSyncRenameAllItemsEvent

/**
 * Reports which resolution the user confirmed for a stalled issue.
 *
 * Called at the point the resolution is applied, so it counts confirmations rather than the
 * sheet selection that precedes them.
 */
internal fun StalledIssueResolutionActionType.trackResolutionConfirmed() {
    when (this) {
        StalledIssueResolutionActionType.RENAME_ALL_ITEMS ->
            Analytics.tracker.trackEvent(AndroidSyncRenameAllItemsEvent)

        StalledIssueResolutionActionType.REMOVE_DUPLICATES ->
            Analytics.tracker.trackEvent(AndroidSyncRemoveDuplicatesEvent)

        StalledIssueResolutionActionType.MERGE_FOLDERS ->
            Analytics.tracker.trackEvent(AndroidSyncMergeFoldersEvent)

        StalledIssueResolutionActionType.REMOVE_DUPLICATES_AND_REMOVE_THE_REST ->
            Analytics.tracker.trackEvent(AndroidSyncRemoveDuplicatesAndRemoveRestEvent)

        StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE ->
            Analytics.tracker.trackEvent(AndroidSyncChooseLocalFileEvent)

        StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE ->
            Analytics.tracker.trackEvent(AndroidSyncChooseRemoteFileEvent)

        StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME ->
            Analytics.tracker.trackEvent(AndroidSyncChooseLatestModifiedTimeEvent)

        StalledIssueResolutionActionType.UNKNOWN -> Unit
    }
}
