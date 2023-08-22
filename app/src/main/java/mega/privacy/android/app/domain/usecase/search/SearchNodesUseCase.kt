package mega.privacy.android.app.domain.usecase.search

import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.data.repository.MegaNodeRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default SearchNodeUseCase search Nodes from searched Query
 *
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getSearchLinkSharesNodesUseCase [GetSearchLinkSharesNodesUseCase]
 * @property getSearchInSharesNodesUseCase [GetSearchInSharesNodesUseCase]
 * @property getSearchOutSharesNodesUseCase [GetSearchOutSharesNodesUseCase]
 * @property getSearchFromMegaNodeParentUseCase [GetSearchFromMegaNodeParentUseCase]
 */
class SearchNodesUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getSearchLinkSharesNodesUseCase: GetSearchLinkSharesNodesUseCase,
    private val getSearchOutSharesNodesUseCase: GetSearchOutSharesNodesUseCase,
    private val getSearchInSharesNodesUseCase: GetSearchInSharesNodesUseCase,
    private val getSearchFromMegaNodeParentUseCase: GetSearchFromMegaNodeParentUseCase,
) {

    /**
     * Use Case which search Nodes
     * @param query query string
     * @param parentHandleSearch ParentHandleSearch
     * @param parentHandle ParentHandle
     * @param drawerItem [DrawerItem]
     * @param sharesTab sharesTab
     * @param isFirstLevel firstLevel
     */
    suspend operator fun invoke(
        query: String?,
        parentHandleSearch: Long,
        parentHandle: Long,
        drawerItem: DrawerItem?,
        sharesTab: Int,
        isFirstLevel: Boolean,
        searchType: Int = -1
    ): List<MegaNode>? {
        return query?.let {
            val parent = if (parentHandleSearch == MegaApiJava.INVALID_HANDLE) {
                when (drawerItem) {
                    DrawerItem.HOMEPAGE -> megaNodeRepository.getRootNode()
                    DrawerItem.CLOUD_DRIVE ->
                        megaNodeRepository.getNodeByHandle(parentHandle)

                    DrawerItem.SHARED_ITEMS -> {
                        when (SharesTab.fromPosition(sharesTab)) {
                            SharesTab.INCOMING_TAB -> {
                                if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                                    return@let getSearchInSharesNodesUseCase(query)
                                }
                                megaNodeRepository.getNodeByHandle(parentHandle)
                            }

                            SharesTab.OUTGOING_TAB -> {
                                if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                                    return@let getSearchOutSharesNodesUseCase(query)
                                }
                                megaNodeRepository.getNodeByHandle(parentHandle)
                            }

                            SharesTab.LINKS_TAB -> {
                                if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                                    return@let getSearchLinkSharesNodesUseCase(
                                        query,
                                        isFirstLevel
                                    )
                                }
                                megaNodeRepository.getNodeByHandle(parentHandle)
                            }

                            else -> {
                                null
                            }
                        }
                    }

                    DrawerItem.RUBBISH_BIN -> {
                        if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                            megaNodeRepository.getRubbishBinNode()
                        } else {
                            megaNodeRepository.getNodeByHandle(parentHandle)
                        }
                    }

                    DrawerItem.INBOX -> {
                        if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                            megaNodeRepository.getInboxNode()
                        } else {
                            megaNodeRepository.getNodeByHandle(parentHandle)
                        }
                    }

                    else -> megaNodeRepository.getRootNode()
                }
            } else {
                megaNodeRepository.getNodeByHandle(parentHandleSearch)
            }

            return getSearchFromMegaNodeParentUseCase(
                query = query,
                parentHandleSearch = parentHandleSearch,
                parent = parent,
                searchType = searchType
            )
        } ?: run {
            emptyList()
        }
    }
}