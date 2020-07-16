package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.AuthenticityCredentialsActivity;
import mega.privacy.android.app.MegaApplication;
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
import nz.mega.sdk.MegaStringMap;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.listeners.CreateFolderListener.ExtraAction.*;
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

    /**
     * When calling {@link MegaApiJava#getUserAttribute(int)}, the MegaRequest object won't store
     * node handle, so we can't get user handle from {@code request.getNodeHandle()}, we should
     * use {@code api.getContact(request.getEmail())} to get MegaUser, then get user handle from it.
     */
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
                    api.createFolder(context.getString(R.string.my_chat_files_folder), api.getRootNode(), new CreateFolderListener(context, MY_CHAT_FILES));
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
                    MegaUser user = api.getContact(request.getEmail());
                    if (user != null) {
                        notifyFirstNameUpdate(context, user.getHandle());
                    }
                }
                break;

            case USER_ATTR_LASTNAME:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateLastName(context, request.getText(), request.getEmail());
                    MegaUser user = api.getContact(request.getEmail());
                    if (user != null) {
                        notifyLastNameUpdate(context, user.getHandle());
                    }
                }
                break;

            case USER_ATTR_ALIAS:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateDBNickname(api, context, request.getMegaStringMap());
                } else {
                    logError("Error recovering the alias" + e.getErrorCode());
                }
                break;

            case USER_ATTR_CAMERA_UPLOADS_FOLDER:
                if (e.getErrorCode() == MegaError.API_OK) {
                    long[] handles = getCUHandles(request);
                    synchronized (this) {
                        handle(handles[0], false, e);
                        handle(handles[1], true, e);
                    }
                } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                    // only when both CU and MU are not set, will return API_ENOENT
                    initCUFolderFromScratch(context, false);
                    if (context instanceof CameraUploadsService) {
                        // The unique process run within shoudRun method in CameraUploadsService
                        ((CameraUploadsService) context).onGetPrimaryFolderAttribute(INVALID_HANDLE, e, true);
                    }
                } else {
                    logError("Error getting USER_ATTR_CAMERA_UPLOADS_FOLDER " + e.getErrorString());
                }
                break;

            case USER_ATTR_ED25519_PUBLIC_KEY:
                if (context instanceof AuthenticityCredentialsActivity) {
                    ((AuthenticityCredentialsActivity) context).setContactCredentials(request, e);
                }
                break;
        }
    }

    /**
     * Get CU and MU folders handle from MegaRequest object.
     *
     * @param request MegaRequest object which contains CU and MU folders handle.
     * @return An array with CU folder handle at the first element, and MU folder handle at the second element.
     */
    private long[] getCUHandles(MegaRequest request) {
        long primaryHandle = INVALID_HANDLE, secondaryHandle = INVALID_HANDLE;
        MegaStringMap map = request.getMegaStringMap();
        if (map != null) {
            String h = map.get("h");
            if (h != null) {
                primaryHandle = MegaApiJava.base64ToHandle(h);
            }
            String sh = map.get("sh");
            if (sh != null) {
                secondaryHandle = MegaApiJava.base64ToHandle(sh);
            }
        } else {
            logError("MegaStringMap is null.");
        }
        return new long[]{primaryHandle, secondaryHandle};
    }

    /**
     * Process CU or MU folder handle after get them from CU attributes.
     *
     * @param handle Folder handle.
     * @param isSecondary Is the handle CU handle or MU handle.
     * @param e MegaError object.
     */
    private void handle(long handle, boolean isSecondary, MegaError e) {
        if (isNodeInRubbishOrDeleted(handle)) {
            initCUFolderFromScratch(context, isSecondary);
        } else {
            boolean shouldCUStop = compareAndUpdateLocalFolderAttribute(handle, isSecondary);
            //stop CU if destination has changed
            if (shouldCUStop && CameraUploadsService.isServiceRunning) {
                JobUtil.stopRunningCameraUploadService(context);
            }

            //notify manager activity to update UI
            if (!(context instanceof MegaApplication)) {
                forceUpdateCameraUploadFolderIcon(isSecondary, handle);
            }
        }
        if (context instanceof CameraUploadsService) {
            // The unique process run within shoudRun method in CameraUploadsService
            if (isSecondary) {
                ((CameraUploadsService) context).onGetSecondaryFolderAttribute(handle, e);
            } else {
                ((CameraUploadsService) context).onGetPrimaryFolderAttribute(handle, e, false);
            }
        }
    }
}
