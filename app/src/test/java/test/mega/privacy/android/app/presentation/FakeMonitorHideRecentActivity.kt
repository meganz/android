package test.mega.privacy.android.app.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import mega.privacy.android.domain.usecase.MonitorHideRecentActivity
import nz.mega.sdk.MegaNode

/**
 * Fake MonitorNodeUpdates class to simulate values emission
 */
class FakeMonitorHideRecentActivity : MonitorHideRecentActivity {

    private val flow = MutableSharedFlow<Boolean>()

    suspend fun emit(value: Boolean) = flow.emit(value)

    override fun invoke(): Flow<Boolean> = flow
}
