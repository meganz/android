package test.mega.privacy.android.app.fcm


import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.foreground.ForegroundProcessor
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.mapper.PushMessageMapper
import mega.privacy.android.app.fcm.PushMessageWorker
import mega.privacy.android.domain.exception.ChatNotInitializedException
import mega.privacy.android.domain.exception.EmptyFolderException
import mega.privacy.android.domain.exception.SessionNotRetrievedException
import mega.privacy.android.domain.usecase.CompleteFastLogin
import mega.privacy.android.domain.usecase.InitialiseMegaChat
import mega.privacy.android.domain.usecase.PushReceived
import mega.privacy.android.domain.usecase.RetryPendingConnections
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PushMessageWorkerTest {

    private lateinit var underTest: PushMessageWorker

    private lateinit var context: Context
    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    private val completeFastLogin = mock<CompleteFastLogin>()
    private val pushReceived = mock<PushReceived>()
    private val retryPendingConnections = mock<RetryPendingConnections>()
    private val pushMessageMapper = mock<PushMessageMapper>()
    private val initialiseMegaChat = mock<InitialiseMegaChat>()
    private val ioDispatcher = UnconfinedTestDispatcher()


    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase = WorkDatabase.create(context, workExecutor.backgroundExecutor, true)

        underTest = PushMessageWorker(
            context = context,
            workerParams = WorkerParameters(
                UUID.randomUUID(),
                workDataOf(),
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                executor,
                workExecutor,
                WorkerFactory.getDefaultWorkerFactory(),
                WorkProgressUpdater(workDatabase, workExecutor),
                WorkForegroundUpdater(workDatabase, object : ForegroundProcessor {
                    override fun startForeground(
                        workSpecId: String,
                        foregroundInfo: ForegroundInfo,
                    ) {
                    }

                    override fun stopForeground(workSpecId: String) {}
                }, workExecutor)
            ),
            completeFastLogin = completeFastLogin,
            pushReceived = pushReceived,
            retryPendingConnections = retryPendingConnections,
            pushMessageMapper = pushMessageMapper,
            initialiseMegaChat = initialiseMegaChat,
            ioDispatcher = ioDispatcher
        )
    }

    @Test
    fun `test that doWork returns failure if fast login failed`() = runTest {
        whenever(completeFastLogin()).thenThrow(SessionNotRetrievedException::class.java)
        val result = underTest.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `test that retryPendingConnections is invoked if fast login success`() = runTest {
        whenever(completeFastLogin()).thenReturn("good_session")
        whenever(pushMessageMapper(any())).thenReturn(mock())

        underTest.doWork()
        verify(retryPendingConnections).invoke(false)
    }

    @Test
    fun `test that MegaChat is initialised if rootNode exists and retryPendingConnections raises ChatNotInitializedException`() =
        runTest {
            whenever(completeFastLogin()).thenReturn("good_session")
            whenever(pushMessageMapper(any())).thenReturn(mock())
            whenever(retryPendingConnections(any())).thenThrow(
                ChatNotInitializedException())

            underTest.doWork()
            verify(initialiseMegaChat).invoke("good_session")
        }

    @Test
    fun `test that initialiseMegaChat is not invoked if rootNode exists and retryPendingConnections raises exception other than ChatNotInitializedException`() =
        runTest {
            whenever(completeFastLogin()).thenReturn("good_session")
            whenever(pushMessageMapper(any())).thenReturn(mock())
            whenever(retryPendingConnections(any())).thenThrow(
                EmptyFolderException())

            underTest.doWork()
            verifyNoInteractions(initialiseMegaChat)
        }

    @Test
    fun `test that doWork returns failure if rootNode exists and retryPendingConnections raises ChatNotInitializedException and initialiseMegaChat fails`() =
        runTest {
            val sessionId = "good_session"
            whenever(completeFastLogin()).thenReturn(sessionId)
            whenever(pushMessageMapper(any())).thenReturn(mock())
            whenever(retryPendingConnections(any())).thenThrow(
                ChatNotInitializedException())
            whenever(initialiseMegaChat(sessionId)).thenThrow(ChatNotInitializedException())

            val result = underTest.doWork()
            assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        }
}