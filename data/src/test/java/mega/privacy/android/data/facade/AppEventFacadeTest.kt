package mega.privacy.android.data.facade

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class AppEventFacadeTest {

    private lateinit var underTest: AppEventGateway
    private lateinit var coroutineScope: CoroutineScope

    @Before
    fun setUp() {
        coroutineScope = TestScope(UnconfinedTestDispatcher())
        underTest = AppEventFacade(
            appScope = coroutineScope,
            context = mock()
        )
    }

    @Test
    fun `test that broadcast upload pause state fires an event`() = runTest {
        underTest.monitorCameraUploadPauseState.test {
            underTest.broadcastUploadPauseState()
            Truth.assertThat(awaitItem()).isTrue()
        }
    }
}
