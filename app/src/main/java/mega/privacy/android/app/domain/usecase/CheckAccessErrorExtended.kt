package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare

/**
 * Check access error extended
 *
 */
fun interface CheckAccessErrorExtended {
    /**
     * Invoke.
     *
     * @param node
     * @param level
     * - [MegaShare.ACCESS_UNKNOWN]
     * - [MegaShare.ACCESS_READ]
     * - [MegaShare.ACCESS_READWRITE]
     * - [MegaShare.ACCESS_FULL]
     * - [MegaShare.ACCESS_OWNER]
     */
    suspend operator fun invoke(node: MegaNode, level: Int): MegaException
}