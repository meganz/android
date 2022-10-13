package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import nz.mega.sdk.MegaNode

/**
 * Use Case to retrieve the list of Inbox Children Nodes
 */
fun interface GetInboxChildrenNodes {

    /**
     * Returns a [Flow] of the latest Inbox Children Nodes
     *
     * @return a [Flow] of the latest Inbox Children Nodes
     */
    operator fun invoke(): Flow<List<MegaNode>>
}