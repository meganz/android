package mega.privacy.android.app.utils;

import android.content.Context;

import java.io.File;

public class SDCardUtils {

    public static String getSDCardRoot(File sd) {
        String s = sd.getPath();
        int i = 0,x = 0;
        for(; x < s.toCharArray().length;x++) {
            char c = s.toCharArray()[x];
            if(c == '/') {
                i++;
            }
            if(i == 3) {
                break;
            }
        }
        return s.substring(0,x);
    }

    public static boolean isLocalFolderOnSDCard(Context context, String localPath) {
        File[] fs = context.getExternalFilesDirs(null);
        if(fs.length > 1 && fs[1] != null) {
            String sdRoot = getSDCardRoot(fs[1]);
            return localPath.startsWith(sdRoot);
        }
        return false;
    }
}
