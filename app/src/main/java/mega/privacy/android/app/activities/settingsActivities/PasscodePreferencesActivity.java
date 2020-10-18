package mega.privacy.android.app.activities.settingsActivities;

import android.os.Bundle;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.PasscodeLockSettingsFragment;

public class PasscodePreferencesActivity extends PreferencesBaseActivity {

    private PasscodeLockSettingsFragment sttPasscodeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aB.setTitle(getString(R.string.settings_pin_lock_switch).toUpperCase());

        sttPasscodeLock = new PasscodeLockSettingsFragment();
        replaceFragment(sttPasscodeLock);
    }
}
