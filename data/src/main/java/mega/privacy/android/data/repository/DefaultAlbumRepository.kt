package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
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
        (0 until setList.size()).map {
            with(setList.get(it)) {
                userSetMapper(id(), name(), cover(), ts())
            }
        }
    }

    override suspend fun getUserSet(albumId: AlbumId): UserSet? = withContext(ioDispatcher) {
        megaApiGateway.getSet(sid = albumId.id)?.let { set ->
            userSetMapper(set.id(), set.name(), set.cover(), set.ts())
        }
    }

    override fun monitorUserSetsUpdate(): Flow<List<UserSet>> = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnSetsUpdate>()
        .mapNotNull { (sets) ->
            sets?.map { set ->
                userSetMapper(set.id(), set.name(), set.cover(), set.ts())
            }
        }

    override suspend fun getAlbumElementIDs(albumId: AlbumId): List<NodeId> =
        withContext(ioDispatcher) {
            val elementList = megaApiGateway.getSetElements(sid = albumId.id)
            (0 until elementList.size()).map {
                NodeId(elementList.get(it).node())
            }
        }

    override fun monitorAlbumElementIds(): Flow<List<NodeId>> = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnSetElementsUpdate>()
        .mapNotNull { (elements) ->
            elements?.map { element ->
                NodeId(id = element.node())
            }
        }

    override suspend fun addPhotosToAlbum(albumID: AlbumId, photoIDs: List<NodeId>) =
        withContext(ioDispatcher) {
            for (photoID in photoIDs) {
                megaApiGateway.createSetElement(albumID.id, photoID.id)
            }
        }
}
