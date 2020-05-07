package mega.privacy.android.app.modalbottomsheet;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.core.content.FileProvider;
import android.widget.Toast;

import java.io.File;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;

public class ModalBottomSheetUtil {

    static void openWith(MegaNode node) {
        if (node == null) {
            logWarning("Node is null");
            return;
        }

        MegaApplication app = MegaApplication.getInstance();
        MegaApiAndroid megaApi = app.getMegaApi();
        String mimeType = MimeTypeList.typeForName(node.getName()).getType();
        Intent mediaIntent = new Intent(Intent.ACTION_VIEW);

        String localPath = getLocalFile(app, node.getName(), node.getSize());
        if (localPath != null) {
            File mediaFile = new File(localPath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaIntent.setDataAndType(FileProvider.getUriForFile(app, AUTHORITY_STRING_FILE_PROVIDER, mediaFile), MimeTypeList.typeForName(node.getName()).getType());
            } else {
                mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(node.getName()).getType());
            }
            mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            if (megaApi.httpServerIsRunning() == 0) {
                megaApi.httpServerStart();
            }

            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
            } else {
                activityManager.getMemoryInfo(mi);
                if (mi.totalMem > BUFFER_COMP) {
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                } else {
                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                }
            }

            String url = megaApi.httpServerGetLocalLink(node);
            if (url == null) {
                Toast.makeText(app, app.getResources().getString(R.string.error_open_file_with), Toast.LENGTH_LONG).show();
            } else {
                mediaIntent.setDataAndType(Uri.parse(url), mimeType);
            }
        }

        if (isIntentAvailable(app, mediaIntent)) {
            app.startActivity(mediaIntent);
        } else {
            Toast.makeText(app, app.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
        }
    }

    public static boolean isBottomSheetDialogShown(BottomSheetDialogFragment bottomSheetDialogFragment) {
        return bottomSheetDialogFragment != null && bottomSheetDialogFragment.isAdded();
    }
}
