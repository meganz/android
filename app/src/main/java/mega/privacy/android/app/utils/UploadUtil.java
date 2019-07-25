package mega.privacy.android.app.utils;

import android.content.Context;
import android.content.Intent;

import java.io.File;

import mega.privacy.android.app.UploadService;
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

    public static void log(String message) {
        Util.log("UploadUtil", message);
    }
}
