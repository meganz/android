package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.repository.CancelTokenProvider
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaNode
import javax.inject.Inject
import javax.inject.Provider

/**
 * Fetch children mapper
 *
 * @property megaApiGateway
 * @property sortOrderIntMapper
 * @property ioDispatcher
 * @property nodeMapperProvider
 * @constructor Create empty Fetch children mapper
 */
internal class FetchChildrenMapper @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val nodeMapperProvider: Provider<NodeMapper>,
    private val cancelTokenProvider: CancelTokenProvider,
    private val megaSearchFilterMapper: MegaSearchFilterMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Invoke
     *
     * @param megaNode
     * @return
     */
    operator fun invoke(
        megaNode: MegaNode,
        fromFolderLink: Boolean = false
    ): suspend (SortOrder) -> List<UnTypedNode> = { order ->
        val token = cancelTokenProvider.getOrCreateCancelToken()
        val filter = megaSearchFilterMapper(
            parentHandle = NodeId(megaNode.handle),
        )
        withContext(ioDispatcher) {
            if (fromFolderLink) {
                megaApiFolderGateway.getChildren(filter, sortOrderIntMapper(order), token)
                    .map { nodeMapperProvider.get().invoke(it) }
            } else {
                megaApiGateway.getChildren(filter, sortOrderIntMapper(order), token)
                    .map { nodeMapperProvider.get().invoke(it) }
            }
        }
    }
}