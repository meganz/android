package mega.privacy.android.app.search.usecase

import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.SortUtil.sortByNameAscending
import mega.privacy.android.app.utils.SortUtil.sortByNameDescending
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case which search nodes.
 *
 * @property megaApi                MegaApiAndroid object.
 * @property sortOrderManagement    SortOrderManagement object to check order.
 * @property getLinksSortOrder
 * @property sortOrderIntMapper
 * @property ioDispatcher
 */
class SearchNodesUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val sortOrderManagement: SortOrderManagement,
    private val getLinksSortOrder: GetLinksSortOrder,
    private val sortOrderIntMapper: SortOrderIntMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    companion object {
        //Available types of search.
        const val TYPE_GENERAL = 1
        const val TYPE_CLOUD_EXPLORER = 2
        const val TYPE_INCOMING_EXPLORER = 3
    }

    /**
     * Gets searched nodes.
     *
     * @param query                 Typed text for search.
     * @param parentHandleSearch    INVALID_HANDLE if has to get the parent handle to search yet,
     *                              a valid handle if not.
     * @param parentHandle          Parent handle where the search has to be made.
     * @param searchType            It can be TYPE_GENERAL, TYPE_CLOUD_EXPLORER or TYPE_INCOMING_EXPLORER.
     * @param megaCancelToken       Cancel token to identify and cancel the search in question.
     * @return Single<ArrayList<MegaNode>> The list of nodes if success.
     */
    fun get(
        query: String?,
        parentHandleSearch: Long,
        parentHandle: Long,
        searchType: Int,
        megaCancelToken: MegaCancelToken,
    ): Single<ArrayList<MegaNode>> =
        get(
            query,
            parentHandleSearch,
            parentHandle,
            searchType,
            megaCancelToken,
            null,
            INVALID_VALUE,
            true
        )

    /**
     * Gets searched nodes.
     *
     * @param query                     Typed text for search.
     * @param parentHandleSearch        INVALID_HANDLE if has to get the parent handle to search yet,
     *                                  a valid handle if not.
     * @param parentHandle              Parent handle where the search has to be made.
     * @param searchType                It can be TYPE_GENERAL, TYPE_CLOUD_EXPLORER or TYPE_INCOMING_EXPLORER.
     * @param megaCancelToken           Cancel token to identify and cancel the search in question.
     * @param drawerItem                DrawerItem in which the search has to be made.
     * @param sharesTab                 Tab of the Shares section if needed, INVALID_VALUE otherwise.
     * @param isFirstNavigationLevel    True if is first navigation level, false otherwise.
     * @return Single<ArrayList<MegaNode>> The list of nodes if success.
     */
    fun get(
        query: String?,
        parentHandleSearch: Long,
        parentHandle: Long,
        searchType: Int,
        megaCancelToken: MegaCancelToken,
        drawerItem: DrawerItem?,
        sharesTab: Int,
        isFirstNavigationLevel: Boolean,
    ): Single<ArrayList<MegaNode>> =
        Single.create { emitter ->
            if (query == null) {
                emitter.onSuccess(ArrayList<MegaNode>())
                return@create
            }

            var parent: MegaNode? = null

            if (parentHandleSearch == MegaApiJava.INVALID_HANDLE) {
                when (searchType) {
                    TYPE_GENERAL -> {
                        when (drawerItem) {
                            DrawerItem.HOMEPAGE -> parent = megaApi.rootNode
                            DrawerItem.CLOUD_DRIVE -> parent =
                                megaApi.getNodeByHandle(parentHandle)
                            DrawerItem.SHARED_ITEMS -> {
                                when (SharesTab.fromPosition(sharesTab)) {
                                    SharesTab.INCOMING_TAB -> {
                                        if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                                            emitter.onSuccess(getInShares(query, megaCancelToken))
                                            return@create
                                        }

                                        parent = megaApi.getNodeByHandle(parentHandle)
                                    }
                                    SharesTab.OUTGOING_TAB -> {
                                        if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                                            emitter.onSuccess(
                                                getOutShares(
                                                    query,
                                                    megaCancelToken
                                                )
                                            )
                                            return@create
                                        }

                                        parent = megaApi.getNodeByHandle(parentHandle)
                                    }
                                    SharesTab.LINKS_TAB -> {
                                        if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                                            runBlocking {
                                                emitter.onSuccess(
                                                    getLinks(
                                                        query,
                                                        isFirstNavigationLevel,
                                                        megaCancelToken
                                                    )
                                                )
                                            }
                                            return@create
                                        }

                                        parent = megaApi.getNodeByHandle(parentHandle)
                                    }
                                    else -> {
                                        Timber.w("Unhandled current sharesTab value")
                                        return@create
                                    }
                                }
                            }
                            DrawerItem.RUBBISH_BIN -> {
                                parent = if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                                    megaApi.rubbishNode
                                } else {
                                    megaApi.getNodeByHandle(parentHandle)
                                }
                            }
                            DrawerItem.INBOX -> {
                                parent = if (parentHandle == MegaApiJava.INVALID_HANDLE) {
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
                        if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                            emitter.onSuccess(getInShares(query, megaCancelToken))
                            return@create
                        }

                        parent = megaApi.getNodeByHandle(parentHandle)
                    }
                }
            } else {
                parent = megaApi.getNodeByHandle(parentHandleSearch)
            }

            if (parent != null) {
                if (query.isEmpty() || parentHandleSearch != MegaApiJava.INVALID_HANDLE) {
                    emitter.onSuccess(megaApi.getChildren(parent))
                } else {
                    emitter.onSuccess(
                        megaApi.search(
                            parent,
                            query,
                            megaCancelToken,
                            true,
                            sortOrderManagement.getOrderCloud()
                        )
                    )
                }
            }
        }

    /**
     * Gets search result nodes of Incoming section, root navigation level.
     */
    private fun getInShares(query: String, megaCancelToken: MegaCancelToken): ArrayList<MegaNode> =
        if (query.isEmpty()) {
            megaApi.getInShares(sortOrderManagement.getOrderOthers())
        } else {
            megaApi.searchOnInShares(query, megaCancelToken, sortOrderManagement.getOrderCloud())
        }

    /**
     * Gets search result nodes of Outgoing section, root navigation level.
     */
    private fun getOutShares(query: String, megaCancelToken: MegaCancelToken): ArrayList<MegaNode> =
        if (query.isEmpty()) {
            val searchNodes = ArrayList<MegaNode>()
            val outShares = megaApi.outShares
            val addedHandles: MutableList<Long> = ArrayList()
            for (outShare in outShares) {
                val node = megaApi.getNodeByHandle(outShare.nodeHandle)
                if (node != null && !addedHandles.contains(node.handle)) {
                    addedHandles.add(node.handle)
                    searchNodes.add(node)
                }
            }
            if (sortOrderManagement.getOrderOthers() == MegaApiJava.ORDER_DEFAULT_DESC) {
                sortByNameDescending(searchNodes)
            } else {
                sortByNameAscending(searchNodes)
            }

            searchNodes
        } else {
            megaApi.searchOnOutShares(query, megaCancelToken, sortOrderManagement.getOrderCloud())
        }

    /**
     * Gets search result nodes of Links section, root navigation level.
     */
    private suspend fun getLinks(
        query: String,
        isFirstNavigationLevel: Boolean,
        megaCancelToken: MegaCancelToken,
    ): ArrayList<MegaNode> = withContext(ioDispatcher) {
        if (query.isEmpty()) {
            megaApi.getPublicLinks(if (isFirstNavigationLevel) sortOrderIntMapper(getLinksSortOrder()) else sortOrderManagement.getOrderCloud())
        } else {
            megaApi.searchOnPublicLinks(query, megaCancelToken, sortOrderManagement.getOrderCloud())
        }
    }
}