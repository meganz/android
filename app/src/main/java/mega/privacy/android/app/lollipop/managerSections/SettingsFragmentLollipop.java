package mega.privacy.android.app.lollipop.managerSections;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.io.File;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.AdvancedPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.CameraUploadsPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.DownloadPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.PasscodePreferencesActivity;
import mega.privacy.android.app.components.TwoLineCheckPreference;
import mega.privacy.android.app.fragments.settingsFragments.SettingsBaseFragment;
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.TwoFactorAuthenticationActivity;

import static mega.privacy.android.app.constants.SettingsConstants.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.DBUtil.callToAccountDetails;
import static mega.privacy.android.app.utils.FileUtil.buildDefaultDownloadDir;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

@SuppressLint("NewApi")
public class SettingsFragmentLollipop extends SettingsBaseFragment {

    public int numberOfClicksSDK = 0;
    public int numberOfClicksKarere = 0;
    public int numberOfClicksAppVersion = 0;
    private PreferenceCategory securityCategory;
    private Preference recoveryKey;
    private Preference pinLockPreference;
    private Preference changePass;
    private SwitchPreferenceCompat twoFASwitch;
    private SwitchPreferenceCompat qrCodeAutoAcceptSwitch;
    private Preference advancedPreference;
    private RecyclerView listView;
    private boolean pinLock = false;
    private boolean setAutoaccept = false;
    private boolean autoAccept = true;
    private Preference cameraUploadsPreference;
    private Preference chatPreference;
    private PreferenceCategory storageCategory;
    private Preference nestedDownloadLocation;
    private Preference fileManagementPrefence;
    private Preference helpSendFeedback;
    private PreferenceCategory aboutCategory;
    private Preference aboutPrivacy;
    private Preference aboutTOS;
    private Preference aboutGDPR;
    private Preference codeLink;
    private Preference aboutSDK;
    private Preference aboutKarere;
    private Preference aboutApp;
    private Preference cancelAccount;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        cameraUploadsPreference = findPreference(KEY_FEATURES_CAMERA_UPLOAD);
        cameraUploadsPreference.setOnPreferenceClickListener(this);
        chatPreference = findPreference(KEY_FEATURES_CHAT);
        chatPreference.setOnPreferenceClickListener(this);

        storageCategory = findPreference(CATEGORY_STORAGE);
        nestedDownloadLocation = findPreference(KEY_STORAGE_DOWNLOAD);
        nestedDownloadLocation.setOnPreferenceClickListener(this);
        fileManagementPrefence = findPreference(KEY_STORAGE_FILE_MANAGEMENT);
        fileManagementPrefence.setOnPreferenceClickListener(this);

        securityCategory = findPreference(CATEGORY_SECURITY);
        recoveryKey = findPreference(KEY_RECOVERY_KEY);
        recoveryKey.setOnPreferenceClickListener(this);
        pinLockPreference = findPreference(KEY_PIN_LOCK);
        pinLockPreference.setOnPreferenceClickListener(this);
        changePass = findPreference(KEY_CHANGE_PASSWORD);
        changePass.setOnPreferenceClickListener(this);
        twoFASwitch = findPreference(KEY_2FA);
        twoFASwitch.setOnPreferenceClickListener(this);
        qrCodeAutoAcceptSwitch = findPreference(KEY_QR_CODE_AUTO_ACCEPT);
        qrCodeAutoAcceptSwitch.setOnPreferenceClickListener(this);
        advancedPreference = findPreference(KEY_SECURITY_ADVANCED);
        advancedPreference.setOnPreferenceClickListener(this);

        helpSendFeedback = findPreference(KEY_HELP_SEND_FEEDBACK);
        helpSendFeedback.setOnPreferenceClickListener(this);

