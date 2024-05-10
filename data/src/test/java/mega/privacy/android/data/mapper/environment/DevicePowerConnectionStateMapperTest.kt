package mega.privacy.android.data.mapper.environment

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.environment.DevicePowerConnectionState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [DevicePowerConnectionStateMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DevicePowerConnectionStateMapperTest {

    private lateinit var underTest: DevicePowerConnectionStateMapper

    @BeforeAll
    fun setUp() {
        underTest = DevicePowerConnectionStateMapper()
    }

    @ParameterizedTest(name = "when the power type is {0}, then the device power connection state is {1}")
    @MethodSource("provideParameters")
    fun `test that the correct device power connection state is mapped`(
        powerType: String?,
        devicePowerConnectionState: DevicePowerConnectionState,
    ) {
        assertThat(underTest(powerType)).isEqualTo(devicePowerConnectionState)
    }

    @Test
    fun `test that a non matching power type returns an unknown device power connection state`() {
        assertThat(underTest("UNKNOWN")).isEqualTo(DevicePowerConnectionState.Unknown)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(Intent.ACTION_POWER_CONNECTED, DevicePowerConnectionState.Connected),
        Arguments.of(Intent.ACTION_POWER_DISCONNECTED, DevicePowerConnectionState.Disconnected),
    )
}