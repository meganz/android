package mega.privacy.android.app.getLink

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.getLink.data.LinkItem
import mega.privacy.android.app.getLink.useCase.ExportNodeUseCase
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder
import mega.privacy.android.app.utils.Util.getSizeString
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import java.io.File

class GetSeveralLinksViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val exportNodeUseCase: ExportNodeUseCase
) : BaseRxViewModel() {

    private val linkItemsList: MutableLiveData<List<LinkItem>> = MutableLiveData()
    fun getLinkItems(): LiveData<List<LinkItem>> = linkItemsList

    private val exportingNodes: MutableLiveData<Boolean> = MutableLiveData()
    fun getExportingNodes(): LiveData<Boolean> = exportingNodes
    fun isExportingNodes(): Boolean = exportingNodes.value ?: true

    private val thumbFolder by lazy { getThumbFolder(MegaApplication.getInstance()) }

    fun initNodes(handlesList: LongArray) {
        val links = ArrayList<LinkItem>()
        val pendingExports = ArrayList<MegaNode>()

        links.add(LinkItem.Header(getString(R.string.tab_links_shares)))

        for (handle in handlesList) {
            val node = megaApi.getNodeByHandle(handle)
            if (node != null) {
                val isExported = node.isExported

                if (!isExported) {
                    pendingExports.add(node)
                }

                val link = if (isExported) node.publicLink else null
                val thumbnail = File(thumbFolder, node.base64Handle + FileUtil.JPG_EXTENSION)

                links.add(
                    LinkItem.Data(
                        node,
                        if (thumbnail.exists()) thumbnail else null,
                        node.name,
                        link,
                        if (node.isFolder) getMegaNodeFolderInfo(node) else getSizeString(node.size)
                    )
                )
            }
        }

        exportNodes(pendingExports)
        linkItemsList.value = links
    }

    private fun exportNodes(pendingExports: List<MegaNode>) {
        if (pendingExports.isNotEmpty()) {
            exportingNodes.value = true
            exportNodeUseCase.export(pendingExports)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { exportedNodes -> notifyExportedNodes(exportedNodes) },
                    onError = { error ->
                        LogUtil.logWarning(error.message)
                    }
                )
                .addTo(composite)
        } else exportingNodes.value = false
    }

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