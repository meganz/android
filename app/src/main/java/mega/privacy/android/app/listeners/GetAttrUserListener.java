package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
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
     * @param context       current application context
     * @param onlyDBUpdate  true if the purpose of the request is only update the DB, false otherwise
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
                } else if (!onlyDBUpdate){
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
        }
    }
}
