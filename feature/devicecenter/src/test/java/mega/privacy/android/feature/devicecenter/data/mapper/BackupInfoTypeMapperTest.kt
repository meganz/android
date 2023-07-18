package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoType
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [BackupInfoTypeMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupInfoTypeMapperTest {
    private lateinit var underTest: BackupInfoTypeMapper

    @BeforeAll
    fun setUp() {
        underTest = BackupInfoTypeMapper()
    }

    @ParameterizedTest(name = "when sdkType is {0}, then backupInfoType is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(sdkType: Int, backupInfoType: BackupInfoType) {
        assertThat(underTest(sdkType)).isEqualTo(backupInfoType)
    }

    @Test
    fun `test that an unknown value throws an exception`() {
        assertThrows<IllegalArgumentException> { underTest(12345) }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(MegaApiJava.BACKUP_TYPE_INVALID, BackupInfoType.INVALID),
        Arguments.of(MegaApiJava.BACKUP_TYPE_TWO_WAY_SYNC, BackupInfoType.TWO_WAY_SYNC),
        Arguments.of(MegaApiJava.BACKUP_TYPE_UP_SYNC, BackupInfoType.UP_SYNC),
        Arguments.of(MegaApiJava.BACKUP_TYPE_DOWN_SYNC, BackupInfoType.DOWN_SYNC),
        Arguments.of(MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS, BackupInfoType.CAMERA_UPLOADS),
        Arguments.of(MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS, BackupInfoType.MEDIA_UPLOADS),
    )
}