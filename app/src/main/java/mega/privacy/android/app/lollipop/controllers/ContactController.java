package mega.privacy.android.app.lollipop.controllers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.ContactNameListener;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity;
import mega.privacy.android.app.lollipop.megachat.ArchivedChatsActivity;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ContactAttachmentActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ContactController {

    Context context;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH;
    MegaPreferences prefs = null;

    public ContactController(Context context){
        logDebug("ContactController created");
        this.context = context;
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public void pickFolderToShare(List<MegaUser> users){

        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_SELECT_FOLDER_TO_SHARE);
        ArrayList<String> longArray = new ArrayList<String>();
        for (int i=0; i<users.size(); i++){
            longArray.add(users.get(i).getEmail());
        }
        intent.putStringArrayListExtra(SELECTED_CONTACTS, longArray);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER);
    }

    public void pickFileToSend(List<MegaUser> users){
        logDebug("pickFileToSend");

        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_MULTISELECT_FILE);
        ArrayList<String> longArray = new ArrayList<String>();
        for (int i=0; i<users.size(); i++){
            longArray.add(users.get(i).getEmail());
        }
        intent.putStringArrayListExtra(SELECTED_CONTACTS, longArray);

        if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
        }
        else if(context instanceof ContactInfoActivityLollipop){
            ((ContactInfoActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
        }
    }

    public void removeContact(MegaUser c){
        logDebug("removeContact");
        final ArrayList<MegaNode> inShares = megaApi.getInShares(c);
        if(inShares.size() != 0)
        {
            for(int i=0; i<inShares.size();i++){
                MegaNode removeNode = inShares.get(i);
                megaApi.remove(removeNode);
            }
        }

        if(context instanceof ManagerActivityLollipop){
            megaApi.removeContact(c, (ManagerActivityLollipop) context);
        }
        else if(context instanceof ContactInfoActivityLollipop){
            megaApi.removeContact(c, (ContactInfoActivityLollipop) context);
        }

    }


    public void removeMultipleContacts(final ArrayList<MegaUser> contacts){
        MultipleRequestListener removeMultipleListener = null;
        if(contacts.size()>1){
            logDebug("Remove multiple contacts");
            removeMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<contacts.size();j++){

                final MegaUser c= contacts.get(j);

                final ArrayList<MegaNode> inShares = megaApi.getInShares(c);

                if(inShares.size() != 0){
                    for(int i=0; i<inShares.size();i++){
                        MegaNode removeNode = inShares.get(i);
                        megaApi.remove(removeNode);
                    }
                }
                megaApi.removeContact(c, removeMultipleListener);
            }
        }
        else{
            logDebug("Remove one contact");

            final MegaUser c= contacts.get(0);

            final ArrayList<MegaNode> inShares = megaApi.getInShares(c);

            if(inShares.size() != 0){
                for(int i=0; i<inShares.size();i++){
                    MegaNode removeNode = inShares.get(i);
                    megaApi.remove(removeNode);
                }
            }
            megaApi.removeContact(c, (ManagerActivityLollipop) context);
        }
    }

    public void reinviteMultipleContacts(final List<MegaContactRequest> requests){
        MultipleRequestListener reinviteMultipleListener = null;
        if(requests.size()>1){
            logDebug("Reinvite multiple request");
            reinviteMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<requests.size();j++){

                final MegaContactRequest request= requests.get(j);

                megaApi.inviteContact(request.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_REMIND, reinviteMultipleListener);
            }
        }
        else{
            logDebug("Reinvite one request");

            final MegaContactRequest request= requests.get(0);

            reinviteContact(request);
        }
    }

    public void deleteMultipleSentRequestContacts(final List<MegaContactRequest> requests){
        MultipleRequestListener deleteMultipleListener = null;
        if(requests.size()>1){
            logDebug("Delete multiple request");
            deleteMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<requests.size();j++){

                final MegaContactRequest request= requests.get(j);

                megaApi.inviteContact(request.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_DELETE, deleteMultipleListener);
            }
        }
        else{
            logDebug("Delete one request");

            final MegaContactRequest request= requests.get(0);

            removeInvitationContact(request);
        }
    }

    public void acceptMultipleReceivedRequest(final List<MegaContactRequest> requests){
        MultipleRequestListener acceptMultipleListener = null;
        if(requests.size()>1){
            logDebug("Accept multiple request");
            acceptMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<requests.size();j++){

                final MegaContactRequest request= requests.get(j);
                megaApi.replyContactRequest(request, MegaContactRequest.REPLY_ACTION_ACCEPT, acceptMultipleListener);
            }
        }
        else{
            logDebug("Accept one request");

            final MegaContactRequest request= requests.get(0);
            acceptInvitationContact(request);
        }
    }

    public void declineMultipleReceivedRequest(final List<MegaContactRequest> requests){
        MultipleRequestListener declineMultipleListener = null;
        if(requests.size()>1){
            logDebug("Decline multiple request");
            declineMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<requests.size();j++){

                final MegaContactRequest request= requests.get(j);
                megaApi.replyContactRequest(request, MegaContactRequest.REPLY_ACTION_DENY, declineMultipleListener);
            }
        }
        else{
            logDebug("Decline one request");

            final MegaContactRequest request= requests.get(0);
            declineInvitationContact(request);
        }
    }

    public void ignoreMultipleReceivedRequest(final List<MegaContactRequest> requests){
        MultipleRequestListener ignoreMultipleListener = null;
        if(requests.size()>1){
            logDebug("Ignore multiple request");
            ignoreMultipleListener = new MultipleRequestListener(-1, context);
            for(int j=0; j<requests.size();j++){

                final MegaContactRequest request= requests.get(j);
                megaApi.replyContactRequest(request, MegaContactRequest.REPLY_ACTION_IGNORE, ignoreMultipleListener);
            }
        }
        else{
            logDebug("Ignore one request");

            final MegaContactRequest request= requests.get(0);
            ignoreInvitationContact(request);
        }
    }

    public void inviteContact(String contactEmail){
        logDebug("inviteContact");

        if(context instanceof ManagerActivityLollipop){
            if (!isOnline(context)){
                ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                return;
            }

            if(((ManagerActivityLollipop) context).isFinishing()){
                return;
            }
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, (ManagerActivityLollipop) context);
        }
        else if(context instanceof GroupChatInfoActivityLollipop){
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, (GroupChatInfoActivityLollipop) context);
        }
        else if(context instanceof ChatActivityLollipop){
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, (ChatActivityLollipop) context);
        }
        else if(context instanceof ContactAttachmentActivityLollipop){
            megaApi.inviteContact(contactEmail, null, MegaContactRequest.INVITE_ACTION_ADD, (ContactAttachmentActivityLollipop) context);
        }
    }

    public void inviteMultipleContacts(ArrayList<String> contactEmails){
        logDebug("inviteMultipleContacts");

        MultipleRequestListener inviteMultipleListener = null;

        if(context instanceof ManagerActivityLollipop){
            if (!isOnline(context)){
                ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                return;
            }

            if(((ManagerActivityLollipop) context).isFinishing()){
                return;
            }

            if (contactEmails.size() == 1){
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, (ManagerActivityLollipop) context);
            }
            else if (contactEmails.size() > 1){
                inviteMultipleListener = new MultipleRequestListener(-1, context);
                for(int i=0; i<contactEmails.size();i++) {
                    megaApi.inviteContact(contactEmails.get(i), null, MegaContactRequest.INVITE_ACTION_ADD, inviteMultipleListener);
                }
            }
        }
        else if(context instanceof ChatActivityLollipop){
            if (!isOnline(context)){
                ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                return;
            }

            if (contactEmails.size() == 1){
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, (ChatActivityLollipop) context);
            }
            else if (contactEmails.size() > 1){
                inviteMultipleListener = new MultipleRequestListener(-1, context);
                for(int i=0; i<contactEmails.size();i++) {
                    megaApi.inviteContact(contactEmails.get(i), null, MegaContactRequest.INVITE_ACTION_ADD, inviteMultipleListener);
                }
            }
        }
        else if(context instanceof ContactAttachmentActivityLollipop){
            if (!isOnline(context)){
                ((ContactAttachmentActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }

            if (contactEmails.size() == 1){
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, (ContactAttachmentActivityLollipop) context);
            }
            else if (contactEmails.size() > 1){
                inviteMultipleListener = new MultipleRequestListener(-1, context);
                for(int i=0; i<contactEmails.size();i++) {
                    megaApi.inviteContact(contactEmails.get(i), null, MegaContactRequest.INVITE_ACTION_ADD, inviteMultipleListener);
                }
            }
        }
        else if(context instanceof AchievementsActivity){
            if (!isOnline(context)){
                ((AchievementsActivity) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }

            if (contactEmails.size() == 1){
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, (AchievementsActivity) context);
            }
            else if (contactEmails.size() > 1){
                inviteMultipleListener = new MultipleRequestListener(-1, context);
                for(int i=0; i<contactEmails.size();i++) {
                    megaApi.inviteContact(contactEmails.get(i), null, MegaContactRequest.INVITE_ACTION_ADD, inviteMultipleListener);
                }
            }
        }
        else if (context instanceof AddContactActivityLollipop) {
            if (!isOnline(context)){
                ((AddContactActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }

            if (contactEmails.size() == 1){
                megaApi.inviteContact(contactEmails.get(0), null, MegaContactRequest.INVITE_ACTION_ADD, (AddContactActivityLollipop) context);
            }
            else if (contactEmails.size() > 1){
                inviteMultipleListener = new MultipleRequestListener(-1, context);
                for(int i=0; i<contactEmails.size();i++) {
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
        }
    }


    public void addContactDB(String email){

        MegaUser user = megaApi.getContact(email);
        if(user!=null){
            //Check the user is not previously in the DB
            if(dbH.findContactByHandle(String.valueOf(user.getHandle()))==null){
                MegaContactDB megaContactDB = new MegaContactDB(String.valueOf(user.getHandle()), user.getEmail(), "", "", null);
                dbH.setContact(megaContactDB);
                megaApi.getUserAttribute(user, 1, new ContactNameListener(context));
                megaApi.getUserAttribute(user, 2, new ContactNameListener(context));
                megaApi.getUserAlias(user.getHandle(), new ContactNameListener(context));
            }
            else{
                megaApi.getUserAttribute(user, 1, new ContactNameListener(context));
                megaApi.getUserAttribute(user, 2, new ContactNameListener(context));
                megaApi.getUserAlias(user.getHandle(), new ContactNameListener(context));
            }
        }
    }


    public void acceptInvitationContact(MegaContactRequest c){
        logDebug("acceptInvitationContact");
        megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_ACCEPT, (ManagerActivityLollipop) context);
    }

    public void declineInvitationContact(MegaContactRequest c){
        logDebug("declineInvitationContact");
        megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_DENY, (ManagerActivityLollipop) context);
    }

    public void ignoreInvitationContact(MegaContactRequest c){
        logDebug("ignoreInvitationContact");
        megaApi.replyContactRequest(c, MegaContactRequest.REPLY_ACTION_IGNORE, (ManagerActivityLollipop) context);
    }

    public void reinviteContact(MegaContactRequest c){
        logDebug("inviteContact");
        megaApi.inviteContact(c.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_REMIND, (ManagerActivityLollipop) context);
    }

    public void removeInvitationContact(MegaContactRequest c){
        logDebug("removeInvitationContact");
        megaApi.inviteContact(c.getTargetEmail(), null, MegaContactRequest.INVITE_ACTION_DELETE, (ManagerActivityLollipop) context);
    }

    public String getFullName(String name, String lastName, String mail){

        if(name==null){
            name="";
        }
        if(lastName==null){
            lastName="";
        }
        String fullName = "";

        if (name.trim().length() <= 0){
            fullName = lastName;
        }
        else{
            fullName = name + " " + lastName;
        }

        if (fullName.trim().length() <= 0){
            logWarning("Full name empty");
            logDebug("Put email as fullname");

            if(mail==null){
                mail="";
            }

            if (mail.trim().length() <= 0){
                return "";
            }
            else{
                return mail;
            }
        }

        return fullName;
    }

    public void checkShares(List<MegaShare> shares, int newPermission, MegaNode node, MegaRequestListenerInterface changeListener) {
        for (int i = 0; i < shares.size(); i++) {
            String userId = shares.get(i).getUser();
            changePermission(userId, newPermission, node, changeListener);
        }
    }

    private void changePermission(String userId, int newPermission, MegaNode node, MegaRequestListenerInterface changeListener) {
        if (userId != null) {
            MegaUser megaUser = megaApi.getContact(userId);
            if(megaUser != null){
                megaApi.share(node, megaUser, newPermission, changeListener);
            }else{
                megaApi.share(node, userId, newPermission, changeListener);
            }
        }
    }
}
