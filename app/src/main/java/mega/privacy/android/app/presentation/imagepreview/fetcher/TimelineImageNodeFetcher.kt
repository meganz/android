@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.FilterCameraUploadImageNodesUseCase
import mega.privacy.android.domain.usecase.FilterCloudDriveImageNodesUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelineNodesUseCase
import javax.inject.Inject

class TimelineImageNodeFetcher @Inject constructor(
    private val monitorTimelineNodesUseCase: MonitorTimelineNodesUseCase,
    private val filterCloudDriveImageNodesUseCase: FilterCloudDriveImageNodesUseCase,
    private val filterCameraUploadImageNodesUseCase: FilterCameraUploadImageNodesUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ImageNodeFetcher {

    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        val sortType = bundle.serializable<Sort>(TIMELINE_SORT_TYPE) ?: Sort.NEWEST
        val mediaType =
            bundle.serializable<FilterMediaType>(TIMELINE_FILTER_TYPE) ?: FilterMediaType.ALL_MEDIA
        val source = bundle.serializable<TimelinePhotosSource>(TIMELINE_MEDIA_SOURCE)
            ?: TimelinePhotosSource.ALL_PHOTOS

        return monitorTimelineNodesUseCase().mapLatest { imageNodes ->
            val filteredImageNodes = filterImageNodes(imageNodes, mediaType, source)
            sortImageNodes(filteredImageNodes, sortType)
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

    private fun sortImageNodes(imageNodes: List<ImageNode>, sortType: Sort): List<ImageNode> {
        return when (sortType) {
            Sort.NEWEST -> imageNodes.sortedWith(compareByDescending<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
            Sort.OLDEST -> imageNodes.sortedWith(compareBy<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
            else -> imageNodes
        }
    }

    internal companion object {
        const val TIMELINE_SORT_TYPE = "sortType"
        const val TIMELINE_FILTER_TYPE = "filterType"
        const val TIMELINE_MEDIA_SOURCE = "MediaSource"
    }
}

