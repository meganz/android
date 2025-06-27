package mega.privacy.android.domain.usecase.pdf

import kotlinx.coroutines.test.runTest
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
class DeleteLastPageViewedInPdfUseCaseTest {

    private lateinit var underTest: DeleteLastPageViewedInPdfUseCase

    private val pdfRepository = mock<PdfRepository>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteLastPageViewedInPdfUseCase(pdfRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(pdfRepository)
    }

    @Test
    fun `test that use case invokes correctly`() = runTest {
        val nodeHandle = 12345L

        underTest(nodeHandle)

        verify(pdfRepository).deleteLastPageViewedInPdf(nodeHandle)
        verifyNoMoreInteractions(pdfRepository)
    }
}