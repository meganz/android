package mega.privacy.android.app.activities.settingsActivities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.AdvancedSettingsFragment;

import static mega.privacy.android.app.constants.BroadcastConstants.*;

public class AdvancedPreferencesActivity extends PreferencesBaseActivity {

    private AdvancedSettingsFragment sttAdvanced;

    private BroadcastReceiver offlineReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttAdvanced == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_ONLINE_OPTIONS_SETTING)) {
                boolean isOnline = intent.getBooleanExtra(ONLINE_OPTION, false);
                sttAdvanced.setOnlineOptions(isOnline);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aB.setTitle(getString(R.string.settings_advanced_features).toUpperCase());
        sttAdvanced = new AdvancedSettingsFragment();
        replaceFragment(sttAdvanced);
        registerReceiver(offlineReceiver, new IntentFilter(ACTION_UPDATE_ONLINE_OPTIONS_SETTING));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(offlineReceiver);
    }
}