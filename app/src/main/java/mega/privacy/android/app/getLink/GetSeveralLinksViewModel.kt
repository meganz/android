package mega.privacy.android.app.getLink

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.getLink.data.LinkItem
import mega.privacy.android.app.getLink.useCase.ExportNodeUseCase
import mega.privacy.android.app.usecase.GetThumbnailUseCase
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo
import mega.privacy.android.app.utils.ThumbnailUtils.getThumbFolder
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View Model used for manage data related to [GetSeveralLinksFragment].
 * Its shared with its activity [GetLinkActivity].
 *
 * @property megaApi             MegaApiAndroid instance to use.
 * @property exportNodeUseCase   Use case to export nodes.
 * @property getThumbnailUseCase Use case to request thumbnails.
 */
@HiltViewModel
class GetSeveralLinksViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val exportNodeUseCase: ExportNodeUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase,
) : BaseRxViewModel() {

    private val linkItemsList: MutableLiveData<List<LinkItem>> = MutableLiveData()
    fun getLinkItems(): LiveData<List<LinkItem>> = linkItemsList

    private val exportingNodes: MutableLiveData<Boolean> = MutableLiveData()
    fun getExportingNodes(): LiveData<Boolean> = exportingNodes
    fun isExportingNodes(): Boolean = exportingNodes.value ?: true

    private var linksNumber = 0
    fun getLinksNumber(): Int = linksNumber

    private val thumbFolder by lazy { getThumbFolder(MegaApplication.getInstance()) }

    /**
     * Initializes the nodes and all the available info.
     *
     * @param handlesList List of MegaNode identifiers.
     */
    fun initNodes(handlesList: LongArray, context: Context) {
        linksNumber = handlesList.size
        val links = ArrayList<LinkItem>()
        val pendingExports = ArrayList<MegaNode>()
        val pendingThumbnails = ArrayList<MegaNode>()

        links.add(LinkItem.Header(context.getString(R.string.tab_links_shares)))

        for (handle in handlesList) {
            val node = megaApi.getNodeByHandle(handle)
            if (node != null) {
                val isExported = node.isExported

                if (!isExported) {
                    pendingExports.add(node)
                }

                val link = if (isExported) node.publicLink else null
                val thumbnail = if (node.isFile && node.hasThumbnail()) {
                    File(thumbFolder, node.base64Handle + FileUtil.JPG_EXTENSION)
                } else null

                if (thumbnail != null && !thumbnail.exists()) {
                    pendingThumbnails.add(node)
                }

                links.add(
                    LinkItem.Data(
                        node,
                        if (thumbnail?.exists() == true) thumbnail else null,
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
        requestThumbnails(pendingThumbnails)
        linkItemsList.value = links
    }

    /**
     * Export a list of nodes.
     *
     * @param pendingExports List of nodes to export.
     */
    private fun exportNodes(pendingExports: List<MegaNode>) {
        if (pendingExports.isNotEmpty()) {
            exportingNodes.value = true
            exportNodeUseCase.export(pendingExports)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { exportedNodes -> notifyExportedNodes(exportedNodes) },
                    onError = { error -> Timber.e(error) }
                )
        } else {
            exportingNodes.value = false
        }
    }

    /**
     * Updates the list of [LinkItem]s when the export actions have been finished.
     *
     * @param exportedNodes HashMap<Long, String> Key is the node handle, value the node link.
     */
    private fun notifyExportedNodes(exportedNodes: HashMap<Long, String>) {
        val links = (linkItemsList.value ?: return).toMutableList()

        for (item in links.indices) {
            val linkItem = links[item]

            if (linkItem is LinkItem.Data && linkItem.link == null) {
                val link = exportedNodes[linkItem.node.handle]

                if (link != null) {
                    links[item] = LinkItem.Data(
                        linkItem.node,
                        linkItem.thumbnail,
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
     * Request a list of thumbnails which have not been get yet.
     *
     * @param pendingThumbnails List of nodes to get their thumbnails.
     */
    private fun requestThumbnails(pendingThumbnails: List<MegaNode>) {
        if (pendingThumbnails.isNotEmpty()) {
            getThumbnailUseCase.get(pendingThumbnails)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = { handle -> notifyThumbnailUpdate(handle) },
                    onError = Timber::e
                )
                .addTo(composite)
        }
    }

    /**
     * Updates the list of [LinkItem]s with the thumbnail already get.
     *
     * @param handle Node identifier from which the thumbnail has been get.
     */
    private fun notifyThumbnailUpdate(handle: Long) {
        val links = (linkItemsList.value ?: return).toMutableList()

        for (item in links.indices) {
            val linkItem = links[item]

            if (linkItem is LinkItem.Data && linkItem.id == handle) {
                links[item] = LinkItem.Data(
                    linkItem.node,
                    File(thumbFolder, linkItem.node.base64Handle + FileUtil.JPG_EXTENSION),
                    linkItem.name,
                    linkItem.link,
                    linkItem.info
                )

                break
            }
        }

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
    fun getLinksString(): String {
        var links = ""

        linkItemsList.value.let { list ->
            val linksList = list ?: return@let

            for (linkItem in linksList) {
                if (linkItem is LinkItem.Data)
                    links += linkItem.link + "\n"
            }
        }

        return links
    }
}