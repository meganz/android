package mega.privacy.android.data.mapper.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import nz.mega.sdk.MegaDateSection
import nz.mega.sdk.MegaDateSectionList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MediaTimelineSectionMapperTest {

    private val underTest = MediaTimelineSectionMapper()

    @Test
    fun `test that each sdk section is mapped to a media timeline section in order`() {
        val firstSection = createSdkSection(
            groupId = "May 2026",
            startDate = 100L,
            endDate = 200L,
            count = 5L,
        )
        val secondSection = createSdkSection(
            groupId = "April 2026",
            startDate = 10L,
            endDate = 90L,
            count = 3L,
        )
        val sectionList = mock<MegaDateSectionList> {
            on { size() } doReturn 2
            on { get(0) } doReturn firstSection
            on { get(1) } doReturn secondSection
        }

        val result = underTest(sectionList)

        assertThat(result).containsExactly(
            MediaTimelineSection(groupId = "May 2026", startDate = 100L, endDate = 200L, count = 5L),
            MediaTimelineSection(groupId = "April 2026", startDate = 10L, endDate = 90L, count = 3L),
        ).inOrder()
    }

    @Test
    fun `test that an empty section list returns an empty list`() {
        val sectionList = mock<MegaDateSectionList> {
            on { size() } doReturn 0
        }

        val result = underTest(sectionList)

        assertThat(result).isEmpty()
    }

    private fun createSdkSection(
        groupId: String = "group",
        startDate: Long = 0L,
        endDate: Long = 0L,
        count: Long = 0L,
    ): MegaDateSection = mock {
        on { this.groupId } doReturn groupId
        on { this.startDate } doReturn startDate
        on { this.endDate } doReturn endDate
        on { this.count } doReturn count
    }
}
