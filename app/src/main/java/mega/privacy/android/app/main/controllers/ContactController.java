package mega.privacy.android.app.main.controllers;

import static mega.privacy.android.app.listeners.ShareListener.CHANGE_PERMISSIONS_LISTENER;
import static mega.privacy.android.app.utils.Util.isOnline;

import android.content.Context;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import mega.privacy.android.app.listeners.ShareListener;
import mega.privacy.android.app.main.legacycontact.AddContactActivity;
import mega.privacy.android.app.main.listeners.MultipleRequestListener;
import mega.privacy.android.app.main.megachat.ContactAttachmentActivity;
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity;
import mega.privacy.android.app.presentation.achievements.AchievementsFeatureActivity;
import mega.privacy.android.data.database.DatabaseHandler;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import timber.log.Timber;

public class ContactController {

    Context context;
    MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi = null;
    DatabaseHandler dbH;

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
            dbH = DbHandlerModuleKt.getDbHandler();
        }
    }

    public void inviteContact(String contactEmail) {
        Timber.d("inviteContact");

        if (context instanceof GroupChatInfoActivity) {
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, (GroupChatInfoActivity) context);
        } else if (context instanceof ContactAttachmentActivity) {
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, (ContactAttachmentActivity) context);
        } else {
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, null);
        }
    }

    public void inviteMultipleContacts(ArrayList<String> contactEmails) {
        Timber.d("inviteMultipleContacts");

        MultipleRequestListener inviteMultipleListener = null;

        if (context instanceof ContactAttachmentActivity) {
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
        } else if (context instanceof AchievementsFeatureActivity) {
            if (!isOnline(context)) {
                ((AchievementsFeatureActivity) context).showSnackbar(R.string.error_server_connection_problem);
                return;
            }

            if (contactEmails.size() == 1) {
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, ((AchievementsFeatureActivity) context).getFetcher());
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
