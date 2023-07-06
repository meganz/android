package mega.privacy.android.data.mapper.sync

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.sync.SyncType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [SyncTypeMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncTypeMapperTest {
    private lateinit var underTest: SyncTypeMapper

    @BeforeAll
    fun setUp() {
        underTest = SyncTypeMapper()
    }

    @ParameterizedTest(name = " when type is {0}, then syncType is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(type: Int, syncType: SyncType) {
        assertThat(underTest(type)).isEqualTo(syncType)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(-1, SyncType.INVALID),
        Arguments.of(0, SyncType.TWO_WAY),
        Arguments.of(1, SyncType.UP_SYNC),
        Arguments.of(2, SyncType.DOWN_SYNC),
        Arguments.of(3, SyncType.CAMERA_UPLOAD),
        Arguments.of(4, SyncType.MEDIA_UPLOAD),
        Arguments.of(5, SyncType.BACKUP_UPLOAD),
    )

    @Test
    fun `test that an unknown value throws an exception`() {
        assertThrows<IllegalArgumentException> { underTest(12345) }
    }
}