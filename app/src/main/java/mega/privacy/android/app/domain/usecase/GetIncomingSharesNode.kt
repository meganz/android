package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get a list of all incoming shares
 */
fun interface GetIncomingSharesNode {
    /**
     * Get a list of all incoming shares
     *
     * @param order sort order, if null the default order is applied
     * @return List of MegaNode that other users are sharing with this account
     */
    suspend operator fun invoke(order: Int?): List<MegaNode>
}