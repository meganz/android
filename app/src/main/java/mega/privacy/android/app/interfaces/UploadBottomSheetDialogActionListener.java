package mega.privacy.android.app.interfaces;

/*
 * This interface is to define what methods the activity
 * should implement when having UploadBottomSheetDialog
 */
public interface UploadBottomSheetDialogActionListener {
    void uploadFromDevice();

    void uploadFromSystem();

    void takePictureAndUpload();

    void showNewFolderDialog();
}
