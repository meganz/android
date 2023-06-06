package mega.privacy.android.data.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaTransfer

/**
 * Transfers Repository
 */
interface TransfersRepository {

    /**
     * Cancels a [MegaTransfer]
     *
     * @param transfer the [MegaTransfer] to cancel
     */
    suspend fun cancelTransfer(transfer: MegaTransfer)

    /**
     * Upload a file or folder
     *
     * @param localPath The local path of the file or folder
     * @param parentNode The parent node for the file or folder
     * @param fileName The custom file name for the file or folder. Leave the parameter as "null"
     * if there are no changes
     * @param modificationTime The custom modification time for the file or folder, denoted in
     * seconds since the epoch
     * @param appData The custom app data to save, which can be nullable
     * @param isSourceTemporary Whether the temporary file or folder that is created for upload
     * should be deleted or not
     * @param shouldStartFirst Whether the file or folder should be placed on top of the upload
     * queue or not
     * @param cancelToken The token to cancel an ongoing file or folder upload, which can be
     * nullable
     *
     * @return a Flow of [GlobalTransfer]
     */
    fun startUpload(
        localPath: String,
        parentNodeId: NodeId,
        fileName: String?,
        modificationTime: Long,
        appData: String?,
        isSourceTemporary: Boolean,
        shouldStartFirst: Boolean,
        cancelToken: MegaCancelToken?,
    ): Flow<GlobalTransfer>
}
