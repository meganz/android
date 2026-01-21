package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.repository.SearchRepository
import javax.inject.Inject

/**
 * Clear recent searches use case
 */
class ClearRecentSearchesUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
) {
    /**
     * Clear all recent searches
     */
    suspend operator fun invoke() {
        searchRepository.clearRecentSearches()
    }
}

