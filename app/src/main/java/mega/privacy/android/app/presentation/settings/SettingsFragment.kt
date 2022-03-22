package mega.privacy.android.app.presentation.settings

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.activities.settingsActivities.*
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_HIDE_RECENT_ACTIVITY
import mega.privacy.android.app.constants.SettingsConstants.*
import mega.privacy.android.app.exportRK.ExportRecoveryKeyActivity
import mega.privacy.android.app.main.ChangePasswordActivity
import mega.privacy.android.app.main.TwoFactorAuthenticationActivity
import mega.privacy.android.app.main.VerifyTwoFactorActivity
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.presentation.extensions.hideKeyboard
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.SharedPreferenceConstants.HIDE_RECENT_ACTIVITY
import mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES
import mega.privacy.android.app.utils.ThemeHelper.applyTheme

@AndroidEntryPoint
@SuppressLint("NewApi")
class SettingsFragment : SharedPreferences.OnSharedPreferenceChangeListener, PreferenceFragmentCompat() {

    private var numberOfClicksKarere = 0
    private var numberOfClicksAppVersion = 0
    private var numberOfClicksSDK = 0

    private val viewModel: SettingsViewModel by viewModels()

    private var playerService: MediaPlayerService? = null
    private val mediaServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            playerService = (service as MediaPlayerServiceBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            playerService = null
        }
    }

    private val updateMyAccountReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.refreshAccount()
        }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        val playerServiceIntent = Intent(requireContext(), AudioPlayerService::class.java)
        requireContext().bindService(playerServiceIntent, mediaServiceConnection, 0)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeState()
        navigateToInitialPreference()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect { state ->
                    findPreference<Preference>(KEY_FEATURES_CAMERA_UPLOAD)?.isEnabled =
                        state.cameraUploadEnabled
                    findPreference<Preference>(KEY_FEATURES_CHAT)?.isEnabled = state.chatEnabled

                    findPreference<SwitchPreferenceCompat>(KEY_2FA)?.apply {
                        isVisible = state.multiFactorVisible
                        isChecked = state.multiFactorAuthChecked
                        isEnabled = state.multiFactorEnabled
                    }

                    findPreference<SwitchPreferenceCompat>(KEY_QR_CODE_AUTO_ACCEPT)?.apply {
                        isEnabled = state.autoAcceptEnabled
                        isChecked = state.autoAcceptChecked
                    }

                    findPreference<Preference>(KEY_CANCEL_ACCOUNT)?.apply {
                        isVisible = state.deleteAccountVisible
                        isEnabled = state.deleteEnabled
                    }

                    //        TODO: Move Summaries to initialise and update summaries methods
                    val startScreenSummary =
                        resources.getStringArray(R.array.settings_start_screen)[state.startScreen]
                    findPreference<Preference>(KEY_START_SCREEN)?.summary = startScreenSummary

                    findPreference<SwitchPreferenceCompat>(KEY_HIDE_RECENT_ACTIVITY)?.takeIf { it.isChecked != state.hideRecentActivityChecked }
                        ?.let { it.isChecked = state.hideRecentActivityChecked }

                    findPreference<Preference>(KEY_FEATURES_CHAT)?.isEnabled = state.chatEnabled

                }
            }
        }
    }

    private fun navigateToInitialPreference() {
        val initial =
            arguments?.getString(INITIAL_PREFERENCE)?.let {
                findPreference<Preference>(it)
            }

        initial?.let {
            scrollToPreference(it)
            if (arguments?.getBoolean(NAVIGATE_TO_INITIAL_PREFERENCE, false) == true) {
                onPreferenceTreeClick(it)
            }
        }
    }

    override fun onResume() {
        registerAccountChangeReceiver()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        refreshSummaries()
        resetCounters(null)
        super.onResume()
    }

    private fun registerAccountChangeReceiver() {
        val filter = IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
        requireContext().registerReceiver(updateMyAccountReceiver, filter)
    }

    private fun refreshSummaries() {
//        TODO: Once all fragments Settings fragments have been refactored, these functions can be improved.
        updateCameraUploadSummary()
        updatePasscodeLockSummary()
    }

    private fun updateCameraUploadSummary() {
        val isCameraUploadOn = viewModel.isCamSyncEnabled
        findPreference<Preference>(KEY_FEATURES_CAMERA_UPLOAD)?.summary =
            getString(if (isCameraUploadOn) R.string.mute_chat_notification_option_on else R.string.mute_chatroom_notification_option_off)
    }

    private fun updatePasscodeLockSummary() {
        findPreference<Preference>(KEY_PASSCODE_LOCK)?.setSummary(if (viewModel.passcodeLock) R.string.mute_chat_notification_option_on else R.string.mute_chatroom_notification_option_off)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        requireContext().unregisterReceiver(updateMyAccountReceiver)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KEY_APPEARNCE_COLOR_THEME -> findPreference<ListPreference>(key)?.value?.let {
                applyTheme(
                    it
                )
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        val key = preference?.key
        when (key) {
            KEY_FEATURES_CAMERA_UPLOAD -> startActivity(
                Intent(
                    context,
                    CameraUploadsPreferencesActivity::class.java
                )
            )
            KEY_FEATURES_CHAT -> startActivity(
                Intent(
                    context,
                    ChatPreferencesActivity::class.java
                )
            )
            KEY_STORAGE_DOWNLOAD -> startActivity(
                Intent(
                    context,
                    DownloadPreferencesActivity::class.java
                )
            )
            KEY_STORAGE_FILE_MANAGEMENT -> requireActivity().startActivity(
                Intent(
                    context,
                    FileManagementPreferencesActivity::class.java
                )
            )
            KEY_RECOVERY_KEY -> startActivity(
                Intent(
                    context,
                    ExportRecoveryKeyActivity::class.java
                )
            )
            KEY_PASSCODE_LOCK -> startActivity(
                Intent(
                    context,
                    PasscodePreferencesActivity::class.java
                )
            )
            KEY_CHANGE_PASSWORD -> startActivity(
                Intent(
                    context,
                    ChangePasswordActivity::class.java
                )
            )
            KEY_2FA -> if (viewModel.uiState.value.multiFactorAuthChecked) {
                val intent = Intent(context, VerifyTwoFactorActivity::class.java)
                intent.putExtra(VerifyTwoFactorActivity.KEY_VERIFY_TYPE, Constants.DISABLE_2FA)
                startActivity(intent)
            } else {
                val intent = Intent(context, TwoFactorAuthenticationActivity::class.java)
                startActivity(intent)
            }
            KEY_QR_CODE_AUTO_ACCEPT -> {
                viewModel.toggleAutoAcceptPreference()
            }
            KEY_SECURITY_ADVANCED -> startActivity(
                Intent(
                    context,
                    AdvancedPreferencesActivity::class.java
                )
            )
            KEY_HELP_CENTRE -> {
                launchWebPage(HELP_CENTRE_URL)
            }
            KEY_HELP_SEND_FEEDBACK -> showEvaluatedAppDialog()
            KEY_ABOUT_PRIVACY_POLICY -> {
                launchWebPage(PRIVACY_POLICY_URL)
            }
            KEY_ABOUT_TOS -> {
                launchWebPage(TERMS_OF_SERVICE_URL)
            }
            KEY_ABOUT_CODE_LINK -> {
                launchWebPage(GITHUB_URL)
            }
            KEY_ABOUT_APP_VERSION -> {
                if (++numberOfClicksAppVersion == 5) {
                    numberOfClicksAppVersion = 0
                    if (!MegaApplication.isShowInfoChatMessages()) {
                        MegaApplication.setShowInfoChatMessages(true)
                        view?.let {
                            Snackbar.make(
                                it,
                                R.string.show_info_chat_msg_enabled,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        MegaApplication.setShowInfoChatMessages(false)
                        view?.let {
                            Snackbar.make(
                                it,
                                R.string.show_info_chat_msg_disabled,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            KEY_ABOUT_SDK_VERSION -> {
                if (++numberOfClicksSDK == 5) {
                    numberOfClicksSDK = 0
                    toggleLogger()
                }
            }
            KEY_ABOUT_KARERE_VERSION -> {
                if (++numberOfClicksKarere == 5) {
                    numberOfClicksKarere = 0
                    toggleChatLogger()
                }
            }
            KEY_CANCEL_ACCOUNT -> deleteAccountClicked()
            KEY_ABOUT_COOKIE_POLICY -> {
                val intent = Intent(context, WebViewActivity::class.java)
                intent.data = Uri.parse(COOKIES_URI)
                startActivity(intent)
            }
            KEY_COOKIE_SETTINGS -> startActivity(
                Intent(
                    context,
                    CookiePreferencesActivity::class.java
                )
            )
            KEY_AUDIO_BACKGROUND_PLAY_ENABLED -> if (playerService != null) {
                playerService?.viewModel?.toggleBackgroundPlay()
            }
            KEY_START_SCREEN -> startActivity(
                Intent(
                    context,
                    StartScreenPreferencesActivity::class.java
                )
            )
            KEY_HIDE_RECENT_ACTIVITY -> {
                val checked =
                    findPreference<SwitchPreferenceCompat>(KEY_HIDE_RECENT_ACTIVITY)?.isChecked
                requireContext().getSharedPreferences(
                    USER_INTERFACE_PREFERENCES,
                    Context.MODE_PRIVATE
                ).edit().putBoolean(HIDE_RECENT_ACTIVITY, checked == true).apply()
                LiveEventBus.get(EVENT_UPDATE_HIDE_RECENT_ACTIVITY, Boolean::class.java)
                    .post(checked)
            }
        }
        resetCounters(key)
        return super.onPreferenceTreeClick(preference)
    }

    private fun toggleLogger() {
        if (viewModel.disableLogger()) {
            view?.let {
                Snackbar.make(it, R.string.settings_disable_logs, Snackbar.LENGTH_SHORT).show()
            }
        } else {
            showConfirmationEnableLogs(this@SettingsFragment::enableSdkLogger)
        }
    }

    private fun enableSdkLogger() {
                viewModel.enableLogger()
                view?.let {
                    Snackbar.make(it, R.string.settings_enable_logs, Snackbar.LENGTH_SHORT).show()
                }
            }

    private fun toggleChatLogger() {
        if (viewModel.disableChatLogger()) {
            view?.let {
                Snackbar.make(it, R.string.settings_disable_logs, Snackbar.LENGTH_SHORT).show()
            }
        } else {
            showConfirmationEnableLogs(this@SettingsFragment::enableChatLogger)
        }
    }

    private fun enableChatLogger() {
                viewModel.enableChatLogger()
                view?.let {
                    Snackbar.make(it, R.string.settings_enable_logs, Snackbar.LENGTH_SHORT).show()
                }
            }

    private fun showConfirmationEnableLogs(enableFunction: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.enable_log_text_dialog)
            .setPositiveButton(R.string.general_enable) { _, _ ->
                enableFunction()
            }
            .setNegativeButton(R.string.general_cancel, null)
            .show()
            .setCanceledOnTouchOutside(false)
    }

    private fun showEvaluatedAppDialog() {
        FeedBackDialog.newInstance(
            viewModel.email,
            viewModel.accountType
        ).show(childFragmentManager, FeedBackDialog.TAG)
    }

    private fun deleteAccountClicked() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_account))
            .setMessage(resources.getString(R.string.delete_account_text))
            .setPositiveButton(R.string.delete_account) { _, _ -> deleteAccountConfirmed() }
            .setNegativeButton(R.string.general_dismiss) { _, _ -> }
            .show()

    }

    private fun deleteAccountConfirmed() {
        if (viewModel.uiState.value.multiFactorAuthChecked) {
            deleteAccountWithMultiFactorAuthentication()
        } else {
            deleteAccount()
        }
    }

    private fun deleteAccountWithMultiFactorAuthentication() {
        val intent = Intent(context, VerifyTwoFactorActivity::class.java)
        intent.putExtra(VerifyTwoFactorActivity.KEY_VERIFY_TYPE, Constants.CANCEL_ACCOUNT_2FA)
        startActivity(intent)
    }

    private fun deleteAccount() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.deleteAccount()) {
                showInfoDialog(
                    R.string.email_verification_title,
                    R.string.email_verification_text
                )
            } else {
                showInfoDialog(
                    R.string.general_error_word,
                    R.string.general_text_error
                )
            }
        }
    }

    private fun showInfoDialog(@StringRes title: Int, @StringRes message: Int) {
        requireActivity().hideKeyboard()
        MaterialAlertDialogBuilder(requireContext())
            .setPositiveButton(R.string.general_ok) { _, _ -> }
            .setTitle(title)
            .setMessage(message)
            .show()
    }

    private fun launchWebPage(url: String) {
        val viewIntent = Intent(Intent.ACTION_VIEW)
        viewIntent.data = Uri.parse(url)
        startActivity(viewIntent)
    }

    private fun resetCounters(key: String?) {
        if (key != KEY_ABOUT_APP_VERSION) {
            numberOfClicksAppVersion = 0
        }
        if (key != KEY_ABOUT_SDK_VERSION) {
            numberOfClicksSDK = 0
        }
        if (key != KEY_ABOUT_KARERE_VERSION) {
            numberOfClicksKarere = 0
        }
    }


    override fun onDestroyView() {
        requireContext().unbindService(mediaServiceConnection)
        super.onDestroyView()
    }

    companion object {
        internal const val INITIAL_PREFERENCE = "initial"
        internal const val NAVIGATE_TO_INITIAL_PREFERENCE = "navigateToInitial"

        internal const val COOKIES_URI = "https://mega.nz/cookie"
        internal const val GITHUB_URL = "https://github.com/meganz/android"
        internal const val TERMS_OF_SERVICE_URL = "https://mega.nz/terms"
        internal const val PRIVACY_POLICY_URL = "https://mega.nz/privacy"
        internal const val HELP_CENTRE_URL = "https://mega.nz/help/client/android"
    }

}
