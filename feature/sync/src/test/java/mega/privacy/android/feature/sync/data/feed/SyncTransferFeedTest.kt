package mega.privacy.android.feature.sync.data.feed

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.data.mapper.progress.SyncTransferMapper
import mega.privacy.android.feature.sync.data.mapper.progress.mockMegaError
import mega.privacy.android.feature.sync.data.mapper.progress.mockSyncMegaTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEvent
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEventType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferUpdate
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncTransferFeedTest {

    private val megaApiGateway = mock<MegaApiGateway>()
    private val syncTransferMapper = mock<SyncTransferMapper>()

    private lateinit var globalTransfers: MutableSharedFlow<GlobalTransfer>
    private lateinit var reseedRequests: MutableSharedFlow<Unit>
    private lateinit var listenerGeneration: MutableStateFlow<Long>

    @BeforeEach
    fun resetMocks() {
        reset(megaApiGateway, syncTransferMapper)
        globalTransfers = MutableSharedFlow(extraBufferCapacity = 64)
        reseedRequests = MutableSharedFlow(extraBufferCapacity = 1)
        whenever(megaApiGateway.globalTransfer).thenReturn(globalTransfers)
        listenerGeneration = MutableStateFlow(1L)
        whenever(megaApiGateway.globalTransferListenerGeneration).thenReturn(listenerGeneration)
        stubSnapshot()
    }

    private fun TestScope.createUnderTest() = SyncTransferFeed(
        megaApiGateway = megaApiGateway,
        syncTransferMapper = syncTransferMapper,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun `test that updates subscribes to transfer events before reading the snapshot`() = runTest {
        var subscriptionsDuringSnapshot = -1
        megaApiGateway.stub {
            onBlocking { getTransfers(MegaTransfer.TYPE_UPLOAD) } doSuspendableAnswer {
                subscriptionsDuringSnapshot = globalTransfers.subscriptionCount.value
                emptyList()
            }
        }

        createUnderTest().updates().test {
            assertThat(awaitItem()).isInstanceOf(SyncTransferUpdate.Seed::class.java)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(subscriptionsDuringSnapshot).isEqualTo(1)
    }

    @Test
    fun `test that updates reads the snapshot only after the transfer listener is registered`() =
        runTest {
            listenerGeneration.value = 0L

            createUnderTest().updates().test {
                expectNoEvents()
                verify(megaApiGateway, never()).getTransfers(any())
                assertThat(globalTransfers.subscriptionCount.value).isEqualTo(1)

                listenerGeneration.value = 1L

                assertThat(awaitItem()).isInstanceOf(SyncTransferUpdate.Seed::class.java)
                verify(megaApiGateway).getTransfers(MegaTransfer.TYPE_UPLOAD)
                verify(megaApiGateway).getTransfers(MegaTransfer.TYPE_DOWNLOAD)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that updates emits the seed before any batch`() = runTest {
        val transfer = syncTransfer(tag = 1, number = 2)
        emitDuringFirstUploadSnapshot(GlobalTransfer.OnTransferUpdate(transfer))

        createUnderTest().updates().test {
            assertThat(awaitItem()).isInstanceOf(SyncTransferUpdate.Seed::class.java)
            assertThat(awaitItem()).isInstanceOf(SyncTransferUpdate.Batch::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that updates delivers events received during the snapshot in the first batch`() =
        runTest {
            val first = syncTransfer(tag = 1, number = 3)
            val second = syncTransfer(tag = 2, number = 1)
            val third = syncTransfer(tag = 3, number = 2)
            emitDuringFirstUploadSnapshot(
                GlobalTransfer.OnTransferUpdate(first),
                GlobalTransfer.OnTransferUpdate(second),
                GlobalTransfer.OnTransferUpdate(third),
            )

            createUnderTest().updates().test {
                awaitItem()
                val batch = awaitItem() as SyncTransferUpdate.Batch
                assertThat(batch.events.map { it.entry.notificationNumber })
                    .containsExactly(1L, 2L, 3L).inOrder()
                assertThat(batch.events.map { it.entry.tag }).containsExactly(2, 3, 1).inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that updates seeds only sync file transfers of both directions`() = runTest {
        val syncUpload = syncTransfer(tag = 1, number = 1)
        val nonSyncUpload = mockSyncMegaTransfer(tag = 2, isSyncTransfer = false)
        val folderUpload = mockSyncMegaTransfer(tag = 3, isFolderTransfer = true)
        val secondSyncUpload = syncTransfer(tag = 6, number = 6)
        val syncDownload = syncTransfer(tag = 4, number = 4)
        val nonSyncDownload = mockSyncMegaTransfer(tag = 5, isSyncTransfer = false)
        stubSnapshot(
            uploads = listOf(syncUpload, nonSyncUpload, folderUpload, secondSyncUpload),
            downloads = listOf(syncDownload, nonSyncDownload),
        )

        createUnderTest().updates().test {
            val seed = awaitItem() as SyncTransferUpdate.Seed
            assertThat(seed.entries.map { it.tag }).containsExactly(1, 6, 4).inOrder()
            cancelAndIgnoreRemainingEvents()
        }

        verify(syncTransferMapper).invoke(syncUpload, false)
        verify(syncTransferMapper).invoke(secondSyncUpload, false)
        verify(syncTransferMapper).invoke(syncDownload, false)
        verify(syncTransferMapper, never()).invoke(any(), eq(true))
        verify(syncTransferMapper, never()).invoke(eq(nonSyncUpload), any())
        verify(syncTransferMapper, never()).invoke(eq(folderUpload), any())
        verify(syncTransferMapper, never()).invoke(eq(nonSyncDownload), any())
    }

    @Test
    fun `test that updates does not map non sync events`() = runTest {
        val transfer = mockSyncMegaTransfer(tag = 1, isSyncTransfer = false)

        createUnderTest().updates().test {
            awaitItem()
            globalTransfers.emit(GlobalTransfer.OnTransferUpdate(transfer))
            advanceTimeBy(CHUNK_MILLIS)
            runCurrent()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        verifyNoInteractions(syncTransferMapper)
    }

    @Test
    fun `test that updates reads only the sync flag of a non sync event`() = runTest {
        val updated = mockSyncMegaTransfer(tag = 1, isSyncTransfer = false)
        val finished = mockSyncMegaTransfer(tag = 2, isSyncTransfer = false)

        createUnderTest().updates().test {
            awaitItem()
            globalTransfers.emit(GlobalTransfer.OnTransferUpdate(updated))
            globalTransfers.emit(
                GlobalTransfer.OnTransferFinish(finished, mockMegaError(MegaError.API_OK))
            )
            advanceTimeBy(10_000)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        verify(updated).isSyncTransfer
        verifyNoMoreInteractions(updated)
        verify(finished).isSyncTransfer
        verifyNoMoreInteractions(finished)
    }

    @Test
    fun `test that updates does not map folder transfer events`() = runTest {
        val transfer = mockSyncMegaTransfer(tag = 1, isFolderTransfer = true)

        createUnderTest().updates().test {
            awaitItem()
            globalTransfers.emit(GlobalTransfer.OnTransferUpdate(transfer))
            advanceTimeBy(CHUNK_MILLIS)
            runCurrent()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        verifyNoInteractions(syncTransferMapper)
    }

    @Test
    fun `test that updates ignores transfer data and quota state events`() = runTest {
        val dataTransfer = mockSyncMegaTransfer(tag = 1)
        val folderTransfer = mockSyncMegaTransfer(tag = 2)
        val quotaTransfer = mockSyncMegaTransfer(tag = 3)

        createUnderTest().updates().test {
            awaitItem()
            globalTransfers.emit(GlobalTransfer.OnTransferData(dataTransfer, null))
            globalTransfers.emit(
                GlobalTransfer.OnFolderTransferUpdate(
                    transfer = folderTransfer,
                    stage = 1,
                    folderCount = 1L,
                    createdFolderCount = 0L,
                    fileCount = 1L,
                    currentFolder = null,
                    currentFileLeafName = null,
                )
            )
            globalTransfers.emit(GlobalTransfer.OnTransferQuotaState(quotaTransfer, 0))
            advanceTimeBy(10_000)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        verifyNoInteractions(dataTransfer, folderTransfer, quotaTransfer, syncTransferMapper)
    }

    @Test
    fun `test that updates fails when the seed read fails`() = runTest {
        megaApiGateway.stub {
            onBlocking { getTransfers(MegaTransfer.TYPE_UPLOAD) } doThrow RuntimeException("boom")
        }

        createUnderTest().updates().test {
            val error = awaitError()
            assertThat(error).isInstanceOf(RuntimeException::class.java)
            assertThat(error.message).isEqualTo("boom")
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(SyncTransferEventType::class)
    fun `test that updates maps each callback to its event type`(type: SyncTransferEventType) =
        runTest {
            val transfer = syncTransfer(tag = 1, number = 1)

            createUnderTest().updates().test {
                awaitItem()
                globalTransfers.emit(callback(type, transfer))
                val batch = awaitItem() as SyncTransferUpdate.Batch
                assertThat(batch.events.single().type).isEqualTo(type)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that updates reads the snapshot once per collection`() = runTest {
        val underTest = createUnderTest()

        underTest.updates().test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        verify(megaApiGateway, times(1)).getTransfers(MegaTransfer.TYPE_UPLOAD)
        verify(megaApiGateway, times(1)).getTransfers(MegaTransfer.TYPE_DOWNLOAD)

        underTest.updates().test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        verify(megaApiGateway, times(2)).getTransfers(MegaTransfer.TYPE_UPLOAD)
        verify(megaApiGateway, times(2)).getTransfers(MegaTransfer.TYPE_DOWNLOAD)
    }

    @Test
    fun `test that updates stops collecting transfer events when cancelled`() = runTest {
        createUnderTest().updates(reseedRequests).test {
            awaitItem()
            assertThat(globalTransfers.subscriptionCount.value).isEqualTo(1)
            assertThat(reseedRequests.subscriptionCount.value).isEqualTo(1)
            assertThat(listenerGeneration.subscriptionCount.value).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
        testScheduler.runCurrent()

        assertThat(globalTransfers.subscriptionCount.value).isEqualTo(0)
        assertThat(reseedRequests.subscriptionCount.value).isEqualTo(0)
        assertThat(listenerGeneration.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `test that updates emits the seed before the first batch without waiting for a chunk`() =
        runTest {
            emitDuringFirstUploadSnapshot(
                GlobalTransfer.OnTransferUpdate(syncTransfer(tag = 1, number = 1))
            )

            createUnderTest().updates().test {
                assertThat(awaitItem()).isInstanceOf(SyncTransferUpdate.Seed::class.java)
                assertThat(testScheduler.currentTime).isEqualTo(0L)
                assertThat(awaitItem()).isInstanceOf(SyncTransferUpdate.Batch::class.java)
                assertThat(testScheduler.currentTime).isEqualTo(CHUNK_MILLIS)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that updates closes a chunk one chunk duration after its first event`() = runTest {
        createUnderTest().updates().test {
            awaitItem()
            globalTransfers.emit(update(tag = 1, number = 1))
            advanceTimeBy(999)
            globalTransfers.emit(update(tag = 2, number = 2))
            advanceTimeBy(2)
            globalTransfers.emit(update(tag = 3, number = 3))

            val first = awaitItem() as SyncTransferUpdate.Batch
            assertThat(first.events.map { it.entry.tag }).containsExactly(1, 2).inOrder()
            val second = awaitItem() as SyncTransferUpdate.Batch
            assertThat(second.events.map { it.entry.tag }).containsExactly(3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that updates emits nothing while no event arrives`() = runTest {
        createUnderTest().updates().test {
            awaitItem()
            advanceTimeBy(10_000)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that updates keeps only the newest event per tag within a chunk`() = runTest {
        createUnderTest().updates().test {
            awaitItem()
            globalTransfers.emit(update(tag = 1, number = 2))
            globalTransfers.emit(update(tag = 1, number = 5))
            globalTransfers.emit(update(tag = 1, number = 3))

            val batch = awaitItem() as SyncTransferUpdate.Batch
            val event = batch.events.single()
            assertThat(event.entry.tag).isEqualTo(1)
            assertThat(event.entry.notificationNumber).isEqualTo(5L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that updates keeps the finish when a chunk also holds updates for that tag`() =
        runTest {
            createUnderTest().updates().test {
                awaitItem()
                globalTransfers.emit(update(tag = 1, number = 3))
                globalTransfers.emit(
                    callback(SyncTransferEventType.Finish, syncTransfer(tag = 1, number = 4))
                )
                globalTransfers.emit(update(tag = 1, number = 5))

                val event = (awaitItem() as SyncTransferUpdate.Batch).events.single()
                assertThat(event.type).isEqualTo(SyncTransferEventType.Finish)
                assertThat(event.entry.notificationNumber).isEqualTo(4L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that updates records the start notification number when a chunk holds a start`() =
        runTest {
            createUnderTest().updates().test {
                awaitItem()
                globalTransfers.emit(
                    callback(SyncTransferEventType.Start, syncTransfer(tag = 1, number = 1))
                )
                globalTransfers.emit(update(tag = 2, number = 2))
                globalTransfers.emit(update(tag = 1, number = 3))

                val events = (awaitItem() as SyncTransferUpdate.Batch).events
                val started = events.single { it.entry.tag == 1 }
                assertThat(started.type).isEqualTo(SyncTransferEventType.Update)
                assertThat(started.entry.notificationNumber).isEqualTo(3L)
                assertThat(started.startNotificationNumber).isEqualTo(1L)
                assertThat(events.single { it.entry.tag == 2 }.startNotificationNumber).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that updates sorts the surviving events by notification number`() = runTest {
        createUnderTest().updates().test {
            awaitItem()
            globalTransfers.emit(
                callback(SyncTransferEventType.Start, syncTransfer(tag = 1, number = 1))
            )
            globalTransfers.emit(
                callback(SyncTransferEventType.Finish, syncTransfer(tag = 2, number = 2))
            )
            globalTransfers.emit(update(tag = 1, number = 3))

            val events = (awaitItem() as SyncTransferUpdate.Batch).events
            assertThat(events).containsExactly(
                SyncTransferEvent(SyncTransferEventType.Finish, entry(tag = 2, number = 2), null),
                SyncTransferEvent(SyncTransferEventType.Update, entry(tag = 1, number = 3), 1L),
            ).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that updates maps only the surviving event of each tag`() = runTest {
        val first = syncTransfer(tag = 1, number = 1)
        val second = syncTransfer(tag = 1, number = 2)
        val survivor = syncTransfer(tag = 1, number = 3)

        createUnderTest().updates().test {
            awaitItem()
            globalTransfers.emit(GlobalTransfer.OnTransferUpdate(first))
            globalTransfers.emit(GlobalTransfer.OnTransferUpdate(second))
            globalTransfers.emit(GlobalTransfer.OnTransferUpdate(survivor))
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify(syncTransferMapper, times(1)).invoke(eq(survivor), any())
        verify(syncTransferMapper, never()).invoke(eq(first), any())
        verify(syncTransferMapper, never()).invoke(eq(second), any())
    }

    @Test
    fun `test that updates reads the path only for finish and terminal events`() = runTest {
        val seeded = syncTransfer(tag = 9, number = 1)
        stubSnapshot(uploads = listOf(seeded))
        val started = syncTransfer(tag = 1, number = 2)
        val updated = syncTransfer(tag = 2, number = 3)
        val retrying = syncTransfer(tag = 3, number = 4)
        val finished = syncTransfer(tag = 4, number = 5)

        createUnderTest().updates().test {
            awaitItem()
            globalTransfers.emit(callback(SyncTransferEventType.Start, started))
            globalTransfers.emit(callback(SyncTransferEventType.Update, updated))
            globalTransfers.emit(callback(SyncTransferEventType.TemporaryError, retrying))
            globalTransfers.emit(callback(SyncTransferEventType.Finish, finished))
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify(syncTransferMapper).invoke(seeded, false)
        verify(syncTransferMapper).invoke(started, false)
        verify(syncTransferMapper).invoke(updated, false)
        verify(syncTransferMapper).invoke(retrying, false)
        verify(syncTransferMapper).invoke(finished, true)
        verify(syncTransferMapper, never()).invoke(eq(finished), eq(false))
    }

    @Test
    fun `test that updates drops an event whose mapping fails and keeps the rest`() = runTest {
        val failing = mockSyncMegaTransfer(tag = 2, notificationNumber = 2)
        whenever(syncTransferMapper(eq(failing), any())).thenThrow(IllegalStateException("bad"))

        createUnderTest().updates().test {
            awaitItem()
            globalTransfers.emit(update(tag = 1, number = 1))
            globalTransfers.emit(GlobalTransfer.OnTransferUpdate(failing))
            globalTransfers.emit(update(tag = 3, number = 3))

            val batch = awaitItem() as SyncTransferUpdate.Batch
            assertThat(batch.events.map { it.entry.tag }).containsExactly(1, 3).inOrder()

            globalTransfers.emit(update(tag = 4, number = 4))
            val next = awaitItem() as SyncTransferUpdate.Batch
            assertThat(next.events.map { it.entry.tag }).containsExactly(4)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that updates does not emit an empty batch`() = runTest {
        whenever(syncTransferMapper(any(), any())).thenThrow(IllegalStateException("bad"))

        createUnderTest().updates().test {
            awaitItem()
            globalTransfers.emit(GlobalTransfer.OnTransferUpdate(mockSyncMegaTransfer(tag = 1)))
            globalTransfers.emit(GlobalTransfer.OnTransferUpdate(mockSyncMegaTransfer(tag = 2)))
            advanceTimeBy(CHUNK_MILLIS)
            runCurrent()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that updates emits a reseed right after the batch that precedes the marker`() =
        runTest {
            val reseeded = syncTransfer(tag = 7, number = 7)
            stubUploadSnapshots(emptyList(), listOf(reseeded))

            createUnderTest().updates(reseedRequests).test {
                awaitItem()
                globalTransfers.emit(update(tag = 1, number = 5))
                advanceTimeBy(100)
                reseedRequests.emit(Unit)

                val batch = awaitItem() as SyncTransferUpdate.Batch
                assertThat(testScheduler.currentTime).isEqualTo(100L)
                assertThat(batch.events.map { it.entry.notificationNumber }).containsExactly(5L)
                val reseed = awaitItem() as SyncTransferUpdate.Reseed
                assertThat(reseed.entries).containsExactly(entry(tag = 7, number = 7))
                assertThat(reseed.watermark).isEqualTo(5L)
                cancelAndIgnoreRemainingEvents()
            }

            verify(megaApiGateway, times(2)).getTransfers(MegaTransfer.TYPE_UPLOAD)
        }

    @Test
    fun `test that updates emits events that arrive during the reseed read after the reseed`() =
        runTest {
            val late = update(tag = 3, number = 10)
            var calls = 0
            megaApiGateway.stub {
                onBlocking { getTransfers(MegaTransfer.TYPE_UPLOAD) } doSuspendableAnswer {
                    if (++calls == 2) globalTransfers.tryEmit(late)
                    emptyList()
                }
            }

            createUnderTest().updates(reseedRequests).test {
                awaitItem()
                reseedRequests.emit(Unit)

                val reseed = awaitItem() as SyncTransferUpdate.Reseed
                val batch = awaitItem() as SyncTransferUpdate.Batch
                val number = batch.events.single().entry.notificationNumber
                assertThat(number).isEqualTo(10L)
                assertThat(number).isGreaterThan(reseed.watermark)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that updates sets the reseed watermark to the highest notification number read before the marker`() =
        runTest {
            stubSnapshot(uploads = listOf(syncTransfer(tag = 1, number = 7)))

            createUnderTest().updates(reseedRequests).test {
                awaitItem()
                reseedRequests.emit(Unit)
                assertThat((awaitItem() as SyncTransferUpdate.Reseed).watermark).isEqualTo(7L)

                globalTransfers.emit(update(tag = 2, number = 8))
                globalTransfers.emit(update(tag = 2, number = 9))
                reseedRequests.emit(Unit)
                val batch = awaitItem() as SyncTransferUpdate.Batch
                assertThat(batch.events.map { it.entry.notificationNumber }).containsExactly(9L)
                assertThat((awaitItem() as SyncTransferUpdate.Reseed).watermark).isEqualTo(9L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that updates requests a reseed when the transfer listener is registered again`() =
        runTest {
            val reseeded = syncTransfer(tag = 4, number = 4)
            stubUploadSnapshots(emptyList(), listOf(reseeded))

            createUnderTest().updates(reseedRequests).test {
                assertThat(awaitItem()).isInstanceOf(SyncTransferUpdate.Seed::class.java)
                expectNoEvents()

                listenerGeneration.value = 2L

                val reseed = awaitItem() as SyncTransferUpdate.Reseed
                assertThat(reseed.entries).containsExactly(entry(tag = 4, number = 4))
                assertThat(reseed.watermark).isEqualTo(0L)
                cancelAndIgnoreRemainingEvents()
            }

            verify(megaApiGateway, times(2)).getTransfers(MegaTransfer.TYPE_UPLOAD)
        }

    @Test
    fun `test that updates fails when a reseed read fails`() = runTest {
        var calls = 0
        megaApiGateway.stub {
            onBlocking { getTransfers(MegaTransfer.TYPE_UPLOAD) } doSuspendableAnswer {
                if (++calls == 2) throw RuntimeException("reseed boom")
                emptyList()
            }
        }

        createUnderTest().updates(reseedRequests).test {
            assertThat(awaitItem()).isInstanceOf(SyncTransferUpdate.Seed::class.java)
            reseedRequests.emit(Unit)
            val error = awaitError()
            assertThat(error).isInstanceOf(RuntimeException::class.java)
            assertThat(error.message).isEqualTo("reseed boom")
        }
    }

    private fun stubSnapshot(
        uploads: List<MegaTransfer> = emptyList(),
        downloads: List<MegaTransfer> = emptyList(),
    ) {
        megaApiGateway.stub {
            onBlocking { getTransfers(MegaTransfer.TYPE_UPLOAD) } doReturn uploads
            onBlocking { getTransfers(MegaTransfer.TYPE_DOWNLOAD) } doReturn downloads
        }
    }

    private fun stubUploadSnapshots(vararg snapshots: List<MegaTransfer>) {
        var calls = 0
        megaApiGateway.stub {
            onBlocking { getTransfers(MegaTransfer.TYPE_UPLOAD) } doSuspendableAnswer {
                snapshots[minOf(calls++, snapshots.lastIndex)]
            }
        }
    }

    private fun emitDuringFirstUploadSnapshot(vararg events: GlobalTransfer) {
        var calls = 0
        megaApiGateway.stub {
            onBlocking { getTransfers(MegaTransfer.TYPE_UPLOAD) } doSuspendableAnswer {
                if (calls++ == 0) events.forEach { globalTransfers.tryEmit(it) }
                emptyList()
            }
        }
    }

    private fun syncTransfer(
        tag: Int,
        number: Long,
        state: TransferState = TransferState.STATE_ACTIVE,
    ): MegaTransfer {
        val transfer = mockSyncMegaTransfer(tag = tag, notificationNumber = number)
        whenever(syncTransferMapper(eq(transfer), any())).thenReturn(entry(tag, number, state))
        return transfer
    }

    private fun update(tag: Int, number: Long) =
        GlobalTransfer.OnTransferUpdate(syncTransfer(tag = tag, number = number))

    private fun callback(type: SyncTransferEventType, transfer: MegaTransfer): GlobalTransfer =
        when (type) {
            SyncTransferEventType.Start -> GlobalTransfer.OnTransferStart(transfer)
            SyncTransferEventType.Update -> GlobalTransfer.OnTransferUpdate(transfer)
            SyncTransferEventType.TemporaryError ->
                GlobalTransfer.OnTransferTemporaryError(transfer, mockMegaError(MegaError.API_EAGAIN))

            SyncTransferEventType.Finish ->
                GlobalTransfer.OnTransferFinish(transfer, mockMegaError(MegaError.API_OK))
        }

    private fun entry(
        tag: Int,
        number: Long,
        state: TransferState = TransferState.STATE_ACTIVE,
    ) = SyncTransferEntry(
        tag = tag,
        direction = SyncTransferDirection.Upload,
        state = state,
        fileName = "photo.jpg",
        totalBytes = 100L,
        transferredBytes = 0L,
        speed = 0L,
        meanSpeed = 0L,
        priority = 0x800000000000L,
        notificationNumber = number,
        lastError = null,
        isForeignOverQuota = false,
        path = null,
    )

    private companion object {
        const val CHUNK_MILLIS = 1_000L
    }
}
