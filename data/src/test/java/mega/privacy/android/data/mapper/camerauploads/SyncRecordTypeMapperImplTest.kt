package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.SyncRecordType
import org.junit.Before
import org.junit.Test

/**
 * Test class for [SyncRecordTypeMapper]
 */
class SyncRecordTypeMapperImplTest {
    private lateinit var underTest: SyncRecordTypeMapper

    @Before
    fun setUp() {
        underTest = SyncRecordTypeMapperImpl()
    }

    @Test
    fun `test that SyncRecordType can be mapped correctly`() {
        val expectedResults = HashMap<Int, SyncRecordType?>().apply {
            put(-2, null)
            put(-1, SyncRecordType.TYPE_ANY)
            put(1, SyncRecordType.TYPE_PHOTO)
            put(2, SyncRecordType.TYPE_VIDEO)
        }

        expectedResults.forEach { (value, syncRecordType) ->
            assertThat(underTest(value)).isEqualTo(syncRecordType)
        }
    }
}