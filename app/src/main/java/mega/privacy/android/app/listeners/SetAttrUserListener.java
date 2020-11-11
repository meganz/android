package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.utils.JobUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaStringMap;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.PRIMARY_HANDLE;
import static mega.privacy.android.app.constants.BroadcastConstants.SECONDARY_FOLDER;
import static mega.privacy.android.app.utils.CameraUploadUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static nz.mega.sdk.MegaApiJava.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.*;

public class SetAttrUserListener extends BaseListener {

    public SetAttrUserListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_SET_ATTR_USER) return;

        switch (request.getParamType()) {
            case USER_ATTR_MY_CHAT_FILES_FOLDER:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateMyChatFilesFolderHandle(request.getMegaStringMap());
                } else {
                    logWarning("Error setting \"My chat files\" folder as user's attribute");
                }
                break;
            case USER_ATTR_FIRSTNAME:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateFirstName(context, request.getText(), request.getEmail());
                }
                break;
            case USER_ATTR_LASTNAME:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateLastName(context, request.getText(), request.getEmail());
                }
                break;
            case USER_ATTR_ALIAS:
                if (e.getErrorCode() == MegaError.API_OK) {
                    String nickname = request.getText();
                    dBH.setContactNickname(nickname, request.getNodeHandle());
                    String message;
                    if (request.getText() == null) {
                        message = context.getString(R.string.snackbar_nickname_removed);
                    } else {
                        message = context.getString(R.string.snackbar_nickname_added);
                    }
                    showSnackbar(context, message);
                    notifyNicknameUpdate(context, request.getNodeHandle());
                } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                    dBH.setContactNickname(null, request.getNodeHandle());
                    notifyNicknameUpdate(context, request.getNodeHandle());
                } else {
                    logError("Error adding, updating or removing the alias" + e.getErrorCode());
                }
                break;

            case USER_ATTR_CAMERA_UPLOADS_FOLDER:
                if (e.getErrorCode() == MegaError.API_OK) {
                    MegaPreferences prefs = dBH.getPreferences();
                    // Database and preference update
                    if (prefs == null) return;

                    long primaryHandle = request.getNodeHandle();
                    long secondaryHandle = request.getParentHandle();
                    if(primaryHandle != INVALID_HANDLE){
                        resetPrimaryTimeline();
                        dBH.setCamSyncHandle(primaryHandle);
                        prefs.setCamSyncHandle(String.valueOf(primaryHandle));
                        forceUpdateCameraUploadFolderIcon(false, primaryHandle);
                        if (context instanceof CameraUploadsService) {
                            ((CameraUploadsService) context).onSetFolderAttribute();
                        } else {
                            JobUtil.stopRunningCameraUploadService(context);
                            JobUtil.startCameraUploadServiceIgnoreAttr(context);
                        }
                    }
                    if (secondaryHandle != INVALID_HANDLE) {
                        resetSecondaryTimeline();
                        dBH.setSecondaryFolderHandle(secondaryHandle);
                        prefs.setMegaHandleSecondaryFolder(String.valueOf(secondaryHandle));
                        forceUpdateCameraUploadFolderIcon(true, secondaryHandle);
                        //make sure to start the process once secondary is enabled
                        if (context instanceof CameraUploadsService) {
                            ((CameraUploadsService) context).onSetFolderAttribute();
                        } else {
                            JobUtil.stopRunningCameraUploadService(context);
                            JobUtil.startCameraUploadServiceIgnoreAttr(context);
                        }
                    }

                    Intent intent = new Intent(ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING);
                    if (primaryHandle != INVALID_HANDLE) {
                        intent.putExtra(SECONDARY_FOLDER, false);
                        intent.putExtra(PRIMARY_HANDLE, primaryHandle);
                    }
                    if (secondaryHandle != INVALID_HANDLE) {
                        intent.putExtra(SECONDARY_FOLDER, true);
                        intent.putExtra(PRIMARY_HANDLE, secondaryHandle);
                    }
                    MegaApplication.getInstance().sendBroadcast(intent);
                } else {
                    logWarning("Set CU attributes failed, error code: " + e.getErrorCode() + ", " + e.getErrorString());
                    JobUtil.stopRunningCameraUploadService(context);
                }
                break;

            case MegaApiJava.USER_ATTR_RUBBISH_TIME:
                if (context instanceof FileManagementPreferencesActivity) {
                    if (e.getErrorCode() == MegaError.API_OK) {
                        ((FileManagementPreferencesActivity) context).updateRBScheduler(request.getNumber());
                    } else {
                        Util.showSnackbar(context, context.getString(R.string.error_general_nodes));
                    }
                }
                break;

            case MegaApiJava.USER_ATTR_RICH_PREVIEWS:
                if (context instanceof ChatPreferencesActivity && e.getErrorCode() != MegaError.API_OK) {
                    ((ChatPreferencesActivity) context).needUpdateRichLinks();
                }
                break;

            case MegaApiJava.USER_ATTR_DISABLE_VERSIONS:
                if (context instanceof FileManagementPreferencesActivity) {
                    MegaApplication.setDisableFileVersions(Boolean.parseBoolean(request.getText()));

                    if (e.getErrorCode() != MegaError.API_OK) {
                        logError("ERROR:USER_ATTR_DISABLE_VERSIONS");
                        ((FileManagementPreferencesActivity) context).updateEnabledFileVersions();
                    } else {
                        logDebug("File versioning attribute changed correctly");
                    }
                }
                break;
        }
    }

    /**
     * Updates in DB the handle of "My chat files" folder node if the request
     * for set a node as USER_ATTR_MY_CHAT_FILES_FOLDER finished without errors.
     *
     * Before update the DB, it has to obtain the handle contained in a MegaStringMap,
     * where one of the entries will contain a key "h" and its value, the handle in base64.
     *
     * @param map MegaStringMap which contains the handle of the node set as USER_ATTR_MY_CHAT_FILES_FOLDER.
     */
    private void updateMyChatFilesFolderHandle(MegaStringMap map) {
        if (map != null && map.size() > 0 && !isTextEmpty(map.get("h"))) {
            long handle = base64ToHandle(map.get("h"));
            if (handle != INVALID_HANDLE) {
                MegaApplication.getInstance().getDbH().setMyChatFilesFolderHandle(handle);
            }
        }
    }
}
