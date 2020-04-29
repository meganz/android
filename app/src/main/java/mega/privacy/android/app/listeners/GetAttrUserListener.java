package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.utils.JobUtil;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.jobservices.CameraUploadsService.*;
import static mega.privacy.android.app.utils.CameraUploadUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static nz.mega.sdk.MegaApiJava.*;

public class GetAttrUserListener extends BaseListener {
    /**
     * Indicates if the request is only to update the DB.
     * If so, the rest of the actions in onRequestFinish() can be ignored.
     */
    private boolean onlyDBUpdate;

    public GetAttrUserListener(Context context) {
        super(context);
    }

    /**
     * Constructor to init a request for check the USER_ATTR_MY_CHAT_FILES_FOLDER user's attribute
     * and update the DB with the result.
     *
     * @param context      current application context
     * @param onlyDBUpdate true if the purpose of the request is only update the DB, false otherwise
     */
    public GetAttrUserListener(Context context, boolean onlyDBUpdate) {
        super(context);

        this.onlyDBUpdate = onlyDBUpdate;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_GET_ATTR_USER) return;

        switch (request.getParamType()) {
            case USER_ATTR_MY_CHAT_FILES_FOLDER:
                MegaNode myChatFolderNode = null;
                boolean myChatFolderFound = false;

                if (e.getErrorCode() == MegaError.API_OK) {
                    myChatFolderNode = api.getNodeByHandle(request.getNodeHandle());
                    if (myChatFolderNode == null) {
                        myChatFolderNode = api.getNodeByPath(CHAT_FOLDER, api.getRootNode());
                    }
                } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                    myChatFolderNode = api.getNodeByPath(CHAT_FOLDER, api.getRootNode());

                    if (myChatFolderNode != null && !api.isInRubbish(myChatFolderNode)) {
                        String name = context.getString(R.string.my_chat_files_folder);

                        if (!myChatFolderNode.getName().equals(name)) {
                            api.renameNode(myChatFolderNode, name, new RenameListener(context, true));
                        }
                        api.setMyChatFilesFolder(myChatFolderNode.getHandle(), new SetAttrUserListener(context));
                    }
                } else {
                    logError("Error getting \"My chat files\" folder: " + e.getErrorString());
                }
                if (myChatFolderNode != null && !api.isInRubbish(myChatFolderNode)) {
                    dBH.setMyChatFilesFolderHandle(myChatFolderNode.getHandle());
                    myChatFolderFound = true;
                } else if (!onlyDBUpdate) {
                    api.createFolder(context.getString(R.string.my_chat_files_folder), api.getRootNode(), new CreateFolderListener(context, true));
                }

                if (onlyDBUpdate) {
                    return;
                }

