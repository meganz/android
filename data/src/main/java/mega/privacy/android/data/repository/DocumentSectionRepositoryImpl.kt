package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.DocumentSectionRepository
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Implementation of DocumentSectionRepository
 */
internal class DocumentSectionRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val nodeMapper: NodeMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val megaSearchFilterMapper: MegaSearchFilterMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DocumentSectionRepository {
    override suspend fun getAllDocuments(order: SortOrder): List<UnTypedNode> =
        withContext(ioDispatcher) {
            val offlineItems = getAllOfflineNodeHandle()
            val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
            val filter = megaSearchFilterMapper(
                searchTarget = SearchTarget.ROOT_NODES,
                searchCategory = SearchCategory.ALL_DOCUMENTS
            )
            megaApiGateway.searchWithFilter(
                filter,
                sortOrderIntMapper(order),
                megaCancelToken,
            ).map { megaNode ->
                convertToUnTypedNode(
                    node = megaNode,
                    offline = offlineItems?.get(megaNode.handle.toString())
                )
            }
        }

    private suspend fun getAllOfflineNodeHandle() =
        megaLocalRoomGateway.getAllOfflineInfo()?.associateBy { it.handle }

    private suspend fun convertToUnTypedNode(
        node: MegaNode,
        offline: Offline? = null,
    ): UnTypedNode {
        return nodeMapper(
            megaNode = node, offline = offline
        )
    }
}