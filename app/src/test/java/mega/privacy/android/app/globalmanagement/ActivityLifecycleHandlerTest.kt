package mega.privacy.android.app.globalmanagement

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.StartSyncWorkerUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.StopSyncWorkerUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActivityLifecycleHandlerTest {
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val startSyncWorkerUseCase: StartSyncWorkerUseCase = mock()
    private val stopSyncWorkerUseCase: StopSyncWorkerUseCase = mock()
    private val activity: Activity = mock()

    private lateinit var applicationScope: CoroutineScope
    private lateinit var underTest: ActivityLifecycleHandler

    @BeforeEach
    fun setUp() {
        applicationScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
        whenever(monitorStorageStateEventUseCase()).thenReturn(
            MutableStateFlow(
                StorageStateEvent(
                    handle = 1L,
                    storageState = StorageState.Unknown,
                )
            )
        )
        underTest = ActivityLifecycleHandler(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            startSyncWorkerUseCase = startSyncWorkerUseCase,
            stopSyncWorkerUseCase = stopSyncWorkerUseCase,
            applicationScope = applicationScope,
        )
    }

    @AfterEach
    fun tearDown() {
        applicationScope.cancel()
    }

    @Test
    fun `test that first activity start stops worker and last activity stop starts worker`() = runTest {
        underTest.onActivityStarted(activity)
        underTest.onActivityStopped(activity)

        verifyBlocking(stopSyncWorkerUseCase) { invoke() }
        verifyBlocking(startSyncWorkerUseCase) { invoke() }
    }

    @Test
    fun `test that worker is not toggled while another activity remains started`() = runTest {
        underTest.onActivityStarted(activity)
        underTest.onActivityStarted(activity)
        underTest.onActivityStopped(activity)

        verifyBlocking(stopSyncWorkerUseCase) { invoke() }
        verifyNoInteractions(startSyncWorkerUseCase)

        underTest.onActivityStopped(activity)

        verifyBlocking(startSyncWorkerUseCase) { invoke() }
    }

    @Test
    fun `test that the latest lifecycle intent wins when start and stop are dispatched out of order`() =
        runTest {
            val queuedScope =
                CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            val handler = ActivityLifecycleHandler(
                monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
                startSyncWorkerUseCase = startSyncWorkerUseCase,
                stopSyncWorkerUseCase = stopSyncWorkerUseCase,
                applicationScope = queuedScope,
            )

            handler.onActivityStarted(activity)
            handler.onActivityStopped(activity)
            advanceUntilIdle()

            // Both coroutines observe the latest intent, so the worker converges on started.
            // Starting twice is a no-op because the work is enqueued with KEEP.
            verifyBlocking(startSyncWorkerUseCase, atLeastOnce()) { invoke() }
            verifyNoInteractions(stopSyncWorkerUseCase)

            queuedScope.cancel()
        }

    @Test
    fun `test that current activity is updated on resume and cleared on pause`() {
        underTest.onActivityResumed(activity)

        assertThat(underTest.getCurrentActivity()).isSameInstanceAs(activity)

        underTest.onActivityPaused(activity)

        assertThat(underTest.getCurrentActivity()).isNull()
    }
}
