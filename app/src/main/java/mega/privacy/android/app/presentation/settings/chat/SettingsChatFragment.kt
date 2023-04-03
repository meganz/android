package mega.privacy.android.app.presentation.settings.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.ChatNotificationsPreferencesActivity
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.TwoLineCheckPreference
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.listeners.SetAttrUserListener
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.settings.chat.imagequality.SettingsChatImageQualityActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatPresenceConfig
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SettingsChatFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    lateinit var dbH: DatabaseHandler

    private val viewModel: SettingsChatViewModel by viewModels()

    private var statusConfig: MegaChatPresenceConfig? = null
    private var chatNotificationsPreference: Preference? = null
    private var statusChatListPreference: ListPreference? = null
    private var autoAwaySwitch: SwitchPreferenceCompat? = null
    private var chatAutoAwayPreference: Preference? = null
    private var chatPersistenceCheck: TwoLineCheckPreference? = null
    private var enableLastGreenChatSwitch: SwitchPreferenceCompat? = null
    private var chatAttachmentsChatListPreference: ListPreference? = null
    private var richLinksSwitch: SwitchPreferenceCompat? = null

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_chat)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        setOnlineOptions(viewModel.isConnected && megaApi.rootNode != null)
        return v
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
            findPreference<TwoLineCheckPreference?>(SettingsConstants.KEY_CHAT_PERSISTENCE)?.apply {
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
                setValueIndex(dbH.chatVideoQuality)
                summary = chatAttachmentsChatListPreference?.entry
            }

        richLinksSwitch =
            findPreference<SwitchPreferenceCompat?>(SettingsConstants.KEY_CHAT_RICH_LINK)?.apply {
                onPreferenceClickListener = this@SettingsChatFragment
                isChecked = MegaApplication.isEnabledRichLinks
            }

        if (megaChatApi.isSignalActivityRequired) {
            megaChatApi.signalPresenceActivity()
        }

        //Get chat status
        statusConfig = megaChatApi.presenceConfig
        if (statusConfig != null) {
            Timber.d("ChatStatus pending: ${statusConfig!!.isPending}, status: ${statusConfig!!.onlineStatus}")
            statusChatListPreference?.apply {
                value = statusConfig!!.onlineStatus.toString()
                summary =
                    if (statusConfig?.onlineStatus == MegaChatApi.STATUS_INVALID) getString(R.string.recovering_info)
                    else statusChatListPreference?.entry
            }

            showPresenceChatConfig()
            if (megaChatApi.isSignalActivityRequired) {
                megaChatApi.signalPresenceActivity()
            }
        } else {
            waitPresenceConfig()
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { state ->
                    findPreference<Preference?>(KEY_CHAT_IMAGE_QUALITY)?.summary =
                        state.imageQuality?.title?.let { getString(it) }
                }
            }
        }

        viewLifecycleOwner.collectFlow(viewModel.monitorConnectivityEvent) { isConnected ->
            setOnlineOptions(isConnected)
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (megaChatApi.isSignalActivityRequired) {
            megaChatApi.signalPresenceActivity()
        }

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
                statusConfig = megaChatApi.presenceConfig

                if (statusConfig != null) {
                    if (statusConfig!!.isAutoawayEnabled) {
                        Timber.d("Change AUTOAWAY chat to false")
                        megaChatApi.setPresenceAutoaway(false, 0)
                        chatAutoAwayPreference?.let { preferenceScreen.removePreference(it) }
                    } else {
                        Timber.d("Change AUTOAWAY chat to true")
                        megaChatApi.setPresenceAutoaway(true, 300)
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
                megaChatApi.setPresencePersist(!statusConfig!!.isPersist)
                return true
            }
            SettingsConstants.KEY_CHAT_LAST_GREEN -> {
                (requireActivity() as ChatPreferencesActivity).enableLastGreen(
                    enableLastGreenChatSwitch!!.isChecked
                )
                return true
            }
            SettingsConstants.KEY_CHAT_RICH_LINK -> {
                megaApi.enableRichPreviews(
                    richLinksSwitch!!.isChecked,
                    SetAttrUserListener(context)
                )
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
                megaChatApi.onlineStatus = (newValue as String).toInt()
                return true
            }
            SettingsConstants.KEY_CHAT_SEND_ORIGINALS -> {
                val newStatus = (newValue as String).toInt()
                dbH.chatVideoQuality = newStatus
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
            MegaApplication.getPushNotificationSettingManagement().pushNotificationSetting

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
     * Method for updating the rich link previews option.
     */
    fun updateEnabledRichLinks() {
        if (MegaApplication.isEnabledRichLinks == richLinksSwitch?.isChecked) return

        richLinksSwitch?.apply {
            onPreferenceClickListener = null
            isChecked = MegaApplication.isEnabledRichLinks
            onPreferenceClickListener = this@SettingsChatFragment
        }
    }

    /**
     * Method for updating the presence configuration.
     *
     * @param cancelled If it is cancelled
     */
    fun updatePresenceConfigChat(cancelled: Boolean) {
        if (!cancelled) {
            statusConfig = megaChatApi.presenceConfig
        }
        showPresenceChatConfig()
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
            value = MegaChatApi.STATUS_OFFLINE.toString()
            summary = statusChatListPreference?.entry
        }

        enableLastGreenChatSwitch?.isEnabled = false
    }

    private fun showPresenceChatConfig() {
        statusChatListPreference?.apply {
            value = statusConfig?.onlineStatus.toString()
            summary = statusChatListPreference?.entry
        }

        if (statusConfig?.onlineStatus == MegaChatApi.STATUS_ONLINE ||
            statusConfig?.onlineStatus != MegaChatApi.STATUS_OFFLINE
        ) {
            chatPersistenceCheck?.let { preferenceScreen.addPreference(it) }
            chatPersistenceCheck?.isChecked = statusConfig!!.isPersist
        }

        if (statusConfig?.onlineStatus != MegaChatApi.STATUS_ONLINE) {
            autoAwaySwitch?.let { preferenceScreen.removePreference(it) }
            chatAutoAwayPreference?.let { preferenceScreen.removePreference(it) }
            if (statusConfig?.onlineStatus == MegaChatApi.STATUS_OFFLINE) {
                chatPersistenceCheck?.let { preferenceScreen.removePreference(it) }
            }
        } else if (statusConfig!!.isPersist) {
            autoAwaySwitch?.let { preferenceScreen.removePreference(it) }
            chatAutoAwayPreference?.let { preferenceScreen.removePreference(it) }
        } else {
            autoAwaySwitch?.let { preferenceScreen.addPreference(it) }

            if (statusConfig!!.isAutoawayEnabled) {
                val timeout = statusConfig!!.autoawayTimeout.toInt() / 60
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
                isChecked = statusConfig!!.isLastGreenVisible
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