package mega.privacy.android.app.presentation.settings

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.CheckedTextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.*
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
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop
import mega.privacy.android.app.lollipop.TwoFactorAuthenticationActivity
import mega.privacy.android.app.lollipop.VerifyTwoFactorActivity
import mega.privacy.android.app.lollipop.managerSections.settings.Settings
import mega.privacy.android.app.lollipop.managerSections.settings.SettingsActivity
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.service.RATE_APP_URL
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.SharedPreferenceConstants.HIDE_RECENT_ACTIVITY
import mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES
import mega.privacy.android.app.utils.ThemeHelper.applyTheme
import mega.privacy.android.app.utils.Util

@AndroidEntryPoint
@SuppressLint("NewApi")
class SettingsFragment : Preference.OnPreferenceChangeListener,
    Settings, PreferenceFragmentCompat() {

    override var numberOfClicksKarere = 0
    override var numberOfClicksAppVersion = 0
    override var numberOfClicksSDK = 0

    override var setAutoAccept = false
    override var autoAcceptSetting = false

    private val viewModel: SettingsViewModel by viewModels()

    private var playerService: MediaPlayerService? = null
    private var bEvaluateAppDialogShow = false
    private var evaluateAppDialog: AlertDialog? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
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

    /**
     * Update the Cancel Account settings.
     */
    override fun updateCancelAccountSetting() {}

    override fun update2FAVisibility() {}

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

    override fun setOnlineOptions(isOnline: Boolean) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LogUtil.logDebug("onViewCreated")

        navigateToInitialPreference()

        restoreEvaluateDialogState(savedInstanceState)
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

    override fun goToCategoryStorage() {}

    override fun goToCategoryQR() {}

    override fun goToSectionStartScreen() {}

    /**
     * Method for controlling whether or not to display the action bar elevation.
     */
    override fun checkScroll() {}

    private fun restoreEvaluateDialogState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            bEvaluateAppDialogShow = savedInstanceState.getBoolean(EVALUATE_APP_DIALOG_SHOW)
        }
        if (bEvaluateAppDialogShow) {
            showEvaluatedAppDialog()
        }
    }

    private fun showEvaluatedAppDialog() {
        evaluateAppDialog = createFeedbackDialog()
        evaluateAppDialog?.show()
        bEvaluateAppDialogShow = true
    }

    private fun createFeedbackDialog(): AlertDialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setView(createFeedbackDialogView())
            .setTitle(getString(R.string.title_evaluate_the_app_panel))
            .create()
    }

    @Suppress("DEPRECATION")
    private fun createFeedbackDialogView(): View? {
        val dialogLayout = View.inflate(context, R.layout.evaluate_the_app_dialog, null)

        val display = requireActivity().windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val rateAppCheck = dialogLayout.findViewById<CheckedTextView>(R.id.rate_the_app)
        setFeedbackMargins(rateAppCheck, outMetrics)
        rateAppCheck.setOnClickListener {
            LogUtil.logDebug("Rate the app")
            //Rate the app option:
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RATE_APP_URL)))
            dismissEvaluateDialog()
        }

        val sendFeedbackCheck =
            dialogLayout.findViewById<CheckedTextView>(R.id.send_feedback)
        setFeedbackMargins(sendFeedbackCheck, outMetrics)
        sendFeedbackCheck.setOnClickListener {
            LogUtil.logDebug("Send Feedback")
            val body = generateBody()
            val versionApp = getString(R.string.app_version)
            val subject = getString(R.string.setting_feedback_subject) + " v" + versionApp
            sendEmail(subject, body)
            dismissEvaluateDialog()
        }
        return dialogLayout
    }

    private fun setFeedbackMargins(
        sendFeedbackCheck: CheckedTextView,
        outMetrics: DisplayMetrics
    ) {
        sendFeedbackCheck.compoundDrawablePadding = Util.scaleWidthPx(10, outMetrics)
        val sendFeedbackMLP = sendFeedbackCheck.layoutParams as MarginLayoutParams
        sendFeedbackMLP.setMargins(
            Util.scaleWidthPx(15, outMetrics),
            Util.scaleHeightPx(10, outMetrics),
            0,
            Util.scaleHeightPx(10, outMetrics)
        )
    }

    private fun generateBody(): String {
        return StringBuilder()
            .append(getString(R.string.setting_feedback_body))
            .append("\n\n\n\n\n\n\n\n\n\n\n")
            .append(getString(R.string.settings_feedback_body_device_model)).append("  ")
            .append(Util.getDeviceName()).append("\n")
            .append(getString(R.string.settings_feedback_body_android_version)).append("  ")
            .append(Build.VERSION.RELEASE)
            .append(" ").append(Build.DISPLAY).append("\n")
            .append(getString(R.string.user_account_feedback)).append("  ")
            .append(viewModel.email)
            .append(" (${getAccountTypeLabel()})")
            .toString()
    }

    private fun getAccountTypeLabel() = when (viewModel.accountType) {
        Constants.FREE -> getString(R.string.my_account_free)
        Constants.PRO_I -> getString(R.string.my_account_pro1)
        Constants.PRO_II -> getString(R.string.my_account_pro2)
        Constants.PRO_III -> getString(R.string.my_account_pro3)
        Constants.PRO_LITE -> getString(R.string.my_account_prolite_feedback_email)
        Constants.BUSINESS -> getString(R.string.business_label)
        else -> getString(R.string.my_account_free)
    }

    private fun sendEmail(subject: String, body: String) {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = Constants.TYPE_TEXT_PLAIN
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.MAIL_ANDROID))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, body)
        startActivity(Intent.createChooser(emailIntent, " "))
    }

    private fun dismissEvaluateDialog() {
        if (evaluateAppDialog != null) {
            evaluateAppDialog?.dismiss()
            bEvaluateAppDialogShow = false
        }
    }

    override fun onResume() {
        registerAccountChangeReceiver()
        refreshCameraUploadsSettings()
        updatePasscodeLockSubtitle()
        super.onResume()
    }

    private fun registerAccountChangeReceiver() {
        val filter = IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
        requireContext().registerReceiver(updateMyAccountReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(updateMyAccountReceiver)
    }

    /**
     * Refresh the Camera Uploads service settings depending on the service status.
     */
    override fun refreshCameraUploadsSettings() {
        val isCameraUploadOn = viewModel.isCamSyncEnabled
        findPreference<Preference>(KEY_FEATURES_CAMERA_UPLOAD)?.summary =
            getString(if (isCameraUploadOn) R.string.mute_chat_notification_option_on else R.string.mute_chatroom_notification_option_off)
    }

    fun updatePasscodeLockSubtitle() {
        findPreference<Preference>(KEY_PASSCODE_LOCK)?.setSummary(if (viewModel.passcodeLock) R.string.mute_chat_notification_option_on else R.string.mute_chatroom_notification_option_off)
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        when (preference?.key) {
            KEY_APPEARNCE_COLOR_THEME -> applyTheme(
                (newValue as String)
            )
        }
        return true
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        val key = preference?.key
        LogUtil.logDebug("KEY pressed: $key")
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
                    ChangePasswordActivityLollipop::class.java
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
                launchWebPage("https://mega.nz/help/client/android")
            }
            KEY_HELP_SEND_FEEDBACK -> showEvaluatedAppDialog()
            KEY_ABOUT_PRIVACY_POLICY -> {
                launchWebPage("https://mega.nz/privacy")
            }
            KEY_ABOUT_TOS -> {
                launchWebPage("https://mega.nz/terms")
            }
            KEY_ABOUT_CODE_LINK -> {
                launchWebPage("https://github.com/meganz/android")
            }
            KEY_ABOUT_APP_VERSION -> {
                LogUtil.logDebug("KEY_ABOUT_APP_VERSION pressed")
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
                    if (viewModel.isLoggerEnabled) {
                        LogUtil.setStatusLoggerSDK(activity, false)
                    } else {
                        LogUtil.logWarning("SDK file logger attribute is NULL")
                        (activity as SettingsActivity).showConfirmationEnableLogsSDK()
                    }
                }
            }
            KEY_ABOUT_KARERE_VERSION -> {
                if (++numberOfClicksKarere == 5) {
                    numberOfClicksKarere = 0
                    if (viewModel.isChatLoggerEnabled) {
                        LogUtil.setStatusLoggerKarere(activity, false)
                    } else {
                        LogUtil.logWarning("Karere file logger attribute is NULL")
                        (activity as SettingsActivity).showConfirmationEnableLogsKarere()
                    }
                }
            }
            KEY_CANCEL_ACCOUNT -> deleteAccountClicked()
            KEY_ABOUT_COOKIE_POLICY -> {
                val intent = Intent(context, WebViewActivity::class.java)
                intent.data = Uri.parse("https://mega.nz/cookie")
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
                )
                    .edit().putBoolean(HIDE_RECENT_ACTIVITY, checked == true).apply()
                LiveEventBus.get(EVENT_UPDATE_HIDE_RECENT_ACTIVITY, Boolean::class.java)
                    .post(checked)
            }
        }
        resetCounters(key)
        return true
    }

    private fun deleteAccountClicked() {
        LogUtil.logDebug("askConfirmationDeleteAccount")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_account))
            .setMessage(resources.getString(R.string.delete_account_text))
            .setPositiveButton(R.string.delete_account) { _, _ -> deleteAccountConfirmed() }
            .setNegativeButton(R.string.general_dismiss) { _, _ -> }
            .show()

    }

    private fun deleteAccountConfirmed() {
        LogUtil.logDebug("deleteAccount")
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
        Util.hideKeyboard(requireActivity(), 0)
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

    override fun goToFirstCategory() {}


    override fun reEnable2faSwitch() {}

    override fun hidePreferencesChat() {}

    override fun setValueOfAutoAccept(autoAccept: Boolean) {}

    override fun update2FAPreference(enabled: Boolean) {}


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (evaluateAppDialog != null && evaluateAppDialog?.isShowing == true) {
            outState.putBoolean(EVALUATE_APP_DIALOG_SHOW, bEvaluateAppDialogShow)
        }
    }

    override fun onDestroyView() {
        requireContext().unbindService(mediaServiceConnection)
        super.onDestroyView()
    }

    companion object {
        internal const val INITIAL_PREFERENCE = "initial"
        internal const val NAVIGATE_TO_INITIAL_PREFERENCE = "navigateToInitial"
    }

}

private const val EVALUATE_APP_DIALOG_SHOW = "EvaluateAppDialogShow"