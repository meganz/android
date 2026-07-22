package mega.privacy.android.domain.usecase.featureflag

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.RemoteConfigRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetchAndActivateRemoteConfigUseCaseTest {
    private lateinit var underTest: FetchAndActivateRemoteConfigUseCase

    private val remoteConfigRepository = mock<RemoteConfigRepository>()

    @BeforeAll
    fun setUp() {
        underTest = FetchAndActivateRemoteConfigUseCase(
            remoteConfigRepository = remoteConfigRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(remoteConfigRepository)
    }

    @Test
    fun `test that invoke returns value from repository`() = runTest {
        whenever(remoteConfigRepository.fetchAndActivate(false)).thenReturn(true)

        val actual = underTest()

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that invoke passes useMinimalFetchInterval to repository`() = runTest {
        whenever(remoteConfigRepository.fetchAndActivate(true)).thenReturn(false)

        underTest(useMinimalFetchInterval = true)

        verify(remoteConfigRepository).fetchAndActivate(true)
    }
}
