package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.model.GlobalTransfer
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode

/**
 * Use Case to upload a file or folder
 */
fun interface StartUpload {

    /**
     * Invokes the Use Case to upload a file or folder
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
     * @return A flow of [GlobalTransfer]
     */
    suspend operator fun invoke(
        localPath: String,
        parentNode: MegaNode,
        fileName: String?,
        modificationTime: Long,
        appData: String?,
        isSourceTemporary: Boolean,
        shouldStartFirst: Boolean,
        cancelToken: MegaCancelToken?,
    ): Flow<GlobalTransfer>
}