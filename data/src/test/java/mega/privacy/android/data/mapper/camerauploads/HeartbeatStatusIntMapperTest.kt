package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import org.junit.Before
import org.junit.Test

/**
 * Test class for [HeartbeatStatusIntMapper]
 */
internal class HeartbeatStatusIntMapperTest {
    private lateinit var underTest: HeartbeatStatusIntMapper

    @Before
    fun setUp() {
        underTest = HeartbeatStatusIntMapper()
    }

    @Test
    fun `test that HeartbeatStatus is mapped correctly`() {
        val expectedResults = HashMap<HeartbeatStatus, Int>().apply {
            put(HeartbeatStatus.UP_TO_DATE, 100)
            put(HeartbeatStatus.INACTIVE, -1)
            put(HeartbeatStatus.SYNCING, 0)
            put(HeartbeatStatus.PENDING, 0)
            put(HeartbeatStatus.UNKNOWN, 0)
        }

        expectedResults.forEach { (heartbeatStatus, progressValue) ->
            assertThat(underTest(heartbeatStatus)).isEqualTo(progressValue)
        }
    }
}
