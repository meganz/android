package mega.privacy.android.domain.usecase.chat.giphy

import mega.privacy.android.domain.entity.chat.Giphy
import mega.privacy.android.domain.repository.chat.GiphyRepository
import javax.inject.Inject

/**
 * Use case for getting trending GIFs.
 */
class GetTrendingGiphysUseCase @Inject constructor(
    private val giphyRepository: GiphyRepository,
) {

    /**
     * Invoke.
     *
     * @param limit Maximum number of GIFs to return.
     * @param offset Position of the first GIF to return.
     * @return List of [Giphy].
     */
    suspend operator fun invoke(limit: Int, offset: Int): List<Giphy> =
        giphyRepository.getTrendingGiphys(limit = limit, offset = offset)
}
