package mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

internal class RenameFilesWithTheSameNameUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    suspend operator fun invoke(fileUris: List<UriPath>) {
        fileSystemRepository.renameDocumentWithTheSameName(fileUris.drop(1))
    }
}
