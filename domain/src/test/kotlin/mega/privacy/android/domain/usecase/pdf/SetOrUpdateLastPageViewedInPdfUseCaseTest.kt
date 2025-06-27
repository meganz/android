package mega.privacy.android.domain.usecase.pdf

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import mega.privacy.android.domain.repository.files.PdfRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoMoreInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetOrUpdateLastPageViewedInPdfUseCaseTest {

    private lateinit var underTest: SetOrUpdateLastPageViewedInPdfUseCase

    private val pdfRepository = mock<PdfRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetOrUpdateLastPageViewedInPdfUseCase(pdfRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(pdfRepository)
    }

    @Test
    fun `test that use case invokes correctly`() = runTest {
        val lastPageViewedInPdf = LastPageViewedInPdf(
            nodeHandle = 12345L,
            lastPageViewed = 10
        )

        underTest(lastPageViewedInPdf)

        verify(pdfRepository).setOrUpdateLastPageViewedInPdf(lastPageViewedInPdf)
        verifyNoMoreInteractions(pdfRepository)
    }
}