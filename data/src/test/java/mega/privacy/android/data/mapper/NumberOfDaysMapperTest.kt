package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Calendar
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NumberOfDaysMapperTest {
    private lateinit var underTest: NumberOfDaysMapper

    @BeforeAll
    fun setup() {
        underTest = NumberOfDaysMapper()
    }

    @Test
    fun `test that number of days mapper is invoked should return correct days`() {
        val dayDiff = Random.nextInt(from = 1, until = 5)
        val testDateTimeStamp = Calendar.getInstance().apply {
            add(Calendar.DATE, dayDiff)
        }.timeInMillis

        assertThat(underTest(testDateTimeStamp)).isEqualTo(dayDiff)
    }
}