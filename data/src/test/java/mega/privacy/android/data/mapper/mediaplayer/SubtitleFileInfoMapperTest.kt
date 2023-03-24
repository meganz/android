package mega.privacy.android.data.mapper.mediaplayer

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import org.junit.Before
import org.junit.Test

class SubtitleFileInfoMapperTest {
    private lateinit var underTest: SubtitleFileInfoMapper

    @Before
    fun setUp() {
        underTest = SubtitleFileInfoMapper()
    }

    @Test
    fun `test that subtitle file info can be mapped correctly`() {
        val expectedId: Long = 1234567
        val expectedSubtitleName = "test.srt"
        val expectedSubtitleUrl = "test.com"
        val expectedSubtitleParentName = "root"

        assertThat(
            underTest(
                expectedId,
                expectedSubtitleName,
                expectedSubtitleUrl,
                expectedSubtitleParentName
            )
        )
            .isEqualTo(
                SubtitleFileInfo(
                    expectedId,
                    expectedSubtitleName,
                    expectedSubtitleUrl,
                    expectedSubtitleParentName
                )
            )
    }
}