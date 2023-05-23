package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.facade.AlbumStringResourceGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.CreateSetElementListenerInterface
import mega.privacy.android.data.listener.DisableExportSetsListenerInterface
import mega.privacy.android.data.listener.ExportSetsListenerInterface
import mega.privacy.android.data.listener.GetPreviewElementNodeListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.RemoveSetElementListenerInterface
import mega.privacy.android.data.mapper.PhotoMapper
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumIdLink
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.AlbumPhotosAddingProgress
import mega.privacy.android.domain.entity.photos.AlbumPhotosRemovingProgress
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal typealias AlbumPhotosAddingProgressPool = MutableMap<AlbumId, MutableSharedFlow<AlbumPhotosAddingProgress?>>
internal typealias AlbumPhotosRemovingProgressPool = MutableMap<AlbumId, MutableSharedFlow<AlbumPhotosRemovingProgress?>>

/**
 * Default [AlbumRepository] implementation
 */
@Singleton
internal class DefaultAlbumRepository @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val megaApiGateway: MegaApiGateway,
    private val userSetMapper: UserSetMapper,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val albumStringResourceGateway: AlbumStringResourceGateway,
    private val photoMapper: PhotoMapper,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AlbumRepository {
    private val userSets: MutableMap<Long, UserSet> = mutableMapOf()

    private val nodeSetsMap: MutableMap<NodeId, MutableSet<Long>> = mutableMapOf()

    private val userSetsFlow: MutableSharedFlow<List<UserSet>> = MutableSharedFlow(replay = 1)

    private val userSetsElementsFlow: MutableSharedFlow<List<UserSet>> =
        MutableSharedFlow(replay = 1)

    private val albumElements: MutableMap<AlbumId, List<AlbumPhotoId>> = mutableMapOf()

    private val albumPhotosAddingProgressPool: AlbumPhotosAddingProgressPool = mutableMapOf()

    private val albumPhotosRemovingProgressPool: AlbumPhotosRemovingProgressPool = mutableMapOf()

    private val publicNodesMap: MutableMap<NodeId, MegaNode> = mutableMapOf()

    @Volatile
    private var publicNodesDataMap: Map<NodeId, String> = mapOf()

    private var monitorNodeUpdatesJob: Job? = null

    @Volatile
    private var isMonitoringInitiated: Boolean = false

    private fun monitorNodeUpdates() {
        monitorNodeUpdatesJob?.cancel()
        monitorNodeUpdatesJob = nodeRepository.monitorNodeUpdates()
            .onEach { nodeUpdate ->
                val userSets = nodeUpdate.changes.keys
                    .flatMap { node ->
                        val setIds = nodeSetsMap[node.id] ?: emptySet()
                        setIds.mapNotNull { userSets[it] }
                    }.distinctBy { it.id }

                if (userSets.isNotEmpty()) {
                    userSetsFlow.tryEmit(userSets)
                    userSetsElementsFlow.tryEmit(userSets)
                }
            }.launchIn(appScope)
    }

    override suspend fun createAlbum(name: String): UserSet = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            megaApiGateway.createSet(
                name,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            val newSet = request.megaSet
                            continuation.resumeWith(
                                Result.success(
                                    userSetMapper(
                                        newSet.id(),
                                        newSet.name(),
                                        newSet.cover(),
                                        newSet.cts(),
                                        newSet.ts(),
                                        newSet.isExported,
                                    )
                                )
                            )
                        } else {
                            Timber.e("Error creating new album: ${error.errorString}")
                            continuation.failWithError(error, "createAlbum")
                        }
                    }
                )
            )
        }
    }

    override suspend fun getAllUserSets(): List<UserSet> {
        if (!isMonitoringInitiated) {
            isMonitoringInitiated = true
            monitorNodeUpdates()
        }

        return withContext(ioDispatcher) {
            val setList = megaApiGateway.getSets()
            userSets.clear()

            (0 until setList.size()).map { index ->
                val userSet = setList.get(index).toUserSet()
                userSets[userSet.id] = userSet
                userSet
            }
        }
    }

    override suspend fun getUserSet(albumId: AlbumId): UserSet? =
        userSets[albumId.id] ?: withContext(ioDispatcher) {
            megaApiGateway.getSet(sid = albumId.id)?.toUserSet()?.also {
                userSets[it.id] = it
            }
        }

    override fun monitorUserSetsUpdate(): Flow<List<UserSet>> = merge(
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnSetsUpdate>()
            .mapNotNull { it.sets }
            .map { sets -> sets.map { it.toUserSet() } },
        userSetsFlow
            .filter { it.isNotEmpty() }
            .onEach { sets ->
                sets.forEach { albumElements.remove(AlbumId(it.id)) }
            },
    )

    override suspend fun getAlbumElementIDs(albumId: AlbumId): List<AlbumPhotoId> =
        albumElements[albumId] ?: withContext(ioDispatcher) {
            val elementList = megaApiGateway.getSetElements(sid = albumId.id)
            (0 until elementList.size()).mapNotNull { index ->
                val element = elementList[index]
                val sets = nodeSetsMap.getOrPut(NodeId(element.node())) { mutableSetOf() }
                sets.add(element.setId())

                if (isNodeInRubbish(element.node())) null
                else element.toAlbumPhotoId()
            }.also { albumElements[albumId] = it }
        }

    override fun monitorAlbumElementIds(albumId: AlbumId): Flow<List<AlbumPhotoId>> = merge(
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnSetElementsUpdate>()
            .mapNotNull { it.elements }
            .map { elements -> elements.filter { it.setId() == albumId.id } }
            .onEach(::checkSetsCoverRemoved)
            .map { listOf(AlbumPhotoId.default) }
            .onEach { albumElements.remove(albumId) },
        userSetsElementsFlow
            .mapNotNull { sets -> sets.find { it.id == albumId.id } }
            .map { listOf(AlbumPhotoId.default) }
            .onEach { albumElements.remove(albumId) },
    )

    private fun checkSetsCoverRemoved(elements: List<MegaSetElement>) {
        val userSets = elements.mapNotNull { element ->
            val userSet = userSets[element.setId()]
            userSet.takeIf { element.id() == userSet?.cover }
        }

        if (userSets.isEmpty()) return
        userSetsFlow.tryEmit(userSets)
    }

    override suspend fun addPhotosToAlbum(albumID: AlbumId, photoIDs: List<NodeId>) =
        withContext(ioDispatcher) {
            val progressFlow = getAlbumPhotosAddingProgressFlow(albumID)
            progressFlow.tryEmit(
                AlbumPhotosAddingProgress(
                    isProgressing = true,
                    totalAddedPhotos = 0,
                )
            )

            val listener = CreateSetElementListenerInterface(
                target = photoIDs.size,
                onCompletion = { success, _ ->
                    progressFlow.tryEmit(
                        AlbumPhotosAddingProgress(
                            isProgressing = false,
                            totalAddedPhotos = success,
                        )
                    )
                }
            )

            for (photoID in photoIDs) {
                megaApiGateway.createSetElement(albumID.id, photoID.longValue, listener)
            }
        }

    override suspend fun removePhotosFromAlbum(albumID: AlbumId, photoIDs: List<AlbumPhotoId>) =
        withContext(ioDispatcher) {
            val progressFlow = getAlbumPhotosRemovingProgressFlow(albumID)
            progressFlow.tryEmit(
                AlbumPhotosRemovingProgress(
                    isProgressing = true,
                    totalRemovedPhotos = 0,
                )
            )

            val listener = RemoveSetElementListenerInterface(
                target = photoIDs.size,
                onCompletion = { success, _ ->
                    progressFlow.tryEmit(
                        AlbumPhotosRemovingProgress(
                            isProgressing = false,
                            totalRemovedPhotos = success,
                        )
                    )
                }
            )

            for (photoID in photoIDs) {
                megaApiGateway.removeSetElement(albumID.id, photoID.id, listener)
            }
        }

    override suspend fun removeAlbums(albumIds: List<AlbumId>) = withContext(ioDispatcher) {
        albumIds.map { albumId ->
            async {
                suspendCoroutine { continuation ->
                    megaApiGateway.removeSet(
                        albumId.id,
                        OptionalMegaRequestListenerInterface(
                            onRequestFinish = { _, error ->
                                if (error.errorCode == MegaError.API_OK) {
                                    continuation.resumeWith(Result.success(Unit))
                                } else {
                                    continuation.failWithError(error, "removeAlbums")
                                }
                            }
                        ),
                    )
                }
            }
        }.joinAll()
    }

    override fun observeAlbumPhotosAddingProgress(albumId: AlbumId): Flow<AlbumPhotosAddingProgress?> =
        getAlbumPhotosAddingProgressFlow(albumId).distinctUntilChanged()

    override suspend fun updateAlbumPhotosAddingProgressCompleted(albumId: AlbumId) {
        val progressFlow = getAlbumPhotosAddingProgressFlow(albumId)
        progressFlow.tryEmit(null)
    }

    override fun observeAlbumPhotosRemovingProgress(albumId: AlbumId): Flow<AlbumPhotosRemovingProgress?> =
        getAlbumPhotosRemovingProgressFlow(albumId).distinctUntilChanged()

    override suspend fun updateAlbumPhotosRemovingProgressCompleted(albumId: AlbumId) {
        val progressFlow = getAlbumPhotosRemovingProgressFlow(albumId)
        progressFlow.tryEmit(null)
    }

    override suspend fun updateAlbumName(
        albumId: AlbumId,
        newName: String,
    ): String = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("updateAlbumName") {
                return@getRequestListener it.text
            }
            megaApiGateway.updateSetName(
                sid = albumId.id,
                name = newName,
                listener = listener
            )
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
    }

    override suspend fun getProscribedAlbumTitles(): List<String> =
        albumStringResourceGateway.getSystemAlbumNames() + albumStringResourceGateway.getProscribedStrings()

    override suspend fun updateAlbumCover(albumId: AlbumId, elementId: NodeId) =
        withContext(ioDispatcher) {
            megaApiGateway.putSetCover(
                sid = albumId.id,
                eid = elementId.longValue,
            )
        }

    override suspend fun exportAlbums(albumIds: List<AlbumId>): List<AlbumIdLink> =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = ExportSetsListenerInterface(
                    totalSets = albumIds.size,
                    onCompletion = { setLinks ->
                        val albumIdsLinks = setLinks.map { (sid, link) ->
                            AlbumId(sid) to AlbumLink(link)
                        }
                        continuation.resumeWith(Result.success(albumIdsLinks))
                    },
                )

                if (albumIds.isNotEmpty()) {
                    for (albumId in albumIds) {
                        megaApiGateway.exportSet(
                            sid = albumId.id,
                            listener = listener,
                        )
                    }
                } else {
                    continuation.resumeWith(Result.success(listOf()))
                }

                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun disableExportAlbums(albumIds: List<AlbumId>) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = DisableExportSetsListenerInterface(
                totalSets = albumIds.size,
                onCompletion = { success, _ ->
                    continuation.resumeWith(Result.success(success))
                }
            )

            if (albumIds.isNotEmpty()) {
                for (albumId in albumIds) {
                    megaApiGateway.disableExportSet(
                        sid = albumId.id,
                        listener = listener,
                    )
                }
            } else {
                continuation.resumeWith(Result.success(0))
            }

            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun fetchPublicAlbum(albumLink: AlbumLink) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        launch {
                            val userSet = request.megaSet?.toUserSet()
                            val albumPhotoIds = request.megaSetElementList?.let { elementList ->
                                (0 until elementList.size()).map { index ->
                                    elementList.get(index).toAlbumPhotoId()
                                }
                            }.orEmpty()

                            if (userSet != null) {
                                continuation.resume(userSet to albumPhotoIds)
                            } else {
                                continuation.failWithError(error, "fetchPublicAlbum")
                            }
                        }
                    } else {
                        continuation.failWithError(error, "fetchPublicAlbum")
                    }
                },
            )

            megaApiGateway.stopPublicSetPreview()

            megaApiGateway.fetchPublicSet(
                publicSetLink = albumLink.link,
                listener = listener,
            )

            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun getPublicPhotos(albumPhotoIds: List<AlbumPhotoId>): List<Photo> {
        return withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val nodeAlbumPhotoIdMap = albumPhotoIds.associateBy(
                    keySelector = { it.nodeId.longValue },
                    valueTransform = { it },
                )

                val listener = GetPreviewElementNodeListenerInterface(
                    nodeAlbumPhotoIdMap = nodeAlbumPhotoIdMap,
                    onCompletion = { nodeAlbumPhotoIdPairs ->
                        launch {
                            publicNodesMap.clear()

                            val photos = nodeAlbumPhotoIdPairs.mapNotNull { (node, albumPhotoId) ->
                                publicNodesMap[NodeId(node.handle)] = node
                                photoMapper(node, albumPhotoId)
                            }

                            publicNodesDataMap = publicNodesMap.mapValues { (_, node) ->
                                node.serialize()
                            }
                            continuation.resume(photos)
                        }
                    }
                )

                if (albumPhotoIds.isNotEmpty()) {
                    for (albumPhotoId in albumPhotoIds) {
                        megaApiGateway.getPreviewElementNode(
                            eid = albumPhotoId.id,
                            listener = listener,
                        )
                    }
                } else {
                    continuation.resume(listOf())
                }

                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }
    }

    override suspend fun downloadPublicThumbnail(photo: Photo, callback: (Boolean) -> Unit) {
        withContext(ioDispatcher) {
            val node = publicNodesMap[NodeId(photo.id)]
            val thumbnailFilePath = photo.thumbnailFilePath

            if (thumbnailFilePath.isNullOrBlank()) {
                callback(false)
            } else if (File(thumbnailFilePath).exists()) {
                callback(true)
            } else if (node == null) {
                callback(false)
            } else {
                megaApiGateway.getThumbnail(
                    node = node,
                    thumbnailFilePath = thumbnailFilePath,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = { _, error ->
                            callback(error.errorCode == MegaError.API_OK)
                        },
                    ),
                )
            }
        }
    }

    override suspend fun downloadPublicPreview(photo: Photo, callback: (Boolean) -> Unit) {
        withContext(ioDispatcher) {
            val node = publicNodesMap[NodeId(photo.id)]
            val previewFilePath = photo.previewFilePath

            if (previewFilePath.isNullOrBlank()) {
                callback(false)
            } else if (File(previewFilePath).exists()) {
                callback(true)
            } else if (node == null) {
                callback(false)
            } else {
                megaApiGateway.getPreview(
                    node = node,
                    previewFilePath = previewFilePath,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = { _, error ->
                            callback(error.errorCode == MegaError.API_OK)
                        },
                    ),
                )
            }
        }
    }

    override fun getPublicAlbumNodesData(): Map<NodeId, String> = publicNodesDataMap

    override fun clearCache() {
        monitorNodeUpdatesJob?.cancel()
        monitorNodeUpdatesJob = null

        isMonitoringInitiated = false

        userSets.clear()
        nodeSetsMap.clear()
        albumElements.clear()
        publicNodesMap.clear()
        publicNodesDataMap = mapOf()
        albumPhotosAddingProgressPool.clear()
        albumPhotosRemovingProgressPool.clear()

        userSetsFlow.tryEmit(listOf())
        userSetsElementsFlow.tryEmit(listOf())
    }

    private fun getAlbumPhotosAddingProgressFlow(albumId: AlbumId): MutableSharedFlow<AlbumPhotosAddingProgress?> =
        albumPhotosAddingProgressPool.getOrPut(albumId) { MutableSharedFlow(replay = 1) }

    private fun getAlbumPhotosRemovingProgressFlow(albumId: AlbumId): MutableSharedFlow<AlbumPhotosRemovingProgress?> =
        albumPhotosRemovingProgressPool.getOrPut(albumId) { MutableSharedFlow(replay = 1) }

    private fun MegaSet.toUserSet(): UserSet {
        val cover = cover().takeIf { it != -1L }
        return userSetMapper(
            id(),
            name(),
            cover,
            cts(),
            ts(),
            isExported
        )
    }

    private fun MegaSetElement.toAlbumPhotoId(): AlbumPhotoId = AlbumPhotoId(
        id = id(),
        nodeId = NodeId(node()),
        albumId = AlbumId(setId()),
    )
}
