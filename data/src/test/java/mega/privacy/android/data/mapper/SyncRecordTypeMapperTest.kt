package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.SyncRecordType
import org.junit.Test

/**
 * SyncRecordType mapper test
 */
class SyncRecordTypeMapperTest {
    private val underTest = ::toSyncRecordType

    @Test
    fun `test that mapper returns correct value when input is type any`() {
        assertThat(underTest(-1)).isEqualTo(SyncRecordType.TYPE_ANY)
    }

    @Test
    fun `test that mapper returns correct value when input is type photo`() {
        assertThat(underTest(1)).isEqualTo(SyncRecordType.TYPE_PHOTO)
    }

    @Test
    fun `test that mapper returns correct value when input is type video`() {
        assertThat(underTest(2)).isEqualTo(SyncRecordType.TYPE_VIDEO)
    }

    @Test
    fun `test that mapper returns null value when input is invalid integer`() {
        assertThat(underTest(666)).isEqualTo(null)
    }
}
