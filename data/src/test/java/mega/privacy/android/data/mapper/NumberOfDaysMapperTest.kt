package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.gateway.DeviceGateway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import java.util.Calendar
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NumberOfDaysMapperTest {
    private val deviceGateway = mock<DeviceGateway>()
    private lateinit var underTest: NumberOfDaysMapper

    @BeforeAll
    fun setup() {
        underTest = NumberOfDaysMapper(deviceGateway)
    }

    @Test
    fun `test that number of days mapper is invoked should return correct days`() {
        for (i in 0 until 20) {
            val dayDiff = Random.nextInt(from = 1, until = 100)
            val currentTimeInMillis = Calendar.getInstance().timeInMillis
            val testDateTimeStamp = Calendar.getInstance().apply {
                timeInMillis = currentTimeInMillis
                add(Calendar.DATE, dayDiff)
            }.timeInMillis

            assertThat(underTest(testDateTimeStamp, currentTimeInMillis)).isEqualTo(dayDiff)
        }
    }
}