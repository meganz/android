package mega.privacy.android.domain.usecase.psa

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.psa.PsaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ClearPsaUseCaseTest {
    private lateinit var underTest: ClearPsaUseCase

    private val psaRepository = mock<PsaRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = ClearPsaUseCase(psaRepository = psaRepository)
    }

    @Test
    internal fun `test that cache is cleared`() = runTest {
        underTest()

        verify(psaRepository).clearCache()
    }

    @Test
    internal fun `test that last fetched time is set to null`() = runTest {
        underTest()

        verify(psaRepository).setLastFetchedTime(null)
    }
}