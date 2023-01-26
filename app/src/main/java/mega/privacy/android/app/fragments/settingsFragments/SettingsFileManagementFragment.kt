package mega.privacy.android.app.fragments.settingsFragments

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
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
import mega.privacy.android.app.listeners.GetAttrUserListener
import mega.privacy.android.app.main.tasks.ManageCacheTask
import mega.privacy.android.app.main.tasks.ManageOfflineTask
import mega.privacy.android.app.presentation.settings.filesettings.FilePreferencesViewModel
import mega.privacy.android.app.presentation.settings.filesettings.model.FilePreferencesState
import mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.MegaAccountDetails
import timber.log.Timber
import javax.inject.Inject

/**
 * The fragment for file management of settings
 */
@AndroidEntryPoint
class SettingsFileManagementFragment : SettingsBaseFragment() {

    /**
     * [MyAccountInfo] injection
     */
    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    private val viewModel by viewModels<FilePreferencesViewModel>()

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
        autoPlaySwitch?.isChecked = prefs.isAutoPlayEnabled()
        mobileDataHighResolution?.onPreferenceClickListener = this
        if (megaApi.serverSideRubbishBinAutopurgeEnabled()) {
            megaApi.getRubbishBinAutopurgePeriod(GetAttrUserListener(context))
            enableRbSchedulerSwitch?.let {
                preferenceScreen.addPreference(it)
            }
            daysRbSchedulerPreference?.let {
                preferenceScreen.addPreference(it)
                it.onPreferenceClickListener = this
            }
        } else {
            enableRbSchedulerSwitch?.let {
                preferenceScreen.removePreference(it)
            }
            daysRbSchedulerPreference?.let {
                preferenceScreen.removePreference(it)
            }
        }
        cacheAdvancedOptions?.summary = getString(R.string.settings_advanced_features_calculating)
        offlineFileManagement?.summary = getString(R.string.settings_advanced_features_calculating)
        rubbishFileManagement?.summary = getString(R.string.settings_advanced_features_size,
            myAccountInfo.formattedUsedRubbish)
        taskGetSizeCache()
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
        viewLifecycleOwner.collectFlow(viewModel.monitorConnectivityEvent,
            Lifecycle.State.STARTED) { isConnected ->
            setOnlineOptions(isConnected)
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
                fileVersionsFileManagement?.summary = StringResourcesUtils.getQuantityString(
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(IS_DISABLE_VERSIONS_SHOWN, isAlertDialogShown(disableVersionsWarning))
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissAlertDialogIfExists(disableVersionsWarning)
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            KEY_OFFLINE -> (context as FileManagementPreferencesActivity).showClearOfflineDialog()
            KEY_CACHE -> {
                val clearCacheTask = ManageCacheTask(true)
                clearCacheTask.execute()
            }
            KEY_RUBBISH -> (context as FileManagementPreferencesActivity).showClearRubbishBinDialog()
            KEY_ENABLE_RB_SCHEDULER -> {
                enableRbSchedulerSwitch?.let {
                    if (!viewModel.isConnected) return false
                    if (it.isChecked) {
                        (context as? FileManagementPreferencesActivity)?.showRbSchedulerValueDialog(
                            true)
                    } else if (myAccountInfo.accountType == MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                        (context as FileManagementPreferencesActivity).showRBNotDisabledDialog()
                        it.onPreferenceClickListener = null
                        it.isChecked = true
                        it.onPreferenceClickListener = this
                    } else {
                        (context as FileManagementPreferencesActivity).setRBSchedulerValue(
                            INITIAL_VALUE)
                    }
                }

            }
            KEY_DAYS_RB_SCHEDULER -> {
                if (!viewModel.isConnected) return false
                (context as? FileManagementPreferencesActivity)?.showRbSchedulerValueDialog(false)
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
                (context as? FileManagementPreferencesActivity)?.showConfirmationClearAllVersions()
            KEY_AUTO_PLAY_SWITCH ->
                autoPlaySwitch?.let {
                    dbH.setAutoPlayEnabled(it.isChecked.toString())
                }
        }
        return true
    }

    override fun onResume() {
        taskGetSizeCache()
        taskGetSizeOffline()
        super.onResume()
    }

    /**
     * Method for reset the version information.
     */
    fun resetVersionsInfo() {
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
     * The task for get cache size
     *
     * @return cache size
     */
    fun taskGetSizeCache() {
        val getCacheSizeTask = ManageCacheTask(false)
        getCacheSizeTask.execute()
    }

    /**
     * The task for get offline size
     *
     * @return offline size
     */
    fun taskGetSizeOffline() {
        val getOfflineSizeTask = ManageOfflineTask(false)
        getOfflineSizeTask.execute()
    }

    /**
     * Method for updating rubbish bin Scheduler.
     */
    fun updateRBScheduler(daysCount: Long) {
        Timber.d("updateRBScheduler: %s", daysCount)
        if (daysCount < 1) {
            enableRbSchedulerSwitch?.let {
                it.onPreferenceClickListener = null
                it.isChecked = false
                it.summary = null
                it.onPreferenceClickListener = this
            }
            daysRbSchedulerPreference?.let {
                //Hide preference to show days
                preferenceScreen.removePreference(it)
                it.onPreferenceClickListener = null
            }

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
                            R.string.settings_rb_scheduler_enable_period_PRO)
                }"
                it.onPreferenceClickListener = this
            }
            daysRbSchedulerPreference?.let {
                //Show and set preference to show days
                preferenceScreen.addPreference(it)
                it.onPreferenceClickListener = this
                it.summary =
                    getString(R.string.settings_rb_scheduler_select_days_subtitle, daysCount)
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
    fun setCacheSize(size: String?) {
        if (isAdded) {
            cacheAdvancedOptions?.summary =
                getString(R.string.settings_advanced_features_size, size)
        }
    }

    /**
     * Set offline size
     *
     * @param size offline size
     */
    fun setOfflineSize(size: String?) {
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
            .setTitle(StringResourcesUtils.getString(R.string.disable_versioning_label))
            .setMessage(StringResourcesUtils.getString(R.string.disable_versioning_warning))
            .setPositiveButton(StringResourcesUtils.getString(R.string.verify_2fa_subtitle_diable_2fa)
            ) { _, _ ->
                viewModel.enableFileVersionOption(false)
            }
            .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
            .show()
    }

    companion object {
        private const val IS_DISABLE_VERSIONS_SHOWN = "IS_DISABLE_VERSIONS_SHOWN"
        private const val INITIAL_VALUE = "0"
    }
}