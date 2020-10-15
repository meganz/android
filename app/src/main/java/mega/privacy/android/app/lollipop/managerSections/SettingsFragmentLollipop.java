package mega.privacy.android.app.lollipop.managerSections;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.CameraUploadsPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.DownloadPreferencesActivity;
import mega.privacy.android.app.components.TwoLineCheckPreference;
import mega.privacy.android.app.fragments.settingsFragments.SettingsBaseFragment;
import mega.privacy.android.app.jobservices.SyncRecord;
import mega.privacy.android.app.listeners.SetAttrUserListener;
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.PinLockActivityLollipop;
import mega.privacy.android.app.lollipop.TwoFactorAuthenticationActivity;
import mega.privacy.android.app.lollipop.tasks.ClearCacheTask;
import mega.privacy.android.app.lollipop.tasks.ClearOfflineTask;
import mega.privacy.android.app.lollipop.tasks.GetCacheSizeTask;
import mega.privacy.android.app.lollipop.tasks.GetOfflineSizeTask;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.constants.SettingsConstants.*;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.BUSINESS_CU_FRAGMENT_SETTINGS;
import static mega.privacy.android.app.MegaPreferences.*;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.FragmentTag.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.PermissionUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.CameraUploadUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

@SuppressLint("NewApi")
public class SettingsFragmentLollipop extends SettingsBaseFragment {

	PreferenceScreen preferenceScreen;

	PreferenceCategory featuresCategory;
	Preference cameraUploadsPreference;
	Preference chatPreference;

	PreferenceCategory storageCategory;
	Preference downloadLocationPreference;
	Preference fileManagementPrefence;

	PreferenceCategory securityCategory;
	Preference backupRecoveryKeyPreference;
	SwitchPreferenceCompat pinLockEnableSwitch;
	Preference pinLockCode;
	Preference changePasswordPrefence;
	SwitchPreferenceCompat twoFASwitch;
	SwitchPreferenceCompat qrCodeAutoAcceptSwitch;
	Preference advancedPreference;

	PreferenceCategory helpCategory;
	Preference helpSendFeedback;

	PreferenceCategory aboutCategory;
	Preference aboutPrivacy;
	Preference aboutTOS;
	Preference aboutGDPR;
	Preference codeLink;
	Preference aboutSDK;
	Preference aboutKarere;
	Preference aboutApp;
	Preference deleteAccount;

	SwitchPreferenceCompat autoPlaySwitch;

	PreferenceCategory fileManagementCategory;


	//File management
	Preference offlineFileManagement;
	Preference rubbishFileManagement;
	Preference fileVersionsFileManagement;
	Preference clearVersionsFileManagement;
	SwitchPreferenceCompat enableVersionsSwitch;

	SwitchPreferenceCompat enableRbSchedulerSwitch;
	Preference daysRbSchedulerPreference;

//	TwoLineCheckPreference useHttpsOnly;

	Preference recoveryKey;
	Preference changePass;

	boolean pinLock = false;
	boolean fileNames = false;
	boolean autoAccept = true;

	String camSyncLocalPath = "";
	boolean isExternalSDCard = false;
	Long camSyncHandle = null;
	MegaNode camSyncMegaNode = null;
	String camSyncMegaPath = "";
	String ast = "";
	String pinLockCodeTxt = "";

	boolean useHttpsOnlyValue = false;

	//Secondary Folder
	String localSecondaryFolderPath = "";
	Long handleSecondaryMediaFolder = null;
	MegaNode megaNodeSecondaryMediaFolder = null;
	String megaPathSecMediaFolder = "";

	public int numberOfClicksSDK = 0;
	public int numberOfClicksKarere = 0;
	public int numberOfClicksAppVersion = 0;
	RecyclerView listView;

	boolean setAutoaccept = false;

    private SetAttrUserListener setAttrUserListener;
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.preferences);

		preferenceScreen = findPreference(GENERAL_SETTINGS);

		featuresCategory = findPreference(CATEGORY_FEATURES);
		cameraUploadsPreference = findPreference(KEY_FEATURES_CAMERA_UPLOAD);
		cameraUploadsPreference.setOnPreferenceClickListener(this);
		chatPreference = findPreference(KEY_FEATURES_CHAT);
		chatPreference.setOnPreferenceClickListener(this);

		storageCategory = findPreference(CATEGORY_STORAGE);
		downloadLocationPreference = findPreference(KEY_STORAGE_DOWNLOAD_LOCATION);
		downloadLocationPreference.setOnPreferenceClickListener(this);
		fileManagementPrefence = findPreference(KEY_STORAGE_FILE_MANAGEMENT);
		fileManagementPrefence.setOnPreferenceClickListener(this);

		securityCategory = findPreference(CATEGORY_SECURITY);
		backupRecoveryKeyPreference = findPreference(KEY_SECURITY_RECOVERY_KEY);
		backupRecoveryKeyPreference.setOnPreferenceClickListener(this);
		pinLockEnableSwitch = findPreference(KEY_PIN_LOCK_ENABLE);
		pinLockEnableSwitch.setOnPreferenceClickListener(this);
		pinLockCode = findPreference(KEY_PIN_LOCK_CODE);
		pinLockCode.setOnPreferenceClickListener(this);
		changePasswordPrefence = findPreference(KEY_SECURITY_CHANGE_PASSWORD);
		changePasswordPrefence.setOnPreferenceClickListener(this);
		twoFASwitch = findPreference(KEY_SECURITY_2FA);
		twoFASwitch.setOnPreferenceClickListener(this);
		qrCodeAutoAcceptSwitch = findPreference(KEY_SECURITY_QRCODE);
		qrCodeAutoAcceptSwitch.setOnPreferenceClickListener(this);
		advancedPreference = findPreference(KEY_SECURITY_ADVANCED);
		advancedPreference.setOnPreferenceClickListener(this);

		helpCategory = findPreference(CATEGORY_HELP);
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
		deleteAccount = findPreference(KEY_ABOUT_DELETE_ACCOUNT);
		deleteAccount.setOnPreferenceClickListener(this);
		updateCancelAccountSetting();

		fileManagementCategory = (PreferenceCategory) findPreference(CATEGORY_FILE_MANAGEMENT);
		fileManagementCategory.setVisible(false);

		autoPlaySwitch = (SwitchPreferenceCompat) findPreference(KEY_AUTO_PLAY_SWITCH);
        autoPlaySwitch.setOnPreferenceClickListener(this);
		autoPlaySwitch.setVisible(false);

		boolean autoPlayEnabled = prefs.isAutoPlayEnabled();
        autoPlaySwitch.setChecked(autoPlayEnabled);




