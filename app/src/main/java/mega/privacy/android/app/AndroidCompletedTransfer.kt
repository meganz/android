package mega.privacy.android.app

import android.os.Parcel
import android.os.Parcelable
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeFolderPath
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaError
import mega.privacy.android.app.objects.SDTransfer
import mega.privacy.android.app.utils.SDCardUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.MegaApiJava
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util

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
class AndroidCompletedTransfer : Parcelable {
    var id: Long = 0
    val fileName: String?
    val type: Int
    val state: Int
    val size: String?
    val nodeHandle: String?
    var path: String?
    var isOfflineFile = false
    val timeStamp: Long
    val error: String?
    val originalPath: String?
    val parentHandle: Long

    constructor(
        id: Long, fileName: String?, type: Int, state: Int, size: String?,
        nodeHandle: String?, path: String?, isOfflineFile: Boolean,
        timeStamp: Long, error: String?, originalPath: String?,
        parentHandle: Long,
    ) {
        this.id = id
        this.fileName = fileName
        this.type = type
        this.state = state
        this.size = size
        this.nodeHandle = nodeHandle
        this.path = removeLastFileSeparator(path)
        this.isOfflineFile = isOfflineFile
        this.timeStamp = timeStamp
        this.error = error
        this.originalPath = originalPath
        this.parentHandle = parentHandle
    }

    constructor(transfer: MegaTransfer, error: MegaError) {
        fileName = transfer.fileName
        type = transfer.type
        state = transfer.state
        size = Util.getSizeString(transfer.totalBytes)
        nodeHandle = transfer.nodeHandle.toString()
        path = getTransferPath(transfer)
        timeStamp = System.currentTimeMillis()
        this.error = getErrorString(transfer, error)
        originalPath = transfer.path
        parentHandle = transfer.parentHandle
    }

    constructor(transfer: SDTransfer) {
        fileName = transfer.name
        type = MegaTransfer.TYPE_DOWNLOAD
        state = MegaTransfer.STATE_COMPLETED
        size = transfer.size
        nodeHandle = transfer.nodeHandle
        path = removeLastFileSeparator(SDCardUtils.getSDCardTargetPath(transfer.appData))
        timeStamp = System.currentTimeMillis()
        error = StringResourcesUtils.getString(R.string.api_ok)
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
                        offlineFolder.absolutePath)
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
    private fun getErrorString(transfer: MegaTransfer, error: MegaError): String =
        if (error.errorCode == MegaError.API_EOVERQUOTA && transfer.isForeignOverquota)
            StringResourcesUtils.getString(R.string.error_share_owner_storage_quota)
        else
            StringResourcesUtils.getTranslatedErrorString(error)

    private constructor(parcel: Parcel) {
        id = parcel.readLong()
        fileName = parcel.readString()
        type = parcel.readInt()
        state = parcel.readInt()
        size = parcel.readString()
        nodeHandle = parcel.readString()
        path = parcel.readString()
        isOfflineFile = parcel.readByte().toInt() != 0
        timeStamp = parcel.readLong()
        error = parcel.readString()
        originalPath = parcel.readString()
        parentHandle = parcel.readLong()
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(fileName)
        dest.writeInt(type)
        dest.writeInt(state)
        dest.writeString(size)
        dest.writeString(nodeHandle)
        dest.writeString(path)
        dest.writeByte((if (isOfflineFile) 1 else 0).toByte())
        dest.writeLong(timeStamp)
        dest.writeString(error)
        dest.writeString(originalPath)
        dest.writeLong(parentHandle)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<AndroidCompletedTransfer?> =
            object : Parcelable.Creator<AndroidCompletedTransfer?> {
                override fun createFromParcel(parcel: Parcel): AndroidCompletedTransfer =
                    AndroidCompletedTransfer(parcel)

                override fun newArray(size: Int): Array<AndroidCompletedTransfer?> =
                    arrayOfNulls(size)
            }
    }
}