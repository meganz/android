package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get Backups Node for the current logged in user
 */
fun interface GetBackupsNode {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    suspend operator fun invoke(): MegaNode?
}