//		useHttpsOnly = (TwoLineCheckPreference) findPreference("settings_use_https_only");
//		useHttpsOnly.setOnPreferenceClickListener(this);
//		useHttpsOnly.setVisible(false);


//		cacheAdvancedOptions = findPreference(KEY_CACHE);
//		cacheAdvancedOptions.setOnPreferenceClickListener(this);

		offlineFileManagement = findPreference(KEY_OFFLINE);
		offlineFileManagement.setOnPreferenceClickListener(this);
		offlineFileManagement.setVisible(false);

		rubbishFileManagement = findPreference(KEY_RUBBISH);
		rubbishFileManagement.setOnPreferenceClickListener(this);
		rubbishFileManagement.setVisible(false);

		fileVersionsFileManagement = findPreference(KEY_FILE_VERSIONS);
		fileVersionsFileManagement.setVisible(false);

		clearVersionsFileManagement = findPreference(KEY_CLEAR_VERSIONS);
		clearVersionsFileManagement.setOnPreferenceClickListener(this);
		clearVersionsFileManagement.setVisible(false);

		enableVersionsSwitch = (SwitchPreferenceCompat) findPreference(KEY_ENABLE_VERSIONS);
		enableVersionsSwitch.setVisible(false);

		updateEnabledFileVersions();
		enableRbSchedulerSwitch = (SwitchPreferenceCompat) findPreference(KEY_ENABLE_RB_SCHEDULER);
		daysRbSchedulerPreference = (Preference) findPreference(KEY_DAYS_RB_SCHEDULER);
		enableRbSchedulerSwitch.setVisible(false);
		daysRbSchedulerPreference.setVisible(false);

		if(megaApi.serverSideRubbishBinAutopurgeEnabled()){
			logDebug("RubbishBinAutopurgeEnabled --> request userAttribute info");
			megaApi.getRubbishBinAutopurgePeriod((ManagerActivityLollipop)context);
			fileManagementCategory.addPreference(enableRbSchedulerSwitch);
			fileManagementCategory.addPreference(daysRbSchedulerPreference);
			daysRbSchedulerPreference.setOnPreferenceClickListener(this);
		}
		else{
			fileManagementCategory.removePreference(enableRbSchedulerSwitch);
			fileManagementCategory.removePreference(daysRbSchedulerPreference);
		}

		if (prefs == null){
			logWarning("pref is NULL");
			dbH.setStorageAskAlways(true);

			File defaultDownloadLocation = buildDefaultDownloadDir(context);
			defaultDownloadLocation.mkdirs();

			dbH.setStorageDownloadLocation(defaultDownloadLocation.getAbsolutePath());

			dbH.setFirstTime(false);
			dbH.setPinLockEnabled(false);
			dbH.setPinLockCode("");
			fileNames = false;
			pinLock = false;
		}
		else{


			if (prefs.getPinLockEnabled() == null){
				dbH.setPinLockEnabled(false);
				dbH.setPinLockCode("");
				pinLock = false;
				pinLockEnableSwitch.setChecked(pinLock);
			}
			else{
				pinLock = Boolean.parseBoolean(prefs.getPinLockEnabled());
				pinLockEnableSwitch.setChecked(pinLock);
				pinLockCodeTxt = prefs.getPinLockCode();
				if (pinLockCodeTxt == null){
					pinLockCodeTxt = "";
					dbH.setPinLockCode(pinLockCodeTxt);
				}
			}
		}



