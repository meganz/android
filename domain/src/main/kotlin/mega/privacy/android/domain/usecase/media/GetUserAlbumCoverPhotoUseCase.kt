package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.timeout
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class GetUserAlbumCoverPhotoUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
) {
    @OptIn(FlowPreview::class)
    suspend operator fun invoke(
        albumId: AlbumId,
        refresh: Boolean = false,
    ): TypedFileNode? {
        val albumPhotos = albumRepository
            .getAlbumElementIDs(albumId = albumId, refresh = refresh)
            .takeIf { it.isNotEmpty() }
            ?: return null

        val showHiddenItems = monitorShowHiddenItemsUseCase().first()
        val accountDetail = monitorAccountDetailUseCase()
            .filter { it.levelDetail != null }
            // prevents long waiting time, worst case is that it will use a different cover
            .timeout(2.seconds)
            .firstOrNull()
        val isPaid = accountDetail?.levelDetail?.accountType?.isPaid == true
        val isHiddenEnabled = showHiddenItems && isPaid

        return findValidCover(
            albumPhotos = albumPhotos,
            selectedCoverId = albumRepository.getUserSet(albumId)?.cover,
            isHiddenEnabled = isHiddenEnabled,
            isPaid = isPaid,
        )
    }

    private suspend fun findValidCover(
        albumPhotos: List<AlbumPhotoId>,
        selectedCoverId: Long?,
        isHiddenEnabled: Boolean,
        isPaid: Boolean,
    ): TypedFileNode? {
        val coverAlbumPhotoId = selectedCoverId?.let { coverId ->
            albumPhotos.find { it.id == coverId }
        }
        val coverNode = coverAlbumPhotoId?.let { getNode(it) }

        if (coverNode != null && isNodeVisible(coverNode, isHiddenEnabled, isPaid)) {
            return coverNode
        }

        return albumPhotos
            .mapNotNull { getNode(it) }
            .sortedWith(
                compareByDescending<TypedFileNode> {
                    it.modificationTime
                }.thenByDescending { it.id.longValue }
            )
            .firstOrNull { isNodeVisible(it, isHiddenEnabled, isPaid) }
    }

    private suspend fun getNode(albumPhotoId: AlbumPhotoId): TypedFileNode? =
        getNodeByIdUseCase(albumPhotoId.nodeId) as? TypedFileNode

    /**
     * Determines if a node should be visible for album cover selection.
     * isMarkedSensitive and isSensitiveInherited only apply when isPaid is true.
     * For free accounts, all nodes are considered visible.
     */
    private fun isNodeVisible(node: TypedFileNode, isHiddenEnabled: Boolean, isPaid: Boolean): Boolean =
        if (!isPaid) true else (isHiddenEnabled || (!node.isMarkedSensitive && !node.isSensitiveInherited))
}
