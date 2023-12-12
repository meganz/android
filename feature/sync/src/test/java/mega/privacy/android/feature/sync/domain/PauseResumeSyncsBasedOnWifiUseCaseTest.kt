package mega.privacy.android.feature.sync.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.GetFolderPairsUseCase
import mega.privacy.android.feature.sync.domain.usecase.IsSyncPausedByTheUserUseCase
import mega.privacy.android.feature.sync.domain.usecase.PauseResumeSyncsBasedOnWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.ResumeSyncUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PauseResumeSyncsBasedOnWifiUseCaseTest {

    private lateinit var underTest: PauseResumeSyncsBasedOnWiFiUseCase

    private val isOnWifiNetworkUseCase = mock<IsOnWifiNetworkUseCase>()
    private val pauseSyncUseCase = mock<PauseSyncUseCase>()
    private val resumeSyncUseCase = mock<ResumeSyncUseCase>()
    private val getFolderPairsUseCase = mock<GetFolderPairsUseCase>()
    private val isSyncPausedByTheUserUseCase = mock<IsSyncPausedByTheUserUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = PauseResumeSyncsBasedOnWiFiUseCase(
            isOnWifiNetworkUseCase,
            pauseSyncUseCase,
            resumeSyncUseCase,
            getFolderPairsUseCase,
            isSyncPausedByTheUserUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isOnWifiNetworkUseCase,
            pauseSyncUseCase,
            resumeSyncUseCase,
            getFolderPairsUseCase,
            isSyncPausedByTheUserUseCase
        )
    }

    @Test
    fun `test that sync is paused when not connected to internet`() = runTest {
        val firstSyncId = 1L
        val secondSyncId = 2L

        val folderPairs = listOf(
            FolderPair(
                firstSyncId,
                "name",
                "localPath",
                RemoteFolder(123L, "remotePath"),
                SyncStatus.SYNCING
            ),
            FolderPair(
                secondSyncId,
                "name2",
                "localPath2",
                RemoteFolder(123L, "remotePath2"),
                SyncStatus.SYNCING
            )
        )
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)
        whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
        whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(true)

        underTest(connectedToInternet = false, syncOnlyByWifi = true)

        verify(pauseSyncUseCase).invoke(firstSyncId)
        verifyNoInteractions(resumeSyncUseCase)
    }

    @Test
    fun `test that sync is resumed when connected to internet and not only on wifi`() = runTest {
        val firstSyncId = 1L
        val secondSyncId = 2L

        val folderPairs = listOf(
            FolderPair(
                firstSyncId,
                "name",
                "localPath",
                RemoteFolder(123L, "remotePath"),
                SyncStatus.SYNCING
            ),
            FolderPair(
                secondSyncId,
                "name2",
                "localPath2",
                RemoteFolder(123L, "remotePath2"),
                SyncStatus.SYNCING
            )
        )
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)
        whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
        whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(true)

        underTest(connectedToInternet = true, syncOnlyByWifi = false)

        verify(resumeSyncUseCase).invoke(firstSyncId)
        verifyNoInteractions(pauseSyncUseCase)
    }
}