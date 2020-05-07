package mega.privacy.android.app.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import java.io.File;

import mega.privacy.android.app.R;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.CacheFolderManager.TEMPORAL_FOLDER;
import static mega.privacy.android.app.utils.CacheFolderManager.buildTempFile;
import static mega.privacy.android.app.utils.CacheFolderManager.getCacheFile;
import static mega.privacy.android.app.utils.FileUtils.isFileAvailable;
import static mega.privacy.android.app.utils.LogUtil.logDebug;

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

        if (parentHandle == -1) {
            parentHandle = megaApi.getRootNode().getHandle();
        }

        Intent intent = new Intent(context, UploadService.class);
        File file = new File(filePath);
        intent.putExtra(UploadService.EXTRA_FILEPATH, file.getAbsolutePath());
        intent.putExtra(UploadService.EXTRA_NAME, file.getName());
        intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentHandle);
        intent.putExtra(UploadService.EXTRA_SIZE, file.length());
        context.startService(intent);
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
        File imgFile = getCacheFile(context, TEMPORAL_FOLDER, "picture.jpg");
        if (!isFileAvailable(imgFile)) {
            Util.showSnackbar(context, context.getString(R.string.general_error));
            return;
        }

        String name = Util.getPhotoSyncName(imgFile.lastModified(), imgFile.getAbsolutePath());
        logDebug("Taken picture Name: "+name);
        File newFile = buildTempFile(context, name);
        imgFile.renameTo(newFile);

        uploadFile(context, newFile.getAbsolutePath(), parentHandle, megaApi);
    }

    /**
     * This method is to start device folder from Activity to choose files or folders to upload
     *
     * @param activity the activity the camera would start from
     */
    public static void chooseFromDevice(Activity activity) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");
        activity.startActivityForResult(Intent.createChooser(intent, null), Constants.REQUEST_CODE_GET);
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


    /** The method is to open FileStorageActivity to select file or folder
     *
     * @param activity the activity where the FileStorageActivity would start
     */
    public static void pickFileFromFileSystem(Activity activity) {
        Intent intent = new Intent();
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FILE.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
        intent.setClass(activity, FileStorageActivityLollipop.class);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_LOCAL);
    }
}
