package mega.privacy.android.app.fragments.settingsFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.preference.Preference;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.TwoLineCheckPreference;

import static mega.privacy.android.app.constants.SettingsConstants.KEY_HTTPS_ONLY;
import static mega.privacy.android.app.utils.Util.isOnline;

public class SettingsAdvancedFragment extends SettingsBaseFragment {

    private boolean useHttpsOnlyValue = false;
    private TwoLineCheckPreference useHttpsOnly;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_advanced);

        useHttpsOnly = findPreference(KEY_HTTPS_ONLY);
        useHttpsOnly.setOnPreferenceClickListener(this);

        useHttpsOnlyValue = Boolean.parseBoolean(dbH.getUseHttpsOnly());
        useHttpsOnly.setChecked(useHttpsOnlyValue);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {
            case KEY_HTTPS_ONLY:
                useHttpsOnlyValue = useHttpsOnly.isChecked();
                dbH.setUseHttpsOnly(useHttpsOnlyValue);
                megaApi.useHttpsOnly(useHttpsOnlyValue);
                break;
        }

        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        useHttpsOnly.setEnabled(isOnline(context) && megaApi != null && megaApi.getRootNode() != null);
        return v;
    }

    public void setOnlineOptions(boolean isOnline) {
        useHttpsOnly.setEnabled(isOnline);
    }
}
