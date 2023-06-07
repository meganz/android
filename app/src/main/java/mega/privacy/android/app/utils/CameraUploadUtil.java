package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.data.model.MegaPreferences;
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase;
import mega.privacy.android.domain.usecase.IsCameraUploadSyncEnabled;
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled;

public class CameraUploadUtil {

    private static final MegaApplication app = MegaApplication.getInstance();
    private static final DatabaseHandler dbH = app.getDbH();

    /**
     * @see GetUploadFolderHandleUseCase
     * @deprecated Replace all calls with use case
     */
    public static long getPrimaryFolderHandle() {
        return getUploadFolderHandle(true);
    }

    /**
     * @see GetUploadFolderHandleUseCase
     * @deprecated Replace all calls with use case
     */
    public static long getSecondaryFolderHandle() {
        return getUploadFolderHandle(false);
    }

    /**
     * @see IsCameraUploadSyncEnabled
     * @deprecated Replace all calls with use case
     */
    public static boolean isPrimaryEnabled() {
        MegaPreferences prefs = dbH.getPreferences();
        return prefs != null && Boolean.parseBoolean(prefs.getCamSyncEnabled());
    }

    /**
     * @see IsSecondaryFolderEnabled
     * @deprecated Replace all calls with use case
     */
    public static boolean isSecondaryEnabled() {
        MegaPreferences prefs = dbH.getPreferences();
        return prefs != null && Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled());
    }

    private static long getUploadFolderHandle(boolean isPrimary) {
        MegaPreferences prefs = dbH.getPreferences();
        if (prefs == null) {
            return INVALID_HANDLE;
        }

        String handle = isPrimary ? prefs.getCamSyncHandle() : prefs.getMegaHandleSecondaryFolder();

        return isTextEmpty(handle) ? INVALID_HANDLE : Long.parseLong(handle);
    }
}
