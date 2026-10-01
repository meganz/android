package mega.privacy.android.domain.usecase.pdf

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.files.PdfRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetLastPageViewedInPdfUseCaseTest {

    private lateinit var underTest: GetLastPageViewedInPdfUseCase

    private val pdfRepository = mock<PdfRepository>()

    private val nodeHandle = 12345L

    @BeforeAll
    fun setUp() {
        underTest = GetLastPageViewedInPdfUseCase(pdfRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(pdfRepository)
    }

    @Test
    fun `test that the last page viewed in PDF is retrieved`() = runTest {
        val expectedPage = 10L

        whenever(pdfRepository.getLastPageViewedInPdf(nodeHandle)) doReturn expectedPage

        assertThat(underTest(nodeHandle)).isEqualTo(expectedPage)
        verify(pdfRepository).getLastPageViewedInPdf(nodeHandle)
        verifyNoMoreInteractions(pdfRepository)
    }

    @Test
    fun `test that the last page viewed in PDF is not retrieved`() = runTest {
        whenever(pdfRepository.getLastPageViewedInPdf(nodeHandle)) doReturn null

        assertThat(underTest(nodeHandle)).isNull()
        verify(pdfRepository).getLastPageViewedInPdf(nodeHandle)
        verifyNoMoreInteractions(pdfRepository)
    }
}