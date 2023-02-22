package mega.privacy.android.app.search.usecase

import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case which search nodes.
 *
 * @property megaApi                MegaApiAndroid object
 * @property getCloudSortOrder      GetCloudSortOrder
 * @property getOthersSortOrder     GetOthersSortOrder
 * @property getLinksSortOrder      GetLinksSortOrder
 * @property sortOrderIntMapper     SortOrderIntMapper
 * @property ioDispatcher           CoroutineDispatcher
 */

@Deprecated(
    message = "This class is deprecated with new use cases introduced in",
    replaceWith = ReplaceWith(
        expression = "For Cloud Explorer and IncomingSharesExplorer ",
        imports = arrayOf(
            "GetCloudExplorerSearchNode",
            "GetIncomingExplorerSearchNode"
        )
    )
)
class SearchNodesUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val getLinksSortOrder: GetLinksSortOrder,
    private val sortOrderIntMapper: SortOrderIntMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    companion object {
        //Available types of search.
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

    @Deprecated(
        message = "This method is deprecated with new use cases introduced in",
        replaceWith = ReplaceWith(
            expression = "For Cloud Explorer and IncomingSharesExplorer ",
            imports = arrayOf(
                "getCloudExplorerSearchNode()",
                "getIncomingExplorerSearchNode()"
            )
        )
    )
    fun get(
        query: String?,
        parentHandleSearch: Long,
        parentHandle: Long,
        searchType: Int,
        megaCancelToken: MegaCancelToken,
    ): Single<ArrayList<MegaNode>> =
        Single.create { emitter ->
            if (query == null) {
                emitter.onSuccess(ArrayList())
                return@create
            }

            var parent: MegaNode? = null

            if (parentHandleSearch == MegaApiJava.INVALID_HANDLE) {
                when (searchType) {
                    TYPE_CLOUD_EXPLORER -> {
                        parent = megaApi.getNodeByHandle(parentHandle)
                    }
                    TYPE_INCOMING_EXPLORER -> {
                        if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                            runBlocking {
                                emitter.onSuccess(getInShares(query, megaCancelToken))
                            }
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
                    runBlocking {
                        emitter.onSuccess(
                            megaApi.search(
                                parent,
                                query,
                                megaCancelToken,
                                true,
                                sortOrderIntMapper(getCloudSortOrder())
                            )
                        )
                    }
                }
            }
        }

    /**
     * Gets search result nodes of Incoming section, root navigation level.
     */
    private suspend fun getInShares(
        query: String,
        megaCancelToken: MegaCancelToken,
    ): ArrayList<MegaNode> =
        if (query.isEmpty()) {
            megaApi.getInShares(sortOrderIntMapper(getOthersSortOrder()))
        } else {
            megaApi.searchOnInShares(
                query,
                megaCancelToken,
                sortOrderIntMapper(getCloudSortOrder())
            )
        }
}