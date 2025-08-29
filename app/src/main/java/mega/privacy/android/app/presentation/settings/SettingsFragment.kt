package mega.privacy.android.app.presentation.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity
import mega.privacy.android.app.activities.settingsActivities.CookiePreferencesActivity
import mega.privacy.android.app.activities.settingsActivities.DownloadPreferencesActivity
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity
import mega.privacy.android.app.activities.settingsActivities.StartScreenPreferencesActivity
import mega.privacy.android.app.constants.SettingsConstants.KEY_2FA
import mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_APP_VERSION
import mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_CODE_LINK
import mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_COOKIE_POLICY
import mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_PRIVACY_POLICY
import mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_TOS
import mega.privacy.android.app.constants.SettingsConstants.KEY_AUDIO_BACKGROUND_PLAY_ENABLED
import mega.privacy.android.app.constants.SettingsConstants.KEY_CANCEL_ACCOUNT
import mega.privacy.android.app.constants.SettingsConstants.KEY_CHANGE_PASSWORD
import mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_SETTINGS
import mega.privacy.android.app.constants.SettingsConstants.KEY_FEATURES_CALLS
import mega.privacy.android.app.constants.SettingsConstants.KEY_FEATURES_CAMERA_UPLOAD
import mega.privacy.android.app.constants.SettingsConstants.KEY_FEATURES_CHAT
import mega.privacy.android.app.constants.SettingsConstants.KEY_FEATURES_SYNC
import mega.privacy.android.app.constants.SettingsConstants.KEY_HELP_CENTRE
import mega.privacy.android.app.constants.SettingsConstants.KEY_HELP_SEND_FEEDBACK
import mega.privacy.android.app.constants.SettingsConstants.KEY_HIDDEN_ITEMS
import mega.privacy.android.app.constants.SettingsConstants.KEY_HIDE_RECENT_ACTIVITY
import mega.privacy.android.app.constants.SettingsConstants.KEY_MEDIA_DISCOVERY_VIEW
import mega.privacy.android.app.constants.SettingsConstants.KEY_PASSCODE_LOCK
import mega.privacy.android.app.constants.SettingsConstants.KEY_QR_CODE_AUTO_ACCEPT
import mega.privacy.android.app.constants.SettingsConstants.KEY_RECOVERY_KEY
import mega.privacy.android.app.constants.SettingsConstants.KEY_START_SCREEN
import mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_DOWNLOAD
import mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_FILE_MANAGEMENT
import mega.privacy.android.app.constants.SettingsConstants.KEY_SUB_FOLDER_MEDIA_DISCOVERY
import mega.privacy.android.app.constants.SettingsConstants.REPORT_ISSUE
import mega.privacy.android.app.di.settings.ViewModelPreferenceDataStoreFactory
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.extensions.hideKeyboard
import mega.privacy.android.app.presentation.settings.calls.SettingsCallsActivity
import mega.privacy.android.app.presentation.settings.camerauploads.INTENT_EXTRA_KEY_SHOW_DISABLE_CU
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsActivity
import mega.privacy.android.app.presentation.settings.exportrecoverykey.ExportRecoveryKeyActivity
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.presentation.settings.model.PreferenceResource
import mega.privacy.android.app.presentation.settings.passcode.PasscodeSettingsActivity
import mega.privacy.android.app.presentation.twofactorauthentication.TwoFactorAuthenticationActivity
import mega.privacy.android.app.presentation.verifytwofactor.VerifyTwoFactorActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncActivity
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedResR

