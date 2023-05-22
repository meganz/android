package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.SyncRecordType
import org.junit.Before
import org.junit.Test

/**
 * Test class for [SyncRecordTypeIntMapper]
 */
class SyncRecordTypeIntMapperTest {
    private lateinit var underTest: SyncRecordTypeIntMapper

    @Before
    fun setUp() {
        underTest = SyncRecordTypeIntMapper()
    }

    @Test
    fun `test that integer values are correctly mapped`() {
        val expectedResults = HashMap<SyncRecordType, Int>().apply {
            put(SyncRecordType.TYPE_ANY, -1)
            put(SyncRecordType.TYPE_PHOTO, 1)
            put(SyncRecordType.TYPE_VIDEO, 2)
        }

        expectedResults.forEach { (syncRecordType, value) ->
            assertThat(underTest(syncRecordType)).isEqualTo(value)
        }
    }
}
