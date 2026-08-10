package mega.privacy.android.app.fragments.settingsFragments

import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.ChatNotificationsPreferencesActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_DND
import mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_NOTIFICATIONS
import mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_SOUND
import mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_VIBRATE
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TimeUtils
import timber.log.Timber

/**
 * The fragment for chat notifications of settings
 */
@AndroidEntryPoint
class SettingsChatNotificationsFragment : SettingsBaseFragment(),
    Preference.OnPreferenceClickListener {

    private val viewModel by viewModels<SettingsChatNotificationsViewModel>()

    private var chatNotificationsSwitch: SwitchPreferenceCompat? = null
    private var chatSoundPreference: Preference? = null
    private var chatVibrateSwitch: SwitchPreferenceCompat? = null
    private var chatDndSwitch: SwitchPreferenceCompat? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_chat_notifications)
        chatNotificationsSwitch = findPreference(KEY_CHAT_NOTIFICATIONS)
        chatSoundPreference = findPreference(KEY_CHAT_SOUND)
        chatVibrateSwitch = findPreference(KEY_CHAT_VIBRATE)
        chatDndSwitch = findPreference(KEY_CHAT_DND)

        chatNotificationsSwitch?.let {
            it.onPreferenceClickListener = this
            it.isChecked = ChatUtil.getGeneralNotification() == Constants.NOTIFICATIONS_ENABLED
        }

        chatSoundPreference?.let {
            preferenceScreen.addPreference(it)
            it.onPreferenceClickListener = this
        }

        chatVibrateSwitch?.let {
            preferenceScreen.addPreference(it)
            it.isEnabled = true
            it.onPreferenceClickListener = this
        }

        chatDndSwitch?.let {
            preferenceScreen.removePreference(it)
            it.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, _ ->
                    if ((preference as SwitchPreferenceCompat).isChecked) {
                        getPushNotificationSettingManagement().controlMuteNotifications(
                            context,
                            Constants.NOTIFICATIONS_ENABLED,
                            null
                        )
                    } else {
                        ChatUtil.createMuteNotificationsChatAlertDialog(
                            requireActivity() as ChatNotificationsPreferencesActivity, null
                        )
                    }
                    false
                }
        }

        updateSwitch()
        megaChatApi.signalPresenceActivity()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(viewModel.uiState) { state ->
            chatVibrateSwitch?.isChecked = state.isVibrationEnabled
            renderSoundSummary(state.notificationsSound)
        }
    }

    /**
     * Method to update the UI items when the Push notification Settings change.
     */
    fun updateSwitch() {
        with(getPushNotificationSettingManagement().pushNotificationSetting) {
            val isDndEnabled = this.isGlobalChatsDndEnabled
            val dndTime = this.globalChatsDnd

            chatNotificationsSwitch?.isChecked = !isDndEnabled

            chatDndSwitch?.let {
                when (dndTime) {
                    0L -> {
                        preferenceScreen.removePreference(it)
                    }

                    else -> {
                        preferenceScreen.addPreference(it)
                        it.isChecked = isDndEnabled
                        if (isDndEnabled) {
                            it.summary = TimeUtils.getCorrectStringDependingOnOptionSelected(
                                dndTime,
                                requireContext()
                            )
                        } else {
                            it.summary = getString(R.string.mute_chatroom_notification_option_off)
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates the chat sound preference summary based on the stored notification sound.
     *
     * @param notificationsSound the raw notification sound value from the UI state.
     */
    private fun renderSoundSummary(notificationsSound: String?) {
        when {
            notificationsSound.isNullOrBlank() -> {
                val defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_NOTIFICATION
                )
                val defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri)
                chatSoundPreference?.summary =
                    if (defaultSound == null) getString(R.string.settings_chat_silent_sound_not)
                    else defaultSound.getTitle(context)
            }

            notificationsSound == Constants.INVALID_OPTION -> {
                chatSoundPreference?.summary = getString(R.string.settings_chat_silent_sound_not)
            }

            notificationsSound == "true" -> {
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri)
                chatSoundPreference?.summary = defaultSound.getTitle(context)
                viewModel.setNotificationSound(defaultSoundUri.toString())
            }

            else -> {
                val sound = RingtoneManager.getRingtone(context, Uri.parse(notificationsSound))
                if (sound != null) {
                    chatSoundPreference?.summary = sound.getTitle(context)
                } else {
                    Timber.w("Sound is null")
                }
            }
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        megaChatApi.signalPresenceActivity()
        when (preference.key) {
            KEY_CHAT_NOTIFICATIONS ->
                chatNotificationsSwitch?.let {
                    getPushNotificationSettingManagement().controlMuteNotifications(
                        context,
                        if (it.isChecked)
                            Constants.NOTIFICATIONS_ENABLED
                        else
                            Constants.NOTIFICATIONS_DISABLED,
                        null
                    )
                }

            KEY_CHAT_VIBRATE -> viewModel.toggleVibration()

            KEY_CHAT_SOUND ->
                (activity as? ChatNotificationsPreferencesActivity)
                    ?.changeSound(viewModel.uiState.value.notificationsSound)
        }
        return true
    }

    /**
     * Method of updating the sound of chat notifications.
     *
     * @param uri The uri of the sound.
     */
    fun setNotificationSound(uri: Uri?) {
        var chosenSound = Constants.INVALID_OPTION
        if (uri != null) {
            val sound = RingtoneManager.getRingtone(context, uri)
            val title = sound.getTitle(context)
            if (title != null) {
                Timber.d("Title sound notification: %s", title)
                chatSoundPreference?.summary = title
            }
            chosenSound = uri.toString()
        } else {
            chatSoundPreference?.summary = getString(R.string.settings_chat_silent_sound_not)
        }
        viewModel.setNotificationSound(chosenSound)
    }
}
