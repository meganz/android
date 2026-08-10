package mega.privacy.android.app.receivers

import android.app.Application
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GlobalNetworkStateHandlerTest {

    private val megaChatApi = mock<MegaChatApiAndroid>()
    private val megaApi = mock<MegaApiAndroid>()
    private val application = mock<Application>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    @BeforeEach
    fun resetMocks() {
        reset(megaChatApi, megaApi, application, monitorConnectivityUseCase)
    }

    private fun createUnderTest(applicationScope: CoroutineScope) = GlobalNetworkStateHandler(
        megaChatApi = megaChatApi,
        megaApi = megaApi,
        application = application,
        applicationScope = applicationScope,
        monitorConnectivityUseCase = monitorConnectivityUseCase,
    )

    @Test
    fun `test that constructor does not start connectivity collection`() = runTest {
        createUnderTest(backgroundScope)
        runCurrent()

        verifyNoInteractions(monitorConnectivityUseCase)
    }

    @Test
    fun `test that start sets localIpAddress to null when disconnected`() = runTest {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false))
        val underTest = createUnderTest(this)

        underTest.start()
        advanceUntilIdle()

        assertThat(underTest.localIpAddress).isNull()
        verifyNoInteractions(megaApi)
        verifyNoInteractions(megaChatApi)
    }

    @Test
    fun `test that start does not reconnect when connected and no valid ip address is available`() =
        runTest {
            whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
            val underTest = createUnderTest(this)

            underTest.start()
            advanceUntilIdle()

            assertThat(underTest.localIpAddress).isNull()
            verifyNoInteractions(megaApi)
            verifyNoInteractions(megaChatApi)
        }
}
