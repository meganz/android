package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

class DefaultDoesPathHaveSufficientSpace @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) : DoesPathHaveSufficientSpace {
    override suspend fun invoke(path: String, requiredSpace: Long) = kotlin.runCatching {
        fileSystemRepository.getDiskSpaceBytes(path) > requiredSpace
    }.getOrDefault(true)
}