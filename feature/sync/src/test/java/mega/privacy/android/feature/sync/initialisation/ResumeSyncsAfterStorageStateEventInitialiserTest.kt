package mega.privacy.android.feature.sync.initialisation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncsSuspendedByStorageOverquotaUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResumeSyncsAfterStorageStateEventInitialiserTest {

    private lateinit var underTest: ResumeSyncsAfterStorageStateEventInitialiser

    private val resumeSyncsSuspendedByStorageOverquotaUseCase =
        mock<ResumeSyncsSuspendedByStorageOverquotaUseCase>()
    private val monitorMyAccountUpdateFakeFlow = MutableSharedFlow<MyAccountUpdate>()

    @BeforeAll
    fun setUp() {
        val monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase = mock {
            whenever(it()).thenReturn(monitorMyAccountUpdateFakeFlow)
        }
        underTest = ResumeSyncsAfterStorageStateEventInitialiser(
            monitorMyAccountUpdateUseCase = monitorMyAccountUpdateUseCase,
            resumeSyncsSuspendedByStorageOverquotaUseCase = resumeSyncsSuspendedByStorageOverquotaUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(resumeSyncsSuspendedByStorageOverquotaUseCase)
    }

    @ParameterizedTest
    @EnumSource(StorageState::class, names = ["Green", "Orange"])
    fun `test that syncs are resumed when storage state is the desired`(
        storageState: StorageState,
    ) = runTest {
        val job = launch {
            underTest("test-session", false)
        }
        advanceUntilIdle()

        monitorMyAccountUpdateFakeFlow.emit(
            MyAccountUpdate(
                action = MyAccountUpdate.Action.STORAGE_STATE_CHANGED,
                storageState = storageState,
            )
        )
        advanceUntilIdle()

        verify(resumeSyncsSuspendedByStorageOverquotaUseCase).invoke()
        job.cancel()
    }

    @ParameterizedTest
    @EnumSource(StorageState::class, names = ["Unknown", "Red", "Change", "PayWall"])
    fun `test that syncs are not resumed when storage state is not the desired`(
        storageState: StorageState,
    ) = runTest {
        val job = launch {
            underTest("test-session", false)
        }
        advanceUntilIdle()

        monitorMyAccountUpdateFakeFlow.emit(
            MyAccountUpdate(
                action = MyAccountUpdate.Action.STORAGE_STATE_CHANGED,
                storageState = storageState,
            )
        )
        advanceUntilIdle()

        verifyNoInteractions(resumeSyncsSuspendedByStorageOverquotaUseCase)
        job.cancel()
    }

    @Test
    fun `test that syncs are resumed only once when the same storage state is emitted consecutively`() =
        runTest {
            val job = launch {
                underTest("test-session", false)
            }
            advanceUntilIdle()

            repeat(3) {
                monitorMyAccountUpdateFakeFlow.emit(
                    MyAccountUpdate(
                        action = MyAccountUpdate.Action.STORAGE_STATE_CHANGED,
                        storageState = StorageState.Green,
                    )
                )
            }
            advanceUntilIdle()

            verify(resumeSyncsSuspendedByStorageOverquotaUseCase, times(1)).invoke()
            job.cancel()
        }

    @Test
    fun `test that syncs are resumed again when the storage state changes between desired states`() =
        runTest {
            val job = launch {
                underTest("test-session", false)
            }
            advanceUntilIdle()

            listOf(StorageState.Green, StorageState.Orange, StorageState.Green).forEach { state ->
                monitorMyAccountUpdateFakeFlow.emit(
                    MyAccountUpdate(
                        action = MyAccountUpdate.Action.STORAGE_STATE_CHANGED,
                        storageState = state,
                    )
                )
            }
            advanceUntilIdle()

            verify(resumeSyncsSuspendedByStorageOverquotaUseCase, times(3)).invoke()
            job.cancel()
        }
}
