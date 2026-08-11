package mega.privacy.android.app.utils;

import mega.privacy.android.app.di.DbHandlerModuleKt;
import timber.log.Timber;

public class DBUtil {

    public static void resetAccountDetailsTimeStamp() {
        Timber.d("resetAccountDetailsTimeStamp");
        DbHandlerModuleKt.getDbHandler().resetAccountDetailsTimeStamp();
    }
}
