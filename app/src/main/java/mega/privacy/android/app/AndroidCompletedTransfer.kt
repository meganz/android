package mega.privacy.android.app

import android.content.Context
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.objects.SDTransfer
import mega.privacy.android.app.presentation.extensions.getErrorStringId
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeFolderPath
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.SDCardUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.extensions.mapTransferType
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.exception.MegaException
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
        path = getTransferPath(transfer.parentPath, transfer.nodeHandle, transfer.parentHandle)
        timeStamp = System.currentTimeMillis()
        this.error = getErrorString(
            transfer.isForeignOverquota,
            error.errorCode,
            StringResourcesUtils.getTranslatedErrorString(error),
            context
        )
        originalPath = transfer.path
        parentHandle = transfer.parentHandle
    }

    constructor(transfer: Transfer, error: MegaException?, context: Context) {
        fileName = transfer.fileName
        type = transfer.transferType.mapTransferType()
        state = transfer.state.mapTransferState()
        size = Util.getSizeString(transfer.totalBytes, context)
        nodeHandle = transfer.nodeHandle.toString()
        path = getTransferPath(transfer.parentPath, transfer.nodeHandle, transfer.parentHandle)
        timeStamp = System.currentTimeMillis()
        this.error = error?.let {
            getErrorString(
                transfer.isForeignOverQuota,
                it.errorCode,
                context.getString(error.getErrorStringId()),
                context
            )
        }
        originalPath = transfer.localPath
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
     * @param parentPath The parent path related to this transfer.
     * @param nodeHandle Handle related to this transfer.
     * @param parentHandle Handle of the parent node related to this transfer.
     * @return  The path of the transfer.
     */
    private fun getTransferPath(parentPath: String, nodeHandle: Long, parentHandle: Long): String? {
        val app = getInstance()
        return when (type) {
            MegaTransfer.TYPE_UPLOAD -> {
                isOfflineFile = false
                removeLastFileSeparator(
                    getNodeFolderPath(app.megaApi.getNodeByHandle(parentHandle))
                )
            }

            MegaTransfer.TYPE_DOWNLOAD -> {
                var path = parentPath
                val offlineFolder = OfflineUtils.getOfflineFolder(app, OfflineUtils.OFFLINE_DIR)
                isOfflineFile =
                    !TextUtil.isTextEmpty(path) && offlineFolder != null && path.startsWith(
                        offlineFolder.absolutePath
                    )
                if (isOfflineFile) {
                    path = OfflineUtils.removeInitialOfflinePath(path, nodeHandle)
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
     * @param isForeignOverquota True if the transfer has failed with MEGAErrorTypeApiEOverquota
     *                           and the target is foreign, false otherwise.
     * @param errorCode Error of the transfer.
     * @param errorMessage Translated error message.
     * @param context [Context]
     * @return The error to show as cause of the failure.
     */
    private fun getErrorString(
        isForeignOverquota: Boolean,
        errorCode: Int,
        errorMessage: String,
        context: Context,
    ): String =
        if (errorCode == MegaError.API_EOVERQUOTA && isForeignOverquota)
            context.getString(R.string.error_share_owner_storage_quota)
        else
            errorMessage

    /**
     * Temporary mapper while transitioning completely from [MegaTransfer] to [Transfer].
     */
    private fun TransferState.mapTransferState(): Int = when (this) {
        TransferState.STATE_NONE -> MegaTransfer.STATE_NONE
        TransferState.STATE_QUEUED -> MegaTransfer.STATE_QUEUED
        TransferState.STATE_ACTIVE -> MegaTransfer.STATE_ACTIVE
        TransferState.STATE_PAUSED -> MegaTransfer.STATE_PAUSED
        TransferState.STATE_RETRYING -> MegaTransfer.STATE_RETRYING
        TransferState.STATE_COMPLETING -> MegaTransfer.STATE_COMPLETING
        TransferState.STATE_COMPLETED -> MegaTransfer.STATE_COMPLETED
        TransferState.STATE_CANCELLED -> MegaTransfer.STATE_CANCELLED
        TransferState.STATE_FAILED -> MegaTransfer.STATE_FAILED
    }
}
