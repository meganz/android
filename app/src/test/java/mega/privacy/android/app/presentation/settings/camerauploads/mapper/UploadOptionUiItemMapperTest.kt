package mega.privacy.android.app.presentation.settings.camerauploads.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.settings.camerauploads.mapper.UploadOptionUiItemMapper
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [UploadOptionUiItemMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UploadOptionUiItemMapperTest {

    private lateinit var underTest: UploadOptionUiItemMapper

    @BeforeAll
    fun setUp() {
        underTest = UploadOptionUiItemMapper()
    }

    @ParameterizedTest(name = "when upload option is {0}, then its ui equivalent is {1}")
    @MethodSource("provideParameters")
    fun `test that the correct upload option ui item is returned`(
        uploadOption: UploadOption,
        uploadOptionUiItem: UploadOptionUiItem,
    ) {
        assertThat(underTest(uploadOption)).isEqualTo(uploadOptionUiItem)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(UploadOption.PHOTOS, UploadOptionUiItem.PhotosOnly),
        Arguments.of(UploadOption.VIDEOS, UploadOptionUiItem.VideosOnly),
        Arguments.of(UploadOption.PHOTOS_AND_VIDEOS, UploadOptionUiItem.PhotosAndVideos),
    )
}