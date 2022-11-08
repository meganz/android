package mega.privacy.android.app.fragments.settingsFragments;

import static android.text.format.Formatter.formatShortFileSize;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_AUTO_PLAY_SWITCH;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_CACHE;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_CLEAR_VERSIONS;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_DAYS_RB_SCHEDULER;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_ENABLE_RB_SCHEDULER;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_ENABLE_VERSIONS;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_FILE_VERSIONS;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_MOBILE_DATA_HIGH_RESOLUTION;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_OFFLINE;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_RUBBISH;
import static mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists;
import static mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity;
import mega.privacy.android.app.arch.extensions.ViewExtensionsKt;
import mega.privacy.android.app.globalmanagement.MyAccountInfo;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.listeners.SetAttrUserListener;
import mega.privacy.android.app.main.tasks.ManageCacheTask;
import mega.privacy.android.app.main.tasks.ManageOfflineTask;
import mega.privacy.android.app.presentation.settings.filesettings.FilePreferencesViewModel;
import mega.privacy.android.app.presentation.settings.filesettings.model.FilePreferencesState;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaAccountDetails;
import timber.log.Timber;

@AndroidEntryPoint
public class SettingsFileManagementFragment extends SettingsBaseFragment {

    private static final String IS_DISABLE_VERSIONS_SHOWN = "IS_DISABLE_VERSIONS_SHOWN";

    @Inject
    MyAccountInfo myAccountInfo;

    private FilePreferencesViewModel viewModel;

    private final static String INITIAL_VALUE = "0";
    private Preference offlineFileManagement;
    private Preference rubbishFileManagement;
    private Preference cacheAdvancedOptions;
    private SwitchPreferenceCompat enableRbSchedulerSwitch;
    private Preference daysRbSchedulerPreference;
    private SwitchPreferenceCompat enableVersionsSwitch;
    private Preference fileVersionsFileManagement;
    private Preference clearVersionsFileManagement;
    private SwitchPreferenceCompat autoPlaySwitch;
    private SwitchPreferenceCompat mobileDataHighResolution;

