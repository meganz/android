package mega.privacy.android.app.activities.exportMK

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.exportMK.ExportRecoveryKeyViewModel.Companion.ERROR_NO_SPACE
import mega.privacy.android.app.activities.exportMK.ExportRecoveryKeyViewModel.Companion.GENERAL_ERROR
import mega.privacy.android.app.databinding.ActivityExportRecoveryKeyBinding
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.showAlert

class ExportRecoveryKeyActivity : PasscodeActivity() {

    private val viewModel by viewModels<ExportRecoveryKeyViewModel>()

    private lateinit var binding: ActivityExportRecoveryKeyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExportRecoveryKeyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpView()
        setUpObservers()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (!viewModel.managePermissionsResult(this, requestCode, permissions, grantResults)) {
            showSnackbar(StringResourcesUtils.getString(R.string.denied_write_permissions))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.manageActivityResult(this, requestCode, resultCode, data)
    }

    private fun setUpView() {
        binding.printMKButton.setOnClickListener { AccountController(this).printRK() }
        binding.copyMKButton.setOnClickListener { viewModel.copyMK(this) }
        binding.saveMKButton.setOnClickListener { viewModel.checkPermissionsBeforeSaveMK(this) }
    }

    private fun setUpObservers() {
        viewModel.onMKCopied().observe(this, ::copiedMKResult)
        viewModel.onMKExported().observe(this, ::exportedMKResult)
    }

    private fun copiedMKResult(copiedMK: String) {
        showAlert(
            this, StringResourcesUtils.getString(
                if (isTextEmpty(copiedMK)) R.string.general_text_error
                else R.string.copy_MK_confirmation
            ), null
        )
    }

    private fun exportedMKResult(exportedMK: String) {
        showSnackbar(
            StringResourcesUtils.getString(
                when (exportedMK) {
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