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
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateApiServerUseCaseTest {

    private lateinit var underTest: UpdateApiServerUseCase

    private val apiServerRepository = mock<ApiServerRepository>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateApiServerUseCase(apiServerRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(apiServerRepository)
    }

    @ParameterizedTest(name = " if current api is {0} and new api is {1}")
    @MethodSource("provideParameters")
    fun `test that update api server invokes the correct repository methods`(
        currentApi: ApiServer,
        newApi: ApiServer,
    ) = runTest {
        underTest.invoke(currentApi, newApi)

        if (currentApi == newApi) {
            verifyNoInteractions(apiServerRepository)
        } else {
            var disablePkp = false
            var setPkp: Boolean? = null

            when {
                currentApi == ApiServer.Sandbox3 || currentApi == ApiServer.Staging444 -> {
                    setPkp = true
                }

                newApi == ApiServer.Sandbox3 || newApi == ApiServer.Staging444 -> {
                    setPkp = false
                    disablePkp = true
                }
            }

            setPkp?.let { verify(apiServerRepository).setPublicKeyPinning(it) }
            verify(apiServerRepository).changeApi(newApi.url, disablePkp)
            verify(apiServerRepository).setNewApi(newApi)
            verifyNoMoreInteractions(apiServerRepository)
        }
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
}