package mega.privacy.android.app.presentation.settings.camerauploads.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.settings.camerauploads.mapper.VideoQualityUiItemMapper
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import mega.privacy.android.domain.entity.VideoQuality
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [VideoQualityUiItemMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VideoQualityUiItemMapperTest {

    private lateinit var underTest: VideoQualityUiItemMapper

    @BeforeAll
    fun setUp() {
        underTest = VideoQualityUiItemMapper()
    }

    @ParameterizedTest(name = "when video quality is {0}, then its ui equivalent is {1}")
    @MethodSource("provideParameters")
    fun `test that the correct video quality ui item is returned`(
        videoQuality: VideoQuality,
        videoQualityUiItem: VideoQualityUiItem,
    ) {
        assertThat(underTest(videoQuality)).isEqualTo(videoQualityUiItem)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(VideoQuality.LOW, VideoQualityUiItem.Low),
        Arguments.of(VideoQuality.MEDIUM, VideoQualityUiItem.Medium),
        Arguments.of(VideoQuality.HIGH, VideoQualityUiItem.High),
        Arguments.of(VideoQuality.ORIGINAL, VideoQualityUiItem.Original),
    )
}