package mega.privacy.android.app.presentation.meeting.mapper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.RecurringMeetingType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RecurringMeetingTypeMapperTest {
    private lateinit var underTest: RecurringMeetingTypeMapper

    @Before
    fun setUp() {
        underTest = RecurringMeetingTypeMapper()
    }

    @Test
    fun `test that RecurringMeetingType EveryDay maps to OccurrenceFrequencyType Daily`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurringMeetingType.EveryDay
                )
            ).isEqualTo(OccurrenceFrequencyType.Daily)
        }

    @Test
    fun `test that RecurringMeetingType EveryWeek maps to OccurrenceFrequencyType Weekly`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurringMeetingType.EveryWeek
                )
            ).isEqualTo(OccurrenceFrequencyType.Weekly)
        }

    @Test
    fun `test that RecurringMeetingType EveryMonth maps to OccurrenceFrequencyType Monthly`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurringMeetingType.EveryMonth
                )
            ).isEqualTo(OccurrenceFrequencyType.Monthly)
        }


    @Test
    fun `test that RecurringMeetingType Custom maps to OccurrenceFrequencyType Invalid`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurringMeetingType.Custom
                )
            ).isEqualTo(OccurrenceFrequencyType.Invalid)
        }

    @Test
    fun `test that RecurringMeetingType Customised maps to OccurrenceFrequencyType Invalid`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurringMeetingType.Customised
                )
            ).isEqualTo(OccurrenceFrequencyType.Invalid)
        }

    @Test
    fun `test that RecurringMeetingType Never maps to OccurrenceFrequencyType Invalid`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurringMeetingType.Never
                )
            ).isEqualTo(OccurrenceFrequencyType.Invalid)
        }
}