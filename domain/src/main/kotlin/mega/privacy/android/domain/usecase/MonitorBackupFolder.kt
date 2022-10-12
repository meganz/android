package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Monitor backup folder
 */
fun interface MonitorBackupFolder {
    /**
     * Return a flow of the latest backup folder id
     */
    operator fun invoke(): Flow<NodeId>
}