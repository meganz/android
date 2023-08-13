package mega.privacy.android.app.fragments.settingsFragments

import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricManager.from
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.PassCodeActivityContract
import mega.privacy.android.app.constants.SettingsConstants.KEY_FINGERPRINT_ENABLE
import mega.privacy.android.app.constants.SettingsConstants.KEY_PASSCODE_ENABLE
import mega.privacy.android.app.constants.SettingsConstants.KEY_REQUIRE_PASSCODE
import mega.privacy.android.app.constants.SettingsConstants.KEY_RESET_PASSCODE
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.PasscodeUtil
import mega.privacy.android.app.utils.wrapper.PasscodePreferenceWrapper
import timber.log.Timber
import java.util.concurrent.Executor
import javax.inject.Inject

@AndroidEntryPoint
class SettingsPasscodeLockFragment : SettingsBaseFragment() {

    companion object {
        private const val IS_REQUIRE_PASSCODE_DIALOG_SHOWN = "IS_REQUIRE_PASSCODE_DIALOG_SHOWN"
        private const val REQUIRE_PASSCODE_DIALOG_OPTION = "REQUIRE_PASSCODE_DIALOG_OPTION"
    }

    @Inject
    lateinit var passcodeUtil: PasscodeUtil

    @Inject
    internal lateinit var passcodePreferencesWrapper: PasscodePreferenceWrapper

    private lateinit var requirePasscodeDialog: AlertDialog

    private var passcodeSwitch: SwitchPreferenceCompat? = null
    private var resetPasscode: Preference? = null
    private var fingerprintSwitch: SwitchPreferenceCompat? = null
    private var requirePasscode: Preference? = null

