package nz.mega.android;

import java.io.File;

import nz.mega.android.utils.Util;
import nz.mega.components.TwoLineCheckPreference;
import nz.mega.sdk.MegaApiAndroid;

import android.content.Intent;
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


public class SettingsActivity extends PinPreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener {

	private MegaApiAndroid megaApi;
	static SettingsActivity preferencesActivity;
	
	Handler handler = new Handler();
	
	private static int REQUEST_DOWNLOAD_FOLDER = 1000;
	private static int REQUEST_CAMERA_FOLDER = 2000;
	
	public static String CATEGORY_PIN_LOCK = "settings_pin_lock";
	public static String CATEGORY_STORAGE = "settings_storage";
	public static String CATEGORY_CAMERA_UPLOAD = "settings_camera_upload";

	public static String KEY_PIN_LOCK_ENABLE = "settings_pin_lock_enable";
	public static String KEY_PIN_LOCK_CODE = "settings_pin_lock_code";
	public static String KEY_STORAGE_DOWNLOAD_LOCATION = "settings_storage_download_location";
	public static String KEY_STORAGE_ASK_ME_ALWAYS = "settings_storage_ask_me_always";
	public static String KEY_CAMERA_UPLOAD_ON = "settings_camera_upload_on";
	public static String KEY_CAMERA_UPLOAD_HOW_TO = "settings_camera_upload_how_to_upload";
	public static String KEY_CAMERA_UPLOAD_WHAT_TO = "settings_camera_upload_what_to_upload";
	public static String KEY_CAMERA_UPLOAD_CAMERA_FOLDER = "settings_camera_upload_folder";
	
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
	Preference cameraUploadFolder;
	
	TwoLineCheckPreference storageAskMeAlways;
	
	boolean cameraUpload = false;
	boolean pinLock = false;
	boolean askMe = false;
	
	DatabaseHandler dbH;
	
	MegaPreferences prefs;
	String wifi = "";
	String camSyncLocalPath = "";
	String fileUpload = "";
	String downloadLocationPath = "";
	String ast = "";
	String pinLockCodeTxt = "";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
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
		
		cameraUploadFolder = findPreference(KEY_CAMERA_UPLOAD_CAMERA_FOLDER);	
		cameraUploadFolder.setOnPreferenceClickListener(this);
		
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
			dbH.setPinLockEnabled(false);
			dbH.setPinLockCode("");
			pinLockCode.setText("");
			cameraUpload = false;
			pinLock = false;
			askMe = true;
		}
		else{
			if (prefs.getCamSyncEnabled() == null){
				dbH.setCamSyncEnabled(false);
				cameraUpload = false;	
			}
			else{
				cameraUpload = Boolean.parseBoolean(prefs.getCamSyncEnabled());
				camSyncLocalPath = prefs.getCamSyncLocalPath();
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
			cameraUploadFolder.setSummary(camSyncLocalPath);
			cameraUploadWhat.setSummary(fileUpload);
			downloadLocation.setSummary(downloadLocationPath);
			cameraUploadCategory.addPreference(cameraUploadHow);
			cameraUploadCategory.addPreference(cameraUploadWhat);
			cameraUploadCategory.addPreference(cameraUploadFolder);
		}
		else{
			cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_on));
			cameraUploadHow.setSummary("");
			cameraUploadFolder.setSummary("");
			cameraUploadWhat.setSummary("");
			downloadLocation.setSummary("");
			cameraUploadCategory.removePreference(cameraUploadHow);
			cameraUploadCategory.removePreference(cameraUploadWhat);
			cameraUploadCategory.removePreference(cameraUploadFolder);
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
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_ON) == 0){
			dbH.setCamSyncTimeStamp(0);
			cameraUpload = !cameraUpload;
			if (cameraUpload){
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
				cameraUploadFolder.setSummary(camSyncLocalPath);
				cameraUploadWhat.setSummary(fileUpload);
				cameraUploadCategory.addPreference(cameraUploadHow);
				cameraUploadCategory.addPreference(cameraUploadWhat);
				cameraUploadCategory.addPreference(cameraUploadFolder);
			}
			else{
				dbH.setCamSyncEnabled(false);
				
				Intent stopIntent = null;
				stopIntent = new Intent(getApplicationContext(), CameraSyncService.class);
				stopIntent.setAction(CameraSyncService.ACTION_STOP);
				startService(stopIntent);
				
				cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_on));
				cameraUploadCategory.removePreference(cameraUploadHow);
				cameraUploadCategory.removePreference(cameraUploadWhat);
				cameraUploadCategory.removePreference(cameraUploadFolder);
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
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_CAMERA_FOLDER) == 0){
			Intent intent = new Intent(SettingsActivity.this, FileStorageActivity.class);
			intent.setAction(FileStorageActivity.Mode.PICK_FOLDER.getAction());
			intent.putExtra(FileStorageActivity.EXTRA_BUTTON_PREFIX, getString(R.string.context_camera_folder));
			startActivityForResult(intent, REQUEST_CAMERA_FOLDER);
		}
		log("KEY = " + preference.getKey());
		
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		prefs = dbH.getPreferences();
		if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK && intent != null) {
			String path = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			dbH.setStorageDownloadLocation(path);
			downloadLocation.setSummary(path);
		}
		else if (requestCode == REQUEST_CAMERA_FOLDER && resultCode == RESULT_OK && intent != null){
			String cameraPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			dbH.setCamSyncLocalPath(cameraPath);
			cameraUploadFolder.setSummary(cameraPath);
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
	}
	
	public static void log(String message) {
		Util.log("SettingsActivity", message);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
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
