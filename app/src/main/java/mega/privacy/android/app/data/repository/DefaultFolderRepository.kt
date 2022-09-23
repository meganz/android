package mega.privacy.android.app.data.repository

import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.domain.repository.FolderRepository
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject


/**
 * Default implementation of [FolderRepository]
 *
 * @property megaLocalStorageGateway
 */
class DefaultFolderRepository @Inject constructor(val megaLocalStorageGateway: MegaLocalStorageGateway) :
    FolderRepository {

    override suspend fun getUploadFolderHandle(isPrimary: Boolean): Long {
        val handle =
            if (isPrimary) megaLocalStorageGateway.getCamSyncHandle() else megaLocalStorageGateway.getMegaHandleSecondaryFolder()
        return handle ?: MegaApiJava.INVALID_HANDLE
    }
}