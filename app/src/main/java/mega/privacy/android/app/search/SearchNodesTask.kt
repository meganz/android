package mega.privacy.android.app.search

import android.os.AsyncTask
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.managerFragments.LinksFragment
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.*
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.SortUtil
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC
import java.util.ArrayList
import javax.inject.Inject

/**
 * Class which manages search actions.
 *
 * @property megaApi             MegaApiAndroid object.
 * @property sortOrderManagement SortOrderManagement object.
 * @property query               Typed text for search.
 * @property parentHandleSearch  INVALID_HANDLE if has to get the parent handle to search yet,
 *                               a valid handle if not.
 * @property parentHandle        Parent handle where the search has to be made.
 * @property nodes               List of MegaNodes shown before make the search.
 * @property callback            Callback to perform final actions.
 * @property searchType          It can be TYPE_GENERAL, TYPE_CLOUD_EXPLORER or TYPE_INCOMING_EXPLORER.
 */
class SearchNodesTask @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val sortOrderManagement: SortOrderManagement,
    private val query: String?,
    private val parentHandleSearch: Long,
    private val parentHandle: Long,
    private val nodes: ArrayList<MegaNode>,
    private val callback: Callback,
    private val searchType: Int
) : AsyncTask<Void, Void, Void>() {

    private var drawerItem: DrawerItem? = null
    private var sharesTab = INVALID_VALUE
    private var isFirstNavigationLevel = true

    private var megaCancelToken: MegaCancelToken? = null
    private var searchNodes = nodes

    /**
     * Constructor which manages TYPE_GENERAL search.
     *
     * @property megaApi                MegaApiAndroid object.
     * @property sortOrderManagement    SortOrderManagement object.
     * @property query                  Typed text for search.
     * @property parentHandleSearch     INVALID_HANDLE if has to get the parent handle to search yet,
     *                                  a valid handle if not.
     * @property parentHandle           Parent handle where the search has to be made.
     * @property nodes               List of MegaNodes shown before make the search.
     * @property callback               Callback to perform final actions.
     * @property searchType             It can be TYPE_GENERAL, TYPE_CLOUD_EXPLORER or TYPE_INCOMING_EXPLORER.
     * @property drawerItem             DrawerItem in which the search has to be made.
     * @property sharedTab              Tab of the Shares section if needed, INVALID_VALUE otherwise.
     * @property isFirstNavigationLevel True if is first navigation level, false otherwise.
     */
    constructor(
        @MegaApi megaApi: MegaApiAndroid,
        sortOrderManagement: SortOrderManagement,
        query: String?,
        parentHandleSearch: Long,
        parentHandle: Long,
        nodes: ArrayList<MegaNode>,
        callback: Callback,
        searchType: Int,
        drawerItem: DrawerItem,
        sharedTab: Int,
        isFirstNavigationLevel: Boolean
    ) : this(
        megaApi,
        sortOrderManagement,
        query,
        parentHandleSearch,
        parentHandle,
        nodes,
        callback,
        searchType
    ) {
        this.drawerItem = drawerItem
        this.sharesTab = sharedTab
        this.isFirstNavigationLevel = isFirstNavigationLevel
    }

    companion object {
        const val TYPE_GENERAL = 1
        const val TYPE_CLOUD_EXPLORER = 2
        const val TYPE_INCOMING_EXPLORER = 3

        /**
         * Updates the progress search view.
         *
         * @param contentLayout     General content view.
         * @param searchProgressBar ProgressBar to show or hide.
         * @param recyclerView      Container of the ProgressBar.
         * @param inProgress        True if the search is in progress, false otherwise.
         */
        @JvmStatic
        fun setSearchProgressView(
            contentLayout: RelativeLayout?,
            searchProgressBar: ProgressBar?,
            recyclerView: RecyclerView?,
            inProgress: Boolean
        ) {
            if (contentLayout == null || searchProgressBar == null || recyclerView == null) {
                logWarning("Cannot set search progress view, one or more parameters are NULL.")
                logDebug("contentLayout: $contentLayout, searchProgressBar: $searchProgressBar, recyclerView: $recyclerView")
                return
            }

            contentLayout.isEnabled = !inProgress

            if (inProgress) {
                contentLayout.alpha = 0.4f
                searchProgressBar.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                contentLayout.alpha = 1f
                searchProgressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun doInBackground(vararg voids: Void?): Void? {
        getSearchNodes()
        return null
    }

    override fun onPostExecute(aVoid: Void?) {
        callback.finishSearchNodes(searchNodes)
    }

    /**
     * Cancels the current search.
     */
    fun cancelSearch() {
        megaCancelToken?.cancel()
    }

    /**
     * Gets the searched nodes depending on the query typed and current section.
     */
    private fun getSearchNodes() {
        if (query == null) {
            searchNodes.clear()
            return
        }

        var parent: MegaNode? = null

        if (parentHandleSearch == INVALID_HANDLE) {
            when (searchType) {
                TYPE_GENERAL -> {
                    when (drawerItem) {
                        DrawerItem.HOMEPAGE -> parent = megaApi.rootNode
                        DrawerItem.CLOUD_DRIVE -> parent = megaApi.getNodeByHandle(parentHandle)
                        DrawerItem.SHARED_ITEMS -> {
                            when (sharesTab) {
                                INCOMING_TAB -> {
                                    if (parentHandle == INVALID_HANDLE) {
                                        getInShares()
                                        return
                                    }

                                    parent = megaApi.getNodeByHandle(parentHandle)
                                }
                                OUTGOING_TAB -> {
                                    if (parentHandle == INVALID_HANDLE) {
                                        getOutShares()
                                        return
                                    }

                                    parent = megaApi.getNodeByHandle(parentHandle)
                                }
                                LINKS_TAB -> {
                                    if (parentHandle == INVALID_HANDLE) {
                                        getLinks()
                                        return
                                    }

                                    parent = megaApi.getNodeByHandle(parentHandle)
                                }
                            }
                        }
                        DrawerItem.RUBBISH_BIN -> {
                            parent = if (parentHandle == INVALID_HANDLE) {
                                megaApi.rubbishNode
                            } else {
                                megaApi.getNodeByHandle(parentHandle)
                            }
                        }
                        DrawerItem.INBOX -> {
                            parent = if (parentHandle == INVALID_HANDLE) {
                                megaApi.inboxNode
                            } else {
                                megaApi.getNodeByHandle(parentHandle)
                            }
                        }
                        else -> parent = megaApi.rootNode
                    }
                }
                TYPE_CLOUD_EXPLORER -> {
                    parent = megaApi.getNodeByHandle(parentHandle)
                }
                TYPE_INCOMING_EXPLORER -> {
                    if (parentHandle == INVALID_HANDLE) {
                        getInShares()
                        return
                    }

                    parent = megaApi.getNodeByHandle(parentHandle)
                }
            }
        } else {
            parent = megaApi.getNodeByHandle(parentHandleSearch)
        }

        if (parent != null) {
            if (TextUtil.isTextEmpty(query) || parentHandleSearch != INVALID_HANDLE) {
                searchNodes = megaApi.getChildren(parent)
            } else {
                megaCancelToken = MegaCancelToken.createInstance()
                searchNodes = megaApi.search(
                    parent,
                    query,
                    megaCancelToken,
                    true,
                    sortOrderManagement.getOrderCloud()
                )
            }
        }
    }

    /**
     * Gets search result nodes of Incoming section, root navigation level.
     */
    private fun getInShares() {
        if (TextUtil.isTextEmpty(query)) {
            searchNodes = megaApi.getInShares(sortOrderManagement.getOrderOthers())
        } else {
            megaCancelToken = MegaCancelToken.createInstance()
            searchNodes = megaApi.searchOnInShares(
                query,
                megaCancelToken,
                sortOrderManagement.getOrderCloud()
            )
        }
    }

    /**
     * Gets search result nodes of Outgoing section, root navigation level.
     */
    private fun getOutShares() {
        if (TextUtil.isTextEmpty(query)) {
            searchNodes.clear()
            val outShares = megaApi.outShares
            val addedHandles: MutableList<Long> = ArrayList()
            for (outShare in outShares) {
                val node = megaApi.getNodeByHandle(outShare.nodeHandle)
                if (node != null && !addedHandles.contains(node.handle)) {
                    addedHandles.add(node.handle)
                    searchNodes.add(node)
                }
            }
            if (sortOrderManagement.getOrderOthers() == ORDER_DEFAULT_DESC) {
                SortUtil.sortByNameDescending(searchNodes)
            } else {
                SortUtil.sortByNameAscending(searchNodes)
            }
        } else {
            megaCancelToken = MegaCancelToken.createInstance()
            searchNodes = megaApi.searchOnOutShares(
                query,
                megaCancelToken,
                sortOrderManagement.getOrderCloud()
            )
        }
    }

    /**
     * Gets search result nodes of Links section, root navigation level.
     */
    private fun getLinks() {
        if (TextUtil.isTextEmpty(query)) {
            searchNodes = megaApi.getPublicLinks(
                LinksFragment.getLinksOrderCloud(
                    sortOrderManagement.getOrderCloud(), isFirstNavigationLevel
                )
            )
        } else {
            megaCancelToken = MegaCancelToken.createInstance()
            searchNodes = megaApi.searchOnPublicLinks(
                query,
                megaCancelToken,
                sortOrderManagement.getOrderCloud()
            )
        }
    }

    interface Callback {
        /**
         * Finishes the search.
         *
         * @param nodes List of the searched nodes.
         */
        fun finishSearchNodes(nodes: ArrayList<MegaNode>)
    }
}