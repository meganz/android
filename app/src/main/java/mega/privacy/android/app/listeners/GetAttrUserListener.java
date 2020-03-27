package mega.privacy.android.app.listeners;

import android.content.Context;
import android.graphics.Bitmap;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaParticipantsChatLollipopAdapter;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static nz.mega.sdk.MegaApiJava.*;

public class GetAttrUserListener extends BaseListener {
    /**
     * Indicates if the request is only to update the DB.
     * If so, the rest of the actions in onRequestFinish() can be ignored.
     */
    private boolean onlyDBUpdate;

    private MegaParticipantsChatLollipopAdapter.ViewHolderParticipantsList holder;
    private MegaParticipantsChatLollipopAdapter adapter;

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

    /**
     * Constructor to init a request for check the USER_ATTR_AVATAR user's attribute
     * and update the holder of the adapter, both passed as parameters.
     *
     * @param context   current application context
     * @param holder    item view of the adapter
     * @param adapter   adapter in which the avatar has to be updated
     */
    public GetAttrUserListener(Context context, MegaParticipantsChatLollipopAdapter.ViewHolderParticipantsList holder, MegaParticipantsChatLollipopAdapter adapter) {
        super(context);

        this.holder = holder;
        this.adapter = adapter;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_GET_ATTR_USER) return;

        switch (request.getParamType()) {
            case USER_ATTR_MY_CHAT_FILES_FOLDER:
                checkMyChatFilesFolderRequest(api, request, e);
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

            case USER_ATTR_AVATAR:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateAvatar(request);
                }
                break;
        }
    }

    /**
     * Checks if the USER_ATTR_MY_CHAT_FILES_FOLDER user's attribute exists when the request finished:
     * - If not exists but exists a file with the old "My chat files" name, it renames it with the old one.
     * - If the old one neither exists, it launches a request to create it.
     *
     * Updates the DB with the result and, if necessary updates the data in the current Context.
     *
     * @param api       MegaApiJava object
     * @param request   result of the request
     * @param e         MegaError received when the request finished
     */
    private void checkMyChatFilesFolderRequest(MegaApiJava api, MegaRequest request, MegaError e) {
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
    }

    /**
     * If the avatar was correctly obtained in the request, updates the holder in the adapter with it.
     *
     * @param request   result of the request
     */
    private void updateAvatar(MegaRequest request) {
        if (adapter == null || holder == null
                || !holder.getUserHandle().equals(request.getEmail())
                || holder.getContactMail() == null || !holder.getContactMail().equals(request.getEmail())) {
            logWarning("Error getting user's avatar");
            return;
        }

        Bitmap bitmap = getAvatarBitmap(request.getEmail());
        if (bitmap != null) {
            holder.setImageView(bitmap);
            adapter.notifyItemChanged(holder.getAdapterPosition());
        }
    }
}
