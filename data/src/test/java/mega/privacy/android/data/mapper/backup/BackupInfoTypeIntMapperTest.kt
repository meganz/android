package mega.privacy.android.data.mapper.backup

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.backup.BackupInfoType
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [BackupInfoTypeIntMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupInfoTypeIntMapperTest {
    private lateinit var underTest: BackupInfoTypeIntMapper

    @BeforeAll
    fun setUp() {
        underTest = BackupInfoTypeIntMapper()
    }

    @ParameterizedTest(name = "when backupInfoType is {1}, then sdkType is {0}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(sdkType: Int, backupInfoType: BackupInfoType) {
        Truth.assertThat(underTest(backupInfoType)).isEqualTo(sdkType)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(MegaApiJava.BACKUP_TYPE_INVALID, BackupInfoType.INVALID),
        Arguments.of(MegaApiJava.BACKUP_TYPE_TWO_WAY_SYNC, BackupInfoType.TWO_WAY_SYNC),
        Arguments.of(MegaApiJava.BACKUP_TYPE_UP_SYNC, BackupInfoType.UP_SYNC),
        Arguments.of(MegaApiJava.BACKUP_TYPE_DOWN_SYNC, BackupInfoType.DOWN_SYNC),
        Arguments.of(MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS, BackupInfoType.CAMERA_UPLOADS),
        Arguments.of(MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS, BackupInfoType.MEDIA_UPLOADS),
        Arguments.of(MegaApiJava.BACKUP_TYPE_BACKUP_UPLOAD, BackupInfoType.BACKUP_UPLOAD),
    )
}
