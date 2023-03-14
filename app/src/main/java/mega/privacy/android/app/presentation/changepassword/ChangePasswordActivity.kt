package mega.privacy.android.app.presentation.changepassword

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.TestPasswordActivity
import mega.privacy.android.app.main.VerifyTwoFactorActivity
import mega.privacy.android.app.main.VerifyTwoFactorActivity.Companion.KEY_NEW_PASSWORD
import mega.privacy.android.app.main.VerifyTwoFactorActivity.Companion.KEY_VERIFY_TYPE
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.presentation.changepassword.view.ChangePasswordView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CHANGE_PASSWORD_2FA
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity for Change Password Screen
 * This activity has been migrated to Jetpack Compose
 * @see ChangePasswordScreen
 */
@AndroidEntryPoint
class ChangePasswordActivity : PasscodeActivity() {
    @ApplicationScope
    @Inject
    internal lateinit var sharingScope: CoroutineScope

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * ViewModel for Change Password Feature
     */
    private val viewModel: ChangePasswordViewModel by viewModels()

    /**
     * Activity On Create
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()

        setContent {
            ChangePasswordScreen()
        }
    }

    /**
     * Activity's init block, called once during on activity creation
     */
    private fun init() {
        if (intent.getStringExtra(KEY_LINK_TO_RESET).isNullOrBlank()) {
            intent.putExtra(KEY_LINK_TO_RESET, intent.dataString)
        }

        if (intent.getStringExtra(KEY_ACTION).isNullOrBlank()) {
            intent.putExtra(KEY_ACTION, intent.action)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Timber.d("onBackPressed")
                if (intent?.getBooleanExtra(KEY_IS_LOGOUT, false) == true) {
                    val intent = Intent(
                        this@ChangePasswordActivity,
                        TestPasswordActivity::class.java
                    ).apply {
                        putExtra(KEY_IS_LOGOUT, true)
                    }
                    startActivity(intent)
                }

                finish()
            }
        })
    }


    /**
     * Jetpack Compose of Change Password Feature
     */
    @Composable
    fun ChangePasswordScreen() {
        val themeMode by getThemeMode()
            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        AndroidTheme(isDark = themeMode.isDarkMode()) {
            LaunchedEffect(Unit) {
                viewModel.determineIfScreenIsResetPasswordMode()
            }

            ChangePasswordView(
                uiState = uiState,
                onTnCLinkClickListener = ::seeTermsAndConditions,
                onPasswordTextChanged = viewModel::checkPasswordStrength,
                onConfirmPasswordTextChanged = viewModel::validateConfirmPasswordToDefault,
                onSnackBarShown = viewModel::onSnackBarShown,
                onTriggerChangePassword = viewModel::onUserClickChangePassword,
                onTriggerResetPassword = viewModel::onExecuteResetPassword,
                onValidatePassword = viewModel::validatePassword,
                onValidateOnSave = viewModel::validateAllPasswordOnSave,
                onResetValidationState = viewModel::onResetPasswordValidation,
                onAfterPasswordChanged = ::handlePasswordChangedState,
                onAfterPasswordReset = ::handlePasswordResetState,
                onPromptedMultiFactorAuth = ::showMultiFactorAuthScreen,
                onFinishActivity = ::finish,
                onShowAlert = ::showAlert
            )
        }
    }

    private fun showMultiFactorAuthScreen(password: String) {
        navigateToMultiFactorAuthScreen(password)
        viewModel.onMultiFactorAuthShown()
    }

    private fun handlePasswordChangedState() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        if (intent != null && intent.getBooleanExtra(KEY_IS_LOGOUT, false)) {
            AccountController.logout(this, megaApi, sharingScope)
        } else {
            navigateAfterPasswordChanged()
        }

        viewModel.onPasswordChanged()
    }

    private fun handlePasswordResetState(isLoggedIn: Boolean, @StringRes errorCode: Int?) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        navigateAfterPasswordReset(isLoggedIn, errorCode)
        viewModel.onPasswordReset()
    }

    private fun navigateAfterPasswordChanged() {
        val intent = Intent(this, ManagerActivity::class.java).apply {
            action = Constants.ACTION_PASS_CHANGED
            putExtra(Constants.RESULT, MegaError.API_OK)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateAfterPasswordReset(isLoggedIn: Boolean, @StringRes errorCode: Int?) {
        val intent: Intent = if (isLoggedIn) {
            Intent(this, ManagerActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java).apply {
                putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
            }
        }.apply {
            action = Constants.ACTION_PASS_CHANGED
            putExtra(Constants.RESULT, errorCode)
        }

        startActivity(intent)
        finish()
    }

    private fun navigateToMultiFactorAuthScreen(password: String) {
        val intent = Intent(this, VerifyTwoFactorActivity::class.java).apply {
            putExtra(KEY_VERIFY_TYPE, CHANGE_PASSWORD_2FA)
            putExtra(KEY_NEW_PASSWORD, password)
            putExtra(KEY_IS_LOGOUT, intent != null && intent.getBooleanExtra(KEY_IS_LOGOUT, false))
        }

        startActivity(intent)
    }

    /**
     * On Menu Selected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /*
    * Action when TNC checkbox link is clicked
    */
    private fun seeTermsAndConditions() {
        try {
            val intent = Intent(this, WebViewActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = Uri.parse(Constants.URL_E2EE)
            }
            startActivity(intent)
        } catch (e: Exception) {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(Constants.URL_E2EE)
            }
            startActivity(viewIntent)
        }
    }

    private fun showAlert() {
        Util.showAlert(
            this,
            getString(R.string.general_text_error),
            getString(R.string.general_error_word)
        )
    }

    companion object {
        internal const val KEY_IS_LOGOUT = "logout"

        /**
         * Key value for link to reset password
         */
        const val KEY_LINK_TO_RESET = "key_link_to_reset"


        /**
         * Key to determine the activity action
         * either Change Password or Reset Password
         */
        const val KEY_ACTION = "key_action"
    }
}