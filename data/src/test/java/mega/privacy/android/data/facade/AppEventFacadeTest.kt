package mega.privacy.android.data.facade

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import org.junit.Before
import org.junit.Test
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
            appScope = coroutineScope
        )
    }

    @Test
    fun `test that broadcast upload pause state fires an event`() = runTest {
        underTest.monitorCameraUploadPauseState.test {
            underTest.broadcastUploadPauseState()
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that set SMS Verification Shown set the correct state`() = runTest {
        underTest.setSMSVerificationShown(true)
        assertThat(underTest.isSMSVerificationShown()).isTrue()
        underTest.setSMSVerificationShown(false)
        assertThat(underTest.isSMSVerificationShown()).isFalse()
    }

    @Test
    fun `test that is SMSVerification Shown default value is the correct one`() = runTest {
        assertThat(underTest.isSMSVerificationShown()).isFalse()
    }
}
