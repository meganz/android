package mega.privacy.android.data.mapper.backup

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.backup.BackupInfoHeartbeatStatusMapper.Companion.BACKUP_STATUS_STALLED
import mega.privacy.android.domain.entity.backup.BackupInfoHeartbeatStatus
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
 * Test class for [BackupInfoHeartbeatStatusMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupInfoHeartbeatStatusMapperTest {
    private lateinit var underTest: BackupInfoHeartbeatStatusMapper

    @BeforeAll
    fun setUp() {
        underTest = BackupInfoHeartbeatStatusMapper()
    }

    @ParameterizedTest(name = "when sdkHeartbeatStatus is {0}, then backupInfoHeartbeatStatus is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(
        sdkHeartbeatStatus: Int,
        backupInfoHeartbeatStatus: BackupInfoHeartbeatStatus,
    ) {
        assertThat(underTest(sdkHeartbeatStatus)).isEqualTo(backupInfoHeartbeatStatus)
    }

    @Test
    fun `test that an unknown value throws an exception`() {
        assertThrows<IllegalArgumentException> { underTest(12345) }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            MegaBackupInfo.BACKUP_STATUS_NOT_INITIALIZED,
            BackupInfoHeartbeatStatus.NOT_INITIALIZED,
        ),
        Arguments.of(MegaBackupInfo.BACKUP_STATUS_UPTODATE, BackupInfoHeartbeatStatus.UPTODATE),
        Arguments.of(MegaBackupInfo.BACKUP_STATUS_SYNCING, BackupInfoHeartbeatStatus.SYNCING),
        Arguments.of(MegaBackupInfo.BACKUP_STATUS_PENDING, BackupInfoHeartbeatStatus.PENDING),
        Arguments.of(MegaBackupInfo.BACKUP_STATUS_INACTIVE, BackupInfoHeartbeatStatus.INACTIVE),
        Arguments.of(MegaBackupInfo.BACKUP_STATUS_UNKNOWN, BackupInfoHeartbeatStatus.UNKNOWN),
        Arguments.of(BACKUP_STATUS_STALLED, BackupInfoHeartbeatStatus.STALLED),
    )
}