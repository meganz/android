package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Intent;

import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;

public class LocalFolderSelector {

    public static final int REQUEST_DOWNLOAD_FOLDER = 2000;

    public static void toSelectFolder(PinActivityLollipop activity,String prompt) {
        log("intent to FileStorageActivityLollipop");
        Intent intent = new Intent(activity, FileStorageActivityLollipop.class);
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
        String sdRoot = null;
        try {
            SDCardOperator sdCardOperator = new SDCardOperator(activity);
            sdRoot = sdCardOperator.getSDCardRoot();
        } catch (SDCardOperator.SDCardException e) {
            e.printStackTrace();
        }
        if(sdRoot != null) {
            intent.putExtra(FileStorageActivityLollipop.EXTRA_SD_ROOT,sdRoot);
        }
        if(prompt != null) {
            intent.putExtra(FileStorageActivityLollipop.EXTRA_PROMPT, prompt);
        }
        activity.startActivityForResult(intent, LocalFolderSelector.REQUEST_DOWNLOAD_FOLDER);
    }

    private static void log(String log) {
        Util.log("LocalFolderSelector", log);
    }
}
