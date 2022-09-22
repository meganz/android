package mega.privacy.android.app.domain.usecase


import nz.mega.sdk.MegaNode

/**
 * Get Inbox Node for the current logged in user
 */
fun interface GetInboxNode {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    suspend operator fun invoke(): MegaNode?
}