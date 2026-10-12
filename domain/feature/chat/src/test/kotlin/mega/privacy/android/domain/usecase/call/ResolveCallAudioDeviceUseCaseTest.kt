package mega.privacy.android.domain.usecase.call

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.call.AudioDevice.Bluetooth
import mega.privacy.android.domain.entity.call.AudioDevice.Earpiece
import mega.privacy.android.domain.entity.call.AudioDevice.None
import mega.privacy.android.domain.entity.call.AudioDevice.SpeakerPhone
import mega.privacy.android.domain.entity.call.AudioDevice.WiredHeadset
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResolveCallAudioDeviceUseCaseTest {

    private lateinit var underTest: ResolveCallAudioDeviceUseCase

    private val phone = setOf(SpeakerPhone, Earpiece)
    private val tablet = setOf(SpeakerPhone)

    @BeforeAll
    fun setUp() {
        underTest = ResolveCallAudioDeviceUseCase()
    }

    @ParameterizedTest(name = "selected {1} from {0}")
    @MethodSource("availableSelections")
    fun `test that invoke returns the user selection when it is available`(
        available: Set<AudioDevice>,
        userSelection: AudioDevice,
    ) {
        val actual = underTest(
            available = available,
            userSelection = userSelection,
            defaultDevice = Earpiece,
        )

        assertThat(actual).isEqualTo(userSelection)
    }

    @ParameterizedTest(name = "default {0}")
    @MethodSource("defaultDevices")
    fun `test that invoke returns the wired headset when no device is selected`(
        defaultDevice: AudioDevice,
    ) {
        val actual = underTest(
            available = phone + WiredHeadset + Bluetooth,
            userSelection = null,
            defaultDevice = defaultDevice,
        )

        assertThat(actual).isEqualTo(WiredHeadset)
    }

    @ParameterizedTest(name = "default {0}")
    @MethodSource("defaultDevices")
    fun `test that invoke returns the bluetooth device when no device is selected and no wired headset is available`(
        defaultDevice: AudioDevice,
    ) {
        val actual = underTest(
            available = phone + Bluetooth,
            userSelection = null,
            defaultDevice = defaultDevice,
        )

        assertThat(actual).isEqualTo(Bluetooth)
    }

    @ParameterizedTest(name = "default {0}")
    @MethodSource("defaultDevices")
    fun `test that invoke returns the default device when no device is selected and no headset is available`(
        defaultDevice: AudioDevice,
    ) {
        val actual = underTest(
            available = phone,
            userSelection = null,
            defaultDevice = defaultDevice,
        )

        assertThat(actual).isEqualTo(defaultDevice)
    }

    @ParameterizedTest(name = "lost {0}, remaining {1}, default {2} -> {3}")
    @MethodSource("unavailableSelections")
    fun `test that invoke ignores the user selection when it is not available`(
        userSelection: AudioDevice,
        available: Set<AudioDevice>,
        defaultDevice: AudioDevice,
        expected: AudioDevice,
    ) {
        val actual = underTest(
            available = available,
            userSelection = userSelection,
            defaultDevice = defaultDevice,
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that invoke ignores a user selection of none`() {
        val actual = underTest(
            available = phone + Bluetooth,
            userSelection = None,
            defaultDevice = Earpiece,
        )

        assertThat(actual).isEqualTo(Bluetooth)
    }

    @Test
    fun `test that invoke returns the speaker when the default is the earpiece and the device has no earpiece`() {
        val actual = underTest(
            available = tablet,
            userSelection = null,
            defaultDevice = Earpiece,
        )

        assertThat(actual).isEqualTo(SpeakerPhone)
    }

    @Test
    fun `test that invoke returns the earpiece when the default is the speaker and the speaker is not available`() {
        val actual = underTest(
            available = setOf(Earpiece),
            userSelection = null,
            defaultDevice = SpeakerPhone,
        )

        assertThat(actual).isEqualTo(Earpiece)
    }

    @Test
    fun `test that invoke returns none when no device is available`() {
        val actual = underTest(
            available = emptySet(),
            userSelection = SpeakerPhone,
            defaultDevice = SpeakerPhone,
        )

        assertThat(actual).isEqualTo(None)
    }

    @Test
    fun `test that invoke ignores none in the available devices`() {
        val actual = underTest(
            available = setOf(None),
            userSelection = None,
            defaultDevice = SpeakerPhone,
        )

        assertThat(actual).isEqualTo(None)
    }

    private fun defaultDevices(): Stream<Arguments> = Stream.of(
        Arguments.of(Earpiece),
        Arguments.of(SpeakerPhone),
    )

    private fun availableSelections(): Stream<Arguments> {
        val everything = phone + WiredHeadset + Bluetooth
        return Stream.of(
            Arguments.of(everything, SpeakerPhone),
            Arguments.of(everything, WiredHeadset),
            Arguments.of(everything, Bluetooth),
            Arguments.of(phone + Bluetooth, Earpiece),
            Arguments.of(phone, SpeakerPhone),
            Arguments.of(phone, Earpiece),
        )
    }

    private fun unavailableSelections(): Stream<Arguments> = Stream.of(
        Arguments.of(Bluetooth, phone, Earpiece, Earpiece),
        Arguments.of(Bluetooth, phone, SpeakerPhone, SpeakerPhone),
        Arguments.of(Bluetooth, phone + WiredHeadset, Earpiece, WiredHeadset),
        Arguments.of(WiredHeadset, phone, Earpiece, Earpiece),
        Arguments.of(WiredHeadset, phone + Bluetooth, SpeakerPhone, Bluetooth),
        Arguments.of(Earpiece, tablet, Earpiece, SpeakerPhone),
    )
}