@AndroidEntryPoint
@SuppressLint("NewApi")
class SettingsFragment :
    PreferenceFragmentCompat(),
    FragmentResultListener {

    @Inject
    lateinit var additionalPreferences: Set<@JvmSuppressWildcards PreferenceResource>

    @Inject
    lateinit var viewModelPreferenceDataStoreFactory: ViewModelPreferenceDataStoreFactory

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    private var numberOfClicksAppVersion = 0

    private val viewModel: SettingsViewModel by viewModels()

    private val twoFactorAuthenticationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.refreshMultiFactorAuthSetting()
            }
        }

    private var cookiePolicyLink: String? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore =
            viewModelPreferenceDataStoreFactory.create(viewModel)
        setPreferencesFromResource(R.xml.preferences, rootKey)
        additionalPreferences.forEach {
            addPreferencesFromResource(it.resource)
        }
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
                    findPreference<Preference>(KEY_FEATURES_CAMERA_UPLOAD)?.apply {
                        isEnabled = state.cameraUploadsEnabled
                        summary =
                            getString(if (state.cameraUploadsOn) R.string.mute_chat_notification_option_on else R.string.mute_chatroom_notification_option_off)
                    }

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

                    findPreference<SwitchPreferenceCompat>(KEY_MEDIA_DISCOVERY_VIEW)?.let {
                        it.isChecked =
                            state.mediaDiscoveryViewState != MediaDiscoveryViewSettings.DISABLED.ordinal
                    }

                    findPreference<Preference>(KEY_FEATURES_SYNC)?.isEnabled = true
                    findPreference<Preference>(KEY_FEATURES_CHAT)?.isEnabled = state.chatEnabled
                    findPreference<Preference>(KEY_FEATURES_CALLS)?.isEnabled = state.callsEnabled
                    updatePasscodeLockSummary(state.passcodeLock)
                    updateSubFolderMediaDiscovery(state.subFolderMediaDiscoveryChecked)
                    findPreference<SwitchPreferenceCompat>(KEY_HIDDEN_ITEMS)?.apply {
                        val accountType = state.accountDetail?.levelDetail?.accountType
                        val isHiddenNodesEnabled = state.isHiddenNodesEnabled == true
                        isVisible = if (isHiddenNodesEnabled && accountType?.isPaid == true) {
                            if (accountType.isBusinessAccount) {
                                viewModel.getBusinessStatus() != BusinessAccountStatus.Expired
                            } else {
                                true
                            }
                        } else {
                            false
                        }

                        isChecked = state.showHiddenItems
                    }
                }
            }
        }
    }

    private fun updateSubFolderMediaDiscovery(checked: Boolean) {
        findPreference<SwitchPreferenceCompat>(KEY_SUB_FOLDER_MEDIA_DISCOVERY)?.apply {
            isVisible = true
            isChecked = checked
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
        refreshSummaries()
        resetCounters(null)
        super.onResume()
    }

    private fun refreshSummaries() {
        viewModel.refreshCameraUploadsOn()
    }

    private fun updatePasscodeLockSummary(enabled: Boolean) {
        findPreference<Preference>(KEY_PASSCODE_LOCK)?.setSummary(if (enabled) R.string.mute_chat_notification_option_on else R.string.mute_chatroom_notification_option_off)
    }

    /**
     * Specifies action when clicking an entry
     */
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val key = preference.key
        when (key) {
            KEY_FEATURES_CAMERA_UPLOAD -> {
                startActivity(Intent(context, SettingsCameraUploadsActivity::class.java).also {
                    val showDisableCU =
                        arguments?.getBoolean(INTENT_EXTRA_KEY_SHOW_DISABLE_CU, false)
                            ?: false

                    it.putExtra(INTENT_EXTRA_KEY_SHOW_DISABLE_CU, showDisableCU)
                })
            }

            KEY_FEATURES_SYNC -> {
                startActivity(Intent(context, SettingsSyncActivity::class.java))
            }

            KEY_FEATURES_CHAT ->
                startActivity(
                    Intent(
                        context,
                        ChatPreferencesActivity::class.java
                    )
                )

            KEY_FEATURES_CALLS ->
                startActivity(
                    Intent(
                        context,
                        SettingsCallsActivity::class.java
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

            KEY_PASSCODE_LOCK -> {
                lifecycleScope.launch {
                    startActivity(
                        Intent(
                            context,
                            PasscodeSettingsActivity::class.java
                        )
                    )
                }
            }

            KEY_CHANGE_PASSWORD -> startActivity(
                Intent(
                    context,
                    ChangePasswordActivity::class.java
                )
            )

            KEY_2FA -> if (viewModel.uiState.value.multiFactorAuthChecked) {
                twoFactorAuthenticationLauncher.launch(Intent(
                    context,
                    VerifyTwoFactorActivity::class.java
                ).apply {
                    putExtra(VerifyTwoFactorActivity.KEY_VERIFY_TYPE, Constants.DISABLE_2FA)
                })
            } else {
                twoFactorAuthenticationLauncher.launch(
                    Intent(
                        context,
                        TwoFactorAuthenticationActivity::class.java
                    )
                )
            }

            KEY_QR_CODE_AUTO_ACCEPT -> {
                viewModel.toggleAutoAcceptPreference()
            }

            KEY_HELP_CENTRE -> {
                context.launchUrl(HELP_CENTRE_URL)
            }

            KEY_HELP_SEND_FEEDBACK -> showEvaluatedAppDialog()
            KEY_ABOUT_PRIVACY_POLICY -> {
                context.launchUrl(PRIVACY_POLICY_URL)
            }

            KEY_ABOUT_TOS -> {
                context.launchUrl(TERMS_OF_SERVICE_URL)
            }

            KEY_ABOUT_CODE_LINK -> {
                context.launchUrl(GITHUB_URL)
            }

            KEY_ABOUT_APP_VERSION -> {
                if (++numberOfClicksAppVersion == 5) {
                    numberOfClicksAppVersion = 0
                    if (!MegaApplication.isShowInfoChatMessages) {
                        MegaApplication.isShowInfoChatMessages = true
                        view?.let {
                            Snackbar.make(
                                it,
                                R.string.show_info_chat_msg_enabled,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        MegaApplication.isShowInfoChatMessages = false
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

            KEY_CANCEL_ACCOUNT -> deleteAccountClicked()
            KEY_ABOUT_COOKIE_POLICY -> viewLifecycleOwner.lifecycleScope.launch {
                if (cookiePolicyLink.isNullOrEmpty()) {
                    cookiePolicyLink = viewModel.getCookiePolicyLink()
                }
                context.launchUrl(cookiePolicyLink)
            }

            KEY_COOKIE_SETTINGS -> startActivity(
                Intent(
                    context,
                    CookiePreferencesActivity::class.java
                )
            )

            KEY_AUDIO_BACKGROUND_PLAY_ENABLED -> {
                val checked = findPreference<SwitchPreferenceCompat>(
                    KEY_AUDIO_BACKGROUND_PLAY_ENABLED
                )?.isChecked == true
                viewModel.toggleBackgroundPlay(checked)
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
                checked?.let {
                    viewModel.hideRecentActivity(checked)
                }
            }

            KEY_MEDIA_DISCOVERY_VIEW -> {
                val checked =
                    findPreference<SwitchPreferenceCompat>(KEY_MEDIA_DISCOVERY_VIEW)?.isChecked
                checked?.let {
                    viewModel.mediaDiscoveryView(
                        if (checked)
                            MediaDiscoveryViewSettings.ENABLED.ordinal
                        else
                            MediaDiscoveryViewSettings.DISABLED.ordinal
                    )
                }
            }

            KEY_SUB_FOLDER_MEDIA_DISCOVERY -> {
                val checked =
                    findPreference<SwitchPreferenceCompat>(KEY_SUB_FOLDER_MEDIA_DISCOVERY)?.isChecked
                checked?.let {
                    viewModel.setSubFolderMediaDiscoveryEnabled(checked)
                }
            }

            KEY_HIDDEN_ITEMS -> {
                val checked =
                    findPreference<SwitchPreferenceCompat>(KEY_HIDDEN_ITEMS)?.isChecked
                viewModel.setShowHiddenItemsEnabled(checked ?: false)
            }
        }
        resetCounters(key)
        return super.onPreferenceTreeClick(preference)
    }

    private fun showEvaluatedAppDialog() {
        FeedBackDialog.newInstance(
            viewModel.uiState.value.email,
            viewModel.uiState.value.accountType
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
            .setPositiveButton(sharedResR.string.general_ok) { _, _ -> }
            .setTitle(title)
            .setMessage(message)
            .show()
    }

    private fun resetCounters(key: String?) {
        if (key != KEY_ABOUT_APP_VERSION) {
            numberOfClicksAppVersion = 0
        }
    }

    companion object {
        internal const val INITIAL_PREFERENCE = "initial"
        internal const val NAVIGATE_TO_INITIAL_PREFERENCE = "navigateToInitial"

        internal const val COOKIES_URI = "https://mega.io/cookie"
        internal const val GITHUB_URL = "https://github.com/meganz/android"
        internal const val TERMS_OF_SERVICE_URL = "https://mega.io/terms"
        internal const val PRIVACY_POLICY_URL = "https://mega.io/privacy"
        internal const val HELP_CENTRE_URL = "https://help.mega.io/installs-apps/mobile"
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        if (requestKey == REPORT_ISSUE) {
            val fragment = findPreference<Preference>(requestKey)?.fragment

            result.getString(fragment)?.let {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    it,
                    Snackbar.LENGTH_LONG
                ).apply {
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                        .maxLines = 5
                }.show()
            }
        }
    }

}
