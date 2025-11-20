package mega.privacy.android.feature.clouddrive.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.mapper.FileNodeContentToNavKeyMapper
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetAncestorsIdsUseCase
import mega.privacy.android.domain.usecase.node.GetFileNodeContentForFileNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeIdFromBase64UseCase
import mega.privacy.android.feature.clouddrive.presentation.shares.links.OpenPasswordLinkDialogNavKey
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.shared.resources.R as sharedR
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
    private val snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? = when (regexPatternType) {
        RegexPatternType.PASSWORD_LINK -> {
            listOf(OpenPasswordLinkDialogNavKey(uri.toString()))
        }

        RegexPatternType.HANDLE_LINK -> if (isLoggedIn) {
            val node = uri.extractNodeHandleBase64FromUri()
                ?.let { getNodeIdFromBase64UseCase(it) }?.longValue?.let { handle ->
                    getNodeByIdUseCase(NodeId(handle))
                }
            buildList {
                if (node != null) {
                    var highlightedNodeHandle: Long? = null
                    if (node is TypedFileNode) {
                        val previewNavKey: NavKey? = fileNodeContentToNavKeyMapper(
                            getFileNodeContentForFileNodeUseCase(node), node
                        )
                        if (previewNavKey != null) {
                            // Add preview if it's possible to open this file
                            add(previewNavKey)
                        } else {
                            // Otherwise the node should be highlighted in its parent folder
                            highlightedNodeHandle = node.id.longValue
                        }
                    } else {
                        // It's a folder, add the node itself as destination
                        add(CloudDriveNavKey(nodeHandle = node.id.longValue))
                    }
                    // Add the ancestors of the node as CloudDriveNavKey
                    getAncestorsIdsUseCase(node).forEach { parentId ->
                        add(
                            CloudDriveNavKey(
                                nodeHandle = parentId.longValue,
                                highlightedNodeHandle = highlightedNodeHandle
                            )
                        )
                        highlightedNodeHandle = null
                    }
                } else {
                    add(CloudDriveNavKey())
                }
            }.reversed()
        } else {
            snackbarEventQueue.queueMessage(sharedR.string.general_alert_not_logged_in)
            emptyList()
        }

        else -> null
    }

    private fun Uri.extractNodeHandleBase64FromUri(): String? =
        this.fragment?.substringBefore('/')
}
