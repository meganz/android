package mega.privacy.android.app.presentation.testpassword

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
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
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.testpassword.view.TestPasswordComposeView
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject

/**
 * Test Password Activity
 */
@AndroidEntryPoint
class TestPasswordActivity : PasscodeActivity() {

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

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
                val parentPath = result.data?.data
                if (parentPath != null) {
                    Timber.d("parentPath no NULL")
                    viewModel.exportRecoveryKey(parentPath.toString())
                }
            }
        }

    private val permissionsLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isPermissionGranted ->
            if (isPermissionGranted) {
                saveRecoveryKeyToStorage()
            } else {
                viewModel.setUserMessage(R.string.denied_write_permissions)
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
                onPrintRecoveryKey = viewModel::printRecoveryKey,
                onCopyRecoveryKey = ::copyRecoveryKey,
                onSaveRecoveryKey = ::chooseRecoverySaveLocation,
                onPrintRecoveryKeyConsumed = viewModel::resetPrintRecoveryKey,
                onPrintRecoveryKeyCompleted = {
                    viewModel.deleteRecoveryKeyFile(it)
                    viewModel.notifyPasswordReminderSucceeded()
                },
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
     * Action when save button is clicked. Will save to storage if permission granted
     * else will ask for permission to write external storage
     */
    private fun chooseRecoverySaveLocation() {
        if (PermissionUtils.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveRecoveryKeyToStorage()
        } else {
            permissionsLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
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