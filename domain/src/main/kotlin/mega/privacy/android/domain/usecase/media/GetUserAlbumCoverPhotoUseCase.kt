package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.timeout
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class GetUserAlbumCoverPhotoUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val photosRepository: PhotosRepository,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
) {
    @OptIn(FlowPreview::class)
    suspend operator fun invoke(
        albumId: AlbumId,
        selectedCoverId: Long? = null,
        refresh: Boolean = false,
    ): Photo? {
        val albumPhotos = albumRepository.getAlbumElementIDs(albumId = albumId, refresh = refresh)
        if (albumPhotos.isEmpty()) return null

        val showHiddenItems = monitorShowHiddenItemsUseCase().first()
        val accountDetail = monitorAccountDetailUseCase()
            .filter { it.levelDetail != null }
            // prevents long waiting time, worst case is that it will use a different cover
            .timeout(2.seconds)
            .firstOrNull()
        val isPaid = accountDetail?.levelDetail?.accountType?.isPaid ?: false
        val isHiddenEnabled = showHiddenItems && isPaid
        val cover = findValidCover(
            albumPhotos = albumPhotos,
            selectedCoverId = selectedCoverId,
            isHiddenEnabled = isHiddenEnabled,
            refresh = refresh,
        ) ?: return null

        return cover
    }

    private suspend fun findValidCover(
        albumPhotos: List<AlbumPhotoId>,
        selectedCoverId: Long?,
        isHiddenEnabled: Boolean,
        refresh: Boolean,
    ): Photo? {
        // Try selected cover first
        selectedCoverId?.let { coverId ->
            albumPhotos.find { it.id == coverId }?.let { albumPhotoId ->
                val photo = photosRepository.getPhotoFromNodeID(
                    nodeId = albumPhotoId.nodeId,
                    albumPhotoId = albumPhotoId,
                    refresh = refresh,
                )
                if (photo != null && isPhotoVisible(photo, isHiddenEnabled)) {
                    return photo
                }
            }
        }

        // Fall back to last visible photo
        for (albumPhotoId in albumPhotos.asReversed()) {
            val photo = photosRepository.getPhotoFromNodeID(
                nodeId = albumPhotoId.nodeId,
                albumPhotoId = albumPhotoId,
                refresh = refresh,
            )
            if (photo != null && isPhotoVisible(photo, isHiddenEnabled)) {
                return photo
            }
        }

        return null
    }

    private fun isPhotoVisible(photo: Photo, isHiddenEnabled: Boolean): Boolean =
        isHiddenEnabled || (!photo.isSensitive && !photo.isSensitiveInherited)
}