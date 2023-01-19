package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileRepository
import javax.inject.Inject

class DefaultDoesPathHaveSufficientSpace @Inject constructor(
    private val fileRepository: FileRepository,
) : DoesPathHaveSufficientSpace {
    override suspend fun invoke(path: String, requiredSpace: Long) = kotlin.runCatching {
        fileRepository.getDiskSpaceBytes(path) > requiredSpace
    }.getOrDefault(true)
}