package mega.privacy.android.app.main.controllers;

import static mega.privacy.android.app.listeners.ShareListener.CHANGE_PERMISSIONS_LISTENER;
import static mega.privacy.android.app.utils.CallUtil.participatingInACall;
import static mega.privacy.android.app.utils.Constants.SELECTED_CONTACTS;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Util.isOnline;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.listeners.ShareListener;
import mega.privacy.android.app.main.AddContactActivity;
import mega.privacy.android.app.main.ContactInfoActivity;
import mega.privacy.android.app.main.FileExplorerActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.listeners.MultipleRequestListener;
import mega.privacy.android.app.main.megaachievements.AchievementsActivity;
import mega.privacy.android.app.main.megachat.ArchivedChatsActivity;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.main.megachat.ContactAttachmentActivity;
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity;
import mega.privacy.android.app.meeting.listeners.HangChatCallListener;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

public class ContactController {

    Context context;
    MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi = null;
    DatabaseHandler dbH;
    MegaPreferences prefs = null;

    public ContactController(Context context) {
        Timber.d("ContactController created");
        this.context = context;
        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }
        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        if (dbH == null) {
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public static Intent getPickFolderToShareIntent(Context context, List<MegaUser> users) {
        Timber.d("pickFolderToShare");

        Intent intent = new Intent(context, FileExplorerActivity.class);
        intent.setAction(FileExplorerActivity.ACTION_SELECT_FOLDER_TO_SHARE);
        ArrayList<String> longArray = new ArrayList<String>();
        for (int i = 0; i < users.size(); i++) {
            longArray.add(users.get(i).getEmail());
        }
        intent.putStringArrayListExtra(SELECTED_CONTACTS, longArray);
        return intent;
    }

    public static Intent getPickFileToSendIntent(Context context, List<MegaUser> users) {
        Timber.d("pickFileToSend");

        Intent intent = new Intent(context, FileExplorerActivity.class);
        intent.setAction(FileExplorerActivity.ACTION_MULTISELECT_FILE);
        ArrayList<String> longArray = new ArrayList<String>();
        for (int i = 0; i < users.size(); i++) {
            longArray.add(users.get(i).getEmail());
        }
        intent.putStringArrayListExtra(SELECTED_CONTACTS, longArray);
        return intent;
    }

    /**
     * @deprecated Use {@link mega.privacy.android.app.contacts.usecase.RemoveContactUseCase} instead
     */
    @Deprecated
    public void removeContact(MegaUser c) {
        Timber.d("removeContact");

        checkRemoveContact(c);

        if (context instanceof ManagerActivity) {
            megaApi.removeContact(c, (ManagerActivity) context);
        } else if (context instanceof ContactInfoActivity) {
            megaApi.removeContact(c, (ContactInfoActivity) context);
        } else {
            megaApi.removeContact(c);
        }
    }

    private void checkRemoveContact(MegaUser c) {
        ArrayList<MegaNode> inShares = megaApi.getInShares(c);

        if (inShares.size() != 0) {
            for (int i = 0; i < inShares.size(); i++) {
                MegaNode removeNode = inShares.get(i);
                megaApi.remove(removeNode);
            }
        }

        if (megaChatApi != null && participatingInACall()) {
            MegaChatRoom chatRoomTo = megaChatApi.getChatRoomByUser(c.getHandle());
            if (chatRoomTo != null) {
                long chatId = chatRoomTo.getChatId();
                MegaChatCall call = megaChatApi.getChatCall(chatId);
                if (call != null && (context instanceof ManagerActivity ||
                        context instanceof ContactInfoActivity)) {
                    megaChatApi.hangChatCall(call.getCallId(), new HangChatCallListener(context));
                }
            }
        }
    }


    public void removeMultipleContacts(final ArrayList<MegaUser> contacts) {
        if (contacts.size() > 1) {
            MultipleRequestListener removeMultipleListener = new MultipleRequestListener(-1, context);

            for (MegaUser c : contacts) {
                checkRemoveContact(c);
                megaApi.removeContact(c, removeMultipleListener);
            }
        } else {
            removeContact(contacts.get(0));
        }
    }

    public void reinviteMultipleContacts(final List<MegaContactRequest> requests) {
        MultipleRequestListener reinviteMultipleListener = null;
        if (requests.size() > 1) {
            Timber.d("Reinvite multiple request");
            reinviteMultipleListener = new MultipleRequestListener(-1, context);
            for (int j = 0; j < requests.size(); j++) {

                final MegaContactRequest request = requests.get(j);

                megaApi.inviteContact(request.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_REMIND, reinviteMultipleListener);
            }
        } else {
            Timber.d("Reinvite one request");

            final MegaContactRequest request = requests.get(0);

            reinviteContact(request);
        }
    }

    public void deleteMultipleSentRequestContacts(final List<MegaContactRequest> requests) {
        MultipleRequestListener deleteMultipleListener = null;
        if (requests.size() > 1) {
            Timber.d("Delete multiple request");
            deleteMultipleListener = new MultipleRequestListener(-1, context);
            for (int j = 0; j < requests.size(); j++) {

                final MegaContactRequest request = requests.get(j);

                megaApi.inviteContact(request.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_DELETE, deleteMultipleListener);
            }
        } else {
            Timber.d("Delete one request");

            final MegaContactRequest request = requests.get(0);

            removeInvitationContact(request);
        }
    }

    public void acceptMultipleReceivedRequest(final List<MegaContactRequest> requests) {
        MultipleRequestListener acceptMultipleListener = null;
        if (requests.size() > 1) {
            Timber.d("Accept multiple request");
            acceptMultipleListener = new MultipleRequestListener(-1, context);
            for (int j = 0; j < requests.size(); j++) {

                final MegaContactRequest request = requests.get(j);
                megaApi.replyContactRequest(request, MegaContactRequest.REPLY_ACTION_ACCEPT, acceptMultipleListener);
            }
        } else {
            Timber.d("Accept one request");

            final MegaContactRequest request = requests.get(0);
            acceptInvitationContact(request);
        }
    }

    public void declineMultipleReceivedRequest(final List<MegaContactRequest> requests) {
        MultipleRequestListener declineMultipleListener = null;
        if (requests.size() > 1) {
            Timber.d("Decline multiple request");
            declineMultipleListener = new MultipleRequestListener(-1, context);
            for (int j = 0; j < requests.size(); j++) {

                final MegaContactRequest request = requests.get(j);
                megaApi.replyContactRequest(request, MegaContactRequest.REPLY_ACTION_DENY, declineMultipleListener);
            }
        } else {
            Timber.d("Decline one request");

            final MegaContactRequest request = requests.get(0);
            declineInvitationContact(request);
        }
    }

    public void ignoreMultipleReceivedRequest(final List<MegaContactRequest> requests) {
        MultipleRequestListener ignoreMultipleListener = null;
        if (requests.size() > 1) {
            Timber.d("Ignore multiple request");
            ignoreMultipleListener = new MultipleRequestListener(-1, context);
            for (int j = 0; j < requests.size(); j++) {

                final MegaContactRequest request = requests.get(j);
                megaApi.replyContactRequest(request, MegaContactRequest.REPLY_ACTION_IGNORE, ignoreMultipleListener);
            }
        } else {
            Timber.d("Ignore one request");

            final MegaContactRequest request = requests.get(0);
            ignoreInvitationContact(request);
        }
    }

    public void inviteContact(String contactEmail) {
        Timber.d("inviteContact");

        if (context instanceof ManagerActivity) {
            if (!isOnline(context)) {
                ((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                return;
            }

            if (((ManagerActivity) context).isFinishing()) {
                return;
            }
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, (ManagerActivity) context);
        } else if (context instanceof GroupChatInfoActivity) {
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, (GroupChatInfoActivity) context);
        } else if (context instanceof ChatActivity) {
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, (ChatActivity) context);
        } else if (context instanceof ContactAttachmentActivity) {
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, (ContactAttachmentActivity) context);
        } else {
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, null);
        }
    }

    public void inviteMultipleContacts(ArrayList<String> contactEmails) {
        Timber.d("inviteMultipleContacts");

        MultipleRequestListener inviteMultipleListener = null;

        if (context instanceof ManagerActivity) {
            if (!isOnline(context)) {
                ((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                return;
            }

            if (((ManagerActivity) context).isFinishing()) {
                return;
            }

            if (contactEmails.size() == 1) {
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, (ManagerActivity) context);
            } else if (contactEmails.size() > 1) {
                inviteMultipleListener = new MultipleRequestListener(-1, context);
                for (int i = 0; i < contactEmails.size(); i++) {
                    megaApi.inviteContact(contactEmails.get(i), null, MegaContactRequest.INVITE_ACTION_ADD, inviteMultipleListener);
                }
            }
        } else if (context instanceof ChatActivity) {
            if (!isOnline(context)) {
                ((ChatActivity) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                return;
            }

            if (contactEmails.size() == 1) {
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, (ChatActivity) context);
            } else if (contactEmails.size() > 1) {
                inviteMultipleListener = new MultipleRequestListener(-1, context);
                for (int i = 0; i < contactEmails.size(); i++) {
                    megaApi.inviteContact(contactEmails.get(i), null, MegaContactRequest.INVITE_ACTION_ADD, inviteMultipleListener);
                }
            }
        } else if (context instanceof ContactAttachmentActivity) {
            if (!isOnline(context)) {
                ((ContactAttachmentActivity) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }

            if (contactEmails.size() == 1) {
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, (ContactAttachmentActivity) context);
            } else if (contactEmails.size() > 1) {
                inviteMultipleListener = new MultipleRequestListener(-1, context);
                for (int i = 0; i < contactEmails.size(); i++) {
                    megaApi.inviteContact(contactEmails.get(i), null, MegaContactRequest.INVITE_ACTION_ADD, inviteMultipleListener);
                }
            }
        } else if (context instanceof AchievementsActivity) {
            if (!isOnline(context)) {
                ((AchievementsActivity) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }

            if (contactEmails.size() == 1) {
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, AchievementsActivity.sFetcher);
            } else if (contactEmails.size() > 1) {
                inviteMultipleListener = new MultipleRequestListener(-1, context);
                for (int i = 0; i < contactEmails.size(); i++) {
                    megaApi.inviteContact(contactEmails.get(i), null, MegaContactRequest.INVITE_ACTION_ADD, inviteMultipleListener);
                }
            }
        } else if (context instanceof AddContactActivity) {
            if (!isOnline(context)) {
                ((AddContactActivity) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }

            if (contactEmails.size() == 1) {
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, (AddContactActivity) context);
            } else if (contactEmails.size() > 1) {
                inviteMultipleListener = new MultipleRequestListener(-1, context);
                for (int i = 0; i < contactEmails.size(); i++) {
                    megaApi.inviteContact(contactEmails.get(i), null, MegaContactRequest.INVITE_ACTION_ADD, inviteMultipleListener);
                }
            }
        } else if (context instanceof ArchivedChatsActivity) {
            if (!isOnline(context)) {
                ((ArchivedChatsActivity) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }

            if (contactEmails.size() == 1) {
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, (ArchivedChatsActivity) context);
            } else if (contactEmails.size() > 1) {
                inviteMultipleListener = new MultipleRequestListener(-1, context);
                for (int i = 0; i < contactEmails.size(); i++) {
                    megaApi.inviteContact(contactEmails.get(i), null, MegaContactRequest.INVITE_ACTION_ADD, inviteMultipleListener);
                }
            }
        } else {
            if (contactEmails.size() == 1) {
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, null);
            } else if (contactEmails.size() > 1) {
                for (int i = 0; i < contactEmails.size(); i++) {
                    megaApi.inviteContact(contactEmails.get(i), null, MegaContactRequest.INVITE_ACTION_ADD, null);
                }
            }
        }
    }

    public void addContactDB(String email) {
        MegaUser user = megaApi.getContact(email);
        if (user == null) return;
        //Check the user is not previously in the DB
        if (dbH.findContactByHandle(String.valueOf(user.getHandle())) == null) {
            MegaContactDB megaContactDB = new MegaContactDB(String.valueOf(user.getHandle()), user.getEmail(), "", "");
            dbH.setContact(megaContactDB);
        }
        GetAttrUserListener listener = new GetAttrUserListener(context);
        megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_FIRSTNAME, listener);
        megaApi.getUserAttribute(user, MegaApiJava.USER_ATTR_LASTNAME, listener);
        megaApi.getUserAlias(user.getHandle(), listener);
    }

    public void acceptInvitationContact(MegaContactRequest c) {
        Timber.d("acceptInvitationContact");
        megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_ACCEPT, (ManagerActivity) context);
    }

    public void declineInvitationContact(MegaContactRequest c) {
        Timber.d("declineInvitationContact");
        megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_DENY, (ManagerActivity) context);
    }

    public void ignoreInvitationContact(MegaContactRequest c) {
        Timber.d("ignoreInvitationContact");
        megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_IGNORE, (ManagerActivity) context);
    }

    public void reinviteContact(MegaContactRequest c) {
        Timber.d("inviteContact");
        megaApi.inviteContact(c.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_REMIND, (ManagerActivity) context);
    }

    public void removeInvitationContact(MegaContactRequest c) {
        Timber.d("removeInvitationContact");
        megaApi.inviteContact(c.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_DELETE, (ManagerActivity) context);
    }

    public String getFullName(String name, String lastName, String mail) {

        if (name == null) {
            name = "";
        }
        if (lastName == null) {
            lastName = "";
        }
        String fullName = "";

        if (name.trim().length() <= 0) {
            fullName = lastName;
        } else {
            fullName = name + " " + lastName;
        }

        if (fullName.trim().length() <= 0) {
            Timber.w("Full name empty");
            Timber.d("Put email as fullname");

            if (mail == null) {
                mail = "";
            }

            if (mail.trim().length() <= 0) {
                return "";
            } else {
                return mail;
            }
        }

        return fullName;
    }

    public ArrayList<String> getEmailShares(ArrayList<MegaShare> shares) {
        if (shares == null || shares.isEmpty()) return null;

        ArrayList<String> sharesEmails = new ArrayList<>();

        for (int i = 0; i < shares.size(); i++) {
            sharesEmails.add(shares.get(i).getUser());
        }

        return sharesEmails;
    }

    public void changePermissions(ArrayList<String> shares, int newPermission, MegaNode node) {
        if (shares == null || shares.isEmpty()) return;

        ShareListener shareListener = new ShareListener(context, CHANGE_PERMISSIONS_LISTENER, shares.size());

        for (int i = 0; i < shares.size(); i++) {
            changePermission(shares.get(i), newPermission, node, shareListener);
        }
    }

    public void changePermission(String email, int newPermission, MegaNode node, ShareListener shareListener) {
        if (email != null) {
            megaApi.share(node, email, newPermission, shareListener);
        }
    }
}
