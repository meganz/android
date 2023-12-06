package mega.privacy.android.feature.sync.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncStats
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncStatusMapperTest {

    private val underTest = SyncStatusMapper()

    private val megaSyncStats: MegaSyncStats = mock()

    @Test
    fun `test that the sync status is paused if the run state is paused`() {
        val runState = MegaSync.SyncRunningState.RUNSTATE_SUSPENDED.swigValue()

        val syncStatus = underTest(syncStats = megaSyncStats, runningState = runState)

        assertThat(syncStatus).isEqualTo(SyncStatus.PAUSED)
    }

    @Test
    fun `test that the sync status is synced if sync stats are null and run state is running`() {
        val runState = MegaSync.SyncRunningState.RUNSTATE_RUNNING.swigValue()

        val syncStatus = underTest(syncStats = megaSyncStats, runningState = runState)

        assertThat(syncStatus).isEqualTo(SyncStatus.SYNCED)
    }

    @Test
    fun `test that the sync status is syncing if sync stats are syncing and run state is running`() {
        val runState = MegaSync.SyncRunningState.RUNSTATE_RUNNING.swigValue()
        whenever(megaSyncStats.isSyncing).thenReturn(true)
        whenever(megaSyncStats.isScanning).thenReturn(false)

        val syncStatus = underTest(syncStats = megaSyncStats, runningState = runState)

        assertThat(syncStatus).isEqualTo(SyncStatus.SYNCING)
    }

    @Test
    fun `test that the sync status is syncing if sync stats are scanning and run state is running`() {
        val runState = MegaSync.SyncRunningState.RUNSTATE_RUNNING.swigValue()
        whenever(megaSyncStats.isSyncing).thenReturn(false)
        whenever(megaSyncStats.isScanning).thenReturn(true)

        val syncStatus = underTest(syncStats = megaSyncStats, runningState = runState)

        assertThat(syncStatus).isEqualTo(SyncStatus.SYNCING)
    }
}