package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.MonitorBackupFolder

/**
 * Fake [MonitorBackupFolder] to simulate the emission of values
 */
class FakeMonitorBackupFolder : MonitorBackupFolder {
    private val flow = MutableSharedFlow<Result<NodeId>>()

    suspend fun emit(value: Result<NodeId>) = flow.emit(value)

    override fun invoke(): Flow<Result<NodeId>> = flow
}