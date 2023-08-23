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
class CreatePdfThumbnailUseCaseTest {

    private lateinit var underTest: CreatePdfThumbnailUseCase

    private val pdfRepository = mock<PdfRepository>()
    private val setThumbnailUseCase = mock<SetThumbnailUseCase>()

    @BeforeAll
    fun setup() {
        underTest = CreatePdfThumbnailUseCase(
            pdfRepository = pdfRepository,
            setThumbnailUseCase = setThumbnailUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(pdfRepository, setThumbnailUseCase)
    }

    @ParameterizedTest(name = " {0}")
    @ValueSource(strings = ["test/path"])
    @NullSource
    fun `test that create pdf thumbnail returns correctly when repository returns `(
        expectedThumbnailPath: String?,
    ) = runTest {
        val nodeHandle = 1L
        val path = "test/filePath"
        val localFile = File(path)
        whenever(pdfRepository.createThumbnail(nodeHandle, localFile))
            .thenReturn(expectedThumbnailPath)
        expectedThumbnailPath?.let {
            whenever(setThumbnailUseCase(nodeHandle, expectedThumbnailPath)).thenReturn(Unit)
        }
        underTest.invoke(nodeHandle, localFile)
        verify(pdfRepository).createThumbnail(nodeHandle, localFile)

        if (expectedThumbnailPath == null) {
            verify(setThumbnailUseCase, never()).invoke(nodeHandle, path)
        } else {
            verify(setThumbnailUseCase).invoke(nodeHandle, expectedThumbnailPath)
        }
    }
}