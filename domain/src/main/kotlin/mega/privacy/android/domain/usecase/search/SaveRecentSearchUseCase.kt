package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.repository.SearchRepository
import javax.inject.Inject

/**
 * Save recent search use case
 */
class SaveRecentSearchUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
) {
    /**
     * Invoke
     *
     * @param query The search query to save
     */
    suspend operator fun invoke(query: String) = searchRepository.saveRecentSearch(query)
}