        aboutCategory = findPreference(CATEGORY_ABOUT);
        aboutPrivacy = findPreference(KEY_ABOUT_PRIVACY_POLICY);
        aboutPrivacy.setOnPreferenceClickListener(this);
        aboutTOS = findPreference(KEY_ABOUT_TOS);
        aboutTOS.setOnPreferenceClickListener(this);
        aboutGDPR = findPreference(KEY_ABOUT_GDPR);
        aboutGDPR.setOnPreferenceClickListener(this);
        codeLink = findPreference(KEY_ABOUT_CODE_LINK);
        codeLink.setOnPreferenceClickListener(this);
        aboutApp = findPreference(KEY_ABOUT_APP_VERSION);
        aboutApp.setOnPreferenceClickListener(this);
        aboutSDK = findPreference(KEY_ABOUT_SDK_VERSION);
        aboutSDK.setOnPreferenceClickListener(this);
        aboutKarere = findPreference(KEY_ABOUT_KARERE_VERSION);
        aboutKarere.setOnPreferenceClickListener(this);
        cancelAccount = findPreference(KEY_CANCEL_ACCOUNT);
        cancelAccount.setOnPreferenceClickListener(this);

        updateCancelAccountSetting();

        if (prefs == null) {
            logWarning("prefs is NULL");
            dbH.setStorageAskAlways(true);
            File defaultDownloadLocation = buildDefaultDownloadDir(context);
            defaultDownloadLocation.mkdirs();
            dbH.setStorageDownloadLocation(defaultDownloadLocation.getAbsolutePath());
            dbH.setFirstTime(false);
        }

