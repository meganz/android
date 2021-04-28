package mega.privacy.android.app.activities.exportMK

import android.os.Bundle
import androidx.activity.viewModels
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityExportRecoveryKeyBinding
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.utils.LogUtil.logWarning

class ExportRecoveryKeyActivity : PasscodeActivity() {

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
        viewModel.managePermissionsResult(this, requestCode, permissions, grantResults)
    }

    private fun setUpView() {
        binding.printMKButton.setOnClickListener { AccountController(this).printRK() }

        binding.copyMKButton.setOnClickListener { AccountController(this).copyMK(false) }

        binding.saveMKButton.setOnClickListener { viewModel.checkPermissionsBeforeSaveMK(this) }
    }
}