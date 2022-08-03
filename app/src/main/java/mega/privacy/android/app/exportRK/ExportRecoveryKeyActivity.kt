package mega.privacy.android.app.exportRK

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import androidx.activity.viewModels
import com.google.android.material.button.MaterialButton
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityExportRecoveryKeyBinding
import mega.privacy.android.app.exportRK.ExportRecoveryKeyViewModel.Companion.ERROR_NO_SPACE
import mega.privacy.android.app.exportRK.ExportRecoveryKeyViewModel.Companion.GENERAL_ERROR
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.showAlert
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import timber.log.Timber

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
        grantResults: IntArray,
    ) {
        if (grantResults.isEmpty() || requestCode != WRITE_STORAGE_TO_SAVE_RK) {
            Timber.w("Permissions ${permissions[0]} not granted")
        }

        if (hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            viewModel.saveRK(this)
        } else {
            showSnackbar(StringResourcesUtils.getString(R.string.denied_write_permissions))
        }
    }

    @Suppress("deprecation") // TODO Migrate to registerForActivityResult()
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        viewModel.manageActivityResult(this, requestCode, resultCode, intent)
    }

    private fun setUpView() {
        binding.MKButtonsLayout.post {
            if (isOverOneLine()) {
                setVerticalLayout()
            }
        }

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
     * Determines if one of those buttons show the content in more than one line.
     *
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
     *
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
    private fun recoveryKeyCopied(copiedRK: String?) {
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