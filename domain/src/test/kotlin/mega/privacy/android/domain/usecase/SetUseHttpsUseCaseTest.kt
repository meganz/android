package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NetworkRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class SetUseHttpsUseCaseTest {
    private lateinit var underTest: SetUseHttpsUseCase

    private val networkRepository = mock<NetworkRepository>()

    @Before
    fun setUp() {
        underTest = SetUseHttpsUseCase(networkRepository = networkRepository)
    }

    @Test
    fun `test that network setup is updated`() = runTest {
        underTest(true)

        verify(networkRepository).setUseHttps(true)
    }

    @Test
    fun `test that enabled state is returned`() = runTest {
        assertThat(underTest(true)).isTrue()
    }
}