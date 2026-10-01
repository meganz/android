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
 * Test class for [DeleteThumbnailUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteThumbnailUseCaseTest {

    private lateinit var underTest: DeleteThumbnailUseCase

    private val thumbnailPreviewRepository = mock<ThumbnailPreviewRepository>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteThumbnailUseCase(
            thumbnailPreviewRepository = thumbnailPreviewRepository,
        )
    }

    @ParameterizedTest(name = "invoked with {0}")
    @ValueSource(booleans = [true, false])
    @NullSource
    internal fun `test that thumbnail is deleted when invoked if exists`(result: Boolean?) =
        runTest {
            val handle = 1L
            whenever(thumbnailPreviewRepository.deleteThumbnail(handle)).thenReturn(result)
            Truth.assertThat(underTest(handle)).isEqualTo(result ?: true)
        }
}
