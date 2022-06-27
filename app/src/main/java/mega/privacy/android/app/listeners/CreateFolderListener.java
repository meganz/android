package mega.privacy.android.app.listeners;

import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.main.FileExplorerActivity;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.utils.JobUtil;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import timber.log.Timber;

public class CreateFolderListener extends BaseListener {

    public enum ExtraAction {
        NONE,
        MY_CHAT_FILES,
        INIT_CAMERA_UPLOAD
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

        if (extraAction == ExtraAction.INIT_CAMERA_UPLOAD) {
            if (name.equals(context.getString(R.string.section_photo_sync))) {
                CameraUploadsService.isCreatingPrimary = false;
                //set primary only
                if (e.getErrorCode() == MegaError.API_OK) {
                    Timber.d("Set CU primary attribute on create folder: %s", handle);
                    api.setCameraUploadsFolders(handle, MegaApiJava.INVALID_HANDLE, new SetAttrUserListener(context));
                } else {
                    Timber.w("Create CU folder failed, error code: %d, %s", e.getErrorCode(), e.getErrorString());
                    JobUtil.fireStopCameraUploadJob(context);
                }
            } else if (name.equals(context.getString(R.string.section_secondary_media_uploads))) {
                CameraUploadsService.isCreatingSecondary = false;
                //set secondary only
                if (e.getErrorCode() == MegaError.API_OK) {
                    Timber.d("Set CU secondary attribute on create folder: %s", handle);
                    api.setCameraUploadsFolders(MegaApiJava.INVALID_HANDLE, handle, new SetAttrUserListener(context));
                } else {
                    Timber.w("Create MU folder failed, error code: %d, %s", e.getErrorCode(), e.getErrorString());
                    JobUtil.fireStopCameraUploadJob(context);
                }
            }
        }

        if (context instanceof FileExplorerActivity) {
            FileExplorerActivity fileExplorerActivity = (FileExplorerActivity) context;

            if (e.getErrorCode() == MegaError.API_OK) {
                if (extraAction == ExtraAction.MY_CHAT_FILES) {
                    fileExplorerActivity.setMyChatFilesFolder(node);
                    api.setMyChatFilesFolder(handle, new SetAttrUserListener(fileExplorerActivity));
                    fileExplorerActivity.checkIfFilesExistsInMEGA();
                } else {
                    fileExplorerActivity.finishCreateFolder(true, handle);
                }
            } else {
                if (extraAction == ExtraAction.MY_CHAT_FILES) {
                    fileExplorerActivity.showSnackbar(context.getString(R.string.general_text_error));
                } else {
                    fileExplorerActivity.finishCreateFolder(false, handle);
                    fileExplorerActivity.showSnackbar(context.getString(R.string.error_creating_folder, name));
                }
            }
        } else if (context instanceof ChatActivity) {
            ChatActivity chatActivity = (ChatActivity) context;

            if (e.getErrorCode() == MegaError.API_OK) {
                api.setMyChatFilesFolder(handle, new SetAttrUserListener(chatActivity));
                chatActivity.setMyChatFilesFolder(node);
                if (chatActivity.isForwardingFromNC()) {
                    chatActivity.handleStoredData();
                } else {
                    chatActivity.proceedWithAction();
                }
            } else {
                chatActivity.showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_text_error), -1);
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
            Timber.e("Error creating folder: %s", e.getErrorString());
        }
    }
}
