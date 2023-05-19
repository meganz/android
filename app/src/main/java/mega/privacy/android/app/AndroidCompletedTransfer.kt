package mega.privacy.android.app

import android.content.Context
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.objects.SDTransfer
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeFolderPath
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.SDCardUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer

/**
 * The entity for completed transfer
 *
 * @property id
 * @property fileName
 * @property type
 * @property state
 * @property size
 * @property nodeHandle
 * @property path
 * @property isOfflineFile
 * @property timeStamp
 * @property error
 * @property originalPath
 * @property parentHandle
 */
class AndroidCompletedTransfer {
    var id: Long = 0
    val fileName: String?
    val type: Int
    val state: Int
    val size: String?
    val nodeHandle: String
    var path: String?
    var isOfflineFile = false
    val timeStamp: Long
    val error: String?
    val originalPath: String?
    val parentHandle: Long

    constructor(transfer: MegaTransfer, error: MegaError, context: Context) {
        fileName = transfer.fileName
        type = transfer.type
        state = transfer.state
        size = Util.getSizeString(transfer.totalBytes, context)
        nodeHandle = transfer.nodeHandle.toString()
        path = getTransferPath(transfer)
        timeStamp = System.currentTimeMillis()
        this.error = getErrorString(transfer, error, context)
        originalPath = transfer.path
        parentHandle = transfer.parentHandle
    }

    constructor(transfer: SDTransfer, context: Context) {
        fileName = transfer.name
        type = MegaTransfer.TYPE_DOWNLOAD
        state = MegaTransfer.STATE_COMPLETED
        size = transfer.size
        nodeHandle = transfer.nodeHandle
        path = removeLastFileSeparator(SDCardUtils.getSDCardTargetPath(transfer.appData))
        timeStamp = System.currentTimeMillis()
        error = context.getString(R.string.api_ok)
        originalPath = transfer.path
        parentHandle = MegaApiJava.INVALID_HANDLE
        isOfflineFile = false
    }

    /**
     * Remove the last character of the path if it is a file separator.
     *
     * @param path  path of a file.
     * @return The path without the last item if it is a file separator.
     */
    private fun removeLastFileSeparator(path: String?): String? =
        if (!TextUtil.isTextEmpty(path) && path?.last() == '/')
            path.substring(0, path.length - 1)
        else
            path

    /**
     * Gets the path of a transfer.
     *
     * @param transfer  MegaTransfer from which the path has to be obtained
     * @return  The path of the transfer.
     */
    private fun getTransferPath(transfer: MegaTransfer): String? {
        val app = getInstance()
        return when (type) {
            MegaTransfer.TYPE_UPLOAD -> {
                isOfflineFile = false
                removeLastFileSeparator(
                    getNodeFolderPath(app.megaApi.getNodeByHandle(transfer.parentHandle))
                )
            }
            MegaTransfer.TYPE_DOWNLOAD -> {
                var path = transfer.parentPath
                val offlineFolder = OfflineUtils.getOfflineFolder(app, OfflineUtils.OFFLINE_DIR)
                isOfflineFile =
                    !TextUtil.isTextEmpty(path) && offlineFolder != null && path.startsWith(
                        offlineFolder.absolutePath
                    )
                if (isOfflineFile) {
                    path = OfflineUtils.removeInitialOfflinePath(path, transfer.nodeHandle)
                }
                removeLastFileSeparator(path)
            }
            else -> {
                isOfflineFile = false
                null
            }
        }
    }

    /**
     * Gets the error string to show as cause of the failure.
     *
     * @param transfer MegaTransfer to get its error.
     * @param error    MegaError of the transfer.
     * @return The error to show as cause of the failure.
     */
    private fun getErrorString(transfer: MegaTransfer, error: MegaError, context: Context): String =
        if (error.errorCode == MegaError.API_EOVERQUOTA && transfer.isForeignOverquota)
            context.getString(R.string.error_share_owner_storage_quota)
        else
            StringResourcesUtils.getTranslatedErrorString(error)
}
