package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.Constants.*;
import static nz.mega.sdk.MegaApiJava.*;

public class GetAttrUserListener extends BaseListener {

    public GetAttrUserListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_GET_ATTR_USER) return;

        switch (request.getParamType()) {
            case USER_ATTR_MY_CHAT_FILES_FOLDER:

                MegaNode myChatFolderNode = null;
                boolean myChatFolderFound = false;

                if (e.getErrorCode() == MegaError.API_OK) {
                    myChatFolderFound = true;
                    myChatFolderNode = api.getNodeByHandle(request.getNodeHandle());
                } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                    checkMyChatFolderNode(api, myChatFolderNode, myChatFolderFound);
                }

                if (context instanceof FileExplorerActivityLollipop) {
                    FileExplorerActivityLollipop fileExplorerActivityLollipop = (FileExplorerActivityLollipop) context;

                    if (myChatFolderFound) {
                        fileExplorerActivityLollipop.setMyChatFilesNode(myChatFolderNode);
                        fileExplorerActivityLollipop.checkIfFilesExistsInMEGA();
                    } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                        api.createFolder(context.getString(R.string.my_chat_files_folder), api.getRootNode(), new CreateFolderListener(fileExplorerActivityLollipop, true));
                    }
                } else if (context instanceof ChatActivityLollipop) {
                    ChatActivityLollipop chatActivityLollipop = (ChatActivityLollipop) context;

                    if (myChatFolderFound) {
                        chatActivityLollipop.proceedWithForward();
                    } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                        api.createFolder(context.getString(R.string.my_chat_files_folder), api.getRootNode(), new CreateFolderListener(chatActivityLollipop, true));
                    }
                }
                break;
        }
    }

    private void checkMyChatFolderNode(MegaApiJava api, MegaNode myChatFolderNode, boolean myChatFolderFound) {
        myChatFolderNode = api.getNodeByPath(CHAT_FOLDER, api.getRootNode());
        if (myChatFolderNode != null) {
            myChatFolderFound = true;
            api.renameNode(myChatFolderNode, context.getString(R.string.my_chat_files_folder), new RenameListener(context, true));
            api.setMyChatFilesFolder(myChatFolderNode.getHandle(), new SetAttrUserListener(context));
        }
    }
}
