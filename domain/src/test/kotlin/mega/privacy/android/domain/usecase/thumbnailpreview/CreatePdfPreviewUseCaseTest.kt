package mega.privacy.android.domain.usecase.thumbnailpreview

import com.google.common.truth.Truth
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
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreatePdfPreviewUseCaseTest {

    private lateinit var underTest: CreatePdfPreviewUseCase

    private lateinit var pdfRepository: PdfRepository

    @BeforeAll
    fun setup() {
        pdfRepository = mock()
        underTest = CreatePdfPreviewUseCase(pdfRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(pdfRepository)
    }

    @ParameterizedTest(name = " {0}")
    @ValueSource(strings = ["test/path"])
    @NullSource
    fun `test that create pdf preview returns correctly when repository returns `(
        expectedPreviewPath: String?,
    ) = runTest {
        val file = File("test/filePath")
        val localPath = "test/localPath"
        whenever(pdfRepository.createPreview(file, localPath)).thenReturn(expectedPreviewPath)
        Truth.assertThat(underTest.invoke(file, localPath)).isEqualTo(expectedPreviewPath)
    }
}