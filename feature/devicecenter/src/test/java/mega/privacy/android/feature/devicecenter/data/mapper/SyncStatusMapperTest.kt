package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.data.entity.SyncStatus
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
 * Test class for [SyncStatusMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncStatusMapperTest {
    private lateinit var underTest: SyncStatusMapper

    @BeforeAll
    fun setUp() {
        underTest = SyncStatusMapper()
    }

    @ParameterizedTest(name = "when sdkStatus is {0}, then syncStatus is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(sdkStatus: Int, syncStatus: SyncStatus) {
        assertThat(underTest(sdkStatus)).isEqualTo(syncStatus)
    }

    @Test
    fun `test that an unknown value throws an exception`() {
        assertThrows<IllegalArgumentException> { underTest(12345) }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(MegaBackupInfo.BACKUP_STATUS_NOT_INITIALIZED, SyncStatus.NOT_INITIALIZED),
        Arguments.of(MegaBackupInfo.BACKUP_STATUS_UPTODATE, SyncStatus.UPTODATE),
        Arguments.of(MegaBackupInfo.BACKUP_STATUS_SYNCING, SyncStatus.SYNCING),
        Arguments.of(MegaBackupInfo.BACKUP_STATUS_PENDING, SyncStatus.PENDING),
        Arguments.of(MegaBackupInfo.BACKUP_STATUS_INACTIVE, SyncStatus.INACTIVE),
        Arguments.of(MegaBackupInfo.BACKUP_STATUS_UNKNOWN, SyncStatus.UNKNOWN),
    )
}