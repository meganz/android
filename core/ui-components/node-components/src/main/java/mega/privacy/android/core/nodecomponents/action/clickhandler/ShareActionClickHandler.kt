package mega.privacy.android.core.nodecomponents.action.clickhandler

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.mapper.NodeShareContentUrisIntentMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.domain.entity.node.NodeShareContentUri
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByNodeIdUseCase
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.shared.resources.R as sharedResR
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

class ShareActionClickHandler @Inject constructor(
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase,
    private val exportNodesUseCase: ExportNodeUseCase,
    private val getOfflineNodeInformationByNodeIdUseCase: GetOfflineNodeInformationByNodeIdUseCase,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val nodeShareContentUrisIntentMapper: NodeShareContentUrisIntentMapper,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is ShareMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        val nodeSourceType = provider.viewModel.getNodeSourceType()
        provider.coroutineScope.launch {
            val isConnected = if (nodeSourceType == NodeSourceType.OFFLINE) {
                monitorConnectivityUseCase().first()
            } else false

            withContext(NonCancellable) {
                try {
                    if (nodeSourceType == NodeSourceType.OFFLINE) {
                        handleOfflineShare(node, provider, isConnected)
                    } else {
                        handleCloudShare(node, provider)
                    }
                } finally {
                    provider.viewModel.dismiss()
                }
            }
        }
    }

    private suspend fun handleOfflineShare(
        node: TypedNode,
        provider: SingleNodeActionProvider,
        isConnected: Boolean,
    ) {
        // For offline files, share the local file directly using NodeShareContentUrisIntentMapper
        if (node is TypedFileNode) {
            val offlineFile = getOfflineFileForNode(node)
            if (offlineFile != null) {
                val shareIntent = nodeShareContentUrisIntentMapper(
                    title = node.name,
                    content = NodeShareContentUri.LocalContentUris(listOf(offlineFile)),
                    mimeType = node.type.mimeType
                )
                provider.coroutineScope.ensureActive()
                provider.context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        provider.context.getString(sharedResR.string.general_share)
                    )
                )
            } else {
                Timber.e("Offline file not found for node: ${node.id}")
            }
        } else if (node is TypedFolderNode) {
            // For offline folders, share the link if online
            if (isConnected) {
                val publicLink = node.exportedData?.publicLink
                if (publicLink != null) {
                    provider.coroutineScope.ensureActive()
                    startShareIntent(
                        context = provider.context,
                        path = publicLink,
                        name = node.name
                    )
                } else {
                    runCatching {
                        exportNodesUseCase(
                            nodeToExport = node.id,
                            callerName = "ShareActionClickHandler:single"
                        )
                    }
                        .onSuccess { exportPath ->
                            provider.coroutineScope.ensureActive()
                            startShareIntent(
                                context = provider.context,
                                path = exportPath,
                                name = node.name
                            )
                        }
                        .onFailure { Timber.e(it) }
                }
            } else {
                provider.postMessage(
                    provider.context.getString(NodesR.string.error_server_connection_problem)
                )
            }
        }
    }

    private suspend fun handleCloudShare(node: TypedNode, provider: SingleNodeActionProvider) {
        val path = runCatching {
            getLocalFilePathUseCase(node)
        }.getOrElse {
            Timber.e(it)
            null
        }
        if (node is TypedFileNode && path != null && File(path).exists()) {
            val file = File(path)
            val shareIntent = nodeShareContentUrisIntentMapper(
                title = node.name,
                content = NodeShareContentUri.LocalContentUris(listOf(file)),
                mimeType = node.type.mimeType
            )
            provider.coroutineScope.ensureActive()
            provider.context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    provider.context.getString(sharedResR.string.general_share)
                )
            )
        } else {
            val publicLink = node.exportedData?.publicLink
            if (publicLink != null) {
                provider.coroutineScope.ensureActive()
                startShareIntent(
                    context = provider.context,
                    path = publicLink,
                    name = node.name
                )
            } else {
                runCatching {
                    exportNodesUseCase(
                        nodeToExport = node.id,
                        callerName = "ShareActionClickHandler:single"
                    )
                }
                    .onSuccess { exportPath ->
                        provider.coroutineScope.ensureActive()
                        startShareIntent(
                            context = provider.context,
                            path = exportPath,
                            name = node.name
                        )
                    }.onFailure {
                        Timber.e(it)
                    }
            }
        }
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val context = provider.context
        val nodeSourceType = provider.viewModel.getNodeSourceType()
        provider.coroutineScope.launch {
            val isConnected = if (nodeSourceType == NodeSourceType.OFFLINE) {
                monitorConnectivityUseCase().first()
            } else false

            withContext(NonCancellable) {
                try {
                    if (nodeSourceType == NodeSourceType.OFFLINE) {
                        handleOfflineMultiNodeShare(nodes, provider, isConnected)
                    } else {
                        val fileNodeList = nodes.filterIsInstance<TypedFileNode>()
                        val localFiles = fileNodeList.mapNotNull { node ->
                            runCatching {
                                getLocalFilePathUseCase(node)?.let { File(it) }
                            }.getOrElse {
                                Timber.e(it)
                                null
                            }
                        }.filter { it.exists() }
                        if (localFiles.size == nodes.size) {
                            val similarTypes = fileNodeList.groupBy {
                                it.type.mimeType
                            }
                            val intentType = if (similarTypes.size == 1) {
                                similarTypes.keys.first()
                            } else {
                                "*/*"
                            }
                            val shareIntent = nodeShareContentUrisIntentMapper(
                                title = getTitle(nodes),
                                content = NodeShareContentUri.LocalContentUris(localFiles),
                                mimeType = intentType
                            )
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    context.getString(sharedResR.string.general_share)
                                )
                            )
                        } else {
                            val allExportedNodes = nodes.all { it.exportedData != null }
                            if (allExportedNodes) {
                                val uris = nodes.mapNotNull {
                                    it.exportedData?.publicLink
                                }
                                shareLinks(
                                    context = context,
                                    links = uris,
                                    selectedNodes = nodes
                                )
                            } else {
                                val uris = nodes.mapNotNull { node ->
                                    runCatching {
                                        exportNodesUseCase(
                                            nodeToExport = node.id,
                                            callerName = "ShareActionClickHandler:multiple"
                                        )
                                    }.getOrElse {
                                        Timber.e(it)
                                        null
                                    }
                                }
                                shareLinks(
                                    context = context,
                                    links = uris,
                                    selectedNodes = nodes
                                )
                            }
                        }
                    }
                } finally {
                    provider.viewModel.dismiss()
                }
            }
        }
    }

    private suspend fun handleOfflineMultiNodeShare(
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
        isConnected: Boolean,
    ) {
        val context = provider.context
        val fileNodeList = nodes.filterIsInstance<TypedFileNode>()
        val isAllFiles = fileNodeList.size == nodes.size
        if (isAllFiles) {
            // Share offline files locally
            val offlineFiles = fileNodeList.mapNotNull { getOfflineFileForNode(it) }
            if (offlineFiles.isNotEmpty()) {
                val similarTypes = fileNodeList.groupBy { it.type.mimeType }
                val intentType = if (similarTypes.size == 1) {
                    similarTypes.keys.first()
                } else {
                    "*/*"
                }
                val shareIntent = nodeShareContentUrisIntentMapper(
                    title = getTitle(nodes),
                    content = NodeShareContentUri.LocalContentUris(offlineFiles),
                    mimeType = intentType
                )
                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        context.getString(sharedResR.string.general_share)
                    )
                )
            }
        } else {
            // Has folders - need to export links
            if (isConnected) {
                val uris = nodes.mapNotNull { node ->
                    runCatching {
                        exportNodesUseCase(
                            nodeToExport = node.id,
                            callerName = "ShareActionClickHandler:multiple:offline"
                        )
                    }.getOrElse { exception ->
                        Timber.e(exception)
                        null
                    }
                }
                shareLinks(context = context, links = uris, selectedNodes = nodes)
            } else {
                provider.postMessage(
                    context.getString(NodesR.string.error_server_connection_problem)
                )
            }
        }
    }

    private fun shareLinks(context: Context, links: List<String>, selectedNodes: List<TypedNode>) {
        if (links.isNotEmpty()) {
            val title = getTitle(selectedNodes)
            val shareIntent = nodeShareContentUrisIntentMapper(
                title = title,
                content = NodeShareContentUri.RemoteContentUris(links)
            )
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    context.getString(sharedResR.string.general_share)
                )
            )
        }
    }

    private fun startShareIntent(context: Context, path: String, name: String) {
        val shareIntent = nodeShareContentUrisIntentMapper(
            title = name,
            content = NodeShareContentUri.RemoteContentUris(listOf(path))
        )
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(sharedResR.string.general_share)
            )
        )
    }

    private fun getTitle(selectedNodes: List<TypedNode>): String {
        return if (selectedNodes.size == 1) {
            selectedNodes.first().name
        } else {
            "${UUID.randomUUID()}"
        }
    }

    /**
     * Get the offline file for a given node
     *
     * @param node The typed file node
     * @return The offline file if it exists, null otherwise
     */
    private suspend fun getOfflineFileForNode(node: TypedFileNode): File? {
        return runCatching {
            getOfflineNodeInformationByNodeIdUseCase(node.id)?.let {
                getOfflineFileUseCase(it)
            }
        }.getOrElse {
            Timber.e(it)
            null
        }?.takeIf { it.exists() }
    }
}
