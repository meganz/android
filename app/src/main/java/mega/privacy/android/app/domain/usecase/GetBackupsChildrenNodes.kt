package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import nz.mega.sdk.MegaNode

/**
 * Use Case to retrieve the list of Backups Children Nodes
 */
fun interface GetBackupsChildrenNodes {

    /**
     * Returns a [Flow] of the latest Backups Children Nodes
     *
     * @return a [Flow] of the latest Backups Children Nodes
     */
    operator fun invoke(): Flow<List<MegaNode>>
}