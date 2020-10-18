package mega.privacy.android.app.activities.settingsActivities;

import android.os.Bundle;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.AdvancedSettingsFragment;

public class AdvancedPreferencesActivity extends PreferencesBaseActivity {

    private AdvancedSettingsFragment sttAdvanced;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aB.setTitle(getString(R.string.settings_advanced_features).toUpperCase());

        sttAdvanced = new AdvancedSettingsFragment();
        replaceFragment(sttAdvanced);
    }
}