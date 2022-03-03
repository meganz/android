package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.FolderVersionInfo
import mega.privacy.android.app.domain.repository.FilesRepository
import javax.inject.Inject

class DefaultGetFolderVersionInfo @Inject constructor(private val filesRepository: FilesRepository) : GetFolderVersionInfo {
    override suspend fun invoke(): FolderVersionInfo? {
        return filesRepository.getFolderVersionInfo().takeIf { it.numberOfVersions >= 0 }
    }
}