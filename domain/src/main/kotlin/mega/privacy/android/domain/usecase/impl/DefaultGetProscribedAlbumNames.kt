package mega.privacy.android.domain.usecase.impl

import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNames
import javax.inject.Inject

/**
 * Implementation for the usecase [GetProscribedAlbumNames]
 */
class DefaultGetProscribedAlbumNames @Inject constructor(
    private val albumRepository: AlbumRepository,
) : GetProscribedAlbumNames {
    override suspend fun invoke(): List<String> = albumRepository.getProscribedAlbumTitles()
}