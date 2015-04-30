package nz.mega.android;

import java.io.File;

import nz.mega.android.utils.Util;
import nz.mega.components.TwoLineCheckPreference;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.widget.Toast;


public class SettingsActivity extends PinPreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener {

	private MegaApiAndroid megaApi;
	static SettingsActivity preferencesActivity;
	
	Handler handler = new Handler();
	
	private static int REQUEST_DOWNLOAD_FOLDER = 1000;
	private static int REQUEST_CAMERA_FOLDER = 2000;
	private static int REQUEST_MEGA_CAMERA_FOLDER = 3000;
	private static int REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER = 4000;
	private static int REQUEST_MEGA_SECONDARY_MEDIA_FOLDER = 5000;
	
	public static String CATEGORY_PIN_LOCK = "settings_pin_lock";
	public static String CATEGORY_STORAGE = "settings_storage";
	public static String CATEGORY_CAMERA_UPLOAD = "settings_camera_upload";

	public static String KEY_PIN_LOCK_ENABLE = "settings_pin_lock_enable";
	public static String KEY_PIN_LOCK_CODE = "settings_pin_lock_code";
	public static String KEY_STORAGE_DOWNLOAD_LOCATION = "settings_storage_download_location";
	public static String KEY_STORAGE_ASK_ME_ALWAYS = "settings_storage_ask_me_always";
	public static String KEY_CAMERA_UPLOAD_ON = "settings_camera_upload_on";
	public static String KEY_CAMERA_UPLOAD_HOW_TO = "settings_camera_upload_how_to_upload";
	public static String KEY_CAMERA_UPLOAD_CHARGING = "settings_camera_upload_charging";
	public static String KEY_KEEP_FILE_NAMES = "settings_keep_file_names";
	public static String KEY_CAMERA_UPLOAD_WHAT_TO = "settings_camera_upload_what_to_upload";
	public static String KEY_CAMERA_UPLOAD_CAMERA_FOLDER = "settings_local_camera_upload_folder";
	public static String KEY_CAMERA_UPLOAD_MEGA_FOLDER = "settings_mega_camera_folder";
	
	public static String KEY_SECONDARY_MEDIA_FOLDER_ON = "settings_secondary_media_folder_on";
	public static String KEY_LOCAL_SECONDARY_MEDIA_FOLDER = "settings_local_secondary_media_folder";
	public static String KEY_MEGA_SECONDARY_MEDIA_FOLDER = "settings_mega_secondary_media_folder";
	
	public static String KEY_ABOUT_PRIVACY_POLICY = "settings_about_privacy_policy";
	public static String KEY_ABOUT_TOS = "settings_about_terms_of_service";
	
	public final static int CAMERA_UPLOAD_WIFI_OR_DATA_PLAN = 1001;
	public final static int CAMERA_UPLOAD_WIFI = 1002;
	
	public final static int CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS = 1001;
	public final static int CAMERA_UPLOAD_FILE_UPLOAD_VIDEOS = 1002;
	public final static int CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS_AND_VIDEOS = 1003;
	
	PreferenceCategory pinLockCategory;
	PreferenceCategory storageCategory;
	PreferenceCategory cameraUploadCategory;
	
	Preference pinLockEnable;
	EditTextPreference pinLockCode;
	Preference downloadLocation;
	Preference cameraUploadOn;
	ListPreference cameraUploadHow;
	ListPreference cameraUploadWhat;
	TwoLineCheckPreference cameraUploadCharging;
	TwoLineCheckPreference keepFileNames;
	Preference localCameraUploadFolder;
	Preference megaCameraFolder;
	Preference aboutPrivacy;
	Preference aboutTOS;
	Preference secondaryMediaFolderOn;
	Preference localSecondaryFolder;
	Preference megaSecondaryFolder;
	
	TwoLineCheckPreference storageAskMeAlways;
	
	boolean cameraUpload = false;
	boolean secondaryUpload = false;
	boolean charging = false;
	boolean pinLock = false;
	boolean askMe = false;
	boolean fileNames = false;
	
	DatabaseHandler dbH;
	
	MegaPreferences prefs;
	String wifi = "";
	String camSyncLocalPath = "";
	Long camSyncHandle = null;
	MegaNode camSyncMegaNode = null;
	String camSyncMegaPath = "";
	String fileUpload = "";
	String downloadLocationPath = "";
	String ast = "";
	String pinLockCodeTxt = "";
	
