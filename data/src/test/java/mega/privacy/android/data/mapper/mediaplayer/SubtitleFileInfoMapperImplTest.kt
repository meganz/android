package mega.privacy.android.data.mapper.mediaplayer

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import org.junit.Before
import org.junit.Test

class SubtitleFileInfoMapperImplTest {
    private lateinit var underTest: SubtitleFileInfoMapper

    @Before
    fun setUp() {
        underTest = SubtitleFileInfoMapperImpl()
    }

    @Test
    fun `test that subtitle file info can be mapped correctly`() {
        val expectedSubtitleName = "test.srt"
        val expectedSubtitleUrl = "test.com"
        val expectedSubtitlePath = "root/test.srt"

        assertThat(underTest(expectedSubtitleName, expectedSubtitleUrl, expectedSubtitlePath))
            .isEqualTo(
                SubtitleFileInfo(
                    expectedSubtitleName,
                    expectedSubtitleUrl,
                    expectedSubtitlePath
                )
            )
    }
}