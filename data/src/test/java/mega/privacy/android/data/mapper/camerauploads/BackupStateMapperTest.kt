package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.BackupState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [BackupStateMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupStateMapperTest {
    private lateinit var underTest: BackupStateMapper

    @BeforeAll
    fun setUp() {
        underTest = BackupStateMapper()
    }

    @ParameterizedTest(name = "when backupTypeInt is {0}, then backupState is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(backupTypeInt: Int, backupState: BackupState) {
        assertThat(underTest(backupTypeInt)).isEqualTo(backupState)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(-1, BackupState.INVALID),
        Arguments.of(0, BackupState.NOT_INITIALIZED),
        Arguments.of(1, BackupState.ACTIVE),
        Arguments.of(2, BackupState.FAILED),
        Arguments.of(3, BackupState.TEMPORARILY_DISABLED),
        Arguments.of(4, BackupState.DISABLED),
        Arguments.of(5, BackupState.PAUSE_UPLOADS),
        Arguments.of(6, BackupState.PAUSE_DOWNLOADS),
        Arguments.of(7, BackupState.PAUSE_ALL),
        Arguments.of(8, BackupState.DELETED),
    )
}