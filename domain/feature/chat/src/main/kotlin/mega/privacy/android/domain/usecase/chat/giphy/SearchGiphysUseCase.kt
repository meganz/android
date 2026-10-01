package mega.privacy.android.domain.usecase.chat.giphy

import mega.privacy.android.domain.entity.chat.Giphy
import mega.privacy.android.domain.repository.chat.GiphyRepository
import javax.inject.Inject

/**
 * Use case for searching GIFs.
 */
class SearchGiphysUseCase @Inject constructor(
    private val giphyRepository: GiphyRepository,
) {

    /**
     * Invoke.
     *
     * @param query Search term.
     * @param limit Maximum number of GIFs to return.
     * @param offset Position of the first GIF to return.
     * @return List of [Giphy].
     */
    suspend operator fun invoke(query: String, limit: Int, offset: Int): List<Giphy> =
        giphyRepository.searchGiphys(query = query, limit = limit, offset = offset)
}
