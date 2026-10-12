package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.call.CallAudioScenario
import javax.inject.Inject

/**
 * Returns the built-in device that call audio uses when the user has not selected a device and no
 * headset is connected.
 *
 * The result is always [AudioDevice.SpeakerPhone] or [AudioDevice.Earpiece]. Use
 * [ResolveCallAudioDeviceUseCase] to apply it to the devices that are available.
 */
class GetDefaultCallAudioDeviceUseCase @Inject constructor() {

    /**
     * Returns the default device for [scenario]: the earpiece for an audio call, because the user
     * holds the phone to the ear, and the speaker otherwise, because the user looks at the screen.
     */
    operator fun invoke(scenario: CallAudioScenario): AudioDevice = when (scenario) {
        CallAudioScenario.AudioCall -> AudioDevice.Earpiece
        CallAudioScenario.VideoCall,
        CallAudioScenario.MeetingOnboarding,
            -> AudioDevice.SpeakerPhone
    }
}
