package mega.privacy.android.app.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;

import java.io.File;

import mega.privacy.android.app.R;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;

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
        log("uploadTakePicture, parentHandle: " + parentHandle);

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
        log("uploadTakePicture");

        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.temporalPicDIR + "/picture.jpg";
        File imgFile = new File(filePath);

        String name = Util.getPhotoSyncName(imgFile.lastModified(), imgFile.getAbsolutePath());
        log("Taken picture Name: " + name);
        String newPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.temporalPicDIR + "/" + name;
        log("----NEW Name: " + newPath);
        File newFile = new File(newPath);
        imgFile.renameTo(newFile);
        UploadUtil.uploadFile(context, newPath, parentHandle, megaApi);
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

    /**
     * This method is to start system folder from Activity to choose files or folders to upload
     *
     * @param activity the activity the camera would start from
     */
    public static void uploadFromSystem(final Activity activity) {
        final File[] fs = activity.getExternalFilesDirs(null);
        //has SD card
        if (fs.length > 1) {
            Dialog localCameraDialog;
            String[] sdCardOptions = activity.getResources().getStringArray(R.array.settings_storage_download_location_array);
            android.support.v7.app.AlertDialog.Builder b = new android.support.v7.app.AlertDialog.Builder(activity);

            b.setTitle(activity.getResources().getString(R.string.upload_to_filesystem_from));
            b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: {
                            pickFileFromFileSystem(false, activity);
                            break;
                        }
                        case 1: {
                            if (fs[1] != null) {
                                pickFileFromFileSystem(true, activity);
                            } else {
                                pickFileFromFileSystem(false, activity);
                            }
                            break;
                        }
                    }
                }
            });
            b.setNegativeButton(activity.getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            localCameraDialog = b.create();
            localCameraDialog.show();
        } else {
            pickFileFromFileSystem(false, activity);
        }
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
     * @param fromSDCard whether from SD card root or not
     * @param activity the activity where the FileStorageActivity would start
     */
    public static void pickFileFromFileSystem(boolean fromSDCard, Activity activity) {
        Intent intent = new Intent();
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FILE.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
        intent.setClass(activity, FileStorageActivityLollipop.class);
        if (fromSDCard) {
            File[] fs = activity.getExternalFilesDirs(null);
            String sdRoot = getSDCardRoot(fs[1]);
            intent.putExtra(FileStorageActivityLollipop.EXTRA_SD_ROOT, sdRoot);
        }
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_LOCAL);
    }

    public static void log(String message) {
        Util.log("UploadUtil", message);
    }
}