	//Secondary Folder
	String localSecondaryFolderPath = "";
	Long handleSecondaryMediaFolder = null;
	MegaNode megaNodeSecondaryMediaFolder = null;
	String megaPathSecMediaFolder = "";
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		log("onCreate");
		super.onCreate(savedInstanceState);
		
		preferencesActivity = this;
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
//		dbH = new DatabaseHandler(getApplicationContext());
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		if (dbH.getCredentials() == null){
			ManagerActivity.logout(this, megaApi, false);
			return;
		}
		
		prefs = dbH.getPreferences();		
		
		addPreferencesFromResource(R.xml.preferences);
		
		storageCategory = (PreferenceCategory) findPreference(CATEGORY_STORAGE);
		cameraUploadCategory = (PreferenceCategory) findPreference(CATEGORY_CAMERA_UPLOAD);	
		pinLockCategory = (PreferenceCategory) findPreference(CATEGORY_PIN_LOCK);
		
		pinLockEnable = findPreference(KEY_PIN_LOCK_ENABLE);
		pinLockEnable.setOnPreferenceClickListener(this);
		
		pinLockCode = (EditTextPreference) findPreference(KEY_PIN_LOCK_CODE);
		pinLockCode.setOnPreferenceChangeListener(this);
		pinLockCode.setOnPreferenceClickListener(this);
		
		downloadLocation = findPreference(KEY_STORAGE_DOWNLOAD_LOCATION);
		downloadLocation.setOnPreferenceClickListener(this);
		
		storageAskMeAlways = (TwoLineCheckPreference) findPreference(KEY_STORAGE_ASK_ME_ALWAYS);
		storageAskMeAlways.setOnPreferenceClickListener(this);
		
		cameraUploadOn = findPreference(KEY_CAMERA_UPLOAD_ON);
		cameraUploadOn.setOnPreferenceClickListener(this);
		
		cameraUploadHow = (ListPreference) findPreference(KEY_CAMERA_UPLOAD_HOW_TO);
		cameraUploadHow.setOnPreferenceChangeListener(this);
		
		cameraUploadWhat = (ListPreference) findPreference(KEY_CAMERA_UPLOAD_WHAT_TO);
		cameraUploadWhat.setOnPreferenceChangeListener(this);
		
		cameraUploadCharging = (TwoLineCheckPreference) findPreference(KEY_CAMERA_UPLOAD_CHARGING);
		cameraUploadCharging.setOnPreferenceClickListener(this);
		
		keepFileNames = (TwoLineCheckPreference) findPreference(KEY_KEEP_FILE_NAMES);
		keepFileNames.setOnPreferenceClickListener(this);
		
		localCameraUploadFolder = findPreference(KEY_CAMERA_UPLOAD_CAMERA_FOLDER);	
		localCameraUploadFolder.setOnPreferenceClickListener(this);
		
		megaCameraFolder = findPreference(KEY_CAMERA_UPLOAD_MEGA_FOLDER);	
		megaCameraFolder.setOnPreferenceClickListener(this);
		
		secondaryMediaFolderOn = findPreference(KEY_SECONDARY_MEDIA_FOLDER_ON);	
		secondaryMediaFolderOn.setOnPreferenceClickListener(this);
		
		localSecondaryFolder= findPreference(KEY_LOCAL_SECONDARY_MEDIA_FOLDER);	
		localSecondaryFolder.setOnPreferenceClickListener(this);
		
		megaSecondaryFolder= findPreference(KEY_MEGA_SECONDARY_MEDIA_FOLDER);	
		megaSecondaryFolder.setOnPreferenceClickListener(this);
		
		aboutPrivacy = findPreference(KEY_ABOUT_PRIVACY_POLICY);
		aboutPrivacy.setOnPreferenceClickListener(this);
		
		aboutTOS = findPreference(KEY_ABOUT_TOS);
		aboutTOS.setOnPreferenceClickListener(this);
		
		if (prefs == null){
			dbH.setStorageAskAlways(false);
			
			File defaultDownloadLocation = null;
			if (Environment.getExternalStorageDirectory() != null){
				defaultDownloadLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.downloadDIR + "/");
			}
			else{
				defaultDownloadLocation = getFilesDir();
			}
			
			defaultDownloadLocation.mkdirs();
			
			dbH.setStorageDownloadLocation(defaultDownloadLocation.getAbsolutePath());
			
