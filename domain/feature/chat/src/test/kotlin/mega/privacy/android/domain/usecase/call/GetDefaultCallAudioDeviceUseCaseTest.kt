package mega.privacy.android.domain.usecase.call

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.call.CallAudioScenario
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDefaultCallAudioDeviceUseCaseTest {

    private lateinit var underTest: GetDefaultCallAudioDeviceUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetDefaultCallAudioDeviceUseCase()
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("defaults")
    fun `test that invoke returns the default device for the scenario`(
        scenario: CallAudioScenario,
        expected: AudioDevice,
    ) {
        assertThat(underTest(scenario)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(CallAudioScenario::class)
    fun `test that invoke never returns none or a headset`(scenario: CallAudioScenario) {
        assertThat(underTest(scenario)).isAnyOf(AudioDevice.SpeakerPhone, AudioDevice.Earpiece)
    }

    private fun defaults(): Stream<Arguments> = Stream.of(
        Arguments.of(CallAudioScenario.AudioCall, AudioDevice.Earpiece),
        Arguments.of(CallAudioScenario.VideoCall, AudioDevice.SpeakerPhone),
        Arguments.of(CallAudioScenario.MeetingOnboarding, AudioDevice.SpeakerPhone),
    )
}
