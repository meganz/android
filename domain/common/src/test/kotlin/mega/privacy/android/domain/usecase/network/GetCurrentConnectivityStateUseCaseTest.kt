package mega.privacy.android.domain.usecase.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.repository.NetworkRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCurrentConnectivityStateUseCaseTest {
    private lateinit var underTest: GetCurrentConnectivityStateUseCase

    private val networkRepository = mock<NetworkRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetCurrentConnectivityStateUseCase(
            networkRepository = networkRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(networkRepository)
    }

    @Test
    fun `test that invoke returns Connected when repository returns Connected`() = runTest {
        val expected = ConnectivityState.Connected(isOnWifi = true)
        whenever(networkRepository.getCurrentConnectivityState()).thenReturn(expected)

        val actual = underTest()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that invoke returns Disconnected when repository returns Disconnected`() = runTest {
        val expected = ConnectivityState.Disconnected
        whenever(networkRepository.getCurrentConnectivityState()).thenReturn(expected)

        val actual = underTest()

        assertThat(actual).isEqualTo(expected)
    }
}