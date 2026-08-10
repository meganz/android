package mega.privacy.android.data.mapper.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Location
import nz.mega.sdk.MegaNodeScopeFilter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaTimelineLocationIntMapperTest {

    private val underTest = MediaTimelineLocationIntMapper()

    @ParameterizedTest(name = "when location is {0}, then the int value is {1}")
    @MethodSource("provideParameters")
    fun `test that the location maps to the correct int value`(
        location: Location,
        expected: Int,
    ) {
        val actual = underTest(location)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when int value is {1}, then the location is {0}")
    @MethodSource("provideParameters")
    fun `test that the int value maps back to the correct location`(
        expected: Location,
        value: Int,
    ) {
        val actual = underTest(value)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that an unknown int value throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> { underTest(UNKNOWN_VALUE) }
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            Location.CloudDrive,
            MegaNodeScopeFilter.LOCATION_CLOUD_DRIVE,
        ),
        Arguments.of(
            Location.CloudDriveAndVault,
            MegaNodeScopeFilter.LOCATION_CLOUD_DRIVE_AND_VAULT,
        ),
        Arguments.of(
            Location.CloudDriveVaultAndRubbish,
            MegaNodeScopeFilter.LOCATION_CLOUD_DRIVE_VAULT_AND_RUBBISH,
        ),
    )

    companion object {
        private const val UNKNOWN_VALUE = -1
    }
}
