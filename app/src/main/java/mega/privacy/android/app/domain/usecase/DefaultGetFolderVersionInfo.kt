package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.FolderVersionInfo
import mega.privacy.android.domain.usecase.GetFolderVersionInfo
import timber.log.Timber
import javax.inject.Inject

/**
 * Default get folder version info
 *
 * @property megaNodeRepository
 */
class DefaultGetFolderVersionInfo @Inject constructor(private val megaNodeRepository: MegaNodeRepository) :
    GetFolderVersionInfo {
    override suspend fun invoke(): FolderVersionInfo? {
        return runCatching {
            megaNodeRepository.getRootFolderVersionInfo().takeIf { it.numberOfVersions >= 0 }
        }.fold(
            onSuccess = { it },
            onFailure = {
                Timber.e(it)
                null
            }
        )
    }
}
