package mega.privacy.android.app.lollipop.managerSections;

import static mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_HIDE_RECENT_ACTIVITY;
import static mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_START_SCREEN;
import static mega.privacy.android.app.constants.SettingsConstants.CATEGORY_ABOUT;
import static mega.privacy.android.app.constants.SettingsConstants.CATEGORY_SECURITY;
import static mega.privacy.android.app.constants.SettingsConstants.CATEGORY_STORAGE;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_2FA;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_APP_VERSION;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_CODE_LINK;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_COOKIE_POLICY;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_KARERE_VERSION;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_PRIVACY_POLICY;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_SDK_VERSION;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_ABOUT_TOS;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_APPEARNCE_COLOR_THEME;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_AUDIO_BACKGROUND_PLAY_ENABLED;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_CANCEL_ACCOUNT;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_CHANGE_PASSWORD;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_COOKIE_SETTINGS;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_FEATURES;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_FEATURES_CAMERA_UPLOAD;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_FEATURES_CHAT;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_HELP_CENTRE;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_HELP_SEND_FEEDBACK;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_HIDE_RECENT_ACTIVITY;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_PASSCODE_LOCK;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_QR_CODE_AUTO_ACCEPT;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_RECOVERY_KEY;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_SECURITY_ADVANCED;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_START_SCREEN;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_DOWNLOAD;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_FILE_MANAGEMENT;
import static mega.privacy.android.app.fragments.settingsFragments.startSceen.util.StartScreenUtil.HOME_BNV;
import static mega.privacy.android.app.service.PlatformConstantsKt.RATE_APP_URL;
import static mega.privacy.android.app.utils.Constants.BUSINESS;
import static mega.privacy.android.app.utils.Constants.CLICKS_ENABLE_DEBUG;
import static mega.privacy.android.app.utils.Constants.DISABLE_2FA;
import static mega.privacy.android.app.utils.Constants.FREE;
import static mega.privacy.android.app.utils.Constants.MAIL_ANDROID;
import static mega.privacy.android.app.utils.Constants.PRO_I;
import static mega.privacy.android.app.utils.Constants.PRO_II;
import static mega.privacy.android.app.utils.Constants.PRO_III;
import static mega.privacy.android.app.utils.Constants.PRO_LITE;
import static mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.TYPE_TEXT_PLAIN;
import static mega.privacy.android.app.utils.DBUtil.callToAccountDetails;
import static mega.privacy.android.app.utils.FileUtil.buildDefaultDownloadDir;
import static mega.privacy.android.app.utils.LogUtil.areKarereLogsEnabled;
import static mega.privacy.android.app.utils.LogUtil.areSDKLogsEnabled;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.LogUtil.setStatusLoggerKarere;
import static mega.privacy.android.app.utils.LogUtil.setStatusLoggerSDK;
import static mega.privacy.android.app.utils.SharedPreferenceConstants.HIDE_RECENT_ACTIVITY;
import static mega.privacy.android.app.utils.SharedPreferenceConstants.PREFERRED_START_SCREEN;
import static mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES;
import static mega.privacy.android.app.utils.Util.getDeviceName;
import static mega.privacy.android.app.utils.Util.isAndroid11OrUpper;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.io.File;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.WebViewActivity;
import mega.privacy.android.app.activities.settingsActivities.AdvancedPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.CameraUploadsPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.CookiePreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.DownloadPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.PasscodePreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.StartScreenPreferencesActivity;
import mega.privacy.android.app.exportRK.ExportRecoveryKeyActivity;
import mega.privacy.android.app.fragments.settingsFragments.SettingsBaseFragment;
import mega.privacy.android.app.globalmanagement.MyAccountInfo;
import mega.privacy.android.app.lollipop.ChangePasswordActivity;
import mega.privacy.android.app.lollipop.ManagerActivity;
import mega.privacy.android.app.lollipop.TwoFactorAuthenticationActivity;
import mega.privacy.android.app.lollipop.VerifyTwoFactorActivity;
import mega.privacy.android.app.lollipop.managerSections.settings.Settings;
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService;
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService;
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceBinder;
import mega.privacy.android.app.utils.ThemeHelper;

