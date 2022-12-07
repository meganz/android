package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.VideoQuality
import org.junit.Test

class VideoQualityMapperTest {
    private val underTest: VideoQualityMapper = ::toVideoQuality

    @Test
    fun `test null value returns null`() {
        assertThat(underTest(null)).isNull()
    }

    @Test
    fun `test that invalid value returns null`() {
        assertThat(underTest("A what now?")).isNull()
    }

    @Test
    fun `test valid values`() {
        assertThat(underTest("0")).isEqualTo(VideoQuality.LOW)
        assertThat(underTest("1")).isEqualTo(VideoQuality.MEDIUM)
        assertThat(underTest("2")).isEqualTo(VideoQuality.HIGH)
        assertThat(underTest("3")).isEqualTo(VideoQuality.ORIGINAL)
    }
}