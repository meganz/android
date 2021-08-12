package mega.privacy.android.app.getLink

import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.getLink.data.LinkItem
import mega.privacy.android.app.utils.Constants.EXTRA_SEVERAL_LINKS
import mega.privacy.android.app.utils.Constants.PLAIN_TEXT_SHARE_TYPE
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder
import mega.privacy.android.app.utils.Util.getSizeString
import nz.mega.sdk.MegaApiAndroid
import java.io.File

class GetSeveralLinksViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) : BaseRxViewModel() {

    private val linkItemsList: MutableLiveData<List<LinkItem>> = MutableLiveData()
    fun getLinkItems(): LiveData<List<LinkItem>> = linkItemsList

    private val thumbFolder by lazy { getThumbFolder(MegaApplication.getInstance()) }

    fun initNodes(handlesList: LongArray) {
        val links = ArrayList<LinkItem>()
        for (handle in handlesList) {
            val node = megaApi.getNodeByHandle(handle)
            if (node != null) {
                val link = node.publicLink
                val thumbnail = File(thumbFolder, node.base64Handle + FileUtil.JPG_EXTENSION)

                links.add(
                    LinkItem(
                        node,
                        if (thumbnail.exists()) thumbnail else null,
                        node.name,
                        link,
                        if (node.isFolder) getMegaNodeFolderInfo(node) else getSizeString(node.size)
                    )
                )
            }
        }

        linkItemsList.value = links.toList()
    }

    fun getLinksString(): String {
        var links = ""

        linkItemsList.value.let { list ->
            val linksList = list ?: return@let

            for (linkItem in linksList) {
                links += linkItem.link + "\n"
            }
        }

        return links
    }
}