package test.mega.privacy.android.app.presentation.shares

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import nz.mega.sdk.MegaNode

/**
 * Fake MonitorNodeUpdates class to simulate values emission
 */
class FakeMonitorUpdates : MonitorNodeUpdates {

    private val flow = MutableSharedFlow<List<MegaNode>>()

    suspend fun emit(value: List<MegaNode>) = flow.emit(value)

    override fun invoke(): Flow<List<MegaNode>> = flow
}
