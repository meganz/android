package mega.privacy.android.app.lollipop.managerSections;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
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
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.DownloadPreferencesActivity;
import mega.privacy.android.app.components.TwoLineCheckPreference;
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder;
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
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.tasks.ClearCacheTask;
import mega.privacy.android.app.lollipop.tasks.ClearOfflineTask;
import mega.privacy.android.app.lollipop.tasks.GetCacheSizeTask;
import mega.privacy.android.app.lollipop.tasks.GetOfflineSizeTask;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatPresenceConfig;
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
public class SettingsFragmentLollipop extends SettingsBaseFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    Handler handler = new Handler();

	PreferenceCategory qrCodeCategory;
	SwitchPreferenceCompat qrCodeAutoAcceptSwitch;

	PreferenceCategory twoFACategory;
	SwitchPreferenceCompat twoFASwitch;
    SwitchPreferenceCompat autoPlaySwitch;

	PreferenceScreen preferenceScreen;
	PreferenceCategory pinLockCategory;
	PreferenceCategory chatEnabledCategory;
	PreferenceCategory chatNotificationsCategory;
	PreferenceCategory storageCategory;
	PreferenceCategory cameraUploadCategory;
	PreferenceCategory advancedFeaturesCategory;
	PreferenceCategory autoawayChatCategory;
	PreferenceCategory persistenceChatCategory;
	PreferenceCategory securityCategory;
	PreferenceCategory fileManagementCategory;

	SwitchPreferenceCompat pinLockEnableSwitch;
	SwitchPreferenceCompat richLinksSwitch;

	SwitchPreferenceCompat enableLastGreenChatSwitch;

	//New autoaway
	SwitchPreferenceCompat autoAwaySwitch;
	Preference chatAutoAwayPreference;
	TwoLineCheckPreference chatPersistenceCheck;

	private Preference nestedDownloadLocation;

	Preference nestedNotificationsChat;
	Preference pinLockCode;
	Preference cameraUploadOn;
	ListPreference cameraUploadHow;
	ListPreference cameraUploadWhat;
	ListPreference videoQuality;
    SwitchPreferenceCompat cameraUploadCharging;
    SwitchPreferenceCompat cameraUploadIncludeGPS;
	Preference cameraUploadVideoQueueSize;
	TwoLineCheckPreference keepFileNames;
	Preference localCameraUploadFolder;
	Preference localCameraUploadFolderSDCard;
	Preference megaCameraFolder;
	Preference helpSendFeedback;
	Preference cacheAdvancedOptions;
	Preference cancelAccount;

	Preference aboutPrivacy;
	Preference aboutTOS;
	Preference aboutGDPR;
	Preference aboutSDK;
	Preference aboutKarere;
	Preference aboutApp;
	Preference codeLink;
	Preference secondaryMediaFolderOn;
	Preference localSecondaryFolder;
	Preference megaSecondaryFolder;

	//File management
	Preference offlineFileManagement;
	Preference rubbishFileManagement;
	Preference fileVersionsFileManagement;
	Preference clearVersionsFileManagement;
	SwitchPreferenceCompat enableVersionsSwitch;

	SwitchPreferenceCompat enableRbSchedulerSwitch;
	Preference daysRbSchedulerPreference;

	ListPreference statusChatListPreference;
	ListPreference chatAttachmentsChatListPreference;

	TwoLineCheckPreference useHttpsOnly;

	MegaChatPresenceConfig statusConfig;

	Preference recoveryKey;
	Preference changePass;

	boolean cameraUpload = false;
	boolean secondaryUpload = false;
	boolean charging = false;
	boolean includeGPS;
	boolean pinLock = false;
	boolean fileNames = false;
	boolean autoAccept = true;

	ChatSettings chatSettings;
	String wifi = "";
	String camSyncLocalPath = "";
	boolean isExternalSDCard = false;
	Long camSyncHandle = null;
	MegaNode camSyncMegaNode = null;
	String camSyncMegaPath = "";
	String fileUpload = "";
	String videoQualitySummary = "";
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
    AlertDialog compressionQueueSizeDialog;
    private EditText queueSizeInput;

    private SetAttrUserListener setAttrUserListener;
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.preferences);

		preferenceScreen = (PreferenceScreen) findPreference("general_preference_screen");

		storageCategory = (PreferenceCategory) findPreference(CATEGORY_STORAGE);
		cameraUploadCategory = (PreferenceCategory) findPreference(CATEGORY_CAMERA_UPLOAD);
		pinLockCategory = (PreferenceCategory) findPreference(CATEGORY_PIN_LOCK);
		chatEnabledCategory = (PreferenceCategory) findPreference(CATEGORY_CHAT_ENABLED);
		chatNotificationsCategory = (PreferenceCategory) findPreference(CATEGORY_CHAT_NOTIFICATIONS);
		advancedFeaturesCategory = (PreferenceCategory) findPreference(CATEGORY_ADVANCED_FEATURES);
		autoawayChatCategory = (PreferenceCategory) findPreference(CATEGORY_AUTOAWAY_CHAT);
		persistenceChatCategory = (PreferenceCategory) findPreference(CATEGORY_PERSISTENCE_CHAT);
		qrCodeCategory = (PreferenceCategory) findPreference(CATEGORY_QR_CODE);
		securityCategory = (PreferenceCategory) findPreference(CATEGORY_SECURITY);
		twoFACategory = (PreferenceCategory) findPreference(CATEGORY_2FA);
		fileManagementCategory = (PreferenceCategory) findPreference(CATEGORY_FILE_MANAGEMENT);
		pinLockEnableSwitch = (SwitchPreferenceCompat) findPreference(KEY_PIN_LOCK_ENABLE);
		pinLockEnableSwitch.setOnPreferenceClickListener(this);

		richLinksSwitch = (SwitchPreferenceCompat) findPreference(KEY_RICH_LINKS_ENABLE);
		richLinksSwitch.setOnPreferenceClickListener(this);

		autoAwaySwitch = (SwitchPreferenceCompat) findPreference(KEY_AUTOAWAY_ENABLE);
		autoAwaySwitch.setOnPreferenceClickListener(this);

		qrCodeAutoAcceptSwitch = (SwitchPreferenceCompat) findPreference(KEY_QR_CODE_AUTO_ACCEPT);
		qrCodeAutoAcceptSwitch.setOnPreferenceClickListener(this);

		twoFASwitch = (SwitchPreferenceCompat) findPreference(KEY_2FA);
		twoFASwitch.setOnPreferenceClickListener(this);

		autoPlaySwitch = (SwitchPreferenceCompat) findPreference(KEY_AUTO_PLAY_SWITCH);
        autoPlaySwitch.setOnPreferenceClickListener(this);
        boolean autoPlayEnabled = prefs.isAutoPlayEnabled();
        autoPlaySwitch.setChecked(autoPlayEnabled);

		chatAttachmentsChatListPreference = (ListPreference) findPreference("settings_chat_send_originals");
		chatAttachmentsChatListPreference.setOnPreferenceChangeListener(this);

		statusChatListPreference = (ListPreference) findPreference("settings_chat_list_status");
		statusChatListPreference.setOnPreferenceChangeListener(this);

		chatAutoAwayPreference = findPreference(KEY_CHAT_AUTOAWAY);
		chatAutoAwayPreference.setOnPreferenceClickListener(this);

		chatPersistenceCheck = (TwoLineCheckPreference) findPreference(KEY_CHAT_PERSISTENCE);
		chatPersistenceCheck.setOnPreferenceClickListener(this);

		nestedNotificationsChat = findPreference(KEY_CHAT_NESTED_NOTIFICATIONS);
		nestedNotificationsChat.setOnPreferenceClickListener(this);

		nestedDownloadLocation = findPreference(KEY_STORAGE_DOWNLOAD);
		nestedDownloadLocation.setOnPreferenceClickListener(this);

		pinLockCode = findPreference(KEY_PIN_LOCK_CODE);
		pinLockCode.setOnPreferenceClickListener(this);

		useHttpsOnly = (TwoLineCheckPreference) findPreference("settings_use_https_only");
		useHttpsOnly.setOnPreferenceClickListener(this);

		cameraUploadOn = findPreference(KEY_CAMERA_UPLOAD_ON);
		cameraUploadOn.setOnPreferenceClickListener(this);

		cameraUploadHow = (ListPreference) findPreference(KEY_CAMERA_UPLOAD_HOW_TO);
		cameraUploadHow.setOnPreferenceChangeListener(this);

		cameraUploadWhat = (ListPreference) findPreference(KEY_CAMERA_UPLOAD_WHAT_TO);
		cameraUploadWhat.setOnPreferenceChangeListener(this);

		videoQuality = (ListPreference)findPreference(KEY_CAMERA_UPLOAD_VIDEO_QUALITY);
		videoQuality.setOnPreferenceChangeListener(this);

        cameraUploadIncludeGPS = (SwitchPreferenceCompat)findPreference(KEY_CAMERA_UPLOAD_INCLUDE_GPS);
        cameraUploadIncludeGPS.setOnPreferenceClickListener(this);

        cameraUploadCharging = (SwitchPreferenceCompat)findPreference(KEY_CAMERA_UPLOAD_CHARGING);
        cameraUploadCharging.setOnPreferenceClickListener(this);

        cameraUploadVideoQueueSize = findPreference(KEY_CAMERA_UPLOAD_VIDEO_QUEUE_SIZE);
        cameraUploadVideoQueueSize.setOnPreferenceClickListener(this);

		keepFileNames = (TwoLineCheckPreference) findPreference(KEY_KEEP_FILE_NAMES);
		keepFileNames.setOnPreferenceClickListener(this);

		localCameraUploadFolder = findPreference(KEY_CAMERA_UPLOAD_CAMERA_FOLDER);
		localCameraUploadFolder.setOnPreferenceClickListener(this);

		localCameraUploadFolderSDCard = findPreference(KEY_CAMERA_UPLOAD_CAMERA_FOLDER_SDCARD);
		localCameraUploadFolderSDCard.setOnPreferenceClickListener(this);

		megaCameraFolder = findPreference(KEY_CAMERA_UPLOAD_MEGA_FOLDER);
		megaCameraFolder.setOnPreferenceClickListener(this);

		secondaryMediaFolderOn = findPreference(KEY_SECONDARY_MEDIA_FOLDER_ON);
		secondaryMediaFolderOn.setOnPreferenceClickListener(this);

		localSecondaryFolder= findPreference(KEY_LOCAL_SECONDARY_MEDIA_FOLDER);
		localSecondaryFolder.setOnPreferenceClickListener(this);

		megaSecondaryFolder= findPreference(KEY_MEGA_SECONDARY_MEDIA_FOLDER);
		megaSecondaryFolder.setOnPreferenceClickListener(this);

		cacheAdvancedOptions = findPreference(KEY_CACHE);
		cacheAdvancedOptions.setOnPreferenceClickListener(this);
		offlineFileManagement = findPreference(KEY_OFFLINE);
		offlineFileManagement.setOnPreferenceClickListener(this);
		rubbishFileManagement = findPreference(KEY_RUBBISH);
		rubbishFileManagement.setOnPreferenceClickListener(this);

		fileVersionsFileManagement = findPreference(KEY_FILE_VERSIONS);
		clearVersionsFileManagement = findPreference(KEY_CLEAR_VERSIONS);
		clearVersionsFileManagement.setOnPreferenceClickListener(this);

		enableVersionsSwitch = (SwitchPreferenceCompat) findPreference(KEY_ENABLE_VERSIONS);

		updateEnabledFileVersions();
		enableRbSchedulerSwitch = (SwitchPreferenceCompat) findPreference(KEY_ENABLE_RB_SCHEDULER);
		enableLastGreenChatSwitch = (SwitchPreferenceCompat) findPreference(KEY_ENABLE_LAST_GREEN_CHAT);
		daysRbSchedulerPreference = (Preference) findPreference(KEY_DAYS_RB_SCHEDULER);

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

		recoveryKey = findPreference(KEY_RECOVERY_KEY);
		recoveryKey.setOnPreferenceClickListener(this);
		changePass = findPreference(KEY_CHANGE_PASSWORD);
		changePass.setOnPreferenceClickListener(this);

		helpSendFeedback = findPreference(KEY_HELP_SEND_FEEDBACK);
		helpSendFeedback.setOnPreferenceClickListener(this);

		cancelAccount = findPreference(KEY_CANCEL_ACCOUNT);
		cancelAccount.setOnPreferenceClickListener(this);
		updateCancelAccountSetting();

		aboutPrivacy = findPreference(KEY_ABOUT_PRIVACY_POLICY);
		aboutPrivacy.setOnPreferenceClickListener(this);

		aboutTOS = findPreference(KEY_ABOUT_TOS);
		aboutTOS.setOnPreferenceClickListener(this);

		aboutGDPR = findPreference(KEY_ABOUT_GDPR);
		aboutGDPR.setOnPreferenceClickListener(this);

		aboutApp = findPreference(KEY_ABOUT_APP_VERSION);
		aboutApp.setOnPreferenceClickListener(this);
		aboutSDK = findPreference(KEY_ABOUT_SDK_VERSION);
		aboutSDK.setOnPreferenceClickListener(this);
		aboutKarere = findPreference(KEY_ABOUT_KARERE_VERSION);
		aboutKarere.setOnPreferenceClickListener(this);

		codeLink = findPreference(KEY_ABOUT_CODE_LINK);
		codeLink.setOnPreferenceClickListener(this);

		if (prefs == null){
			logWarning("pref is NULL");
			dbH.setStorageAskAlways(true);

			File defaultDownloadLocation = buildDefaultDownloadDir(context);
			defaultDownloadLocation.mkdirs();

			dbH.setStorageDownloadLocation(defaultDownloadLocation.getAbsolutePath());

			dbH.setFirstTime(false);
			dbH.setCamSyncEnabled(false);
			dbH.setSecondaryUploadEnabled(false);
			dbH.setPinLockEnabled(false);
			dbH.setPinLockCode("");
			cameraUpload = false;
			charging = true;
			fileNames = false;
			pinLock = false;
		}
		else{
			if (prefs.getCamSyncEnabled() == null){
				dbH.setCamSyncEnabled(false);
				cameraUpload = false;
				charging = true;
				fileNames = false;
			}
			else{
				cameraUpload = Boolean.parseBoolean(prefs.getCamSyncEnabled());

				if (prefs.getCameraFolderExternalSDCard() != null){
					isExternalSDCard = Boolean.parseBoolean(prefs.getCameraFolderExternalSDCard());
				}
				String tempHandle = prefs.getCamSyncHandle();
				if(tempHandle!=null){
					camSyncHandle = Long.valueOf(tempHandle);
					if(camSyncHandle!=-1){
						camSyncMegaNode = megaApi.getNodeByHandle(camSyncHandle);
						if(camSyncMegaNode!=null){
							camSyncMegaPath = camSyncMegaNode.getName();
						}
						else
						{
							//The node for the Camera Sync no longer exists...
							dbH.setCamSyncHandle(-1);
							camSyncHandle = (long) -1;
							//Meanwhile is not created, set just the name
							camSyncMegaPath = getString(R.string.section_photo_sync);
						}
					}
					else{
						//Meanwhile is not created, set just the name
						camSyncMegaPath = getString(R.string.section_photo_sync);
					}
				}
				else{
					dbH.setCamSyncHandle(-1);
					camSyncHandle = (long) -1;
					//Meanwhile is not created, set just the name
					camSyncMegaPath = getString(R.string.section_photo_sync);
				}

				setWhatToUploadForCameraUpload();

				if (Boolean.parseBoolean(prefs.getCamSyncWifi())){
					wifi = getString(R.string.cam_sync_wifi);
					cameraUploadHow.setValueIndex(1);
				}
				else{
					wifi = getString(R.string.cam_sync_data);
					cameraUploadHow.setValueIndex(0);
				}

                if(!getString(R.string.settings_camera_upload_only_photos).equals(fileUpload)){
                    //video quality
                    String uploadQuality = prefs.getUploadVideoQuality();
                    int quality;
                    if (uploadQuality == null || uploadQuality.isEmpty()) {
                        dbH.setCameraUploadVideoQuality(MEDIUM);
                        quality = VIDEO_QUALITY_MEDIUM;
                    } else if (Integer.parseInt(uploadQuality) == ORIGINAL) {
                        quality = VIDEO_QUALITY_ORIGINAL;
                    } else {
                        quality = VIDEO_QUALITY_MEDIUM;
                    }
                    videoQuality.setValueIndex(quality);
                    videoQuality.setSummary(videoQuality.getEntry());

                    //require me to charge
                    if(quality == VIDEO_QUALITY_MEDIUM){
                        enableChargingSettings();
                        //convention on charging
                        if (prefs.getConversionOnCharging() == null){
                            dbH.setConversionOnCharging(true);
                            charging = true;
                        }
                        else{
                            charging = Boolean.parseBoolean(prefs.getConversionOnCharging());
                        }
                        cameraUploadCharging.setChecked(charging);

                        //show charge when size over $MB
                        if(charging){
                            enableVideoCompressionSizeSettings();
                        }else{
                            disableVideoCompressionSizeSettings();
                        }

                    }else{
                        disableChargingSettings();
                    }

                }else{
					hideVideoQualitySettingsSection();
                    dbH.setCameraUploadVideoQuality(ORIGINAL);
                    dbH.setConversionOnCharging(false);
                    dbH.setChargingOnSize(DEFAULT_CONVENTION_QUEUE_SIZE);
                }

                if (!getString(R.string.settings_camera_upload_only_videos).equals(fileUpload)) {
                    setupRemoveGPS();
                }

				// keep file name
				if (prefs.getKeepFileNames() == null){
					dbH.setKeepFileNames(false);
					fileNames = false;
				}
				else{
					fileNames = Boolean.parseBoolean(prefs.getKeepFileNames());
				}

				camSyncLocalPath = prefs.getCamSyncLocalPath();
				if (camSyncLocalPath == null){
					File cameraDownloadLocation = null;
					if (Environment.getExternalStorageDirectory() != null){
						cameraDownloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
					}

					cameraDownloadLocation.mkdirs();

					dbH.setCamSyncLocalPath(cameraDownloadLocation.getAbsolutePath());
					dbH.setCameraFolderExternalSDCard(false);
					isExternalSDCard = false;
					camSyncLocalPath = cameraDownloadLocation.getAbsolutePath();
				}
				else{
					if (INVALID_PATH.equals(camSyncLocalPath)){
						File cameraDownloadLocation = null;
						if (Environment.getExternalStorageDirectory() != null){
							cameraDownloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
						}

						cameraDownloadLocation.mkdirs();

						dbH.setCamSyncLocalPath(cameraDownloadLocation.getAbsolutePath());
						dbH.setCameraFolderExternalSDCard(false);
						isExternalSDCard = false;
						camSyncLocalPath = cameraDownloadLocation.getAbsolutePath();
					}
					else{
						File camFolder = new File(camSyncLocalPath);
						if (!isExternalSDCard){
							if(!camFolder.exists()){
								File cameraDownloadLocation = null;
								if (Environment.getExternalStorageDirectory() != null){
									cameraDownloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
								}

								cameraDownloadLocation.mkdirs();

								dbH.setCamSyncLocalPath(cameraDownloadLocation.getAbsolutePath());
								camSyncLocalPath = cameraDownloadLocation.getAbsolutePath();
							}
						}
						else{
							Uri uri = Uri.parse(prefs.getUriExternalSDCard());

							DocumentFile pickedDir = DocumentFile.fromTreeUri(context, uri);
							String pickedDirName = pickedDir.getName();
							if(pickedDirName!=null){
								camSyncLocalPath = pickedDir.getName();
								localCameraUploadFolder.setSummary(pickedDir.getName());
								localCameraUploadFolderSDCard.setSummary(pickedDir.getName());
							}
							else{
								logDebug("pickedDirNAme NULL");
							}
						}
					}
				}

				//Check if the secondary sync is enabled
				if (prefs.getSecondaryMediaFolderEnabled() == null){
					dbH.setSecondaryUploadEnabled(false);
					secondaryUpload = false;
				}
				else{
					secondaryUpload = Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled());
					logDebug("Secondary is: " + secondaryUpload);
				}
			}

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

		if (chatSettings == null) {
			dbH.setNotificationEnabledChat(true + "");
			dbH.setVibrationEnabledChat(true + "");
		}

		//Get chat status
		statusConfig = megaChatApi.getPresenceConfig();
		if (statusConfig != null) {
			logDebug("SETTINGS chatStatus pending: " + statusConfig.isPending());
			logDebug("Status: " + statusConfig.getOnlineStatus());

			statusChatListPreference.setValue(statusConfig.getOnlineStatus() + "");
			if (statusConfig.getOnlineStatus() == MegaChatApi.STATUS_INVALID) {
				statusChatListPreference.setSummary(getString(R.string.recovering_info));
			} else {
				statusChatListPreference.setSummary(statusChatListPreference.getEntry());
			}

			showPresenceChatConfig();

			if (megaChatApi.isSignalActivityRequired()) {
				megaChatApi.signalPresenceActivity();
			}
		} else {
			waitPresenceConfig();
		}

		boolean sendOriginalAttachment = isSendOriginalAttachments();
		if (sendOriginalAttachment) {
			chatAttachmentsChatListPreference.setValue(1 + "");
		} else {
			chatAttachmentsChatListPreference.setValue(0 + "");
		}
		chatAttachmentsChatListPreference.setSummary(chatAttachmentsChatListPreference.getEntry());

		boolean richLinks = MegaApplication.isEnabledRichLinks();
		richLinksSwitch.setChecked(richLinks);

		cacheAdvancedOptions.setSummary(getString(R.string.settings_advanced_features_calculating));
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

		if (cameraUpload){
			cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_off));
			cameraUploadHow.setSummary(wifi);
			localCameraUploadFolder.setSummary(camSyncLocalPath);
			localCameraUploadFolderSDCard.setSummary(camSyncLocalPath);
			megaCameraFolder.setSummary(camSyncMegaPath);
			localSecondaryFolder.setSummary(localSecondaryFolderPath);
			megaSecondaryFolder.setSummary(megaPathSecMediaFolder);
			cameraUploadWhat.setSummary(fileUpload);
			cameraUploadCharging.setChecked(charging);
			keepFileNames.setChecked(fileNames);
			cameraUploadCategory.addPreference(cameraUploadHow);
			cameraUploadCategory.addPreference(cameraUploadWhat);
            if(!charging){
                disableVideoCompressionSizeSettings();
            }
			cameraUploadCategory.addPreference(keepFileNames);

			File[] fs = context.getExternalFilesDirs(null);
			if (fs.length == 1){
				cameraUploadCategory.addPreference(localCameraUploadFolder);
				cameraUploadCategory.removePreference(localCameraUploadFolderSDCard);
			}
			else{
				if (fs.length > 1){
					if (fs[1] == null){
						cameraUploadCategory.addPreference(localCameraUploadFolder);
						cameraUploadCategory.removePreference(localCameraUploadFolderSDCard);
					}
					else{
						cameraUploadCategory.removePreference(localCameraUploadFolder);
						cameraUploadCategory.addPreference(localCameraUploadFolderSDCard);
					}
				}
			}

			if(secondaryUpload){
				//Check if the node exists in MEGA
				String secHandle = prefs.getMegaHandleSecondaryFolder();
				if(secHandle!=null){
					if (secHandle.compareTo("") != 0){
						logDebug("handleSecondaryMediaFolder NOT empty");
						handleSecondaryMediaFolder = Long.valueOf(secHandle);
						if(handleSecondaryMediaFolder!=null && handleSecondaryMediaFolder!=-1){
							megaNodeSecondaryMediaFolder = megaApi.getNodeByHandle(handleSecondaryMediaFolder);
							if(megaNodeSecondaryMediaFolder!=null){
								megaPathSecMediaFolder = megaNodeSecondaryMediaFolder.getName();
							}
							else{
								megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads);
							}
						}
						else{
							megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads);
						}
					}
					else{
						logWarning("handleSecondaryMediaFolder empty string");
						megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads);
					}

				}
				else{
					logWarning("handleSecondaryMediaFolder Null");
					dbH.setSecondaryFolderHandle(-1);
					handleSecondaryMediaFolder = (long) -1;
					megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads);
				}

				//check if the local secondary folder exists
				localSecondaryFolderPath = prefs.getLocalPathSecondaryFolder();
				if(localSecondaryFolderPath==null || localSecondaryFolderPath.equals(INVALID_PATH)){
					logWarning("Secondary ON: invalid localSecondaryFolderPath");
					localSecondaryFolderPath = getString(R.string.settings_empty_folder);
					Toast.makeText(context, getString(R.string.secondary_media_service_error_local_folder), Toast.LENGTH_SHORT).show();
				}
				else
				{
					File checkSecondaryFile = new File(localSecondaryFolderPath);
					if(!checkSecondaryFile.exists()){
						logWarning("Secondary ON: the local folder does not exist");
						dbH.setSecondaryFolderPath(INVALID_PATH);
						//If the secondary folder does not exist
						Toast.makeText(context, getString(R.string.secondary_media_service_error_local_folder), Toast.LENGTH_SHORT).show();
						localSecondaryFolderPath = getString(R.string.settings_empty_folder);

					}
				}

				megaSecondaryFolder.setSummary(megaPathSecMediaFolder);
				localSecondaryFolder.setSummary(localSecondaryFolderPath);
				secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_off));
				cameraUploadCategory.addPreference(localSecondaryFolder);
				cameraUploadCategory.addPreference(megaSecondaryFolder);

			}
			else{
				secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_on));
				cameraUploadCategory.removePreference(localSecondaryFolder);
				cameraUploadCategory.removePreference(megaSecondaryFolder);
			}
		}
		else{
			cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_on));
            cameraUploadOn.setSummary("");
			cameraUploadHow.setSummary("");
			localCameraUploadFolder.setSummary("");
			localCameraUploadFolderSDCard.setSummary("");
			megaCameraFolder.setSummary("");
			localSecondaryFolder.setSummary("");
			megaSecondaryFolder.setSummary("");
			cameraUploadWhat.setSummary("");
			cameraUploadCategory.removePreference(localCameraUploadFolder);
			cameraUploadCategory.removePreference(localCameraUploadFolderSDCard);
			hideVideoQualitySettingsSection();
            removeRemoveGPS();
			cameraUploadCategory.removePreference(keepFileNames);
			cameraUploadCategory.removePreference(megaCameraFolder);
			cameraUploadCategory.removePreference(cameraUploadHow);
			cameraUploadCategory.removePreference(cameraUploadWhat);

			//Remove Secondary Folder
			cameraUploadCategory.removePreference(secondaryMediaFolderOn);
			cameraUploadCategory.removePreference(localSecondaryFolder);
			cameraUploadCategory.removePreference(megaSecondaryFolder);
		}

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
			pinLockCategory.addPreference(pinLockCode);
		}
		else{
//			pinLockEnableSwitch.setTitle(getString(R.string.settings_pin_lock_on));
			pinLockCategory.removePreference(pinLockCode);
		}

		useHttpsOnlyValue = Boolean.parseBoolean(dbH.getUseHttpsOnly());
		logDebug("Value of useHttpsOnly: " + useHttpsOnlyValue);

		useHttpsOnly.setChecked(useHttpsOnlyValue);

		setAutoaccept = false;
		autoAccept = true;
		if (megaApi.multiFactorAuthAvailable()) {
			preferenceScreen.addPreference(twoFACategory);
			megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), (ManagerActivityLollipop) context);
		}
		else {
			preferenceScreen.removePreference(twoFACategory);
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
        cameraUploadCharging.setSummary(chargingHelper);

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
			advancedFeaturesCategory.removePreference(cancelAccount);
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
		scrollToPreference(qrCodeCategory);
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
		chatEnabledCategory.setEnabled(isOnline);
		chatNotificationsCategory.setEnabled(isOnline);
		autoawayChatCategory.setEnabled(isOnline);
		persistenceChatCategory.setEnabled(isOnline);
		cameraUploadCategory.setEnabled(isOnline);
		rubbishFileManagement.setEnabled(isOnline);
		clearVersionsFileManagement.setEnabled(isOnline);
		securityCategory.setEnabled(isOnline);
		qrCodeCategory.setEnabled(isOnline);
		twoFACategory.setEnabled(isOnline);

		//Rubbish bin scheduler
		daysRbSchedulerPreference.setEnabled(isOnline);
		enableRbSchedulerSwitch.setEnabled(isOnline);

		//File versioning
		fileVersionsFileManagement.setEnabled(isOnline);
		enableVersionsSwitch.setEnabled(isOnline);

		//Use of HTTP
		useHttpsOnly.setEnabled(isOnline);

		//Cancel account
		cancelAccount.setEnabled(isOnline);

		if (isOnline) {
			clearVersionsFileManagement.setLayoutResource(R.layout.delete_versions_preferences);
			cancelAccount.setLayoutResource(R.layout.cancel_account_preferences);
		}
		else {
			clearVersionsFileManagement.setLayoutResource(R.layout.delete_versions_preferences_disabled);
			cancelAccount.setLayoutResource(R.layout.cancel_account_preferences_disabled);
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
		if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_HOW_TO) == 0){
			switch (Integer.parseInt((String)newValue)){
				case CAMERA_UPLOAD_WIFI:{
					dbH.setCamSyncWifi(true);
					wifi = getString(R.string.cam_sync_wifi);
					cameraUploadHow.setValueIndex(1);
					break;
				}
				case CAMERA_UPLOAD_WIFI_OR_DATA_PLAN:{
					dbH.setCamSyncWifi(false);
					wifi = getString(R.string.cam_sync_data);
					cameraUploadHow.setValueIndex(0);
					break;
				}
			}
			cameraUploadHow.setSummary(wifi);
            rescheduleCameraUpload(context);
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_WHAT_TO) == 0){
			switch(Integer.parseInt((String)newValue)){
				case CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS:{
					dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
					fileUpload = getString(R.string.settings_camera_upload_only_photos);
					cameraUploadWhat.setValueIndex(0);
					setupRemoveGPS();
					resetVideoQualitySettings();
                    disableVideoQualitySettings();
					break;
				}
				case CAMERA_UPLOAD_FILE_UPLOAD_VIDEOS:{
					dbH.setCamSyncFileUpload(MegaPreferences.ONLY_VIDEOS);
					fileUpload = getString(R.string.settings_camera_upload_only_videos);
					cameraUploadWhat.setValueIndex(1);
					resetVideoQualitySettings();
					enableVideoQualitySettings();
					removeRemoveGPS();
					break;
				}
				case CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS_AND_VIDEOS:{
					dbH.setCamSyncFileUpload(MegaPreferences.PHOTOS_AND_VIDEOS);
					fileUpload = getString(R.string.settings_camera_upload_photos_and_videos);
					cameraUploadWhat.setValueIndex(2);
					setupRemoveGPS();
					resetVideoQualitySettings();
                    enableVideoQualitySettings();
					break;
				}
			}
			cameraUploadWhat.setSummary(fileUpload);
			resetCUTimestampsAndCache();
            rescheduleCameraUpload(context);
		}else if(preference.getKey().compareTo(KEY_CAMERA_UPLOAD_VIDEO_QUALITY) == 0){

			logDebug( "Video quality selected");
            switch(Integer.parseInt((String)newValue)){
                case VIDEO_QUALITY_ORIGINAL:{
                    dbH.setCameraUploadVideoQuality(ORIGINAL);
                    prefs.setUploadVideoQuality(ORIGINAL + "");
                    videoQuality.setValueIndex(VIDEO_QUALITY_ORIGINAL);
                    disableChargingSettings();
                    dbH.updateVideoState(SyncRecord.STATUS_PENDING);
                    break;
                }
                case VIDEO_QUALITY_MEDIUM:{
                    dbH.setCameraUploadVideoQuality(MEDIUM);
                    prefs.setUploadVideoQuality(MEDIUM + "");
                    videoQuality.setValueIndex(VIDEO_QUALITY_MEDIUM);
					resetVideoQualitySettings();
                    enableChargingSettings();
                    dbH.updateVideoState(SyncRecord.STATUS_TO_COMPRESS);
                    break;
                }
                default:
                    break;
            }

            videoQuality.setSummary(videoQuality.getEntry());
            rescheduleCameraUpload(context);

        } else if (preference.getKey().compareTo(KEY_PIN_LOCK_CODE) == 0){
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
		else if (preference.getKey().compareTo("settings_chat_list_status") == 0){
			logDebug("Change status (CHAT)");
			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}
			statusChatListPreference.setSummary(statusChatListPreference.getEntry());
			int newStatus= Integer.parseInt((String)newValue);
			megaChatApi.setOnlineStatus(newStatus, (ManagerActivityLollipop) context);
		}
		else if (preference.getKey().compareTo("settings_chat_send_originals") == 0){
			logDebug("Change send originals (CHAT)");
			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}

			int newStatus= Integer.parseInt((String)newValue);
			if(newStatus==0){
				dbH.setSendOriginalAttachments(false+"");
				chatAttachmentsChatListPreference.setValue(0+"");
			}
			else if(newStatus==1){
				dbH.setSendOriginalAttachments(true+"");
				chatAttachmentsChatListPreference.setValue(1+"");
			}
			chatAttachmentsChatListPreference.setSummary(chatAttachmentsChatListPreference.getEntry());
		}
		return true;
	}

	public void setCacheSize(String size){
		if(isAdded()){
			cacheAdvancedOptions.setSummary(getString(R.string.settings_advanced_features_size, size));
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
        else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_VIDEO_QUEUE_SIZE) == 0){
            showResetCompressionQueueSizeDialog();
        }
		else if (preference.getKey().compareTo(KEY_SECONDARY_MEDIA_FOLDER_ON) == 0){
			logDebug("Changing the secondary uploads");

			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}

			secondaryUpload = !secondaryUpload;
            if (secondaryUpload){
            	//If there is any possible secondary folder, set it as the default one
				long setSecondaryFolderHandle = getSecondaryFolderHandle();
				long possibleSecondaryFolderHandle = findDefaultFolder(getString(R.string.section_secondary_media_uploads));
				if ((setSecondaryFolderHandle == INVALID_HANDLE || isNodeInRubbishOrDeleted(setSecondaryFolderHandle)) &&
						possibleSecondaryFolderHandle != INVALID_HANDLE) {
					megaApi.setCameraUploadsFolders(INVALID_HANDLE, possibleSecondaryFolderHandle, setAttrUserListener);
				}

                restoreSecondaryTimestampsAndSyncRecordProcess();
                dbH.setSecondaryUploadEnabled(true);
				secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_off));
				//Check MEGA folder
				if(handleSecondaryMediaFolder!=null){
					if(handleSecondaryMediaFolder==-1){
						megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads);
					}
				}
				else{
					megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads);
				}

				megaSecondaryFolder.setSummary(megaPathSecMediaFolder);

				prefs = dbH.getPreferences();
				localSecondaryFolderPath = prefs.getLocalPathSecondaryFolder();

				//Check local folder
				if(localSecondaryFolderPath!=null){
					File checkSecondaryFile = new File(localSecondaryFolderPath);
					if(!checkSecondaryFile.exists()){
						dbH.setSecondaryFolderPath("-1");
						//If the secondary folder does not exist any more
						Toast.makeText(context, getString(R.string.secondary_media_service_error_local_folder), Toast.LENGTH_SHORT).show();

						if(localSecondaryFolderPath==null || localSecondaryFolderPath.equals(INVALID_PATH)){
							localSecondaryFolderPath = getString(R.string.settings_empty_folder);
						}
					} else {
                        stopRunningCameraUploadService(context);
                        startCameraUploadServiceIgnoreAttr(context);
                    }
				}
				else{
					dbH.setSecondaryFolderPath(INVALID_PATH);
					//If the secondary folder does not exist any more
					Toast.makeText(context, getString(R.string.secondary_media_service_error_local_folder), Toast.LENGTH_SHORT).show();
					localSecondaryFolderPath = getString(R.string.settings_empty_folder);
				}

				localSecondaryFolder.setSummary(localSecondaryFolderPath);
				cameraUploadCategory.addPreference(localSecondaryFolder);
				cameraUploadCategory.addPreference(megaSecondaryFolder);
			}
			else{
                resetMUTimestampsAndCache();
				dbH.setSecondaryUploadEnabled(false);
				secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_on));
				cameraUploadCategory.removePreference(localSecondaryFolder);
				cameraUploadCategory.removePreference(megaSecondaryFolder);
			}
			rescheduleCameraUpload(context);
		}
		else if (preference.getKey().compareTo(KEY_LOCAL_SECONDARY_MEDIA_FOLDER) == 0){
			logDebug("Changing the local folder for secondary uploads");
			Intent intent = new Intent(context, FileStorageActivityLollipop.class);
			intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
			intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
			intent.putExtra(FileStorageActivityLollipop.EXTRA_CAMERA_FOLDER, true);
			intent.putExtra(FileStorageActivityLollipop.IS_CU_OR_MU_FOLDER,true);
			startActivityForResult(intent, REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER);
		}
		else if (preference.getKey().compareTo(KEY_MEGA_SECONDARY_MEDIA_FOLDER) == 0){
			logDebug("Changing the MEGA folder for secondary uploads");
			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}
			Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
			intent.setAction(FileExplorerActivityLollipop.ACTION_CHOOSE_MEGA_FOLDER_SYNC);
			startActivityForResult(intent, REQUEST_MEGA_SECONDARY_MEDIA_FOLDER);
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_ON) == 0){
			logDebug("Changing camera upload");
			if(cameraUpload){
				if (!isOnline(context)){
					((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
					return false;
				}
			}

			dbH.setCamSyncTimeStamp(0);
			cameraUpload = !cameraUpload;
			refreshCameraUploadsSettings();
		}
		else if (preference.getKey().compareTo(KEY_PIN_LOCK_ENABLE) == 0){
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
				pinLockCategory.removePreference(pinLockCode);
			}
		} else if (preference.getKey().compareTo(KEY_AUTOAWAY_ENABLE) == 0) {
			logDebug("KEY_AUTOAWAY_ENABLE");
			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}
			statusConfig = megaChatApi.getPresenceConfig();
			if(statusConfig!=null){
				if(statusConfig.isAutoawayEnabled()){
					logDebug("Change AUTOAWAY chat to false");
					megaChatApi.setPresenceAutoaway(false, 0);
					autoawayChatCategory.removePreference(chatAutoAwayPreference);
				}
				else{
					logDebug("Change AUTOAWAY chat to true");
					megaChatApi.setPresenceAutoaway(true, 300);
					autoawayChatCategory.addPreference(chatAutoAwayPreference);
					chatAutoAwayPreference.setSummary(getString(R.string.settings_autoaway_value, 5));
				}
			}
		}
		else if (preference.getKey().compareTo(KEY_RICH_LINKS_ENABLE) == 0){

			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}

			if(richLinksSwitch.isChecked()){
				logDebug("Enable rich links");
				megaApi.enableRichPreviews(true, (ManagerActivityLollipop)context);
			}
			else{
				logDebug("Disable rich links");
				megaApi.enableRichPreviews(false, (ManagerActivityLollipop)context);
			}
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
		}
		else if (preference.getKey().compareTo(KEY_ENABLE_LAST_GREEN_CHAT) == 0){
			logDebug("Change KEY_ENABLE_LAST_GREEN_CHAT");

			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}

			if(!enableLastGreenChatSwitch.isChecked()){
				logDebug("Disable last green");
				((ManagerActivityLollipop)context).enableLastGreen(false);
			}
			else{
				logDebug("Enable last green");
				((ManagerActivityLollipop)context).enableLastGreen(true);
			}
		}
		else if(preference.getKey().compareTo(KEY_CHAT_AUTOAWAY) == 0){
			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}
			((ManagerActivityLollipop)context).showAutoAwayValueDialog();
		}
		else if(preference.getKey().compareTo(KEY_CHAT_PERSISTENCE) == 0){
			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}

			if(statusConfig.isPersist()){
				logDebug("Change persistence chat to false");
				megaChatApi.setPresencePersist(false);
			}
			else{
				logDebug("Change persistence chat to true");
				megaChatApi.setPresencePersist(true);
			}
		}
		else if(preference.getKey().compareTo(KEY_CHAT_NESTED_NOTIFICATIONS) == 0){
		    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		        //create default chat summary channel
                ChatAdvancedNotificationBuilder.createChatSummaryChannel(context);
		        //to system channel settings page
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                // change the setting of chat summary channel
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2);
                startActivity(intent);
            } else {
                //Intent to new activity Chat Settings
                Intent i = new Intent(context, ChatPreferencesActivity.class);
                startActivity(i);
            }
		} else if (preference.getKey().equals(KEY_STORAGE_DOWNLOAD)) {
			startActivity(new Intent(context, DownloadPreferencesActivity.class));
		} else if (preference.getKey().compareTo(KEY_PIN_LOCK_CODE) == 0){
			//Intent to reset the PIN
			logDebug("KEY_PIN_LOCK_CODE");
			resetPinLock();
		}
		else if (preference.getKey().compareTo("settings_use_https_only") == 0){
			logDebug("settings_use_https_only");
			useHttpsOnlyValue = useHttpsOnly.isChecked();
			dbH.setUseHttpsOnly(useHttpsOnlyValue);
			megaApi.useHttpsOnly(useHttpsOnlyValue);
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_CHARGING) == 0){
			logDebug("KEY_CAMERA_UPLOAD_CHARGING");
			charging = cameraUploadCharging.isChecked();
			if(charging){
                enableVideoCompressionSizeSettingsAndRestartUpload();
            }else{
                disableVideoCompressionSizeSettingsAndRestartUpload();
            }
			dbH.setConversionOnCharging(charging);
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_INCLUDE_GPS) == 0){
            logDebug("KEY_CAMERA_UPLOAD_INCLUDE_GPS");
            includeGPS = cameraUploadIncludeGPS.isChecked();
            dbH.setRemoveGPS(!includeGPS);
            rescheduleCameraUpload(context);
        }
		else if(preference.getKey().compareTo(KEY_KEEP_FILE_NAMES) == 0){
			logDebug("KEY_KEEP_FILE_NAMES");
			fileNames = keepFileNames.isChecked();
			dbH.setKeepFileNames(fileNames);
            Toast.makeText(context, getString(R.string.message_keep_device_name), Toast.LENGTH_SHORT).show();
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_CAMERA_FOLDER) == 0){
			logDebug("Changing the LOCAL folder for camera uploads");
			Intent intent = new Intent(context, FileStorageActivityLollipop.class);
			intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
			intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
			intent.putExtra(FileStorageActivityLollipop.EXTRA_CAMERA_FOLDER,true);
			intent.putExtra(FileStorageActivityLollipop.IS_CU_OR_MU_FOLDER,true);
			startActivityForResult(intent, REQUEST_CAMERA_FOLDER);
		}
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
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_MEGA_FOLDER) == 0){
			logDebug("Changing the MEGA folder for camera uploads");
			if (!isOnline(context)){
				((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
				return false;
			}
			Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
			intent.setAction(FileExplorerActivityLollipop.ACTION_CHOOSE_MEGA_FOLDER_SYNC);
			startActivityForResult(intent, REQUEST_MEGA_CAMERA_FOLDER);

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
		else if (preference.getKey().compareTo(KEY_CANCEL_ACCOUNT) == 0){
			logDebug("Cancel account preference");
			((ManagerActivityLollipop)context).askConfirmationDeleteAccount();
		}
		else if (preference.getKey().compareTo(KEY_QR_CODE_AUTO_ACCEPT) == 0){
//			First query if QR auto-accept is enabled or not, then change the value
			setAutoaccept = true;
			megaApi.getContactLinksOption((ManagerActivityLollipop) context);
		}
		else if (preference.getKey().compareTo(KEY_RECOVERY_KEY) == 0){
			logDebug("Export Recovery Key");
			((ManagerActivityLollipop)context).showMKLayout();
		}
		else if (preference.getKey().compareTo(KEY_CHANGE_PASSWORD) == 0){
			logDebug("Change password");
			Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
			startActivity(intent);
		}
		else if (preference.getKey().compareTo(KEY_2FA) == 0){
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

	/**
	 * Refresh the Camera Uploads service settings depending on the service status.
	 */
    public void refreshCameraUploadsSettings() {
		logDebug("refreshCameraUploadsSettings");
        boolean cuEnabled = false;
        if (prefs != null) {
            cuEnabled = Boolean.parseBoolean(prefs.getCamSyncEnabled());
        }
        if (!cuEnabled) {
			logDebug("Camera Uploads ON");
            String[] PERMISSIONS = {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            };

            if (!hasPermissions(context,PERMISSIONS)) {
                ActivityCompat.requestPermissions((ManagerActivityLollipop)context,PERMISSIONS,REQUEST_CAMERA_UPLOAD);
            } else {
				((ManagerActivityLollipop) context).checkIfShouldShowBusinessCUAlert(BUSINESS_CU_FRAGMENT_SETTINGS, false);
            }
        } else {
            disableCameraUpload();
        }
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		logDebug("onActivityResult");

		prefs = dbH.getPreferences();
		logDebug("REQUEST CODE: " + requestCode + "___RESULT CODE: " + resultCode);
		if (requestCode == REQUEST_CODE_TREE_LOCAL_CAMERA && resultCode == Activity.RESULT_OK){
			if (intent == null){
				logWarning("intent NULL");
				return;
			}

			Uri treeUri = intent.getData();

			if (dbH == null){
				dbH = DatabaseHandler.getDbHandler(context);
			}

			dbH.setUriExternalSDCard(treeUri.toString());
			dbH.setCameraFolderExternalSDCard(true);
			isExternalSDCard = true;

			DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);
			String pickedDirName = pickedDir.getName();
			if(pickedDirName!=null){
				prefs.setCamSyncLocalPath(pickedDir.getName());
				//prefs.setCamSyncHandle();
				camSyncLocalPath = pickedDir.getName();
				dbH.setCamSyncLocalPath(pickedDir.getName());
				localCameraUploadFolder.setSummary(pickedDir.getName());
				localCameraUploadFolderSDCard.setSummary(pickedDir.getName());
			}
			else{
				logWarning("pickedDirNAme NULL");
			}

			resetCUTimestampsAndCache();
			rescheduleCameraUpload(context);
		}
		else if(requestCode == SET_PIN){
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
			localSecondaryFolder.setSummary(secondaryPath);
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
			megaCameraFolder.setSummary(camSyncMegaPath);
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
			chatEnabledCategory.setEnabled(false);
			cameraUploadCategory.setEnabled(false);
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
				preferenceScreen.addPreference(twoFACategory);
				megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), (ManagerActivityLollipop) context);
			} else {
				logDebug("update2FAVisbility false");
				preferenceScreen.removePreference(twoFACategory);
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
		pinLockCategory.addPreference(pinLockCode);
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

	public void updatePresenceConfigChat(boolean cancelled, MegaChatPresenceConfig config){
		logDebug("updatePresenceConfigChat: " + cancelled);

		if(!cancelled){
			statusConfig = config;
		}

		showPresenceChatConfig();
	}

	public void updateEnabledRichLinks(){
		logDebug("updateEnabledRichLinks");

		if(MegaApplication.isEnabledRichLinks()!=richLinksSwitch.isChecked()){
			richLinksSwitch.setOnPreferenceClickListener(null);
			richLinksSwitch.setChecked(MegaApplication.isEnabledRichLinks());
			richLinksSwitch.setOnPreferenceClickListener(this);
		}
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

	public void waitPresenceConfig(){
		logDebug("waitPresenceConfig: ");

		preferenceScreen.removePreference(autoawayChatCategory);
		preferenceScreen.removePreference(persistenceChatCategory);

		statusChatListPreference.setValue(MegaChatApi.STATUS_OFFLINE+"");
		statusChatListPreference.setSummary(statusChatListPreference.getEntry());

		enableLastGreenChatSwitch.setEnabled(false);

	}

	public void showPresenceChatConfig(){
		logDebug("showPresenceChatConfig: " + statusConfig.getOnlineStatus());

		statusChatListPreference.setValue(statusConfig.getOnlineStatus()+"");
		statusChatListPreference.setSummary(statusChatListPreference.getEntry());

		if(statusConfig.getOnlineStatus()!= MegaChatApi.STATUS_ONLINE){
			preferenceScreen.removePreference(autoawayChatCategory);
			if(statusConfig.getOnlineStatus()== MegaChatApi.STATUS_OFFLINE){
				preferenceScreen.removePreference(persistenceChatCategory);
			}
			else{
				preferenceScreen.addPreference(persistenceChatCategory);
				if(statusConfig.isPersist()){
					chatPersistenceCheck.setChecked(true);
				}
				else{
					chatPersistenceCheck.setChecked(false);
				}
			}
		}
		else if(statusConfig.getOnlineStatus()== MegaChatApi.STATUS_ONLINE){
			//I'm online
			preferenceScreen.addPreference(persistenceChatCategory);
			if(statusConfig.isPersist()){
				chatPersistenceCheck.setChecked(true);
			}
			else{
				chatPersistenceCheck.setChecked(false);
			}

			if(statusConfig.isPersist()){
				preferenceScreen.removePreference(autoawayChatCategory);
			}
			else{
				preferenceScreen.addPreference(autoawayChatCategory);
				if(statusConfig.isAutoawayEnabled()){
					int timeout = (int)statusConfig.getAutoawayTimeout()/60;
					autoAwaySwitch.setChecked(true);
					autoawayChatCategory.addPreference(chatAutoAwayPreference);
					chatAutoAwayPreference.setSummary(getString(R.string.settings_autoaway_value, timeout));
				}
				else{
					autoAwaySwitch.setChecked(false);
					autoawayChatCategory.removePreference(chatAutoAwayPreference);
				}
			}
		}
		else{
			hidePreferencesChat();
		}

		//Show configuration last green
		if(statusConfig.isLastGreenVisible()){
			logDebug("Last visible ON");
			enableLastGreenChatSwitch.setEnabled(true);
			if(!enableLastGreenChatSwitch.isChecked()){
				enableLastGreenChatSwitch.setOnPreferenceClickListener(null);
				enableLastGreenChatSwitch.setChecked(true);
			}
			enableLastGreenChatSwitch.setOnPreferenceClickListener(this);
		}
		else{
			logDebug("Last visible OFF");
			enableLastGreenChatSwitch.setEnabled(true);
			if(enableLastGreenChatSwitch.isChecked()){
				enableLastGreenChatSwitch.setOnPreferenceClickListener(null);
				enableLastGreenChatSwitch.setChecked(false);
			}
			enableLastGreenChatSwitch.setOnPreferenceClickListener(this);
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
		logDebug("hidePreferencesChat");

		getPreferenceScreen().removePreference(chatNotificationsCategory);
		getPreferenceScreen().removePreference(autoawayChatCategory);
		getPreferenceScreen().removePreference(persistenceChatCategory);
		chatEnabledCategory.removePreference(chatAttachmentsChatListPreference);
		chatEnabledCategory.removePreference(richLinksSwitch);
		chatEnabledCategory.removePreference(enableLastGreenChatSwitch);
		chatEnabledCategory.removePreference(statusChatListPreference);
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

	private String getLocalDCIMFolderPath(){
		return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
	}

	private void setupRemoveGPS() {
        String removeGPSString = prefs.getRemoveGPS();
        if(TextUtils.isEmpty(removeGPSString)) {
            // default should set as false.
            includeGPS = false;
            dbH.setRemoveGPS(true);
        } else {
            includeGPS = !Boolean.parseBoolean(removeGPSString);
        }
        cameraUploadIncludeGPS.setChecked(includeGPS);
        cameraUploadIncludeGPS.setSummary(getResources().getString(R.string.settings_camera_upload_include_gps_helper_label));
        cameraUploadCategory.addPreference(cameraUploadIncludeGPS);
    }

    private void removeRemoveGPS() {
        cameraUploadIncludeGPS.setSummary("");
        cameraUploadCategory.removePreference(cameraUploadIncludeGPS);
    }

	private void setWhatToUploadForCameraUpload(){
		if (prefs.getCamSyncFileUpload() == null){
			dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
			fileUpload = getString(R.string.settings_camera_upload_only_photos);
			cameraUploadWhat.setValueIndex(0);
            setupRemoveGPS();
		}
		else{
			switch(Integer.parseInt(prefs.getCamSyncFileUpload())){
				case MegaPreferences.ONLY_PHOTOS:{
					fileUpload = getString(R.string.settings_camera_upload_only_photos);
					cameraUploadWhat.setValueIndex(0);
					disableVideoQualitySettings();
                    setupRemoveGPS();
					break;
				}
				case MegaPreferences.ONLY_VIDEOS:{
					fileUpload = getString(R.string.settings_camera_upload_only_videos);
					cameraUploadWhat.setValueIndex(1);
					removeRemoveGPS();
					break;
				}
				case MegaPreferences.PHOTOS_AND_VIDEOS:{
					fileUpload = getString(R.string.settings_camera_upload_photos_and_videos);
					cameraUploadWhat.setValueIndex(2);
                    setupRemoveGPS();
					break;
				}
				default:{
					fileUpload = getString(R.string.settings_camera_upload_only_photos);
					cameraUploadWhat.setValueIndex(0);
					disableVideoQualitySettings();
                    setupRemoveGPS();
					break;
				}
			}
		}
		cameraUploadWhat.setSummary(fileUpload);
	}

	private void setupConnectionTypeForCameraUpload(){
		if (prefs.getCamSyncWifi() == null){
			dbH.setCamSyncWifi(true);
			cameraUploadHow.setSummary(getString(R.string.cam_sync_wifi));
			cameraUploadHow.setValueIndex(1);
		}else{
			if(Boolean.parseBoolean(prefs.getCamSyncWifi())){
				cameraUploadHow.setSummary(getString(R.string.cam_sync_wifi));
				cameraUploadHow.setValueIndex(1);
			}else{
				cameraUploadHow.setSummary(getString(R.string.cam_sync_data));
				cameraUploadHow.setValueIndex(0);
			}
		}
	}

	private void setupLocalPathForCameraUpload(){
	    String cameraFolderLocation = prefs.getCamSyncLocalPath();
        if(TextUtils.isEmpty(cameraFolderLocation)) {
            cameraFolderLocation = getLocalDCIMFolderPath();
        }
		if (camSyncLocalPath != null) {
			if (!isExternalSDCard) {
				File checkFile = new File(camSyncLocalPath);
				if (!checkFile.exists()) {
					logWarning("Local path not exist, use default camera folder path");
					camSyncLocalPath = cameraFolderLocation;
				}
			} else {
				Uri uri = Uri.parse(prefs.getUriExternalSDCard());
				DocumentFile pickedDir = DocumentFile.fromTreeUri(context, uri);
				String pickedDirName = pickedDir.getName();
				if (pickedDirName != null) {
					camSyncLocalPath = pickedDirName;
				} else {
					logError("pickedDirName is NULL");
				}
			}
		} else {
			logError("Local path is NULL");
			dbH.setCameraFolderExternalSDCard(false);
			isExternalSDCard = false;
			camSyncLocalPath = cameraFolderLocation;
		}

		dbH.setCamSyncLocalPath(cameraFolderLocation);
		localCameraUploadFolder.setSummary(camSyncLocalPath);
		localCameraUploadFolderSDCard.setSummary(camSyncLocalPath);
		File[] fs = context.getExternalFilesDirs(null);
		if (fs.length == 1) {
			cameraUploadCategory.addPreference(localCameraUploadFolder);
			cameraUploadCategory.removePreference(localCameraUploadFolderSDCard);
		} else {
			if (fs.length > 1) {
				if (fs[1] == null) {
					cameraUploadCategory.addPreference(localCameraUploadFolder);
					cameraUploadCategory.removePreference(localCameraUploadFolderSDCard);
				} else {
					cameraUploadCategory.removePreference(localCameraUploadFolder);
					cameraUploadCategory.addPreference(localCameraUploadFolderSDCard);
				}
			}
		}
	}

	private void setupVideoOptionsForCameraUpload(){
		if (prefs.getCamSyncFileUpload() == null) {
			disableVideoQualitySettings();
		} else {
			boolean isPhotoOnly = Integer.parseInt(prefs.getCamSyncFileUpload()) == MegaPreferences.ONLY_PHOTOS;
			if (!isPhotoOnly) {
				enableVideoQualitySettings();
			}
		}
	}

	private void setupSecondaryUpload(){
		if (prefs.getSecondaryMediaFolderEnabled() == null) {
			dbH.setSecondaryUploadEnabled(false);
			secondaryUpload = false;
		} else {
			secondaryUpload = Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled());
		}

		if (secondaryUpload) {
			//Check if the node exists in MEGA
			String secHandle = prefs.getMegaHandleSecondaryFolder();
			if (secHandle != null) {
				if (!TextUtils.isEmpty(secHandle)) {
					logDebug("handleSecondaryMediaFolder NOT empty");
					handleSecondaryMediaFolder = Long.valueOf(secHandle);
					if (handleSecondaryMediaFolder != -1) {
						megaNodeSecondaryMediaFolder = megaApi.getNodeByHandle(handleSecondaryMediaFolder);
						if (megaNodeSecondaryMediaFolder != null) {
							megaPathSecMediaFolder = megaNodeSecondaryMediaFolder.getName();
						} else {
							megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads);
						}
					} else {
						megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads);
					}
				} else {
					logWarning("handleSecondaryMediaFolder empty string");
					megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads);
				}
			} else {
				logWarning("handleSecondaryMediaFolder Null");
				dbH.setSecondaryFolderHandle(-1);
				handleSecondaryMediaFolder = (long) -1;
				megaPathSecMediaFolder = getString(R.string.section_secondary_media_uploads);
			}

			//check if the local secondary folder exists
			localSecondaryFolderPath = prefs.getLocalPathSecondaryFolder();
			if (localSecondaryFolderPath == null || localSecondaryFolderPath.equals(INVALID_PATH)) {
				logWarning("Secondary ON: invalid localSecondaryFolderPath");
				localSecondaryFolderPath = getString(R.string.settings_empty_folder);
				Toast.makeText(context, getString(R.string.secondary_media_service_error_local_folder), Toast.LENGTH_SHORT).show();
			} else {
				File checkSecondaryFile = new File(localSecondaryFolderPath);
				if (!checkSecondaryFile.exists()) {
					logDebug("Secondary ON: the local folder does not exist");
					dbH.setSecondaryFolderPath(INVALID_PATH);
					//If the secondary folder does not exist
					Toast.makeText(context, getString(R.string.secondary_media_service_error_local_folder), Toast.LENGTH_SHORT).show();
					localSecondaryFolderPath = getString(R.string.settings_empty_folder);

				}
			}
			megaSecondaryFolder.setSummary(megaPathSecMediaFolder);
			localSecondaryFolder.setSummary(localSecondaryFolderPath);
			secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_off));
			cameraUploadCategory.addPreference(localSecondaryFolder);
			cameraUploadCategory.addPreference(megaSecondaryFolder);

		} else {
			secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_on));
			cameraUploadCategory.removePreference(localSecondaryFolder);
			cameraUploadCategory.removePreference(megaSecondaryFolder);
		}
	}

	private void setupPrimaryCloudFolder() {
		if (camSyncHandle == null) {
			camSyncMegaPath = getString(R.string.section_photo_sync);
		} else {
			if (camSyncHandle == -1) {
				camSyncMegaPath = getString(R.string.section_photo_sync);
			} else {
				logDebug("camSyncHandle is " + camSyncHandle);
			}
		}
		megaCameraFolder.setSummary(camSyncMegaPath);
	}

	public void enableCameraUpload() {
		cameraUpload = true;
		prefs = dbH.getPreferences();

		//internet connect type
		setupConnectionTypeForCameraUpload();

		//upload type
		setWhatToUploadForCameraUpload();

		//video options
		setupVideoOptionsForCameraUpload();

		//local primary folder
		setupLocalPathForCameraUpload();

        restorePrimaryTimestampsAndSyncRecordProcess();

		//cloud primary folder
		setupPrimaryCloudFolder();

		//secondary upload
		setupSecondaryUpload();

		//set cu enabled and start the service
		dbH.setCamSyncEnabled(true);

		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				logDebug("Enable Camera Uploads, Now I start the service");
				startCameraUploadService(context);
			}
		}, 1000);

		cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_off));
		cameraUploadCategory.addPreference(cameraUploadHow);
		cameraUploadCategory.addPreference(cameraUploadWhat);
		cameraUploadCategory.addPreference(keepFileNames);
		cameraUploadCategory.addPreference(megaCameraFolder);
		cameraUploadCategory.addPreference(secondaryMediaFolderOn);
	}

	/**
	 * This method is to do the setting process
	 * and UI related process
	 */
    public void disableCameraUpload(){
		disableCameraUploadSettingProcess();
		disableCameraUploadUIProcess();
	}

	/**
	 * Disable MediaUpload UI related process
	 */
	public void disableMediaUploadUIProcess() {
		logDebug("changes to sec folder only");
        secondaryUpload = false;
        secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_on));
        cameraUploadCategory.removePreference(localSecondaryFolder);
        cameraUploadCategory.removePreference(megaSecondaryFolder);
	}
	/**
	 * Disable CameraUpload UI related process
	 */
    public void disableCameraUploadUIProcess() {
		logDebug("Camera Uploads OFF");
		cameraUpload = false;
		cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_on));
		cameraUploadOn.setSummary("");
		cameraUploadCategory.removePreference(cameraUploadHow);
		cameraUploadCategory.removePreference(cameraUploadWhat);
		cameraUploadCategory.removePreference(localCameraUploadFolder);
		cameraUploadCategory.removePreference(localCameraUploadFolderSDCard);
		cameraUploadCategory.removePreference(cameraUploadIncludeGPS);
		hideVideoQualitySettingsSection();
		cameraUploadCategory.removePreference(keepFileNames);
		cameraUploadCategory.removePreference(megaCameraFolder);
		cameraUploadCategory.removePreference(secondaryMediaFolderOn);
		disableMediaUploadUIProcess();
	}

    public void showResetCompressionQueueSizeDialog(){
		logDebug("showResetCompressionQueueSizeDialog");
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        int margin = 20;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(px2dp(margin, outMetrics), px2dp(margin, outMetrics), px2dp(margin, outMetrics), 0);

        queueSizeInput = new EditText(context);
        queueSizeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(queueSizeInput, params);

        queueSizeInput.setSingleLine();
        queueSizeInput.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        queueSizeInput.setHint(getString(R.string.label_mega_byte));
        queueSizeInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        queueSizeInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String value = v.getText().toString().trim();
                    setCompressionQueueSize(value, queueSizeInput);

                    return true;
                }
                return false;
            }
        });

        queueSizeInput.setImeActionLabel(getString(R.string.general_create),EditorInfo.IME_ACTION_DONE);
        queueSizeInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showKeyboardDelayed(v);
                }
            }
        });

        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(px2dp(margin+5, outMetrics), px2dp(0, outMetrics), px2dp(margin, outMetrics), 0);
        final TextView text = new TextView(context);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);
        text.setText(getString(R.string.settings_compression_queue_subtitle,
				getString(R.string.label_file_size_mega_byte, String.valueOf(COMPRESSION_QUEUE_SIZE_MIN)),
				getString(R.string.label_file_size_mega_byte, String.valueOf(COMPRESSION_QUEUE_SIZE_MAX))));
        layout.addView(text,params);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.settings_video_compression_queue_size_popup_title));
        builder.setPositiveButton(getString(R.string.general_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) { }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), null);
        builder.setView(layout);
        compressionQueueSizeDialog = builder.create();
        compressionQueueSizeDialog.show();

        compressionQueueSizeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = queueSizeInput.getText().toString().trim();
                setCompressionQueueSize(value, queueSizeInput);
            }
        });
    }

    private void setCompressionQueueSize(String value, EditText input){
        if (value.length() == 0) {
            compressionQueueSizeDialog.dismiss();
            return;
        }

        try{
            int size = Integer.parseInt(value);
            if(isQueueSizeValid(size)){
                compressionQueueSizeDialog.dismiss();
                cameraUploadVideoQueueSize.setSummary(getResources().getString(R.string.label_file_size_mega_byte, String.valueOf(size)));
                String chargingHelper = getResources().getString(R.string.settings_camera_upload_charging_helper_label,
						getResources().getString(R.string.label_file_size_mega_byte, String.valueOf(size)));
                cameraUploadCharging.setSummary(chargingHelper);
                dbH.setChargingOnSize(size);
                prefs.setChargingOnSize(size + "");
                rescheduleCameraUpload(context);
            }else{
                resetSizeInput(input);
            }
        } catch (Exception e){
            resetSizeInput(input);
        }
    }

    private boolean isQueueSizeValid(int size){
		return size >= COMPRESSION_QUEUE_SIZE_MIN && size <= COMPRESSION_QUEUE_SIZE_MAX;
    }

    private void resetSizeInput(EditText input){
        input.setText("");
        input.requestFocus();
    }

	private void hideVideoQualitySettingsSection(){
		cameraUploadCategory.removePreference(videoQuality);
		cameraUploadCategory.removePreference(cameraUploadCharging);
		cameraUploadCategory.removePreference(cameraUploadVideoQueueSize);
	}

    private void disableVideoQualitySettings(){
		prefs.setUploadVideoQuality(String.valueOf(VIDEO_QUALITY_MEDIUM));
		dbH.setCameraUploadVideoQuality(VIDEO_QUALITY_MEDIUM);
        cameraUploadCategory.removePreference(videoQuality);
        disableChargingSettings();
    }

    private void disableChargingSettings(){
		charging = false;
		dbH.setConversionOnCharging(charging);
		cameraUploadCharging.setChecked(charging);
		cameraUploadCategory.removePreference(cameraUploadCharging);
		disableVideoCompressionSizeSettings();
    }

    private void disableVideoCompressionSizeSettings(){
        cameraUploadCategory.removePreference(cameraUploadVideoQueueSize);
    }

    private void disableVideoCompressionSizeSettingsAndRestartUpload(){
        disableVideoCompressionSizeSettings();
        rescheduleCameraUpload(context);
    }

    private void resetVideoQualitySettings(){
		dbH.setCameraUploadVideoQuality(VIDEO_QUALITY_MEDIUM);
		dbH.setConversionOnCharging(true);
		dbH.setChargingOnSize(DEFAULT_CONVENTION_QUEUE_SIZE);
		String chargingHelper = getResources().getString(R.string.settings_camera_upload_charging_helper_label,
				getResources().getString(R.string.label_file_size_mega_byte, String.valueOf(DEFAULT_CONVENTION_QUEUE_SIZE)));
		cameraUploadCharging.setSummary(chargingHelper);
	}

	private void enableVideoQualitySettings() {
		prefs = dbH.getPreferences();
		cameraUploadCategory.addPreference(videoQuality);
		String uploadQuality = prefs.getUploadVideoQuality();
		if (TextUtils.isEmpty(uploadQuality)) {
			prefs.setUploadVideoQuality(String.valueOf(VIDEO_QUALITY_MEDIUM));
			dbH.setCameraUploadVideoQuality(VIDEO_QUALITY_MEDIUM);
			videoQuality.setValueIndex(VIDEO_QUALITY_MEDIUM);
			enableChargingSettings();
		} else if (Integer.parseInt(uploadQuality) == MEDIUM) {
			enableChargingSettings();
			videoQuality.setValueIndex(VIDEO_QUALITY_MEDIUM);
		} else if (Integer.parseInt(uploadQuality) == ORIGINAL) {
			videoQuality.setValueIndex(VIDEO_QUALITY_ORIGINAL);
		}
		videoQuality.setSummary(videoQuality.getEntry());
	}

	private void enableChargingSettings() {
		prefs = dbH.getPreferences();
		if (prefs.getConversionOnCharging() == null) {
			dbH.setConversionOnCharging(true);
			charging = true;
		} else {
			charging = Boolean.parseBoolean(prefs.getConversionOnCharging());
		}
		cameraUploadCharging.setChecked(charging);
		cameraUploadCategory.addPreference(cameraUploadCharging);

		if(charging){
			enableVideoCompressionSizeSettings();
		}
	}

	private void enableVideoCompressionSizeSettings() {
		prefs = dbH.getPreferences();
		cameraUploadCategory.addPreference(cameraUploadVideoQueueSize);

		//convention queue size
		String sizeInDB = prefs.getChargingOnSize();
		int size;
		if (sizeInDB == null) {
			dbH.setChargingOnSize(DEFAULT_CONVENTION_QUEUE_SIZE);
			size = DEFAULT_CONVENTION_QUEUE_SIZE;
		} else {
			size = Integer.parseInt(sizeInDB);
		}
		cameraUploadVideoQueueSize.setSummary(getResources().getString(R.string.label_file_size_mega_byte, String.valueOf(size)));
	}

    private void enableVideoCompressionSizeSettingsAndRestartUpload(){
        enableVideoCompressionSizeSettings();
        rescheduleCameraUpload(context);
    }

    private boolean isNewSettingValid(String primaryPath, String secondaryPath, String primaryHandle, String secondaryHandle){
	    if(!secondaryUpload || primaryPath == null || primaryHandle == null || secondaryPath == null || secondaryHandle == null){
	        return true;
        }else if(primaryHandle.equals(secondaryHandle) && (primaryPath.contains(secondaryPath) || secondaryPath.contains(primaryPath))){
	        return false;
        }else{
            return true;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(compressionQueueSizeDialog != null && compressionQueueSizeDialog.isShowing()){
            outState.putBoolean(KEY_SET_QUEUE_DIALOG, true);
            outState.putString(KEY_SET_QUEUE_SIZE, queueSizeInput.getText().toString());
        }
    }
}
