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

import java.io.File;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;

import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbnailFromCache;
import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbnailFromFolder;
import static mega.privacy.android.app.utils.ThumbnailUtils.getRoundedBitmap;
import static mega.privacy.android.app.utils.Util.dp2px;

public class ModalBottomSheetUtil {

    public static void openWith(Context context, MegaNode node) {
        if (node == null) {
            logWarning("Node is null");
            return;
        }

        MegaApplication app = MegaApplication.getInstance();
        MegaApiAndroid megaApi = app.getMegaApi();
        String mimeType = MimeTypeList.typeForName(node.getName()).getType();

        if (MimeTypeList.typeForName(node.getName()).isURL()) {
            manageURLNode(context, MegaApplication.getInstance().getMegaApi(), node);
            return;
        }

        Intent mediaIntent = new Intent(Intent.ACTION_VIEW);

        String localPath = getLocalFile(node);
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
                Util.showSnackbar(context, getString(R.string.error_open_file_with));
            } else {
                mediaIntent.setDataAndType(Uri.parse(url), mimeType);
            }
        }

        if (isIntentAvailable(app, mediaIntent)) {
            mediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            app.startActivity(mediaIntent);
        } else {
            Util.showSnackbar(context, getString(R.string.intent_not_available_file));
        }
    }

    public static boolean isBottomSheetDialogShown(BottomSheetDialogFragment bottomSheetDialogFragment) {
        return bottomSheetDialogFragment != null && bottomSheetDialogFragment.isAdded();
    }

    /**
     * Gets a node thumbnail if available and sets it in the UI.
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

        setThumbnail(context, thumb, nodeThumb, node.getName());
    }

    /**
     * Sets a thumbnail in the UI if available or the default file icon if not.
     *
     * @param thumb     Bitmap thumbnail if available, null otherwise.
     * @param nodeThumb ImageView in which the thumbnail has to be set.
     * @param fileName  Name of the file.
     * @return True if thumbnail is available, false otherwise.
     */
    public static boolean setThumbnail(Context context, Bitmap thumb, ImageView nodeThumb, String fileName) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();

        if (thumb != null) {
            params.height = params.width = dp2px(THUMB_SIZE_DP);
            int margin = dp2px(THUMB_MARGIN_DP);
            params.setMargins(margin, margin, margin, margin);
            nodeThumb.setImageBitmap(getRoundedBitmap(context, thumb, dp2px(THUMB_CORNER_RADIUS_DP)));
        } else {
            params.height = params.width = dp2px(ICON_SIZE_DP);
            int margin = dp2px(ICON_MARGIN_DP);
            params.setMargins(margin, margin, margin, margin);
            nodeThumb.setImageResource(MimeTypeList.typeForName(fileName).getIconResourceId());
        }

        nodeThumb.setLayoutParams(params);

        return  thumb != null;
    }
}
