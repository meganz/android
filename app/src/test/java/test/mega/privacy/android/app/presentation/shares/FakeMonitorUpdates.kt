package test.mega.privacy.android.app.presentation.shares

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeUpdate

/**
 * Fake MonitorNodeUpdates class to simulate values emission
 */
class FakeMonitorUpdates : MonitorNodeUpdates {

    private val flow = MutableSharedFlow<NodeUpdate>()

    suspend fun emit(value: NodeUpdate) = flow.emit(value)

    override fun invoke(): Flow<NodeUpdate> = flow
}
