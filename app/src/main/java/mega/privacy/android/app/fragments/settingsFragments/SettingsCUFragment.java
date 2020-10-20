package mega.privacy.android.app.fragments.settingsFragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import java.io.File;

import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.CameraUploadsPreferencesActivity;
import mega.privacy.android.app.components.TwoLineCheckPreference;
import mega.privacy.android.app.jobservices.SyncRecord;
import mega.privacy.android.app.listeners.SetAttrUserListener;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.MegaPreferences.MEDIUM;
import static mega.privacy.android.app.MegaPreferences.ORIGINAL;
import static mega.privacy.android.app.constants.SettingsConstants.*;
import static mega.privacy.android.app.utils.CameraUploadUtil.*;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_UPLOAD;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.JobUtil.rescheduleCameraUpload;
import static mega.privacy.android.app.utils.JobUtil.startCameraUploadService;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.isNodeInRubbishOrDeleted;
import static mega.privacy.android.app.utils.PermissionUtils.hasPermissions;
import static mega.privacy.android.app.utils.SDCardUtils.getSDCardDirName;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.px2dp;
import static mega.privacy.android.app.utils.Util.showKeyboardDelayed;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class SettingsCUFragment  extends SettingsBaseFragment implements Preference.OnPreferenceClickListener {

    private SwitchPreferenceCompat cameraUploadOnOff;

    private ListPreference cameraUploadHow;
    private ListPreference cameraUploadWhat;
    private SwitchPreferenceCompat cameraUploadIncludeGPS;
    private ListPreference videoQuality;
    private SwitchPreferenceCompat cameraUploadCharging;
    private Preference cameraUploadVideoQueueSize;
    private TwoLineCheckPreference keepFileNames;
    private Preference localCameraUploadFolder;
    private Preference megaCameraFolder;
    private Preference secondaryMediaFolderOn;
    private Preference localSecondaryFolder;
    private Preference megaSecondaryFolder;

    boolean cameraUpload = false;
    boolean secondaryUpload = false;
    boolean charging = false;
    boolean includeGPS;
    boolean fileNames = false;
    Handler handler = new Handler();

    String wifi = "";
    String camSyncLocalPath = "";
    private boolean isExternalSDCardCU;
    Long camSyncHandle = null;
    MegaNode camSyncMegaNode = null;
    String camSyncMegaPath = "";
    String fileUpload = "";
    AlertDialog compressionQueueSizeDialog;
    private EditText queueSizeInput;

    //Secondary Folder
    String localSecondaryFolderPath = "";
    Long handleSecondaryMediaFolder = null;
    MegaNode megaNodeSecondaryMediaFolder = null;
    String megaPathSecMediaFolder = "";
    private boolean isExternalSDCardMU;
    private SetAttrUserListener setAttrUserListener;


    public SettingsCUFragment () {
        super();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_cu);

        cameraUploadOnOff = findPreference(KEY_CAMERA_UPLOAD_ON_OFF);
        cameraUploadOnOff.setOnPreferenceClickListener(this);

        cameraUploadHow = findPreference(KEY_CAMERA_UPLOAD_HOW_TO);
        cameraUploadHow.setOnPreferenceChangeListener(this);

        cameraUploadWhat = findPreference(KEY_CAMERA_UPLOAD_WHAT_TO);
        cameraUploadWhat.setOnPreferenceChangeListener(this);

        cameraUploadIncludeGPS = findPreference(KEY_CAMERA_UPLOAD_INCLUDE_GPS);
        cameraUploadIncludeGPS.setOnPreferenceClickListener(this);

        videoQuality = findPreference(KEY_CAMERA_UPLOAD_VIDEO_QUALITY);
        videoQuality.setOnPreferenceChangeListener(this);

        cameraUploadCharging = findPreference(KEY_CAMERA_UPLOAD_CHARGING);
        cameraUploadCharging.setOnPreferenceClickListener(this);

        cameraUploadVideoQueueSize = findPreference(KEY_CAMERA_UPLOAD_VIDEO_QUEUE_SIZE);
        cameraUploadVideoQueueSize.setOnPreferenceClickListener(this);

        keepFileNames = findPreference(KEY_KEEP_FILE_NAMES);
        keepFileNames.setOnPreferenceClickListener(this);

        localCameraUploadFolder = findPreference(KEY_CAMERA_UPLOAD_CAMERA_FOLDER);
        localCameraUploadFolder.setOnPreferenceClickListener(this);
        megaCameraFolder = findPreference(KEY_CAMERA_UPLOAD_MEGA_FOLDER);
        megaCameraFolder.setOnPreferenceClickListener(this);

        secondaryMediaFolderOn = findPreference(KEY_SECONDARY_MEDIA_FOLDER_ON);
        secondaryMediaFolderOn.setOnPreferenceClickListener(this);
        localSecondaryFolder= findPreference(KEY_LOCAL_SECONDARY_MEDIA_FOLDER);
        localSecondaryFolder.setOnPreferenceClickListener(this);
        megaSecondaryFolder = findPreference(KEY_MEGA_SECONDARY_MEDIA_FOLDER);
        megaSecondaryFolder.setOnPreferenceClickListener(this);

        if (prefs == null){
            dbH.setFirstTime(false);
            dbH.setCamSyncEnabled(false);
            dbH.setSecondaryUploadEnabled(false);
            cameraUpload = false;
            charging = true;
            fileNames = false;
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
                    isExternalSDCardCU = Boolean.parseBoolean(prefs.getCameraFolderExternalSDCard());
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
                if ((isTextEmpty(camSyncLocalPath) || (!isExternalSDCardCU &&!isFileAvailable(new File(camSyncLocalPath))))
                        && Environment.getExternalStorageDirectory() != null){
                    File cameraDownloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    dbH.setCamSyncLocalPath(cameraDownloadLocation.getAbsolutePath());
                    dbH.setCameraFolderExternalSDCard(false);
                    camSyncLocalPath = cameraDownloadLocation.getAbsolutePath();
                } else if (isExternalSDCardCU) {
                    Uri uri = Uri.parse(prefs.getUriExternalSDCard());
                    String pickedDirName = getSDCardDirName(uri);
                    if (pickedDirName!= null) {
                        camSyncLocalPath = pickedDirName;
                        localCameraUploadFolder.setSummary(pickedDirName);
                    } else {
                        logWarning("pickedDirNAme NULL");
                    }
                }

                //Check if the secondary sync is enabled
                if (prefs.getSecondaryMediaFolderEnabled() == null) {
                    dbH.setSecondaryUploadEnabled(false);
                    secondaryUpload = false;
                } else {
                    secondaryUpload = Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled());
                    logDebug("Secondary is: " + secondaryUpload);
                }

                isExternalSDCardMU = dbH.getMediaFolderExternalSdCard();
            }
        }

        if (cameraUpload){
            cameraUploadOnOff.setChecked(true);

            cameraUploadHow.setSummary(wifi);
            localCameraUploadFolder.setSummary(camSyncLocalPath);
            megaCameraFolder.setSummary(camSyncMegaPath);
            megaSecondaryFolder.setSummary(megaPathSecMediaFolder);
            cameraUploadWhat.setSummary(fileUpload);
            cameraUploadCharging.setChecked(charging);
            keepFileNames.setChecked(fileNames);
            getPreferenceScreen().addPreference(cameraUploadHow);
            getPreferenceScreen().addPreference(cameraUploadWhat);
            if(!charging){
                disableVideoCompressionSizeSettings();
            }
            getPreferenceScreen().addPreference(keepFileNames);
            getPreferenceScreen().addPreference(localCameraUploadFolder);

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
                checkMediaUploadsPath();

                megaSecondaryFolder.setSummary(megaPathSecMediaFolder);
                localSecondaryFolder.setSummary(localSecondaryFolderPath);
                secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_off));
                getPreferenceScreen().addPreference(localSecondaryFolder);
                getPreferenceScreen().addPreference(megaSecondaryFolder);

            }
            else{
                secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_on));
                getPreferenceScreen().removePreference(localSecondaryFolder);
                getPreferenceScreen().removePreference(megaSecondaryFolder);
            }
        }
        else{
            cameraUploadOnOff.setChecked(false);

            cameraUploadHow.setSummary("");
            localCameraUploadFolder.setSummary("");
            megaCameraFolder.setSummary("");
            localSecondaryFolder.setSummary("");
            megaSecondaryFolder.setSummary("");
            cameraUploadWhat.setSummary("");
            getPreferenceScreen().removePreference(localCameraUploadFolder);
            hideVideoQualitySettingsSection();
            removeRemoveGPS();
            getPreferenceScreen().removePreference(keepFileNames);
            getPreferenceScreen().removePreference(megaCameraFolder);
            getPreferenceScreen().removePreference(cameraUploadHow);
            getPreferenceScreen().removePreference(cameraUploadWhat);

            //Remove Secondary Folder
            getPreferenceScreen().removePreference(secondaryMediaFolderOn);
            getPreferenceScreen().removePreference(localSecondaryFolder);
            getPreferenceScreen().removePreference(megaSecondaryFolder);
        }

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

    private boolean isOffline(){
        if (!isOnline(context)){
            Util.showSnackbar(context, getString(R.string.error_server_connection_problem));
            return true;
        }
        return false;
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
                ActivityCompat.requestPermissions((CameraUploadsPreferencesActivity)context, PERMISSIONS, REQUEST_CAMERA_UPLOAD);
            } else {
                ((CameraUploadsPreferencesActivity) context).checkIfShouldShowBusinessCUAlert(false);
            }
        } else {
            disableCameraUpload();
        }
    }

    /**
     * This method is to do the setting process
     * and UI related process
     */
    public void disableCameraUpload(){
        disableCameraUploadSettingProcess();
        disableCameraUploadUIProcess();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }
        Intent intent;
        switch (preference.getKey()) {
            case KEY_CAMERA_UPLOAD_ON_OFF:
                logDebug("Changing camera upload");
                if(cameraUpload && isOffline()){
                    return false;
                }

                dbH.setCamSyncTimeStamp(0);
                cameraUpload = !cameraUpload;
                refreshCameraUploadsSettings();
                break;

            case KEY_CAMERA_UPLOAD_INCLUDE_GPS:
                logDebug("KEY_CAMERA_UPLOAD_INCLUDE_GPS");
                includeGPS = cameraUploadIncludeGPS.isChecked();
                dbH.setRemoveGPS(!includeGPS);
                rescheduleCameraUpload(context);
                break;

            case KEY_CAMERA_UPLOAD_CHARGING:
                logDebug("KEY_CAMERA_UPLOAD_CHARGING");
                charging = cameraUploadCharging.isChecked();
                if(charging){
                    enableVideoCompressionSizeSettingsAndRestartUpload();
                }else{
                    disableVideoCompressionSizeSettingsAndRestartUpload();
                }
                dbH.setConversionOnCharging(charging);
                break;

            case KEY_CAMERA_UPLOAD_VIDEO_QUEUE_SIZE:
                showResetCompressionQueueSizeDialog();
                break;

            case KEY_KEEP_FILE_NAMES:
                logDebug("KEY_KEEP_FILE_NAMES");
                fileNames = keepFileNames.isChecked();
                dbH.setKeepFileNames(fileNames);
                Toast.makeText(context, getString(R.string.message_keep_device_name), Toast.LENGTH_SHORT).show();
                break;

            case KEY_CAMERA_UPLOAD_CAMERA_FOLDER:
                logDebug("Changing the LOCAL folder for camera uploads");
                intent = new Intent(context, FileStorageActivityLollipop.class);
                intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
                intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
                intent.putExtra(FileStorageActivityLollipop.PICK_FOLDER_TYPE, FileStorageActivityLollipop.PickFolderType.CU_FOLDER.getFolderType());
                startActivityForResult(intent, REQUEST_CAMERA_FOLDER);
                break;

            case KEY_CAMERA_UPLOAD_MEGA_FOLDER:
                logDebug("Changing the MEGA folder for camera uploads");
                if (isOffline())
                    return false;

                intent = new Intent(context, FileExplorerActivityLollipop.class);
                intent.setAction(FileExplorerActivityLollipop.ACTION_CHOOSE_MEGA_FOLDER_SYNC);
                startActivityForResult(intent, REQUEST_MEGA_CAMERA_FOLDER);
                break;

            case KEY_SECONDARY_MEDIA_FOLDER_ON:
                logDebug("Changing the secondary uploads");
                if (isOffline())
                    return false;

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
                    checkMediaUploadsPath();

                    localSecondaryFolder.setSummary(localSecondaryFolderPath);
                    getPreferenceScreen().addPreference(localSecondaryFolder);
                    getPreferenceScreen().addPreference(megaSecondaryFolder);
                }
                else{
                    resetMUTimestampsAndCache();
                    dbH.setSecondaryUploadEnabled(false);
                    secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_on));
                    getPreferenceScreen().removePreference(localSecondaryFolder);
                    getPreferenceScreen().removePreference(megaSecondaryFolder);
                }
                rescheduleCameraUpload(context);
                break;

            case KEY_LOCAL_SECONDARY_MEDIA_FOLDER:
                logDebug("Changing the local folder for secondary uploads");
                intent = new Intent(context, FileStorageActivityLollipop.class);
                intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
                intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
                intent.putExtra(FileStorageActivityLollipop.PICK_FOLDER_TYPE, FileStorageActivityLollipop.PickFolderType.MU_FOLDER.getFolderType());
                startActivityForResult(intent, REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER);
                break;

            case KEY_MEGA_SECONDARY_MEDIA_FOLDER:
                logDebug("Changing the MEGA folder for secondary uploads");
                if (isOffline())
                    return false;

                intent = new Intent(context, FileExplorerActivityLollipop.class);
                intent.setAction(FileExplorerActivityLollipop.ACTION_CHOOSE_MEGA_FOLDER_SYNC);
                startActivityForResult(intent, REQUEST_MEGA_SECONDARY_MEDIA_FOLDER);
                break;
        }

        return true;
    }

    /**
     * Checks the Media Uploads local path.
     */
    private void checkMediaUploadsPath() {
        localSecondaryFolderPath = prefs.getLocalPathSecondaryFolder();

        if (isTextEmpty(localSecondaryFolderPath) || (!isExternalSDCardMU && !isFileAvailable(new File(localSecondaryFolderPath)))) {
            logWarning("Secondary ON: invalid localSecondaryFolderPath");
            localSecondaryFolderPath = getString(R.string.settings_empty_folder);
            Toast.makeText(context, getString(R.string.secondary_media_service_error_local_folder), Toast.LENGTH_SHORT).show();
            if (!isFileAvailable(new File(localSecondaryFolderPath))) {
                dbH.setSecondaryFolderPath(INVALID_PATH);
            }
        } else if (isExternalSDCardMU) {
            Uri uri = Uri.parse(dbH.getUriMediaExternalSdCard());
            String pickedDirName = getSDCardDirName(uri);
            if (pickedDirName!= null) {
                localSecondaryFolderPath = pickedDirName;
            }
        }
    }


    private void setupRemoveGPS() {
        String removeGPSString = prefs.getRemoveGPS();
        if(TextUtils.isEmpty(removeGPSString)) {
            includeGPS = false;
            dbH.setRemoveGPS(true);
        } else {
            includeGPS = !Boolean.parseBoolean(removeGPSString);
        }
        cameraUploadIncludeGPS.setChecked(includeGPS);
        cameraUploadIncludeGPS.setSummary(getResources().getString(R.string.settings_camera_upload_include_gps_helper_label));
        getPreferenceScreen().addPreference(cameraUploadIncludeGPS);
    }

    private void removeRemoveGPS() {
        cameraUploadIncludeGPS.setSummary("");
        getPreferenceScreen().removePreference(cameraUploadIncludeGPS);
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
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
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

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.settings_video_compression_queue_size_popup_title));
        builder.setPositiveButton(getString(R.string.general_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) { }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), null);
        builder.setView(layout);
        compressionQueueSizeDialog = builder.create();
        compressionQueueSizeDialog.show();

        compressionQueueSizeDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = queueSizeInput.getText().toString().trim();
                setCompressionQueueSize(value, queueSizeInput);
            }
        });
    }


    private void hideVideoQualitySettingsSection(){
        getPreferenceScreen().removePreference(videoQuality);
        getPreferenceScreen().removePreference(cameraUploadCharging);
        getPreferenceScreen().removePreference(cameraUploadVideoQueueSize);
    }

    private void disableVideoQualitySettings(){
        prefs.setUploadVideoQuality(String.valueOf(VIDEO_QUALITY_MEDIUM));
        dbH.setCameraUploadVideoQuality(VIDEO_QUALITY_MEDIUM);
        getPreferenceScreen().removePreference(videoQuality);
        disableChargingSettings();
    }

    private void disableChargingSettings(){
        charging = false;
        dbH.setConversionOnCharging(charging);
        cameraUploadCharging.setChecked(charging);
        getPreferenceScreen().removePreference(cameraUploadCharging);
        disableVideoCompressionSizeSettings();
    }

    private void disableVideoCompressionSizeSettings(){
        getPreferenceScreen().removePreference(cameraUploadVideoQueueSize);
    }

    private void disableVideoCompressionSizeSettingsAndRestartUpload(){
        disableVideoCompressionSizeSettings();
        rescheduleCameraUpload(context);
    }

    private void enableVideoCompressionSizeSettingsAndRestartUpload(){
        enableVideoCompressionSizeSettings();
        rescheduleCameraUpload(context);
    }

    private void enableVideoCompressionSizeSettings() {
        prefs = dbH.getPreferences();
        getPreferenceScreen().addPreference(cameraUploadVideoQueueSize);

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

    private void resetVideoQualitySettings(){
        dbH.setCameraUploadVideoQuality(VIDEO_QUALITY_MEDIUM);
        dbH.setConversionOnCharging(true);
        dbH.setChargingOnSize(DEFAULT_CONVENTION_QUEUE_SIZE);
        String chargingHelper = getResources().getString(R.string.settings_camera_upload_charging_helper_label,
                getResources().getString(R.string.label_file_size_mega_byte, String.valueOf(DEFAULT_CONVENTION_QUEUE_SIZE)));
        cameraUploadCharging.setSummary(chargingHelper);
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
        getPreferenceScreen().addPreference(cameraUploadCharging);

        if(charging){
            enableVideoCompressionSizeSettings();
        }
    }

    private void enableVideoQualitySettings() {
        prefs = dbH.getPreferences();
        getPreferenceScreen().addPreference(videoQuality);
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        prefs = dbH.getPreferences();

        int value = Integer.parseInt((String)newValue);
        switch (preference.getKey()){
            case KEY_CAMERA_UPLOAD_HOW_TO:
                switch (value){
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
                break;

            case KEY_CAMERA_UPLOAD_WHAT_TO:
                switch(value){
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
                break;

            case KEY_CAMERA_UPLOAD_VIDEO_QUALITY:
                switch(value){
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
                break;
        }
        return true;
    }

    public void setOnlineOptions(boolean isOnline){
        cameraUploadOnOff.setEnabled(isOnline);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        final ListView lv = v.findViewById(android.R.id.list);
        if(lv != null) {
            lv.setPadding(0, 0, 0, 0);
        }

        setOnlineOptions(isOnline(context) && megaApi != null && megaApi.getRootNode() != null);
        setAttrUserListener = new SetAttrUserListener(context);
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(compressionQueueSizeDialog != null && compressionQueueSizeDialog.isShowing()){
            outState.putBoolean(KEY_SET_QUEUE_DIALOG, true);
            outState.putString(KEY_SET_QUEUE_SIZE, queueSizeInput.getText().toString());
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

        cameraUploadOnOff.setChecked(true);
        getPreferenceScreen().addPreference(cameraUploadHow);
        getPreferenceScreen().addPreference(cameraUploadWhat);
        getPreferenceScreen().addPreference(keepFileNames);
        getPreferenceScreen().addPreference(megaCameraFolder);
        getPreferenceScreen().addPreference(secondaryMediaFolderOn);
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

    /**
     * Disable MediaUpload UI related process
     */
    public void disableMediaUploadUIProcess() {
        logDebug("changes to sec folder only");
        secondaryUpload = false;
        secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_on));
        getPreferenceScreen().removePreference(localSecondaryFolder);
        getPreferenceScreen().removePreference(megaSecondaryFolder);
    }

    /**
     * Disable CameraUpload UI related process
     */
    public void disableCameraUploadUIProcess() {
        logDebug("Camera Uploads OFF");
        cameraUpload = false;
        cameraUploadOnOff.setChecked(false);
        getPreferenceScreen().removePreference(cameraUploadHow);
        getPreferenceScreen().removePreference(cameraUploadWhat);
        getPreferenceScreen().removePreference(localCameraUploadFolder);
        getPreferenceScreen().removePreference(cameraUploadIncludeGPS);
        hideVideoQualitySettingsSection();
        getPreferenceScreen().removePreference(keepFileNames);
        getPreferenceScreen().removePreference(megaCameraFolder);
        getPreferenceScreen().removePreference(secondaryMediaFolderOn);
        disableMediaUploadUIProcess();
    }

    private String getLocalDCIMFolderPath(){
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
    }

    private void setupLocalPathForCameraUpload(){
        String cameraFolderLocation = prefs.getCamSyncLocalPath();
        if(TextUtils.isEmpty(cameraFolderLocation)) {
            cameraFolderLocation = getLocalDCIMFolderPath();
        }
        if (camSyncLocalPath != null) {
            if (!isExternalSDCardCU) {
                File checkFile = new File(camSyncLocalPath);
                if (!checkFile.exists()) {
                    logWarning("Local path not exist, use default camera folder path");
                    camSyncLocalPath = cameraFolderLocation;
                }
            } else {
                Uri uri = Uri.parse(prefs.getUriExternalSDCard());
                String pickedDirName = getSDCardDirName(uri);
                if (pickedDirName != null) {
                    camSyncLocalPath = pickedDirName;
                } else {
                    logError("pickedDirName is NULL");
                }
            }
        } else {
            logError("Local path is NULL");
            dbH.setCameraFolderExternalSDCard(false);
            isExternalSDCardCU = false;
            camSyncLocalPath = cameraFolderLocation;
        }

        dbH.setCamSyncLocalPath(cameraFolderLocation);
        localCameraUploadFolder.setSummary(camSyncLocalPath);
        getPreferenceScreen().addPreference(localCameraUploadFolder);
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
            checkMediaUploadsPath();

            megaSecondaryFolder.setSummary(megaPathSecMediaFolder);
            localSecondaryFolder.setSummary(localSecondaryFolderPath);
            secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_off));
            getPreferenceScreen().addPreference(localSecondaryFolder);
            getPreferenceScreen().addPreference(megaSecondaryFolder);

        } else {
            secondaryMediaFolderOn.setTitle(getString(R.string.settings_secondary_upload_on));
            getPreferenceScreen().removePreference(localSecondaryFolder);
            getPreferenceScreen().removePreference(megaSecondaryFolder);
        }
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
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        prefs = dbH.getPreferences();
        logDebug("REQUEST CODE: " + requestCode + "___RESULT CODE: " + resultCode);
        if (requestCode == REQUEST_CAMERA_FOLDER && resultCode == Activity.RESULT_OK && intent != null){
            //Local folder to sync
            String cameraPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if(!isNewSettingValid(cameraPath, prefs.getLocalPathSecondaryFolder(), prefs.getCamSyncHandle(), prefs.getMegaHandleSecondaryFolder())){
                Toast.makeText(context, getString(R.string.error_invalid_folder_selected), Toast.LENGTH_LONG).show();
                return;
            }

            isExternalSDCardCU = Boolean.parseBoolean(prefs.getCameraFolderExternalSDCard());
            camSyncLocalPath = isExternalSDCardCU ? getSDCardDirName(Uri.parse(prefs.getUriExternalSDCard())) : cameraPath;
            prefs.setCamSyncLocalPath(camSyncLocalPath);
            dbH.setCamSyncLocalPath(camSyncLocalPath);
            localCameraUploadFolder.setSummary(camSyncLocalPath);
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

            isExternalSDCardMU = dbH.getMediaFolderExternalSdCard();
            localSecondaryFolderPath = isExternalSDCardMU ? getSDCardDirName(Uri.parse(dbH.getUriMediaExternalSdCard())) : secondaryPath;
            dbH.setSecondaryFolderPath(localSecondaryFolderPath);
            prefs.setLocalPathSecondaryFolder(localSecondaryFolderPath);
            localSecondaryFolder.setSummary(localSecondaryFolderPath);
            dbH.setSecSyncTimeStamp(0);
            dbH.setSecVideoSyncTimeStamp(0);
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
}

