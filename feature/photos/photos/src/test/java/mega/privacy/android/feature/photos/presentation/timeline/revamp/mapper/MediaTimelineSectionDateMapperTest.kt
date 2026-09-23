package mega.privacy.android.feature.photos.presentation.timeline.revamp.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.TimeZone

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MediaTimelineSectionDateMapperTest {

    private val underTest = MediaTimelineSectionDateMapper()

    private val originalTimeZone = TimeZone.getDefault()

    /**
     * Madrid was CET (+01:00) in January 2000 and is CEST (+02:00) in summer, so a section fetched
     * with the current offset carries a [MediaTimelineSection.startDate] whose system-zone reading
     * falls on the previous day. Every case below uses that zone so a regression shows up.
     */
    @BeforeAll
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
    }

    @AfterAll
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `test that a day group id maps to that day`() {
        val actual = underTest(section(groupId = "2000-01-01"))

        assertThat(actual).isEqualTo(LocalDate.of(2000, 1, 1))
    }

    @Test
    fun `test that a month group id maps to the first of the month`() {
        val actual = underTest(section(groupId = "2000-01"))

        assertThat(actual).isEqualTo(LocalDate.of(2000, 1, 1))
    }

    @Test
    fun `test that a year group id maps to the first of the year`() {
        val actual = underTest(section(groupId = "2000"))

        assertThat(actual).isEqualTo(LocalDate.of(2000, 1, 1))
    }

    @Test
    fun `test that a group id is preferred over the start date when the two disagree`() {
        val actual = underTest(section(groupId = "2000-01-01"))

        assertThat(actual).isNotEqualTo(LocalDate.of(1999, 12, 31))
    }

    @Test
    fun `test that a malformed group id falls back to the start date`() {
        val actual = underTest(section(groupId = "not-a-date"))

        assertThat(actual).isEqualTo(LocalDate.of(1999, 12, 31))
    }

    @Test
    fun `test that an empty group id falls back to the start date`() {
        val actual = underTest(section(groupId = ""))

        assertThat(actual).isEqualTo(LocalDate.of(1999, 12, 31))
    }

    @Test
    fun `test that an out of range group id falls back to the start date`() {
        val actual = underTest(section(groupId = "2000-13-01"))

        assertThat(actual).isEqualTo(LocalDate.of(1999, 12, 31))
    }

    private fun section(groupId: String) = MediaTimelineSection(
        groupId = groupId,
        startDate = JANUARY_2000_START_AT_SUMMER_OFFSET,
        endDate = JANUARY_2000_START_AT_SUMMER_OFFSET + 86_400L,
        count = 1L,
    )

    private companion object {
        /**
         * What the SDK reports for the 2000-01-01 bucket when the app sends today's summer offset:
         * local midnight minus "+02:00", i.e. 1999-12-31 22:00 UTC. Read back through Madrid's
         * January offset (+01:00) this is 1999-12-31 23:00 — the previous day.
         */
        const val JANUARY_2000_START_AT_SUMMER_OFFSET = 946_677_600L
    }
}
