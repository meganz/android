package mega.privacy.android.app.modalbottomsheet

/**
 * Listener to receive events from [FileContactsListBottomSheetDialogFragment]
 */
internal interface FileContactsListBottomSheetDialogListener {
    /**
     * change permission option has been selected
     */
    fun changePermissions(userEmail: String)

    /**
     * remove share option has been selected
     */
    fun removeFileContactShare(userEmail: String)
}