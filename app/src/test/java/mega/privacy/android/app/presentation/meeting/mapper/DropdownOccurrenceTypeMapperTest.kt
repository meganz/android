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
class DropdownOccurrenceTypeMapperTest {
    private lateinit var underTest: DropdownOccurrenceTypeMapper

    @Before
    fun setUp() {
        underTest = DropdownOccurrenceTypeMapper()
    }

    @Test
    fun `test that DropdownOccurrenceType Day maps to OccurrenceFrequencyType Daily`() =
        runTest {
            Truth.assertThat(
                underTest(
                    DropdownOccurrenceType.Day
                )
            ).isEqualTo(OccurrenceFrequencyType.Daily)
        }

    @Test
    fun `test that DropdownOccurrenceType Week maps to OccurrenceFrequencyType Weekly`() =
        runTest {
            Truth.assertThat(
                underTest(
                    DropdownOccurrenceType.Week
                )
            ).isEqualTo(OccurrenceFrequencyType.Weekly)
        }

    @Test
    fun `test that DropdownOccurrenceType Month maps to OccurrenceFrequencyType Monthly`() =
        runTest {
            Truth.assertThat(
                underTest(
                    DropdownOccurrenceType.Month
                )
            ).isEqualTo(OccurrenceFrequencyType.Monthly)
        }
}