package test.mega.privacy.android.app.presentation.apiserver

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.apiserver.ApiServerViewModel
import mega.privacy.android.domain.entity.apiserver.ApiServer
import mega.privacy.android.domain.usecase.apiserver.GetCurrentApiServerUseCase
import mega.privacy.android.domain.usecase.apiserver.UpdateApiServerUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiServerViewModelTest {

    private lateinit var underTest: ApiServerViewModel

    private val getCurrentApiServerUseCase = mock<GetCurrentApiServerUseCase>()
    private val updateApiServerUseCase = mock<UpdateApiServerUseCase>()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initTestClass()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(getCurrentApiServerUseCase, updateApiServerUseCase)
    }

    @ParameterizedTest(name = " returns {0}")
    @EnumSource(ApiServer::class)
    fun `test that current api server updates correctly when use case`(
        apiServer: ApiServer,
    ) = runTest {
        whenever(getCurrentApiServerUseCase()).thenReturn(apiServer)
        initTestClass()
        underTest.state.test {
            Truth.assertThat(awaitItem().currentApiServer).isEqualTo(apiServer)
        }
    }

    @ParameterizedTest(name = " with {0}")
    @EnumSource(ApiServer::class)
    fun `test that new api server is updated`(newApiServer: ApiServer) = runTest {
        with(underTest) {
            updateNewApiServer(newApiServer)
            state.test {
                Truth.assertThat(awaitItem().newApiServer).isEqualTo(newApiServer)
            }
        }
    }

    @ParameterizedTest(name = " with {0} as current value and {1} as new one")
    @MethodSource("provideParameters")
    fun `test that update api server use case is invoked when confirmUpdateApiServer is called`(
        currentApiServer: ApiServer,
        newApiServer: ApiServer,
    ) = runTest {
        whenever(getCurrentApiServerUseCase()).thenReturn(currentApiServer)
        whenever(updateApiServerUseCase(currentApiServer, newApiServer)).thenReturn(Unit)
        initTestClass()
        with(underTest) {
            updateNewApiServer(newApiServer)
            confirmUpdateApiServer()
        }
        verify(updateApiServerUseCase).invoke(currentApiServer, newApiServer)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ApiServer.Production, ApiServer.Production),
        Arguments.of(ApiServer.Production, ApiServer.Staging),
        Arguments.of(ApiServer.Production, ApiServer.Staging444),
        Arguments.of(ApiServer.Production, ApiServer.Sandbox3),
        Arguments.of(ApiServer.Staging, ApiServer.Production),
        Arguments.of(ApiServer.Staging, ApiServer.Staging),
        Arguments.of(ApiServer.Staging, ApiServer.Staging444),
        Arguments.of(ApiServer.Staging, ApiServer.Sandbox3),
        Arguments.of(ApiServer.Staging444, ApiServer.Production),
        Arguments.of(ApiServer.Staging444, ApiServer.Staging),
        Arguments.of(ApiServer.Staging444, ApiServer.Staging444),
        Arguments.of(ApiServer.Staging444, ApiServer.Sandbox3),
        Arguments.of(ApiServer.Sandbox3, ApiServer.Production),
        Arguments.of(ApiServer.Sandbox3, ApiServer.Staging),
        Arguments.of(ApiServer.Sandbox3, ApiServer.Staging444),
        Arguments.of(ApiServer.Sandbox3, ApiServer.Sandbox3),
    )

    private fun initTestClass() {
        underTest = ApiServerViewModel(
            getCurrentApiServerUseCase = getCurrentApiServerUseCase,
            updateApiServerUseCase = updateApiServerUseCase,
        )
    }
}