    private var passcodeLock = false

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var pinLauncher: ActivityResultLauncher<Boolean>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinLauncher = registerForActivityResult(PassCodeActivityContract()) { isSuccess ->
            if (isSuccess) {
                enablePasscode()
            } else {
                Timber.w("Set PIN ERROR")
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_passcode)
        passcodeSwitch = findPreference(KEY_PASSCODE_ENABLE)
        passcodeSwitch?.setOnPreferenceClickListener {
            if (passcodeLock) {
                disablePasscode()
            } else {
                passcodeSwitch?.isChecked = false
                intentToPasscodeLock(false)
            }

            true
        }

        resetPasscode = findPreference(KEY_RESET_PASSCODE)
        resetPasscode?.setOnPreferenceClickListener {
            intentToPasscodeLock(true)
            true
        }

        fingerprintSwitch = findPreference(KEY_FINGERPRINT_ENABLE)
        fingerprintSwitch?.setOnPreferenceClickListener {
            if (fingerprintSwitch?.isChecked == false) {
                viewLifecycleOwner.lifecycleScope.launch {
                    passcodePreferencesWrapper.setFingerprintLockEnabled(false)
                }
            } else {
                showEnableFingerprint()
            }

            true
        }

        requirePasscode = findPreference(KEY_REQUIRE_PASSCODE)
        requirePasscode?.setOnPreferenceClickListener {
            showRequirePasscodeDialog(INVALID_POSITION)
            true
        }

        if (savedInstanceState != null
            && savedInstanceState.getBoolean(IS_REQUIRE_PASSCODE_DIALOG_SHOWN, false)
        ) {
            showRequirePasscodeDialog(savedInstanceState.getInt(REQUIRE_PASSCODE_DIALOG_OPTION))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            val passcodeEnabled = passcodePreferencesWrapper.isPasscodeEnabled()
            val passcode = passcodePreferencesWrapper.getPasscode()

            if (!passcodeEnabled || passcode == null) {
                disablePasscode()
            } else {
                enablePasscode()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (isRequirePasscodeDialogShow()) {
            outState.putBoolean(IS_REQUIRE_PASSCODE_DIALOG_SHOWN, true)
            outState.putInt(
                REQUIRE_PASSCODE_DIALOG_OPTION,
                requirePasscodeDialog.listView.checkedItemPosition
            )
        }

        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        setupFingerprintSetting()
    }

    /**
     * Checks if fingerprint setting should be enabled.
     */
    private fun setupFingerprintSetting() {
        if (passcodeSwitch?.isChecked == false) {
            return
        }

        when (val canAuthenticate = from(requireContext()).canAuthenticate(BIOMETRIC_STRONG)) {
            BIOMETRIC_SUCCESS -> {
                Timber.d("Show fingerprint setting, hardware available and fingerprint enabled.")
                fingerprintSwitch?.let { preferenceScreen.addPreference(it) }
                viewLifecycleOwner.lifecycleScope.launch {
                    fingerprintSwitch?.isChecked =
                        passcodePreferencesWrapper.isFingerPrintLockEnabled()
                }
            }

            else -> {
                fingerprintSwitch?.let { preferenceScreen.removePreference(it) }
                Timber.d("Error. Cannot show fingerprint setting: $canAuthenticate")
            }
        }
    }

    /**
     * Shows the dialog to enable fingerprint unlock.
     */
    private fun showEnableFingerprint() {
        if (!this::executor.isInitialized) {
            executor = ContextCompat.getMainExecutor(requireContext())
        }

        if (!this::biometricPrompt.isInitialized) {
            biometricPrompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        super.onAuthenticationError(errorCode, errString)
                        Timber.w("Error: $errString")
                        fingerprintSwitch?.isChecked = false
                    }

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult,
                    ) {
                        super.onAuthenticationSucceeded(result)
                        viewLifecycleOwner.lifecycleScope.launch {
                            passcodePreferencesWrapper.setFingerprintLockEnabled(true)
                        }
                        snackbarCallBack?.showSnackbar(
                            getString(R.string.confirmation_fingerprint_enabled)
                        )
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Timber.w("Authentication failed")
                    }
                })
        }

        if (!this::promptInfo.isInitialized) {
            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.title_enable_fingerprint))
                .setNegativeButtonText(getString(R.string.general_cancel))
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .build()
        }

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showRequirePasscodeDialog(selectedPosition: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val requiredTime = passcodePreferencesWrapper.getPasscodeTimeOut()
            requirePasscodeDialog =
                passcodeUtil.showRequirePasscodeDialog(selectedPosition, requireContext())
            requirePasscodeDialog.setOnDismissListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    val newTime = passcodePreferencesWrapper.getPasscodeTimeOut()
                    requirePasscode?.summary =
                        passcodeUtil.getRequiredPasscodeText(newTime)
                }

            }
        }
    }

    private fun isRequirePasscodeDialogShow(): Boolean =
        this::requirePasscodeDialog.isInitialized && requirePasscodeDialog.isShowing

    private fun enablePasscode() {
        passcodeLock = true
        passcodeSwitch?.isChecked = true
        preferenceScreen.apply {
            resetPasscode?.let { addPreference(it) }
            requirePasscode?.let { addPreference(it) }
        }

        setupFingerprintSetting()
        viewLifecycleOwner.lifecycleScope.launch {
            val requiredTime = passcodePreferencesWrapper.getPasscodeTimeOut()
            requirePasscode?.summary =
                passcodeUtil.getRequiredPasscodeText(requiredTime)
        }
    }

    private fun disablePasscode() {
        passcodeLock = false
        passcodeSwitch?.isChecked = false
        preferenceScreen.apply {
            resetPasscode?.let { removePreference(it) }
            fingerprintSwitch?.let { removePreference(it) }
            requirePasscode?.let { removePreference(it) }
        }

        passcodeUtil.disablePasscode()
    }

    /**
     * Launches an Intent to open passcode screen.
     */
    private fun intentToPasscodeLock(reset: Boolean) {
        pinLauncher.launch(reset)
    }

    override fun onDestroy() {
        if (isRequirePasscodeDialogShow()) {
            requirePasscodeDialog.dismiss()
        }

        super.onDestroy()
    }
}