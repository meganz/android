package mega.privacy.android.feature.sync.domain.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeAllSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.IsSyncPausedByTheUserUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResumeAllSyncsUseCaseTest {

    private lateinit var underTest: ResumeAllSyncsUseCase

    private val getFolderPairsUseCase = mock<GetFolderPairsUseCase>()
    private val isSyncPausedByTheUserUseCase = mock<IsSyncPausedByTheUserUseCase>()
    private val setUserPausedSyncUseCase = mock<SetUserPausedSyncUseCase>()
    private val resumeSyncUseCase = mock<ResumeSyncUseCase>()
    private val monitorShouldSyncUseCase = mock<MonitorShouldSyncUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = ResumeAllSyncsUseCase(
            getFolderPairsUseCase = getFolderPairsUseCase,
            isSyncPausedByTheUserUseCase = isSyncPausedByTheUserUseCase,
            setUserPausedSyncUseCase = setUserPausedSyncUseCase,
            resumeSyncUseCase = resumeSyncUseCase,
            monitorShouldSyncUseCase = monitorShouldSyncUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getFolderPairsUseCase,
            isSyncPausedByTheUserUseCase,
            setUserPausedSyncUseCase,
            resumeSyncUseCase,
            monitorShouldSyncUseCase,
        )
        whenever(monitorShouldSyncUseCase()).thenReturn(flowOf(true))
    }

    @Test
    fun `test that invoke clears the user pause and resumes a paused sync when syncing is allowed`() =
        runTest {
            stubSyncs(pausedSync(1), markedIds = setOf(1))

            underTest()

            verify(setUserPausedSyncUseCase).invoke(1, false)
            verify(resumeSyncUseCase).invoke(1)
        }

    @Test
    fun `test that invoke clears the user pause without resuming when a condition holds syncs back`() =
        runTest {
            whenever(monitorShouldSyncUseCase()).thenReturn(flowOf(false))
            stubSyncs(pausedSync(1), markedIds = setOf(1))

            val result = underTest()

            verify(setUserPausedSyncUseCase).invoke(1, false)
            verify(resumeSyncUseCase, never()).invoke(any())
            assertThat(result).isEqualTo(1)
        }

    @Test
    fun `test that invoke clears the user pause before resuming the sync`() = runTest {
        stubSyncs(pausedSync(1), markedIds = setOf(1))

        underTest()

        inOrder(setUserPausedSyncUseCase, resumeSyncUseCase) {
            verify(setUserPausedSyncUseCase).invoke(1, false)
            verify(resumeSyncUseCase).invoke(1)
        }
    }

    @Test
    fun `test that invoke does not resume a sync whose user pause could not be cleared`() =
        runTest {
            stubSyncs(pausedSync(1), pausedSync(2), markedIds = setOf(1, 2))
            whenever(setUserPausedSyncUseCase(1, false)).thenThrow(RuntimeException())

            underTest()

            verify(resumeSyncUseCase, never()).invoke(1)
            verify(setUserPausedSyncUseCase).invoke(2, false)
            verify(resumeSyncUseCase).invoke(2)
        }

    @Test
    fun `test that invoke clears the user pause of a sync with an error without resuming it`() =
        runTest {
            stubSyncs(pausedSync(1, error = SyncError.STORAGE_OVERQUOTA), markedIds = setOf(1))

            underTest()

            verify(setUserPausedSyncUseCase).invoke(1, false)
            verify(resumeSyncUseCase, never()).invoke(any())
        }

    @ParameterizedTest
    @EnumSource(SyncStatus::class, names = ["SYNCING", "SYNCED"])
    fun `test that invoke clears the user pause of an active sync without resuming it`(
        status: SyncStatus,
    ) = runTest {
        stubSyncs(folderPair(1, status = status), markedIds = setOf(1))

        underTest()

        verify(setUserPausedSyncUseCase).invoke(1, false)
        verify(resumeSyncUseCase, never()).invoke(any())
    }

    @Test
    fun `test that invoke clears the user pause without resuming a sync shown in error for the storage quota`() =
        runTest {
            stubSyncs(folderPair(1, status = SyncStatus.ERROR, error = null), markedIds = setOf(1))

            underTest()

            verify(setUserPausedSyncUseCase).invoke(1, false)
            verify(resumeSyncUseCase, never()).invoke(any())
        }

    @Test
    fun `test that invoke leaves syncs not paused by the user untouched`() = runTest {
        stubSyncs(pausedSync(1), markedIds = emptySet())

        underTest()

        verify(setUserPausedSyncUseCase, never()).invoke(any(), any())
        verify(resumeSyncUseCase, never()).invoke(any())
    }

    @Test
    fun `test that invoke treats a sync as not paused by the user when the read fails`() =
        runTest {
            stubSyncs(pausedSync(1), pausedSync(2), markedIds = setOf(2))
            whenever(isSyncPausedByTheUserUseCase(1)).thenThrow(RuntimeException())

            val result = underTest()

            verify(setUserPausedSyncUseCase, never()).invoke(1, false)
            verify(resumeSyncUseCase, never()).invoke(1)
            verify(setUserPausedSyncUseCase).invoke(2, false)
            verify(resumeSyncUseCase).invoke(2)
            assertThat(result).isEqualTo(1)
        }

    @Test
    fun `test that invoke writes nothing when the sync condition read fails`() = runTest {
        whenever(monitorShouldSyncUseCase()).thenReturn(flow { throw RuntimeException() })

        assertThrows<RuntimeException> { underTest() }

        verifyNoInteractions(setUserPausedSyncUseCase, resumeSyncUseCase, getFolderPairsUseCase)
    }

    @Test
    fun `test that invoke reads the sync condition once`() = runTest {
        stubSyncs(pausedSync(1), pausedSync(2), pausedSync(3), markedIds = setOf(1, 2, 3))

        underTest()

        verify(monitorShouldSyncUseCase, times(1)).invoke()
    }

    @Test
    fun `test that invoke continues with the next sync when one resume fails`() = runTest {
        stubSyncs(pausedSync(1), pausedSync(2), markedIds = setOf(1, 2))
        whenever(resumeSyncUseCase(1)).thenThrow(RuntimeException())

        underTest()

        verify(setUserPausedSyncUseCase).invoke(2, false)
        verify(resumeSyncUseCase).invoke(2)
    }

    @Test
    fun `test that invoke returns the number of syncs paused by the user`() = runTest {
        stubSyncs(
            folderPair(1, status = SyncStatus.SYNCING),
            pausedSync(2, error = SyncError.STORAGE_OVERQUOTA),
            pausedSync(3),
            pausedSync(4),
            markedIds = setOf(1, 2, 3),
        )

        assertThat(underTest()).isEqualTo(3)
    }

    @Test
    fun `test that invoke completes every write when cancelled midway`() = runTest {
        val gate = CompletableDeferred<Unit>()
        stubSyncs(pausedSync(1), pausedSync(2), markedIds = setOf(1, 2))
        wheneverBlocking { resumeSyncUseCase(1) }.doSuspendableAnswer { gate.await() }

        val job = launch { underTest() }
        runCurrent()
        job.cancel()
        gate.complete(Unit)
        job.join()

        verify(setUserPausedSyncUseCase).invoke(2, false)
        verify(resumeSyncUseCase).invoke(2)
    }

    private suspend fun stubSyncs(vararg syncs: FolderPair, markedIds: Set<Long>) {
        whenever(getFolderPairsUseCase()).thenReturn(syncs.toList())
        markedIds.forEach { whenever(isSyncPausedByTheUserUseCase(it)).thenReturn(true) }
    }

    private fun pausedSync(id: Long, error: SyncError? = null) =
        folderPair(id, status = SyncStatus.PAUSED, error = error)

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
