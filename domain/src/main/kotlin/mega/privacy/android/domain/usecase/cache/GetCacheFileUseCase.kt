package mega.privacy.android.domain.usecase.cache

import mega.privacy.android.domain.repository.CacheRepository
import java.io.File
import javax.inject.Inject

/**
 * Get cache file use case
 *
 * @property cacheRepository
 */
class GetCacheFileUseCase @Inject constructor(
    private val cacheRepository: CacheRepository,
) {

    /**
     * Get cache file
     *
     * @param folder  folder name.
     * @param fileName
     * @return [File]
     */
    operator fun invoke(folder: String, fileName: String): File? =
        cacheRepository.getCacheFile(folder, fileName)
}