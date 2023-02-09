package mega.privacy.android.app.presentation.settings.exportrecoverykey

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.presentation.settings.exportrecoverykey.view.ExportRecoveryKeyView
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util.showAlert
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity to export or backup the Recovery Key
 */
@AndroidEntryPoint
class ExportRecoveryKeyActivity : PasscodeActivity() {
    private val viewModel by viewModels<ExportRecoveryKeyViewModel>()

    /**
     * Account Controller class
     */
    @Inject
    lateinit var accountController: AccountController

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val downloadFolderActivityResult =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let {
                    exportRecoveryKey(it)
                }
            }
        }

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AndroidTheme(isDark = themeMode.isDarkMode()) {
                ExportRecoveryKeyView(
                    uiState = uiState,
                    onSnackBarShown = { viewModel.setSnackBarShown() },
                    onButtonOverflow = { viewModel.setActionGroupVertical() },
                    onClickPrint = { printRecoveryKey() },
                    onClickCopy = { copyRecoveryKey() },
                    onClickSave = { chooseRecoverySaveLocation() }
                )
            }
        }
    }

    /**
     * Callback for the result from requesting permissions.
     * @param requestCode   The request code passed in requestPermissions(Activity, String[], int)
     * @param permissions   The requested permissions. Never null.
     * @param grantResults  The grant results for the corresponding permissions which is either
     *                      PackageManager.PERMISSION_GRANTED or PackageManager.PERMISSION_DENIED. Never null.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty() || requestCode != WRITE_STORAGE_TO_SAVE_RK) {
            Timber.w("Permissions ${permissions[0]} not granted")
        }

        onPermissionAsked()
    }

    /**
     * Action when permission has been asked to the user
     * Will save to storage if permission granted
     * else it will display a Snackbar telling that the user denied the request
     */
    private fun onPermissionAsked() {
        if (hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveRecoveryKeyToStorage()
        } else {
            viewModel.showSnackBar(StringResourcesUtils.getString(R.string.denied_write_permissions))
        }
    }

    /**
     * Shows the result of a copy RK action.
     *
     * @param key Message to show as copy RK action result.
     */
    private fun onRecoveryKeyCopied(key: String?) {
        showAlert(
            this,
            StringResourcesUtils.getString(
                if (key.isNullOrBlank()) R.string.general_text_error
                else R.string.copy_MK_confirmation
            ),
            null
        )
    }

    /**
     * Action when save button is clicked. Will save to storage if permission granted
     * else will ask for permission to write external storage
     */
    private fun chooseRecoverySaveLocation() {
        if (hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveRecoveryKeyToStorage()
        } else {
            PermissionUtils.requestPermission(
                this,
                WRITE_STORAGE_TO_SAVE_RK,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Action when User finished choosing folder to save recovery key
     * Will show SnackBar message from Compose
     */
    private fun exportRecoveryKey(result: Intent) = lifecycleScope.launch {
        val key = viewModel.getRecoveryKey()

        val resId = when {
            key.isNullOrBlank() -> R.string.general_text_error
            isSaveToTextFileSuccessful(key, result) -> R.string.save_MK_confirmation
            else -> R.string.general_text_error
        }

        viewModel.showSnackBar(StringResourcesUtils.getString(resId))
    }

    /**
     * Saving the recovery key to text file
     * @return is save successful as [Boolean]
     */
    private suspend fun isSaveToTextFileSuccessful(key: String, result: Intent): Boolean =
        withContext(Dispatchers.IO) {
            FileUtil.saveTextOnContentUri(
                this@ExportRecoveryKeyActivity.contentResolver,
                result.data,
                key
            )
        }

    /**
     * Prints the recovery key using Account Controller
     * @see AccountController
     */
    private fun printRecoveryKey() {
        accountController.printRK()
    }

    /**
     * Copy the recovery key to Clipboard.
     */
    private fun copyRecoveryKey() = lifecycleScope.launch {
        val key = viewModel.getRecoveryKey()

        if (key.isNullOrBlank().not()) {
            TextUtil.copyToClipboard(this@ExportRecoveryKeyActivity, key)
        }

        onRecoveryKeyCopied(key)
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
         * Request code value indicating app is requesting `WRITE_EXTERNAL_STORAGE` permission
         * in order to save the Recovery Key
         */
        const val WRITE_STORAGE_TO_SAVE_RK = 1
    }
}