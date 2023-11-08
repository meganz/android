package mega.privacy.android.data.repository.apiServer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.apiserver.ApiServerMapper
import mega.privacy.android.data.repository.apiserver.ApiServerRepositoryImpl
import mega.privacy.android.domain.repository.apiserver.ApiServerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApiServerRepositoryImplTest {

    private lateinit var underTest: ApiServerRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val apiServerMapper = ApiServerMapper()

    @BeforeAll
    fun setup() {
        underTest = ApiServerRepositoryImpl(
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            apiServerMapper = apiServerMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
            context = mock()
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(megaApiGateway, megaApiFolderGateway)
    }

    @ParameterizedTest(name = " with {0} value as param")
    @ValueSource(booleans = [true, false])
    fun `test that set public key pinning invokes megaApi`(enable: Boolean) = runTest {
        underTest.setPublicKeyPinning(enable)
        verify(megaApiGateway).setPublicKeyPinning(enable)
        verifyNoMoreInteractions(megaApiGateway)
    }

    @ParameterizedTest(name = " with {0} value as param")
    @ValueSource(booleans = [true, false])
    fun `test that set public key pinning invokes megaApiFolder`(enable: Boolean) = runTest {
        underTest.setPublicKeyPinning(enable)
        verify(megaApiFolderGateway).setPublicKeyPinning(enable)
        verifyNoMoreInteractions(megaApiFolderGateway)
    }

    @ParameterizedTest(name = " with {0} value as param")
    @ValueSource(booleans = [true, false])
    fun `test that change api url invokes megaApi`(disablePkp: Boolean) = runTest {
        val apiUrl = "apiURL"
        underTest.changeApi(apiUrl, disablePkp)
        verify(megaApiGateway).changeApiUrl(apiUrl, disablePkp)
        verifyNoMoreInteractions(megaApiGateway)
    }

    @ParameterizedTest(name = " with {0} value as param")
    @ValueSource(booleans = [true, false])
    fun `test that change api url invokes megaApiFolder`(disablePkp: Boolean) = runTest {
        val apiUrl = "apiURL"
        underTest.changeApi(apiUrl, disablePkp)
        verify(megaApiFolderGateway).changeApiUrl(apiUrl, disablePkp)
        verifyNoMoreInteractions(megaApiFolderGateway)
    }
}