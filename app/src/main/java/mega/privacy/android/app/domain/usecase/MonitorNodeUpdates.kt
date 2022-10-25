package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.Node

/**
 * Monitor global node updates for the current logged in user
 */
fun interface MonitorNodeUpdates {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    operator fun invoke(): Flow<List<Node>>
}
