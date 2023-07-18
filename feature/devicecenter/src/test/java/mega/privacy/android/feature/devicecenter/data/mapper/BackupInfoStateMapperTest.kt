package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoState
import nz.mega.sdk.MegaBackupInfo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [BackupInfoStateMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupInfoStateMapperTest {
    private lateinit var underTest: BackupInfoStateMapper

    @BeforeAll
    fun setUp() {
        underTest = BackupInfoStateMapper()
    }

    @ParameterizedTest(name = "when sdkState is {0}, then backupInfoState is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(sdkState: Int, backupInfoState: BackupInfoState) {
        assertThat(underTest(sdkState)).isEqualTo(backupInfoState)
    }

    @Test
    fun `test that an unknown value throws an exception`() {
        assertThrows<IllegalArgumentException> {
            underTest(12345)
        }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(MegaBackupInfo.BACKUP_STATE_NOT_INITIALIZED, BackupInfoState.NOT_INITIALIZED),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_ACTIVE, BackupInfoState.ACTIVE),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_FAILED, BackupInfoState.FAILED),
        Arguments.of(
            MegaBackupInfo.BACKUP_STATE_TEMPORARY_DISABLED,
            BackupInfoState.TEMPORARY_DISABLED,
        ),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_DISABLED, BackupInfoState.DISABLED),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_PAUSE_UP, BackupInfoState.PAUSE_UP),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_PAUSE_DOWN, BackupInfoState.PAUSE_DOWN),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_PAUSE_FULL, BackupInfoState.PAUSE_FULL),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_DELETED, BackupInfoState.DELETED),
    )
}