package mega.privacy.android.app.utils;

import android.content.Context;

import dagger.hilt.android.EntryPointAccessors;
import mega.privacy.android.app.di.DatabaseHandlerEntryPoint;
import timber.log.Timber;

public class DBUtil {

    public static void resetAccountDetailsTimeStamp(Context context) {
        Timber.d("resetAccountDetailsTimeStamp");
        EntryPointAccessors.fromApplication(
                context,
                DatabaseHandlerEntryPoint.class).getDbH().resetAccountDetailsTimeStamp();
    }
}
