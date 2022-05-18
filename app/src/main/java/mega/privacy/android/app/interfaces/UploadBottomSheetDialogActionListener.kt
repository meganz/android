package mega.privacy.android.app.interfaces

/**
 * This interface is to define what methods the activity
 * should implement when having UploadBottomSheetDialog
 */
interface UploadBottomSheetDialogActionListener {
    fun uploadFiles()
    fun uploadFolder()
    fun takePictureAndUpload()
    fun scanDocument()
    fun showNewFolderDialog(typedText: String? = null)
    fun showNewTextFileDialog(typedName: String? = null)
}