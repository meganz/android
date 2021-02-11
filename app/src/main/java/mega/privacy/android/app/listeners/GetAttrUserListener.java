package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.AuthenticityCredentialsActivity;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.listeners.CreateFolderListener.ExtraAction.MY_CHAT_FILES;
import static mega.privacy.android.app.utils.Constants.CHAT_FOLDER;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaApiJava.*;

public class GetAttrUserListener extends BaseListener {

    private static final int DAYS_USER_FREE = 30;
    private static final int DAYS_USER_PRO = 90;

    /**
     * Indicates if the request is only to update the DB.
     * If so, the rest of the actions in onRequestFinish() can be ignored.
     */
    private boolean onlyDBUpdate;

    private int holderPosition;

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

     * Constructor to init a request for check the USER_ATTR_AVATAR user's attribute
     * and updates the holder of the adapter.
     *
     * @param context           current application context
     * @param holderPosition    position of the holder to update
     */
    public GetAttrUserListener(Context context, int holderPosition) {
        super(context);

        this.holderPosition = holderPosition;
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
                checkMyChatFilesFolderRequest(api, request, e);
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

            case USER_ATTR_ED25519_PUBLIC_KEY:
                if (context instanceof AuthenticityCredentialsActivity) {
                    ((AuthenticityCredentialsActivity) context).setContactCredentials(request, e);
                }
                break;

            case USER_ATTR_AVATAR:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateAvatar(request);
                }
                break;

            case MegaApiJava.USER_ATTR_RUBBISH_TIME:
                Intent intent = new Intent(ACTION_UPDATE_RB_SCHEDULER);

                if (e.getErrorCode() == MegaError.API_ENOENT) {
                    MyAccountInfo myAccountInfo = MegaApplication.getInstance().getMyAccountInfo();
                    if (myAccountInfo == null)
                        break;

                    intent.putExtra(DAYS_COUNT, myAccountInfo.getAccountType() == MegaAccountDetails.ACCOUNT_TYPE_FREE
                            ? DAYS_USER_FREE
                            : DAYS_USER_PRO);
                } else {
                    intent.putExtra(DAYS_COUNT, request.getNumber());
                }

                MegaApplication.getInstance().sendBroadcast(intent);
                break;

            case MegaApiJava.USER_ATTR_DISABLE_VERSIONS:
                MegaApplication.setDisableFileVersions(request.getFlag());
                MegaApplication.getInstance().sendBroadcast(new Intent(ACTION_UPDATE_FILE_VERSIONS));
                break;

            case MegaApiJava.USER_ATTR_RICH_PREVIEWS:
                if (e.getErrorCode() == MegaError.API_ENOENT) {
                    logWarning("Attribute USER_ATTR_RICH_PREVIEWS not set");
                }

                if (request.getNumDetails() == 1) {
                    MegaApplication.setShowRichLinkWarning(request.getFlag());
                    MegaApplication.setCounterNotNowRichLinkWarning((int) request.getNumber());
                } else if (request.getNumDetails() == 0) {
                    MegaApplication.setEnabledRichLinks(request.getFlag());
                    MegaApplication.getInstance().sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_RICH_LINK_SETTING_UPDATE));
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
    }

    /**
     * If the avatar was correctly obtained in the request, updates the holder in the adapter with it.
     *
     * @param request   result of the request
     */
    private void updateAvatar(MegaRequest request) {
        if (context instanceof GroupChatInfoActivityLollipop) {
            ((GroupChatInfoActivityLollipop) context).updateParticipantAvatar(holderPosition, request.getEmail());
        }
    }
}
