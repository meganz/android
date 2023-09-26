package mega.privacy.android.app.utils;

import android.content.Context;

import java.io.File;

import kotlin.coroutines.Continuation;

public class SDCardUtils {

    /**
     * Retrieves the Root SD Card path
     *
     * @param path the File path
     * @return the Root SD Card path
     * @deprecated This function is no longer acceptable to retrieve the Root SD Card path. Use
     * {@link mega.privacy.android.data.gateway.SDCardGateway#getRootSDCardPath(String, Continuation)}
     * instead
     */
    public static String getSDCardRoot(String path) {
        int i = 0, x = 0;
        char[] chars = path.toCharArray();
        for (; x < chars.length; x++) {
            char c = chars[x];
            if (c == '/') {
                i++;
            }
            if (i == 3) {
                break;
            }
        }
        return path.substring(0, x);
    }

    /**
     * Checks whether the Local Folder is inside the SD Card or not
     *
     * @param context   The Context
     * @param localPath The Folder Local Path
     * @return true if the Local Folder is inside the SD Card, and false if otherwise
     * @deprecated This function is no longer acceptable to check whether the Local Folder is inside
     * the SD Card or not. Use
     * {@link mega.privacy.android.data.gateway.SDCardGateway#doesFolderExists(String, Continuation)}
     * instead
     */
    @Deprecated
    public static boolean isLocalFolderOnSDCard(Context context, String localPath) {
        File[] fs = context.getExternalFilesDirs(null);
        if (fs.length > 1 && fs[1] != null) {
            String sdRoot = getSDCardRoot(fs[1].getAbsolutePath());
            return localPath.startsWith(sdRoot);
        }
        return false;
    }
}
