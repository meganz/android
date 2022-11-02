package test.mega.privacy.android.app.presentation.shares

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.domain.entity.node.Node

/**
 * Fake MonitorNodeUpdates class to simulate values emission
 */
class FakeMonitorUpdates : MonitorNodeUpdates {

    private val flow = MutableSharedFlow<List<Node>>()

    suspend fun emit(value: List<Node>) = flow.emit(value)

    override fun invoke(): Flow<List<Node>> = flow
}
