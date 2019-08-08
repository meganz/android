package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Intent;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;

public class LocalFolderSelector {

    public static final int REQUEST_DOWNLOAD_FOLDER = 2000;

    public static void toFileStorageActivity(Activity activity) {
        log("intent to FileStorageActivityLollipop");
        Intent intent = new Intent(activity, FileStorageActivityLollipop.class);
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_PROMPT, activity.getString(R.string.sdcard_unavailable));
        activity.startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
    }

    private static void log(String log) {
        Util.log("LocalFolderSelector", log);
    }
}
