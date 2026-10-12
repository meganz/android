package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.call.AudioDevice
import javax.inject.Inject

/**
 * Chooses the device that call audio must play on.
 *
 * The rules, in order:
 * 1. The device that the user selected, if it is available. A user selection of the speaker
 *    therefore wins over a connected headset.
 * 2. A connected wired headset, then a connected Bluetooth device. The user connects a headset to
 *    hear the call privately, so a headset wins over the default device.
 * 3. The default device, if it is available.
 * 4. Any other built-in device: the speaker, then the earpiece. A tablet has no earpiece, so the
 *    speaker replaces it.
 * 5. [AudioDevice.None] when no device is available.
 *
 * Rule 1 also gives the fallback when the selected device disconnects: the selection is no longer
 * available, so rules 2 to 4 choose again.
 */
class ResolveCallAudioDeviceUseCase @Inject constructor() {

    /**
     * Returns the device for call audio.
     *
     * @param available The devices that call audio can be routed to now. [AudioDevice.None] is
     * ignored.
     * @param userSelection The device that the user selected, or null when the user has not
     * selected one. [AudioDevice.None] is the same as null.
     * @param defaultDevice The device from [GetDefaultCallAudioDeviceUseCase].
     */
    operator fun invoke(
        available: Set<AudioDevice>,
        userSelection: AudioDevice?,
        defaultDevice: AudioDevice,
    ): AudioDevice {
        val candidates = listOfNotNull(userSelection) +
                AudioDevice.WiredHeadset +
                AudioDevice.Bluetooth +
                defaultDevice +
                AudioDevice.SpeakerPhone +
                AudioDevice.Earpiece
        return candidates.firstOrNull { it != AudioDevice.None && it in available }
            ?: AudioDevice.None
    }
}
