package mega.privacy.android.app.utils;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

import mega.privacy.android.app.R;
import mega.privacy.android.app.uploadFolder.UploadFolderActivity;

import static mega.privacy.android.app.utils.CacheFolderManager.TEMPORAL_FOLDER;
import static mega.privacy.android.app.utils.CacheFolderManager.buildTempFile;
import static mega.privacy.android.app.utils.CacheFolderManager.getCacheFile;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;

public class UploadUtil {

    /**
     * This method is to upload camera taken photos to Cloud
     *
     * @param context The context where to start the upload service
     * @return The temporal file in which the picture is stored.
     */
    public static File getTemporalTakePictureFile(Context context) {
        logDebug("uploadTakePicture");
        File imgFile = getCacheFile(context, TEMPORAL_FOLDER, "picture.jpg");
        if (!isFileAvailable(imgFile)) {
            Util.showSnackbar(context, StringResourcesUtils.getString(R.string.general_error));
            return null;
        }

        String name = Util.getPhotoSyncName(imgFile.lastModified(), imgFile.getAbsolutePath());
        logDebug("Taken picture Name: "+name);
        File newFile = buildTempFile(context, name);
        imgFile.renameTo(newFile);

        return newFile;
    }

    /**
     * Opens the system file picker to choose files to upload.
     *
     * @param activity Activity to start the Intent.
     */
    public static void chooseFiles(Activity activity) {
        activity.startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                .setType("*/*"), null), Constants.REQUEST_CODE_GET_FILES);
    }

    /**
     * Opens the system file picker to choose a folder to upload.
     *
     * @param activity Activity to start the Intent.
     */
    public static void chooseFolder(Activity activity) {
        activity.startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null), Constants.REQUEST_CODE_GET_FOLDER);
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
            logWarning("resultCode: " + resultCode);
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

    /** The method is to return sdcard root of the file
     * @param sd the sd card file
     * @return where the file's sd card root is
     */
    public static String getSDCardRoot(File sd) {
        String s = sd.getPath();
        int i = 0, x = 0;
        for (;
             x < s.toCharArray().length;
             x++) {
            char c = s.toCharArray()[x];
            if (c == '/') {
                i++;
            }
            if (i == 3) {
                break;
            }
        }
        return s.substring(0, x);
    }
}
