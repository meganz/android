package mega.privacy.android.domain.usecase.psa

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.psa.PsaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DismissPsaUseCaseTest {
    private lateinit var underTest: DismissPsaUseCase

    private val psaRepository = mock<PsaRepository>()

    @BeforeEach
    fun setUp() {
        underTest = DismissPsaUseCase(psaRepository = psaRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(psaRepository)
    }

    @Test
    fun `test that psa is dismissed`() = runTest {
        val psaId = 12
        psaRepository.stub { on { monitorDisplayedPsa() } doReturn flowOf(psaId) }

        underTest(psaId = psaId)

        verify(psaRepository).dismissPsa(psaId)
    }

    @Test
    fun `test that cache is cleared`() = runTest {
        val psaId = 12
        psaRepository.stub { on { monitorDisplayedPsa() } doReturn flowOf(psaId) }

        underTest(psaId = psaId)

        verify(psaRepository).clearCache()
    }

    @Test
    fun `test that displayed psa is cleared if id matches`() = runTest {
        val psaId = 13
        psaRepository.stub { on { monitorDisplayedPsa() } doReturn flowOf(psaId) }

        underTest(psaId = psaId)

        verify(psaRepository).setDisplayedPsa(null)
    }

    @Test
    fun `test that displayed psa is not cleared if id does not match`() = runTest {
        val psaId = 13
        psaRepository.stub { on { monitorDisplayedPsa() } doReturn flowOf(psaId + 1) }

        underTest(psaId = psaId)

        verify(psaRepository, never()).setDisplayedPsa(null)
    }
}