package mega.privacy.android.app.modalbottomsheet

/**
 * Interface for the callback to be invoked when the user leaves the folder.
 */
interface OnSharedFolderUpdatedCallBack {
    fun onSharedFolderUpdated()
    fun showLeaveFolderDialog(nodeIds: List<Long>)
}
