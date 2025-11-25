package mega.privacy.android.app.sslverification.initialiser

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.sslverification.SSLErrorDialog
import mega.privacy.android.domain.usecase.network.MonitorSslVerificationFailedUseCase
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class SSLErrorMonitorInitialiserTest {
    private lateinit var underTest: SSLErrorMonitorInitialiser

    private val monitorSslVerificationFailedUseCase = mock<MonitorSslVerificationFailedUseCase>()
    private val appDialogsEventQueue = mock<AppDialogsEventQueue>()

    @Test
    fun `test that correct event is emitted`() = runTest {
        monitorSslVerificationFailedUseCase.stub {
            on { invoke() } doReturn flowOf(Unit)
        }

        underTest = SSLErrorMonitorInitialiser(
            monitorSslVerificationFailedUseCase = monitorSslVerificationFailedUseCase,
            appDialogEventQueue = appDialogsEventQueue,
        )

        underTest()

        verify(appDialogsEventQueue).emit(
            AppDialogEvent(SSLErrorDialog)
        )
    }
}