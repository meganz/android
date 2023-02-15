package mega.privacy.android.app.modalbottomsheet

/**
 * Node options UI state
 *
 * @param currentNodeHandle Node handle of the current node for which bottom sheet dialog is opened
 * @param isOpenShareDialogSuccess if openShareDialog API call is a success or failure
 */
data class NodeOptionsBottomSheetState(
    val currentNodeHandle: Long = -1L,
    val isOpenShareDialogSuccess: Boolean? = null,
)
