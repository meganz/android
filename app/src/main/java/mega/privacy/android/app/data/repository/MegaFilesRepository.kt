package mega.privacy.android.app.data.repository

import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.entity.FolderVersionInfo
import mega.privacy.android.app.domain.exception.MegaException
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaFolderInfo
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class MegaFilesRepository @Inject constructor(@MegaApi  private val sdk: MegaApiAndroid) : FilesRepository {
    override suspend fun getFolderVersionInfo(): FolderVersionInfo {
        return suspendCoroutine { continuation ->
            sdk.getFolderInfo(sdk.rootNode, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        val info: MegaFolderInfo = request.megaFolderInfo
                        continuation.resumeWith(Result.success(FolderVersionInfo(info.numVersions, info.versionsSize)))
                    } else {
                        continuation.resumeWith(Result.failure(
                            MegaException(
                                error.errorCode,
                                error.errorString
                            )
                        ))
                    }
                }
            ))
        }
    }
}