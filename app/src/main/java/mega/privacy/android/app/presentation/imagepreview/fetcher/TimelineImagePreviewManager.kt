package mega.privacy.android.app.presentation.imagepreview.fetcher

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.ImageNodeInfo
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetImageNodeByIdUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadFolderHandlesUseCase
import mega.privacy.android.domain.usecase.photos.GetMediaTimelineSectionsUseCase
import mega.privacy.android.domain.usecase.photos.ListTimelineImageNodeInfoByOffsetUseCase
import mega.privacy.android.feature.photos.mapper.TimelineFilterUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.TimelinePhotosSource
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * The image viewer's paged timeline source. After [initialize] it knows the media count and resolves
 * the node at any global index — paging a lightweight id list on demand — or by id for the tapped
 * anchor. Index-based resolution suspends until [initialize] completes, so callers can query it as
 * soon as the pager renders. A fresh instance per viewer session.
 */
class TimelineImagePreviewManager @Inject constructor(
    private val getMediaTimelineSectionsUseCase: GetMediaTimelineSectionsUseCase,
    private val listTimelineImageNodeInfoByOffsetUseCase: ListTimelineImageNodeInfoByOffsetUseCase,
    private val getImageNodeByIdUseCase: GetImageNodeByIdUseCase,
    private val getCameraUploadFolderHandlesUseCase: GetCameraUploadFolderHandlesUseCase,
    private val timelineFilterUiStateMapper: TimelineFilterUiStateMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private lateinit var filter: MediaTimelineFilter
    private var order: SortOrder = SortOrder.ORDER_MODIFICATION_DESC
    private var total: Int = 0
    private val loadedInfos = mutableMapOf<Int, ImageNodeInfo>()
    private val mutex = Mutex()
    private val ready = CompletableDeferred<Unit>()

    /**
     * Builds the filter/order and resolves the total media count. Runs once per session and unblocks
     * [getImageNodeAtIndex]/[indexOfImageNode]. When [knownTotal] is positive (the grid already
     * summed the sections) it's used directly, skipping the expensive sections aggregate.
     */
    suspend fun initialize(
        sort: Sort,
        mediaType: FilterMediaType,
        source: TimelinePhotosSource,
        hideSensitive: Boolean,
        knownTotal: Int = 0,
    ): Int = withContext(ioDispatcher) {
        order = sort.toSortOrder()
        filter = buildFilter(mediaType, source, hideSensitive)
        total = if (knownTotal > 0) {
            knownTotal
        } else {
            runCatching { getMediaTimelineSectionsUseCase(filter, order) }
                .onFailure { Timber.e(it, "Failed to load timeline sections") }
                .getOrDefault(emptyList())
                .distinctBy { it.groupId }
                .sumOf { it.count.toInt() }
        }
        ready.complete(Unit)
        total
    }

    /**
     * The full node for [id], resolved directly — used to show the tapped node before the ordering is
     * ready. Null when it can't be resolved.
     */
    suspend fun getImageNode(id: NodeId) =
        runCatching { getImageNodeByIdUseCase(id) }
            .onFailure { Timber.e(it, "Failed to resolve image node $id") }
            .getOrNull()

    /**
     * The node at global [index], paging the id list as needed. Null when out of range or unresolved.
     */
    suspend fun getImageNodeAtIndex(index: Int): ImageNode? {
        ready.await()
        val id = mutex.withLock { getImageNodeInfoAt(index)?.id } ?: return null
        return getImageNode(id)
    }

    /**
     * [preferredIndex] corrected to where [nodeId] actually sits when the tapped node has shifted
     * since the grid computed the index; [preferredIndex] when it can't be located.
     */
    suspend fun indexOfImageNode(preferredIndex: Int, nodeId: NodeId): Int {
        ready.await()
        return mutex.withLock {
            if (getImageNodeInfoAt(preferredIndex)?.id == nodeId) {
                preferredIndex
            } else {
                loadedInfos.entries.firstOrNull { it.value.id == nodeId }?.key ?: preferredIndex
            }
        }
    }

    private suspend fun getImageNodeInfoAt(index: Int): ImageNodeInfo? {
        if (index !in 0 until total) return null
        if (!loadedInfos.containsKey(index)) {
            val pageStart = index - index % PAGE_SIZE
            fetchInfos(pageStart.toLong())
                .forEachIndexed { offset, info -> loadedInfos[pageStart + offset] = info }
        }
        return loadedInfos[index]
    }

    private suspend fun fetchInfos(offset: Long): List<ImageNodeInfo> = withContext(ioDispatcher) {
        runCatching {
            listTimelineImageNodeInfoByOffsetUseCase(
                filter = filter,
                section = null,
                order = order,
                maxElements = PAGE_SIZE,
                offset = offset,
            )
        }.onFailure { Timber.e(it, "Failed to load image node info at offset $offset") }
            .getOrDefault(emptyList())
    }

    private suspend fun buildFilter(
        mediaType: FilterMediaType,
        source: TimelinePhotosSource,
        hideSensitive: Boolean,
    ): MediaTimelineFilter {
        val cameraUploadFolderHandles = runCatching { getCameraUploadFolderHandlesUseCase() }
            .onFailure { Timber.e(it, "Failed to resolve camera upload folder handles") }
            .getOrDefault(emptyList())
        val baseFilter = timelineFilterUiStateMapper(
            TimelineFilterUiState(
                mediaType = mediaType,
                mediaSource = source.toFilterMediaSource()
            ),
            cameraUploadFolderHandles,
        )
        return baseFilter.copy(
            sensitivity = if (hideSensitive) {
                MediaTimelineFilter.Sensitivity.HideSensitive
            } else {
                MediaTimelineFilter.Sensitivity.ShowAll
            },
        )
    }

    private fun TimelinePhotosSource.toFilterMediaSource(): FilterMediaSource = when (this) {
        TimelinePhotosSource.ALL_PHOTOS -> FilterMediaSource.AllPhotos
        TimelinePhotosSource.CLOUD_DRIVE -> FilterMediaSource.CloudDrive
        TimelinePhotosSource.CAMERA_UPLOAD -> FilterMediaSource.CameraUpload
    }

    private fun Sort.toSortOrder(): SortOrder = when (this) {
        Sort.OLDEST -> SortOrder.ORDER_MODIFICATION_ASC
        else -> SortOrder.ORDER_MODIFICATION_DESC
    }

    companion object {
        private const val PAGE_SIZE = 30
    }
}
