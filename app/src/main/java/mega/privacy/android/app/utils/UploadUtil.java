package mega.privacy.android.app.utils;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.uploadFolder.UploadFolderActivity;
import mega.privacy.android.app.uploadFolder.list.data.UploadFolderResult;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.uploadFolder.UploadFolderActivity.UPLOAD_RESULTS;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;

import androidx.core.content.ContextCompat;

public class UploadUtil {

    /**
     * This method is to start upload file service within context
     *
     * @param context      the passed context to start upload service
     * @param filePath     the path of file to be uploaded
     * @param parentHandle the handle of parent node where file would be uploaded
     * @param megaApi      the api to process the upload                 '
     */

    public static void uploadFile(Context context, String filePath, long parentHandle, MegaApiAndroid megaApi) {
        logDebug("uploadTakePicture, parentHandle: " + parentHandle);

        if (MegaApplication.getInstance().getStorageState() == STORAGE_STATE_PAYWALL) {
            showOverDiskQuotaPaywallWarning();
            return;
        }

        if (parentHandle == -1) {
            parentHandle = megaApi.getRootNode().getHandle();
        }

        Intent intent = new Intent(context, UploadService.class);
        File file = new File(filePath);
        intent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
        intent.putExtra(UploadService.EXTRA_NAME, file.getName());
        intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentHandle);
        intent.putExtra(UploadService.EXTRA_SIZE, file.length());
        ContextCompat.startForegroundService(context, intent);
    }

    /**
     * This method is to upload camera taken photos to Cloud
     *
     * @param context The context where to start the upload service
     * @param parentHandle The handle of the folder where photo would be located
     * @param megaApi The Mega Api to upload the picture
     */
    public static void uploadTakePicture(Context context, long parentHandle, MegaApiAndroid megaApi) {
        logDebug("uploadTakePicture");
        File imgFile = CacheFolderManager.getCacheFile(context, CacheFolderManager.TEMPORARY_FOLDER, "picture.jpg");
        if (!isFileAvailable(imgFile)) {
            Util.showSnackbar(context, context.getString(R.string.general_error));
            return;
        }

        String name = Util.getPhotoSyncName(imgFile.lastModified(), imgFile.getAbsolutePath());
        logDebug("Taken picture Name: "+name);
        File newFile = CacheFolderManager.buildTempFile(context, name);
        imgFile.renameTo(newFile);

        uploadFile(context, newFile.getAbsolutePath(), parentHandle, megaApi);
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

    /**
     * Uploads the result obtained from UploadFolderActivity.
     *
     * @param activity   Activity to start the Intent.
     * @param resultCode Result code of the onActivityResult with the content to upload.
     * @param data       Intent received in onActivityResult with the .
     */
    @SuppressWarnings("unchecked")
    public static void uploadFolder(Activity activity, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            logWarning("resultCode: " + resultCode);
            return;
        }

        List<UploadFolderResult> uploadResults = (List<UploadFolderResult>) data.getSerializableExtra(UPLOAD_RESULTS);
        if (uploadResults == null) {
            logWarning("Upload results are null");
            return;
        }

        for (UploadFolderResult result : uploadResults) {
            ContextCompat.startForegroundService(activity, new Intent(activity, UploadService.class)
                    .putExtra(UploadService.EXTRA_FILEPATH, result.getAbsolutePath())
                    .putExtra(UploadService.EXTRA_NAME, result.getName())
                    .putExtra(UploadService.EXTRA_LAST_MODIFIED, result.getLastModified())
                    .putExtra(UploadService.EXTRA_PARENT_HASH, result.getParentHandle()));
        }

        int size = uploadResults.size();
        Util.showSnackbar(activity, size > 0
                ? getQuantityString(R.plurals.upload_began, size, size)
                : getString(R.string.no_uploads_empty_folder));
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
