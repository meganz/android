package mega.privacy.android.app.fragments.settingsFragments;

import android.content.Context;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class SettingsBaseFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    protected Context context;
    protected MegaApiAndroid megaApi;
    protected MegaChatApiAndroid megaChatApi;
    protected DatabaseHandler dbH;
    protected MegaPreferences prefs;

    public SettingsBaseFragment () {
        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        dbH = MegaApplication.getInstance().getDbH();
        prefs = dbH.getPreferences();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) { }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
