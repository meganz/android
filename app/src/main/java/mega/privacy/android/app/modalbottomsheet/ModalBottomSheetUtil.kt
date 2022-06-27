package mega.privacy.android.app.modalbottomsheet

import mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode
import nz.mega.sdk.MegaNode
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import android.content.Intent
import android.app.ActivityManager
import android.content.Context
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.MegaApiUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeUtil.setupStreamingServer
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util
import timber.log.Timber
import java.io.File

/**
 * Util object for modal bottom sheets.
 */
object ModalBottomSheetUtil {

    /**
     * Launches an intent to open a node in the apps installed in the device if any.
     *
     * @param context           Required Context.
     * @param node              MegaNode to open.
     */
    @JvmStatic
    fun openWith(context: Context, node: MegaNode?) {
        openWith(context, node)
    }

    /**
     * Launches an intent to open a node in the apps installed in the device if any.
     *
     * @param context           Required Context.
     * @param node              MegaNode to open.
     * @param nodeDownloader    Download action to perform if the file type is not supported.
     */
    @JvmStatic
    fun BottomSheetDialogFragment?.openWith(
        context: Context,
        node: MegaNode?,
        nodeDownloader: ((node: MegaNode) -> Unit)? = null,
    ): AlertDialog? {
        if (node == null) {
            Timber.w("Node is null")
            return null
        }
        val app = MegaApplication.getInstance()
        val megaApi = app.megaApi
        val mimeType = MimeTypeList.typeForName(node.name).type
        if (MimeTypeList.typeForName(node.name).isURL) {
            manageURLNode(context, MegaApplication.getInstance().megaApi, node)
            return null
        }
        val mediaIntent = Intent(Intent.ACTION_VIEW)
        val localPath = FileUtil.getLocalFile(node)
        if (localPath != null) {
            val mediaFile = File(localPath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaIntent.setDataAndType(FileProvider.getUriForFile(app,
                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
                    mediaFile), MimeTypeList.typeForName(node.name).type)
            } else {
                mediaIntent.setDataAndType(Uri.fromFile(mediaFile),
                    MimeTypeList.typeForName(node.name).type)
            }
            mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            setupStreamingServer(megaApi, context)
            val url = megaApi.httpServerGetLocalLink(node)
            if (url == null) {
                Util.showSnackbar(context,
                    StringResourcesUtils.getString(R.string.error_open_file_with))
            } else {
                mediaIntent.setDataAndType(Uri.parse(url), mimeType)
            }
        }

        if (MegaApiUtils.isIntentAvailable(app, mediaIntent)) {
            mediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(mediaIntent)
        } else if (this != null && nodeDownloader != null && localPath == null) {
            return this.showCannotOpenFileDialog(context, node, nodeDownloader)
        } else {
            Util.showSnackbar(context,
                StringResourcesUtils.getString(R.string.intent_not_available_file))
        }

        return null
    }


    /**
     * Shows a warning dialog informing a file cannot be opened because the type is not supported.
     *
     * @param context           Required context.
     * @param node              MegaNode to open.
     * @param nodeDownloader    Download action to perform if the user confirms it.
     * @return The AlertDialog.
     */
    @JvmStatic
    fun BottomSheetDialogFragment.showCannotOpenFileDialog(
        context: Context,
        node: MegaNode,
        nodeDownloader: (node: MegaNode) -> Unit,
    ): AlertDialog =
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.dialog_cannot_open_file_title))
            .setMessage(getString(R.string.dialog_cannot_open_file_text))
            .setPositiveButton(getString(R.string.context_download)
            ) { _, _ ->
                nodeDownloader(node)
                this.dismissAllowingStateLoss()
            }
            .setNegativeButton(getString(R.string.general_cancel), null)
            .show()

    /**
     * Checks if a bottom sheet dialog fragment is shown.
     *
     * @return True if the bottom sheet is shown, false otherwise.
     */
    @JvmStatic
    fun BottomSheetDialogFragment?.isBottomSheetDialogShown(): Boolean =
        this?.isAdded == true

    /**
     * Gets a node thumbnail if available and sets it in the UI.
     *
     * @param node      MegaNode from which the thumbnail has to be set.
     * @param nodeThumb ImageView in which the thumbnail has to be set.
     */
    @JvmStatic
    fun setNodeThumbnail(context: Context?, node: MegaNode, nodeThumb: ImageView) {
        var thumb: Bitmap? = null

        if (node.hasThumbnail()) {
            thumb = ThumbnailUtils.getThumbnailFromCache(node)
            if (thumb == null) {
                thumb = ThumbnailUtils.getThumbnailFromFolder(node, context)
            }
        }

        setThumbnail(context, thumb, nodeThumb, node.name)
    }

    /**
     * Sets a thumbnail in the UI if available or the default file icon if not.
     *
     * @param thumb     Bitmap thumbnail if available, null otherwise.
     * @param nodeThumb ImageView in which the thumbnail has to be set.
     * @param fileName  Name of the file.
     * @return True if thumbnail is available, false otherwise.
     */
    @JvmStatic
    fun setThumbnail(
        context: Context?,
        thumb: Bitmap?,
        nodeThumb: ImageView,
        fileName: String?,
    ): Boolean {
        val params = nodeThumb.layoutParams as RelativeLayout.LayoutParams

        if (thumb != null) {
            params.width = Util.dp2px(Constants.THUMB_SIZE_DP.toFloat())
            params.height = params.width
            val margin = Util.dp2px(Constants.THUMB_MARGIN_DP.toFloat())
            params.setMargins(margin, margin, margin, margin)
            nodeThumb.setImageBitmap(ThumbnailUtils.getRoundedBitmap(context, thumb, Util.dp2px(
                Constants.THUMB_CORNER_RADIUS_DP)))
        } else {
            params.width = Util.dp2px(Constants.ICON_SIZE_DP.toFloat())
            params.height = params.width
            val margin = Util.dp2px(Constants.ICON_MARGIN_DP.toFloat())
            params.setMargins(margin, margin, margin, margin)
            nodeThumb.setImageResource(MimeTypeList.typeForName(fileName).iconResourceId)
        }

        nodeThumb.layoutParams = params

        return thumb != null
    }
}