package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransferByTagUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferOverflowEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PauseSyncTransferUseCaseTest {

    private lateinit var underTest: PauseSyncTransferUseCase

    private val syncTransferStoreRepository = mock<SyncTransferStoreRepository>()
    private val pauseTransferByTagUseCase = mock<PauseTransferByTagUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = PauseSyncTransferUseCase(
            syncTransferStoreRepository = syncTransferStoreRepository,
            pauseTransferByTagUseCase = pauseTransferByTagUseCase,
            getSyncTransferRowStateUseCase = GetSyncTransferRowStateUseCase(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncTransferStoreRepository, pauseTransferByTagUseCase)
    }

    @Test
    fun `test that invoke pauses a queued row by tag with an optimistic state`() = runTest {
        givenStore(storeOf(row(1, TransferState.STATE_QUEUED)))
        givenRequestEchoes()

        val result = underTest(tag = 1, isPause = true)

        assertThat(result).isTrue()
        inOrder(syncTransferStoreRepository, pauseTransferByTagUseCase) {
            verify(syncTransferStoreRepository).setOptimisticPause(setOf(1), true)
            verify(pauseTransferByTagUseCase).invoke(1, true)
        }
    }

    @ParameterizedTest
    @EnumSource(value = TransferState::class, names = ["STATE_ACTIVE", "STATE_RETRYING"])
    fun `test that invoke pauses an active or retrying row`(state: TransferState) = runTest {
        givenStore(storeOf(row(1, state)))
        givenRequestEchoes()

        val result = underTest(tag = 1, isPause = true)

        assertThat(result).isTrue()
        verify(pauseTransferByTagUseCase).invoke(1, true)
    }

    @Test
    fun `test that invoke does nothing for a completing row`() = runTest {
        givenStore(storeOf(row(1, TransferState.STATE_COMPLETING)))

        val result = underTest(tag = 1, isPause = true)

        assertThat(result).isFalse()
        verify(syncTransferStoreRepository, never()).setOptimisticPause(any(), any())
        verifyNoInteractions(pauseTransferByTagUseCase)
    }

    @Test
    fun `test that invoke does nothing for a row in the none state`() = runTest {
        givenStore(storeOf(row(1, TransferState.STATE_NONE)))

        val result = underTest(tag = 1, isPause = true)

        assertThat(result).isFalse()
        verifyNoInteractions(pauseTransferByTagUseCase)
    }

    @Test
    fun `test that invoke does not pause a row that is already paused`() = runTest {
        givenStore(
            storeOf(
                row(1, TransferState.STATE_PAUSED),
                row(2, TransferState.STATE_QUEUED, optimisticPause = true),
            )
        )

        assertThat(underTest(tag = 1, isPause = true)).isFalse()
        assertThat(underTest(tag = 2, isPause = true)).isFalse()
        verify(syncTransferStoreRepository, never()).setOptimisticPause(any(), any())
        verifyNoInteractions(pauseTransferByTagUseCase)
    }

    @Test
    fun `test that invoke pauses a row whose resume is not confirmed yet`() = runTest {
        givenStore(storeOf(row(1, TransferState.STATE_PAUSED, optimisticPause = false)))
        givenRequestEchoes()

        val result = underTest(tag = 1, isPause = true)

        assertThat(result).isTrue()
        verify(syncTransferStoreRepository).setOptimisticPause(setOf(1), true)
        verify(pauseTransferByTagUseCase).invoke(1, true)
    }

    @Test
    fun `test that invoke returns false for a tag the store does not list`() = runTest {
        givenStore(
            storeOf(
                overflow = persistentHashMapOf(
                    1 to SyncTransferOverflowEntry(
                        direction = SyncTransferDirection.Upload,
                        state = TransferState.STATE_QUEUED,
                    )
                ),
            )
        )

        val result = underTest(tag = 1, isPause = true)

        assertThat(result).isFalse()
        verifyNoInteractions(pauseTransferByTagUseCase)
    }

    @Test
    fun `test that invoke resumes a paused row by tag with an optimistic state`() = runTest {
        givenStore(storeOf(row(1, TransferState.STATE_PAUSED)))
        givenRequestEchoes()

        val result = underTest(tag = 1, isPause = false)

        assertThat(result).isTrue()
        inOrder(syncTransferStoreRepository, pauseTransferByTagUseCase) {
            verify(syncTransferStoreRepository).setOptimisticPause(setOf(1), false)
            verify(pauseTransferByTagUseCase).invoke(1, false)
        }
    }

    @Test
    fun `test that invoke does not resume a row that is not paused`() = runTest {
        givenStore(
            storeOf(
                row(1, TransferState.STATE_QUEUED),
                row(2, TransferState.STATE_PAUSED, optimisticPause = false),
            )
        )

        assertThat(underTest(tag = 1, isPause = false)).isFalse()
        assertThat(underTest(tag = 2, isPause = false)).isFalse()
        verify(syncTransferStoreRepository, never()).setOptimisticPause(any(), any())
        verifyNoInteractions(pauseTransferByTagUseCase)
    }

    @Test
    fun `test that invoke keeps the optimistic resume when the request succeeds`() = runTest {
        givenStore(storeOf(row(1, TransferState.STATE_PAUSED)))
        whenever(pauseTransferByTagUseCase(1, false)).thenReturn(false)

        val result = underTest(tag = 1, isPause = false)

        assertThat(result).isTrue()
        verify(syncTransferStoreRepository, never()).revertOptimisticPause(any())
    }

    @Test
    fun `test that invoke reverts the optimistic state when the request throws`() = runTest {
        givenStore(storeOf(row(1, TransferState.STATE_QUEUED)))
        whenever(pauseTransferByTagUseCase(1, true)).doSuspendableAnswer { throw requestFailure() }

        val result = underTest(tag = 1, isPause = true)

        assertThat(result).isFalse()
        verify(syncTransferStoreRepository).revertOptimisticPause(1)
    }

    @Test
    fun `test that invoke reverts and rethrows a cancellation raised by the request`() = runTest {
        givenStore(storeOf(row(1, TransferState.STATE_QUEUED)))
        whenever(pauseTransferByTagUseCase(1, true))
            .doSuspendableAnswer { throw CancellationException("request cancelled") }

        val thrown = try {
            underTest(tag = 1, isPause = true)
            null
        } catch (e: CancellationException) {
            e
        }

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
        verify(syncTransferStoreRepository).revertOptimisticPause(1)
    }

    @Test
    fun `test that invoke resumes a row that is paused only optimistically`() = runTest {
        givenStore(storeOf(row(1, TransferState.STATE_ACTIVE, optimisticPause = true)))
        givenRequestEchoes()

        val result = underTest(tag = 1, isPause = false)

        assertThat(result).isTrue()
        verify(syncTransferStoreRepository).setOptimisticPause(setOf(1), false)
        verify(pauseTransferByTagUseCase).invoke(1, false)
    }

    @Test
    fun `test that invoke resumes only the tapped row`() = runTest {
        givenStore(
            storeOf(
                row(1, TransferState.STATE_PAUSED),
                row(2, TransferState.STATE_PAUSED),
            )
        )
        givenRequestEchoes()

        underTest(tag = 1, isPause = false)

        verify(pauseTransferByTagUseCase).invoke(1, false)
        verify(pauseTransferByTagUseCase, never()).invoke(eq(2), any())
        verify(syncTransferStoreRepository, times(1)).setOptimisticPause(any(), any())
        verify(syncTransferStoreRepository).setOptimisticPause(setOf(1), false)
    }

    @Test
    fun `test that a row resume never changes a sync run state`() = runTest {
        givenStore(storeOf(row(1, TransferState.STATE_PAUSED)))
        givenRequestEchoes()

        underTest(tag = 1, isPause = false)

        verify(syncTransferStoreRepository).monitorSyncTransferStore()
        verify(syncTransferStoreRepository).setOptimisticPause(setOf(1), false)
        verify(pauseTransferByTagUseCase).invoke(1, false)
        verifyNoMoreInteractions(syncTransferStoreRepository, pauseTransferByTagUseCase)
        val parameterTypes = PauseSyncTransferUseCase::class.java.declaredConstructors
            .single()
            .parameterTypes
            .toList()
        assertThat(parameterTypes).containsExactly(
            SyncTransferStoreRepository::class.java,
            PauseTransferByTagUseCase::class.java,
            GetSyncTransferRowStateUseCase::class.java,
        ).inOrder()
    }

    @Test
    fun `test that invoke finishes the request and the revert when the caller is cancelled`() =
        runTest {
            givenStore(storeOf(row(1, TransferState.STATE_QUEUED)))
            val gate = CompletableDeferred<Boolean>()
            whenever(pauseTransferByTagUseCase(1, true)).doSuspendableAnswer { gate.await() }

            val caller = launch { underTest(tag = 1, isPause = true) }
            runCurrent()
            caller.cancel()
            runCurrent()
            verify(syncTransferStoreRepository, never()).revertOptimisticPause(any())

            gate.completeExceptionally(requestFailure())
            runCurrent()

            verify(syncTransferStoreRepository).revertOptimisticPause(1)
            assertThat(caller.isCancelled).isTrue()
        }

    private fun requestFailure() = MegaException(
        errorCode = -9,
        errorString = "API_ENOENT",
        methodName = "pauseTransferByTag",
    )

    private suspend fun givenRequestEchoes() {
        whenever(pauseTransferByTagUseCase(any(), any())).thenAnswer { it.arguments[1] }
    }

    private fun givenStore(state: SyncTransferStoreState) {
        whenever(syncTransferStoreRepository.monitorSyncTransferStore())
            .thenReturn(MutableStateFlow(state))
    }

    private fun storeOf(
        vararg rows: SyncTransferRow,
        overflow: ImmutableMap<Int, SyncTransferOverflowEntry> = persistentHashMapOf(),
    ) = SyncTransferStoreState.NotSeeded.copy(
        isSeeded = true,
        active = rows.associateBy { it.entry.tag }.toPersistentHashMap(),
        overflow = overflow,
    )

    private fun row(
        tag: Int,
        state: TransferState,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
        optimisticPause: Boolean? = null,
    ) = SyncTransferRow(
        entry = SyncTransferEntry(
            tag = tag,
            direction = direction,
            state = state,
            fileName = "photo.jpg",
            totalBytes = 100,
            transferredBytes = 0,
            speed = 0,
            meanSpeed = 0,
            priority = tag.toLong(),
            notificationNumber = 1,
            lastError = null,
            isForeignOverQuota = false,
            path = null,
        ),
        mediaType = SyncMediaType.Other,
        optimisticPause = optimisticPause,
    )
}
