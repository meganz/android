package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.RemoteConfigGateway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoteConfigRepositoryImplTest {
    private lateinit var underTest: RemoteConfigRepositoryImpl

    private val remoteConfigGateway = mock<RemoteConfigGateway>()

    @BeforeEach
    fun setUp() {
        reset(remoteConfigGateway)
        underTest = RemoteConfigRepositoryImpl(
            ioDispatcher = UnconfinedTestDispatcher(),
            remoteConfigGateway = remoteConfigGateway,
        )
    }

    @Test
    fun `test that fetchAndActivate returns value from gateway`() = runTest {
        whenever(remoteConfigGateway.fetchAndActivate()).thenReturn(true)

        val actual = underTest.fetchAndActivate()

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that fetchAndActivate sets minimum fetch interval when useMinimalFetchInterval is true`() =
        runTest {
            whenever(remoteConfigGateway.fetchAndActivate()).thenReturn(false)

            underTest.fetchAndActivate(useMinimalFetchInterval = true)

            verify(remoteConfigGateway).setMinimumFetchInterval(0)
            verify(remoteConfigGateway).fetchAndActivate()
        }

    @Test
    fun `test that fetchAndActivate does not set minimum fetch interval when useMinimalFetchInterval is false`() =
        runTest {
            whenever(remoteConfigGateway.fetchAndActivate()).thenReturn(false)

            underTest.fetchAndActivate(useMinimalFetchInterval = false)

            verify(remoteConfigGateway, never()).setMinimumFetchInterval(0)
        }
}
