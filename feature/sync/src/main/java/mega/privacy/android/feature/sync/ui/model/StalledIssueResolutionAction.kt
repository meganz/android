package mega.privacy.android.feature.sync.ui.model

internal data class StalledIssueResolutionAction(
    val actionName: String,
    val resolutionActionType: StalledIssueResolutionActionType,
)

internal enum class StalledIssueResolutionActionType {
    RENAME_ALL_ITEMS,
    REMOVE_DUPLICATES,
    MERGE_FOLDERS,
    REMOVE_DUPLICATES_AND_REMOVE_THE_REST,
    CHOOSE_LOCAL_FILE,
    CHOOSE_REMOTE_FILE,
    CHOOSE_LATEST_MODIFIED_TIME
}