                if (context instanceof FileExplorerActivityLollipop) {
                    FileExplorerActivityLollipop fileExplorerActivityLollipop = (FileExplorerActivityLollipop) context;
                    if (myChatFolderFound) {
                        fileExplorerActivityLollipop.setMyChatFilesFolder(myChatFolderNode);
                        fileExplorerActivityLollipop.checkIfFilesExistsInMEGA();
                    }
                } else if (context instanceof ChatActivityLollipop) {
                    ChatActivityLollipop chatActivityLollipop = (ChatActivityLollipop) context;
                    if (myChatFolderFound) {
                        chatActivityLollipop.setMyChatFilesFolder(myChatFolderNode);

                        if (chatActivityLollipop.isForwardingFromNC()) {
                            chatActivityLollipop.handleStoredData();
                        } else {
                            chatActivityLollipop.proceedWithAction();
                        }
                    }
                } else if (context instanceof NodeAttachmentHistoryActivity) {
                    NodeAttachmentHistoryActivity nodeAttachmentHistoryActivity = (NodeAttachmentHistoryActivity) context;
                    if (myChatFolderFound) {
                        nodeAttachmentHistoryActivity.setMyChatFilesFolder(myChatFolderNode);
                        nodeAttachmentHistoryActivity.handleStoredData();
                    }
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
                    String nickname = request.getName();
                    if (nickname == null) {
                        updateDBNickname(api, context, request.getMegaStringMap());
                        break;
                    }
                    dBH.setContactNickname(nickname, request.getNodeHandle());
                    notifyNicknameUpdate(context, request.getNodeHandle());
                } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                    dBH.setContactNickname(null, request.getNodeHandle());
                    notifyNicknameUpdate(context, request.getNodeHandle());
                } else {
                    logError("Error recovering the alias" + e.getErrorCode());
                }
                break;

            case USER_ATTR_CAMERA_UPLOADS_FOLDER:
                if (context instanceof CameraUploadsService) {
                    if (request.getFlag()) {
                        ((CameraUploadsService) context).onGetSecondaryFolderAttribute(request, e);
                    } else {
                        ((CameraUploadsService) context).onGetPrimaryFolderAttribute(request, e);
                    }
                } else if (e.getErrorCode() == MegaError.API_OK) {
                    synchronized (this) {
                        MegaPreferences prefs = dBH.getPreferences();
                        long handleInUserAttr = request.getNodeHandle();
                        if (isNodeInRubbishOrDeleted(handleInUserAttr)) {
                            boolean isSecondary = request.getFlag();
                            if (isSecondary) {
                                long secondaryHandle = findDefaultFolder(SECONDARY_UPLOADS);
                                if (secondaryHandle == INVALID_HANDLE) {
                                    secondaryHandle = findDefaultFolder(SECONDARY_UPLOADS_ENGLISH);
                                }
                                if (secondaryHandle == INVALID_HANDLE) {
                                    if (prefs != null &&
                                            prefs.getSecondaryMediaFolderEnabled() != null &&
                                            Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled())) {
                                        api.createFolder(SECONDARY_UPLOADS, api.getRootNode(), new CreateFolderListener(context));
                                    }
                                } else {
                                    api.setCameraUploadsFolderSecondary(secondaryHandle, new SetAttrUserListener(context));
                                    if (!SECONDARY_UPLOADS.equals(SECONDARY_UPLOADS_ENGLISH)) {
                                        api.renameNode(api.getNodeByHandle(secondaryHandle), SECONDARY_UPLOADS, new RenameListener(context));
                                    }
                                }

                            } else {
                                long primaryHandle = findDefaultFolder(CAMERA_UPLOADS);
                                if (primaryHandle == INVALID_HANDLE) {
                                    primaryHandle = findDefaultFolder(CAMERA_UPLOADS_ENGLISH);
                                }
                                if (primaryHandle == INVALID_HANDLE) {
                                    if (prefs != null &&
                                            prefs.getCamSyncEnabled() != null &&
                                            Boolean.parseBoolean(prefs.getCamSyncEnabled())) {
                                        api.createFolder(CAMERA_UPLOADS, api.getRootNode(), new CreateFolderListener(context));
                                    }
                                } else {
                                    api.setCameraUploadsFolder(primaryHandle, new SetAttrUserListener(context));
                                    if (!CAMERA_UPLOADS.equals(CAMERA_UPLOADS_ENGLISH)) {
                                        api.renameNode(api.getNodeByHandle(primaryHandle), CAMERA_UPLOADS, new RenameListener(context));
                                    }
                                }
                            }
                        } else {
                            long primaryHandle = getPrimaryFolderHandle();
                            long secondaryHandle = getSecondaryFolderHandle();
                            boolean shouldCUStop = false;

                            //save changes to local DB
                            boolean isSecondary = request.getFlag();
                            if (isSecondary && handleInUserAttr != secondaryHandle) {
                                dBH.setSecondaryFolderHandle(handleInUserAttr);
                                resetSecondaryTimeline();
                                shouldCUStop = true;

                            } else if (!isSecondary && handleInUserAttr != primaryHandle) {
                                dBH.setCamSyncHandle(handleInUserAttr);
                                resetPrimaryTimeline();
                                shouldCUStop = true;
                            }

                            //stop CU if destination has changed
                            if (shouldCUStop && CameraUploadsService.isServiceRunning) {
                                JobUtil.stopRunningCameraUploadService(context);
                            }

                            //notify manager activity to update UI
                            if (!(context instanceof MegaApplication)) {
                                forceUpdateCameraUploadFolderIcon(isSecondary, handleInUserAttr);
                            }
                        }
                    }
                }
                break;
        }
    }
}
