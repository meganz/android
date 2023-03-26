package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * UseCase for getting proscribed album names
 */
class GetProscribedAlbumNamesUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(): List<String> = albumRepository.getProscribedAlbumTitles()
}