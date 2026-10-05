package mega.privacy.android.feature.sync.domain.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.GetFolderPairsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseAllSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetUserPausedSyncUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PauseAllSyncsUseCaseTest {

    private lateinit var underTest: PauseAllSyncsUseCase

    private val getFolderPairsUseCase = mock<GetFolderPairsUseCase>()
    private val setUserPausedSyncUseCase = mock<SetUserPausedSyncUseCase>()
    private val pauseSyncUseCase = mock<PauseSyncUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = PauseAllSyncsUseCase(
            getFolderPairsUseCase = getFolderPairsUseCase,
            setUserPausedSyncUseCase = setUserPausedSyncUseCase,
            pauseSyncUseCase = pauseSyncUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getFolderPairsUseCase, setUserPausedSyncUseCase, pauseSyncUseCase)
    }

    @ParameterizedTest
    @EnumSource(SyncStatus::class, names = ["SYNCING", "SYNCED"])
    fun `test that invoke marks and pauses an active sync`(status: SyncStatus) = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(listOf(folderPair(1, status = status)))

        underTest()

        verify(setUserPausedSyncUseCase).invoke(1, true)
        verify(pauseSyncUseCase).invoke(1)
    }

    @Test
    fun `test that invoke marks a suspended sync without pausing it again`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(
            listOf(folderPair(1, status = SyncStatus.PAUSED))
        )

        underTest()

        verify(setUserPausedSyncUseCase).invoke(1, true)
        verify(pauseSyncUseCase, never()).invoke(any())
    }

    @Test
    fun `test that invoke leaves a sync with an error untouched`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(
            listOf(folderPair(1, error = SyncError.LOCAL_PATH_UNAVAILABLE))
        )

        underTest()

        verifyNoInteractions(setUserPausedSyncUseCase, pauseSyncUseCase)
    }

    @Test
    fun `test that invoke treats a missing sync error as no error`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(
            listOf(folderPair(1, error = null), folderPair(2, error = SyncError.NO_SYNC_ERROR))
        )

        underTest()

        verify(setUserPausedSyncUseCase).invoke(1, true)
        verify(pauseSyncUseCase).invoke(1)
        verify(setUserPausedSyncUseCase).invoke(2, true)
        verify(pauseSyncUseCase).invoke(2)
    }

    @Test
    fun `test that invoke writes the user pause before pausing the sync`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(listOf(folderPair(1)))

        underTest()

        inOrder(setUserPausedSyncUseCase, pauseSyncUseCase) {
            verify(setUserPausedSyncUseCase).invoke(1, true)
            verify(pauseSyncUseCase).invoke(1)
        }
    }

    @Test
    fun `test that invoke still pauses the sync when the user pause write fails`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(listOf(folderPair(1)))
        whenever(setUserPausedSyncUseCase(1, true)).thenThrow(RuntimeException())

        underTest()

        verify(pauseSyncUseCase).invoke(1)
    }

    @Test
    fun `test that invoke continues with the next sync when one pause fails`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(listOf(folderPair(1), folderPair(2)))
        whenever(pauseSyncUseCase(1)).thenThrow(RuntimeException())

        underTest()

        verify(setUserPausedSyncUseCase).invoke(2, true)
        verify(pauseSyncUseCase).invoke(2)
    }

    @Test
    fun `test that invoke pauses backups too`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(
            listOf(folderPair(1, type = SyncType.TYPE_BACKUP))
        )

        underTest()

        verify(setUserPausedSyncUseCase).invoke(1, true)
        verify(pauseSyncUseCase).invoke(1)
    }

    @Test
    fun `test that invoke returns the number of syncs without an error`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(
            listOf(
                folderPair(1),
                folderPair(2, error = SyncError.LOCAL_PATH_UNAVAILABLE),
                folderPair(3, status = SyncStatus.PAUSED),
            )
        )

        assertThat(underTest()).isEqualTo(2)
    }

    @Test
    fun `test that invoke returns zero when there are no syncs`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(emptyList())

        assertThat(underTest()).isEqualTo(0)
        verifyNoInteractions(setUserPausedSyncUseCase, pauseSyncUseCase)
    }

    @Test
    fun `test that invoke completes every write when cancelled midway`() = runTest {
        val gate = CompletableDeferred<Unit>()
        whenever(getFolderPairsUseCase()).thenReturn(listOf(folderPair(1), folderPair(2)))
        wheneverBlocking { pauseSyncUseCase(1) }.doSuspendableAnswer { gate.await() }

        val job = launch { underTest() }
        runCurrent()
        job.cancel()
        gate.complete(Unit)
        job.join()

        verify(setUserPausedSyncUseCase).invoke(2, true)
        verify(pauseSyncUseCase).invoke(2)
    }

    @Test
    fun `test that invoke rethrows a cancellation from a write`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(listOf(folderPair(1)))
        whenever(setUserPausedSyncUseCase(1, true)).thenThrow(CancellationException())

        assertThrows<CancellationException> { underTest() }
    }

    private fun folderPair(
        id: Long,
        status: SyncStatus = SyncStatus.SYNCING,
        error: SyncError? = null,
        type: SyncType = SyncType.TYPE_TWOWAY,
    ) = FolderPair(
        id,
        type,
        "name$id",
        "localPath$id",
        RemoteFolder(NodeId(id), "remotePath$id"),
        status,
        error,
    )
}
