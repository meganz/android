@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.core.sharedcomponents.serializable
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.FilterCameraUploadImageNodesUseCase
import mega.privacy.android.domain.usecase.FilterCloudDriveImageNodesUseCase
import mega.privacy.android.domain.usecase.photos.ListTimelineImageNodeInfoByOffsetUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelineNodesUseCase
import mega.privacy.android.feature.photos.model.TimelinePhotosSource
import timber.log.Timber
import javax.inject.Inject

class TimelineImageNodeFetcher @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val monitorTimelineNodesUseCase: MonitorTimelineNodesUseCase,
    private val filterCloudDriveImageNodesUseCase: FilterCloudDriveImageNodesUseCase,
    private val filterCameraUploadImageNodesUseCase: FilterCameraUploadImageNodesUseCase,
    private val listTimelineImageNodeInfoByOffsetUseCase: ListTimelineImageNodeInfoByOffsetUseCase,
    private val timelineMediaFilterBuilder: TimelineMediaFilterBuilder,
) : ImageNodeFetcher {

    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        val sortOrder = bundle.readEnum<SortOrder>(TIMELINE_SORT_ORDER)
            ?: when (bundle.readEnum<Sort>(TIMELINE_SORT_TYPE) ?: Sort.NEWEST) {
                Sort.NEWEST -> SortOrder.ORDER_MODIFICATION_DESC
                Sort.OLDEST -> SortOrder.ORDER_MODIFICATION_ASC
                else -> null
            }
        val mediaType = bundle.readEnum<FilterMediaType>(TIMELINE_FILTER_TYPE)
            ?: FilterMediaType.ALL_MEDIA
        val source = bundle.readEnum<TimelinePhotosSource>(TIMELINE_MEDIA_SOURCE)
            ?: TimelinePhotosSource.ALL_PHOTOS

        return monitorTimelineNodesUseCase()
            .mapLatest { imageNodes ->
                val filteredImageNodes = filterImageNodes(imageNodes, mediaType, source)
                sortImageNodes(filteredImageNodes, sortOrder, mediaType, source)
            }.flowOn(defaultDispatcher)
    }

    private suspend fun filterImageNodes(
        imageNodes: List<ImageNode>,
        mediaType: FilterMediaType,
        source: TimelinePhotosSource,
    ): List<ImageNode> {
        val filteredImageNodes = when (source) {
            TimelinePhotosSource.ALL_PHOTOS -> imageNodes
            TimelinePhotosSource.CLOUD_DRIVE -> filterCloudDriveImageNodesUseCase(imageNodes)
            TimelinePhotosSource.CAMERA_UPLOAD -> filterCameraUploadImageNodesUseCase(imageNodes)
        }

        return when (mediaType) {
            FilterMediaType.ALL_MEDIA -> filteredImageNodes
            FilterMediaType.IMAGES -> filteredImageNodes.filter { it.type is ImageFileTypeInfo }
            FilterMediaType.VIDEOS -> filteredImageNodes.filter { it.type is VideoFileTypeInfo }
        }
    }

    private suspend fun sortImageNodes(
        imageNodes: List<ImageNode>,
        sortOrder: SortOrder?,
        mediaType: FilterMediaType,
        source: TimelinePhotosSource,
    ): List<ImageNode> = when (sortOrder) {
        SortOrder.ORDER_MODIFICATION_DESC, SortOrder.ORDER_MODIFICATION_ASC,
        SortOrder.ORDER_MEDIATS_DESC, SortOrder.ORDER_MEDIATS_ASC,
            -> sortBySdkOrder(imageNodes, sortOrder, mediaType, source)
                ?: sortByModificationTime(imageNodes, sortOrder)

        else -> imageNodes
    }

    /**
     * Orders the nodes by the grid's own SDK query, so ties match the grid. Null on failure.
     */
    private suspend fun sortBySdkOrder(
        imageNodes: List<ImageNode>,
        sortOrder: SortOrder,
        mediaType: FilterMediaType,
        source: TimelinePhotosSource,
    ): List<ImageNode>? {
        val orderedIds = runCatching {
            listTimelineImageNodeInfoByOffsetUseCase(
                filter = timelineMediaFilterBuilder(mediaType, source, hideSensitive = false),
                section = null,
                order = sortOrder,
                maxElements = 0,
                offset = 0,
            )
        }.onFailure { Timber.e(it, "Failed to load the timeline ordering") }
            .getOrNull()
            ?.map { it.id }
            ?: return null
        val nodesById = imageNodes.associateBy { it.id }
        return orderedIds.mapNotNull { nodesById[it] }
    }

    private fun sortByModificationTime(imageNodes: List<ImageNode>, sortOrder: SortOrder) =
        when (sortOrder) {
            SortOrder.ORDER_MODIFICATION_DESC -> imageNodes.sortedWith(
                compareByDescending<ImageNode> { it.modificationTime }
                    .thenByDescending { it.id.longValue }
            )

            SortOrder.ORDER_MODIFICATION_ASC -> imageNodes.sortedWith(
                compareBy<ImageNode> { it.modificationTime }.thenBy { it.id.longValue }
            )

            else -> imageNodes
        }

    internal companion object {
        const val TIMELINE_SORT_TYPE = "sortType"

        /**
         * The exact [mega.privacy.android.domain.entity.SortOrder] the grid queried with. Read by the
         * paginated viewer ([TimelineImagePreviewManager]), which re-queries the timeline and so needs
         * the timestamp column too, not just the newest / oldest direction of [TIMELINE_SORT_TYPE].
         */
        const val TIMELINE_SORT_ORDER = "sortOrder"
        const val TIMELINE_FILTER_TYPE = "filterType"
        const val TIMELINE_MEDIA_SOURCE = "MediaSource"

        internal inline fun <reified T : Enum<T>> Bundle.readEnum(key: String): T? =
            getString(key)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
                ?: serializable<T>(key)

        /**
         * The grid's [TIMELINE_SORT_ORDER] column in the given direction.
         */
        internal fun Bundle.timelineSortOrderWithDirection(ascending: Boolean): SortOrder =
            when (readEnum<SortOrder>(TIMELINE_SORT_ORDER)) {
                SortOrder.ORDER_MEDIATS_ASC, SortOrder.ORDER_MEDIATS_DESC ->
                    if (ascending) SortOrder.ORDER_MEDIATS_ASC else SortOrder.ORDER_MEDIATS_DESC

                else -> if (ascending) SortOrder.ORDER_MODIFICATION_ASC else SortOrder.ORDER_MODIFICATION_DESC
            }
    }
}
