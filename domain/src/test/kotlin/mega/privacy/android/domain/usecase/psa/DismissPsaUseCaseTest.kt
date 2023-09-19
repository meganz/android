package mega.privacy.android.domain.usecase.psa

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.psa.PsaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DismissPsaUseCaseTest {
    private lateinit var underTest: DismissPsaUseCase

    private val psaRepository = mock<PsaRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = DismissPsaUseCase(psaRepository = psaRepository)
    }

    @Test
    internal fun `test that psa is dismissed`() = runTest {
        val psaId = 12
        underTest(psaId = psaId)

        verify(psaRepository).dismissPsa(psaId)
    }

    @Test
    internal fun `test that cache is cleared`() = runTest {
        val psaId = 12
        underTest(psaId = psaId)

        verify(psaRepository).clearCache()
    }
}