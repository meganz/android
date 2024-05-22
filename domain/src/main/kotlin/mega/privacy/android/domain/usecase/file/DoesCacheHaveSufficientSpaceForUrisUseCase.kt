package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Does cache have sufficient space for all the files identified by uris
 */
class DoesCacheHaveSufficientSpaceForUrisUseCase @Inject constructor(
    private val doesPathHaveSufficientSpaceUseCase: DoesPathHaveSufficientSpaceUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val cacheRepository: CacheRepository,
) {
    /**
     * Invoke
     *
     * @param uris the list of uris to get the required space
     * @return true if path has sufficient space, otherwise false
     */
    suspend operator fun invoke(uris: List<String>) =
        cacheRepository.getCacheFolder(TEMPORARY_FOLDER)?.path?.let { cacheFolderPath ->
            doesPathHaveSufficientSpaceUseCase(
                path = cacheFolderPath,
                requiredSpace = uris.sumOf { fileSystemRepository.getFileSizeFromUri(it) ?: 0L }
            )
        } ?: false

    companion object {
        private const val TEMPORARY_FOLDER =
            "tempMEGA" //it doesn't really matter which subfolder in this case because the free space will be the same
    }
}