package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.BackupState
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Test class for [BackupStateIntMapper]
 */
internal class BackupStateIntMapperTest {
    private val underTest = BackupStateIntMapper()

    @TestFactory
    fun `test that BackupState is mapped correctly`() =
        HashMap<BackupState, Int>().apply {
            put(BackupState.INVALID, -1)
            put(BackupState.ACTIVE, 1)
            put(BackupState.FAILED, 2)
            put(BackupState.TEMPORARILY_DISABLED, 3)
            put(BackupState.DISABLED, 4)
            put(BackupState.PAUSE_UPLOADS, 5)
            put(BackupState.PAUSE_DOWNLOADS, 6)
            put(BackupState.PAUSE_ALL, 7)
            put(BackupState.DELETED, 8)
        }.map { (backupState, value) ->
            DynamicTest.dynamicTest("test that $backupState is mapped to $value") {
                assertThat(underTest(backupState)).isEqualTo(value)
            }
        }
}
