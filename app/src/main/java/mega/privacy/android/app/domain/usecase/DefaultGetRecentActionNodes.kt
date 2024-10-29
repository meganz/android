package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import nz.mega.sdk.MegaNodeList
import timber.log.Timber
import javax.inject.Inject

/**
 * Transform a [MegaNodeList] into a list of [NodeItem]
 */
class DefaultGetRecentActionNodes @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : GetRecentActionNodes {

    /**
     * Transform a List of [TypedFileNode] into a list of [NodeItem]
     *
     * @param nodes the nodes to convert
     * @return a list of node item resulting from the conversion
     */
    override suspend fun invoke(nodes: List<TypedFileNode>): List<NodeItem> =
        withContext(ioDispatcher) {
            List(nodes.size) { nodes[it] }
                .map { node ->
                    async {
                        ensureActive()
                        createNodeItem(node)
                    }
                }.awaitAll()
                .filterNotNull()
        }

    /**
     * Create a single [NodeItem] from [TypedFileNode]
     *
     * @param node
     * @return the corresponding [NodeItem], null if an error occurred
     */
    private suspend fun createNodeItem(node: TypedFileNode): NodeItem? =
        runCatching {
            val megaNode = getNodeByHandle.invoke(node.id.longValue)
            val hiddenNodeEnabled = isHiddenNodesActive()
            val shouldApplySensitiveMode = hiddenNodeEnabled && run {
                val accountType =
                    monitorAccountDetailUseCase().firstOrNull()?.levelDetail?.accountType
                val isBusinessAccountExpired =
                    getBusinessStatusUseCase() == BusinessAccountStatus.Expired
                accountType?.isPaid == true && !isBusinessAccountExpired
            }
            NodeItem(
                node = megaNode,
                index = -1,
                isVideo = node.type is VideoFileTypeInfo,
                modifiedDate = node.modificationTime.toString(),
                isSensitive = shouldApplySensitiveMode && (node.isMarkedSensitive || node.isSensitiveInherited),
                isSensitiveInherited = node.isSensitiveInherited,
                isMarkedSensitive = node.isMarkedSensitive,
            )
        }.onFailure {
            Timber.e(it)
        }.getOrNull()

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }
}
