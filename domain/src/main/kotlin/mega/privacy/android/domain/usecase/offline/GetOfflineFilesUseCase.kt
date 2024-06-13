package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import javax.inject.Inject

/**
 * Use case to get files from list of offline nodes
 */
class GetOfflineFilesUseCase @Inject constructor(
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
) {
    /**
     * Invoke
     * @param nodes List<OfflineNodeInformation>
     */
    suspend operator fun invoke(nodes: List<OfflineNodeInformation>) = coroutineScope {
        val semaphore = Semaphore(8)
        val results = nodes.map {
            async {
                semaphore.withPermit {
                    runCatching { getOfflineFileUseCase(it) }
                }
            }
        }.awaitAll()

        if (results.any { it.isSuccess }) {
            results.mapNotNull { it.getOrNull() }
        } else {
            throw results.first { it.isFailure }.exceptionOrNull()
                ?: IllegalStateException("Unable to get offline files")
        }
    }
}