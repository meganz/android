package mega.privacy.android.domain.usecase.network

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NetworkRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsConnectedToInternetUseCaseTest {
    private lateinit var underTest: IsConnectedToInternetUseCase

    private val networkRepository = mock<NetworkRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsConnectedToInternetUseCase(
            networkRepository = networkRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(networkRepository)
    }

    @ParameterizedTest(name = "is connected {0}")
    @ValueSource(booleans = [true, false])
    fun `test that when invoked it returns correct value`(isConnected: Boolean) = runTest {
        whenever(networkRepository.isConnectedToInternet()).thenReturn(isConnected)
        Truth.assertThat(underTest()).isEqualTo(isConnected)
    }
}
