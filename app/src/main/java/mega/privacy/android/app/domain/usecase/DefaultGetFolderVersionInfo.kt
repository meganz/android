package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.domain.entity.FolderVersionInfo
import mega.privacy.android.domain.usecase.GetFolderVersionInfo
import timber.log.Timber
import javax.inject.Inject

/**
 * Default get folder version info
 *
 * @property filesRepository
 */
class DefaultGetFolderVersionInfo @Inject constructor(private val filesRepository: FilesRepository) :
    GetFolderVersionInfo {
    override suspend fun invoke(): FolderVersionInfo? {
        return runCatching {
            filesRepository.getRootFolderVersionInfo().takeIf { it.numberOfVersions >= 0 }
        }.fold(
            onSuccess = { it },
            onFailure = {
                Timber.e(it)
                null
            }
        )
    }
}
