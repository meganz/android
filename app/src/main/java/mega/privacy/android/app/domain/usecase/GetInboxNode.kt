package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Use case for getting the Inbox node.
 */
fun interface GetInboxNode {

    /**
     * Invoke.
     *
     * @return The Inbox node if exists, null otherwise.
     */
    suspend operator fun invoke(): MegaNode?
}