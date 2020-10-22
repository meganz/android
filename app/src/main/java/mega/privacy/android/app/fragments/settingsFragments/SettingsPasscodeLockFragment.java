package mega.privacy.android.app.fragments.settingsFragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.PasscodePreferencesActivity;
import mega.privacy.android.app.lollipop.PinLockActivityLollipop;

import static mega.privacy.android.app.constants.SettingsConstants.KEY_PIN_LOCK_CODE;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_PIN_LOCK_ENABLE;
import static mega.privacy.android.app.utils.Constants.SET_PIN;
import static mega.privacy.android.app.utils.LogUtil.logWarning;

public class SettingsPasscodeLockFragment extends SettingsBaseFragment implements Preference.OnPreferenceClickListener {

    private SwitchPreferenceCompat pinLockEnableSwitch;
    private Preference pinLockCode;
    private String ast = "";
    private String pinLockCodeTxt = "";
    private boolean pinLock = false;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_passcode);

        pinLockEnableSwitch = findPreference(KEY_PIN_LOCK_ENABLE);
        pinLockEnableSwitch.setOnPreferenceClickListener(this);

        pinLockCode = findPreference(KEY_PIN_LOCK_CODE);
        pinLockCode.setOnPreferenceClickListener(this);
        pinLockCode.setOnPreferenceChangeListener(this);

        updatePinLock();

        if (pinLock) {
            ast = "";
            if (pinLockCodeTxt.compareTo("") == 0) {
                ast = getString(R.string.settings_pin_lock_code_not_set);
            } else {
                for (int i = 0; i < pinLockCodeTxt.length(); i++) {
                    ast = ast + "*";
                }
            }
            pinLockCode.setSummary(ast);
            getPreferenceScreen().addPreference(pinLockCode);
        } else {
            getPreferenceScreen().removePreference(pinLockCode);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {
            case KEY_PIN_LOCK_ENABLE:
                pinLock = !pinLock;
                if (pinLock) {
                    ((PasscodePreferencesActivity) getActivity()).showPanelSetPinLock();
                } else {
                    dbH.setPinLockEnabled(false);
                    dbH.setPinLockCode("");
                    getPreferenceScreen().removePreference(pinLockCode);
                }
                break;

            case KEY_PIN_LOCK_CODE:
                resetPinLock();
                break;
        }

        return true;
    }

    @Override
    public void onResume() {
        prefs = dbH.getPreferences();
        updatePinLock();
        super.onResume();
    }

    private void updatePinLock() {
        if (prefs == null || prefs.getPinLockEnabled() == null) {
            cancelSetPinLock();
        } else {
            pinLock = Boolean.parseBoolean(prefs.getPinLockEnabled());
            pinLockEnableSwitch.setChecked(pinLock);
            pinLockCodeTxt = prefs.getPinLockCode();
            if (pinLockCodeTxt == null) {
                pinLockCodeTxt = "";
                dbH.setPinLockCode(pinLockCodeTxt);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        prefs = dbH.getPreferences();
        if (preference.getKey().compareTo(KEY_PIN_LOCK_CODE) == 0) {
            pinLockCodeTxt = (String) newValue;
            dbH.setPinLockCode(pinLockCodeTxt);

            ast = "";
            if (pinLockCodeTxt.compareTo("") == 0) {
                ast = getString(R.string.settings_pin_lock_code_not_set);
            } else {
                for (int i = 0; i < pinLockCodeTxt.length(); i++) {
                    ast = ast + "*";
                }
            }
            pinLockCode.setSummary(ast);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        prefs = dbH.getPreferences();
        if (requestCode != SET_PIN)
            return;

        if (resultCode == Activity.RESULT_OK) {
            afterSetPinLock();
        } else {
            logWarning("Set PIN ERROR");
        }

    }

    private void afterSetPinLock() {
        prefs = dbH.getPreferences();

        if (prefs != null) {
            pinLockCodeTxt = prefs.getPinLockCode();
            if (pinLockCodeTxt == null) {
                pinLockCodeTxt = "";
                dbH.setPinLockCode(pinLockCodeTxt);

            }
            ast = "";
            if (pinLockCodeTxt.compareTo("") == 0) {
                ast = getString(R.string.settings_pin_lock_code_not_set);
            } else {
                for (int i = 0; i < pinLockCodeTxt.length(); i++) {
                    ast = ast + "*";
                }
            }
            pinLockCode.setSummary(ast);
            getPreferenceScreen().addPreference(pinLockCode);
            dbH.setPinLockEnabled(true);
        }
    }

    public void cancelSetPinLock() {
        pinLock = false;
        pinLockEnableSwitch.setChecked(pinLock);
        dbH.setPinLockEnabled(false);
        dbH.setPinLockCode("");
    }

    private void resetPinLock() {
        Intent intent = new Intent(context, PinLockActivityLollipop.class);
        intent.setAction(PinLockActivityLollipop.ACTION_RESET_PIN_LOCK);
        startActivity(intent);
    }

    public void intentToPinLock() {
        Intent intent = new Intent(context, PinLockActivityLollipop.class);
        intent.setAction(PinLockActivityLollipop.ACTION_SET_PIN_LOCK);
        startActivityForResult(intent, SET_PIN);
    }
}
