package mega.privacy.android.app.modalbottomsheet;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.core.content.FileProvider;

import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbnailFromCache;
import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbnailFromFolder;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getRoundedBitmap;
import static mega.privacy.android.app.utils.Util.dp2px;

public class ModalBottomSheetUtil {

    public static final int THUMB_ROUND_DP = 4;
    public static final int THUMB_SIZE_DP = 36;
    public static final int THUMB_MARGIN_DP = 18;
    public static final int ICON_SIZE_DP = 48;
    public static final int ICON_MARGIN_DP = 12;

    public static void openWith(MegaNode node) {
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
            mediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            app.startActivity(mediaIntent);
        } else {
            Toast.makeText(app, app.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
        }
    }

    public static boolean isBottomSheetDialogShown(BottomSheetDialogFragment bottomSheetDialogFragment) {
        return bottomSheetDialogFragment != null && bottomSheetDialogFragment.isAdded();
    }

    /**
     * Sets a node thumbnail if available or default if not.
     *
     * @param node      MegaNode from which the thumbnail has to be set.
     * @param nodeThumb ImageView in which the thumbnail has to be set.
     */
    public static void setNodeThumbnail(Context context, MegaNode node, ImageView nodeThumb) {
        Bitmap thumb = null;

        if (node.hasThumbnail()) {
            thumb = getThumbnailFromCache(node);

            if (thumb == null) {
                thumb = getThumbnailFromFolder(node, context);
            }
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();

        if (thumb != null) {
            params.height = params.width = dp2px(THUMB_SIZE_DP);
            int margin = dp2px(THUMB_MARGIN_DP);
            params.setMargins(margin, margin, margin, margin);
            nodeThumb.setImageBitmap(getRoundedBitmap(context, thumb, dp2px(THUMB_ROUND_DP)));
        } else {
            params.height = params.width = dp2px(ICON_SIZE_DP);
            int margin = dp2px(ICON_MARGIN_DP);
            params.setMargins(margin, margin, margin, margin);
            nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
        }

        nodeThumb.setLayoutParams(params);
    }
}
