package mega.privacy.android.data.mapper.node

import mega.privacy.android.domain.entity.shares.AccessPermission
import nz.mega.sdk.MegaNode

/**
 * Mapper from mega node to method to set [AccessPermission] for the given users to this node
 */
fun interface NodeShareKeyResultMapper {
    /**
     * Mapper from mega node to method to set [AccessPermission] for the given users to this node
     * @param megaNode [MegaNode] to set access permission
     * @return a suspend block to be executed to set the corresponding [AccessPermission] to the user with the given email
     */
    operator fun invoke(
        megaNode: MegaNode,
    ): (suspend (AccessPermission, email: String) -> Unit)
}