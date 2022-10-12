package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Use Case to retrieve the list of Inbox Children Nodes
 */
fun interface GetInboxChildrenNodes {

    /**
     * Retrieve the list of Inbox Children Nodes
     *
     * @return the list of Inbox Children Nodes
     */
    suspend operator fun invoke(): List<MegaNode>
}