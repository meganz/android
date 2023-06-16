package mega.privacy.android.app.fragments.settingsFragments

import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.ChatNotificationsPreferencesActivity
import mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_DND
import mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_NOTIFICATIONS
import mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_SOUND
import mega.privacy.android.app.constants.SettingsConstants.KEY_CHAT_VIBRATE
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_OFF
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_ON
import timber.log.Timber

/**
 * The fragment for chat notifications of settings
 */
class SettingsChatNotificationsFragment : SettingsBaseFragment() {
    private var chatSettings: ChatSettings?
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
                            context as ChatNotificationsPreferencesActivity, null
                        )
                    }
                    false
                }
        }

        updateSwitch()
        megaChatApi.signalPresenceActivity()
    }

    /**
     * Method to update the UI items when the Push notification Settings change.
     */
    fun updateSwitch() {
        val pushNotificationSettings =
            getPushNotificationSettingManagement().pushNotificationSetting

        val option = pushNotificationSettings?.let {
            if (it.isGlobalChatsDndEnabled) {
                if (it.globalChatsDnd == 0L)
                    Constants.NOTIFICATIONS_DISABLED
                else
                    Constants.NOTIFICATIONS_DISABLED_X_TIME
            } else {
                Constants.NOTIFICATIONS_ENABLED
            }
        } ?: Constants.NOTIFICATIONS_ENABLED

        if (option == Constants.NOTIFICATIONS_DISABLED) {
            chatDndSwitch?.let {
                preferenceScreen.removePreference(it)
            }
            return
        }
        chatNotificationsSwitch?.isChecked = option == Constants.NOTIFICATIONS_ENABLED
        if (chatSettings == null) {
            chatSettings = dbH.chatSettings
        }
        val isVibrationEnabled =
            chatSettings?.vibrationEnabled == null || chatSettings?.vibrationEnabled.toBoolean()
        dbH.setVibrationEnabledChat(isVibrationEnabled.toString())
        chatSettings = chatSettings?.copy(vibrationEnabled = isVibrationEnabled.toString())
        chatVibrateSwitch?.isChecked = isVibrationEnabled
        if (TextUtil.isTextEmpty(chatSettings?.notificationsSound)) {
            val defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_NOTIFICATION
            )
            val defaultSound = RingtoneManager.getRingtone(context, defaultSoundUri)
            chatSoundPreference?.summary =
                if (defaultSound == null) getString(R.string.settings_chat_silent_sound_not) else defaultSound.getTitle(
                    context
                )
        } else if (chatSettings?.notificationsSound == Constants.INVALID_OPTION) {
            chatSoundPreference?.summary = getString(R.string.settings_chat_silent_sound_not)
        } else {
            val soundString = chatSettings?.notificationsSound
            if (soundString == "true") {
                val defaultSoundUri2 =
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val defaultSound2 = RingtoneManager.getRingtone(context, defaultSoundUri2)
                chatSoundPreference?.summary = defaultSound2.getTitle(context)
                dbH.setNotificationSoundChat(defaultSoundUri2.toString())
            } else {
                val sound = RingtoneManager.getRingtone(context, Uri.parse(soundString))
                if (sound != null) {
                    chatSoundPreference?.summary = sound.getTitle(context)
                } else {
                    Timber.w("Sound is null")
                }
            }
        }
        chatDndSwitch?.let {
            preferenceScreen.addPreference(it)
            if (option == Constants.NOTIFICATIONS_ENABLED) {
                it.isChecked = false
                it.summary = getString(R.string.mute_chatroom_notification_option_off)
            } else {
                it.isChecked = true
                pushNotificationSettings?.globalChatsDnd?.let { timestampMute ->
                    it.summary = TimeUtils.getCorrectStringDependingOnOptionSelected(
                        timestampMute,
                        requireContext()
                    )
                }
            }
        }

        chatSoundPreference?.let {
            preferenceScreen.addPreference(it)
        }
        chatVibrateSwitch?.let {
            preferenceScreen.addPreference(it)
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
            KEY_CHAT_VIBRATE ->
                if (chatSettings?.vibrationEnabled == null
                    || chatSettings?.vibrationEnabled.toBoolean()
                ) {
                    dbH.setVibrationEnabledChat(VIBRATION_OFF)
                    chatSettings = chatSettings?.copy(vibrationEnabled = VIBRATION_OFF)
                } else {
                    dbH.setVibrationEnabledChat(VIBRATION_ON)
                    chatSettings = chatSettings?.copy(vibrationEnabled = VIBRATION_ON)
                }
            KEY_CHAT_SOUND ->
                (context as? ChatNotificationsPreferencesActivity)
                    ?.changeSound(chatSettings?.notificationsSound)
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
        if (chatSettings == null) {
            chatSettings = ChatSettings()
            chatSettings = chatSettings?.copy(notificationsSound = chosenSound)
            dbH.chatSettings = chatSettings
        } else {
            chatSettings = chatSettings?.copy(notificationsSound = chosenSound)
            dbH.setNotificationSoundChat(chosenSound)
        }
    }

    init {
        chatSettings = dbH.chatSettings
    }
}