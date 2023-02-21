package mega.privacy.android.app.presentation.bottomsheet

/**
 * This interface is to define what methods the activity
 * should implement when having UploadBottomSheetDialog
 */
interface UploadBottomSheetDialogActionListener :
    UploadFilesActionListener,
    UploadFolderActionListener,
    TakePictureAndUploadActionListener,
    ScanDocumentActionListener,
    ShowNewFolderDialogActionListener,
    ShowNewTextFileDialogActionListener