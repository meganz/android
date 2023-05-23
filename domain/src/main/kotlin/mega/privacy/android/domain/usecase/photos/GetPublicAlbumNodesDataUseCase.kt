package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Get public album nodes data use case
 */
class GetPublicAlbumNodesDataUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    operator fun invoke() = albumRepository.getPublicAlbumNodesData()
}
