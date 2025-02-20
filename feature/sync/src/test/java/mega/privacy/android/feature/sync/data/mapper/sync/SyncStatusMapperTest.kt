package mega.privacy.android.feature.sync.data.mapper.sync

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.feature.sync.data.mapper.SyncStatusMapper
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncStats
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

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

    @ParameterizedTest(name = "when BackupState is {0}, then SyncStatus is {1}")
    @MethodSource("provideBackupStateParameters")
    fun `test that BackupState mapping is correct`(
        backupState: BackupState,
        syncStatus: SyncStatus
    ) {
        assertThat(underTest(backupState)).isEqualTo(syncStatus)
    }

    private fun provideBackupStateParameters() = Stream.of(
        Arguments.of(BackupState.INVALID, SyncStatus.ERROR),
        Arguments.of(BackupState.NOT_INITIALIZED, SyncStatus.DISABLED),
        Arguments.of(BackupState.ACTIVE, SyncStatus.SYNCED),
        Arguments.of(BackupState.FAILED, SyncStatus.ERROR),
        Arguments.of(BackupState.TEMPORARILY_DISABLED, SyncStatus.DISABLED),
        Arguments.of(BackupState.DISABLED, SyncStatus.DISABLED),
        Arguments.of(BackupState.PAUSE_UPLOADS, SyncStatus.PAUSED),
        Arguments.of(BackupState.PAUSE_DOWNLOADS, SyncStatus.PAUSED),
        Arguments.of(BackupState.PAUSE_ALL, SyncStatus.PAUSED),
        Arguments.of(BackupState.DELETED, SyncStatus.ERROR),
    )
}