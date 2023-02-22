package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode

/**
 * Use Case which search Nodes in CloudExplorer
 */
interface GetCloudExplorerSearchNode {

    /**
     * Use Case which search Nodes in CloudExplorer
     * @param query Query to be searched
     * @param parentHandle
     * @param parentHandleSearch
     * @param megaCancelToken [MegaCancelToken]
     */
    suspend operator fun invoke(
        query: String?,
        parentHandle: Long,
        parentHandleSearch: Long,
        megaCancelToken: MegaCancelToken,
    ): List<MegaNode>
}