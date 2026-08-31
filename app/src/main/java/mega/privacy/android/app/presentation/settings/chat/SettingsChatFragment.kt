package mega.privacy.android.app.presentation.settings.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.ChatNotificationsPreferencesActivity
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.PushNotificationSettingManagement
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.settings.MegaSwitchPreference
import mega.privacy.android.app.presentation.settings.chat.imagequality.SettingsChatImageQualityActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.chat.ChatPresenceConfig
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SettingsChatFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener {

    @Inject
    lateinit var pushNotificationSettingManagement: PushNotificationSettingManagement

    private val viewModel: SettingsChatViewModel by viewModels()

    private var chatNotificationsPreference: Preference? = null
    private var statusChatListPreference: ListPreference? = null
    private var autoAwaySwitch: SwitchPreferenceCompat? = null
    private var chatAutoAwayPreference: Preference? = null
    private var chatPersistenceCheck: MegaSwitchPreference? = null
    private var enableLastGreenChatSwitch: SwitchPreferenceCompat? = null
    private var chatAttachmentsChatListPreference: ListPreference? = null
    private var richLinksSwitch: SwitchPreferenceCompat? = null

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_chat)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatNotificationsPreference =
            findPreference<Preference?>(SettingsConstants.KEY_CHAT_NOTIFICATIONS_CHAT)?.apply {
                onPreferenceClickListener = this@SettingsChatFragment
                onPreferenceChangeListener = this@SettingsChatFragment
            }

        updateNotifChat()

        statusChatListPreference =
            findPreference<ListPreference?>(SettingsConstants.KEY_CHAT_STATUS)?.apply {
                onPreferenceChangeListener = this@SettingsChatFragment
                // Set entries (display strings)
                entries = arrayOf(
                    getString(R.string.settings_chat_status_online),
                    getString(R.string.settings_chat_status_away),
                    getString(R.string.settings_chat_status_busy),
                    getString(R.string.settings_chat_status_offline),
                )
                // Set entryValues (persisted values)
                entryValues = resources.getStringArray(R.array.settings_chat_status_values)
            }

        autoAwaySwitch =
            findPreference<SwitchPreferenceCompat?>(SettingsConstants.KEY_CHAT_AUTOAWAY_SWITCH)?.apply {
                onPreferenceClickListener = this@SettingsChatFragment
            }

        chatAutoAwayPreference =
            findPreference<Preference?>(SettingsConstants.KEY_CHAT_AUTOAWAY_PREFERENCE)?.apply {
                onPreferenceClickListener = this@SettingsChatFragment
            }

        chatPersistenceCheck =
            findPreference<MegaSwitchPreference?>(SettingsConstants.KEY_CHAT_PERSISTENCE)?.apply {
                onPreferenceClickListener = this@SettingsChatFragment
            }

        enableLastGreenChatSwitch =
            findPreference<SwitchPreferenceCompat?>(SettingsConstants.KEY_CHAT_LAST_GREEN)?.apply {
                onPreferenceClickListener = this@SettingsChatFragment
            }

        findPreference<Preference?>(KEY_CHAT_IMAGE_QUALITY)?.onPreferenceClickListener =
            this@SettingsChatFragment

        chatAttachmentsChatListPreference =
            findPreference<ListPreference?>(SettingsConstants.KEY_CHAT_SEND_ORIGINALS)?.apply {
                onPreferenceChangeListener = this@SettingsChatFragment
                // Set entries (display strings)
                entries = arrayOf(
                    getString(R.string.settings_chat_upload_quality_low),
                    getString(R.string.settings_chat_upload_quality_medium),
                    getString(R.string.settings_chat_upload_quality_high),
                    getString(R.string.settings_chat_upload_quality_original),
                )
                // Set entryValues (numeric values for persistence)
                entryValues = resources.getStringArray(R.array.settings_chat_upload_quality_values)
            }

        richLinksSwitch =
            findPreference<SwitchPreferenceCompat?>(SettingsConstants.KEY_CHAT_RICH_LINK)?.apply {
                onPreferenceClickListener = this@SettingsChatFragment
                isChecked = viewModel.state.value.isRichLinkEnabled
            }

        viewModel.signalChatPresence()

        setupObservers()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { state ->
                    setOnlineOptions(viewModel.isConnected && state.rootNodeExists)

                    findPreference<Preference?>(KEY_CHAT_IMAGE_QUALITY)?.summary =
                        state.imageQuality?.title?.let { getString(it) }

                    richLinksSwitch?.apply {
                        onPreferenceClickListener = null
                        isChecked = state.isRichLinkEnabled
                        onPreferenceClickListener = this@SettingsChatFragment
                    }

                    state.chatVideoQuality?.let { quality ->
                        chatAttachmentsChatListPreference?.apply {
                            setValueIndex(quality)
                            summary = entry
                        }
                    }

                    val statusConfig = state.chatPresenceConfig
                    if (statusConfig != null) {
                        Timber.d("ChatStatus pending: ${statusConfig.isPending}, status: ${statusConfig.onlineStatus}")
                        showPresenceChatConfig(statusConfig)
                    } else {
                        waitPresenceConfig()
                    }
                }
            }
        }

        viewLifecycleOwner.collectFlow(viewModel.monitorConnectivityEvent) { isConnected ->
            setOnlineOptions(isConnected && viewModel.state.value.rootNodeExists)
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        viewModel.signalChatPresence()

        if (Util.isOffline(context)) return false

        when (preference.key) {
            SettingsConstants.KEY_CHAT_NOTIFICATIONS_CHAT -> {
                startActivity(
                    Intent(
                        requireContext(),
                        ChatNotificationsPreferencesActivity::class.java
                    )
                )
                return true
            }

            SettingsConstants.KEY_CHAT_AUTOAWAY_SWITCH -> {
                val statusConfig = viewModel.state.value.chatPresenceConfig

                if (statusConfig != null) {
                    if (statusConfig.isAutoAwayEnabled) {
                        Timber.d("Change AUTOAWAY chat to false")
                        viewModel.setAutoAwayEnabled(false)
                        chatAutoAwayPreference?.let { preferenceScreen.removePreference(it) }
                    } else {
                        Timber.d("Change AUTOAWAY chat to true")
                        viewModel.setAutoAwayEnabled(true)
                        chatAutoAwayPreference?.let { preferenceScreen.addPreference(it) }
                        chatAutoAwayPreference?.summary =
                            getString(R.string.settings_autoaway_value, 5)
                    }
                }
                return true
            }

            SettingsConstants.KEY_CHAT_AUTOAWAY_PREFERENCE -> {
                (requireActivity() as ChatPreferencesActivity).showAutoAwayValueDialog()
                return true
            }

            SettingsConstants.KEY_CHAT_PERSISTENCE -> {
                viewModel.state.value.chatPresenceConfig?.let { statusConfig ->
                    viewModel.setPresencePersist(!statusConfig.isPersist)
                }
                return true
            }

            SettingsConstants.KEY_CHAT_LAST_GREEN -> {
                (requireActivity() as ChatPreferencesActivity).enableLastGreen(
                    enableLastGreenChatSwitch!!.isChecked
                )
                return true
            }

            SettingsConstants.KEY_CHAT_RICH_LINK -> {
                viewModel.enableRichLinkPreview(richLinksSwitch!!.isChecked)
                return true
            }

            KEY_CHAT_IMAGE_QUALITY -> {
                startActivity(
                    Intent(
                        requireActivity(),
                        SettingsChatImageQualityActivity::class.java
                    )
                )
                return true
            }
        }

        return false
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (Util.isOffline(context)) return false

        when (preference.key) {
            SettingsConstants.KEY_CHAT_NOTIFICATIONS_CHAT -> {
                updateNotifChat()
                return true
            }

            SettingsConstants.KEY_CHAT_STATUS -> {
                statusChatListPreference?.summary = statusChatListPreference?.entry
                viewModel.setUserStatus((newValue as String).toInt())
                return true
            }

            SettingsConstants.KEY_CHAT_SEND_ORIGINALS -> {
                val newStatus = (newValue as String).toInt()
                viewModel.setChatVideoQuality(newStatus)
                chatAttachmentsChatListPreference?.setValueIndex(newStatus)
                chatAttachmentsChatListPreference?.summary =
                    chatAttachmentsChatListPreference?.entry
                return true
            }
        }

        return false
    }

    /**
     * Method for updating chat notifications.
     */
    fun updateNotifChat() {
        val pushNotificationSettings =
            pushNotificationSettingManagement.pushNotificationSetting

        var option = Constants.NOTIFICATIONS_ENABLED

        if (pushNotificationSettings != null && pushNotificationSettings.isGlobalChatsDndEnabled) {
            option =
                if (pushNotificationSettings.globalChatsDnd == 0L) Constants.NOTIFICATIONS_DISABLED
                else Constants.NOTIFICATIONS_DISABLED_X_TIME
        }

        chatNotificationsPreference?.summary = when (option) {
            Constants.NOTIFICATIONS_DISABLED -> getString(R.string.mute_chatroom_notification_option_off)
            Constants.NOTIFICATIONS_ENABLED -> getString(R.string.mute_chat_notification_option_on)
            else -> TimeUtils.getCorrectStringDependingOnOptionSelected(
                pushNotificationSettings!!.globalChatsDnd,
                requireContext()
            )
        }
    }

    /**
     * Method for showing and hiding what is needed while waiting for presence config.
     */
    private fun waitPresenceConfig() {
        preferenceScreen.apply {
            autoAwaySwitch?.let { removePreference(it) }
            chatAutoAwayPreference?.let { removePreference(it) }
            chatPersistenceCheck?.let { removePreference(it) }
        }

        statusChatListPreference?.apply {
            value = UserChatStatus.Offline.toChatStatusPreferenceValue()?.toString()
            summary = statusChatListPreference?.entry
        }

        enableLastGreenChatSwitch?.isEnabled = false
    }

    private fun showPresenceChatConfig(statusConfig: ChatPresenceConfig) {
        statusChatListPreference?.apply {
            value = statusConfig.onlineStatus.toChatStatusPreferenceValue()?.toString()
            summary =
                if (statusConfig.onlineStatus == UserChatStatus.Invalid) getString(R.string.recovering_info)
                else entry
        }

        if (statusConfig.onlineStatus != UserChatStatus.Offline) {
            chatPersistenceCheck?.let { preferenceScreen.addPreference(it) }
            chatPersistenceCheck?.isChecked = statusConfig.isPersist
        }

        if (statusConfig.onlineStatus != UserChatStatus.Online) {
            autoAwaySwitch?.let { preferenceScreen.removePreference(it) }
            chatAutoAwayPreference?.let { preferenceScreen.removePreference(it) }
            if (statusConfig.onlineStatus == UserChatStatus.Offline) {
                chatPersistenceCheck?.let { preferenceScreen.removePreference(it) }
            }
        } else if (statusConfig.isPersist) {
            autoAwaySwitch?.let { preferenceScreen.removePreference(it) }
            chatAutoAwayPreference?.let { preferenceScreen.removePreference(it) }
        } else {
            autoAwaySwitch?.let { preferenceScreen.addPreference(it) }

            if (statusConfig.isAutoAwayEnabled) {
                val timeout = statusConfig.autoAwayTimeout.toInt() / 60
                autoAwaySwitch?.isChecked = true
                chatAutoAwayPreference?.let { preferenceScreen.addPreference(it) }
                chatAutoAwayPreference?.summary =
                    getString(R.string.settings_autoaway_value, timeout)
            } else {
                autoAwaySwitch?.isChecked = false
                chatAutoAwayPreference?.let { preferenceScreen.removePreference(it) }
            }
        }

        enableLastGreenChatSwitch?.apply {
            isEnabled = true

            if (!isChecked) {
                onPreferenceClickListener = null
                isChecked = statusConfig.isLastGreenVisible
            }

            onPreferenceClickListener = this@SettingsChatFragment
        }
    }

    fun setOnlineOptions(isOnline: Boolean) {
        chatNotificationsPreference?.isEnabled = isOnline
        statusChatListPreference?.isEnabled = isOnline
        autoAwaySwitch?.isEnabled = isOnline
        chatAutoAwayPreference?.isEnabled = isOnline
        chatPersistenceCheck?.isEnabled = isOnline
        enableLastGreenChatSwitch?.isEnabled = isOnline
        chatAttachmentsChatListPreference?.isEnabled = isOnline
        richLinksSwitch?.isEnabled = isOnline
    }

    companion object {
        private const val KEY_CHAT_IMAGE_QUALITY = "settings_chat_image_quality"
    }
}