			dbH.setFirstTime(false);
			dbH.setCamSyncEnabled(false);
			dbH.setSecondaryUploadEnabled(false);
			dbH.setPinLockEnabled(false);
			dbH.setPinLockCode("");
			pinLockCode.setText("");
			cameraUpload = false;
			charging = true;
			fileNames = false;
			pinLock = false;
			askMe = true;
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
				camSyncLocalPath = prefs.getCamSyncLocalPath();
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
						}
					}
				}		
				else{
					dbH.setCamSyncHandle(-1);
					camSyncHandle = (long) -1;
				}				
				
				if (prefs.getCamSyncFileUpload() == null){
					dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
					fileUpload = getString(R.string.settings_camera_upload_only_photos);
				}
				else{
					switch(Integer.parseInt(prefs.getCamSyncFileUpload())){
						case MegaPreferences.ONLY_PHOTOS:{
							fileUpload = getString(R.string.settings_camera_upload_only_photos);
							cameraUploadWhat.setValueIndex(0);
							break;
						}
						case MegaPreferences.ONLY_VIDEOS:{
							fileUpload = getString(R.string.settings_camera_upload_only_videos);
							cameraUploadWhat.setValueIndex(1);
							break;
						}
						case MegaPreferences.PHOTOS_AND_VIDEOS:{
							fileUpload = getString(R.string.settings_camera_upload_photos_and_videos);
							cameraUploadWhat.setValueIndex(2);
							break;
						}
						default:{
							fileUpload = getString(R.string.settings_camera_upload_only_photos);
							cameraUploadWhat.setValueIndex(0);
							break;
						}
					}
				}
				
				if (Boolean.parseBoolean(prefs.getCamSyncWifi())){
					wifi = getString(R.string.cam_sync_wifi);
					cameraUploadHow.setValueIndex(1);
				}
				else{
					wifi = getString(R.string.cam_sync_data);
					cameraUploadHow.setValueIndex(0);
				}	
				
				if (prefs.getCamSyncCharging() == null){
					dbH.setCamSyncCharging(true);
					charging = true;
				}
				else{
					charging = Boolean.parseBoolean(prefs.getCamSyncCharging());
				}
				
				if (prefs.getKeepFileNames() == null){
					dbH.setKeepFileNames(false);
					fileNames = false;
				}
				else{
					fileNames = Boolean.parseBoolean(prefs.getKeepFileNames());
				}
				
				if (camSyncLocalPath == null){
					File cameraDownloadLocation = null;
					if (Environment.getExternalStorageDirectory() != null){
						cameraDownloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
					}
					
					cameraDownloadLocation.mkdirs();
					
					dbH.setCamSyncLocalPath(cameraDownloadLocation.getAbsolutePath());
					
					camSyncLocalPath = cameraDownloadLocation.getAbsolutePath();
				}
				else{
					if (camSyncLocalPath.compareTo("") == 0){
						File cameraDownloadLocation = null;
						if (Environment.getExternalStorageDirectory() != null){
							cameraDownloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
						}
						
						cameraDownloadLocation.mkdirs();
						
						dbH.setCamSyncLocalPath(cameraDownloadLocation.getAbsolutePath());
						
						camSyncLocalPath = cameraDownloadLocation.getAbsolutePath();
					}
					else{
						File camFolder = new File(camSyncLocalPath);
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
				}
				
				//Check if the secondary sync is enabled
				if (prefs.getSecondaryMediaFolderEnabled() == null){
					dbH.setSecondaryUploadEnabled(false);
					secondaryUpload = false;						
				}
				else{
					secondaryUpload = Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled());
					log("onCreate, secondary is: "+secondaryUpload);
					
					if(secondaryUpload){
						secondaryUpload=true;						
					}
					else{
						secondaryUpload=false;
					}
				}
			}
			
			if (prefs.getPinLockEnabled() == null){
				dbH.setPinLockEnabled(false);
				dbH.setPinLockCode("");
				pinLockCode.setText("");
				pinLock = false;
			}
			else{
				pinLock = Boolean.parseBoolean(prefs.getPinLockEnabled());
				pinLockCodeTxt = prefs.getPinLockCode();
				if (pinLockCodeTxt == null){
					pinLockCodeTxt = "";
					dbH.setPinLockCode(pinLockCodeTxt);
					pinLockCode.setText(pinLockCodeTxt);
				}
			}
			
			if (prefs.getStorageAskAlways() == null){
				dbH.setStorageAskAlways(false);
				
				File defaultDownloadLocation = null;
				if (Environment.getExternalStorageDirectory() != null){
					defaultDownloadLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.downloadDIR + "/");
				}
				else{
					defaultDownloadLocation = getFilesDir();
				}
				
				defaultDownloadLocation.mkdirs();
				
				dbH.setStorageDownloadLocation(defaultDownloadLocation.getAbsolutePath());
				
				askMe = false;
				downloadLocationPath = defaultDownloadLocation.getAbsolutePath();
			}
			else{
				askMe = Boolean.parseBoolean(prefs.getStorageAskAlways());
				if (prefs.getStorageDownloadLocation() == null){
					File defaultDownloadLocation = null;
					if (Environment.getExternalStorageDirectory() != null){
						defaultDownloadLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.downloadDIR + "/");
					}
					else{
						defaultDownloadLocation = getFilesDir();
					}
					
					defaultDownloadLocation.mkdirs();
					
					dbH.setStorageDownloadLocation(defaultDownloadLocation.getAbsolutePath());
					
					downloadLocationPath = defaultDownloadLocation.getAbsolutePath();
				}
				else{
					downloadLocationPath = prefs.getStorageDownloadLocation();
					
					if (downloadLocationPath.compareTo("") == 0){
						File defaultDownloadLocation = null;
						if (Environment.getExternalStorageDirectory() != null){
							defaultDownloadLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.downloadDIR + "/");
						}
						else{
							defaultDownloadLocation = getFilesDir();
						}
						
						defaultDownloadLocation.mkdirs();
						
						dbH.setStorageDownloadLocation(defaultDownloadLocation.getAbsolutePath());
						
						downloadLocationPath = defaultDownloadLocation.getAbsolutePath();
					}
				}
			}
		}		

		if (cameraUpload){
			cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_off));
			cameraUploadHow.setSummary(wifi);
			localCameraUploadFolder.setSummary(camSyncLocalPath);
			megaCameraFolder.setSummary(camSyncMegaPath);
			localSecondaryFolder.setSummary(localSecondaryFolderPath);
			megaSecondaryFolder.setSummary(megaPathSecMediaFolder);
			cameraUploadWhat.setSummary(fileUpload);
			downloadLocation.setSummary(downloadLocationPath);
			cameraUploadCharging.setChecked(charging);
			keepFileNames.setChecked(fileNames);
			cameraUploadCategory.addPreference(cameraUploadHow);
			cameraUploadCategory.addPreference(cameraUploadWhat);
			cameraUploadCategory.addPreference(localCameraUploadFolder);
			cameraUploadCategory.addPreference(cameraUploadCharging);
			cameraUploadCategory.addPreference(keepFileNames);
			
			if(secondaryUpload){
				
				//Check if the node exists in MEGA
				String secHandle = prefs.getMegaHandleSecondaryFolder();
				if(secHandle!=null){
					handleSecondaryMediaFolder = Long.valueOf(secHandle);
					if(handleSecondaryMediaFolder!=null && handleSecondaryMediaFolder!=-1){
						megaNodeSecondaryMediaFolder = megaApi.getNodeByHandle(handleSecondaryMediaFolder);	
						if(megaNodeSecondaryMediaFolder!=null){
							megaPathSecMediaFolder = megaNodeSecondaryMediaFolder.getName();
						}
						else{
							megaPathSecMediaFolder = CameraSyncService.SECONDARY_UPLOADS;
						}
					}
					else{
						megaPathSecMediaFolder = CameraSyncService.SECONDARY_UPLOADS;
					}
				}
				else{
					dbH.setSecondaryFolderHandle(-1);
					handleSecondaryMediaFolder = (long) -1;
				}
				
				//check if the local secondary folder exists				
				localSecondaryFolderPath = prefs.getLocalPathSecondaryFolder();
				if(localSecondaryFolderPath==null || localSecondaryFolderPath.equals("-1")){
					log("secondary ON: invalid localSecondaryFolderPath");
					localSecondaryFolderPath = getString(R.string.settings_empty_folder);
					Util.showToast(this, getString(R.string.secondary_media_service_error_local_folder));					
				}
				else
				{
					File checkSecondaryFile = new File(localSecondaryFolderPath);
					if(!checkSecondaryFile.exists()){
						log("secondary ON: the local folder does not exist");
						dbH.setSecondaryFolderPath("-1");
						//If the secondary folder does not exist
						Util.showToast(this, getString(R.string.secondary_media_service_error_local_folder));
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
			cameraUploadHow.setSummary("");
			localCameraUploadFolder.setSummary("");
			megaCameraFolder.setSummary("");
			localSecondaryFolder.setSummary("");
			megaSecondaryFolder.setSummary("");
			cameraUploadWhat.setSummary("");
			downloadLocation.setSummary("");
			cameraUploadCategory.removePreference(localCameraUploadFolder);
			cameraUploadCategory.removePreference(cameraUploadCharging);
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
			pinLockEnable.setTitle(getString(R.string.settings_pin_lock_off));
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
			pinLockEnable.setTitle(getString(R.string.settings_pin_lock_on));
			pinLockCategory.removePreference(pinLockCode);
		}
		
		storageAskMeAlways.setChecked(askMe);

		if (storageAskMeAlways.isChecked()){
			downloadLocation.setEnabled(false);
			downloadLocation.setSummary("");
		}
		else{
			downloadLocation.setEnabled(true);
			downloadLocation.setSummary(downloadLocationPath);
		}
		
//		cameraUploadHow.setEnabled(false);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		prefs = dbH.getPreferences();
		if (preference.getKey().compareTo(KEY_STORAGE_DOWNLOAD_LOCATION) == 0){
			Intent intent = new Intent(SettingsActivity.this, FileStorageActivity.class);
			intent.setAction(FileStorageActivity.Mode.PICK_FOLDER.getAction());
			intent.putExtra(FileStorageActivity.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
			startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
		}
		else if (preference.getKey().compareTo(KEY_SECONDARY_MEDIA_FOLDER_ON) == 0){
			log("Changing the secondaty uploads");
			dbH.setSecSyncTimeStamp(0);			
			secondaryUpload = !secondaryUpload;
			if (secondaryUpload){
				dbH.setSecondaryUploadEnabled(true);
				secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_off));
				//Check MEGA folder
				if(handleSecondaryMediaFolder!=null){
					if(handleSecondaryMediaFolder==-1){
						megaPathSecMediaFolder = CameraSyncService.SECONDARY_UPLOADS;
					}
				}		
				else{
					megaPathSecMediaFolder = CameraSyncService.SECONDARY_UPLOADS;
				}
				
				megaSecondaryFolder.setSummary(megaPathSecMediaFolder);			
				
				//Check local folder
				File checkSecondaryFile = new File(localSecondaryFolderPath);
				if(!checkSecondaryFile.exists()){					
					dbH.setSecondaryFolderPath("-1");
					//If the secondary folder does not exist any more
					Util.showToast(this, getString(R.string.secondary_media_service_error_local_folder));		
					prefs = dbH.getPreferences();
					localSecondaryFolderPath = prefs.getLocalPathSecondaryFolder();
					log("Secondary folder in database: "+localSecondaryFolderPath);
					if(localSecondaryFolderPath==null || localSecondaryFolderPath.equals("-1")){
						localSecondaryFolderPath = getString(R.string.settings_empty_folder);
						
					}					
				}
				localSecondaryFolder.setSummary(localSecondaryFolderPath);
				
				cameraUploadCategory.addPreference(localSecondaryFolder);
				cameraUploadCategory.addPreference(megaSecondaryFolder);
				
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						log("Now I start the service");
						startService(new Intent(preferencesActivity, CameraSyncService.class));		
					}
				}, 5 * 1000);
				
			}
			else{				

				dbH.setSecondaryUploadEnabled(false);
				
				secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_on));
				cameraUploadCategory.removePreference(localSecondaryFolder);
				cameraUploadCategory.removePreference(megaSecondaryFolder);
			}			
		}
		else if (preference.getKey().compareTo(KEY_LOCAL_SECONDARY_MEDIA_FOLDER) == 0){
			Intent intent = new Intent(SettingsActivity.this, FileStorageActivity.class);
			intent.setAction(FileStorageActivity.Mode.PICK_FOLDER.getAction());
			intent.putExtra(FileStorageActivity.EXTRA_BUTTON_PREFIX, getString(R.string.general_select));
			startActivityForResult(intent, REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER);
		}
		else if (preference.getKey().compareTo(KEY_MEGA_SECONDARY_MEDIA_FOLDER) == 0){
			log("Changing the MEGA folder for secondary mega folder");
			Intent intent = new Intent(this, FileExplorerActivity.class);
			intent.setAction(FileExplorerActivity.ACTION_CHOOSE_MEGA_FOLDER_SYNC);
			startActivityForResult(intent, REQUEST_MEGA_SECONDARY_MEDIA_FOLDER);
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_ON) == 0){
			dbH.setCamSyncTimeStamp(0);			
			cameraUpload = !cameraUpload;			
			
			if (cameraUpload){
				if (camSyncLocalPath!=null){
					File checkFile = new File(camSyncLocalPath);
					if(!checkFile.exists()){
						//Local path does not exist, then Camera folder by default

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
					//Local path not valid = null, then Camera folder by default
					File cameraDownloadLocation = null;
					if (Environment.getExternalStorageDirectory() != null){
						cameraDownloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
					}
					
					cameraDownloadLocation.mkdirs();
					
					dbH.setCamSyncLocalPath(cameraDownloadLocation.getAbsolutePath());
					
					camSyncLocalPath = cameraDownloadLocation.getAbsolutePath();
				}

				if(camSyncHandle!=null){
					if(camSyncHandle==-1){
						camSyncMegaPath = CameraSyncService.CAMERA_UPLOADS;
					}
				}		
				else{
					camSyncMegaPath = CameraSyncService.CAMERA_UPLOADS;
				}
				
				megaCameraFolder.setSummary(camSyncMegaPath);
					
				dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
				fileUpload = getString(R.string.settings_camera_upload_only_photos);
				cameraUploadWhat.setValueIndex(0);
				
				dbH.setCamSyncWifi(true);
				wifi = getString(R.string.cam_sync_wifi);
				cameraUploadHow.setValueIndex(1);
				
				dbH.setCamSyncEnabled(true);
				
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						log("Now I start the service");
						startService(new Intent(preferencesActivity, CameraSyncService.class));		
					}
				}, 5 * 1000);
				
				cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_off));
				cameraUploadHow.setSummary(wifi);
				localCameraUploadFolder.setSummary(camSyncLocalPath);
						
				/*
				
				if(secondaryUpload){
					
					File checkSecondaryFile = new File(localSecondaryFolderPath);
					if(!checkSecondaryFile.exists()){
						dbH.setSecondaryUploadEnabled(true);
						dbH.setSecondaryFolderPath("-1");
						//If the secondary folder does not exist
						Util.showToast(this, getString(R.string.secondary_media_service_error_local_folder));
						secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_off));					
						prefs = dbH.getPreferences();
						localSecondaryFolderPath = prefs.getLocalPathSecondaryFolder();
						log("Sencondary folder in database: "+localSecondaryFolderPath);
						if(localSecondaryFolderPath==null || localSecondaryFolderPath.equals("-1")){
							localSecondaryFolderPath = getString(R.string.settings_empty_folder);
							localSecondaryFolder.setSummary(localSecondaryFolderPath);
						}	
						cameraUploadCategory.addPreference(localSecondaryFolder);
						cameraUploadCategory.addPreference(megaSecondaryFolder);								
					}
					else{
						secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_off));
						cameraUploadCategory.addPreference(localSecondaryFolder);
						cameraUploadCategory.addPreference(megaSecondaryFolder);	
					}							
				}
				else{
					secondaryMediaFolderOn.setTitle(getString(R.string.settings_camera_upload_on));
					cameraUploadCategory.removePreference(localSecondaryFolder);
					cameraUploadCategory.removePreference(megaSecondaryFolder);
				}*/
					
				cameraUploadWhat.setSummary(fileUpload);
				cameraUploadCategory.addPreference(cameraUploadHow);
				cameraUploadCategory.addPreference(cameraUploadWhat);
				cameraUploadCategory.addPreference(localCameraUploadFolder);
				cameraUploadCategory.addPreference(cameraUploadCharging);
				cameraUploadCategory.addPreference(keepFileNames);
				cameraUploadCategory.addPreference(megaCameraFolder);								
				cameraUploadCategory.addPreference(secondaryMediaFolderOn);
				cameraUploadCategory.removePreference(localSecondaryFolder);
				cameraUploadCategory.removePreference(megaSecondaryFolder);		


			}
			else{
				dbH.setCamSyncEnabled(false);
				dbH.setSecondaryUploadEnabled(false);
				secondaryUpload = false;
				Intent stopIntent = null;
				stopIntent = new Intent(getApplicationContext(), CameraSyncService.class);
				stopIntent.setAction(CameraSyncService.ACTION_STOP);
				startService(stopIntent);
				
				cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_on));
				secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_on));
				cameraUploadCategory.removePreference(cameraUploadHow);
				cameraUploadCategory.removePreference(cameraUploadWhat);
				cameraUploadCategory.removePreference(localCameraUploadFolder);
				cameraUploadCategory.removePreference(cameraUploadCharging);
				cameraUploadCategory.removePreference(keepFileNames);
				cameraUploadCategory.removePreference(megaCameraFolder);
				cameraUploadCategory.removePreference(secondaryMediaFolderOn);
				cameraUploadCategory.removePreference(localSecondaryFolder);
				cameraUploadCategory.removePreference(megaSecondaryFolder);
			}			
		}
		else if (preference.getKey().compareTo(KEY_PIN_LOCK_ENABLE) == 0){
			pinLock = !pinLock;
			if (pinLock){
				pinLockCodeTxt = prefs.getPinLockCode();
				if (pinLockCodeTxt == null){
					pinLockCodeTxt = "";
					dbH.setPinLockCode(pinLockCodeTxt);
					pinLockCode.setText(pinLockCodeTxt);
				}
				pinLockEnable.setTitle(getString(R.string.settings_pin_lock_off));
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
			else{
				dbH.setPinLockEnabled(false);
				dbH.setPinLockCode("");
				pinLockCode.setText("");
				pinLockEnable.setTitle(getString(R.string.settings_pin_lock_on));
				pinLockCategory.removePreference(pinLockCode);
			}
		}
		else if (preference.getKey().compareTo(KEY_STORAGE_ASK_ME_ALWAYS) == 0){
			askMe = storageAskMeAlways.isChecked();
			dbH.setStorageAskAlways(askMe);
			if (storageAskMeAlways.isChecked()){
				downloadLocation.setEnabled(false);
			}
			else{
				downloadLocation.setEnabled(true);
			}
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_CHARGING) == 0){
			charging = cameraUploadCharging.isChecked();
			dbH.setCamSyncCharging(charging);
		}
		else if(preference.getKey().compareTo(KEY_KEEP_FILE_NAMES) == 0){
			fileNames = keepFileNames.isChecked();
			dbH.setKeepFileNames(fileNames);
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_CAMERA_FOLDER) == 0){
			Intent intent = new Intent(SettingsActivity.this, FileStorageActivity.class);
			intent.setAction(FileStorageActivity.Mode.PICK_FOLDER.getAction());
			intent.putExtra(FileStorageActivity.EXTRA_BUTTON_PREFIX, getString(R.string.general_select));
			startActivityForResult(intent, REQUEST_CAMERA_FOLDER);
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_MEGA_FOLDER) == 0){
			log("Changing the MEGA folder for camera uploads");
			Intent intent = new Intent(this, FileExplorerActivity.class);
			intent.setAction(FileExplorerActivity.ACTION_CHOOSE_MEGA_FOLDER_SYNC);
			startActivityForResult(intent, REQUEST_MEGA_CAMERA_FOLDER);
		}
		else if (preference.getKey().compareTo(KEY_ABOUT_PRIVACY_POLICY) == 0){
			Intent viewIntent = new Intent(Intent.ACTION_VIEW);
			viewIntent.setData(Uri.parse("https://mega.nz/mobile_privacy.html"));
			startActivity(viewIntent);
		}
		else if (preference.getKey().compareTo(KEY_ABOUT_TOS) == 0){
			Intent viewIntent = new Intent(Intent.ACTION_VIEW);
			viewIntent.setData(Uri.parse("https://mega.nz/mobile_terms.html"));
			startActivity(viewIntent);
		}
		log("KEY = " + preference.getKey());
		
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		log("onActivityResult");
		
		prefs = dbH.getPreferences();
		if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK && intent != null) {
			String path = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			dbH.setStorageDownloadLocation(path);
			downloadLocation.setSummary(path);
		}		
		else if (requestCode == REQUEST_CAMERA_FOLDER && resultCode == RESULT_OK && intent != null){
			//Local folder to sync
			String cameraPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			dbH.setCamSyncLocalPath(cameraPath);
			localCameraUploadFolder.setSummary(cameraPath);
			dbH.setCamSyncTimeStamp(0);
			
			Intent photosVideosIntent = null;
			photosVideosIntent = new Intent(getApplicationContext(), CameraSyncService.class);
			photosVideosIntent.setAction(CameraSyncService.ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER);
			startService(photosVideosIntent);
			
			handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					log("Now I start the service");
					startService(new Intent(preferencesActivity, CameraSyncService.class));		
				}
			}, 5 * 1000);
		}
		else if (requestCode == REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER && resultCode == RESULT_OK && intent != null){
			//Local folder to sync
			String secondaryPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			
			dbH.setSecondaryFolderPath(secondaryPath);
			localSecondaryFolder.setSummary(secondaryPath);
			dbH.setSecSyncTimeStamp(0);
			
			Intent photosVideosIntent = null;
			photosVideosIntent = new Intent(getApplicationContext(), CameraSyncService.class);
			photosVideosIntent.setAction(CameraSyncService.ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER);
			startService(photosVideosIntent);
			
			handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					log("Now I start the service");
					startService(new Intent(preferencesActivity, CameraSyncService.class));		
				}
			}, 5 * 1000);
		}		
		else if (requestCode == REQUEST_MEGA_SECONDARY_MEDIA_FOLDER && resultCode == RESULT_OK && intent != null){
			//Mega folder to sync
			
			Long handle = intent.getLongExtra("SELECT_MEGA_FOLDER",-1);
			if(handle!=-1){
				dbH.setSecondaryFolderHandle(handle);						
				
				handleSecondaryMediaFolder = handle;
				megaNodeSecondaryMediaFolder = megaApi.getNodeByHandle(handleSecondaryMediaFolder);
				megaPathSecMediaFolder = megaNodeSecondaryMediaFolder.getName();
				
				megaSecondaryFolder.setSummary(megaPathSecMediaFolder);
				dbH.setSecSyncTimeStamp(0);
				
				Intent photosVideosIntent = null;
				photosVideosIntent = new Intent(getApplicationContext(), CameraSyncService.class);
				photosVideosIntent.setAction(CameraSyncService.ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER);
				startService(photosVideosIntent);
				
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						log("Now I start the service");
						startService(new Intent(preferencesActivity, CameraSyncService.class));		
					}
				}, 5 * 1000);
				
				log("Mega folder to secondary uploads change!!");
			}
			else{
				log("Error choosing the secondary uploads");
			}
			
		}
		else if (requestCode == REQUEST_MEGA_CAMERA_FOLDER && resultCode == RESULT_OK && intent != null){
			//Mega folder to sync
			
			Long handle = intent.getLongExtra("SELECT_MEGA_FOLDER",-1);
			if(handle!=-1){
				dbH.setCamSyncHandle(handle);
				
				camSyncHandle = handle;
				camSyncMegaNode = megaApi.getNodeByHandle(camSyncHandle);	
				camSyncMegaPath = camSyncMegaNode.getName();
				
				megaCameraFolder.setSummary(camSyncMegaPath);
				dbH.setCamSyncTimeStamp(0);
				
				Intent photosVideosIntent = null;
				photosVideosIntent = new Intent(getApplicationContext(), CameraSyncService.class);
				photosVideosIntent.setAction(CameraSyncService.ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER);
				startService(photosVideosIntent);
				
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						log("Now I start the service");
						startService(new Intent(preferencesActivity, CameraSyncService.class));		
					}
				}, 5 * 1000);
				
				log("Mega folder to sync the Camera CHANGED!!");
			}
			else{
				log("Error choosing the Mega folder to sync the Camera");
			}
			
		}
	}
	
	public static void log(String message) {
		Util.log("SettingsActivity", message);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		log("onPreferenceChange");
		
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
			handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					log("Now I start the service");
					startService(new Intent(preferencesActivity, CameraSyncService.class));		
				}
			}, 30 * 1000);
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_WHAT_TO) == 0){
			switch(Integer.parseInt((String)newValue)){
				case CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS:{
					dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
					fileUpload = getString(R.string.settings_camera_upload_only_photos);
					cameraUploadWhat.setValueIndex(0);
					break;
				}
				case CAMERA_UPLOAD_FILE_UPLOAD_VIDEOS:{
					dbH.setCamSyncFileUpload(MegaPreferences.ONLY_VIDEOS);
					fileUpload = getString(R.string.settings_camera_upload_only_videos);
					cameraUploadWhat.setValueIndex(1);
					break;
				}
				case CAMERA_UPLOAD_FILE_UPLOAD_PHOTOS_AND_VIDEOS:{
					dbH.setCamSyncFileUpload(MegaPreferences.PHOTOS_AND_VIDEOS);
					fileUpload = getString(R.string.settings_camera_upload_photos_and_videos);
					cameraUploadWhat.setValueIndex(2);
					break;
				}
			}
			cameraUploadWhat.setSummary(fileUpload);
			
			Intent photosVideosIntent = null;
			photosVideosIntent = new Intent(getApplicationContext(), CameraSyncService.class);
			photosVideosIntent.setAction(CameraSyncService.ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER);
			startService(photosVideosIntent);
			
			handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					log("Now I start the service");
					startService(new Intent(preferencesActivity, CameraSyncService.class));		
				}
			}, 30 * 1000);
		}
		else if (preference.getKey().compareTo(KEY_PIN_LOCK_CODE) == 0){
			pinLockCodeTxt = (String) newValue;
			dbH.setPinLockCode(pinLockCodeTxt);
			pinLockCode.setText(pinLockCodeTxt);
			
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
			log("Object: " + newValue);
		}
		
		return true;
	}
}
