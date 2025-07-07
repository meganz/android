package mega.privacy.android.feature.sync.domain.sync

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.IsSyncPausedByTheUserUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PauseResumeSyncsBasedOnBatteryAndWifiUseCaseTest {

    private lateinit var underTest: PauseResumeSyncsBasedOnBatteryAndWiFiUseCase

    private val pauseSyncUseCase = mock<PauseSyncUseCase>()
    private val resumeSyncUseCase = mock<ResumeSyncUseCase>()
    private val monitorSyncsUseCase = mock<MonitorSyncsUseCase>()
    private val isSyncPausedByTheUserUseCase = mock<IsSyncPausedByTheUserUseCase>()

    private val firstSyncId = 1L
    private val secondSyncId = 2L

    private val folderPairs = listOf(
        FolderPair(
            id = firstSyncId,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "name",
            localFolderPath = "localPath",
            remoteFolder = RemoteFolder(NodeId(123L), "remotePath"),
            syncStatus = SyncStatus.SYNCING
        ), FolderPair(
            id = secondSyncId,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "name2",
            localFolderPath = "localPath2",
            remoteFolder = RemoteFolder(NodeId(234L), "remotePath2"),
            syncStatus = SyncStatus.SYNCING
        )
    )

    @BeforeAll
    fun setUp() {
        underTest = PauseResumeSyncsBasedOnBatteryAndWiFiUseCase(
            pauseSyncUseCase,
            resumeSyncUseCase,
            monitorSyncsUseCase,
            isSyncPausedByTheUserUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            pauseSyncUseCase,
            resumeSyncUseCase,
            monitorSyncsUseCase,
            isSyncPausedByTheUserUseCase
        )
    }

    @Test
    fun `test that sync is paused when  invoked with false`() = runTest {
        val newPairs = folderPairs.mapIndexed { index, item ->
            item.copy(
                syncStatus = if (index == 0) SyncStatus.SYNCING else SyncStatus.SYNCED,
            )
        }
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(newPairs))


        underTest(false)

        verify(pauseSyncUseCase).invoke(secondSyncId)
        verifyNoInteractions(resumeSyncUseCase, isSyncPausedByTheUserUseCase)
    }

    @Test
    fun `test that sync is resumed when invoked with true`() = runTest {
        val newPairs = folderPairs.mapIndexed { index, item ->
            item.copy(
                syncStatus = SyncStatus.PAUSED,
            )
        }
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(newPairs))
        whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
        whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(true)

        underTest(true)

        verify(resumeSyncUseCase).invoke(firstSyncId)
        verify(resumeSyncUseCase, times(0)).invoke(secondSyncId)
        verifyNoInteractions(pauseSyncUseCase)
    }

    @Test

    fun `test that sync is not resumed when syncs have errors`() =
        runTest {
            val newPairs = folderPairs.mapIndexed { index, item ->
                item.copy(
                    syncStatus = if (index == 0) SyncStatus.PAUSED else SyncStatus.ERROR,
                    syncError = if (index == 0) SyncError.NO_SYNC_ERROR else SyncError.UNKNOWN_ERROR,
                )
            }
            whenever(monitorSyncsUseCase()).thenReturn(flowOf(newPairs))
            whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
            whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(false)

            underTest(true)

            verify(resumeSyncUseCase).invoke(firstSyncId)
            verify(resumeSyncUseCase, times(0)).invoke(secondSyncId)
            verifyNoInteractions(pauseSyncUseCase)
        }

    @Test
    fun `test that sync is not paused when syncs have error`() =
        runTest {
            val newPairs = folderPairs.mapIndexed { index, item ->
                item.copy(
                    syncStatus = if (index == 0) SyncStatus.SYNCING else SyncStatus.ERROR,
                    syncError = if (index == 0) SyncError.NO_SYNC_ERROR else SyncError.UNKNOWN_ERROR,
                )
            }
            whenever(monitorSyncsUseCase()).thenReturn(flowOf(newPairs))
            whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
            whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(false)

            underTest(false)

            verify(pauseSyncUseCase).invoke(firstSyncId)
            verify(pauseSyncUseCase, times(0)).invoke(secondSyncId)
            verifyNoInteractions(resumeSyncUseCase)
        }
}
