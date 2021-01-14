package mega.privacy.android.app.activities.settingsActivities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsAdvancedFragment;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE;
import static mega.privacy.android.app.utils.Constants.GO_OFFLINE;
import static mega.privacy.android.app.utils.Constants.GO_ONLINE;
import static mega.privacy.android.app.utils.Constants.INVALID_VALUE;
import static mega.privacy.android.app.utils.LogUtil.logDebug;

public class AdvancedPreferencesActivity extends PreferencesBaseActivity {

    private SettingsAdvancedFragment sttAdvanced;

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logDebug("Network broadcast received!");
            if (intent == null || intent.getAction() == null || sttAdvanced == null)
                return;

            int actionType = intent.getIntExtra(ACTION_TYPE, INVALID_VALUE);

            if (actionType == GO_OFFLINE) {
                sttAdvanced.setOnlineOptions(false);
            } else if (actionType == GO_ONLINE) {
                sttAdvanced.setOnlineOptions(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aB.setTitle(getString(R.string.settings_advanced_features).toUpperCase());

        sttAdvanced = new SettingsAdvancedFragment();
        replaceFragment(sttAdvanced);

        registerReceiver(networkReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkReceiver);
    }
}