package mega.privacy.android.app.fragments.settingsFragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;

import mega.privacy.android.app.R;

public class AdvancedSettingsFragment extends SettingsBaseFragment implements Preference.OnPreferenceClickListener {


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_advanced);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(intent == null)
            return;
        switch (requestCode){
        }

    }
}
