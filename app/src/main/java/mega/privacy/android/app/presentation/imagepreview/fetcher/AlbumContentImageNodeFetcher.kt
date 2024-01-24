package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.presentation.photos.albums.model.AlbumType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.MonitorCustomAlbumNodesUseCase
import mega.privacy.android.domain.usecase.MonitorFavouriteAlbumNodesUseCase
import mega.privacy.android.domain.usecase.MonitorGifAlbumNodesUseCase
import mega.privacy.android.domain.usecase.MonitorRawAlbumNodesUseCase
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumContentImageNodeFetcher @Inject constructor(
    private val monitorFavouriteAlbumNodesUseCase: MonitorFavouriteAlbumNodesUseCase,
    private val monitorGifAlbumNodesUseCase: MonitorGifAlbumNodesUseCase,
    private val monitorRawAlbumNodesUseCase: MonitorRawAlbumNodesUseCase,
    private val monitorCustomAlbumNodesUseCase: MonitorCustomAlbumNodesUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return when (bundle.serializable<AlbumType>(ALBUM_TYPE)) {
            AlbumType.Favourite -> {
                monitorFavouriteAlbumNodesUseCase()
            }

            AlbumType.Gif -> {
                monitorGifAlbumNodesUseCase()
            }

            AlbumType.Raw -> {
                monitorRawAlbumNodesUseCase()
            }

            AlbumType.Custom -> {
                val id = bundle.getLong(CUSTOM_ALBUM_ID)
                monitorCustomAlbumNodesUseCase(albumId = AlbumId(id))
            }

            else -> {
                emptyFlow()
            }
        }.mapLatest { imageNodes ->
            when (bundle.serializable<Sort>(ALBUM_SORT_TYPE)) {
                Sort.NEWEST -> imageNodes.sortedWith(compareByDescending<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
                Sort.OLDEST -> imageNodes.sortedWith(compareBy<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
                else -> imageNodes
            }
        }.flowOn(defaultDispatcher)
    }

    internal companion object {
        const val ALBUM_TYPE: String = "albumType"

        const val CUSTOM_ALBUM_ID: String = "customAlbumId"

        const val ALBUM_SORT_TYPE: String = "albumSortType"
    }
}
