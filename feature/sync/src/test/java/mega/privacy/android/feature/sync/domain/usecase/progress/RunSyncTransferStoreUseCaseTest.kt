package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEvent
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEventType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreCommand
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTally
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferUpdate
import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.io.IOException
import kotlin.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RunSyncTransferStoreUseCaseTest {
    private lateinit var underTest: RunSyncTransferStoreUseCase

    private val syncTransferStoreRepository = mock<SyncTransferStoreRepository>()
    private val getFileTypeInfoByNameUseCase = mock<GetFileTypeInfoByNameUseCase>()

    private val published = mutableListOf<Published>()
    private var clock: () -> Long = { 0L }

    @BeforeAll
    fun setUp() {
        underTest = createUnderTest(createApplySyncTransferStoreInputUseCase())
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncTransferStoreRepository, getFileTypeInfoByNameUseCase)
        whenever(getFileTypeInfoByNameUseCase(any(), any())).thenAnswer { invocation ->
            fileTypeInfoFor(invocation.getArgument<String>(0))
        }
        published.clear()
        clock = { 0L }
        syncTransferStoreRepository.stub {
            onBlocking { publishSyncTransferStore(any(), any()) } doAnswer { invocation ->
                published += Published(invocation.getArgument(0), invocation.getArgument(1))
                Unit
            }
        }
    }

    @Test
    fun `test that invoke folds each command into the store with the reducer`() = runTest {
        stubCommands(
            command(0L, seed(entry(1), entry(2))),
            command(0L, batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L)))),
        )

        underTest()

        val state = published.last().state
        assertThat(state.isSeeded).isTrue()
        assertThat(state.active.keys).containsExactly(2)
        assertThat(state.completed.map { it.entry.tag }).containsExactly(1)
        assertThat(state.counts.completedTransfers).isEqualTo(1)
    }

    @Test
    fun `test that invoke publishes each folded state with the generation of its command`() =
        runTest {
            stubCommands(
                command(2L, seed(entry(1))),
                SyncTransferStoreCommand(2L, SyncTransferStoreInput.SetOptimisticPause(setOf(1), true)),
            )

            underTest()

            assertThat(published.map { it.generation }).containsExactly(2L, 2L)
            assertThat(published[0].state.active.getValue(1).optimisticPause).isNull()
            assertThat(published[1].state.active.getValue(1).optimisticPause).isTrue()
        }

    @Test
    fun `test that invoke applies commands and feed updates in arrival order`() = runTest {
        stubCommands(
            command(0L, seed(entry(1), entry(2))),
            command(0L, batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L)))),
            SyncTransferStoreCommand(0L, SyncTransferStoreInput.ClearCompleted(setOf(1))),
            command(0L, batch(finish(entry(2, TransferState.STATE_COMPLETED, n = 3L)))),
        )

        underTest()

        assertThat(published.last().state.completed.map { it.entry.tag }).containsExactly(2)
    }

    @Test
    fun `test that invoke removes every completed row on clear all`() = runTest {
        stubCommands(
            command(0L, seed(entry(1), entry(2))),
            command(
                0L,
                batch(
                    finish(entry(1, TransferState.STATE_COMPLETED, n = 2L)),
                    finish(entry(2, TransferState.STATE_COMPLETED, n = 3L)),
                ),
            ),
            SyncTransferStoreCommand(0L, SyncTransferStoreInput.ClearAllCompleted),
        )

        underTest()

        assertThat(published[1].state.completed).hasSize(2)
        assertThat(published.last().state.completed).isEmpty()
    }

    @Test
    fun `test that invoke reverts an optimistic pause`() = runTest {
        stubCommands(
            command(0L, seed(entry(1, TransferState.STATE_ACTIVE))),
            SyncTransferStoreCommand(0L, SyncTransferStoreInput.SetOptimisticPause(setOf(1), true)),
            SyncTransferStoreCommand(0L, SyncTransferStoreInput.RevertOptimisticPause(1)),
        )

        underTest()

        assertThat(published[1].state.active.getValue(1).optimisticPause).isTrue()
        assertThat(published.last().state.active.getValue(1).optimisticPause).isNull()
    }

    @Test
    fun `test that invoke stamps completed rows with the clock`() = runTest {
        clock = { 1_234L }
        stubCommands(
            command(0L, seed(entry(1))),
            command(0L, batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L)))),
        )

        underTest()

        assertThat(published.last().state.completed[0].finishedAtMillis).isEqualTo(1_234L)
    }

    @Test
    fun `test that invoke restarts from the reset state when the generation changes`() = runTest {
        stubCommands(
            command(0L, seed(entry(1), entry(2))),
            command(0L, batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L)))),
            command(1L, seed(entry(4))),
        )

        underTest()

        val last = published.last()
        assertThat(last.generation).isEqualTo(1L)
        assertThat(last.state.isSeeded).isTrue()
        assertThat(last.state.active.keys).containsExactly(4)
        assertThat(last.state.completed).isEmpty()
        assertThat(last.state.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 1, pending = 1))
        assertThat(last.state.counts.pendingSyncTransfers).isEqualTo(1)
    }

    @Test
    fun `test that invoke folds the first command of a new generation into the reset state`() =
        runTest {
            stubCommands(
                command(0L, seed(entry(1, TransferState.STATE_ACTIVE))),
                SyncTransferStoreCommand(1L, SyncTransferStoreInput.SetOptimisticPause(setOf(1), true)),
                command(1L, seed(entry(1, TransferState.STATE_ACTIVE))),
            )

            underTest()

            assertThat(published.last().state.active.getValue(1).optimisticPause).isNull()
        }

    @Test
    fun `test that invoke drops a command of an older generation`() = runTest {
        stubCommands(
            command(1L, seed(entry(1))),
            command(0L, batch(start(entry(2, n = 2L)))),
        )

        underTest()

        assertThat(published).hasSize(1)
        assertThat(published.single().state.active.keys).containsExactly(1)
    }

    @Test
    fun `test that invoke keeps completed rows when the feed seeds again in the same generation`() =
        runTest {
            stubCommands(
                command(0L, seed(entry(1), entry(2))),
                command(0L, batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L)))),
                command(0L, seed(entry(3))),
            )

            underTest()

            val state = published.last().state
            assertThat(state.completed.map { it.entry.tag }).containsExactly(1)
            assertThat(state.active).containsKey(3)
            assertThat(state.counts.completedTransfers).isEqualTo(1)
        }

    @Test
    fun `test that invoke skips an input whose clock read throws and keeps the last state`() =
        runTest {
            var calls = 0
            clock = {
                calls++
                if (calls == 2) throw IllegalStateException("clock failed")
                0L
            }
            stubCommands(
                command(0L, seed(entry(1))),
                command(0L, batch(start(entry(2, n = 2L)))),
                command(0L, batch(start(entry(3, n = 3L)))),
            )

            underTest()

            assertThat(published).hasSize(2)
            assertThat(published.last().state.active.keys).containsExactly(1, 3)
        }

    @Test
    fun `test that invoke skips an input the reducer fails on`() = runTest {
        val applySyncTransferStoreInputUseCase = mock<ApplySyncTransferStoreInputUseCase>()
        val folded = SyncTransferStoreState.NotSeeded.copy(isSeeded = true)
        whenever(applySyncTransferStoreInputUseCase(any(), any(), any(), any(), any()))
            .thenThrow(IllegalStateException("reducer failed"))
            .thenReturn(folded)
        stubCommands(command(0L, seed(entry(1))), command(0L, seed(entry(2))))

        createUnderTest(applySyncTransferStoreInputUseCase)()

        assertThat(published).containsExactly(Published(folded, 0L))
    }

    @Test
    fun `test that invoke rethrows a cancellation from the fold`() = runTest {
        clock = { throw CancellationException("cancelled") }
        stubCommands(command(0L, seed(entry(1))))

        val actual = runCatching { underTest() }.exceptionOrNull()

        assertThat(actual).isInstanceOf(CancellationException::class.java)
        assertThat(published).isEmpty()
    }

    @Test
    fun `test that invoke propagates a failure of the commands flow`() = runTest {
        whenever(syncTransferStoreRepository.monitorSyncTransferStoreCommands()).thenReturn(
            flow {
                emit(command(0L, seed(entry(1))))
                throw IOException("commands failed")
            }
        )

        val actual = runCatching { underTest() }.exceptionOrNull()

        assertThat(actual).isInstanceOf(IOException::class.java)
        assertThat(published).hasSize(1)
    }

    @Test
    fun `test that invoke stops when it is cancelled`() = runTest {
        whenever(syncTransferStoreRepository.monitorSyncTransferStoreCommands()).thenReturn(
            flow {
                emit(command(0L, seed(entry(1))))
                awaitCancellation()
            }
        )
        val job = launch { underTest() }
        runCurrent()

        job.cancel()
        runCurrent()

        assertThat(job.isCancelled).isTrue()
        assertThat(job.isCompleted).isTrue()
        assertThat(published).hasSize(1)
    }

    private fun createUnderTest(applySyncTransferStoreInputUseCase: ApplySyncTransferStoreInputUseCase) =
        RunSyncTransferStoreUseCase(
            syncTransferStoreRepository = syncTransferStoreRepository,
            applySyncTransferStoreInputUseCase = applySyncTransferStoreInputUseCase,
            currentTimeProvider = { clock() },
        )

    private fun stubCommands(vararg commands: SyncTransferStoreCommand) {
        whenever(syncTransferStoreRepository.monitorSyncTransferStoreCommands())
            .thenReturn(flowOf(*commands))
    }

    private fun createApplySyncTransferStoreInputUseCase(): ApplySyncTransferStoreInputUseCase {
        val mergeSyncTransferRowUseCase = MergeSyncTransferRowUseCase()
        return ApplySyncTransferStoreInputUseCase(
            getSyncMediaTypeUseCase = GetSyncMediaTypeUseCase(getFileTypeInfoByNameUseCase),
            placeSyncTransferUseCase = PlaceSyncTransferUseCase(),
            orderSyncTransferSeedUseCase = OrderSyncTransferSeedUseCase(),
            decideSyncTransferEventUseCase = DecideSyncTransferEventUseCase(mergeSyncTransferRowUseCase),
            decideSyncReseedEntryUseCase = DecideSyncReseedEntryUseCase(mergeSyncTransferRowUseCase),
            decideSyncStaleTrackedRowUseCase = DecideSyncStaleTrackedRowUseCase(),
            decideOptimisticPauseUseCase = DecideOptimisticPauseUseCase(),
            planSyncTransferClearUseCase = PlanSyncTransferClearUseCase(),
            settleSyncTransferCompletionsUseCase =
                SettleSyncTransferCompletionsUseCase(ApplySyncTransferCountersUseCase()),
        )
    }

    private fun fileTypeInfoFor(fileName: String): FileTypeInfo {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return when (extension) {
            "jpg" -> StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg")
            "mp4" -> VideoFileTypeInfo(mimeType = "video/mp4", extension = "mp4", duration = Duration.ZERO)
            "mp3" -> AudioFileTypeInfo(mimeType = "audio/mpeg", extension = "mp3", duration = Duration.ZERO)
            "pdf" -> PdfFileTypeInfo
            "zip" -> ZipFileTypeInfo(mimeType = "application/zip", extension = "zip")
            else -> UnknownFileTypeInfo(mimeType = "application/octet-stream", extension = extension)
        }
    }

    private fun command(generation: Long, update: SyncTransferUpdate) =
        SyncTransferStoreCommand(generation, SyncTransferStoreInput.Feed(update))

    private fun seed(vararg entries: SyncTransferEntry): SyncTransferUpdate =
        SyncTransferUpdate.Seed(entries.toList())

    private fun batch(vararg events: SyncTransferEvent): SyncTransferUpdate =
        SyncTransferUpdate.Batch(events.toList())

    private fun start(entry: SyncTransferEntry): SyncTransferEvent =
        SyncTransferEvent(SyncTransferEventType.Start, entry, null)

    private fun finish(entry: SyncTransferEntry): SyncTransferEvent = SyncTransferEvent(
        SyncTransferEventType.Finish,
        entry.copy(path = entry.path ?: "/sync/f${entry.tag}.jpg"),
        null,
    )

    private fun entry(
        tag: Int,
        state: TransferState = TransferState.STATE_QUEUED,
        n: Long = 1L,
    ): SyncTransferEntry = SyncTransferEntry(
        tag = tag,
        direction = SyncTransferDirection.Upload,
        state = state,
        fileName = "f$tag.jpg",
        totalBytes = 100L,
        transferredBytes = 0L,
        speed = 0L,
        meanSpeed = 0L,
        priority = tag.toLong(),
        notificationNumber = n,
        lastError = null,
        isForeignOverQuota = false,
        path = null,
    )

    private data class Published(val state: SyncTransferStoreState, val generation: Long)
}
