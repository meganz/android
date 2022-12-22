package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * Default [AlbumRepository] implementation
 */
internal class DefaultAlbumRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val userSetMapper: UserSetMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AlbumRepository {

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
                                        newSet.ts(),
                                    )
                                )
                            )
                        } else {
                            Timber.e("Error creating new album: ${error.errorString}")
                            continuation.failWithError(error)
                        }
                    }
                )
            )
        }
    }

    override suspend fun getAllUserSets(): List<UserSet> = withContext(ioDispatcher) {
        val setList = megaApiGateway.getSets()
        (0 until setList.size()).map { index ->
            setList.get(index).toUserSet()
        }
    }

    override suspend fun getUserSet(albumId: AlbumId): UserSet? = withContext(ioDispatcher) {
        megaApiGateway.getSet(sid = albumId.id)?.toUserSet()
    }

    override fun monitorUserSetsUpdate(): Flow<List<UserSet>> = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnSetsUpdate>()
        .mapNotNull { it.sets }
        .map { sets -> sets.map { it.toUserSet() } }

    override suspend fun getAlbumElementIDs(albumId: AlbumId): List<NodeId> =
        withContext(ioDispatcher) {
            val elementList = megaApiGateway.getSetElements(sid = albumId.id)
            (0 until elementList.size()).map { index ->
                elementList.get(index).toNodeId()
            }
        }

    override fun monitorAlbumElementIds(albumId: AlbumId): Flow<List<NodeId>> =
        megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnSetElementsUpdate>()
            .mapNotNull { it.elements }
            .map { elements -> elements.filter { it.setId() == albumId.id } }
            .map { elements -> elements.map { it.toNodeId() } }

    override suspend fun addPhotosToAlbum(albumID: AlbumId, photoIDs: List<NodeId>) =
        withContext(ioDispatcher) {
            for (photoID in photoIDs) {
                megaApiGateway.createSetElement(albumID.id, photoID.id)
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
                                    continuation.failWithError(error)
                                }
                            }
                        ),
                    )
                }
            }
        }.joinAll()
    }

    private fun MegaSet.toUserSet(): UserSet = userSetMapper(id(), name(), cover(), ts())

    private fun MegaSetElement.toNodeId(): NodeId = NodeId(id = node())
}
