package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.main.DrawerItem
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode

/**
 * Use case which search nodes.
 */
interface SearchNodeUseCase {

    /**
     * Use Case which search Nodes
     * @param query query string
     * @param parentHandleSearch ParentHandleSearch
     * @param parentHandle ParentHandle
     * @param drawerItem [DrawerItem]
     * @param sharesTab sharesTab
     * @param megaCancelToken [MegaCancelToken]
     * @param isFirstLevel firstLevel
     */
    suspend operator fun invoke(
        query: String?,
        parentHandleSearch: Long,
        parentHandle: Long,
        drawerItem: DrawerItem?,
        sharesTab: Int,
        megaCancelToken: MegaCancelToken,
        isFirstLevel: Boolean,
    ): List<MegaNode>?
}