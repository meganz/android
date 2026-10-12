package mega.privacy.android.feature.sync.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.data.feed.SyncTransferFeed
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEvent
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEventType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreCommand
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferUpdate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncTransferStoreRepositoryImplTest {

    private val syncTransferFeed = mock<SyncTransferFeed>()

    private val feeds = mutableListOf<Channel<SyncTransferUpdate>>()
    private val feedStartTimes = mutableListOf<Long>()
    private val received = mutableListOf<SyncTransferStoreCommand>()
    private var activeFeeds = 0
    private var reseedRequestCount = 0

    private val seeded = SyncTransferStoreState.NotSeeded.copy(isSeeded = true)

    @BeforeEach
    fun resetMocks() {
        reset(syncTransferFeed)
        feeds.clear()
        feedStartTimes.clear()
        received.clear()
        activeFeeds = 0
        reseedRequestCount = 0
    }

    @Test
    fun `test that building the repository does not start the feed`() = runTest {
        val underTest = createUnderTest()

        underTest.monitorSyncTransferStore()
        underTest.monitorSyncTransferStoreCommands()
        runCurrent()

        verify(syncTransferFeed, never()).updates(any())
    }

    @Test
    fun `test that monitorSyncTransferStore returns the same StateFlow to every caller`() =
        runTest {
            val underTest = createUnderTest()

            assertThat(underTest.monitorSyncTransferStore())
                .isSameInstanceAs(underTest.monitorSyncTransferStore())
        }

    @Test
    fun `test that monitorSyncTransferStore emits NotSeeded before anything is published`() =
        runTest {
            val underTest = createUnderTest()

            assertThat(underTest.current()).isEqualTo(SyncTransferStoreState.NotSeeded)
        }

    @Test
    fun `test that publishSyncTransferStore publishes a state of the current generation`() =
        runTest {
            val underTest = createUnderTest()

            underTest.publishSyncTransferStore(seeded, generation = 0L)

            assertThat(underTest.current()).isEqualTo(seeded)
        }

    @Test
    fun `test that publishSyncTransferStore drops a state of an older generation`() = runTest {
        val underTest = createUnderTest()
        underTest.reset()

        underTest.publishSyncTransferStore(seeded, generation = 0L)

        assertThat(underTest.current()).isEqualTo(SyncTransferStoreState.NotSeeded)
    }

    @Test
    fun `test that a publish racing a reset never leaves a state of the older generation`() =
        runTest {
            repeat(RACE_ROUNDS) {
                val underTest = createUnderTest()

                withContext(Dispatchers.Default) {
                    coroutineScope {
                        launch { underTest.publishSyncTransferStore(seeded, generation = 0L) }
                        launch { underTest.reset() }
                    }
                }

                assertThat(underTest.current()).isEqualTo(SyncTransferStoreState.NotSeeded)
            }
        }

    @Test
    fun `test that reset publishes NotSeeded at once without a collector`() = runTest {
        val underTest = createUnderTest()
        underTest.publishSyncTransferStore(seeded, generation = 0L)

        underTest.monitorSyncTransferStore().test {
            assertThat(awaitItem()).isEqualTo(seeded)

            underTest.reset()

            assertThat(awaitItem()).isEqualTo(SyncTransferStoreState.NotSeeded)
        }
        verify(syncTransferFeed, never()).updates(any())
    }

    @Test
    fun `test that reset does not start the feed`() = runTest {
        val underTest = createUnderTest()

        underTest.reset()
        underTest.reset()
        runCurrent()

        verify(syncTransferFeed, never()).updates(any())
    }

    @Test
    fun `test that monitorSyncTransferStoreCommands stamps each command with the generation at call time`() =
        runTest {
            val underTest = createUnderTest()
            startCollecting(underTest)

            underTest.clearCompleted(setOf(1))
            underTest.setOptimisticPause(setOf(2), isPaused = true)
            underTest.reset()
            underTest.revertOptimisticPause(2)
            underTest.clearAllCompleted()
            runCurrent()

            assertThat(received).containsExactly(
                SyncTransferStoreCommand(0L, SyncTransferStoreInput.ClearCompleted(setOf(1))),
                SyncTransferStoreCommand(0L, SyncTransferStoreInput.SetOptimisticPause(setOf(2), true)),
                SyncTransferStoreCommand(1L, SyncTransferStoreInput.RevertOptimisticPause(2)),
                SyncTransferStoreCommand(1L, SyncTransferStoreInput.ClearAllCompleted),
            ).inOrder()
        }

    @Test
    fun `test that monitorSyncTransferStoreCommands drops a command sent before a reset`() =
        runTest {
            val underTest = createUnderTest(StandardTestDispatcher(testScheduler))
            startCollecting(underTest)

            underTest.setOptimisticPause(setOf(1), isPaused = true)
            underTest.reset()
            runCurrent()

            assertThat(received.map { it.input })
                .doesNotContain(SyncTransferStoreInput.SetOptimisticPause(setOf(1), true))
        }

    @Test
    fun `test that monitorSyncTransferStoreCommands delivers commands sent before the collection`() =
        runTest {
            val underTest = createUnderTest()
            underTest.clearCompleted(setOf(1))
            underTest.clearAllCompleted()

            startCollecting(underTest)

            assertThat(received).containsExactly(
                SyncTransferStoreCommand(0L, SyncTransferStoreInput.ClearCompleted(setOf(1))),
                SyncTransferStoreCommand(0L, SyncTransferStoreInput.ClearAllCompleted),
            ).inOrder()
        }

    @Test
    fun `test that monitorSyncTransferStoreCommands forwards feed updates stamped with the generation`() =
        runTest {
            val underTest = createUnderTest()
            startCollecting(underTest)

            sendFeed(seed(entry(1)))
            sendFeed(batch(start(entry(2, n = 2L))))

            assertThat(received).containsExactly(
                feedCommand(0L, seed(entry(1))),
                feedCommand(0L, batch(start(entry(2, n = 2L)))),
            ).inOrder()
        }

    @Test
    fun `test that monitorSyncTransferStoreCommands collects the feed again for each generation`() =
        runTest {
            val underTest = createUnderTest()
            startCollecting(underTest)
            verify(syncTransferFeed, times(1)).updates(any())

            underTest.reset()
            runCurrent()
            sendFeed(seed(entry(4)), index = 1)

            verify(syncTransferFeed, times(2)).updates(any())
            assertThat(activeFeeds).isEqualTo(1)
            assertThat(received.last()).isEqualTo(feedCommand(1L, seed(entry(4))))
        }

    @Test
    fun `test that monitorSyncTransferStoreCommands stops forwarding the old feed after a reset`() =
        runTest {
            val underTest = createUnderTest(StandardTestDispatcher(testScheduler))
            startCollecting(underTest)

            feeds[0].send(batch(start(entry(3, n = 2L))))
            underTest.reset()
            runCurrent()
            sendFeed(seed(), index = 1)

            assertThat(received).containsExactly(feedCommand(1L, seed()))
        }

    @Test
    fun `test that monitorSyncTransferStoreCommands collects a failed feed again in the same generation`() =
        runTest {
            val underTest = createUnderTest()
            startCollecting(underTest)
            sendFeed(seed(entry(1)))

            feeds[0].close(IOException())
            runCurrent()
            sendFeed(seed(entry(3)), index = 1)

            assertThat(feeds).hasSize(2)
            verify(syncTransferFeed, times(1)).updates(any())
            assertThat(received.last()).isEqualTo(feedCommand(0L, seed(entry(3))))
        }

    @Test
    fun `test that the feed retry waits longer after each consecutive failure`() = runTest {
        val underTest = createUnderTest(StandardTestDispatcher(testScheduler))
        startCollecting(underTest)
        val firstFailureAt = testScheduler.currentTime

        repeat(3) { attempt -> failFeedAndAwaitRetry(attempt) }

        assertThat(feedStartTimes.drop(1)).containsExactly(
            firstFailureAt,
            firstFailureAt + 100L,
            firstFailureAt + 300L,
        ).inOrder()
    }

    @Test
    fun `test that the retry delay resets after a successful update`() = runTest {
        val underTest = createUnderTest(StandardTestDispatcher(testScheduler))
        startCollecting(underTest)
        failFeedAndAwaitRetry(0)
        failFeedAndAwaitRetry(1)
        assertThat(feeds).hasSize(3)

        feeds[2].send(seed(entry(1)))
        runCurrent()
        val failedAt = testScheduler.currentTime
        feeds[2].close(IOException())
        runCurrent()

        assertThat(feeds).hasSize(4)
        assertThat(feedStartTimes[3]).isEqualTo(failedAt)
    }

    @Test
    fun `test that reset during a retry wait starts one new feed`() = runTest {
        val underTest = createUnderTest(StandardTestDispatcher(testScheduler))
        startCollecting(underTest)

        feeds[0].close(IOException())
        runCurrent()
        feeds[1].close(IOException())
        runCurrent()
        advanceTimeBy(50L)
        underTest.reset()
        advanceTimeBy(1_000L)
        runCurrent()

        verify(syncTransferFeed, times(2)).updates(any())
        assertThat(feeds).hasSize(3)
    }

    @Test
    fun `test that cancelling the collection stops the feed without a retry`() = runTest {
        val underTest = createUnderTest()
        val collection = startCollecting(underTest)
        assertThat(activeFeeds).isEqualTo(1)

        collection.cancel()
        advanceTimeBy(1_000L)
        runCurrent()

        assertThat(activeFeeds).isEqualTo(0)
        assertThat(feeds).hasSize(1)
    }

    @Test
    fun `test that requestReseed reaches the feed`() = runTest {
        val underTest = createUnderTest()
        startCollecting(underTest)

        underTest.requestReseed()
        runCurrent()

        assertThat(reseedRequestCount).isEqualTo(1)
    }

    @Test
    fun `test that requestReseed is dropped when no feed collects`() = runTest {
        val underTest = createUnderTest()

        underTest.requestReseed()
        startCollecting(underTest)

        assertThat(reseedRequestCount).isEqualTo(0)
    }

    private fun TestScope.createUnderTest(
        dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
    ): SyncTransferStoreRepositoryImpl {
        whenever(syncTransferFeed.updates(any())).thenAnswer { invocation ->
            val requests = invocation.getArgument<Flow<Unit>>(0)
            channelFlow {
                val channel = Channel<SyncTransferUpdate>(Channel.UNLIMITED).also {
                    feeds += it
                    feedStartTimes += testScheduler.currentTime
                }
                activeFeeds++
                try {
                    launch { requests.collect { reseedRequestCount++ } }
                    for (update in channel) send(update)
                } finally {
                    activeFeeds--
                }
            }
        }
        return SyncTransferStoreRepositoryImpl(
            syncTransferFeed = syncTransferFeed,
            defaultDispatcher = dispatcher,
        )
    }

    private fun TestScope.startCollecting(underTest: SyncTransferStoreRepositoryImpl): Job {
        val job = backgroundScope.launch {
            underTest.monitorSyncTransferStoreCommands().toList(received)
        }
        runCurrent()
        return job
    }

    // Steps 1 ms at a time, so the failure happens the moment the retried collection starts.
    private fun TestScope.failFeedAndAwaitRetry(index: Int) {
        feeds[index].close(IOException())
        runCurrent()
        repeat(MAX_RETRY_WAIT_MILLIS) {
            if (feeds.size > index + 1) return
            advanceTimeBy(1L)
            runCurrent()
        }
    }

    private suspend fun TestScope.sendFeed(update: SyncTransferUpdate, index: Int = feeds.lastIndex) {
        feeds[index].send(update)
        runCurrent()
    }

    private suspend fun SyncTransferStoreRepositoryImpl.current(): SyncTransferStoreState =
        monitorSyncTransferStore().first()

    private fun feedCommand(generation: Long, update: SyncTransferUpdate) =
        SyncTransferStoreCommand(generation, SyncTransferStoreInput.Feed(update))

    private fun seed(vararg entries: SyncTransferEntry): SyncTransferUpdate =
        SyncTransferUpdate.Seed(entries.toList())

    private fun batch(vararg events: SyncTransferEvent): SyncTransferUpdate =
        SyncTransferUpdate.Batch(events.toList())

    private fun start(entry: SyncTransferEntry): SyncTransferEvent =
        SyncTransferEvent(SyncTransferEventType.Start, entry, null)

    private fun entry(tag: Int, n: Long = 1L): SyncTransferEntry = SyncTransferEntry(
        tag = tag,
        direction = SyncTransferDirection.Upload,
        state = TransferState.STATE_QUEUED,
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

    companion object {
        private const val MAX_RETRY_WAIT_MILLIS = 1_000
        private const val RACE_ROUNDS = 1_000
    }
}
