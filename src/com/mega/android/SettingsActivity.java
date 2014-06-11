package com.mega.android;

import com.mega.sdk.MegaApiAndroid;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener {

	private MegaApiAndroid megaApi;
	static SettingsActivity preferencesActivity;
	
	private static int REQUEST_DOWNLOAD_FOLDER = 1000;
	
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
	
	PreferenceCategory pinLockCategory;
	PreferenceCategory storageCategory;
	PreferenceCategory cameraUploadCategory;
	
	
	Preference pinLockEnable;
	Preference pinLockCode;
	Preference downloadLocation;
	Preference cameraUploadOn;
	Preference cameraUploadHow;
	Preference cameraUploadWhat;
	Preference cameraUploadFolder;
	
	CheckBoxPreference storageAskMeAlways;
	
	boolean cameraUpload = false;
	boolean pinLock = false;
	boolean askMe = false;
	
	DatabaseHandler dbH;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		preferencesActivity = this;
		
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		dbH = new DatabaseHandler(getApplicationContext()); 
		if (dbH.getCredentials() == null){
			ManagerActivity.logout(this, app, megaApi);
			return;
		}	
		
		Preferences prefs = dbH.getPreferences();
		String wifi = "";
		String camSyncLocalPath = "";
		
		if (prefs == null){
			cameraUpload = false;
		}
		else{
			if (prefs.getCamSyncEnabled() == null){
				cameraUpload = false;	
			}
			else{
				cameraUpload = Boolean.parseBoolean(prefs.getCamSyncEnabled());
				camSyncLocalPath = prefs.getCamSyncLocalPath();
				if (Boolean.parseBoolean(prefs.getWifi())){
					wifi = getString(R.string.cam_sync_wifi);
				}
				else{
					wifi = getString(R.string.cam_sync_data);
				}
			}			
		}
		
		addPreferencesFromResource(R.xml.preferences);
		
		storageCategory = (PreferenceCategory) findPreference(CATEGORY_STORAGE);
		cameraUploadCategory = (PreferenceCategory) findPreference(CATEGORY_CAMERA_UPLOAD);	
		pinLockCategory = (PreferenceCategory) findPreference(CATEGORY_PIN_LOCK);
		
		pinLockEnable = findPreference(KEY_PIN_LOCK_ENABLE);
		pinLockEnable.setOnPreferenceClickListener(this);
		
		pinLockCode = findPreference(KEY_PIN_LOCK_CODE);
		
		downloadLocation = findPreference(KEY_STORAGE_DOWNLOAD_LOCATION);
		downloadLocation.setOnPreferenceClickListener(this);
		
		storageAskMeAlways = (CheckBoxPreference) findPreference(KEY_STORAGE_ASK_ME_ALWAYS);
		storageAskMeAlways.setOnPreferenceClickListener(this);
		
		storageAskMeAlways.setChecked(askMe);
		
		cameraUploadOn = findPreference(KEY_CAMERA_UPLOAD_ON);
		cameraUploadOn.setOnPreferenceClickListener(this);
		
		cameraUploadHow = findPreference(KEY_CAMERA_UPLOAD_HOW_TO);
		cameraUploadWhat = findPreference(KEY_CAMERA_UPLOAD_WHAT_TO);
		cameraUploadFolder = findPreference(KEY_CAMERA_UPLOAD_CAMERA_FOLDER);

		if (cameraUpload){
			cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_off));
			cameraUploadHow.setSummary(wifi);
			cameraUploadFolder.setSummary(camSyncLocalPath);
			cameraUploadCategory.addPreference(cameraUploadHow);
			cameraUploadCategory.addPreference(cameraUploadWhat);
			cameraUploadCategory.addPreference(cameraUploadFolder);
		}
		else{
			cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_on));
			cameraUploadHow.setSummary("");
			cameraUploadFolder.setSummary("");
			cameraUploadCategory.removePreference(cameraUploadHow);
			cameraUploadCategory.removePreference(cameraUploadWhat);
			cameraUploadCategory.removePreference(cameraUploadFolder);
		}
		
		if (pinLock){
			pinLockEnable.setTitle(getString(R.string.settings_pin_lock_off));
			pinLockCategory.addPreference(pinLockCode);
		}
		else{
			pinLockEnable.setTitle(getString(R.string.settings_pin_lock_on));
			pinLockCategory.removePreference(pinLockCode);
		}
		
		if (storageAskMeAlways.isChecked()){
			downloadLocation.setEnabled(false);
		}
		else{
			downloadLocation.setEnabled(true);
		}
		
//		cameraUploadHow.setEnabled(false);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().compareTo(KEY_STORAGE_DOWNLOAD_LOCATION) == 0){
			Intent intent = new Intent(SettingsActivity.this, FileStorageActivity.class);
			intent.setAction(FileStorageActivity.Mode.PICK_FOLDER.getAction());
			intent.putExtra(FileStorageActivity.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
			startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
		}
		else if (preference.getKey().compareTo(KEY_CAMERA_UPLOAD_ON) == 0){
			cameraUpload = !cameraUpload;
			if (cameraUpload){
				cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_off));
				cameraUploadCategory.addPreference(cameraUploadHow);
				cameraUploadCategory.addPreference(cameraUploadWhat);
				cameraUploadCategory.addPreference(cameraUploadFolder);
			}
			else{
				cameraUploadOn.setTitle(getString(R.string.settings_camera_upload_on));
				cameraUploadCategory.removePreference(cameraUploadHow);
				cameraUploadCategory.removePreference(cameraUploadWhat);
				cameraUploadCategory.removePreference(cameraUploadFolder);
			}
		}
		else if (preference.getKey().compareTo(KEY_PIN_LOCK_ENABLE) == 0){
			pinLock = !pinLock;
			if (pinLock){
				pinLockEnable.setTitle(getString(R.string.settings_pin_lock_off));
				pinLockCategory.addPreference(pinLockCode);
			}
			else{
				pinLockEnable.setTitle(getString(R.string.settings_pin_lock_on));
				pinLockCategory.removePreference(pinLockCode);
			}
		}
		else if (preference.getKey().compareTo(KEY_STORAGE_ASK_ME_ALWAYS) == 0){
			if (storageAskMeAlways.isChecked()){
				downloadLocation.setEnabled(false);
			}
			else{
				downloadLocation.setEnabled(true);
			}
		}
		return false;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK && intent != null) {
			String path = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
			Toast.makeText(this, "Download to: " + path, Toast.LENGTH_LONG).show();
			setDownloadLocation(path);
		}
	}
	
	public void setDownloadLocation(String path) {
		downloadLocation.setSummary(path);
	}
	
}