        updatePasscodeLock();
        refreshCameraUploadsSettings();
        update2FAVisibility();
        setAutoaccept = false;
        autoAccept = true;
    }

    /**
     * Method for update the Passcode lock section.
     */
    private void updatePasscodeLock() {
        if (prefs == null || prefs.getPinLockEnabled() == null) {
            pinLock = false;
            dbH.setPinLockEnabled(false);
        } else {
            pinLock = Boolean.parseBoolean(prefs.getPinLockEnabled());
        }
        updatePasscodeLockSubtitle();
    }

    public void updatePasscodeLockSubtitle() {
        pinLockPreference.setSummary(pinLock ? R.string.mute_chat_notification_option_on : R.string.mute_chatroom_notification_option_off);
    }

    /**
     * Refresh the Camera Uploads service settings depending on the service status.
     */
    public void refreshCameraUploadsSettings() {
        boolean isCameraUploadOn = false;
        prefs = dbH.getPreferences();
        if (prefs != null && prefs.getCamSyncEnabled() != null) {
            isCameraUploadOn = Boolean.parseBoolean(prefs.getCamSyncEnabled());
        }

        cameraUploadsPreference.setSummary(getString(isCameraUploadOn ? R.string.mute_chat_notification_option_on : R.string.mute_chatroom_notification_option_off));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        logDebug("onViewCreated");
        listView = view.findViewById(android.R.id.list);
        if (((ManagerActivityLollipop) context).openSettingsStorage) {
            goToCategoryStorage();
        } else if (((ManagerActivityLollipop) context).openSettingsQR) {
            goToCategoryQR();
        }
        if (listView != null) {
            listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    checkScroll();
                }
            });
        }
    }

    /**
     * Method for controlling whether or not to display the action bar elevation.
     */
    public void checkScroll() {
        if (listView != null) {
            ((ManagerActivityLollipop) context).changeActionBarElevation(listView.canScrollVertically(-1));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        setOnlineOptions(isOnline(context) && megaApi != null && megaApi.getRootNode() != null);
        return v;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        prefs = dbH.getPreferences();
        logDebug("KEY pressed: " + preference.getKey());
        Intent viewIntent;
        switch (preference.getKey()) {
            case KEY_FEATURES_CAMERA_UPLOAD:
                startActivity(new Intent(context, CameraUploadsPreferencesActivity.class));
                break;

            case KEY_FEATURES_CHAT:
                startActivity(new Intent(context, ChatPreferencesActivity.class));
                break;

            case KEY_STORAGE_DOWNLOAD:
                startActivity(new Intent(context, DownloadPreferencesActivity.class));
                break;

            case KEY_STORAGE_FILE_MANAGEMENT:
                startActivity(new Intent(context, FileManagementPreferencesActivity.class));
                break;

            case KEY_RECOVERY_KEY:
                ((ManagerActivityLollipop) context).showMKLayout();
                break;

            case KEY_PIN_LOCK:
                startActivity(new Intent(context, PasscodePreferencesActivity.class));
                break;

            case KEY_CHANGE_PASSWORD:
                startActivity(new Intent(context, ChangePasswordActivityLollipop.class));
                break;

            case KEY_2FA:
                if (((ManagerActivityLollipop) context).is2FAEnabled()) {
                    twoFASwitch.setChecked(true);
                    ((ManagerActivityLollipop) context).showVerifyPin2FA(DISABLE_2FA);
                } else {
                    twoFASwitch.setChecked(false);
                    Intent intent = new Intent(context, TwoFactorAuthenticationActivity.class);
                    startActivity(intent);
                }
                break;

            case KEY_QR_CODE_AUTO_ACCEPT:
                //			First query if QR auto-accept is enabled or not, then change the value
                setAutoaccept = true;
                megaApi.getContactLinksOption((ManagerActivityLollipop) context);
                break;

            case KEY_SECURITY_ADVANCED:
                startActivity(new Intent(context, AdvancedPreferencesActivity.class));
                break;

            case KEY_HELP_SEND_FEEDBACK:
                ((ManagerActivityLollipop) context).showEvaluatedAppDialog();
                break;

            case KEY_ABOUT_PRIVACY_POLICY:
                viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setData(Uri.parse("https://mega.nz/privacy"));
                startActivity(viewIntent);
                break;

            case KEY_ABOUT_TOS:
                viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setData(Uri.parse("https://mega.nz/terms"));
                startActivity(viewIntent);
                break;

            case KEY_ABOUT_GDPR:
                viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setData(Uri.parse("https://mega.nz/gdpr"));
                startActivity(viewIntent);
                break;

            case KEY_ABOUT_CODE_LINK:
                viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setData(Uri.parse("https://github.com/meganz/android"));
                startActivity(viewIntent);
                break;

            case KEY_ABOUT_APP_VERSION:
                logDebug("KEY_ABOUT_APP_VERSION pressed");
                numberOfClicksAppVersion++;
                if (numberOfClicksAppVersion == 5) {
                    if (!MegaApplication.isShowInfoChatMessages()) {
                        MegaApplication.setShowInfoChatMessages(true);
                        numberOfClicksAppVersion = 0;
                        ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.show_info_chat_msg_enabled), MEGACHAT_INVALID_HANDLE);
                    } else {
                        MegaApplication.setShowInfoChatMessages(false);
                        numberOfClicksAppVersion = 0;
                        ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.show_info_chat_msg_disabled), MEGACHAT_INVALID_HANDLE);
                    }
                }
                break;

            case KEY_ABOUT_SDK_VERSION:
                numberOfClicksSDK++;
                if (numberOfClicksSDK == 5) {
                    MegaAttributes attrs = dbH.getAttributes();

                    if (attrs != null && attrs.getFileLoggerSDK() != null && Boolean.parseBoolean(attrs.getFileLoggerSDK())) {
                        numberOfClicksSDK = 0;
                        setStatusLoggerSDK(context, false);
                    } else {
                        logWarning("SDK file logger attribute is NULL");
                        ((ManagerActivityLollipop) context).showConfirmationEnableLogsSDK();
                    }
                }
                break;

            case KEY_ABOUT_KARERE_VERSION:
                numberOfClicksKarere++;
                if (numberOfClicksKarere == 5) {
                    MegaAttributes attrs = dbH.getAttributes();

                    if (attrs != null && attrs.getFileLoggerKarere() != null && Boolean.parseBoolean(attrs.getFileLoggerKarere())) {
                        numberOfClicksKarere = 0;
                        setStatusLoggerKarere(context, false);
                    } else {
                        logWarning("Karere file logger attribute is NULL");
                        ((ManagerActivityLollipop) context).showConfirmationEnableLogsKarere();
                    }
                }
                break;

            case KEY_CANCEL_ACCOUNT:
                ((ManagerActivityLollipop) context).askConfirmationDeleteAccount();
                break;
        }

        if (preference.getKey().compareTo(KEY_ABOUT_APP_VERSION) != 0) {
            numberOfClicksAppVersion = 0;
        }

        if (preference.getKey().compareTo(KEY_ABOUT_SDK_VERSION) != 0) {
            numberOfClicksSDK = 0;
        }

        if (preference.getKey().compareTo(KEY_ABOUT_KARERE_VERSION) != 0) {
            numberOfClicksKarere = 0;
        }

        return true;
    }

    @Override
    public void onResume() {
        refreshAccountInfo();

        prefs = dbH.getPreferences();
        updatePasscodeLock();

        if (!isOnline(context)) {
            chatPreference.setEnabled(false);
            cameraUploadsPreference.setEnabled(false);
        }
        super.onResume();
    }

    /**
     * Update the Cancel Account settings.
     */
    public void updateCancelAccountSetting() {
        if (megaApi.isBusinessAccount() && !megaApi.isMasterBusinessAccount()) {
            aboutCategory.removePreference(cancelAccount);
        }
    }

    public void goToCategoryStorage() {
        scrollToPreference(storageCategory);
    }

    public void goToCategoryQR() {
        scrollToPreference(qrCodeAutoAcceptSwitch);
    }

    private void refreshAccountInfo() {
        //Check if the call is recently
        logDebug("Check the last call to getAccountDetails");
        MyAccountInfo myAccountInfo = MegaApplication.getInstance().getMyAccountInfo();
        if (callToAccountDetails() || myAccountInfo.getUsedFormatted().trim().length() <= 0) {
            ((MegaApplication) ((Activity) context).getApplication()).askForAccountDetails();
        }
    }

    public void setOnlineOptions(boolean isOnline) {
        cameraUploadsPreference.setEnabled(isOnline);
        chatPreference.setEnabled(isOnline);
        cancelAccount.setEnabled(isOnline);
        twoFASwitch.setEnabled(isOnline);
        qrCodeAutoAcceptSwitch.setEnabled(isOnline);
        cancelAccount.setEnabled(isOnline);
        cancelAccount.setLayoutResource(isOnline ? R.layout.cancel_account_preferences : R.layout.cancel_account_preferences_disabled);
    }

    public void update2FAVisibility() {
        if (megaApi == null && context != null && ((Activity) context).getApplication() != null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaApi != null) {
            if (megaApi.multiFactorAuthAvailable()) {
                twoFASwitch.setVisible(true);
                megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), (ManagerActivityLollipop) context);
            } else {
                twoFASwitch.setVisible(false);
            }
        }
    }

    public void hidePreferencesChat() {
        chatPreference.setEnabled(false);
    }

    public void update2FAPreference(boolean enabled) {
        twoFASwitch.setChecked(enabled);
    }

    public void setValueOfAutoaccept(boolean autoAccept) {
        qrCodeAutoAcceptSwitch.setChecked(autoAccept);
    }

    public boolean getSetAutoaccept() {
        return setAutoaccept;
    }

    public void setSetAutoaccept(boolean autoAccept) {
        this.setAutoaccept = autoAccept;
    }

    public boolean getAutoacceptSetting() {
        return autoAccept;
    }

    public void setAutoacceptSetting(boolean autoAccept) {
        this.autoAccept = autoAccept;
    }
}
