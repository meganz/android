package mega.privacy.android.feature.clouddrive.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.mapper.FileNodeContentToNavKeyMapper
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetAncestorsIdsUseCase
import mega.privacy.android.domain.usecase.node.GetFileNodeContentForFileNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeIdFromBase64UseCase
import mega.privacy.android.domain.usecase.node.IsNodeInCloudDriveUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.feature.clouddrive.presentation.shares.links.OpenPasswordLinkDialogNavKey
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.DriveSyncNavKey
import mega.privacy.android.navigation.destination.HomeScreensNavKey
import mega.privacy.android.navigation.destination.RubbishBinNavKey
import mega.privacy.android.navigation.destination.SharesNavKey
import javax.inject.Inject

/**
 * Cloud drive deep link handler
 */
class CloudDriveDeepLinkHandler @Inject constructor(
    private val getNodeIdFromBase64UseCase: GetNodeIdFromBase64UseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getFileNodeContentForFileNodeUseCase: GetFileNodeContentForFileNodeUseCase,
    private val fileNodeContentToNavKeyMapper: FileNodeContentToNavKeyMapper,
    private val getAncestorsIdsUseCase: GetAncestorsIdsUseCase,
    snackbarEventQueue: SnackbarEventQueue,
    private val isNodeInCloudDriveUseCase: IsNodeInCloudDriveUseCase,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
) : DeepLinkHandler(snackbarEventQueue) {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? = when (regexPatternType) {
        RegexPatternType.PASSWORD_LINK -> {
            listOf(OpenPasswordLinkDialogNavKey(uri.toString()))
        }

        RegexPatternType.HANDLE_LINK -> catchWithEmptyListAndLog {
            val node = uri.extractNodeHandleBase64FromUri()
                ?.let { getNodeIdFromBase64UseCase(it) }?.longValue?.let { handle ->
                    getNodeByIdUseCase(NodeId(handle))
                }
            val nodeSourceType: NodeSourceType
            val rootDestination: NavKey
            when {
                node == null || isNodeInCloudDriveUseCase(node.id.longValue) -> {
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE
                    rootDestination = DriveSyncNavKey()
                }

                isNodeInRubbishBinUseCase(node.id) -> {
                    nodeSourceType = NodeSourceType.RUBBISH_BIN
                    rootDestination = RubbishBinNavKey()
                }

                else -> {
                    nodeSourceType = NodeSourceType.INCOMING_SHARES
                    rootDestination = SharesNavKey
                }
            }
            val previewDestination = (node as? TypedFileNode)?.let { fileNode ->
                runCatching {
                    fileNodeContentToNavKeyMapper(
                        content = getFileNodeContentForFileNodeUseCase(fileNode),
                        fileNode = fileNode,
                        nodeSourceType = nodeSourceType,
                    )
                }.getOrNull()
            }
            val highlightedNode = node?.id.takeIf { node is FileNode && previewDestination == null }
            val childDestinations = runCatching {
                buildList {
                    if (node != null) {
                        // Add the node itself as destination if it's a folder
                        if (node is FolderNode) add(CloudDriveNavKey(nodeHandle = node.id.longValue))
                        addAll(
                            getAncestorsIdsUseCase(node)
                                .dropLast(
                                    if (nodeSourceType == NodeSourceType.INCOMING_SHARES) 0 else 1
                                )
                                .mapIndexed { index, parentId ->
                                    CloudDriveNavKey(
                                        nodeHandle = parentId.longValue,
                                        highlightedNodeHandle = if (index == 0) highlightedNode?.longValue else null,
                                        nodeSourceType = nodeSourceType,
                                    )
                                }
                        )
                    }
                }
            }
                .getOrElse { emptyList() }
                .reversed() //reversed as we want the deepest destinations in the back stack last in the list

            return@catchWithEmptyListAndLog buildList {
                if (nodeSourceType == NodeSourceType.CLOUD_DRIVE) {
                    //cloud drive under HomeScreensNavKey to show bottom navigation
                    add(
                        HomeScreensNavKey(
                            rootDestination,
                            childDestinations.takeIf { it.isNotEmpty() }
                        )
                    )
                } else {
                    add(rootDestination)
                    addAll(childDestinations)
                }
                add(previewDestination)
            }.filterNotNull()
        }

        else -> null
    }

    private fun Uri.extractNodeHandleBase64FromUri(): String? =
        this.fragment?.substringBefore('/')
}
