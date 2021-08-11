package mega.privacy.android.app.getLink

import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.getLink.data.LinkItem
import mega.privacy.android.app.utils.Constants.EXTRA_SEVERAL_LINKS
import mega.privacy.android.app.utils.Constants.PLAIN_TEXT_SHARE_TYPE
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid

class GetSeveralLinksViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) : BaseRxViewModel() {

    private val linkItemsList: MutableLiveData<List<LinkItem>> = MutableLiveData()
    fun getLinkItems(): LiveData<List<LinkItem>> = linkItemsList

    private val linksList = ArrayList<String>()

    fun initNodes(handlesList: LongArray) {
        val links = ArrayList<LinkItem>()
        for (handle in handlesList) {
            val node = megaApi.getNodeByHandle(handle)
            if (node != null) {
                links.add(LinkItem(node, node.name, node.publicLink, Util.getSizeString(node.size)))
                linksList.add(node.publicLink)
            }
        }

        linkItemsList.value = links
    }

    private fun getLinksString(): String {
        var links = ""

        for (link in linksList) {
            links += link + "\n"
        }

        return links
    }

    fun copyAll(action: (String) -> Unit) {
        action.invoke(getLinksString())
    }

    /**
     * Launches an intent to share the links outside the app.
     *
     * @param action Action to perform after manage data.
     */
    fun shareLinks(action: (Intent?) -> Unit) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType(PLAIN_TEXT_SHARE_TYPE)
            .putExtra(Intent.EXTRA_TEXT, getLinksString())

        action.invoke(Intent.createChooser(intent,
            StringResourcesUtils.getString(R.string.context_get_link)
        ))
    }

    /**
     * Shares the links to chat.
     *
     * @param data Intent containing the info to share the content to chats.
     */
    fun sendToChat(
        data: Intent?,
        action: (Intent?) -> Unit
    ) {
        action.invoke(data?.putExtra(EXTRA_SEVERAL_LINKS, linksList))
    }
}