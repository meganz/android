package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.audios.TypedAudioNodeMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AudioSectionRepository
import nz.mega.sdk.MegaApiJava.FILE_TYPE_AUDIO
import nz.mega.sdk.MegaApiJava.SEARCH_TARGET_ROOTNODE
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Implementation of AudioSectionRepository
 */
internal class AudioSectionRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val fileNodeMapper: FileNodeMapper,
    private val typedAudioNodeMapper: TypedAudioNodeMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AudioSectionRepository {
    override suspend fun getAllAudios(order: SortOrder): List<TypedAudioNode> =
        withContext(ioDispatcher) {
            val offlineItems = getAllOfflineNodeHandle()
            val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
            megaApiGateway.searchByType(
                megaCancelToken,
                sortOrderIntMapper(order),
                FILE_TYPE_AUDIO,
                SEARCH_TARGET_ROOTNODE
            ).map { megaNode ->
                typedAudioNodeMapper(
                    fileNode = megaNode.convertToFileNode(
                        offlineItems?.get(megaNode.handle.toString())
                    ),
                    duration = megaNode.duration,
                )
            }
        }

    private suspend fun getAllOfflineNodeHandle() =
        megaLocalRoomGateway.getAllOfflineInfo()?.associateBy { it.handle }

    private suspend fun MegaNode.convertToFileNode(offline: Offline?) = fileNodeMapper(
        megaNode = this, requireSerializedData = false, offline = offline
    )
}