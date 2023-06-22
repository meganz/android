package mega.privacy.android.app.presentation.testpassword

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.testpassword.view.TestPasswordComposeView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Test Password Activity
 */
@AndroidEntryPoint
class TestPasswordActivity : PasscodeActivity(), MegaRequestListenerInterface {

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Account Controller
     */
    @Inject
    lateinit var accountController: AccountController

    private val activity = this@TestPasswordActivity
    private val viewModel: TestPasswordViewModel by viewModels()
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (psaWebBrowser != null && psaWebBrowser?.consumeBack() == true) return
            viewModel.dismissPasswordReminder(false)
        }
    }
    private val downloadFolderActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Timber.d("REQUEST_DOWNLOAD_FOLDER")
                var parentPath = result.data?.getStringExtra(FileStorageActivity.EXTRA_PATH)
                if (parentPath != null) {
                    Timber.d("parentPath no NULL")
                    parentPath = parentPath + File.separator + FileUtil.getRecoveryKeyFileName(this)
                    accountController.exportMK(parentPath)
                }
            }
        }

    /**
     * Activity onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent == null) {
            Timber.w("Intent NULL")
            return
        }

        handleSavedState(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setContent {
            TestPasswordScreen()
        }
    }

    private fun handleSavedState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            intent.putExtra(
                WRONG_PASSWORD_COUNTER,
                savedInstanceState.getInt(WRONG_PASSWORD_COUNTER, 0)
            )
            intent.putExtra(
                KEY_TEST_PASSWORD_MODE,
                savedInstanceState.getBoolean(KEY_TEST_PASSWORD_MODE, false)
            )
        }
    }

    /**
     * Test Password Screen in Jetpack Compose
     */
    @Composable
    fun TestPasswordScreen() {
        val themeMode by getThemeMode()
            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        AndroidTheme(isDark = themeMode.isDarkMode()) {
            TestPasswordComposeView(
                uiState = uiState,
                onResetUserMessage = viewModel::resetUserMessage,
                onCheckCurrentPassword = viewModel::checkForCurrentPassword,
                onTestPasswordClick = viewModel::switchToTestPasswordLayout,
                onCheckboxValueChanged = viewModel::setPasswordReminderBlocked,
                onDismiss = viewModel::dismissPasswordReminder,
                onResetPasswordVerificationState = viewModel::resetCurrentPasswordState,
                onUserLogout = ::logoutOrFinish,
                onResetUserLogout = viewModel::resetUserLogout,
                onFinishedCopyingRecoveryKey = ::showCopyDialog,
                onResetFinishedCopyingRecoveryKey = viewModel::resetFinishedCopyingRecoveryKey,
                onExhaustedPasswordAttempts = ::navigateToChangePassword,
                onResetExhaustedPasswordAttempts = viewModel::resetPasswordAttemptsState,
                onPrintRecoveryKey = ::printRecoveryKey,
                onCopyRecoveryKey = ::copyRecoveryKey,
                onSaveRecoveryKey = ::saveRecoveryKeyToStorage
            )
        }
    }

    private fun showCopyDialog(isFinished: Boolean) {
        if (isFinished) {
            AlertDialog.Builder(this).apply {
                setMessage(getString(R.string.copy_MK_confirmation))
                setPositiveButton(getString(R.string.action_logout)) { _, _ ->
                    viewModel.notifyPasswordReminderSucceeded()
                }
                setOnDismissListener {
                    viewModel.notifyPasswordReminderSucceeded()
                }
                show()
            }
        }
    }

    private fun logoutOrFinish(isLogout: Boolean) {
        if (isLogout) {
            viewModel.logout()
        } else {
            finish()
        }
    }

    /**
     * onSaveInstanceState
     * NOTE: This is needed in case the android systems kills the activity, in which case the view model state
     * will not be retained
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(WRONG_PASSWORD_COUNTER, viewModel.uiState.value.wrongPasswordAttempts)
        outState.putBoolean(KEY_TEST_PASSWORD_MODE, viewModel.uiState.value.isUITestPasswordMode)
    }

    /**
     * onOptionsItemSelected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun navigateToChangePassword() {
        val intent = Intent(activity, ChangePasswordActivity::class.java)
        intent.putExtra(ChangePasswordActivity.KEY_IS_LOGOUT, viewModel.uiState.value.isLogoutMode)
        startActivity(intent)
        onBackPressedDispatcher.onBackPressed()
    }

    /**
     * onRequestPermissionsResult
     * This permission was requested from [AccountController.exportMK]
     */
    @Deprecated("This permission result needs to be removed when permission is no longer requested from AccountController.exportMK")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.REQUEST_WRITE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("REQUEST_WRITE_STORAGE PERMISSIONS GRANTED")
                }
            }
        }
    }

    private fun printRecoveryKey() {
        accountController.printRK {
            viewModel.notifyPasswordReminderSucceeded()
        }
    }

    /**
     * Copy the recovery key to Clipboard.
     */
    private fun copyRecoveryKey() {
        lifecycleScope.launch {
            val key = viewModel.getRecoveryKey()

            if (key.isNullOrBlank().not()) {
                TextUtil.copyToClipboard(this@TestPasswordActivity, key)
            }
        }
    }

    /**
     * Open folder selection, where user can select the location the recovery key will be stored.
     */
    private fun saveRecoveryKeyToStorage() {
        val intent = Intent(this, FileStorageActivity::class.java).apply {
            action = FileStorageActivity.Mode.PICK_FOLDER.action
            putExtra(FileStorageActivity.EXTRA_SAVE_RECOVERY_KEY, true)
        }

        downloadFolderActivityResult.launch(intent)
    }

    /**
     * Callback after Recovery Key has been exported from AccountController
     * @see AccountController.exportMK
     */
    @Deprecated("Need to remove this in the future, it's dependent on AccountController.exportMK")
    fun onRecoveryKeyExported() {
        viewModel.notifyPasswordReminderSucceeded()
    }

    /**
     * onRequestStart
     */
    @Deprecated("Need to remove this when AccountController.logout has been converted into use case")
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
    }

    /**
     * onRequestUpdate
     */
    @Deprecated("Need to remove this when AccountController.logout has been converted into use case")
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
    }

    /**
     * onRequestFinish
     */
    @Deprecated("Need to remove this when AccountController.logout has been converted into use case")
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_LOGOUT) {
            Timber.d("END logout sdk request - wait chat logout")
        }
    }

    /**
     * onRequestTemporaryError
     */
    @Deprecated("Need to remove this when AccountController.logout has been converted into use case")
    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
    }

    companion object {
        /**
         * Tag for Wrong password counter
         */
        const val WRONG_PASSWORD_COUNTER = "counter"

        /**
         * Tag to determine if screen is in Logout Mode
         */
        const val KEY_IS_LOGOUT = "logout"

        /**
         * Tag to determine if screen is showing test password layout
         */
        const val KEY_TEST_PASSWORD_MODE = "test_password_mode"
    }
}