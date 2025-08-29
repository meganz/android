package mega.privacy.android.app.fragments.settingsFragments

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.text.format.Formatter
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.StateEventWithContentTriggered
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.SettingsConstants.KEY_AUTO_PLAY_SWITCH
import mega.privacy.android.app.constants.SettingsConstants.KEY_CACHE
import mega.privacy.android.app.constants.SettingsConstants.KEY_CLEAR_VERSIONS
import mega.privacy.android.app.constants.SettingsConstants.KEY_DAYS_RB_SCHEDULER
import mega.privacy.android.app.constants.SettingsConstants.KEY_ENABLE_RB_SCHEDULER
import mega.privacy.android.app.constants.SettingsConstants.KEY_ENABLE_VERSIONS
import mega.privacy.android.app.constants.SettingsConstants.KEY_FILE_VERSIONS
import mega.privacy.android.app.constants.SettingsConstants.KEY_MOBILE_DATA_HIGH_RESOLUTION
import mega.privacy.android.app.constants.SettingsConstants.KEY_OFFLINE
import mega.privacy.android.app.constants.SettingsConstants.KEY_RUBBISH
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.presentation.settings.filesettings.FilePreferencesViewModel
import mega.privacy.android.app.presentation.settings.filesettings.model.FilePreferencesState
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaAccountDetails
import timber.log.Timber
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * The fragment for file management of settings
 */