/**
 * Fragment class that is deprecated and subject for removal.
 * Its replacement is named SettingsFragment
 */
@AndroidEntryPoint
@SuppressLint("NewApi")
@SuppressWarnings( "deprecation" )
public class OldSettingsFragment extends SettingsBaseFragment implements Settings {

    private static final String EVALUATE_APP_DIALOG_SHOW = "EvaluateAppDialogShow";

    @Inject
    MyAccountInfo myAccountInfo;

    public int numberOfClicksSDK = 0;
    public int numberOfClicksKarere = 0;
    public int numberOfClicksAppVersion = 0;
    private ListPreference colorThemeListPreference;
    private PreferenceCategory securityCategory;
    private Preference recoveryKey;
    private Preference passcodeLockPreference;
    private Preference changePass;
    private SwitchPreferenceCompat twoFASwitch;
    private SwitchPreferenceCompat qrCodeAutoAcceptSwitch;
    private Preference advancedPreference;
    private boolean passcodeLock = false;
    private boolean setAutoaccept = false;
    private boolean autoAccept = true;
    private Preference cameraUploadsPreference;
    private Preference chatPreference;
    private PreferenceCategory storageCategory;
    private Preference nestedDownloadLocation;
    private Preference fileManagementPrefence;
    private Preference startScreen;
    private SwitchPreferenceCompat hideRecentActivity;
    private Preference helpHelpCentre;
    private Preference helpSendFeedback;
    private PreferenceCategory aboutCategory;
    private Preference aboutPrivacy;
    private Preference aboutTOS;
    private Preference codeLink;
    private Preference aboutSDK;
    private Preference aboutKarere;
    private Preference aboutApp;
    private Preference cancelAccount;
    private MediaPlayerService playerService;

    private DisplayMetrics outMetrics;
    private boolean bEvaluateAppDialogShow = false;
    private AlertDialog evaluateAppDialog;

