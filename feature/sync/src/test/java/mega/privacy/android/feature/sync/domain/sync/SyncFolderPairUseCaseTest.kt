package mega.privacy.android.feature.sync.domain.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.SyncFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncFolderPairUseCaseTest {

    private val syncRepository: SyncRepository = mock()
    private val pauseSyncUseCase: PauseSyncUseCase = mock()
    private val monitorShouldSyncUseCase: MonitorShouldSyncUseCase = mock()

    private val underTest = SyncFolderPairUseCase(
        syncRepository = syncRepository,
        pauseSyncUseCase = pauseSyncUseCase,
        monitorShouldSyncUseCase = monitorShouldSyncUseCase
    )

    @AfterEach
    fun resetMocks() {
        reset(
            syncRepository,
            pauseSyncUseCase,
            monitorShouldSyncUseCase
        )
    }

    @Test
    fun `test that folder pair is set up successfully and sync is not paused when should sync is true`() =
        runTest {
            val syncType = SyncType.TYPE_TWOWAY
            val name = "Test Sync"
            val localPath = "/storage/emulated/0/DCIM"
            val remoteFolder = RemoteFolder(
                id = mega.privacy.android.domain.entity.node.NodeId(123L),
                name = "Camera"
            )
            val folderPairHandle = 456L

            whenever(syncRepository.setupFolderPair(syncType, name, localPath, 123L))
                .thenReturn(folderPairHandle)
            whenever(monitorShouldSyncUseCase()).thenReturn(flowOf(true))

            val result = underTest(syncType, name, localPath, remoteFolder)

            assertThat(result).isTrue()
            verify(pauseSyncUseCase, never()).invoke(folderPairHandle)
        }

    @Test
    fun `test that folder pair is set up successfully and sync is paused when should sync is false`() =
        runTest {
            val syncType = SyncType.TYPE_TWOWAY
            val name = "Backup Sync"
            val localPath = "/storage/emulated/0/Documents"
            val remoteFolder = RemoteFolder(
                id = mega.privacy.android.domain.entity.node.NodeId(789L),
                name = "Documents"
            )
            val folderPairHandle = 101112L

            whenever(syncRepository.setupFolderPair(syncType, name, localPath, 789L))
                .thenReturn(folderPairHandle)
            whenever(monitorShouldSyncUseCase()).thenReturn(flowOf(false))

            val result = underTest(syncType, name, localPath, remoteFolder)

            assertThat(result).isTrue()
            verify(pauseSyncUseCase).invoke(folderPairHandle)
        }

    @Test
    fun `test that setup fails when repository returns null handle`() = runTest {
        val syncType = SyncType.TYPE_TWOWAY
        val name = null
        val localPath = "/storage/emulated/0/Music"
        val remoteFolder =
            RemoteFolder(id = mega.privacy.android.domain.entity.node.NodeId(999L), name = "Music")

        whenever(syncRepository.setupFolderPair(syncType, name, localPath, 999L))
            .thenReturn(null)

        val result = underTest(syncType, name, localPath, remoteFolder)

        assertThat(result).isFalse()
        verify(pauseSyncUseCase, never()).invoke(anyLong())
    }

    @Test
    fun `test that sync is not paused when repository setup fails`() = runTest {
        val syncType = SyncType.TYPE_TWOWAY
        val name = "Failed Sync"
        val localPath = "/storage/emulated/0/Pictures"
        val remoteFolder = RemoteFolder(
            id = mega.privacy.android.domain.entity.node.NodeId(555L),
            name = "Pictures"
        )

        whenever(syncRepository.setupFolderPair(syncType, name, localPath, 555L))
            .thenReturn(null)
        whenever(monitorShouldSyncUseCase()).thenReturn(flowOf(false))

        val result = underTest(syncType, name, localPath, remoteFolder)

        assertThat(result).isFalse()
        verifyNoInteractions(pauseSyncUseCase)
    }

    @Test
    fun `test that sync uses correct remote folder id from RemoteFolder`() = runTest {
        val syncType = SyncType.TYPE_TWOWAY
        val name = "ID Test"
        val localPath = "/storage/emulated/0/Download"
        val expectedRemoteFolderId = 777888999L
        val remoteFolder = RemoteFolder(
            id = mega.privacy.android.domain.entity.node.NodeId(expectedRemoteFolderId),
            name = "Download"
        )
        val folderPairHandle = 12345L

        whenever(syncRepository.setupFolderPair(syncType, name, localPath, expectedRemoteFolderId))
            .thenReturn(folderPairHandle)
        whenever(monitorShouldSyncUseCase()).thenReturn(flowOf(true))

        assertThat(underTest(syncType, name, localPath, remoteFolder)).isTrue()
    }
}
