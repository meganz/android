package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.TextUtil.getFolderInfo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;
import java.util.regex.Pattern;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;


public class MegaApiUtils {
    /**
     * Gets the string to show as content of a folder.
     *
     * @param node The folder to get its string content.
     * @return The string to show as content of the folder.
     */
    public static String getMegaNodeFolderInfo(MegaNode node, Context context) {
        MegaApiJava megaApi = MegaApplication.getInstance().getMegaApi();

        return getFolderInfo(megaApi.getNumChildFolders(node), megaApi.getNumChildFiles(node), context);
    }

    /**
     * Gets the string to show as content of a folder link.
     *
     * @param node The folder to get its string content.
     * @return The string to show as content of the folder.
     */
    public static String getMegaNodeFolderLinkInfo(MegaNode node, Context context) {
        MegaApiJava megaApiFolder = MegaApplication.getInstance().getMegaApiFolder();

        return getFolderInfo(megaApiFolder.getNumChildFolders(node), megaApiFolder.getNumChildFiles(node), context);
    }


    /*
     * If there is an application that can manage the Intent, returns true. Otherwise, false.
     */
    public static boolean isIntentAvailable(Context ctx, Intent intent) {
        Timber.d("isIntentAvailable");
        final PackageManager mgr = ctx.getPackageManager();
        List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public static int calculateDeepBrowserTreeIncoming(MegaNode node, Context context) {
        Timber.d("calculateDeepBrowserTreeIncoming");
        MegaApiAndroid megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        String path = megaApi.getNodePath(node);
        Timber.d("The path is: %s", path);

        Pattern pattern = Pattern.compile("/");
        int count = Util.countMatches(pattern, path);

        return count + 1;
    }
}
