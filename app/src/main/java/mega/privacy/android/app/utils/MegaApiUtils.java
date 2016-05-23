package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;


public class MegaApiUtils {

    public static long getFolderSize(MegaNode parent, Context context) {
        log("getFolderSize: "+parent.getName());
        MegaApiAndroid megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        long size = 0;
//        File[] files = dir.listFiles();
        ArrayList<MegaNode> nodeList = megaApi.getChildren(parent);
        for (MegaNode node : nodeList) {
            if (node.isFile()) {
                size += node.getSize();
            }
            else{
                size += getFolderSize(node, context);
            }
        }
        return size;
    }

    /*
 * If there is an application that can manage the Intent, returns true. Otherwise, false.
 */
    public static boolean isIntentAvailable(Context ctx, Intent intent) {
        log("isIntentAvailable");
        final PackageManager mgr = ctx.getPackageManager();
        List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public static int calculateDeepBrowserTreeIncoming(MegaNode node, Context context){
        log("calculateDeepBrowserTreeIncoming");
        MegaApiAndroid megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        String path = megaApi.getNodePath(node);
        log("The path is: "+path);

        Pattern pattern = Pattern.compile("/");
        int count = Util.countMatches(pattern, path);

        return count+1;
    }

    private static void log(String message) {

        Util.log("MegaApiUtils", message);
    }

}
