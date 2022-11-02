package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.UserSetMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Default [AlbumRepository] implementation
 */
internal class DefaultAlbumRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val userSetMapper: UserSetMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AlbumRepository {

    override suspend fun createAlbum(name: String) = withContext(ioDispatcher) {
        megaApiGateway.createSet(
            name,
            OptionalMegaRequestListenerInterface()
        )
    }

    override suspend fun getAllUserSets(): List<UserSet> = withContext(ioDispatcher) {
        val setList = megaApiGateway.getSets()
        (0 until setList.size()).map {
            with(setList.get(it)) {
                userSetMapper(id(), name(), cover())
            }
        }
    }

    override suspend fun getAlbumElementIDs(albumId: AlbumId): List<NodeId> =
        withContext(ioDispatcher) {
            val elementList = megaApiGateway.getSetElements(sid = albumId.id)
            (0 until elementList.size()).map {
                NodeId(elementList.get(it).node())
            }
        }

    override suspend fun addPhotosToAlbum(albumID: AlbumId, photoIDs: List<NodeId>) =
        withContext(ioDispatcher) {
            for (photoID in photoIDs) {
                megaApiGateway.createSetElement(albumID.id, photoID.id)
            }
        }
}
