package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.timeout
import mega.privacy.android.domain.entity.photos.AlbumId
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
        refresh: Boolean = false,
    ): Photo? {
        val albumPhotos = albumRepository
            .getAlbumElementIDs(albumId = albumId, refresh = refresh)
            .takeIf { it.isNotEmpty() }
            ?.mapNotNull { albumPhotoId ->
                photosRepository.getPhotoFromNodeID(
                    nodeId = albumPhotoId.nodeId,
                    albumPhotoId = albumPhotoId,
                    refresh = refresh,
                )
            } ?: return null

        val showHiddenItems = monitorShowHiddenItemsUseCase().first()
        val accountDetail = monitorAccountDetailUseCase()
            .filter { it.levelDetail != null }
            // prevents long waiting time, worst case is that it will use a different cover
            .timeout(2.seconds)
            .firstOrNull()
        val isPaid = accountDetail?.levelDetail?.accountType?.isPaid == true
        val isHiddenEnabled = showHiddenItems && isPaid
        val cover = findValidCover(
            albumPhotos = albumPhotos,
            selectedCoverId = albumRepository.getUserSet(albumId)?.cover,
            isHiddenEnabled = isHiddenEnabled,
            isPaid = isPaid,
        ) ?: return null

        return cover
    }

    private fun findValidCover(
        albumPhotos: List<Photo>,
        selectedCoverId: Long?,
        isHiddenEnabled: Boolean,
        isPaid: Boolean,
    ): Photo? {
        // Try selected cover first
        albumPhotos.find { it.id == selectedCoverId }?.let { photo ->
            if (isPhotoVisible(photo, isHiddenEnabled, isPaid)) {
                return photo
            }
        }

        return albumPhotos
            .sortedWith(
                compareByDescending<Photo> {
                    it.modificationTime
                }.thenByDescending { it.id }
            )
            .firstOrNull { isPhotoVisible(it, isHiddenEnabled, isPaid) }
    }

    /**
     * Determines if a photo should be visible for album cover selection.
     * isSensitive and isSensitiveInherited only apply when isPaid is true.
     * For free accounts, all photos are considered visible.
     */
    private fun isPhotoVisible(photo: Photo, isHiddenEnabled: Boolean, isPaid: Boolean): Boolean =
        if (!isPaid) true else (isHiddenEnabled || (!photo.isSensitive && !photo.isSensitiveInherited))
}