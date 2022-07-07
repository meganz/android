package mega.privacy.android.app.listeners;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CU_DESTINATION_FOLDER_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FILE_VERSIONS;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RB_SCHEDULER;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_RICH_LINK_SETTING_UPDATE;
import static mega.privacy.android.app.constants.BroadcastConstants.DAYS_COUNT;
import static mega.privacy.android.app.constants.BroadcastConstants.PRIMARY_HANDLE;
import static mega.privacy.android.app.constants.BroadcastConstants.SECONDARY_FOLDER;
import static mega.privacy.android.app.utils.CameraUploadUtil.forceUpdateCameraUploadFolderIcon;
import static mega.privacy.android.app.utils.CameraUploadUtil.resetPrimaryTimeline;
import static mega.privacy.android.app.utils.CameraUploadUtil.resetSecondaryTimeline;
import static mega.privacy.android.app.utils.ContactUtil.notifyNicknameUpdate;
import static mega.privacy.android.app.utils.ContactUtil.updateFirstName;
import static mega.privacy.android.app.utils.ContactUtil.updateLastName;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.showSnackbar;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_ALIAS;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_FIRSTNAME;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_LASTNAME;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_MY_CHAT_FILES_FOLDER;
import static nz.mega.sdk.MegaApiJava.base64ToHandle;

import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.utils.JobUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaStringMap;
import timber.log.Timber;

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
                    Timber.w("Error setting \"My chat files\" folder as user's attribute");
                }
                break;

            case USER_ATTR_FIRSTNAME:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateFirstName(request.getText(), request.getEmail());
                }
                break;

            case USER_ATTR_LASTNAME:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateLastName(request.getText(), request.getEmail());
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
                    Timber.e("Error adding, updating or removing the alias%s", e.getErrorCode());
                }
                break;

            case USER_ATTR_CAMERA_UPLOADS_FOLDER:
                if (e.getErrorCode() == MegaError.API_OK) {
                    MegaPreferences prefs = dBH.getPreferences();
                    // Database and preference update
                    if (prefs == null) return;

                    long primaryHandle = request.getNodeHandle();
                    long secondaryHandle = request.getParentHandle();

                    Timber.d("Set CU folders successfully primary: %d, secondary: %d", primaryHandle, secondaryHandle);
                    if (primaryHandle != INVALID_HANDLE) {
                        resetPrimaryTimeline();
                        dBH.setCamSyncHandle(primaryHandle);
                        prefs.setCamSyncHandle(String.valueOf(primaryHandle));
                        forceUpdateCameraUploadFolderIcon(false, primaryHandle);
                        if (context instanceof CameraUploadsService) {
                            Timber.d("Trigger on onSetFolderAttribute by set primary.");
                            ((CameraUploadsService) context).onSetFolderAttribute();
                        } else {
                            Timber.d("Start CU by set primary, try to start CU, true.");
                            JobUtil.fireStopCameraUploadJob(context);
                            JobUtil.fireCameraUploadJob(context, true);
                        }
                    }
                    if (secondaryHandle != INVALID_HANDLE) {
                        resetSecondaryTimeline();
                        dBH.setSecondaryFolderHandle(secondaryHandle);
                        prefs.setMegaHandleSecondaryFolder(String.valueOf(secondaryHandle));
                        forceUpdateCameraUploadFolderIcon(true, secondaryHandle);
                        //make sure to start the process once secondary is enabled
                        if (context instanceof CameraUploadsService) {
                            Timber.d("Trigger on onSetFolderAttribute by set secondary.");
                            ((CameraUploadsService) context).onSetFolderAttribute();
                        } else {
                            Timber.d("Start CU by set secondary, try to start CU, true.");
                            JobUtil.fireStopCameraUploadJob(context);
                            JobUtil.fireCameraUploadJob(context, true);
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
                    Timber.w("Set CU attributes failed, error code: %d, %s", e.getErrorCode(), e.getErrorString());
                    JobUtil.fireStopCameraUploadJob(context);
                }
                break;

            case MegaApiJava.USER_ATTR_RUBBISH_TIME:
                if (e.getErrorCode() == MegaError.API_OK) {
                    Intent intent = new Intent(ACTION_UPDATE_RB_SCHEDULER);
                    intent.putExtra(DAYS_COUNT, request.getNumber());
                    MegaApplication.getInstance().sendBroadcast(intent);
                } else {
                    Util.showSnackbar(context, context.getString(R.string.error_general_nodes));
                }

                break;

            case MegaApiJava.USER_ATTR_RICH_PREVIEWS:
                if (e.getErrorCode() != MegaError.API_OK) {
                    MegaApplication.getInstance().sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_RICH_LINK_SETTING_UPDATE));
                }
                break;

            case MegaApiJava.USER_ATTR_DISABLE_VERSIONS:
                MegaApplication.setDisableFileVersions(Boolean.parseBoolean(request.getText()));
                if (e.getErrorCode() != MegaError.API_OK) {
                    Timber.e("ERROR:USER_ATTR_DISABLE_VERSIONS");
                    MegaApplication.getInstance().sendBroadcast(new Intent(ACTION_UPDATE_FILE_VERSIONS));
                } else {
                    Timber.d("File versioning attribute changed correctly");
                }
                break;
        }
    }

    /**
     * Updates in DB the handle of "My chat files" folder node if the request
     * for set a node as USER_ATTR_MY_CHAT_FILES_FOLDER finished without errors.
     * <p>
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
