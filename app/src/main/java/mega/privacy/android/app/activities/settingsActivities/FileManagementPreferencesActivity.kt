package mega.privacy.android.app.activities.settingsActivities

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.DialogTwoVerticalButtonsBinding
import mega.privacy.android.app.fragments.settingsFragments.SettingsFileManagementFragment
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.settings.filesettings.FilePreferencesViewModel
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

@AndroidEntryPoint
class FileManagementPreferencesActivity : PreferencesBaseActivity() {

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val viewModel: FilePreferencesViewModel by viewModels()
    private var sttFileManagement: SettingsFileManagementFragment? = null
    private var clearOfflineDialog: AlertDialog? = null
    private var clearRubbishBinDialog: AlertDialog? = null
    private var generalDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.settings_file_management_category)
        if (savedInstanceState == null) {
            sttFileManagement = SettingsFileManagementFragment()
            sttFileManagement?.let { replaceFragment(it) }
        } else {
            sttFileManagement =
                supportFragmentManager.findFragmentById(R.id.fragment_container) as? SettingsFileManagementFragment
        }
        if (savedInstanceState != null && savedInstanceState.getBoolean(
                CLEAR_OFFLINE_SHOWN,
                false
            )
        ) {
            showClearOfflineDialog()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(CLEAR_OFFLINE_SHOWN, isAlertDialogShown(clearOfflineDialog))
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissAlertDialogIfExists(clearOfflineDialog)
    }

    /**
     * Show Clear Offline confirmation dialog.
     */
    fun showClearOfflineDialog() {
        clearOfflineDialog = MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.clear_offline_confirmation))
            .setPositiveButton(getString(R.string.general_clear)) { _, _ ->
                viewModel.clearOffline()
            }
            .setNegativeButton(getString(R.string.general_dismiss), null)
            .create()
        clearOfflineDialog?.show()
    }

    /**
     * Show Clear Rubbish Bin dialog.
     */
    fun showClearRubbishBinDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(getFormattedStringOrDefault(R.string.context_clear_rubbish))
        builder.setMessage(getFormattedStringOrDefault(R.string.clear_rubbish_confirmation))
        builder.setPositiveButton(
            getFormattedStringOrDefault(R.string.general_clear)
        ) { _: DialogInterface?, _: Int ->
            val nC = NodeController(this)
            nC.cleanRubbishBin()
        }
        builder.setNegativeButton(
            getFormattedStringOrDefault(sharedR.string.general_dialog_cancel_button),
            null
        )
        clearRubbishBinDialog = builder.create()
        clearRubbishBinDialog?.show()
    }

    /**
     * Show confirmation clear all versions dialog.
     */
    fun showConfirmationClearAllVersions() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(getFormattedStringOrDefault(R.string.settings_file_management_delete_versions))
        builder.setMessage(getFormattedStringOrDefault(R.string.text_confirmation_dialog_delete_versions))
        builder.setPositiveButton(
            getFormattedStringOrDefault(R.string.context_delete)
        ) { _: DialogInterface?, _: Int ->
            viewModel.clearAllVersions()
        }
        builder.setNegativeButton(
            getFormattedStringOrDefault(sharedR.string.general_dialog_cancel_button),
            null
        )
        clearRubbishBinDialog = builder.create()
        clearRubbishBinDialog?.show()
    }

    /**
     * Show Rubbish bin not disabled dialog.
     */
    fun showRBNotDisabledDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        val inflater = this.layoutInflater
        val binding = DialogTwoVerticalButtonsBinding.inflate(inflater).root
        builder.setView(binding)
        val title = binding.findViewById<TextView>(R.id.dialog_title)
        title.text = getFormattedStringOrDefault(R.string.settings_rb_scheduler_enable_title)
        val text = binding.findViewById<TextView>(R.id.dialog_text)
        text.text = getFormattedStringOrDefault(R.string.settings_rb_scheduler_alert_disabling)
        val firstButton = binding.findViewById<Button>(R.id.dialog_first_button)
        firstButton.text = getFormattedStringOrDefault(R.string.button_plans_almost_full_warning)
        firstButton.setOnClickListener {
            generalDialog?.dismiss()
            megaNavigator.openUpgradeAccount(context = this)
            myAccountInfo.upgradeOpenedFrom = MyAccountInfo.UpgradeFrom.SETTINGS
        }
        val secondButton = binding.findViewById<Button>(R.id.dialog_second_button)
        secondButton.text = getFormattedStringOrDefault(R.string.button_not_now_rich_links)
        secondButton.setOnClickListener { generalDialog?.dismiss() }
        generalDialog = builder.create()
        generalDialog?.show()
    }

    /**
     * Method required to reset the rubbish bin info.
     */
    fun resetRubbishInfo() {
        if (sttFileManagement != null) {
            sttFileManagement?.resetRubbishInfo()
        }
    }

    companion object {
        private const val CLEAR_OFFLINE_SHOWN = "CLEAR_OFFLINE_SHOWN"
    }
}
