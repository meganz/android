package mega.privacy.android.app.activities.settingsActivities;

import android.os.Bundle;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.FileManagementSettingsFragment;

public class FileManagementPreferencesActivity extends PreferencesBaseActivity {

    private FileManagementSettingsFragment sttFileManagment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aB.setTitle(getString(R.string.settings_file_management_category).toUpperCase());

        sttFileManagment = new FileManagementSettingsFragment();
        replaceFragment(sttFileManagment);
    }
}
