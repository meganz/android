package mega.privacy.android.domain.usecase.apiserver

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.apiserver.ApiServer
import mega.privacy.android.domain.repository.apiserver.ApiServerRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateApiServerUseCaseTest {

    private lateinit var underTest: UpdateApiServerUseCase

    private val apiServerRepository = mock<ApiServerRepository>()
    private val getCurrentApiServerUseCase = mock<GetCurrentApiServerUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateApiServerUseCase(apiServerRepository, getCurrentApiServerUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(apiServerRepository, getCurrentApiServerUseCase)
    }

    @ParameterizedTest(name = " if current api is {0}, stored api is {1} and new api is {2} ")
    @MethodSource("provideParameters")
    fun `test that update api server invokes the correct repository methods`(
        currentApi: ApiServer?,
        storedApi: ApiServer,
        newApi: ApiServer?,
    ) = runTest {
        val expectedStoredApi = currentApi ?: storedApi
        if (currentApi == null) {
            whenever(getCurrentApiServerUseCase()).thenReturn(storedApi)
        }
        underTest.invoke(currentApi, newApi)

        if (expectedStoredApi == newApi || (newApi == null && expectedStoredApi == ApiServer.Production)) {
            verifyNoInteractions(apiServerRepository)
        } else {
            var disablePkp = false
            var setPkp: Boolean? = null

            when {
                newApi != null && (expectedStoredApi == ApiServer.Sandbox3 || expectedStoredApi == ApiServer.Staging444) -> {
                    setPkp = true
                }

                newApi == ApiServer.Sandbox3 || newApi == ApiServer.Staging444 -> {
                    setPkp = false
                    disablePkp = true
                }
            }

            setPkp?.let { verify(apiServerRepository).setPublicKeyPinning(it) }
            verify(apiServerRepository).changeApi(newApi?.url ?: expectedStoredApi.url, disablePkp)
            verify(apiServerRepository).setNewApi(newApi ?: expectedStoredApi)
            verifyNoMoreInteractions(apiServerRepository)
        }
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ApiServer.Production, ApiServer.Production, ApiServer.Production),
        Arguments.of(ApiServer.Production, ApiServer.Production, ApiServer.Staging),
        Arguments.of(ApiServer.Production, ApiServer.Production, ApiServer.Staging444),
        Arguments.of(ApiServer.Production, ApiServer.Production, ApiServer.Sandbox3),
        Arguments.of(ApiServer.Production, ApiServer.Production, null),
        Arguments.of(ApiServer.Staging, ApiServer.Staging, ApiServer.Production),
        Arguments.of(ApiServer.Staging, ApiServer.Staging, ApiServer.Staging),
        Arguments.of(ApiServer.Staging, ApiServer.Staging, ApiServer.Staging444),
        Arguments.of(ApiServer.Staging, ApiServer.Staging, ApiServer.Sandbox3),
        Arguments.of(ApiServer.Staging, ApiServer.Staging, null),
        Arguments.of(ApiServer.Staging444, ApiServer.Staging444, ApiServer.Production),
        Arguments.of(ApiServer.Staging444, ApiServer.Staging444, ApiServer.Staging),
        Arguments.of(ApiServer.Staging444, ApiServer.Staging444, ApiServer.Staging444),
        Arguments.of(ApiServer.Staging444, ApiServer.Staging444, ApiServer.Sandbox3),
        Arguments.of(ApiServer.Staging444, ApiServer.Staging444, null),
        Arguments.of(ApiServer.Sandbox3, ApiServer.Sandbox3, ApiServer.Production),
        Arguments.of(ApiServer.Sandbox3, ApiServer.Sandbox3, ApiServer.Staging),
        Arguments.of(ApiServer.Sandbox3, ApiServer.Sandbox3, ApiServer.Staging444),
        Arguments.of(ApiServer.Sandbox3, ApiServer.Sandbox3, ApiServer.Sandbox3),
        Arguments.of(ApiServer.Sandbox3, ApiServer.Sandbox3, null),
        Arguments.of(null, ApiServer.Production, null),
        Arguments.of(null, ApiServer.Staging, null),
        Arguments.of(null, ApiServer.Staging444, null),
        Arguments.of(null, ApiServer.Sandbox3, null),
    )
}