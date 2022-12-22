package mega.privacy.android.app.utils;

import mega.privacy.android.app.MegaApplication;
import timber.log.Timber;

public class DBUtil {

    public static void resetAccountDetailsTimeStamp() {
        Timber.d("resetAccountDetailsTimeStamp");
        MegaApplication.getInstance().getDbH().resetAccountDetailsTimeStamp();
    }
}
