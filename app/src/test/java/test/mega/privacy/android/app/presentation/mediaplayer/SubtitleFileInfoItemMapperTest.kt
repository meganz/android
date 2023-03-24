package test.mega.privacy.android.app.presentation.mediaplayer

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.mediaplayer.mapper.SubtitleFileInfoItemMapper
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import org.junit.Before
import org.junit.Test

internal class SubtitleFileInfoItemMapperTest {
    private lateinit var underTest: SubtitleFileInfoItemMapper

    @Before
    fun setUp() {
        underTest = SubtitleFileInfoItemMapper()
    }

    @Test
    fun `test that subtitle file info item can be mapped correctly`() {
        val expectedId: Long = 1234567
        val expectedSubtitleName = "test.srt"
        val expectedSubtitleUrl = "test.com"
        val expectedSubtitleParentName = "root"

        val expectedSubtitleFileInfo = SubtitleFileInfo(
            id = expectedId,
            name = expectedSubtitleName,
            url = expectedSubtitleUrl,
            parentName = expectedSubtitleParentName
        )

        assertThat(
            underTest(isSelected = false, expectedSubtitleFileInfo)
        ).isEqualTo(
            SubtitleFileInfoItem(
                false,
                expectedSubtitleFileInfo
            )
        )
    }
}