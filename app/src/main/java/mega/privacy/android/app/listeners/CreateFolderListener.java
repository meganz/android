package mega.privacy.android.app.listeners;

import android.content.Context;

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

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class CreateFolderListener extends BaseListener {

    public enum ExtraAction {
        NONE,
        MY_CHAT_FILES,
        INIT_CU
    }

    private ExtraAction extraAction;

    public CreateFolderListener(Context context) {
        super(context);
        this.extraAction = ExtraAction.NONE;
    }

    public CreateFolderListener(Context context, ExtraAction extraAction) {
        super(context);
        this.extraAction = extraAction;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_CREATE_FOLDER) return;

        long handle = request.getNodeHandle();
        MegaNode node = api.getNodeByHandle(handle);
        String name = request.getName();

        if (extraAction == ExtraAction.INIT_CU) {
            if (name.equals(context.getString(R.string.section_photo_sync))) {
                CameraUploadsService.isCreatingPrimary = false;
                //set primary only
                if (e.getErrorCode() == MegaError.API_OK) {
                    api.setCameraUploadsFolders(handle, MegaApiJava.INVALID_HANDLE, new SetAttrUserListener(context));
                } else {
                    JobUtil.stopRunningCameraUploadService(context);
                }
            } else if (name.equals(context.getString(R.string.section_secondary_media_uploads))) {
                CameraUploadsService.isCreatingSecondary = false;
                //set secondary only
                if (e.getErrorCode() == MegaError.API_OK) {
                    api.setCameraUploadsFolders(MegaApiJava.INVALID_HANDLE, handle, new SetAttrUserListener(context));
                } else {
                    JobUtil.stopRunningCameraUploadService(context);
                }
            }
        }

        if (context instanceof FileExplorerActivityLollipop) {
            FileExplorerActivityLollipop fileExplorerActivityLollipop = (FileExplorerActivityLollipop) context;

            if (e.getErrorCode() == MegaError.API_OK) {
                if (extraAction == ExtraAction.MY_CHAT_FILES) {
                    fileExplorerActivityLollipop.setMyChatFilesFolder(node);
                    api.setMyChatFilesFolder(handle, new SetAttrUserListener(fileExplorerActivityLollipop));
                    fileExplorerActivityLollipop.checkIfFilesExistsInMEGA();
                } else {
                    fileExplorerActivityLollipop.finishCreateFolder(true, handle);
                }
            } else {
                if (extraAction == ExtraAction.MY_CHAT_FILES) {
                    fileExplorerActivityLollipop.showSnackbar(context.getString(R.string.general_text_error));
                } else {
                    fileExplorerActivityLollipop.finishCreateFolder(false, handle);
                    fileExplorerActivityLollipop.showSnackbar(context.getString(R.string.error_creating_folder, name));
                }
            }
        } else if (context instanceof ChatActivityLollipop) {
            ChatActivityLollipop chatActivityLollipop = (ChatActivityLollipop) context;

            if (e.getErrorCode() == MegaError.API_OK) {
                api.setMyChatFilesFolder(handle, new SetAttrUserListener(chatActivityLollipop));
                chatActivityLollipop.setMyChatFilesFolder(node);
                if (chatActivityLollipop.isForwardingFromNC()) {
                    chatActivityLollipop.handleStoredData();
                } else {
                    chatActivityLollipop.proceedWithAction();
                }
            } else {
                chatActivityLollipop.showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_text_error), -1);
            }
        } else if (context instanceof NodeAttachmentHistoryActivity) {
            NodeAttachmentHistoryActivity nodeAttachmentHistoryActivity = (NodeAttachmentHistoryActivity) context;

            if (e.getErrorCode() == MegaError.API_OK) {
                api.setMyChatFilesFolder(handle, new SetAttrUserListener(nodeAttachmentHistoryActivity));
                nodeAttachmentHistoryActivity.setMyChatFilesFolder(node);
                nodeAttachmentHistoryActivity.handleStoredData();
            } else {
                nodeAttachmentHistoryActivity.showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_text_error));
            }
        } else if (context instanceof CameraUploadsService) {
            ((CameraUploadsService) context).onCreateFolder(e.getErrorCode() == MegaError.API_OK);
        }

        if (e.getErrorCode() != MegaError.API_OK) {
            logError("Error creating folder: " + e.getErrorString());
        }
    }
}
