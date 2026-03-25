@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.core.sharedcomponents.serializable
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.photos.FilterCameraUploadMediaUseCase
import mega.privacy.android.domain.usecase.photos.FilterCloudDriveMediaUseCase
import mega.privacy.android.domain.usecase.photos.MonitorMediaTypedNodesUseCase
import mega.privacy.android.feature.photos.model.TimelinePhotosSource
import javax.inject.Inject

class TimelineImageNodeFetcher @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val monitorMediaTypedNodesUseCase: MonitorMediaTypedNodesUseCase,
    private val filterCloudDriveMediaUseCase: FilterCloudDriveMediaUseCase,
    private val filterCameraUploadMediaUseCase: FilterCameraUploadMediaUseCase,
) : ImageNodeFetcher {

    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        val sortType = bundle.serializable<Sort>(TIMELINE_SORT_TYPE) ?: Sort.NEWEST
        val mediaType =
            bundle.serializable<FilterMediaType>(TIMELINE_FILTER_TYPE) ?: FilterMediaType.ALL_MEDIA
        val source = bundle.serializable<TimelinePhotosSource>(TIMELINE_MEDIA_SOURCE)
            ?: TimelinePhotosSource.ALL_PHOTOS

        return monitorMediaTypedNodesUseCase().mapLatest { imageNodes ->
            val filteredImageNodes = filterImageNodes(imageNodes, mediaType, source)
            val sortResult = sortImageNodes(filteredImageNodes, sortType)
            sortResult.toImageNodes()
        }.flowOn(defaultDispatcher)
    }

    private suspend fun filterImageNodes(
        imageNodes: List<TypedFileNode>,
        mediaType: FilterMediaType,
        source: TimelinePhotosSource,
    ): List<TypedFileNode> {
        val filteredImageNodes = when (source) {
            TimelinePhotosSource.ALL_PHOTOS -> imageNodes
            TimelinePhotosSource.CLOUD_DRIVE -> filterCloudDriveMediaUseCase(imageNodes)
            TimelinePhotosSource.CAMERA_UPLOAD -> filterCameraUploadMediaUseCase(imageNodes)
        }

        return when (mediaType) {
            FilterMediaType.ALL_MEDIA -> filteredImageNodes
            FilterMediaType.IMAGES -> filteredImageNodes.filter { it.type is ImageFileTypeInfo }
            FilterMediaType.VIDEOS -> filteredImageNodes.filter { it.type is VideoFileTypeInfo }
        }
    }

    private fun sortImageNodes(
        imageNodes: List<TypedFileNode>,
        sortType: Sort,
    ): List<TypedFileNode> {
        return when (sortType) {
            Sort.NEWEST -> imageNodes.sortedWith(compareByDescending<TypedFileNode> { it.modificationTime }.thenByDescending { it.id.longValue })
            Sort.OLDEST -> imageNodes.sortedWith(compareBy<TypedFileNode> { it.modificationTime }.thenByDescending { it.id.longValue })
            else -> imageNodes
        }
    }

    private fun List<TypedFileNode>.toImageNodes(): List<ImageNode> = map {
        object : ImageNode {
            override val id = it.id
            override val name = it.name
            override val size = it.size
            override val label = it.label
            override val nodeLabel = it.nodeLabel
            override val parentId = it.parentId
            override val base64Id = it.base64Id
            override val restoreId = it.restoreId
            override val creationTime = it.creationTime
            override val modificationTime = it.modificationTime
            override val thumbnailPath = null
            override val previewPath = null
            override val fullSizePath = null
            override val type = it.type
            override val isFavourite = it.isFavourite
            override val isMarkedSensitive = it.isMarkedSensitive
            override val isSensitiveInherited = it.isSensitiveInherited
            override val exportedData = it.exportedData
            override val isTakenDown = it.isTakenDown
            override val isIncomingShare = it.isIncomingShare
            override val fingerprint = it.fingerprint
            override val originalFingerprint = it.originalFingerprint
            override val isNodeKeyDecrypted = it.isNodeKeyDecrypted
            override val hasThumbnail = it.hasThumbnail
            override val hasPreview = it.hasPreview
            override val downloadThumbnail: suspend (String) -> String = { _ -> "" }
            override val downloadPreview: suspend (String) -> String = { _ -> "" }
            override val downloadFullImage: (String, Boolean, () -> Unit) -> Flow<ImageProgress> =
                { _, _, _ -> flowOf() }
            override val latitude = 0.0
            override val longitude = 0.0
            override val serializedData = it.serializedData
            override val isAvailableOffline: Boolean = it.isAvailableOffline
            override val versionCount: Int = it.versionCount
            override val description: String? = it.description
            override val tags: List<String>? = it.tags
        }
    }

    internal companion object {
        const val TIMELINE_SORT_TYPE = "sortType"
        const val TIMELINE_FILTER_TYPE = "filterType"
        const val TIMELINE_MEDIA_SOURCE = "MediaSource"
    }
}

