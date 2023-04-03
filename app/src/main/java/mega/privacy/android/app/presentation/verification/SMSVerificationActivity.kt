package mega.privacy.android.app.presentation.verification

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.verification.view.SMSVerificationView
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity for SMS Verification
 */
@AndroidEntryPoint
class SMSVerificationActivity : PasscodeActivity() {
    /**
     * [SMSVerificationViewModel]
     */
    private val viewModel: SMSVerificationViewModel by viewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val countryCodeLauncher = registerForActivityResult(SelectCountryCodeContract()) {
        it?.let { result ->
            viewModel.setSelectedCodes(
                selectedCountryCode = result.first,
                selectedCountryName = result.second,
                selectedDialCode = result.third
            )
        }
    }

    private val smsCodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                setResult(RESULT_OK)
                finish()
            }
        }

    private fun launchSMSCode() {
        val intent = Intent(this, SMSVerificationTextActivity::class.java).apply {
            viewModel.uiState.value.let {
                putExtra(SELECTED_COUNTRY_CODE, it.selectedDialCode)
                putExtra(ENTERED_PHONE_NUMBER, it.phoneNumber)
                putExtra(NAME_USER_LOCKED, it.isUserLocked)
            }
        }
        smsCodeLauncher.launch(intent)
    }

    private val onBackPressedCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Timber.d("onBackPressed")
                if (viewModel.uiState.value.isUserLocked) {
                    return
                }
                finish()
            }
        }
    }

    /**
     * listener for [MegaChatRequestHandler]
     */
    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        if (intent != null) {
            val isUserLocked = intent.getBooleanExtra(NAME_USER_LOCKED, false)
            viewModel.setIsUserLocked(isUserLocked = isUserLocked, context = this)
        }
        setContent {
            SMSVerificationScreen()
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    @Composable
    private fun SMSVerificationScreen() {
        val themeMode by getThemeMode()
            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        AndroidTheme(isDark = themeMode.isDarkMode()) {
            SMSVerificationView(
                state = uiState,
                onRegionSelection = {
                    countryCodeLauncher.launch(ArrayList(uiState.countryCallingCodes))
                },
                onPhoneNumberChange = viewModel::setPhoneNumber,
                onNotNowClicked = ::finish,
                onNextClicked = viewModel::validatePhoneNumber,
                onLogout = ::logout,
                onConsumeSMSCodeSentFinishedEvent = viewModel::onConsumeSMSCodeSentFinishedEvent,
                onSMSCodeSent = ::launchSMSCode
            )
        }
    }

    private fun logout() {
        AccountController.logout(this, megaApi, lifecycleScope)
    }

    companion object {
        /**
         * bundle key for COUNTRY_CODE
         */
        const val SELECTED_COUNTRY_CODE = "COUNTRY_CODE"

        /**
         * bundle key for ENTERED_PHONE_NUMBER
         */
        const val ENTERED_PHONE_NUMBER = "ENTERED_PHONE_NUMBER"
    }
}
