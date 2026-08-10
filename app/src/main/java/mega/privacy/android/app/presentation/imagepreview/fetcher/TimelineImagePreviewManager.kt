package mega.privacy.android.app.presentation.imagepreview.fetcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.ImageNodeInfo
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.domain.qualifier.IoDispatcher
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
 * Builds the image viewer's ordering as a flat list of lightweight [ImageNodeInfo] refs, section by
 * section so it matches the timeline grid's order. Full nodes are resolved by id on demand. A fresh
 * instance is created per viewer session.
 */
class TimelineImagePreviewManager @Inject constructor(
    private val getMediaTimelineSectionsUseCase: GetMediaTimelineSectionsUseCase,
    private val listTimelineImageNodeInfoByOffsetUseCase: ListTimelineImageNodeInfoByOffsetUseCase,
    private val getCameraUploadFolderHandlesUseCase: GetCameraUploadFolderHandlesUseCase,
    private val timelineFilterUiStateMapper: TimelineFilterUiStateMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private var infos: List<ImageNodeInfo> = emptyList()

    /**
     * Loads the ordering for the given sort/filter/source and returns the total media count.
     */
    suspend fun initialize(
        sort: Sort,
        mediaType: FilterMediaType,
        source: TimelinePhotosSource,
        hideSensitive: Boolean,
    ): Int = withContext(ioDispatcher) {
        val order = sort.toSortOrder()
        val filter = buildFilter(mediaType, source, hideSensitive)
        val sections = runCatching { getMediaTimelineSectionsUseCase(filter, order) }
            .onFailure { Timber.e(it, "Failed to load timeline sections") }
            .getOrDefault(emptyList())
            .distinctBy { it.groupId }
        infos = sections.flatMap { section ->
            runCatching {
                listTimelineImageNodeInfoByOffsetUseCase(
                    filter = filter,
                    section = section,
                    order = order,
                    maxElements = section.count.toInt(),
                    offset = 0L,
                )
            }.onFailure {
                Timber.e(it, "Failed to load refs for ${section.groupId}")
            }.getOrDefault(emptyList())
        }
        infos.size
    }

    /**
     * The [ImageNodeInfo] at global [index], or null when out of range.
     */
    fun getInfoAtIndex(index: Int): ImageNodeInfo? = infos.getOrNull(index)

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
}
