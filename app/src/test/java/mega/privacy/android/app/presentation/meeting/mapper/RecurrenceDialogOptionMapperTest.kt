package mega.privacy.android.app.presentation.meeting.mapper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.RecurrenceDialogOption
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RecurrenceDialogOptionMapperTest {
    private lateinit var underTest: RecurrenceDialogOptionMapper

    @Before
    fun setUp() {
        underTest = RecurrenceDialogOptionMapper()
    }

    @Test
    fun `test that RecurrenceDialogOption EveryDay maps to OccurrenceFrequencyType Daily`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurrenceDialogOption.EveryDay
                )
            ).isEqualTo(OccurrenceFrequencyType.Daily)
        }

    @Test
    fun `test that RecurrenceDialogOption EveryWeek maps to OccurrenceFrequencyType Weekly`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurrenceDialogOption.EveryWeek
                )
            ).isEqualTo(OccurrenceFrequencyType.Weekly)
        }

    @Test
    fun `test that RecurrenceDialogOption EveryMonth maps to OccurrenceFrequencyType Monthly`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurrenceDialogOption.EveryMonth
                )
            ).isEqualTo(OccurrenceFrequencyType.Monthly)
        }


    @Test
    fun `test that RecurrenceDialogOption Custom maps to OccurrenceFrequencyType Invalid`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurrenceDialogOption.Custom
                )
            ).isEqualTo(OccurrenceFrequencyType.Invalid)
        }

    @Test
    fun `test that RecurrenceDialogOption Customised maps to OccurrenceFrequencyType Invalid`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurrenceDialogOption.Customised
                )
            ).isEqualTo(OccurrenceFrequencyType.Invalid)
        }

    @Test
    fun `test that RecurrenceDialogOption Never maps to OccurrenceFrequencyType Invalid`() =
        runTest {
            Truth.assertThat(
                underTest(
                    RecurrenceDialogOption.Never
                )
            ).isEqualTo(OccurrenceFrequencyType.Invalid)
        }
}