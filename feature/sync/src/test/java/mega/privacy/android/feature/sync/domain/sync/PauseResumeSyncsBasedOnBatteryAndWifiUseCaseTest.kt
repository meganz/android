package mega.privacy.android.feature.sync.domain.sync

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.feature.sync.data.service.SyncBackgroundService
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
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PauseResumeSyncsBasedOnBatteryAndWifiUseCaseTest {

    private lateinit var underTest: PauseResumeSyncsBasedOnBatteryAndWiFiUseCase

    private val isOnWifiNetworkUseCase = mock<IsOnWifiNetworkUseCase>()
    private val pauseSyncUseCase = mock<PauseSyncUseCase>()
    private val resumeSyncUseCase = mock<ResumeSyncUseCase>()
    private val monitorSyncsUseCase = mock<MonitorSyncsUseCase>()
    private val isSyncPausedByTheUserUseCase = mock<IsSyncPausedByTheUserUseCase>()

    private val firstSyncId = 1L
    private val secondSyncId = 2L

    private val folderPairs = listOf(
        FolderPair(
            id = firstSyncId,
            pairName = "name",
            localFolderPath = "localPath",
            remoteFolder = RemoteFolder(123L, "remotePath"),
            syncStatus = SyncStatus.SYNCING
        ), FolderPair(
            id = secondSyncId,
            pairName = "name2",
            localFolderPath = "localPath2",
            remoteFolder = RemoteFolder(234L, "remotePath2"),
            syncStatus = SyncStatus.SYNCING
        )
    )

    @BeforeAll
    fun setUp() {
        underTest = PauseResumeSyncsBasedOnBatteryAndWiFiUseCase(
            isOnWifiNetworkUseCase,
            pauseSyncUseCase,
            resumeSyncUseCase,
            monitorSyncsUseCase,
            isSyncPausedByTheUserUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isOnWifiNetworkUseCase,
            pauseSyncUseCase,
            resumeSyncUseCase,
            monitorSyncsUseCase,
            isSyncPausedByTheUserUseCase
        )
    }

    @Test
    fun `test that sync is paused when not connected to internet`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(folderPairs))
        whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
        whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(true)

        underTest(
            connectedToInternet = false,
            syncOnlyByWifi = true,
            batteryInfo = BatteryInfo(100, true),
            isFreeAccount = false
        )

        verify(pauseSyncUseCase).invoke(firstSyncId)
        verifyNoInteractions(resumeSyncUseCase)
    }

    @Test
    fun `test that sync is resumed when connected to internet and not only on wifi`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(folderPairs))
        whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
        whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(true)

        underTest(
            connectedToInternet = true,
            syncOnlyByWifi = false,
            batteryInfo = BatteryInfo(100, true),
            isFreeAccount = false
        )

        verify(resumeSyncUseCase).invoke(firstSyncId)
        verifyNoInteractions(pauseSyncUseCase)
    }

    @Test
    fun `test that sync is paused when device has low battery level and not charging`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(folderPairs))
        whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
        whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(true)

        underTest(
            connectedToInternet = true,
            syncOnlyByWifi = true,
            batteryInfo = BatteryInfo(SyncBackgroundService.LOW_BATTERY_LEVEL - 1, false),
            isFreeAccount = false
        )

        verify(pauseSyncUseCase).invoke(firstSyncId)
        verifyNoInteractions(resumeSyncUseCase)
    }

    @Test
    fun `test that sync is resumed when device has low battery level but device is charging`() =
        runTest {
            whenever(monitorSyncsUseCase()).thenReturn(flowOf(folderPairs))
            whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
            whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(true)

            underTest(
                connectedToInternet = true,
                syncOnlyByWifi = false,
                batteryInfo = BatteryInfo(SyncBackgroundService.LOW_BATTERY_LEVEL - 1, true),
                isFreeAccount = false
            )

            verify(resumeSyncUseCase).invoke(firstSyncId)
            verifyNoInteractions(pauseSyncUseCase)
        }

    @Test
    fun `test that sync is paused when user account is not pro`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(folderPairs))
        whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
        whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(true)

        underTest(
            connectedToInternet = true,
            syncOnlyByWifi = true,
            batteryInfo = BatteryInfo(100, true),
            isFreeAccount = true
        )

        verify(pauseSyncUseCase).invoke(firstSyncId)
        verifyNoInteractions(resumeSyncUseCase)
    }

    @Test
    fun `test that sync is resumed when user account is pro`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(folderPairs))
        whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
        whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(true)

        underTest(
            connectedToInternet = true,
            syncOnlyByWifi = true,
            batteryInfo = BatteryInfo(100, true),
            isFreeAccount = false
        )

        verify(pauseSyncUseCase).invoke(firstSyncId)
        verifyNoInteractions(resumeSyncUseCase)
    }
}