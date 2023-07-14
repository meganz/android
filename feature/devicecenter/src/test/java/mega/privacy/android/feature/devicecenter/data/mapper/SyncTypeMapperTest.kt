package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.data.entity.SyncType
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
 * Test class for [SyncTypeMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncTypeMapperTest {
    private lateinit var underTest: SyncTypeMapper

    @BeforeAll
    fun setUp() {
        underTest = SyncTypeMapper()
    }

    @ParameterizedTest(name = "when sdkType is {0}, then syncType is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(sdkType: Int, syncType: SyncType) {
        assertThat(underTest(sdkType)).isEqualTo(syncType)
    }

    @Test
    fun `test that an unknown value throws an exception`() {
        assertThrows<IllegalArgumentException> { underTest(12345) }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(MegaApiJava.BACKUP_TYPE_INVALID, SyncType.INVALID),
        Arguments.of(MegaApiJava.BACKUP_TYPE_TWO_WAY_SYNC, SyncType.TWO_WAY_SYNC),
        Arguments.of(MegaApiJava.BACKUP_TYPE_UP_SYNC, SyncType.UP_SYNC),
        Arguments.of(MegaApiJava.BACKUP_TYPE_DOWN_SYNC, SyncType.DOWN_SYNC),
        Arguments.of(MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS, SyncType.CAMERA_UPLOADS),
        Arguments.of(MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS, SyncType.MEDIA_UPLOADS),
    )
}