//		cacheAdvancedOptions.setSummary(getString(R.string.settings_advanced_features_calculating));
		offlineFileManagement.setSummary(getString(R.string.settings_advanced_features_calculating));
		if(((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo()==null){
			fileVersionsFileManagement.setSummary(getString(R.string.settings_advanced_features_calculating));
			rubbishFileManagement.setSummary(getString(R.string.settings_advanced_features_calculating));
			fileManagementCategory.removePreference(clearVersionsFileManagement);
		}
		else{
			rubbishFileManagement.setSummary(getString(R.string.settings_advanced_features_size, ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo().getFormattedUsedRubbish()));
			if(((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo().getNumVersions() == -1){
				fileVersionsFileManagement.setSummary(getString(R.string.settings_advanced_features_calculating));
				fileManagementCategory.removePreference(clearVersionsFileManagement);
			}
			else{
				setVersionsInfo();
			}
		}

		taskGetSizeCache();
		taskGetSizeOffline();

		if (pinLock){
//			pinLockEnableSwitch.setTitle(getString(R.string.settings_pin_lock_off));
			ast = "";
			if (pinLockCodeTxt.compareTo("") == 0){
				ast = getString(R.string.settings_pin_lock_code_not_set);
			}
			else{
				for (int i=0;i<pinLockCodeTxt.length();i++){
					ast = ast + "*";
				}
			}
			pinLockCode.setSummary(ast);
			securityCategory.addPreference(pinLockCode);
		}
		else{
//			pinLockEnableSwitch.setTitle(getString(R.string.settings_pin_lock_on));
			securityCategory.removePreference(pinLockCode);
		}

		useHttpsOnlyValue = Boolean.parseBoolean(dbH.getUseHttpsOnly());
		logDebug("Value of useHttpsOnly: " + useHttpsOnlyValue);

//		useHttpsOnly.setChecked(useHttpsOnlyValue);

		setAutoaccept = false;
		autoAccept = true;
		if (megaApi.multiFactorAuthAvailable()) {
			twoFASwitch.setVisible(true);
//			securityCategory.addPreference(twoFASwitch);
			megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), (ManagerActivityLollipop) context);
		}
		else {
			twoFASwitch.setVisible(false);

//			securityCategory.removePreference(twoFASwitch);
		}
		megaApi.getContactLinksOption((ManagerActivityLollipop) context);
		megaApi.getFileVersionsOption((ManagerActivityLollipop)context);

        String sizeInDB = prefs.getChargingOnSize();
        String size;
        if(sizeInDB == null){
            dbH.setChargingOnSize(DEFAULT_CONVENTION_QUEUE_SIZE);
            size = String.valueOf(DEFAULT_CONVENTION_QUEUE_SIZE);
        }else{
            size = String.valueOf(Integer.parseInt(sizeInDB));
        }
        String chargingHelper = getResources().getString(R.string.settings_camera_upload_charging_helper_label,
				getResources().getString(R.string.label_file_size_mega_byte, size));

        if(savedInstanceState != null){
            boolean isShowingQueueDialog = savedInstanceState.getBoolean(KEY_SET_QUEUE_DIALOG, false);
            if(isShowingQueueDialog){
                showResetCompressionQueueSizeDialog();
                String input = savedInstanceState.getString(KEY_SET_QUEUE_SIZE, "");
                queueSizeInput.setText(input);
                queueSizeInput.setSelection(input.length());
            }
        }
	}

	public void updateCancelAccountSetting() {
		if (megaApi.isBusinessAccount() && !megaApi.isMasterBusinessAccount()) {
			aboutCategory.removePreference(deleteAccount);
		}
	}

	public void setVersionsInfo(){
		logDebug("setVersionsInfo");

		MyAccountInfo myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();

		if(myAccountInfo!=null){
			int numVersions = myAccountInfo.getNumVersions();
			logDebug("Num versions: " + numVersions);
			String previousVersions = myAccountInfo.getFormattedPreviousVersionsSize();
			String text = getString(R.string.settings_file_management_file_versions_subtitle, numVersions, previousVersions);
			logDebug("Previous versions: " + previousVersions);
			fileVersionsFileManagement.setSummary(text);
			if(numVersions>0){
				fileManagementCategory.addPreference(clearVersionsFileManagement);
			}
			else{
				fileManagementCategory.removePreference(clearVersionsFileManagement);
			}
		}
	}

	public void resetVersionsInfo(){
		logDebug("resetVersionsInfo");

		String text = getString(R.string.settings_file_management_file_versions_subtitle, 0, "0 B");
		fileVersionsFileManagement.setSummary(text);
		fileManagementCategory.removePreference(clearVersionsFileManagement);
	}

	public void setRubbishInfo(){
		logDebug("setRubbishInfo");
		rubbishFileManagement.setSummary(getString(R.string.settings_advanced_features_size, ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo().getFormattedUsedRubbish()));
	}

	public void resetRubbishInfo() {
		logInfo("Updating size after clean the Rubbish Bin");
		String emptyString = getString(R.string.label_file_size_byte, "0");
		rubbishFileManagement.setSummary(getString(R.string.settings_advanced_features_size, emptyString));
		MegaApplication.getInstance().getMyAccountInfo().setFormattedUsedRubbish(emptyString);
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

	public void checkScroll () {
		if (listView != null) {
			if (listView.canScrollVertically(-1)) {
				((ManagerActivityLollipop) context).changeActionBarElevation(true);
			}
			else {
				((ManagerActivityLollipop) context).changeActionBarElevation(false);
			}
		}
	}

	public void goToCategoryStorage() {
		logDebug("goToCategoryStorage");
		scrollToPreference(storageCategory);
	}

	public void goToCategoryQR() {
		logDebug("goToCategoryQR");
		scrollToPreference(qrCodeAutoAcceptSwitch);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);
		final ListView lv = (ListView) v.findViewById(android.R.id.list);
		if(lv != null) {
			lv.setPadding(0, 0, 0, 0);
		}

		if(isOnline(context)){
			if(megaApi==null || megaApi.getRootNode()==null){
				setOnlineOptions(false);
			}
			else{
				setOnlineOptions(true);
			}
		}
		else{
			logDebug("Offline");
			setOnlineOptions(false);
		}

		setAttrUserListener = new SetAttrUserListener(context, SETTINGS);

		return v;
	}

	public void setOnlineOptions(boolean isOnline){
		featuresCategory.setEnabled(isOnline);
		chatPreference.setEnabled(isOnline);
		cameraUploadsPreference.setEnabled(isOnline);

		rubbishFileManagement.setEnabled(isOnline);
		clearVersionsFileManagement.setEnabled(isOnline);
		securityCategory.setEnabled(isOnline);
		qrCodeAutoAcceptSwitch.setEnabled(isOnline);
		twoFASwitch.setEnabled(isOnline);

		//Rubbish bin scheduler
		daysRbSchedulerPreference.setEnabled(isOnline);
		enableRbSchedulerSwitch.setEnabled(isOnline);

		//File versioning
		fileVersionsFileManagement.setEnabled(isOnline);
		enableVersionsSwitch.setEnabled(isOnline);

		//Use of HTTP
//		useHttpsOnly.setEnabled(isOnline);

		//Cancel account
		deleteAccount.setEnabled(isOnline);

		if (isOnline) {
			clearVersionsFileManagement.setLayoutResource(R.layout.delete_versions_preferences);
			deleteAccount.setLayoutResource(R.layout.cancel_account_preferences);
		}
		else {
			clearVersionsFileManagement.setLayoutResource(R.layout.delete_versions_preferences_disabled);
			deleteAccount.setLayoutResource(R.layout.cancel_account_preferences_disabled);
		}
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
    }

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		this.context = context;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		logDebug("onPreferenceChange");
		prefs = dbH.getPreferences();
		if (preference.getKey().compareTo(KEY_PIN_LOCK_CODE) == 0){
			pinLockCodeTxt = (String) newValue;
			dbH.setPinLockCode(pinLockCodeTxt);

			ast = "";
			if (pinLockCodeTxt.compareTo("") == 0){
				ast = getString(R.string.settings_pin_lock_code_not_set);
			}
			else{
				for (int i=0;i<pinLockCodeTxt.length();i++){
					ast = ast + "*";
				}
			}
			pinLockCode.setSummary(ast);

			pinLockCode.setSummary(ast);
			logDebug("Object: " + newValue);
		}

		return true;
	}

	public void setCacheSize(String size){
		if(isAdded()){
//			cacheAdvancedOptions.setSummary(getString(R.string.settings_advanced_features_size, size));
		}
	}

	public void setOfflineSize(String size){
		if(isAdded()){
			offlineFileManagement.setSummary(getString(R.string.settings_advanced_features_size, size));
		}
	}

    @Override
	public boolean onPreferenceClick(Preference preference) {
		prefs = dbH.getPreferences();
		logDebug("KEY pressed: " + preference.getKey());

		if (preference.getKey().compareTo(KEY_ABOUT_SDK_VERSION) == 0) {
			numberOfClicksSDK++;
			if (numberOfClicksSDK == 5) {
				MegaAttributes attrs = dbH.getAttributes();
				if (attrs != null && attrs.getFileLoggerSDK() != null) {
					if (Boolean.parseBoolean(attrs.getFileLoggerSDK())) {
						numberOfClicksSDK = 0;
						setStatusLoggerSDK(context, false);
					} else {
						((ManagerActivityLollipop) context).showConfirmationEnableLogsSDK();
					}
				} else {
					logWarning("SDK file logger attribute is NULL");
					((ManagerActivityLollipop) context).showConfirmationEnableLogsSDK();
				}
			}
		} else {
			numberOfClicksSDK = 0;
		}

		if (preference.getKey().compareTo(KEY_ABOUT_KARERE_VERSION) == 0) {
			numberOfClicksKarere++;
			if (numberOfClicksKarere == 5) {
				MegaAttributes attrs = dbH.getAttributes();
				if (attrs != null && attrs.getFileLoggerKarere() != null) {
					if (Boolean.parseBoolean(attrs.getFileLoggerKarere())) {
						numberOfClicksKarere = 0;
						setStatusLoggerKarere(context, false);
					} else {
						((ManagerActivityLollipop) context).showConfirmationEnableLogsKarere();
					}
				} else {
					logWarning("Karere file logger attribute is NULL");
					((ManagerActivityLollipop) context).showConfirmationEnableLogsKarere();
				}
			}
		} else {
			numberOfClicksKarere = 0;
		}

		if (preference.getKey().compareTo(KEY_ABOUT_APP_VERSION) == 0) {
			logDebug("KEY_ABOUT_APP_VERSION pressed");
			numberOfClicksAppVersion++;
			if (numberOfClicksAppVersion == 5) {

				if (!MegaApplication.isShowInfoChatMessages()) {
					MegaApplication.setShowInfoChatMessages(true);
					numberOfClicksAppVersion = 0;
					((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, "Action to show info of chat messages is enabled", -1);
				} else {
					MegaApplication.setShowInfoChatMessages(false);
					numberOfClicksAppVersion = 0;
					((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, "Action to show info of chat messages is disabled", -1);
				}
			}
		} else {
			numberOfClicksAppVersion = 0;
		}

		switch (preference.getKey()) {
			case KEY_FEATURES_CAMERA_UPLOAD:
				startActivity(new Intent(context, CameraUploadsPreferencesActivity.class));
				break;

			case KEY_FEATURES_CHAT:
				startActivity(new Intent(context, ChatPreferencesActivity.class));
				break;

			case KEY_STORAGE_DOWNLOAD_LOCATION:
				startActivity(new Intent(context, DownloadPreferencesActivity.class));
				break;

			case KEY_PIN_LOCK_ENABLE:
				logDebug("KEY_PIN_LOCK_ENABLE");
				pinLock = !pinLock;
				if (pinLock){
					//Intent to set the PIN
					logDebug("Call to showPanelSetPinLock");
					((ManagerActivityLollipop)getActivity()).showPanelSetPinLock();
				}
				else{
					dbH.setPinLockEnabled(false);
					dbH.setPinLockCode("");
//				pinLockEnableSwitch.setTitle(getString(R.string.settings_pin_lock_on));
					securityCategory.removePreference(pinLockCode);
				}
				break;

			default:
				break;

		}

		if (preference.getKey().compareTo(KEY_CACHE) == 0){
			logDebug("Clear Cache!");

			ClearCacheTask clearCacheTask = new ClearCacheTask(context);
			clearCacheTask.execute();
		}
		else if (preference.getKey().compareTo(KEY_OFFLINE) == 0){
			logDebug("Clear Offline!");

			ClearOfflineTask clearOfflineTask = new ClearOfflineTask(context);
			clearOfflineTask.execute();
		}
		else if(preference.getKey().compareTo(KEY_RUBBISH) == 0){
			((ManagerActivityLollipop)context).showClearRubbishBinDialog();
		}
		else if(preference.getKey().compareTo(KEY_CLEAR_VERSIONS) == 0){
			((ManagerActivityLollipop)context).showConfirmationClearAllVersions();
		}
		else if (preference.getKey().compareTo(KEY_ENABLE_VERSIONS) == 0){
			logDebug("Change KEY_ENABLE_VERSIONS");

			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}

			if(!enableVersionsSwitch.isChecked()){
				megaApi.setFileVersionsOption(true, (ManagerActivityLollipop)context);
			}
			else{
				megaApi.setFileVersionsOption(false, (ManagerActivityLollipop)context);
			}
		}
		else if (preference.getKey().compareTo(KEY_ENABLE_RB_SCHEDULER) == 0){
			logDebug("Change KEY_ENABLE_RB_SCHEDULER");

			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}

			if(!enableRbSchedulerSwitch.isChecked()){
				logDebug("Disable RB schedule");
				//Check the account type
				MyAccountInfo myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();
				if(myAccountInfo!=null ){
					if(myAccountInfo.getAccountType()== MegaAccountDetails.ACCOUNT_TYPE_FREE){
						((ManagerActivityLollipop)context).showRBNotDisabledDialog();
						enableRbSchedulerSwitch.setOnPreferenceClickListener(null);
						enableRbSchedulerSwitch.setChecked(true);
						enableRbSchedulerSwitch.setOnPreferenceClickListener(this);
					}
					else{
						((ManagerActivityLollipop)context).setRBSchedulerValue("0");
					}
				}
			}
			else{
				logDebug("ENABLE RB schedule");
				((ManagerActivityLollipop)context).showRbSchedulerValueDialog(true);
			}
		}
		else if (preference.getKey().compareTo(KEY_DAYS_RB_SCHEDULER) == 0){
			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}

			((ManagerActivityLollipop)context).showRbSchedulerValueDialog(false);
		} else if (preference.getKey().compareTo(KEY_PIN_LOCK_CODE) == 0){
			//Intent to reset the PIN
			logDebug("KEY_PIN_LOCK_CODE");
			resetPinLock();
		}
