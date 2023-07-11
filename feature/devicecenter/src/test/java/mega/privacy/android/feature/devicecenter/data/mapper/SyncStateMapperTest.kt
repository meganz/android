package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.data.entity.SyncState
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
 * Test class for [SyncStateMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncStateMapperTest {
    private lateinit var underTest: SyncStateMapper

    @BeforeAll
    fun setUp() {
        underTest = SyncStateMapper()
    }

    @ParameterizedTest(name = "when sdkState is {0}, then syncState is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(sdkState: Int, syncState: SyncState) {
        assertThat(underTest(sdkState)).isEqualTo(syncState)
    }

    @Test
    fun `test that an unknown value throws an exception`() {
        assertThrows<IllegalArgumentException> {
            underTest(12345)
        }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(MegaBackupInfo.BACKUP_STATE_NOT_INITIALIZED, SyncState.NOT_INITIALIZED),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_ACTIVE, SyncState.ACTIVE),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_FAILED, SyncState.FAILED),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_TEMPORARY_DISABLED, SyncState.TEMPORARY_DISABLED),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_DISABLED, SyncState.DISABLED),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_PAUSE_UP, SyncState.PAUSE_UP),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_PAUSE_DOWN, SyncState.PAUSE_DOWN),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_PAUSE_FULL, SyncState.PAUSE_FULL),
        Arguments.of(MegaBackupInfo.BACKUP_STATE_DELETED, SyncState.DELETED),
    )
}