@AndroidEntryPoint
class SettingsFileManagementFragment : SettingsBaseFragment(),
    Preference.OnPreferenceClickListener {

    /**
     * [MyAccountInfo] injection
     */
    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    private val viewModel by activityViewModels<FilePreferencesViewModel>()

    private var offlineFileManagement: Preference? = null
    private var rubbishFileManagement: Preference? = null
    private var cacheAdvancedOptions: Preference? = null
    private var enableRbSchedulerSwitch: SwitchPreferenceCompat? = null
    private var daysRbSchedulerPreference: Preference? = null
    private var enableVersionsSwitch: SwitchPreferenceCompat? = null
    private var fileVersionsFileManagement: Preference? = null
    private var clearVersionsFileManagement: Preference? = null
    private var autoPlaySwitch: SwitchPreferenceCompat? = null
    private var mobileDataHighResolution: SwitchPreferenceCompat? = null
    private var disableVersionsWarning: AlertDialog? = null
    private var newFolderDialog: AlertDialog? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_file_management)
        offlineFileManagement = findPreference(KEY_OFFLINE)
        cacheAdvancedOptions = findPreference(KEY_CACHE)
        rubbishFileManagement = findPreference(KEY_RUBBISH)
        enableRbSchedulerSwitch = findPreference(KEY_ENABLE_RB_SCHEDULER)
        daysRbSchedulerPreference = findPreference(KEY_DAYS_RB_SCHEDULER)
        enableVersionsSwitch = findPreference(KEY_ENABLE_VERSIONS)
        fileVersionsFileManagement = findPreference(KEY_FILE_VERSIONS)
        clearVersionsFileManagement = findPreference(KEY_CLEAR_VERSIONS)
        mobileDataHighResolution = findPreference(KEY_MOBILE_DATA_HIGH_RESOLUTION)
        autoPlaySwitch = findPreference(KEY_AUTO_PLAY_SWITCH)

        offlineFileManagement?.onPreferenceClickListener = this
        cacheAdvancedOptions?.onPreferenceClickListener = this
        rubbishFileManagement?.onPreferenceClickListener = this
        enableRbSchedulerSwitch?.onPreferenceClickListener = this
        clearVersionsFileManagement?.onPreferenceClickListener = this
        autoPlaySwitch?.onPreferenceClickListener = this
        autoPlaySwitch?.isChecked = prefs?.isAutoPlayEnabled() == true
        mobileDataHighResolution?.onPreferenceClickListener = this
        cacheAdvancedOptions?.summary = getString(R.string.settings_advanced_features_calculating)
        offlineFileManagement?.summary = getString(R.string.settings_advanced_features_calculating)
        rubbishFileManagement?.summary = getString(
            R.string.settings_advanced_features_size,
            myAccountInfo.formattedUsedRubbish
        )
        viewModel.getCacheSize()
        taskGetSizeOffline()
        if (savedInstanceState != null
            && savedInstanceState.getBoolean(IS_DISABLE_VERSIONS_SHOWN, false)
        ) {
            showWarningDisableVersions()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(viewModel.state) {
            observeState(it)
        }
        viewLifecycleOwner.collectFlow(
            viewModel.monitorConnectivityEvent,
            Lifecycle.State.STARTED
        ) { isConnected ->
            setOnlineOptions(isConnected)
        }
        viewLifecycleOwner.collectFlow(viewModel.monitorMyAccountUpdateEvent) {
            if (it.action == MyAccountUpdate.Action.UPDATE_ACCOUNT_DETAILS) {
                setRubbishInfo()
                viewModel.getRubbishBinAutopurgeInfo()
            }
        }
    }

    private fun observeState(filePreferencesState: FilePreferencesState) {
        val versions = filePreferencesState.numberOfPreviousVersions
        updateEnabledFileVersions(filePreferencesState.isFileVersioningEnabled)
        if (versions == null) {
            fileVersionsFileManagement?.summary =
                getString(R.string.settings_advanced_features_calculating)
            clearVersionsFileManagement?.let {
                preferenceScreen.removePreference(it)
            }
        } else {
            filePreferencesState.sizeOfPreviousVersionsInBytes?.let { sizeBytes ->
                val size = Formatter.formatShortFileSize(requireActivity(), sizeBytes)
                fileVersionsFileManagement?.summary = resources.getQuantityString(
                    R.plurals.settings_file_management_file_versions_subtitle,
                    versions,
                    versions,
                    size
                )
            }
            clearVersionsFileManagement?.let {
                if (versions > 0) {
                    preferenceScreen.addPreference(it)
                } else {
                    preferenceScreen.removePreference(it)
                }
            }
        }

        if (filePreferencesState.updateCacheSizeSetting != null) {
            setCacheSize(
                Util.getSizeString(
                    filePreferencesState.updateCacheSizeSetting,
                    requireContext()
                )
            )
            viewModel.resetUpdateCacheSizeSetting()
        }

        filePreferencesState.updateOfflineSize?.let { size ->
            val offlineSize = Util.getSizeString(size, requireContext())
            setOfflineSize(offlineSize)
            viewModel.resetUpdateOfflineSize()
        }

        if (filePreferencesState.deleteAllVersionsEvent is StateEventWithContentTriggered) {
            val exception = filePreferencesState.deleteAllVersionsEvent.content
            if (exception == null) {
                Util.showSnackbar(requireActivity(), getString(R.string.success_delete_versions))
                resetVersionsInfo()
            } else {
                Util.showSnackbar(requireActivity(), getString(R.string.error_delete_versions))
            }
            viewModel.resetDeleteAllVersionsEvent()
        }

        enableRbSchedulerSwitch?.isVisible = filePreferencesState.isRubbishBinAutopurgeEnabled
        daysRbSchedulerPreference?.isVisible = filePreferencesState.isRubbishBinAutopurgeEnabled
        updateRBScheduler(filePreferencesState.rubbishBinAutopurgePeriod)

        if (filePreferencesState.errorMessageId != 0) {
            Util.showSnackbar(requireActivity(), getString(filePreferencesState.errorMessageId))
            viewModel.resetErrorMessage()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(IS_DISABLE_VERSIONS_SHOWN, isAlertDialogShown(disableVersionsWarning))
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissAlertDialogIfExists(disableVersionsWarning)
        dismissAlertDialogIfExists(newFolderDialog)
    }


    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            KEY_OFFLINE -> (activity as? FileManagementPreferencesActivity)?.showClearOfflineDialog()
            KEY_CACHE -> {
                viewModel.clearCache()
            }

            KEY_RUBBISH -> (activity as? FileManagementPreferencesActivity)?.showClearRubbishBinDialog()
            KEY_ENABLE_RB_SCHEDULER -> {
                enableRbSchedulerSwitch?.let {
                    if (!viewModel.isConnected) return false
                    if (it.isChecked) {
                        showRbSchedulerValueDialog(true)
                    } else if (myAccountInfo.accountType == MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                        (activity as? FileManagementPreferencesActivity)?.showRBNotDisabledDialog()
                        it.onPreferenceClickListener = null
                        it.isChecked = true
                        it.onPreferenceClickListener = this
                    } else {
                        viewModel.setRubbishBinAutopurgePeriod(INITIAL_VALUE.toInt())
                    }
                }

            }

            KEY_DAYS_RB_SCHEDULER -> {
                if (!viewModel.isConnected) return false
                showRbSchedulerValueDialog(false)
            }

            KEY_ENABLE_VERSIONS -> {
                enableVersionsSwitch?.let {
                    if (!viewModel.isConnected) return false
                    if (!it.isChecked) {
                        showWarningDisableVersions()
                        return false
                    }
                    viewModel.enableFileVersionOption(it.isChecked)
                }
            }

            KEY_CLEAR_VERSIONS ->
                (activity as? FileManagementPreferencesActivity)?.showConfirmationClearAllVersions()

            KEY_AUTO_PLAY_SWITCH ->
                autoPlaySwitch?.let {
                    dbH.setAutoPlayEnabled(it.isChecked.toString())
                }
        }
        return true
    }

    override fun onResume() {
        viewModel.getCacheSize()
        taskGetSizeOffline()
        super.onResume()
    }

    /**
     * Method for reset the version information.
     */
    private fun resetVersionsInfo() {
        viewModel.resetVersionsInfo()
        clearVersionsFileManagement?.let {
            preferenceScreen.removePreference(it)
        }
    }

    /**
     * Method for enable or disable the file versions.
     */
    private fun updateEnabledFileVersions(enableFileVersions: Boolean) {
        Timber.d("updateEnabledFileVersions: $enableFileVersions")
        enableVersionsSwitch?.apply {
            onPreferenceClickListener = null
            isChecked = enableFileVersions
            onPreferenceClickListener = this@SettingsFileManagementFragment
        }
    }

    /**
     * Method for updating rubbish information.
     */
    fun setRubbishInfo() {
        rubbishFileManagement?.summary =
            getString(R.string.settings_advanced_features_size, myAccountInfo.formattedUsedRubbish)
    }

    /**
     * The task for get offline size
     *
     * @return offline size
     */
    fun taskGetSizeOffline() {
        viewModel.getOfflineFolderSize()
    }

    /**
     * Method for updating rubbish bin Scheduler.
     */
    private fun updateRBScheduler(daysCount: Int) {
        Timber.d("updateRBScheduler: %s", daysCount)
        if (daysCount < 1) {
            enableRbSchedulerSwitch?.let {
                it.onPreferenceClickListener = null
                it.isChecked = false
                it.summary = null
                it.onPreferenceClickListener = this
            }
            daysRbSchedulerPreference?.isVisible = false

        } else {
            enableRbSchedulerSwitch?.let {
                it.onPreferenceClickListener = null
                it.isChecked = true
                val subtitle = getString(R.string.settings_rb_scheduler_enable_subtitle)
                it.summary = "$subtitle ${
                    getString(
                        if (myAccountInfo.accountType == MegaAccountDetails.ACCOUNT_TYPE_FREE)
                            R.string.settings_rb_scheduler_enable_period_FREE
                        else
                            R.string.settings_rb_scheduler_enable_period_PRO
                    )
                }"
                it.onPreferenceClickListener = this
            }
            daysRbSchedulerPreference?.let {
                //Show and set preference to show days
                it.isVisible = true
                it.onPreferenceClickListener = this
                it.summary = context?.resources?.getQuantityString(
                    R.plurals.settings_file_management_remove_files_older_than_days,
                    daysCount,
                    daysCount
                )
            }
        }
    }

    /**
     * Method for reset the rubbish bin Scheduler.
     */
    fun resetRubbishInfo() {
        Timber.i("Updating size after clean the Rubbish Bin")
        val emptyString = getString(R.string.label_file_size_byte, INITIAL_VALUE)
        rubbishFileManagement?.summary =
            getString(R.string.settings_advanced_features_size, emptyString)
        myAccountInfo.formattedUsedRubbish = emptyString
    }

    /**
     * Set cache size
     *
     * @param size cache size
     */
    private fun setCacheSize(size: String?) {
        cacheAdvancedOptions?.summary =
            getString(R.string.settings_advanced_features_size, size)
    }

    /**
     * Set offline size
     *
     * @param size offline size
     */
    private fun setOfflineSize(size: String?) {
        if (isAdded) {
            offlineFileManagement?.summary =
                getString(R.string.settings_advanced_features_size, size)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        rubbishFileManagement?.isEnabled =
            viewModel.isConnected && megaApi != null && megaApi.rootNode != null
        return v
    }

    /**
     * Set online options
     *
     * @param isOnline ture is online, otherwise is false
     */
    fun setOnlineOptions(isOnline: Boolean) {
        rubbishFileManagement?.isEnabled = isOnline
        daysRbSchedulerPreference?.isEnabled = isOnline
        enableRbSchedulerSwitch?.isEnabled = isOnline
        enableVersionsSwitch?.isEnabled = isOnline
        fileVersionsFileManagement?.isEnabled = isOnline
        clearVersionsFileManagement?.isEnabled = isOnline
        clearVersionsFileManagement?.layoutResource =
            if (isOnline)
                R.layout.delete_versions_preferences
            else
                R.layout.delete_versions_preferences_disabled
    }

    private fun showWarningDisableVersions() {
        enableVersionsSwitch?.isChecked = true
        disableVersionsWarning = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.disable_versioning_label))
            .setMessage(getString(R.string.disable_versioning_warning))
            .setPositiveButton(
                getString(R.string.verify_2fa_subtitle_diable_2fa)
            ) { _, _ ->
                viewModel.enableFileVersionOption(false)
            }
            .setNegativeButton(getString(sharedR.string.general_dialog_cancel_button), null)
            .show()
    }

    /**
     * Show Rubbish bin scheduler value dialog.
     */
    private fun showRbSchedulerValueDialog(isEnabling: Boolean) {
        val activity = (activity as? FileManagementPreferencesActivity) ?: return
        val outMetrics = resources.displayMetrics
        val layout = LinearLayout(activity).apply {
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
        val input = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            layout.addView(this, layout.layoutParams)
            setSingleLine()
            setTextColor(
                getThemeColor(
                    activity,
                    com.google.android.material.R.attr.colorSecondary
                )
            )
            hint = activity.getFormattedStringOrDefault(R.string.hint_days)
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
                activity.getFormattedStringOrDefault(R.string.general_create),
                EditorInfo.IME_ACTION_DONE
            )
            requestFocus()
        }
        val text = TextView(activity)
        if (myAccountInfo.accountType > MegaAccountDetails.ACCOUNT_TYPE_FREE) {
            text.text =
                activity.getFormattedStringOrDefault(R.string.settings_rb_scheduler_enable_period_PRO)
        } else {
            text.text =
                activity.getFormattedStringOrDefault(R.string.settings_rb_scheduler_enable_period_FREE)
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
        val builder = MaterialAlertDialogBuilder(activity)
        builder.setTitle(activity.getFormattedStringOrDefault(R.string.settings_rb_scheduler_select_days_title))
        builder.setPositiveButton(
            activity.getFormattedStringOrDefault(sharedResR.string.general_ok)
        ) { _: DialogInterface?, _: Int -> }
        builder.setNegativeButton(
            getString(sharedR.string.general_dialog_cancel_button)
        ) { _: DialogInterface?, _: Int ->
            if (isEnabling) {
                updateRBScheduler(0)
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
            if (viewModel.isRubbishBinAutopurgePeriodValid(daysCount)) {
                viewModel.setRubbishBinAutopurgePeriod(daysCount)
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

    companion object {
        private const val IS_DISABLE_VERSIONS_SHOWN = "IS_DISABLE_VERSIONS_SHOWN"
        private const val INITIAL_VALUE = "0"
    }
}
