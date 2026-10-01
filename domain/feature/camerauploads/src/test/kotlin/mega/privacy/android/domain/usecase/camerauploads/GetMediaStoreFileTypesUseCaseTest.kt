package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMediaStoreFileTypesUseCaseTest {

    private lateinit var underTest: GetMediaStoreFileTypesUseCase

    private val getUploadOptionUseCase = mock<GetUploadOptionUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetMediaStoreFileTypesUseCase(
            getUploadOptionUseCase = getUploadOptionUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getUploadOptionUseCase)
    }

    @ParameterizedTest(name = "upload option {0} and media store file types {1}")
    @MethodSource("provideParameters")
    internal fun `test that correct media types are returned when invoked`(
        uploadOption: UploadOption,
        mediaStoreFileTypes: List<MediaStoreFileType>,
    ) =
        runTest {
            whenever(getUploadOptionUseCase.invoke()).thenReturn(uploadOption)
            val actual = underTest()
            Truth.assertThat(actual).isEqualTo(mediaStoreFileTypes)
        }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            UploadOption.PHOTOS,
            listOf(MediaStoreFileType.IMAGES_INTERNAL, MediaStoreFileType.IMAGES_EXTERNAL)
        ),
        Arguments.of(
            UploadOption.VIDEOS,
            listOf(MediaStoreFileType.VIDEO_INTERNAL, MediaStoreFileType.VIDEO_EXTERNAL)
        ),
        Arguments.of(
            UploadOption.PHOTOS_AND_VIDEOS,
            listOf(
                MediaStoreFileType.IMAGES_INTERNAL,
                MediaStoreFileType.IMAGES_EXTERNAL,
                MediaStoreFileType.VIDEO_INTERNAL,
                MediaStoreFileType.VIDEO_EXTERNAL
            )
        ),
    )
}
