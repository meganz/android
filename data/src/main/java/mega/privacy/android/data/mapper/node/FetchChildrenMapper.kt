package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.domain.entity.SortOrder
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
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val nodeMapperProvider: Provider<NodeMapper>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Invoke
     *
     * @param megaNode
     * @return
     */
    operator fun invoke(megaNode: MegaNode): suspend (SortOrder) -> List<UnTypedNode> {
        return { order ->
            withContext(ioDispatcher) {
                megaApiGateway.getChildren(megaNode, sortOrderIntMapper(order))
                    .map { nodeMapperProvider.get().invoke(it) }
            }
        }
    }
}