package mega.privacy.android.app.exportRK

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.exportRK.ExportRecoveryKeyViewModel.Companion.ERROR_NO_SPACE
import mega.privacy.android.app.exportRK.ExportRecoveryKeyViewModel.Companion.GENERAL_ERROR
import mega.privacy.android.app.databinding.ActivityExportRecoveryKeyBinding
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.showAlert

class ExportRecoveryKeyActivity : PasscodeActivity() {

    companion object {
        const val WRITE_STORAGE_TO_SAVE_RK = 1
    }

    private val viewModel by viewModels<ExportRecoveryKeyViewModel>()

    private lateinit var binding: ActivityExportRecoveryKeyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExportRecoveryKeyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpView()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty() || requestCode != WRITE_STORAGE_TO_SAVE_RK) {
            logWarning("Permissions ${permissions[0]} not granted")
        }

        if (hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            viewModel.saveRK(this)
        } else {
            showSnackbar(StringResourcesUtils.getString(R.string.denied_write_permissions))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.manageActivityResult(this, requestCode, resultCode, data)
    }

    private fun setUpView() {
        binding.printMKButton.setOnClickListener { AccountController(this).printRK() }

        binding.copyMKButton.setOnClickListener {
            viewModel.copyRK(this) { copiedRK ->
                recoveryKeyCopied(
                    copiedRK
                )
            }
        }

        binding.saveMKButton.setOnClickListener {
            viewModel.checkPermissionsBeforeSaveRK(this) { exportedRK ->
                recoveryKeyExported(exportedRK)
            }
        }
    }

    /**
     * Shows the result of a copy RK action.
     *
     * @param copiedRK Message to show as copy RK action result.
     */
    private fun recoveryKeyCopied(copiedRK: String) {
        showAlert(
            this, StringResourcesUtils.getString(
                if (isTextEmpty(copiedRK)) R.string.general_text_error
                else R.string.copy_MK_confirmation
            ), null
        )
    }

    /**
     * Shows the result of an export RK action.
     *
     * @param exportedRK Message to show as export RK action result.
     */
    private fun recoveryKeyExported(exportedRK: String) {
        showSnackbar(
            StringResourcesUtils.getString(
                when (exportedRK) {
                    ERROR_NO_SPACE -> R.string.error_not_enough_free_space
                    GENERAL_ERROR -> R.string.general_text_error
                    else -> R.string.save_MK_confirmation //RK_EXPORTED
                }
            )
        )
    }

    private fun showSnackbar(text: String) {
        showSnackbar(binding.exportMKFragmentContainer, text)
    }
}