//		else if (preference.getKey().compareTo("settings_use_https_only") == 0){
//			logDebug("settings_use_https_only");
//			useHttpsOnlyValue = useHttpsOnly.isChecked();
//			dbH.setUseHttpsOnly(useHttpsOnlyValue);
//			megaApi.useHttpsOnly(useHttpsOnlyValue);
//		}


		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_CAMERA_FOLDER_SDCARD) == 0){
			logDebug("KEY_CAMERA_UPLOAD_CAMERA_FOLDER_SDCARD");
			Dialog localCameraDialog;
			String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
	        AlertDialog.Builder b=new AlertDialog.Builder(context);

			b.setTitle(getResources().getString(R.string.settings_local_camera_upload_folder));
			b.setItems(sdCardOptions, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(which){
						case 0:{
							Intent intent = new Intent(context, FileStorageActivityLollipop.class);
							intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
							intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
							intent.putExtra(FileStorageActivityLollipop.EXTRA_CAMERA_FOLDER, true);
							intent.putExtra(FileStorageActivityLollipop.IS_CU_OR_MU_FOLDER,true);
							startActivityForResult(intent, REQUEST_CAMERA_FOLDER);
							break;
						}
						case 1:{
							File[] fs = context.getExternalFilesDirs(null);
							if (fs.length > 1){
								if (fs[1] != null){
									Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
									startActivityForResult(intent, REQUEST_CODE_TREE_LOCAL_CAMERA);
								}
								else{
									Intent intent = new Intent(context, FileStorageActivityLollipop.class);
									intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
									intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
									intent.putExtra(FileStorageActivityLollipop.EXTRA_CAMERA_FOLDER, true);
									intent.putExtra(FileStorageActivityLollipop.IS_CU_OR_MU_FOLDER,true);
									startActivityForResult(intent, REQUEST_CAMERA_FOLDER);
								}
							}
							break;
						}
					}
				}
			});
			b.setNegativeButton(getResources().getString(R.string.general_cancel), new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			localCameraDialog = b.create();
			localCameraDialog.show();

		}else if (preference.getKey().compareTo(KEY_HELP_SEND_FEEDBACK) == 0){
			((ManagerActivityLollipop) context).showEvaluatedAppDialog();
		}
		else if (preference.getKey().compareTo(KEY_ABOUT_PRIVACY_POLICY) == 0){
			Intent viewIntent = new Intent(Intent.ACTION_VIEW);
			viewIntent.setData(Uri.parse("https://mega.nz/privacy"));
			startActivity(viewIntent);
		}
		else if (preference.getKey().compareTo(KEY_ABOUT_TOS) == 0){
			Intent viewIntent = new Intent(Intent.ACTION_VIEW);
			viewIntent.setData(Uri.parse("https://mega.nz/terms"));
			startActivity(viewIntent);
		}
		else if (preference.getKey().compareTo(KEY_ABOUT_GDPR) == 0){
			Intent viewIntent = new Intent(Intent.ACTION_VIEW);
			viewIntent.setData(Uri.parse("https://mega.nz/gdpr"));
			startActivity(viewIntent);
		}
		else if(preference.getKey().compareTo(KEY_ABOUT_CODE_LINK) == 0){
			Intent viewIntent = new Intent(Intent.ACTION_VIEW);
			viewIntent.setData(Uri.parse("https://github.com/meganz/android"));
			startActivity(viewIntent);
		}
		else if (preference.getKey().compareTo(KEY_ABOUT_DELETE_ACCOUNT) == 0){
			logDebug("Cancel account preference");
			((ManagerActivityLollipop)context).askConfirmationDeleteAccount();
		}
		else if (preference.getKey().compareTo(KEY_SECURITY_QRCODE) == 0){
//			First query if QR auto-accept is enabled or not, then change the value
			setAutoaccept = true;
			megaApi.getContactLinksOption((ManagerActivityLollipop) context);
		}
		else if (preference.getKey().compareTo(KEY_SECURITY_RECOVERY_KEY) == 0){
			logDebug("Export Recovery Key");
			((ManagerActivityLollipop)context).showMKLayout();
		}
		else if (preference.getKey().compareTo(KEY_SECURITY_CHANGE_PASSWORD) == 0){
			logDebug("Change password");
			Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
			startActivity(intent);
		}
		else if (preference.getKey().compareTo(KEY_SECURITY_2FA) == 0){
			if (((ManagerActivityLollipop) context).is2FAEnabled()){
				logDebug("2FA is Checked");
				twoFASwitch.setChecked(true);
				((ManagerActivityLollipop) context).showVerifyPin2FA(DISABLE_2FA);
			}
			else {
				logDebug("2FA is NOT Checked");
				twoFASwitch.setChecked(false);
				Intent intent = new Intent(context, TwoFactorAuthenticationActivity.class);
				startActivity(intent);
			}
		}else if(preference.getKey().compareTo(KEY_AUTO_PLAY_SWITCH) == 0 ){
            boolean isChecked = autoPlaySwitch.isChecked();
			logDebug("Is auto play checked " + isChecked);
            dbH.setAutoPlayEnabled(String.valueOf(isChecked));
        }

		return true;
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		logDebug("onActivityResult");

		prefs = dbH.getPreferences();
		logDebug("REQUEST CODE: " + requestCode + "___RESULT CODE: " + resultCode);
		if(requestCode == SET_PIN){
			if(resultCode == Activity.RESULT_OK) {
				logDebug("Set PIN Ok");

				afterSetPinLock();
			}
			else{
				logWarning("Set PIN ERROR");
			}
		}
		else if (requestCode == REQUEST_CAMERA_FOLDER && resultCode == Activity.RESULT_OK && intent != null){
			//Local folder to sync
			String cameraPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if(!isNewSettingValid(cameraPath, prefs.getLocalPathSecondaryFolder(), prefs.getCamSyncHandle(), prefs.getMegaHandleSecondaryFolder())){
                Toast.makeText(context, getString(R.string.error_invalid_folder_selected), Toast.LENGTH_LONG).show();
                return;
            }

			prefs.setCamSyncLocalPath(cameraPath);
			camSyncLocalPath = cameraPath;
			dbH.setCamSyncLocalPath(cameraPath);
			dbH.setCameraFolderExternalSDCard(false);
			isExternalSDCard = false;
			localCameraUploadFolder.setSummary(cameraPath);
			localCameraUploadFolderSDCard.setSummary(cameraPath);
            resetCUTimestampsAndCache();
            rescheduleCameraUpload(context);
		}
		else if (requestCode == REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER && resultCode == Activity.RESULT_OK && intent != null){
			//Local folder to sync
			String secondaryPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if(!isNewSettingValid(prefs.getCamSyncLocalPath(), secondaryPath, prefs.getCamSyncHandle(), prefs.getMegaHandleSecondaryFolder())){
                Toast.makeText(context, getString(R.string.error_invalid_folder_selected), Toast.LENGTH_LONG).show();
                return;
            }

			dbH.setSecondaryFolderPath(secondaryPath);
			dbH.setSecSyncTimeStamp(0);
			dbH.setSecVideoSyncTimeStamp(0);
			prefs.setLocalPathSecondaryFolder(secondaryPath);
			rescheduleCameraUpload(context);
		}
		else if (requestCode == REQUEST_MEGA_SECONDARY_MEDIA_FOLDER && resultCode == Activity.RESULT_OK && intent != null){
			//Secondary folder to sync
			long handle = intent.getLongExtra(SELECTED_MEGA_FOLDER, INVALID_HANDLE);
			if (!isNewSettingValid(prefs.getCamSyncLocalPath(), prefs.getLocalPathSecondaryFolder(), prefs.getCamSyncHandle(), String.valueOf(handle))) {
				Toast.makeText(context, getString(R.string.error_invalid_folder_selected), Toast.LENGTH_LONG).show();
				return;
			}

			if (handle != INVALID_HANDLE) {
				megaApi.setCameraUploadsFolders(INVALID_HANDLE,handle, setAttrUserListener);
			} else {
				logError("Error choosing the Mega folder to sync the Camera");
			}

		}
		else if (requestCode == REQUEST_MEGA_CAMERA_FOLDER && resultCode == Activity.RESULT_OK && intent != null){
			//primary folder to sync
			long handle = intent.getLongExtra(SELECTED_MEGA_FOLDER,INVALID_HANDLE);
            if(!isNewSettingValid(prefs.getCamSyncLocalPath(), prefs.getLocalPathSecondaryFolder(), String.valueOf(handle), prefs.getMegaHandleSecondaryFolder())){
                Toast.makeText(context, getString(R.string.error_invalid_folder_selected), Toast.LENGTH_LONG).show();
                return;
            }

			if (handle != INVALID_HANDLE) {
			    //set primary only
				megaApi.setCameraUploadsFolders(handle, INVALID_HANDLE,setAttrUserListener);
			} else {
				logError("Error choosing the Mega folder to sync the Camera");
			}
		}
	}

	public synchronized void setCUDestinationFolder(boolean isSecondary, long handle) {
		MegaNode targetNode = megaApi.getNodeByHandle(handle);
		if (targetNode == null) return;
		if (isSecondary) {
			//reset secondary timeline.
            handleSecondaryMediaFolder = handle;
			megaNodeSecondaryMediaFolder = targetNode;
			megaPathSecMediaFolder = megaNodeSecondaryMediaFolder.getName();
			megaSecondaryFolder.setSummary(megaPathSecMediaFolder);
		} else {
            //reset primary timeline.
			camSyncHandle = handle;
			camSyncMegaNode = targetNode;
			camSyncMegaPath = camSyncMegaNode.getName();
		}
	}

	@Override
	public void onResume() {
		logDebug("onResume");

		refreshAccountInfo();

	    prefs=dbH.getPreferences();

	    if (prefs.getPinLockEnabled() == null){
			dbH.setPinLockEnabled(false);
			dbH.setPinLockCode("");
			pinLock = false;
			pinLockEnableSwitch.setChecked(pinLock);
		}
		else{
			pinLock = Boolean.parseBoolean(prefs.getPinLockEnabled());
			pinLockEnableSwitch.setChecked(pinLock);
			pinLockCodeTxt = prefs.getPinLockCode();
			if (pinLockCodeTxt == null){
				pinLockCodeTxt = "";
				dbH.setPinLockCode(pinLockCodeTxt);
			}
		}

		taskGetSizeCache();
		taskGetSizeOffline();

		if(!isOnline(context)){
			featuresCategory.setEnabled(false);
		}
		super.onResume();
	}

	@Override
	public void onPause(){
		super.onPause();
	}

	private void refreshAccountInfo(){
		logDebug("refreshAccountInfo");

		//Check if the call is recently
		logDebug("Check the last call to getAccountDetails");
		MyAccountInfo myAccountInfo = MegaApplication.getInstance().getMyAccountInfo();
		if(callToAccountDetails() || myAccountInfo.getUsedFormatted().trim().length() <= 0) {
			logDebug("megaApi.getAccountDetails SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForAccountDetails();
		}
	}

	public void update2FAPreference(boolean enabled) {
		logDebug("update2FAPreference - Enabled: " + enabled);
		twoFASwitch.setChecked(enabled);
	}

	public void update2FAVisibility(){
		logDebug("update2FAVisbility");
		if (megaApi == null){
			if (context != null){
				if (((Activity)context).getApplication() != null){
					megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
				}
			}
		}

		if (megaApi != null) {
			if (megaApi.multiFactorAuthAvailable()) {
				logDebug("update2FAVisbility true");
				twoFASwitch.setVisible(true);

//				securityCategory.addPreference(twoFASwitch);
				megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), (ManagerActivityLollipop) context);
			} else {
				logDebug("update2FAVisbility false");
				twoFASwitch.setVisible(false);

//				securityCategory.removePreference(twoFASwitch);
			}
		}
	}

	public void afterSetPinLock(){
		logDebug("afterSetPinLock");

		prefs=dbH.getPreferences();
		pinLockCodeTxt = prefs.getPinLockCode();
		if (pinLockCodeTxt == null){
			pinLockCodeTxt = "";
			dbH.setPinLockCode(pinLockCodeTxt);

		}
//		pinLockEnableSwitch.setTitle(getString(R.string.settings_pin_lock_off));
		ast = "";
		if (pinLockCodeTxt.compareTo("") == 0){
			ast = getString(R.string.settings_pin_lock_code_not_set);
		}
		else{
			for (int i=0;i<pinLockCodeTxt.length();i++){
				ast = ast + "*";
			}
		}
		pinLockCode.setSummary(ast);
		securityCategory.addPreference(pinLockCode);
		dbH.setPinLockEnabled(true);
	}

	public void taskGetSizeCache (){
		logDebug("taskGetSizeCache");
		GetCacheSizeTask getCacheSizeTask = new GetCacheSizeTask(context);
		getCacheSizeTask.execute();
	}

	public void taskGetSizeOffline (){
		logDebug("taskGetSizeOffline");
		GetOfflineSizeTask getOfflineSizeTask = new GetOfflineSizeTask(context);
		getOfflineSizeTask.execute();
	}

	public void intentToPinLock(){
		logDebug("intentToPinLock");
		Intent intent = new Intent(context, PinLockActivityLollipop.class);
		intent.setAction(PinLockActivityLollipop.ACTION_SET_PIN_LOCK);
		startActivityForResult(intent, SET_PIN);
	}

	public void resetPinLock(){
		logDebug("resetPinLock");
		Intent intent = new Intent(context, PinLockActivityLollipop.class);
		intent.setAction(PinLockActivityLollipop.ACTION_RESET_PIN_LOCK);
		startActivity(intent);
	}

	public void updateEnabledFileVersions(){
		logDebug("updateEnabledFileVersions: " + MegaApplication.isDisableFileVersions());

		enableVersionsSwitch.setOnPreferenceClickListener(null);
		if(MegaApplication.isDisableFileVersions() == 1){
			//disable = true - off versions
			if(enableVersionsSwitch.isChecked()){
				enableVersionsSwitch.setChecked(false);
			}
		}
		else if(MegaApplication.isDisableFileVersions() == 0){
			//disable = false - on versions
			if(!enableVersionsSwitch.isChecked()){
				enableVersionsSwitch.setChecked(true);
			}
		}
		else{
			enableVersionsSwitch.setChecked(false);
		}
		enableVersionsSwitch.setOnPreferenceClickListener(this);
	}

	public void updateRBScheduler(long daysCount){
		logDebug("updateRBScheduler: " + daysCount);

		if(daysCount<1){
			enableRbSchedulerSwitch.setOnPreferenceClickListener(null);
			enableRbSchedulerSwitch.setChecked(false);
			enableRbSchedulerSwitch.setSummary(null);
			enableRbSchedulerSwitch.setOnPreferenceClickListener(this);


			//Hide preference to show days
			fileManagementCategory.removePreference(daysRbSchedulerPreference);
			daysRbSchedulerPreference.setOnPreferenceClickListener(null);
		}
		else{
			MyAccountInfo myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();

			enableRbSchedulerSwitch.setOnPreferenceClickListener(null);
			enableRbSchedulerSwitch.setChecked(true);
			if(myAccountInfo!=null ){

				String subtitle = getString(R.string.settings_rb_scheduler_enable_subtitle);

				if(myAccountInfo.getAccountType()== MegaAccountDetails.ACCOUNT_TYPE_FREE){
					enableRbSchedulerSwitch.setSummary(subtitle+ " "+getString(R.string.settings_rb_scheduler_enable_period_FREE));
				}
				else{
					enableRbSchedulerSwitch.setSummary(subtitle+ " "+getString(R.string.settings_rb_scheduler_enable_period_PRO));
				}
			}

			enableRbSchedulerSwitch.setOnPreferenceClickListener(this);

			//Show and set preference to show days
			fileManagementCategory.addPreference(daysRbSchedulerPreference);
			daysRbSchedulerPreference.setOnPreferenceClickListener(this);
			daysRbSchedulerPreference.setSummary(getString(R.string.settings_rb_scheduler_select_days_subtitle, daysCount));
		}
	}


	public void cancelSetPinLock(){
		logDebug("cancelSetPinkLock");
		pinLock = false;
		pinLockEnableSwitch.setChecked(pinLock);

		dbH.setPinLockEnabled(false);
		dbH.setPinLockCode("");
	}

	public void hidePreferencesChat(){
		getPreferenceScreen().removePreference(chatPreference);
	}

	public void setValueOfAutoaccept (boolean autoAccept) {
		qrCodeAutoAcceptSwitch.setChecked(autoAccept);
	}

	public void setSetAutoaccept (boolean autoAccept) {
		this.setAutoaccept = autoAccept;
	}


	public boolean getSetAutoaccept () {
		return setAutoaccept;
	}

	public void setAutoacceptSetting (boolean autoAccept) {
		this.autoAccept  = autoAccept;
	}

	public boolean getAutoacceptSetting () {
		return autoAccept;
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
