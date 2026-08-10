package mega.privacy.android.domain.usecase.search

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.SearchRepository
import javax.inject.Inject

/**
 * Use case to monitor recent searches sorted by timestamp descending (most recent first)
 */
class MonitorRecentSearchesUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
) {
    /**
     * Invoke
     *
     * @return Flow of recent search queries
     */
    operator fun invoke(): Flow<List<String>> = searchRepository.monitorRecentSearches()
}