    private final ServiceConnection mediaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            playerService = ((MediaPlayerServiceBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playerService = null;
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        colorThemeListPreference = findPreference(KEY_APPEARNCE_COLOR_THEME);
        colorThemeListPreference.setOnPreferenceChangeListener(this);

        cameraUploadsPreference = findPreference(KEY_FEATURES_CAMERA_UPLOAD);
        cameraUploadsPreference.setOnPreferenceClickListener(this);
        chatPreference = findPreference(KEY_FEATURES_CHAT);
        chatPreference.setOnPreferenceClickListener(this);

        storageCategory = findPreference(CATEGORY_STORAGE);

        //Some changes are harder to toggle with feature flags, eg. layout file changes. To prevent a crash the original code had to be changed here.
        if (!isAndroid11OrUpper()) {
            nestedDownloadLocation = findPreference(KEY_STORAGE_DOWNLOAD);
            nestedDownloadLocation.setOnPreferenceClickListener(this);
        }

        fileManagementPrefence = findPreference(KEY_STORAGE_FILE_MANAGEMENT);
        fileManagementPrefence.setOnPreferenceClickListener(this);

        startScreen = findPreference(KEY_START_SCREEN);
        startScreen.setOnPreferenceClickListener(this);
        hideRecentActivity = findPreference(KEY_HIDE_RECENT_ACTIVITY);
        hideRecentActivity.setOnPreferenceClickListener(this);

        securityCategory = findPreference(CATEGORY_SECURITY);
        recoveryKey = findPreference(KEY_RECOVERY_KEY);
        recoveryKey.setOnPreferenceClickListener(this);
        passcodeLockPreference = findPreference(KEY_PASSCODE_LOCK);
        passcodeLockPreference.setOnPreferenceClickListener(this);
        changePass = findPreference(KEY_CHANGE_PASSWORD);
        changePass.setOnPreferenceClickListener(this);
        twoFASwitch = findPreference(KEY_2FA);
        twoFASwitch.setOnPreferenceClickListener(this);
        qrCodeAutoAcceptSwitch = findPreference(KEY_QR_CODE_AUTO_ACCEPT);
        qrCodeAutoAcceptSwitch.setOnPreferenceClickListener(this);
        advancedPreference = findPreference(KEY_SECURITY_ADVANCED);
        advancedPreference.setOnPreferenceClickListener(this);

        helpHelpCentre = findPreference(KEY_HELP_CENTRE);
        helpHelpCentre.setOnPreferenceClickListener(this);
        helpSendFeedback = findPreference(KEY_HELP_SEND_FEEDBACK);
        helpSendFeedback.setOnPreferenceClickListener(this);

        aboutCategory = findPreference(CATEGORY_ABOUT);
        aboutPrivacy = findPreference(KEY_ABOUT_PRIVACY_POLICY);
        aboutPrivacy.setOnPreferenceClickListener(this);
        aboutTOS = findPreference(KEY_ABOUT_TOS);
        aboutTOS.setOnPreferenceClickListener(this);
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
        findPreference(KEY_ABOUT_COOKIE_POLICY).setOnPreferenceClickListener(this);
        findPreference(KEY_COOKIE_SETTINGS).setOnPreferenceClickListener(this);
        findPreference(KEY_AUDIO_BACKGROUND_PLAY_ENABLED).setOnPreferenceClickListener(this);

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
        checkUIPreferences();
        update2FAVisibility();
        setAutoaccept = false;
        autoAccept = true;
    }

    /**
     * Method for update the Passcode lock section.
     */
    private void updatePasscodeLock() {
        if (prefs == null || prefs.getPasscodeLockEnabled() == null) {
            passcodeLock = false;
            dbH.setPasscodeLockEnabled(false);
        } else {
            passcodeLock = Boolean.parseBoolean(prefs.getPasscodeLockEnabled());
        }
        updatePasscodeLockSubtitle();
    }

    public void updatePasscodeLockSubtitle() {
        passcodeLockPreference.setSummary(passcodeLock ? R.string.mute_chat_notification_option_on : R.string.mute_chatroom_notification_option_off);
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

    /**
     * Checks and sets the User interface setting values.
     */
    private void checkUIPreferences() {
        SharedPreferences sharedPreferences = requireContext()
                .getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE);

        updateStartScreenSetting(sharedPreferences.getInt(PREFERRED_START_SCREEN, HOME_BNV));
        hideRecentActivity.setChecked(sharedPreferences.getBoolean(HIDE_RECENT_ACTIVITY, false));
    }

    /**
     * Updates the start screen setting.
     *
     * @param newStartScreen Value to set as new start screen.
     */
    private void updateStartScreenSetting(int newStartScreen) {
        String startScreenSummary =
                getResources().getStringArray(R.array.settings_start_screen)[newStartScreen];

        startScreen.setSummary(startScreenSummary);
    }

    /**
     * Updates the hide recent activity setting.
     *
     * @param hide True if should enable the setting, false otherwise.
     */
    private void updateHideRecentActivitySetting(boolean hide) {
        if (hide != hideRecentActivity.isChecked()) {
            hideRecentActivity.setChecked(hide);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        logDebug("onViewCreated");
        setupObservers();

        // Init QR code setting
        megaApi.getContactLinksOption((ManagerActivity) context);

        if (((ManagerActivity) context).openSettingsStorage) {
            goToCategoryStorage();
        } else if (((ManagerActivity) context).openSettingsQR) {
            goToCategoryQR();
        } else if (((ManagerActivity) context).openSettingsStartScreen) {
            goToSectionStartScreen();
        }

        getListView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        if (savedInstanceState != null) {
            bEvaluateAppDialogShow = savedInstanceState.getBoolean(EVALUATE_APP_DIALOG_SHOW);
        }
        if (bEvaluateAppDialogShow) {
            showEvaluatedAppDialog();
        }
    }

    private void setupObservers() {
        LiveEventBus.get(EVENT_UPDATE_START_SCREEN, Integer.class)
                .observe(getViewLifecycleOwner(), this::updateStartScreenSetting);

        LiveEventBus.get(EVENT_UPDATE_HIDE_RECENT_ACTIVITY, Boolean.class)
                .observe(getViewLifecycleOwner(), this::updateHideRecentActivitySetting);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (evaluateAppDialog != null && evaluateAppDialog.isShowing()) {
            outState.putBoolean(EVALUATE_APP_DIALOG_SHOW, bEvaluateAppDialogShow);
        }
    }

    /**
     * Method for controlling whether or not to display the action bar elevation.
     */
    public void checkScroll() {
        if (getListView() == null) {
            return;
        }

        ((ManagerActivity) context)
                .changeAppBarElevation(getListView().canScrollVertically(SCROLLING_UP_DIRECTION));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        setOnlineOptions(isOnline(context) && megaApi != null && megaApi.getRootNode() != null);
        Intent playerServiceIntent = new Intent(requireContext(), AudioPlayerService.class);
        requireContext().bindService(playerServiceIntent, mediaServiceConnection, 0);
        return v;
    }

    @Override
    public void onDestroyView() {
        requireContext().unbindService(mediaServiceConnection);
        super.onDestroyView();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case KEY_APPEARNCE_COLOR_THEME:
                ThemeHelper.INSTANCE.applyTheme((String) newValue);
                break;
        }
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
                startActivity(new Intent(context, ExportRecoveryKeyActivity.class));
                break;

            case KEY_PASSCODE_LOCK:
                startActivity(new Intent(context, PasscodePreferencesActivity.class));
                break;

            case KEY_CHANGE_PASSWORD:
                startActivity(new Intent(context, ChangePasswordActivity.class));
                break;

            case KEY_2FA:
                if (((ManagerActivity) context).is2FAEnabled()) {
                    twoFASwitch.setChecked(true);
                    Intent intent = new Intent(context, VerifyTwoFactorActivity.class);
                    intent.putExtra(VerifyTwoFactorActivity.KEY_VERIFY_TYPE, DISABLE_2FA);

                    context.startActivity(intent);
                } else {
                    twoFASwitch.setChecked(false);
                    Intent intent = new Intent(context, TwoFactorAuthenticationActivity.class);
                    startActivity(intent);
                }
                break;

            case KEY_QR_CODE_AUTO_ACCEPT:
                //			First query if QR auto-accept is enabled or not, then change the value
                setAutoaccept = true;
                megaApi.getContactLinksOption((ManagerActivity) context);
                break;

            case KEY_SECURITY_ADVANCED:
                startActivity(new Intent(context, AdvancedPreferencesActivity.class));
                break;

            case KEY_HELP_CENTRE:
                viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setData(Uri.parse("https://mega.nz/help/client/android"));
                startActivity(viewIntent);
                break;

            case KEY_HELP_SEND_FEEDBACK:
                showEvaluatedAppDialog();
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

            case KEY_ABOUT_CODE_LINK:
                viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setData(Uri.parse("https://github.com/meganz/android"));
                startActivity(viewIntent);
                break;

            case KEY_ABOUT_APP_VERSION:
                logDebug("KEY_ABOUT_APP_VERSION pressed");
                numberOfClicksAppVersion++;
                if (numberOfClicksAppVersion == CLICKS_ENABLE_DEBUG) {
                    if (!MegaApplication.isShowInfoChatMessages()) {
                        MegaApplication.setShowInfoChatMessages(true);
                        numberOfClicksAppVersion = 0;
                        ((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.show_info_chat_msg_enabled), MEGACHAT_INVALID_HANDLE);
                    } else {
                        MegaApplication.setShowInfoChatMessages(false);
                        numberOfClicksAppVersion = 0;
                        ((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.show_info_chat_msg_disabled), MEGACHAT_INVALID_HANDLE);
                    }
                }
                break;

            case KEY_ABOUT_SDK_VERSION:
                numberOfClicksSDK++;
                if (numberOfClicksSDK == CLICKS_ENABLE_DEBUG) {
                    if (areSDKLogsEnabled()) {
                        numberOfClicksSDK = 0;
                        setStatusLoggerSDK(context, false);
                    } else {
                        ((ManagerActivity) context).showConfirmationEnableLogsSDK();
                    }
                }
                break;

            case KEY_ABOUT_KARERE_VERSION:
                numberOfClicksKarere++;
                if (numberOfClicksKarere == CLICKS_ENABLE_DEBUG) {
                    if (areKarereLogsEnabled()) {
                        numberOfClicksKarere = 0;
                        setStatusLoggerKarere(context, false);
                    } else {
                        ((ManagerActivity) context).showConfirmationEnableLogsKarere();
                    }
                }
                break;

            case KEY_CANCEL_ACCOUNT:
                ((ManagerActivity) context).askConfirmationDeleteAccount();
                break;

            case KEY_ABOUT_COOKIE_POLICY:
                Intent intent = new Intent(context, WebViewActivity.class);
                intent.setData(Uri.parse("https://mega.nz/cookie"));
                startActivity(intent);
                break;

            case KEY_COOKIE_SETTINGS:
                startActivity(new Intent(context, CookiePreferencesActivity.class));
                break;

            case KEY_AUDIO_BACKGROUND_PLAY_ENABLED:
                if (playerService != null) {
                    playerService.getViewModel().toggleBackgroundPlay();
                }
                break;
            case KEY_START_SCREEN:
                startActivity(new Intent(context, StartScreenPreferencesActivity.class));
                break;
            case KEY_HIDE_RECENT_ACTIVITY:
                requireContext().getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
                        .edit().putBoolean(HIDE_RECENT_ACTIVITY, hideRecentActivity.isChecked()).apply();

                LiveEventBus.get(EVENT_UPDATE_HIDE_RECENT_ACTIVITY, Boolean.class)
                        .post(hideRecentActivity.isChecked());
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

    /**
     * Scroll to the beginning of Settings page.
     * In this case, the beginning is category KEY_FEATURES.
     * <p>
     * Note: If the first category changes, this method should be updated with the new one.
     */
    public void goToFirstCategory() {
        PreferenceCategory firstCategory = findPreference(KEY_FEATURES);

        if (firstCategory != null) {
            scrollToPreference(firstCategory);
        }
    }

    public void goToCategoryStorage() {
        scrollToPreference(fileManagementPrefence);
        onPreferenceClick(fileManagementPrefence);
    }

    public void goToCategoryQR() {
        scrollToPreference(qrCodeAutoAcceptSwitch);
    }

    public void goToSectionStartScreen() {
        scrollToPreference(startScreen);
        startActivity(new Intent(context, StartScreenPreferencesActivity.class));
        ((ManagerActivity) context).openSettingsStartScreen = false;
    }

    private void refreshAccountInfo() {
        //Check if the call is recently
        logDebug("Check the last call to getAccountDetails");
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
                twoFASwitch.setEnabled(false);
                twoFASwitch.setVisible(true);
                megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), (ManagerActivity) context);
            } else {
                twoFASwitch.setVisible(false);
            }
        }
    }

    private void showEvaluatedAppDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.evaluate_the_app_dialog, null);

        final CheckedTextView rateAppCheck = (CheckedTextView) dialogLayout.findViewById(R.id.rate_the_app);
        rateAppCheck.setText(getString(R.string.rate_the_app_panel));
        rateAppCheck.setCompoundDrawablePadding(scaleWidthPx(10, outMetrics));
        ViewGroup.MarginLayoutParams rateAppMLP = (ViewGroup.MarginLayoutParams) rateAppCheck.getLayoutParams();
        rateAppMLP.setMargins(scaleWidthPx(15, outMetrics), scaleHeightPx(10, outMetrics), 0, scaleHeightPx(10, outMetrics));

        final CheckedTextView sendFeedbackCheck = (CheckedTextView) dialogLayout.findViewById(R.id.send_feedback);
        sendFeedbackCheck.setText(getString(R.string.send_feedback_panel));
        sendFeedbackCheck.setCompoundDrawablePadding(scaleWidthPx(10, outMetrics));
        ViewGroup.MarginLayoutParams sendFeedbackMLP = (ViewGroup.MarginLayoutParams) sendFeedbackCheck.getLayoutParams();
        sendFeedbackMLP.setMargins(scaleWidthPx(15, outMetrics), scaleHeightPx(10, outMetrics), 0, scaleHeightPx(10, outMetrics));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this.context);
        builder.setView(dialogLayout);

        builder.setTitle(getString(R.string.title_evaluate_the_app_panel));
        evaluateAppDialog = builder.create();

        evaluateAppDialog.show();
        bEvaluateAppDialogShow = true;
        rateAppCheck.setOnClickListener(v -> {
            logDebug("Rate the app");
            //Rate the app option:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RATE_APP_URL)));

            if (evaluateAppDialog != null) {
                evaluateAppDialog.dismiss();
                bEvaluateAppDialogShow = false;
            }
        });

        sendFeedbackCheck.setOnClickListener(v -> {
            logDebug("Send Feedback");

            //Send feedback option:
            StringBuilder body = new StringBuilder();
            body.append(getString(R.string.setting_feedback_body))
                    .append("\n\n\n\n\n\n\n\n\n\n\n")
                    .append(getString(R.string.settings_feedback_body_device_model)).append("  ").append(getDeviceName()).append("\n")
                    .append(getString(R.string.settings_feedback_body_android_version)).append("  ").append(Build.VERSION.RELEASE).append(" ").append(Build.DISPLAY).append("\n")
                    .append(getString(R.string.user_account_feedback)).append("  ").append(megaApi.getMyEmail());

            body.append(" (");
            switch (myAccountInfo.getAccountType()) {
                case FREE:
                default:
                    body.append(getString(R.string.my_account_free));
                    break;
                case PRO_I:
                    body.append(getString(R.string.my_account_pro1));
                    break;
                case PRO_II:
                    body.append(getString(R.string.my_account_pro2));
                    break;
                case PRO_III:
                    body.append(getString(R.string.my_account_pro3));
                    break;
                case PRO_LITE:
                    body.append(getString(R.string.my_account_prolite_feedback_email));
                    break;
                case BUSINESS:
                    body.append(getString(R.string.business_label));
                    break;
            }
            body.append(")");

            String versionApp = (getString(R.string.app_version));
            String subject = getString(R.string.setting_feedback_subject) + " v" + versionApp;

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType(TYPE_TEXT_PLAIN);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{MAIL_ANDROID});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body.toString());
            startActivity(Intent.createChooser(emailIntent, " "));

            if (evaluateAppDialog != null) {
                evaluateAppDialog.dismiss();
                bEvaluateAppDialogShow = false;
            }
        });

    }

    /**
     * Re-enable 'twoFASwitch' after 'multiFactorAuthCheck' finished.
     */
    public void reEnable2faSwitch() {
        twoFASwitch.setEnabled(true);
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


    // Call original functions from interface calls
    @Override
    public int getNumberOfClicksKarere() {
        return numberOfClicksKarere;
    }

    @Override
    public void setNumberOfClicksKarere(int numberOfClicksKarere) {
        this.numberOfClicksKarere = numberOfClicksKarere;
    }

    @Override
    public int getNumberOfClicksAppVersion() {
        return numberOfClicksAppVersion;
    }

    @Override
    public void setNumberOfClicksAppVersion(int numberOfClicksAppVersion) {
        this.numberOfClicksAppVersion = numberOfClicksAppVersion;
    }

    @Override
    public int getNumberOfClicksSDK() {
        return numberOfClicksSDK;
    }

    @Override
    public void setNumberOfClicksSDK(int numberOfClicksSDK) {
        this.numberOfClicksSDK = numberOfClicksSDK;
    }

    @Override
    public boolean getSetAutoAccept() {
        return getSetAutoaccept();
    }

    @Override
    public void setSetAutoAccept(boolean setAutoAccept) {
        setSetAutoaccept(setAutoAccept);
    }

    @Override
    public boolean getAutoAcceptSetting() {
        return getAutoacceptSetting();
    }

    @Override
    public void setAutoAcceptSetting(boolean autoAcceptSetting) {
        setAutoacceptSetting(autoAcceptSetting);
    }

    @Override
    public void setValueOfAutoAccept(boolean autoAccept) {
        setValueOfAutoaccept(autoAccept);
    }
}
