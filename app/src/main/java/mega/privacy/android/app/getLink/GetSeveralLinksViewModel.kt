package mega.privacy.android.app.getLink

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_NONE_SENSITIVE
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_WARNING_TYPE_FOLDER
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_WARNING_TYPE_LINKS
import mega.privacy.android.app.getLink.data.LinkItem
import mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo
import mega.privacy.android.app.utils.ThumbnailUtils.getThumbFolder
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasSensitiveDescendantUseCase
import mega.privacy.android.domain.usecase.HasSensitiveInheritedUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import mega.privacy.android.domain.usecase.node.ExportNodesUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model used for manage data related to [GetSeveralLinksFragment].
 * Its shared with its activity [GetLinkActivity].
 *
 * @property megaApi                    MegaApiAndroid instance to use.
 * @property exportNodesUseCase         Use case to export nodes.
 * @property getThumbnailUseCase        Use case to request thumbnails.
 */
@HiltViewModel
class GetSeveralLinksViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val exportNodesUseCase: ExportNodesUseCase,
    private val hasSensitiveDescendantUseCase: HasSensitiveDescendantUseCase,
    private val hasSensitiveInheritedUseCase: HasSensitiveInheritedUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    sendTextMessageUseCase: SendTextMessageUseCase,
) : BaseLinkViewModel(get1On1ChatIdUseCase, sendTextMessageUseCase) {

    private val linkItemsList: MutableLiveData<List<LinkItem>> = MutableLiveData()
    fun getLinkItems(): LiveData<List<LinkItem>> = linkItemsList

    private val exportingNodes: MutableLiveData<Boolean> = MutableLiveData()
    fun getExportingNodes(): LiveData<Boolean> = exportingNodes
    fun isExportingNodes(): Boolean = exportingNodes.value ?: true

    private var linksNumber = 0
    fun getLinksNumber(): Int = linksNumber

    private val thumbFolder by lazy { getThumbFolder(MegaApplication.getInstance()) }

    private val _hasSensitiveItems: MutableStateFlow<Int?> = MutableStateFlow(null)
    val hasSensitiveItemsFlow = _hasSensitiveItems.asStateFlow()

    private var isInitialized = false
    fun isInitialized(): Boolean = isInitialized

    /**
     * Initializes the nodes and all the available info.
     *
     * @param handlesList List of MegaNode identifiers.
     */
    fun initNodes(handlesList: LongArray, context: Context) {
        linksNumber = handlesList.size
        val links = ArrayList<LinkItem>()
        val pendingExports = ArrayList<MegaNode>()

        links.add(LinkItem.Header(context.getString(R.string.tab_links_shares)))

        for (handle in handlesList) {
            val node = megaApi.getNodeByHandle(handle)
            if (node != null) {
                val isExported = node.isExported

                if (!isExported) {
                    pendingExports.add(node)
                }

                val link = if (isExported) node.publicLink else null

                links.add(
                    LinkItem.Data(
                        node,
                        node.name,
                        link,
                        if (node.isFolder) getMegaNodeFolderInfo(node, context) else getSizeString(
                            node.size,
                            context
                        )
                    )
                )
            }
        }

        exportNodes(pendingExports)
        linkItemsList.value = links
        isInitialized = true
    }

    /**
     * Export a list of nodes.
     *
     * @param pendingExports List of nodes to export.
     */
    private fun exportNodes(pendingExports: List<MegaNode>) {
        if (pendingExports.isNotEmpty()) {
            exportingNodes.value = true
            val pendingExportNodeHandles = pendingExports.map { it.handle }
            viewModelScope.launch {
                runCatching {
                    exportNodesUseCase(pendingExportNodeHandles)
                }.onSuccess { exportedNodes ->
                    notifyExportedNodes(exportedNodes)
                }.onFailure { error ->
                    Timber.e(error)
                }
            }
        } else {
            exportingNodes.value = false
        }
    }

    /**
     * Updates the list of [LinkItem]s when the export actions have been finished.
     *
     * @param exportedNodes Map<Long, String> Key is the node handle, value the node link.
     */
    private fun notifyExportedNodes(exportedNodes: Map<Long, String>) {
        val links = (linkItemsList.value ?: return).toMutableList()

        for (item in links.indices) {
            val linkItem = links[item]

            if (linkItem is LinkItem.Data && linkItem.link == null) {
                val link = exportedNodes[linkItem.node.handle]

                if (link != null) {
                    links[item] = LinkItem.Data(
                        linkItem.node,
                        linkItem.name,
                        link,
                        linkItem.info
                    )
                }
            }
        }

        exportingNodes.value = false
        linkItemsList.value = links
    }

    /**
     * Gets a list of Strings containing all the links.
     *
     * @return The list of links.
     */
    fun getLinksList(): ArrayList<String> {
        val links = ArrayList<String>()

        linkItemsList.value.let { list ->
            val linksList = list ?: return@let

            for (linkItem in linksList) {
                if (linkItem is LinkItem.Data)
                    linkItem.link?.let { link -> links.add(link) }
            }
        }

        return links
    }

    /**
     * Gets a unique String containing all the links.
     *
     * @return The links.
     */
    fun getLinksString() = linkItemsList.value?.filterIsInstance<LinkItem.Data>()
        ?.mapNotNull { it.link }
        ?.joinToString(separator = "\n", postfix = "\n") ?: ""

    fun checkSensitiveItems(handles: List<Long>) = viewModelScope.launch {
        var sensitiveType = HIDDEN_NODE_NONE_SENSITIVE

        for (handle in handles) {
            val nodeId = NodeId(handle)
            val typedNode = getNodeByIdUseCase(nodeId) ?: continue
            if (typedNode.exportedData != null) continue

            when {
                typedNode.isMarkedSensitive || hasSensitiveInheritedUseCase(typedNode.id) -> {
                    sensitiveType = HIDDEN_NODE_WARNING_TYPE_LINKS
                    break
                }

                (typedNode is FolderNode) && hasSensitiveDescendantUseCase(typedNode.id) -> {
                    sensitiveType = HIDDEN_NODE_WARNING_TYPE_FOLDER
                }
            }
        }

        _hasSensitiveItems.value = sensitiveType
    }

    fun clearSensitiveItemCheck() {
        _hasSensitiveItems.value = null
    }
}