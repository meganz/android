package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.data.repository.MegaNodeRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default SearchNodeUseCase search Nodes from searched Query
 *
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getSearchLinkSharesNodes [GetSearchLinkSharesNodes]
 * @property getSearchInSharesNodes [GetSearchInSharesNodes]
 * @property getSearchOutSharesNodes [GetSearchOutSharesNodes]
 * @property getSearchFromMegaNodeParent [GetSearchFromMegaNodeParent]
 */
class DefaultSearchNodes @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getSearchLinkSharesNodes: GetSearchLinkSharesNodes,
    private val getSearchOutSharesNodes: GetSearchOutSharesNodes,
    private val getSearchInSharesNodes: GetSearchInSharesNodes,
    private val getSearchFromMegaNodeParent: GetSearchFromMegaNodeParent,
) : SearchNodes {
    override suspend fun invoke(
        query: String?,
        parentHandleSearch: Long,
        parentHandle: Long,
        drawerItem: DrawerItem?,
        sharesTab: Int,
        megaCancelToken: MegaCancelToken,
        isFirstLevel: Boolean,
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
                                    return@let getSearchInSharesNodes(query, megaCancelToken)
                                }
                                megaNodeRepository.getNodeByHandle(parentHandle)
                            }
                            SharesTab.OUTGOING_TAB -> {
                                if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                                    return@let getSearchOutSharesNodes(query, megaCancelToken)
                                }
                                megaNodeRepository.getNodeByHandle(parentHandle)
                            }
                            SharesTab.LINKS_TAB -> {
                                if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                                    return@let getSearchLinkSharesNodes(
                                        query,
                                        megaCancelToken,
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

            return getSearchFromMegaNodeParent(
                query = query,
                parentHandleSearch = parentHandleSearch,
                megaCancelToken = megaCancelToken,
                parent = parent
            )
        } ?: run {
            emptyList()
        }
    }
}