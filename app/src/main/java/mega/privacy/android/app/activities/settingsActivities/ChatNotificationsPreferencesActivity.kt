package mega.privacy.android.app.activities.settingsActivities

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.settingsFragments.SettingsChatNotificationsFragment
import timber.log.Timber

/**
 * ChatNotificationsPreferencesActivity
 */
class ChatNotificationsPreferencesActivity : PreferencesBaseActivity() {
    private var sttChatNotifications: SettingsChatNotificationsFragment? = null
    private lateinit var ringTonePickerResultLauncher: ActivityResultLauncher<Intent>
    private val viewModel by viewModels<ChatNotificationPreferencesViewModel>()

    /**
     * onCreate lifecycle callback
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_properties_chat_notifications_contact)
        sttChatNotifications = SettingsChatNotificationsFragment().apply {
            replaceFragment(this)
        }
        collectFlows()
        registerForActivityResultLauncher()
    }

    private fun collectFlows() {
        collectFlow(viewModel.state) { chatPreferencesState ->
            if (chatPreferencesState.isPushNotificationSettingsUpdatedEvent) {
                sttChatNotifications?.updateSwitch()
                viewModel.onConsumePushNotificationSettingsUpdateEvent()
            }
        }
    }

    private fun registerForActivityResultLauncher() {
        ringTonePickerResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra(
                        RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                        Uri::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }

                if (sttChatNotifications?.isAdded == true) {
                    sttChatNotifications?.setNotificationSound(uri)
                }
            }
    }

    /**
     * Method to change chat notification sound
     */
    fun changeSound(soundString: String?) {
        Timber.d("Sound string: $soundString")
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_TITLE,
                getString(R.string.notification_sound_title)
            )
            val uri = when {
                soundString == "-1" -> null
                soundString.isNullOrEmpty() -> RingtoneManager.getActualDefaultRingtoneUri(
                    this@ChatNotificationsPreferencesActivity, RingtoneManager.TYPE_NOTIFICATION
                )
                else -> Uri.parse(soundString)
            }
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri)
        }
        ringTonePickerResultLauncher.launch(intent)
    }
}