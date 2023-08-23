package mega.privacy.android.domain.usecase.thumbnailpreview

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.files.PdfRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreatePdfPreviewUseCaseTest {

    private lateinit var underTest: CreatePdfPreviewUseCase

    private val pdfRepository = mock<PdfRepository>()
    private val setPreviewUseCase = mock<SetPreviewUseCase>()

    @BeforeAll
    fun setup() {
        underTest = CreatePdfPreviewUseCase(
            pdfRepository = pdfRepository,
            setPreviewUseCase = setPreviewUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(pdfRepository, setPreviewUseCase)
    }

    @ParameterizedTest(name = " {0}")
    @ValueSource(strings = ["test/path"])
    @NullSource
    fun `test that create pdf preview returns correctly when repository returns `(
        expectedPreviewPath: String?,
    ) = runTest {
        val nodeHandle = 1L
        val path = "test/filePath"
        val localFile = File(path)
        whenever(pdfRepository.createPreview(nodeHandle, localFile)).thenReturn(expectedPreviewPath)
        expectedPreviewPath?.let {
            whenever(setPreviewUseCase(nodeHandle, expectedPreviewPath)).thenReturn(Unit)
        }
        underTest.invoke(nodeHandle, localFile)
        verify(pdfRepository).createPreview(nodeHandle, localFile)

        if (expectedPreviewPath == null) {
            verify(setPreviewUseCase, never()).invoke(nodeHandle, path)
        } else {
            verify(setPreviewUseCase).invoke(nodeHandle, expectedPreviewPath)
        }
    }
}