package mega.privacy.android.app.utils;

import static android.app.Activity.RESULT_OK;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

import mega.privacy.android.app.R;
import mega.privacy.android.app.uploadFolder.UploadFolderActivity;
import timber.log.Timber;


public class UploadUtil {

    /**
     * This method is to upload camera taken photos to Cloud
     *
     * @param context The context where to start the upload service
     * @return The temporal file in which the picture is stored.
     */
    public static File getTemporalTakePictureFile(Context context) {
        Timber.d("uploadTakePicture");
        File imgFile = CacheFolderManager.getCacheFile(context, CacheFolderManager.TEMPORARY_FOLDER, "picture.jpg");
        if (!isFileAvailable(imgFile)) {
            Util.showSnackbar(context, StringResourcesUtils.getString(R.string.general_error));
            return null;
        }

        String name = Util.getPhotoSyncName(imgFile.lastModified(), imgFile.getAbsolutePath());
        Timber.d("Taken picture Name: %s", name);
        File newFile = CacheFolderManager.buildTempFile(context, name);
        imgFile.renameTo(newFile);

        return newFile;
    }

    /**
     * Opens the UploadFolderActivity to choose a folder content to upload.
     *
     * @param activity     Activity to start the Intent.
     * @param resultCode   Result code of the onActivityResult.
     * @param data         Intent received in onActivityResult with the picked folder.
     * @param parentHandle Parent handle in which the folder content will be uploaded.
     */
    public static void getFolder(Activity activity, int resultCode, Intent data, long parentHandle) {
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            Timber.w("resultCode: %s", resultCode);
            return;
        }

        Uri uri = data.getData();
        activity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivityForResult(
                new Intent(activity, UploadFolderActivity.class)
                        .setData(uri)
                        .putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, parentHandle),
                Constants.REQUEST_CODE_GET_FOLDER_CONTENT);
    }
}
