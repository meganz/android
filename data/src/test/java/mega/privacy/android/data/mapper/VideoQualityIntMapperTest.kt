package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.VideoQuality
import org.junit.Test

class VideoQualityIntMapperTest{
    private val underTest: VideoQualityIntMapper = ::videoQualityToInt

    @Test
    fun `test values`() {
        assertThat(underTest(VideoQuality.LOW)).isEqualTo(0)
        assertThat(underTest(VideoQuality.MEDIUM)).isEqualTo(1)
        assertThat(underTest(VideoQuality.HIGH)).isEqualTo(2)
        assertThat(underTest(VideoQuality.ORIGINAL)).isEqualTo(3)
    }
}