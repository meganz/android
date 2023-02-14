package mega.privacy.android.app.activities.settingsActivities


import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.KeyEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_REFRESH_CLEAR_OFFLINE_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_RESET_VERSION_INFO_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CACHE_SIZE_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_OFFLINE_SIZE_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RB_SCHEDULER
import mega.privacy.android.app.constants.BroadcastConstants.CACHE_SIZE
import mega.privacy.android.app.constants.BroadcastConstants.DAYS_COUNT
import mega.privacy.android.app.constants.BroadcastConstants.OFFLINE_SIZE
import mega.privacy.android.app.databinding.DialogTwoVerticalButtonsBinding
import mega.privacy.android.app.fragments.settingsFragments.SettingsFileManagementFragment
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.SetAttrUserListener
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.tasks.ManageOfflineTask
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaAccountDetails
import timber.log.Timber

@AndroidEntryPoint
class FileManagementPreferencesActivity : PreferencesBaseActivity() {
    private var sttFileManagement: SettingsFileManagementFragment? = null
    private var clearOfflineDialog: AlertDialog? = null
    private var clearRubbishBinDialog: AlertDialog? = null
    private var newFolderDialog: AlertDialog? = null
    private var generalDialog: AlertDialog? = null
    private val cacheSizeUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (sttFileManagement != null && intent.action == ACTION_UPDATE_CACHE_SIZE_SETTING) {
                sttFileManagement?.setCacheSize(intent.getStringExtra(CACHE_SIZE))
            }
        }
    }
    private val resetVersionInfoReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (sttFileManagement != null && intent.action == ACTION_RESET_VERSION_INFO_SETTING) {
                sttFileManagement?.resetVersionsInfo()
            }
        }
    }
    private val offlineSizeUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (sttFileManagement != null && intent.action == ACTION_UPDATE_OFFLINE_SIZE_SETTING) {
                sttFileManagement?.setOfflineSize(intent.getStringExtra(OFFLINE_SIZE))
            }
        }
    }
    private val updateCUSettingsReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (sttFileManagement != null && intent.action == ACTION_REFRESH_CLEAR_OFFLINE_SETTING) {
                sttFileManagement?.taskGetSizeOffline()
            }
        }
    }
    private val updateMyAccountReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (sttFileManagement != null && intent.action == Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS) {
                val actionType = intent.getIntExtra(ACTION_TYPE, Constants.INVALID_VALUE)
                if (actionType == Constants.UPDATE_ACCOUNT_DETAILS) {
                    if (!isFinishing) {
                        sttFileManagement?.setRubbishInfo()
                    }
                }
            }
        }
    }
    private val updateRBSchedulerReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (sttFileManagement != null) {
                val daysCount = intent.getLongExtra(DAYS_COUNT, Constants.INVALID_VALUE.toLong())
                if (daysCount != Constants.INVALID_VALUE.toLong()) {
                    sttFileManagement?.updateRBScheduler(daysCount)
                }
            }
        }
    }

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
        registerReceiver(
            cacheSizeUpdateReceiver,
            IntentFilter(ACTION_UPDATE_CACHE_SIZE_SETTING)
        )
        registerReceiver(
            offlineSizeUpdateReceiver,
            IntentFilter(ACTION_UPDATE_OFFLINE_SIZE_SETTING)
        )
        registerReceiver(
            updateMyAccountReceiver,
            IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
        )
        val filterUpdateCUSettings =
            IntentFilter(Constants.BROADCAST_ACTION_INTENT_SETTINGS_UPDATED)
        filterUpdateCUSettings.addAction(ACTION_REFRESH_CLEAR_OFFLINE_SETTING)
        registerReceiver(updateCUSettingsReceiver, filterUpdateCUSettings)
        registerReceiver(
            resetVersionInfoReceiver,
            IntentFilter(ACTION_RESET_VERSION_INFO_SETTING)
        )
        registerReceiver(
            updateRBSchedulerReceiver,
            IntentFilter(ACTION_UPDATE_RB_SCHEDULER)
        )
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
        unregisterReceiver(cacheSizeUpdateReceiver)
        unregisterReceiver(offlineSizeUpdateReceiver)
        unregisterReceiver(updateMyAccountReceiver)
        unregisterReceiver(updateCUSettingsReceiver)
        unregisterReceiver(resetVersionInfoReceiver)
        unregisterReceiver(updateRBSchedulerReceiver)
        dismissAlertDialogIfExists(clearOfflineDialog)
    }

    /**
     * Show Clear Offline confirmation dialog.
     */
    fun showClearOfflineDialog() {
        clearOfflineDialog = MaterialAlertDialogBuilder(this)
            .setMessage(getFormattedStringOrDefault(R.string.clear_offline_confirmation))
            .setPositiveButton(
                getFormattedStringOrDefault(R.string.general_clear)
            ) { _: DialogInterface?, _: Int ->
                ManageOfflineTask(true).execute()
            }
            .setNegativeButton(getFormattedStringOrDefault(R.string.general_dismiss), null)
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
        builder.setNegativeButton(getFormattedStringOrDefault(R.string.general_cancel), null)
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
            val nC = NodeController(this)
            nC.clearAllVersions()
        }
        builder.setNegativeButton(getFormattedStringOrDefault(R.string.general_cancel), null)
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
            startActivity(Intent(this, UpgradeAccountActivity::class.java))
            myAccountInfo.upgradeOpenedFrom = MyAccountInfo.UpgradeFrom.SETTINGS
        }
        val secondButton = binding.findViewById<Button>(R.id.dialog_second_button)
        secondButton.text = getFormattedStringOrDefault(R.string.button_not_now_rich_links)
        secondButton.setOnClickListener { generalDialog?.dismiss() }
        generalDialog = builder.create()
        generalDialog?.show()
    }

    /**
     * Update the Rubbish bin Scheduler value.
     *
     * @param value the new value.
     */
    fun setRBSchedulerValue(value: String) {
        Timber.d("Value: $value")
        val intValue = value.toInt()
        megaApi.setRubbishBinAutopurgePeriod(intValue, SetAttrUserListener(this))
    }

    /**
     * Method for controlling the selected option on the RbSchedulerValueDialog.
     *
     * @param value The value.
     * @param input The EditText.
     */
    private fun controlOptionOfRbSchedulerValueDialog(value: String, input: EditText) {
        if (value.isEmpty()) {
            return
        }
        try {
            val daysCount = value.toInt()
            val isNotFree = myAccountInfo.accountType > MegaAccountDetails.ACCOUNT_TYPE_FREE
            if (isNotFree && daysCount > RB_SCHEDULER_MINIMUM_PERIOD
                || daysCount in (RB_SCHEDULER_MINIMUM_PERIOD + 1) until RB_SCHEDULER_MAXIMUM_PERIOD
            ) {
                setRBSchedulerValue(value)
                newFolderDialog?.dismiss()
            } else {
                clearInputText(input)
            }
        } catch (e: Exception) {
            clearInputText(input)
        }
    }

    /**
     * Method for resetting the EditText values
     *
     * @param input The EditText.
     */
    private fun clearInputText(input: EditText) {
        input.text.clear()
        input.requestFocus()
    }

    /**
     * Method required to reset the rubbish bin info.
     */
    fun resetRubbishInfo() {
        if (sttFileManagement != null) {
            sttFileManagement?.resetRubbishInfo()
        }
    }

    /**
     * Show Rubbish bin scheduler value dialog.
     */
    fun showRbSchedulerValueDialog(isEnabling: Boolean) {
        val outMetrics = outMetrics
        val layout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            (layoutParams as LinearLayout.LayoutParams).setMargins(
                Util.scaleWidthPx(20, outMetrics),
                Util.scaleWidthPx(20, outMetrics),
                Util.scaleWidthPx(17, outMetrics),
                0
            )
        }
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            layout.addView(this, layout.layoutParams)
            setSingleLine()
            setTextColor(
                getThemeColor(
                    this@FileManagementPreferencesActivity,
                    R.attr.colorSecondary
                )
            )
            hint = getFormattedStringOrDefault(R.string.hint_days)
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { v: TextView, actionId: Int, _: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    controlOptionOfRbSchedulerValueDialog(
                        v.text.toString().trim { it <= ' ' },
                        this
                    )
                    return@setOnEditorActionListener true
                }
                false
            }
            setImeActionLabel(
                getFormattedStringOrDefault(R.string.general_create),
                EditorInfo.IME_ACTION_DONE
            )
            requestFocus()
        }
        val text = TextView(this@FileManagementPreferencesActivity)
        if (myAccountInfo.accountType > MegaAccountDetails.ACCOUNT_TYPE_FREE) {
            text.text =
                getFormattedStringOrDefault(R.string.settings_rb_scheduler_enable_period_PRO)
        } else {
            text.text =
                getFormattedStringOrDefault(R.string.settings_rb_scheduler_enable_period_FREE)
        }
        val density = resources.displayMetrics.density
        val scaleW = Util.getScaleW(outMetrics, density)
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11 * scaleW)
        layout.addView(text)
        val paramsTextError = text.layoutParams as LinearLayout.LayoutParams
        paramsTextError.height = WRAP_CONTENT
        paramsTextError.width = WRAP_CONTENT
        paramsTextError.setMargins(
            Util.scaleWidthPx(25, outMetrics),
            0,
            Util.scaleWidthPx(25, outMetrics),
            0
        )
        text.layoutParams = paramsTextError
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(getFormattedStringOrDefault(R.string.settings_rb_scheduler_select_days_title))
        builder.setPositiveButton(
            getFormattedStringOrDefault(R.string.general_ok)
        ) { _: DialogInterface?, _: Int -> }
        builder.setNegativeButton(
            getString(R.string.general_cancel)
        ) { _: DialogInterface?, _: Int ->
            if (isEnabling && sttFileManagement != null) {
                sttFileManagement?.updateRBScheduler(0)
            }
        }
        builder.setView(layout)
        newFolderDialog = builder.create()
        newFolderDialog?.window?.setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        newFolderDialog?.show()
        newFolderDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            controlOptionOfRbSchedulerValueDialog(
                input.text.toString().trim { it <= ' ' },
                input
            )
        }
    }

    companion object {
        private const val RB_SCHEDULER_MINIMUM_PERIOD = 6
        private const val RB_SCHEDULER_MAXIMUM_PERIOD = 31
        private const val CLEAR_OFFLINE_SHOWN = "CLEAR_OFFLINE_SHOWN"
    }
}
