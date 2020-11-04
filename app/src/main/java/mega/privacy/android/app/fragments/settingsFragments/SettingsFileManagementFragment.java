package mega.privacy.android.app.fragments.settingsFragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity;
import mega.privacy.android.app.listeners.SettingsListener;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.tasks.ManageCacheTask;
import mega.privacy.android.app.lollipop.tasks.ManageOfflineTask;
import nz.mega.sdk.MegaAccountDetails;

import static mega.privacy.android.app.constants.SettingsConstants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class SettingsFileManagementFragment extends SettingsBaseFragment {

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

        if (megaApi.serverSideRubbishBinAutopurgeEnabled()) {
            megaApi.getRubbishBinAutopurgePeriod(new SettingsListener(context));
            getPreferenceScreen().addPreference(enableRbSchedulerSwitch);
            getPreferenceScreen().addPreference(daysRbSchedulerPreference);
            daysRbSchedulerPreference.setOnPreferenceClickListener(this);
        } else {
            getPreferenceScreen().removePreference(enableRbSchedulerSwitch);
            getPreferenceScreen().removePreference(daysRbSchedulerPreference);
        }

        cacheAdvancedOptions.setSummary(getString(R.string.settings_advanced_features_calculating));
        offlineFileManagement.setSummary(getString(R.string.settings_advanced_features_calculating));

        if (((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo() == null) {
            rubbishFileManagement.setSummary(getString(R.string.settings_advanced_features_calculating));
            fileVersionsFileManagement.setSummary(getString(R.string.settings_advanced_features_calculating));
            getPreferenceScreen().removePreference(clearVersionsFileManagement);
        } else {
            rubbishFileManagement.setSummary(getString(R.string.settings_advanced_features_size, ((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo().getFormattedUsedRubbish()));

            if (((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo().getNumVersions() == -1) {
                fileVersionsFileManagement.setSummary(getString(R.string.settings_advanced_features_calculating));
                getPreferenceScreen().removePreference(clearVersionsFileManagement);
            } else {
                setVersionsInfo();
            }
        }

        taskGetSizeCache();
        taskGetSizeOffline();

        megaApi.getFileVersionsOption(new SettingsListener(context));
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {
            case KEY_OFFLINE:
                ManageOfflineTask clearOfflineTask = new ManageOfflineTask(context, true);
                clearOfflineTask.execute();
                break;

            case KEY_CACHE:
                ManageCacheTask clearCacheTask = new ManageCacheTask(context, true);
                clearCacheTask.execute();
                break;

            case KEY_RUBBISH:
                ((FileManagementPreferencesActivity) context).showClearRubbishBinDialog();
                break;

            case KEY_ENABLE_RB_SCHEDULER:
                if (isOffline(context))
                    return false;

                if (enableRbSchedulerSwitch.isChecked()) {
                    ((FileManagementPreferencesActivity) context).showRbSchedulerValueDialog(true);
                } else {
                    MyAccountInfo myAccountInfo = ((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo();

                    if (myAccountInfo != null) {
                        if (myAccountInfo.getAccountType() == MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                            ((FileManagementPreferencesActivity) context).showRBNotDisabledDialog();
                            enableRbSchedulerSwitch.setOnPreferenceClickListener(null);
                            enableRbSchedulerSwitch.setChecked(true);
                            enableRbSchedulerSwitch.setOnPreferenceClickListener(this);
                        } else {
                            ((FileManagementPreferencesActivity) context).setRBSchedulerValue(INITIAL_VALUE);
                        }
                    }
                }
                break;

            case KEY_DAYS_RB_SCHEDULER:
                if (isOffline(context))
                    return false;

                ((FileManagementPreferencesActivity) context).showRbSchedulerValueDialog(false);
                break;

            case KEY_ENABLE_VERSIONS:
                if (isOffline(context))
                    return false;

                megaApi.setFileVersionsOption(!enableVersionsSwitch.isChecked(), new SettingsListener(context));
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
     * Method for updating version information.
     */
    public void setVersionsInfo() {
        MyAccountInfo myAccountInfo = ((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo();

        if (myAccountInfo == null)
            return;

        int numVersions = myAccountInfo.getNumVersions();
        logDebug("Num versions: " + numVersions);
        String previousVersions = myAccountInfo.getFormattedPreviousVersionsSize();
        String text = getString(R.string.settings_file_management_file_versions_subtitle, numVersions, previousVersions);
        logDebug("Previous versions: " + previousVersions);
        fileVersionsFileManagement.setSummary(text);

        if (numVersions > 0) {
            getPreferenceScreen().addPreference(clearVersionsFileManagement);
        } else {
            getPreferenceScreen().removePreference(clearVersionsFileManagement);
        }
    }

    /**
     * Method for reset the version information.
     */
    public void resetVersionsInfo() {
        String text = getString(R.string.settings_file_management_file_versions_subtitle, 0, "0 B");
        fileVersionsFileManagement.setSummary(text);
        getPreferenceScreen().removePreference(clearVersionsFileManagement);
    }

    /**
     * Method for enable or disable the file versions.
     */
    public void updateEnabledFileVersions() {
        logDebug("updateEnabledFileVersions: " + MegaApplication.isDisableFileVersions());
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
        rubbishFileManagement.setSummary(getString(R.string.settings_advanced_features_size, ((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo().getFormattedUsedRubbish()));
    }

    public void taskGetSizeCache() {
        ManageCacheTask getCacheSizeTask = new ManageCacheTask(context, false);
        getCacheSizeTask.execute();
    }

    public void taskGetSizeOffline() {
        ManageOfflineTask getOfflineSizeTask = new ManageOfflineTask(context, false);
        getOfflineSizeTask.execute();
    }

    /**
     * Method for updating rubbish bin Scheduler.
     */
    public void updateRBScheduler(long daysCount) {
        logDebug("updateRBScheduler: " + daysCount);

        if (daysCount < 1) {
            enableRbSchedulerSwitch.setOnPreferenceClickListener(null);
            enableRbSchedulerSwitch.setChecked(false);
            enableRbSchedulerSwitch.setSummary(null);
            enableRbSchedulerSwitch.setOnPreferenceClickListener(this);
            //Hide preference to show days
            getPreferenceScreen().removePreference(daysRbSchedulerPreference);
            daysRbSchedulerPreference.setOnPreferenceClickListener(null);
        } else {
            MyAccountInfo myAccountInfo = ((MegaApplication) ((Activity) context).getApplication()).getMyAccountInfo();
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
        logInfo("Updating size after clean the Rubbish Bin");
        String emptyString = getString(R.string.label_file_size_byte, INITIAL_VALUE);
        rubbishFileManagement.setSummary(getString(R.string.settings_advanced_features_size, emptyString));
        MegaApplication.getInstance().getMyAccountInfo().setFormattedUsedRubbish(emptyString);
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
        rubbishFileManagement.setEnabled(isOnline(context) && megaApi != null && megaApi.getRootNode() != null);
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
}
