package mega.privacy.android.app.presentation.meeting.mapper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class OccurrenceFrequencyTypeMapperTest {
    private lateinit var underTest: OccurrenceFrequencyTypeMapper

    @Before
    fun setUp() {
        underTest = OccurrenceFrequencyTypeMapper()
    }

    @Test
    fun `test that OccurrenceFrequencyType Daily maps to DropdownOccurrenceType Day`() =
        runTest {
            Truth.assertThat(
                underTest(
                    OccurrenceFrequencyType.Daily
                )
            ).isEqualTo(DropdownOccurrenceType.Day)
        }

    @Test
    fun `test that OccurrenceFrequencyType Weekly maps to DropdownOccurrenceType Week`() =
        runTest {
            Truth.assertThat(
                underTest(
                    OccurrenceFrequencyType.Weekly
                )
            ).isEqualTo(DropdownOccurrenceType.Week)
        }

    @Test
    fun `test that OccurrenceFrequencyType Monthly maps to DropdownOccurrenceType Month`() =
        runTest {
            Truth.assertThat(
                underTest(
                    OccurrenceFrequencyType.Monthly
                )
            ).isEqualTo(DropdownOccurrenceType.Month)
        }

    @Test
    fun `test that OccurrenceFrequencyType Invalid maps to null`() =
        runTest {
            Truth.assertThat(
                underTest(
                    OccurrenceFrequencyType.Invalid
                )
            ).isEqualTo(null)
        }
}