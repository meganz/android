package mega.privacy.android.app.fcm

import com.google.common.truth.Truth.assertThat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.utils.Constants.DEVICE_ANDROID
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.monitoring.CrashReporter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaMessageCoordinatorTest {

    private val workManager: WorkManager = mock()
    private val crashReporter: CrashReporter = mock()
    private val applicationScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var underTest: MegaMessageCoordinator

    @BeforeEach
    fun setUp() {
        reset(workManager, crashReporter)
        stubPendingWork()
        underTest = MegaMessageCoordinator(applicationScope, workManager, crashReporter)
    }

    private fun stubPendingWork() {
        whenever(workManager.getWorkInfosFlow(any<WorkQuery>())).thenReturn(flowOf(emptyList()))
    }

    @Test
    fun `test that onMessageReceived enqueues a push message worker`() = runTest {
        underTest.onMessageReceived(mapOf("key" to "value"))

        val request = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager).enqueue(request.capture())
        assertThat(request.firstValue.tags).contains(PushMessageWorker::class.java.name)
    }

    @Test
    fun `test that onNewToken enqueues a unique new token worker`() = runTest {
        underTest.onNewToken("new-token")

        val request = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager).enqueueUniqueWork(
            eq(NewTokenWorker.WORK_NAME),
            eq(ExistingWorkPolicy.REPLACE),
            request.capture(),
        )
        assertThat(request.firstValue.tags).contains(NewTokenWorker::class.java.name)
    }

    @Test
    fun `test that onMessageReceived logs the pending work to the crash reporter`() = runTest {
        underTest.onMessageReceived(emptyMap())

        verify(crashReporter).log(any())
    }

    @Test
    fun `test that onNewToken carries the token and device type to the worker`() = runTest {
        underTest.onNewToken("new-token")

        val request = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager).enqueueUniqueWork(any(), any(), request.capture())
        assertThat(request.firstValue.workSpec.input.getString(NewTokenWorker.NEW_TOKEN))
            .isEqualTo("new-token")
        assertThat(request.firstValue.workSpec.input.getInt(NewTokenWorker.DEVICE_TYPE, -1))
            .isEqualTo(DEVICE_ANDROID)
    }
}
