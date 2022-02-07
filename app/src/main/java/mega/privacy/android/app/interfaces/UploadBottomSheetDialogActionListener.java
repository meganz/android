package mega.privacy.android.app.interfaces;

/**
 * This interface is to define what methods the activity
 * should implement when having UploadBottomSheetDialog
 */
public interface UploadBottomSheetDialogActionListener {
    void uploadFiles();

    void uploadFolder();

    void takePictureAndUpload();

    void scanDocument();

    void showNewFolderDialog();

    void showNewTextFileDialog(String typedName);
}
