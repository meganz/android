package mega.privacy.android.data.mapper.node

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeMediaTypeMapperTest {
    private val underTest = NodeMediaTypeMapper()

    @Test
    fun `test that a node with a video codec is classified as video`() {
        val result = underTest(
            videoCodecId = 27,
            shortFormat = 5,
            width = 1920,
            height = 1080,
            duration = 120
        )
        assertThat(result).isEqualTo(
            VideoFileTypeInfo(mimeType = "video/mp4", extension = "", duration = 120.seconds)
        )
    }

    @Test
    fun `test that media info with dimensions is classified as video`() {
        val result =
            underTest(videoCodecId = -1, shortFormat = 5, width = 640, height = 480, duration = 30)
        assertThat(result).isEqualTo(
            VideoFileTypeInfo(mimeType = "video/mp4", extension = "", duration = 30.seconds)
        )
    }

    @Test
    fun `test that media info without dimensions is classified as audio`() {
        val result =
            underTest(videoCodecId = -1, shortFormat = 3, width = -1, height = -1, duration = 200)
        assertThat(result).isEqualTo(
            AudioFileTypeInfo(mimeType = "audio/mpeg", extension = "", duration = 200.seconds)
        )
    }

    @Test
    fun `test that a duration without dimensions or media info is classified as audio`() {
        val result =
            underTest(videoCodecId = -1, shortFormat = -1, width = -1, height = -1, duration = 90)
        assertThat(result).isEqualTo(
            AudioFileTypeInfo(mimeType = "audio/mpeg", extension = "", duration = 90.seconds)
        )
    }

    @Test
    fun `test that dimensions without media info or duration are classified as image`() {
        val result =
            underTest(videoCodecId = -1, shortFormat = -1, width = 800, height = 600, duration = -1)
        assertThat(result).isEqualTo(
            StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "")
        )
    }

    @Test
    fun `test that a node with no media attributes returns null`() {
        val result =
            underTest(videoCodecId = -1, shortFormat = -1, width = -1, height = -1, duration = -1)
        assertThat(result).isNull()
    }
}