    private AlertDialog disableVersionsWarning;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_file_management);

        offlineFileManagement = findPreference(KEY_OFFLINE);
        offlineFileManagement.setOnPreferenceClickListener(this);

        cacheAdvancedOptions = findPreference(KEY_CACHE);
        cacheAdvancedOptions.setOnPreferenceClickListener(this);

        rubbishFileManagement = findPreference(KEY_RUBBISH);
        rubbishFileManagement.setOnPreferenceClickListener(this);

        enableRbSchedulerSwitch = findPreference(KEY_ENABLE_RB_SCHEDULER);
        enableRbSchedulerSwitch.setOnPreferenceClickListener(this);
        daysRbSchedulerPreference = findPreference(KEY_DAYS_RB_SCHEDULER);

        enableVersionsSwitch = findPreference(KEY_ENABLE_VERSIONS);
        updateEnabledFileVersions();
        fileVersionsFileManagement = findPreference(KEY_FILE_VERSIONS);

        clearVersionsFileManagement = findPreference(KEY_CLEAR_VERSIONS);
        clearVersionsFileManagement.setOnPreferenceClickListener(this);

        autoPlaySwitch = findPreference(KEY_AUTO_PLAY_SWITCH);
        autoPlaySwitch.setOnPreferenceClickListener(this);
        autoPlaySwitch.setChecked(prefs.isAutoPlayEnabled());

        mobileDataHighResolution = findPreference(KEY_MOBILE_DATA_HIGH_RESOLUTION);
        mobileDataHighResolution.setOnPreferenceClickListener(this);

        if (megaApi.serverSideRubbishBinAutopurgeEnabled()) {
            megaApi.getRubbishBinAutopurgePeriod(new GetAttrUserListener(context));
            getPreferenceScreen().addPreference(enableRbSchedulerSwitch);
            getPreferenceScreen().addPreference(daysRbSchedulerPreference);
            daysRbSchedulerPreference.setOnPreferenceClickListener(this);
        } else {
            getPreferenceScreen().removePreference(enableRbSchedulerSwitch);
            getPreferenceScreen().removePreference(daysRbSchedulerPreference);
        }

        cacheAdvancedOptions.setSummary(getString(R.string.settings_advanced_features_calculating));
        offlineFileManagement.setSummary(getString(R.string.settings_advanced_features_calculating));

        rubbishFileManagement.setSummary(getString(R.string.settings_advanced_features_size, myAccountInfo.getFormattedUsedRubbish()));


        taskGetSizeCache();
        taskGetSizeOffline();

        megaApi.getFileVersionsOption(new GetAttrUserListener(context));

        if (savedInstanceState != null
                && savedInstanceState.getBoolean(IS_DISABLE_VERSIONS_SHOWN, false)) {
            showWarningDisableVersions();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.getFilePreferencesStateLiveData().observe(getViewLifecycleOwner(), this::observeState);
        ViewExtensionsKt.collectFlow(getViewLifecycleOwner(), viewModel.getMonitorConnectivityEvent(), Lifecycle.State.STARTED, isConnected -> {
            setOnlineOptions(isConnected);
            return Unit.INSTANCE;
        });
    }

    private void observeState(FilePreferencesState filePreferencesState) {
        Integer versions = filePreferencesState.getNumberOfPreviousVersions();
        if (versions == null) {
            fileVersionsFileManagement.setSummary(getString(R.string.settings_advanced_features_calculating));
            getPreferenceScreen().removePreference(clearVersionsFileManagement);
        } else {
            String size = formatShortFileSize(requireActivity(), filePreferencesState.getSizeOfPreviousVersionsInBytes());
            String text = StringResourcesUtils.getQuantityString(R.plurals.settings_file_management_file_versions_subtitle, versions, versions, size);
            fileVersionsFileManagement.setSummary(text);

            if (versions > 0) {
                getPreferenceScreen().addPreference(clearVersionsFileManagement);
            } else {
                getPreferenceScreen().removePreference(clearVersionsFileManagement);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(IS_DISABLE_VERSIONS_SHOWN, isAlertDialogShown(disableVersionsWarning));
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        dismissAlertDialogIfExists(disableVersionsWarning);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {
            case KEY_OFFLINE:
                ((FileManagementPreferencesActivity) context).showClearOfflineDialog();
                break;

            case KEY_CACHE:
                ManageCacheTask clearCacheTask = new ManageCacheTask(true);
                clearCacheTask.execute();
                break;

            case KEY_RUBBISH:
                ((FileManagementPreferencesActivity) context).showClearRubbishBinDialog();
                break;

            case KEY_ENABLE_RB_SCHEDULER:
                if (!viewModel.isConnected())
                    return false;

                if (enableRbSchedulerSwitch.isChecked()) {
                    ((FileManagementPreferencesActivity) context).showRbSchedulerValueDialog(true);
                } else if (myAccountInfo.getAccountType() == MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                    ((FileManagementPreferencesActivity) context).showRBNotDisabledDialog();
                    enableRbSchedulerSwitch.setOnPreferenceClickListener(null);
                    enableRbSchedulerSwitch.setChecked(true);
                    enableRbSchedulerSwitch.setOnPreferenceClickListener(this);
                } else {
                    ((FileManagementPreferencesActivity) context).setRBSchedulerValue(INITIAL_VALUE);
                }
                break;

            case KEY_DAYS_RB_SCHEDULER:
                if (!viewModel.isConnected())
                    return false;

                ((FileManagementPreferencesActivity) context).showRbSchedulerValueDialog(false);
                break;

            case KEY_ENABLE_VERSIONS:
                if (!viewModel.isConnected())
                    return false;

                if (!enableVersionsSwitch.isChecked()) {
                    showWarningDisableVersions();
                    return false;
                }

                megaApi.setFileVersionsOption(!enableVersionsSwitch.isChecked(), new SetAttrUserListener(context));
                break;

            case KEY_CLEAR_VERSIONS:
                ((FileManagementPreferencesActivity) context).showConfirmationClearAllVersions();
                break;

            case KEY_AUTO_PLAY_SWITCH:
                dbH.setAutoPlayEnabled(String.valueOf(autoPlaySwitch.isChecked()));
                break;
        }
        return true;
    }

    @Override
    public void onResume() {
        taskGetSizeCache();
        taskGetSizeOffline();
        super.onResume();
    }


    /**
     * Method for reset the version information.
     */
    public void resetVersionsInfo() {
        String text = StringResourcesUtils.getQuantityString(R.plurals.settings_file_management_file_versions_subtitle, 0, 0, "0 B");
        fileVersionsFileManagement.setSummary(text);
        getPreferenceScreen().removePreference(clearVersionsFileManagement);
    }

    /**
     * Method for enable or disable the file versions.
     */
    public void updateEnabledFileVersions() {
        Timber.d("updateEnabledFileVersions: %s", MegaApplication.isDisableFileVersions());
        enableVersionsSwitch.setOnPreferenceClickListener(null);

        if (MegaApplication.isDisableFileVersions() == 1) {
            if (enableVersionsSwitch.isChecked()) {
                enableVersionsSwitch.setChecked(false);
            }
        } else if (MegaApplication.isDisableFileVersions() == 0) {
            if (!enableVersionsSwitch.isChecked()) {
                enableVersionsSwitch.setChecked(true);
            }
        } else {
            enableVersionsSwitch.setChecked(false);
        }

        enableVersionsSwitch.setOnPreferenceClickListener(this);
    }

    /**
     * Method for updating rubbish information.
     */
    public void setRubbishInfo() {
        rubbishFileManagement.setSummary(getString(R.string.settings_advanced_features_size, myAccountInfo.getFormattedUsedRubbish()));
    }

    public void taskGetSizeCache() {
        ManageCacheTask getCacheSizeTask = new ManageCacheTask(false);
        getCacheSizeTask.execute();
    }

    public void taskGetSizeOffline() {
        ManageOfflineTask getOfflineSizeTask = new ManageOfflineTask(false);
        getOfflineSizeTask.execute();
    }

    /**
     * Method for updating rubbish bin Scheduler.
     */
    public void updateRBScheduler(long daysCount) {
        Timber.d("updateRBScheduler: %s", daysCount);

        if (daysCount < 1) {
            enableRbSchedulerSwitch.setOnPreferenceClickListener(null);
            enableRbSchedulerSwitch.setChecked(false);
            enableRbSchedulerSwitch.setSummary(null);
            enableRbSchedulerSwitch.setOnPreferenceClickListener(this);
            //Hide preference to show days
            getPreferenceScreen().removePreference(daysRbSchedulerPreference);
            daysRbSchedulerPreference.setOnPreferenceClickListener(null);
        } else {
            enableRbSchedulerSwitch.setOnPreferenceClickListener(null);
            enableRbSchedulerSwitch.setChecked(true);

            if (myAccountInfo != null) {
                String subtitle = getString(R.string.settings_rb_scheduler_enable_subtitle);
                enableRbSchedulerSwitch.setSummary(subtitle + " " + getString(myAccountInfo.getAccountType() == MegaAccountDetails.ACCOUNT_TYPE_FREE ?
                        R.string.settings_rb_scheduler_enable_period_FREE :
                        R.string.settings_rb_scheduler_enable_period_PRO));
            }

            enableRbSchedulerSwitch.setOnPreferenceClickListener(this);

            //Show and set preference to show days
            getPreferenceScreen().addPreference(daysRbSchedulerPreference);
            daysRbSchedulerPreference.setOnPreferenceClickListener(this);
            daysRbSchedulerPreference.setSummary(getString(R.string.settings_rb_scheduler_select_days_subtitle, daysCount));
        }
    }

    /**
     * Method for reset the rubbish bin Scheduler.
     */
    public void resetRubbishInfo() {
        Timber.i("Updating size after clean the Rubbish Bin");
        String emptyString = getString(R.string.label_file_size_byte, INITIAL_VALUE);
        rubbishFileManagement.setSummary(getString(R.string.settings_advanced_features_size, emptyString));
        myAccountInfo.setFormattedUsedRubbish(emptyString);
    }

    public void setCacheSize(String size) {
        if (isAdded()) {
            cacheAdvancedOptions.setSummary(getString(R.string.settings_advanced_features_size, size));
        }
    }

    public void setOfflineSize(String size) {
        if (isAdded()) {
            offlineFileManagement.setSummary(getString(R.string.settings_advanced_features_size, size));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FilePreferencesViewModel.class);
        rubbishFileManagement.setEnabled(viewModel.isConnected() && megaApi != null && megaApi.getRootNode() != null);
        return v;
    }

    public void setOnlineOptions(boolean isOnline) {
        rubbishFileManagement.setEnabled(isOnline);
        daysRbSchedulerPreference.setEnabled(isOnline);
        enableRbSchedulerSwitch.setEnabled(isOnline);
        enableVersionsSwitch.setEnabled(isOnline);
        fileVersionsFileManagement.setEnabled(isOnline);
        clearVersionsFileManagement.setEnabled(isOnline);
        clearVersionsFileManagement.setLayoutResource(isOnline ? R.layout.delete_versions_preferences : R.layout.delete_versions_preferences_disabled);
    }

    private void showWarningDisableVersions() {
        enableVersionsSwitch.setChecked(true);
        disableVersionsWarning = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(StringResourcesUtils.getString(R.string.disable_versioning_label))
                .setMessage(StringResourcesUtils.getString(R.string.disable_versioning_warning))
                .setPositiveButton(StringResourcesUtils.getString(R.string.verify_2fa_subtitle_diable_2fa),
                        (dialog, which) -> megaApi.setFileVersionsOption(true, new SetAttrUserListener(context)))
                .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
                .show();
    }
}
