package mega.privacy.android.app.meeting

import android.widget.TextView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.OnOffFab
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.utils.StringResourcesUtils.getString

/**
 * Should observer the changing of earphone and update the UI
 */
class SpeakerButtonViewHolder(
    private val speakerFab: OnOffFab,
    private val speakerLabel: TextView,
    private val switchCallback: (AppRTCAudioManager.AudioDevice) -> Boolean
) {
    private var currentDevice = AppRTCAudioManager.AudioDevice.EARPIECE
    private var wiredHeadsetConnected = false
    private var bluetoothConnected = false

    init {
        currentDevice = MegaApplication.getInstance().audioManager.selectedAudioDevice
        updateAppearance()

        speakerFab.setOnClickListener {
            when {
                headphoneConnected() && currentDevice == AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                    switchToHeadphone()
                }
                headphoneConnected() -> {
                    switchToDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)
                }
                currentDevice == AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                    switchToDevice(AppRTCAudioManager.AudioDevice.EARPIECE)
                }
                else -> {
                    switchToDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)
                }
            }
        }
    }

    fun onHeadphoneConnected(wiredHeadset: Boolean, bluetooth: Boolean) {
        val oldHeadphoneConnected = headphoneConnected()

        wiredHeadsetConnected = wiredHeadset
        bluetoothConnected = bluetooth

        val newHeadphoneConnected = headphoneConnected()

        when {
            !oldHeadphoneConnected && newHeadphoneConnected -> {
                switchToHeadphone(false)
            }
            oldHeadphoneConnected && !newHeadphoneConnected -> {
                if (currentDevice == AppRTCAudioManager.AudioDevice.SPEAKER_PHONE) {
                    switchToDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)
                } else {
                    switchToDevice(AppRTCAudioManager.AudioDevice.EARPIECE)
                }
            }
        }
    }

    private fun switchToHeadphone(fireCallback: Boolean = true) {
        switchToDevice(
            if (wiredHeadsetConnected) AppRTCAudioManager.AudioDevice.WIRED_HEADSET
            else AppRTCAudioManager.AudioDevice.BLUETOOTH,
            fireCallback
        )
    }

    private fun switchToDevice(
        device: AppRTCAudioManager.AudioDevice,
        fireCallback: Boolean = true
    ) {

        if (fireCallback) {
            if (switchCallback(device)) {
                currentDevice = device
                MegaApplication.getInstance().audioManager.selectAudioDevice(
                    currentDevice,
                    false
                )
                updateAppearance()
            }
        }
    }

    private fun headphoneConnected() = wiredHeadsetConnected || bluetoothConnected

    private fun updateAppearance() {
        when (currentDevice) {
            AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                speakerFab.isOn = true
                speakerFab.setOnIcon(R.drawable.ic_speaker_on)
                speakerLabel.text = getString(R.string.general_speaker)
            }
            AppRTCAudioManager.AudioDevice.WIRED_HEADSET,
            AppRTCAudioManager.AudioDevice.BLUETOOTH -> {
                speakerFab.isOn = true
                speakerFab.setOnIcon(R.drawable.ic_headphone)
                speakerLabel.text = getString(R.string.general_headphone)
            }
            else -> {
                speakerFab.isOn = false
                speakerLabel.text = getString(R.string.general_speaker)
            }
        }
    }
}
