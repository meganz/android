package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.SyncRecordType
import org.junit.Test

/**
 * SyncRecordType Int mapper test
 */
class SyncRecordTypeIntMapperTest {
    private val underTest = ::toSyncRecordTypeInt

    @Test
    fun `test that mapper returns correct value when input is type any`() {
        assertThat(underTest(SyncRecordType.TYPE_ANY)).isEqualTo(-1)
    }

    @Test
    fun `test that mapper returns correct value when input is type photo`() {
        assertThat(underTest(SyncRecordType.TYPE_PHOTO)).isEqualTo(1)
    }

    @Test
    fun `test that mapper returns correct value when input is type video`() {
        assertThat(underTest(SyncRecordType.TYPE_VIDEO)).isEqualTo(2)
    }
}
