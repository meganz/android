package mega.privacy.android.app.receivers

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BootEventReceiverTest {

    private lateinit var underTest: BootEventReceiver
    private val context = mock<Context>()
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = BootEventReceiver()
        underTest.applicationScope = applicationScope
        underTest.startCameraUploadUseCase = startCameraUploadUseCase
    }

    @Test
    fun `test that the camera uploads is started when an event BOOT_COMPLETED is received`() =
        runTest {
            val intent = mock<Intent> {
                on { action }.thenReturn("android.intent.action.BOOT_COMPLETED")
            }

            underTest.onReceive(context, intent)

            verify(underTest.startCameraUploadUseCase).invoke()
        }
}
