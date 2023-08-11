package mega.privacy.android.domain.usecase.thumbnailpreview

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test class for [DeletePreviewUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeletePreviewUseCaseTest {

    private lateinit var underTest: DeletePreviewUseCase

    private val thumbnailPreviewRepository = mock<ThumbnailPreviewRepository>()

    @BeforeAll
    fun setUp() {
        underTest = DeletePreviewUseCase(
            thumbnailPreviewRepository = thumbnailPreviewRepository,
        )
    }

    @ParameterizedTest(name = "invoked with {0}")
    @ValueSource(booleans = [true, false])
    @NullSource
    internal fun `test that preview is deleted when invoked if exists`(result: Boolean?) =
        runTest {
            val handle = 1L
            whenever(thumbnailPreviewRepository.deletePreview(handle)).thenReturn(result)
            Truth.assertThat(underTest(handle)).isEqualTo(result ?: true)
        }
}
