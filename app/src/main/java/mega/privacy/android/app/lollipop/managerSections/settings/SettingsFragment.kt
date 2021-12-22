package mega.privacy.android.app.lollipop.managerSections.settings

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.activities.settingsActivities.*
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_HIDE_RECENT_ACTIVITY
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_START_SCREEN
import mega.privacy.android.app.constants.SettingsConstants.*
import mega.privacy.android.app.exportRK.ExportRecoveryKeyActivity
import mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.HOME_BNV
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop
import mega.privacy.android.app.lollipop.TwoFactorAuthenticationActivity
import mega.privacy.android.app.lollipop.VerifyTwoFactorActivity
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder
import mega.privacy.android.app.service.RATE_APP_URL
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.SharedPreferenceConstants.HIDE_RECENT_ACTIVITY
import mega.privacy.android.app.utils.SharedPreferenceConstants.PREFERRED_START_SCREEN
import mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES
import mega.privacy.android.app.utils.ThemeHelper.applyTheme
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatApiJava

@AndroidEntryPoint
@SuppressLint("NewApi")
class SettingsFragment : Preference.OnPreferenceChangeListener,
    Settings, PreferenceFragmentCompat() {

    override var numberOfClicksKarere = 0
    override var numberOfClicksAppVersion = 0
    override var numberOfClicksSDK = 0
    override var setAutoAccept = false
    override var autoAcceptSetting = true

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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        updateCancelAccountSetting()
        checkUIPreferences()
        update2FAVisibility()
        setAutoAccept = false
        autoAcceptSetting = true
    }

    /**
     * Update the Cancel Account settings.
     */
    override fun updateCancelAccountSetting() {
        if (viewModel.canNotDeleteAccount) {
            findPreference<PreferenceCategory>(CATEGORY_ABOUT)?.removePreference(
                findPreference(
                    KEY_CANCEL_ACCOUNT
                )
            )
        }
    }

    /**
     * Checks and sets the User interface setting values.
     */
    fun checkUIPreferences() {
        updateStartScreenSetting(viewModel.startScreen)
        findPreference<SwitchPreferenceCompat>(KEY_HIDE_RECENT_ACTIVITY)?.isChecked =
            viewModel.hideRecentActivity
    }

    /**
     * Updates the start screen setting.
     *
     * @param newStartScreen Value to set as new start screen.
     */
    fun updateStartScreenSetting(newStartScreen: Int) {
        val startScreenSummary =
            resources.getStringArray(R.array.settings_start_screen)[newStartScreen]
        findPreference<Preference>(KEY_START_SCREEN)?.summary = startScreenSummary
    }

    override fun update2FAVisibility() {
        if (viewModel.multiFactorAuthAvailable) {
            findPreference<SwitchPreferenceCompat>(KEY_2FA)?.isEnabled = false
            findPreference<SwitchPreferenceCompat>(KEY_2FA)?.isVisible = true
            viewModel.multiFactorAuthCheck(activity as SettingsActivity)
        } else {
            findPreference<SwitchPreferenceCompat>(KEY_2FA)?.isVisible = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        setOnlineOptions(Util.isOnline(context) && viewModel.hasRootNode)
        val playerServiceIntent = Intent(requireContext(), AudioPlayerService::class.java)
        requireContext().bindService(playerServiceIntent, mediaServiceConnection, 0)
        return v
    }

    override fun setOnlineOptions(isOnline: Boolean) {
        findPreference<Preference>(KEY_FEATURES_CAMERA_UPLOAD)?.isEnabled = isOnline
        findPreference<Preference>(KEY_FEATURES_CHAT)?.isEnabled = isOnline
        findPreference<SwitchPreferenceCompat>(KEY_2FA)?.isEnabled = isOnline
        findPreference<SwitchPreferenceCompat>(KEY_QR_CODE_AUTO_ACCEPT)?.isEnabled = isOnline
        findPreference<Preference>(KEY_CANCEL_ACCOUNT)?.isEnabled = isOnline
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LogUtil.logDebug("onViewCreated")
        setupObservers()

        // Init QR code setting
        val managerActivityLollipop = requireActivity() as SettingsActivity
        viewModel.getContactLinksOption(managerActivityLollipop)
        when {
            managerActivityLollipop.openSettingsStorage -> {
                goToCategoryStorage()
            }
            managerActivityLollipop.openSettingsQR -> {
                goToCategoryQR()
            }
            managerActivityLollipop.openSettingsStartScreen -> {
                goToSectionStartScreen()
            }
        }
        listView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })

        restoreEvaluateDialogState(savedInstanceState)
    }

    private fun setupObservers() {
        LiveEventBus.get(EVENT_UPDATE_START_SCREEN, Int::class.java)
            .observe(
                viewLifecycleOwner,
                { newStartScreen: Int -> updateStartScreenSetting(newStartScreen) })
        LiveEventBus.get(EVENT_UPDATE_HIDE_RECENT_ACTIVITY, Boolean::class.java)
            .observe(viewLifecycleOwner, { hide: Boolean -> updateHideRecentActivitySetting(hide) })
    }

    /**
     * Updates the hide recent activity setting.
     *
     * @param hide True if should enable the setting, false otherwise.
     */
    private fun updateHideRecentActivitySetting(hide: Boolean) {
        findPreference<SwitchPreferenceCompat>(KEY_HIDE_RECENT_ACTIVITY)?.takeIf { it.isChecked != hide }?.let { it.isChecked = hide }
    }

    override fun goToCategoryStorage() {
        val storagePreference = findPreference<Preference>(KEY_STORAGE_FILE_MANAGEMENT)
        scrollToPreference(storagePreference)
        onPreferenceTreeClick(storagePreference)
    }

    override fun goToCategoryQR() {
        scrollToPreference(findPreference<SwitchPreferenceCompat>(KEY_QR_CODE_AUTO_ACCEPT))
    }

    override fun goToSectionStartScreen() {
        scrollToPreference(findPreference(KEY_START_SCREEN))
        startActivity(Intent(context, StartScreenPreferencesActivity::class.java))
        (activity as SettingsActivity).openSettingsStartScreen = false
    }

    /**
     * Method for controlling whether or not to display the action bar elevation.
     */
    override fun checkScroll() {
        if (listView == null) {
            return
        }
        (activity as SettingsActivity)
            .changeAppBarElevation(listView.canScrollVertically(Constants.SCROLLING_UP_DIRECTION))
    }

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

    private fun createFeedbackDialogView(): View? {
        val dialogLayout = layoutInflater.inflate(R.layout.evaluate_the_app_dialog, null)

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
        viewModel.refreshAccount()
        refreshCameraUploadsSettings()
        updatePasscodeLockSubtitle()
        if (!Util.isOnline(context)) {
            findPreference<Preference>(KEY_FEATURES_CHAT)?.isEnabled = false
            findPreference<Preference>(KEY_FEATURES_CAMERA_UPLOAD)?.isEnabled = false
        }
        super.onResume()
    }

    /**
     * Refresh the Camera Uploads service settings depending on the service status.
     */
    override fun refreshCameraUploadsSettings() {
        var isCameraUploadOn = viewModel.isCamSyncEnabled
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
            KEY_2FA -> if ((activity as SettingsActivity).is2FAEnabled) {
                findPreference<SwitchPreferenceCompat>(KEY_2FA)?.isChecked = true
                val intent = Intent(context, VerifyTwoFactorActivity::class.java)
                intent.putExtra(VerifyTwoFactorActivity.KEY_VERIFY_TYPE, Constants.DISABLE_2FA)
                requireActivity().startActivity(intent)
            } else {
                findPreference<SwitchPreferenceCompat>(KEY_2FA)?.isChecked = false
                val intent = Intent(context, TwoFactorAuthenticationActivity::class.java)
                startActivity(intent)
            }
            KEY_QR_CODE_AUTO_ACCEPT -> {
                //			First query if QR auto-accept is enabled or not, then change the value
                setAutoAccept = true
                viewModel.getContactLinksOption(activity as SettingsActivity)
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
                        (activity as SettingsActivity).showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.show_info_chat_msg_enabled),
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                    } else {
                        MegaApplication.setShowInfoChatMessages(false)
                        (activity as SettingsActivity).showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.show_info_chat_msg_disabled),
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
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
                    if (viewModel.isKarereLoggerEnabled) {
                        LogUtil.setStatusLoggerKarere(activity, false)
                    } else {
                        LogUtil.logWarning("Karere file logger attribute is NULL")
                        (activity as SettingsActivity).showConfirmationEnableLogsKarere()
                    }
                }
            }
            KEY_CANCEL_ACCOUNT -> (activity as SettingsActivity).askConfirmationDeleteAccount()
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

    /**
     * Scroll to the beginning of Settings page.
     * In this case, the beginning is category KEY_FEATURES.
     *
     *
     * Note: If the first category changes, this method should be updated with the new one.
     */
    override fun goToFirstCategory() {
        scrollToPreference(findPreference<PreferenceCategory>(KEY_FEATURES))
    }

    /**
     * Re-enable 'findPreference<SwitchPreferenceCompat>(KEY_2FA)' after 'multiFactorAuthCheck' finished.
     */
    override fun reEnable2faSwitch() {
        findPreference<SwitchPreferenceCompat>(KEY_2FA)?.isEnabled = true
    }

    override fun hidePreferencesChat() {
        findPreference<Preference>(KEY_FEATURES_CHAT)?.isEnabled = false
    }

    override fun update2FAPreference(enabled: Boolean) {
        findPreference<SwitchPreferenceCompat>(KEY_2FA)?.isChecked = enabled
    }

    override fun setValueOfAutoAccept(autoAccept: Boolean) {
        findPreference<SwitchPreferenceCompat>(KEY_QR_CODE_AUTO_ACCEPT)?.isChecked = autoAccept
    }

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

}

private const val EVALUATE_APP_DIALOG_SHOW = "EvaluateAppDialogShow"