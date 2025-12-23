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
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.node.GetFileNodeContentForFileNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeIdFromBase64UseCase
import mega.privacy.android.domain.usecase.node.GetNodeLocationUseCase
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.DeepLinksAfterFetchNodesDialogNavKey
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
    snackbarEventQueue: SnackbarEventQueue,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val getNodeLocationUseCase: GetNodeLocationUseCase,
) : DeepLinkHandler(snackbarEventQueue) {

    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? = when (regexPatternType) {
        RegexPatternType.HANDLE_LINK -> catchWithEmptyListAndLog {
            when {
                !isLoggedIn -> listOf(HomeScreensNavKey(DriveSyncNavKey()))
                !rootNodeExistsUseCase() -> listOf(
                    DeepLinksAfterFetchNodesDialogNavKey(
                        deepLink = uri.toString(),
                        regexPatternType = RegexPatternType.HANDLE_LINK,
                    )
                )

                else -> {
                    val node = uri.extractNodeHandleBase64FromUri()
                        ?.let { getNodeIdFromBase64UseCase(it) }?.longValue?.let { handle ->
                            getNodeByIdUseCase(NodeId(handle))
                        }
                    val nodeLocation = node?.let { getNodeLocationUseCase(it) }
                    val nodeSourceType = nodeLocation?.nodeSourceType
                        ?: NodeSourceType.CLOUD_DRIVE
                    val ancestorIds = nodeLocation?.ancestorIds ?: emptyList()
                    val previewDestination = (node as? TypedFileNode)?.let { fileNode ->
                        runCatching {
                            fileNodeContentToNavKeyMapper(
                                content = getFileNodeContentForFileNodeUseCase(fileNode),
                                fileNode = fileNode,
                                nodeSourceType = nodeSourceType,
                            )
                        }.getOrNull()
                    }
                    val highlightedNode =
                        node?.id.takeIf { node is FileNode && previewDestination == null }
                    val highlightedInRoot = if (ancestorIds.isEmpty()) highlightedNode else null

                    val childDestinations = runCatching {
                        buildList {
                            if (node != null) {
                                // Add the node itself as destination if it's a folder
                                if (node is FolderNode) add(CloudDriveNavKey(nodeHandle = node.id.longValue))
                                addAll(
                                    ancestorIds
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

                    buildList {
                        if (nodeSourceType == NodeSourceType.CLOUD_DRIVE) {
                            //cloud drive under HomeScreensNavKey to show bottom navigation
                            add(
                                HomeScreensNavKey(
                                    DriveSyncNavKey(highlightedNodeHandle = highlightedInRoot?.longValue),
                                    childDestinations.takeIf { it.isNotEmpty() }
                                )
                            )
                        } else {
                            val rootDestination = when (nodeSourceType) {
                                NodeSourceType.RUBBISH_BIN -> RubbishBinNavKey(highlightedNodeHandle = highlightedInRoot?.longValue)
                                else -> SharesNavKey
                            }
                            add(rootDestination)
                            addAll(childDestinations)
                        }
                        add(previewDestination)
                    }.filterNotNull()
                }
            }
        }

        else -> null
    }

    private fun Uri.extractNodeHandleBase64FromUri(): String? =
        this.fragment?.substringBefore('/')
}
