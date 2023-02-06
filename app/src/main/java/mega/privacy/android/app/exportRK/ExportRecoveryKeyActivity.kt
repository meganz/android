package mega.privacy.android.app.exportRK

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityExportRecoveryKeyBinding
import mega.privacy.android.app.exportRK.model.RecoveryKeyUIState
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util.showAlert
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity to export or backup the Recovery Key
 */
@AndroidEntryPoint
class ExportRecoveryKeyActivity : PasscodeActivity() {
    private val viewModel by viewModels<ExportRecoveryKeyViewModel>()

    private lateinit var binding: ActivityExportRecoveryKeyBinding

    /**
     * Account Controller class
     */
    @Inject
    lateinit var accountController: AccountController

    private val downloadFolderActivityResult =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK || result.data != null) {
                viewModel.onExportRecoveryKey()
            }
        }

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportRecoveryKeyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpView()
        collectUIState()
    }

    /**
     * Initial view setup, called on activity creation
     */
    private fun setUpView() {
        with(binding) {
            MKButtonsLayout.post {
                if (isOverOneLine()) {
                    setVerticalLayout()
                }
            }
            printMKButton.setOnClickListener {
                viewModel.onPrintRecoveryKey()
            }
            copyMKButton.setOnClickListener {
                viewModel.onCopyRecoveryKey()
            }
            saveMKButton.setOnClickListener {
                onSaveButtonClick()
            }
        }
    }

    /**
     * Collect UI State from the View Model to update the view state
     * @see [RecoveryKeyUIState.ExportRecoveryKey]
     * @see [RecoveryKeyUIState.CopyRecoveryKey]
     */
    private fun collectUIState() {
        this.collectFlow(viewModel.uiState) { uiState ->
            when (uiState) {
                is RecoveryKeyUIState.CopyRecoveryKey -> {
                    copyRecoveryKey(uiState.key)
                }
                is RecoveryKeyUIState.ExportRecoveryKey -> {
                    onRecoveryKeyExported(
                        when {
                            uiState.key.isNullOrEmpty() -> GENERAL_ERROR
                            FileUtil.saveTextOnContentUri(
                                this@ExportRecoveryKeyActivity.contentResolver,
                                intent.data,
                                uiState.key
                            ) -> RK_EXPORTED
                            else -> GENERAL_ERROR
                        }
                    )
                }
                RecoveryKeyUIState.PrintRecoveryKey -> {
                    accountController.printRK()
                }
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
     * Action when save button is clicked. Will save to storage if permission granted
     * else will ask for permission to write external storage
     */
    private fun onSaveButtonClick() {
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
     * Action when permission has been asked to the user
     * Will save to storage if permission granted
     * else it will display a Snackbar telling that the user denied the request
     */
    private fun onPermissionAsked() {
        if (hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveRecoveryKeyToStorage()
        } else {
            showSnackbar(StringResourcesUtils.getString(R.string.denied_write_permissions))
        }
    }

    /**
     * Determines if one of those buttons show the content in more than one line.
     * @return True if one of those buttons show the content in more than one line, false otherwise.
     */
    private fun isOverOneLine(): Boolean {
        return binding.printMKButton.lineCount > 1
                || binding.copyMKButton.lineCount > 1
                || binding.saveMKButton.lineCount > 1
    }

    /**
     * Changes the buttons layout to vertical.
     */
    private fun setVerticalLayout() {
        binding.MKButtonsLayout.orientation = LinearLayout.VERTICAL
        updateViewParam(binding.copyMKButton)
        updateViewParam(binding.saveMKButton)
        updateViewParam(binding.printMKButton)
    }

    /**
     * Updates the button params.
     * @param view The target view which needs the update.
     */
    private fun updateViewParam(view: MaterialButton) {
        val params = view.layoutParams as LinearLayout.LayoutParams
        params.marginStart = 0

        view.apply {
            layoutParams = params
            strokeWidth = 0
            setPadding(0, 0, 0, 0)
            gravity = Gravity.START
        }
    }

    /**
     * Shows the result of a copy RK action.
     *
     * @param copiedRK Message to show as copy RK action result.
     */
    private fun onRKCopied(copiedRK: String?) {
        showAlert(
            this,
            StringResourcesUtils.getString(
                if (copiedRK.isNullOrBlank()) R.string.general_text_error
                else R.string.copy_MK_confirmation
            ),
            null
        )
    }

    /**
     * Shows the result of an export RK action.
     *
     * @param exportedRK Message to show as export RK action result.
     */
    private fun onRecoveryKeyExported(exportedRK: String) {
        showSnackbar(
            StringResourcesUtils.getString(
                when (exportedRK) {
                    ERROR_NO_SPACE -> R.string.error_not_enough_free_space
                    GENERAL_ERROR -> R.string.general_text_error
                    else -> R.string.save_MK_confirmation
                }
            )
        )
    }

    /**
     * Copy the recovery key to Clipboard.
     */
    private fun copyRecoveryKey(key: String?) {
        if (key.isNullOrBlank().not()) {
            TextUtil.copyToClipboard(this, key)
        }

        onRKCopied(key)
    }

    private fun showSnackbar(text: String) {
        showSnackbar(binding.exportMKFragmentContainer, text)
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

        /**
         * Message code stating that device has no storage left
         */
        const val ERROR_NO_SPACE = "ERROR_NO_SPACE"

        /**
         * Message code stating general error, or when recovery key is null or empty
         */
        const val GENERAL_ERROR = "GENERAL_ERROR"

        /**
         * Message code stating that recovery key has been exported
         */
        const val RK_EXPORTED = "RK_EXPORTED"
    }
}