package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.Constants.*;

public class CreateFolderListener extends BaseListener {

    private boolean isMyChatFiles;

    public CreateFolderListener(Context context) {
        super(context);
    }

    public CreateFolderListener(Context context, boolean isMyChatFiles) {
        super(context);

        this.isMyChatFiles = isMyChatFiles;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_CREATE_FOLDER) return;

        long handle = request.getNodeHandle();
        String name = request.getName();

        if (context instanceof FileExplorerActivityLollipop) {
            FileExplorerActivityLollipop fileExplorerActivityLollipop = (FileExplorerActivityLollipop) context;

            if (e.getErrorCode() == MegaError.API_OK) {
                if (isMyChatFiles) {
                    fileExplorerActivityLollipop.setMyChatFilesNode(api.getNodeByHandle(handle));
                    api.setMyChatFilesFolder(handle, new SetAttrUserListener(fileExplorerActivityLollipop));
                    fileExplorerActivityLollipop.checkIfFilesExistsInMEGA();
                } else {
                    fileExplorerActivityLollipop.finishCreateFolder(true, handle);
                }
            } else {
                if (isMyChatFiles) {
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
                chatActivityLollipop.proceedWithForward();
            } else {
                chatActivityLollipop.showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_text_error), -1);
            }
        }
    }
}
