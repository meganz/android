package mega.privacy.android.app.lollipop.controllers;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.ChatImportToForwardListener;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ArchivedChatsActivity;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity;
import mega.privacy.android.app.lollipop.megachat.ChatFullScreenImageViewer;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.lollipop.megachat.NonContactInfo;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.ChatUtil.getMegaChatMessage;
import static mega.privacy.android.app.utils.CacheFolderManager.buildVoiceClipFile;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.Util.toCDATA;

public class ChatController {

    Context context;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;
    MegaPreferences prefs = null;

    public ChatController(Context context){
        log("ChatController created");
        this.context = context;
        if(context instanceof  MegaApplication){
            if (megaApi == null){
                megaApi = ((MegaApplication)context).getMegaApi();
            }
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication)context).getMegaChatApi();
            }
        }
        else{
            if (megaApi == null){
                megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
            }
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
            }
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public void leaveChat(MegaChatRoom chat){
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.leaveChat(chat.getChatId(), (ManagerActivityLollipop) context);
        }
        else if(context instanceof GroupChatInfoActivityLollipop){
            megaChatApi.leaveChat(chat.getChatId(), (GroupChatInfoActivityLollipop) context);
        }
        else if(context instanceof ChatActivityLollipop){
            megaChatApi.leaveChat(chat.getChatId(), (ChatActivityLollipop) context);
        }
    }

    public void selectChatsToAttachContact(MegaUser contact){
        log("selectChatsToAttachContact");

        long[] longArray = new long[1];
        longArray[0] = contact.getHandle();

        Intent i = new Intent(context, ChatExplorerActivity.class);
        i.putExtra("USER_HANDLES", longArray);

        if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).startActivityForResult(i, Constants.REQUEST_CODE_SELECT_CHAT);
        }
        else if(context instanceof ContactInfoActivityLollipop){
            ((ContactInfoActivityLollipop) context).startActivityForResult(i, Constants.REQUEST_CODE_SELECT_CHAT);
        }
    }

    public void selectChatsToAttachContacts (ArrayList<MegaUser> contacts) {
        long[] longArray = new long[contacts.size()];

        for (int i=0; i<contacts.size(); i++) {
            longArray[i] = contacts.get(i).getHandle();
        }

        Intent i = new Intent(context, ChatExplorerActivity.class);
        i.putExtra("USER_HANDLES", longArray);

        if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).startActivityForResult(i, Constants.REQUEST_CODE_SELECT_CHAT);
        }
    }

    public void leaveChat(long chatId){
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.leaveChat(chatId, (ManagerActivityLollipop) context);
        }
        else if(context instanceof GroupChatInfoActivityLollipop){
            megaChatApi.leaveChat(chatId, (GroupChatInfoActivityLollipop) context);
        }
        else if(context instanceof ChatActivityLollipop){
            megaChatApi.leaveChat(chatId, (ChatActivityLollipop) context);
        }
    }

    public void clearHistory(MegaChatRoom chat){
        log("clearHistory: "+chat.getTitle());
        clearHistory(chat.getChatId());
    }

    public void clearHistory(long chatId){
        log("clearHistory: "+chatId);
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (ManagerActivityLollipop) context);
        }
        else if(context instanceof ChatActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (ChatActivityLollipop) context);
        }
        else if(context instanceof ContactInfoActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (ContactInfoActivityLollipop) context);
        }
        else if(context instanceof GroupChatInfoActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (GroupChatInfoActivityLollipop) context);
        }

        dbH.removePendingMessageByChatId(chatId);
    }

    public void archiveChat(long chatId){
        log("archiveChat: "+chatId);
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.archiveChat(chatId, true, (ManagerActivityLollipop) context);
        }
    }

    public void archiveChat(MegaChatListItem chatItem){
        log("archiveChat: "+chatItem.getChatId());
        if(context instanceof ManagerActivityLollipop){
            if(chatItem.isArchived()){
                megaChatApi.archiveChat(chatItem.getChatId(), false, (ManagerActivityLollipop) context);
            }
            else{
                megaChatApi.archiveChat(chatItem.getChatId(), true, (ManagerActivityLollipop) context);
            }
        }
        else if(context instanceof ArchivedChatsActivity){
            if(chatItem.isArchived()){
                megaChatApi.archiveChat(chatItem.getChatId(), false, (ArchivedChatsActivity) context);
            }
            else{
                megaChatApi.archiveChat(chatItem.getChatId(), true, (ArchivedChatsActivity) context);
            }
        }
    }

    public void archiveChat(MegaChatRoom chat){
        log("archiveChat: "+chat.getChatId());
        if(context instanceof GroupChatInfoActivityLollipop){

            if(chat.isArchived()){
                megaChatApi.archiveChat(chat.getChatId(), false,(GroupChatInfoActivityLollipop) context);
            }
            else{
                megaChatApi.archiveChat(chat.getChatId(), true, (GroupChatInfoActivityLollipop) context);
            }
        }
        else if(context instanceof ChatActivityLollipop){
            if(chat.isArchived()){
                megaChatApi.archiveChat(chat.getChatId(), false,(ChatActivityLollipop) context);
            }
            else{
                megaChatApi.archiveChat(chat.getChatId(), true, (ChatActivityLollipop) context);
            }
        }
    }

    public void archiveChats(ArrayList<MegaChatListItem> chats){
        log("archiveChats: "+chats.size());
        if(context instanceof ManagerActivityLollipop){
            for(int i=0; i<chats.size(); i++){
                if(chats.get(i).isArchived()){
                    megaChatApi.archiveChat(chats.get(i).getChatId(), false,null);
                }
                else{
                    megaChatApi.archiveChat(chats.get(i).getChatId(), true, null);
                }
            }
        }
        else if(context instanceof ArchivedChatsActivity){
            for(int i=0; i<chats.size(); i++){
                if(chats.get(i).isArchived()){
                    megaChatApi.archiveChat(chats.get(i).getChatId(), false, null);
                }
                else{
                    megaChatApi.archiveChat(chats.get(i).getChatId(), true, null);
                }
            }
        }
    }

    public void deleteMessages(ArrayList<MegaChatMessage> messages, MegaChatRoom chat){
        log("deleteMessages: "+messages.size());
        if(messages!=null){
            for(int i=0; i<messages.size();i++){
                deleteMessage(messages.get(i), chat.getChatId());
            }
        }
    }

    public void deleteAndroidMessages(ArrayList<AndroidMegaChatMessage> messages, MegaChatRoom chat){
        log("deleteMessages: "+messages.size());
        if(messages!=null){
            for(int i=0; i<messages.size();i++){
                deleteMessage(messages.get(i).getMessage(), chat.getChatId());
            }
        }
    }

    public void deleteMessageById(long messageId, long chatId) {
        log("deleteMessage");
        MegaChatMessage message = getMegaChatMessage(context, megaChatApi, chatId, messageId);

        if(message!=null){
            deleteMessage(message, chatId);
        }
    }

    public void deleteMessage(MegaChatMessage message, long chatId) {
        log("deleteMessage");
        MegaChatMessage messageToDelete;
        if (message == null) return;
        if (message.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || message.getType() == MegaChatMessage.TYPE_VOICE_CLIP) {
            log("deleteMessage:Delete node attachment message or voice clip message");
            if (message.getType() == MegaChatMessage.TYPE_VOICE_CLIP && message.getMegaNodeList() != null && message.getMegaNodeList().size() > 0 && message.getMegaNodeList().get(0) != null) {
                deleteOwnVoiceClip(context, message.getMegaNodeList().get(0).getName());
            }
            megaChatApi.revokeAttachmentMessage(chatId, message.getMsgId());
            return;
        }

        log("Delete normal message");
        if(message.getStatus() == MegaChatMessage.STATUS_SENDING){
            messageToDelete = megaChatApi.deleteMessage(chatId, message.getTempId());
        }else{
            messageToDelete = megaChatApi.deleteMessage(chatId, message.getMsgId());
        }

        if (messageToDelete == null) {
            log("The message cannot be deleted");
        }else{
            log("The message has been deleted");
            ((ChatActivityLollipop) context).updatingRemovedMessage(message);
        }
    }

    /*
     * Delete a voice note from local storage
     */
    public static void deleteOwnVoiceClip(Context mContext, String nameFile) {
        log("deleteOwnVoiceClip");
        File localFile = buildVoiceClipFile(mContext, nameFile);
        if (!isFileAvailable(localFile)) return;
        localFile.delete();
    }

    public void alterParticipantsPermissions(long chatid, long uh, int privilege){
        log("alterParticipantsPermissions: "+uh);
        megaChatApi.updateChatPermissions(chatid, uh, privilege, (GroupChatInfoActivityLollipop) context);
    }

    public void removeParticipant(long chatid, long uh){
        log("removeParticipant: "+uh);
        if(context==null){
            log("Context is NULL");
        }
        megaChatApi.removeFromChat(chatid, uh, (GroupChatInfoActivityLollipop) context);
    }

    public void changeTitle(long chatid, String title){
        if(context instanceof GroupChatInfoActivityLollipop){
            megaChatApi.setChatTitle(chatid, title, (GroupChatInfoActivityLollipop) context);
        }
    }

    public void muteChats(ArrayList<MegaChatListItem> chats){
        for(int i=0; i<chats.size();i++){
            muteChat(chats.get(i));
            ((ManagerActivityLollipop)context).showMuteIcon(chats.get(i));
        }
    }

    public void muteChat(long chatHandle){
        log("muteChat");
        ChatItemPreferences chatPrefs = dbH.findChatPreferencesByHandle(Long.toString(chatHandle));
        if(chatPrefs==null){

            chatPrefs = new ChatItemPreferences(Long.toString(chatHandle), Boolean.toString(false), "");
            dbH.setChatItemPreferences(chatPrefs);

        }
        else{
            chatPrefs.setNotificationsEnabled(Boolean.toString(false));
            dbH.setNotificationEnabledChatItem(Boolean.toString(false), Long.toString(chatHandle));
        }
    }

    public void muteChat(MegaChatListItem chat){
        log("muteChatITEM");
        muteChat(chat.getChatId());
    }

    public void unmuteChats(ArrayList<MegaChatListItem> chats){
        for(int i=0; i<chats.size();i++){
            unmuteChat(chats.get(i));
            ((ManagerActivityLollipop)context).showMuteIcon(chats.get(i));
        }
    }

    public void unmuteChat(MegaChatListItem chat){
        log("UNmuteChatITEM");
        unmuteChat(chat.getChatId());
    }

    public void unmuteChat(long chatHandle){
        log("UNmuteChat");
        ChatItemPreferences chatPrefs = dbH.findChatPreferencesByHandle(Long.toString(chatHandle));
        if(chatPrefs==null){
            chatPrefs = new ChatItemPreferences(Long.toString(chatHandle), Boolean.toString(true), "");
            dbH.setChatItemPreferences(chatPrefs);
        }
        else{
            chatPrefs.setNotificationsEnabled(Boolean.toString(true));
            dbH.setNotificationEnabledChatItem(Boolean.toString(true), Long.toString(chatHandle));
        }
    }

    public void enableChat(){
        dbH.setEnabledChat(true+"");
    }

    public String createSingleManagementString(AndroidMegaChatMessage androidMessage, MegaChatRoom chatRoom) {
        log("createSingleManagementString");

        String text = createManagementString(androidMessage.getMessage(), chatRoom);
        if((text!=null) && (!text.isEmpty())){
            text = text.substring(text.indexOf(":")+2);
        }else{
            text = "";

        }
        return text;
    }


    public String createManagementString(MegaChatMessage message, MegaChatRoom chatRoom) {
        log("createManagementString");
        long userHandle = message.getUserHandle();

        if (message.getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) {
            log("ALTER PARTICIPANT MESSAGE!!");

            if (message.getHandleOfAction() == megaApi.getMyUser().getHandle()) {
                log("me alter participant");

                StringBuilder builder = new StringBuilder();
                builder.append(megaChatApi.getMyFullname() + ": ");

                int privilege = message.getPrivilege();
                log("Privilege of me: " + privilege);
                String textToShow = "";
                String fullNameAction = getFullName(message.getUserHandle(), chatRoom);

                if(!fullNameAction.isEmpty()){
                    if (fullNameAction.trim().length() <= 0) {
                        log("No name!");
                        NonContactInfo nonContact = dbH.findNonContactByHandle(message.getUserHandle() + "");
                        if (nonContact != null) {
                            fullNameAction = nonContact.getFullName();
                        } else {
                            log("Ask for name non-contact");
                            fullNameAction = "Participant left";
//                            log("1-Call for nonContactName: "+ message.getUserHandle());
//                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, message.getUserHandle());
//                            megaChatApi.getUserFirstname(message.getUserHandle(), listener);
//                            megaChatApi.getUserLastname(message.getUserHandle(), listener);
                        }
                    }

                }

                if (privilege != MegaChatRoom.PRIV_RM) {
                    log("I was added");
                    textToShow = String.format(context.getString(R.string.non_format_message_add_participant), megaChatApi.getMyFullname(), fullNameAction);
                } else {
                    log("I was removed or left");
                    if (message.getUserHandle() == message.getHandleOfAction()) {
                        log("I left the chat");
                        textToShow = String.format(context.getString(R.string.non_format_message_participant_left_group_chat), megaChatApi.getMyFullname());

                    } else {
                        textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), megaChatApi.getMyFullname(), fullNameAction);
                    }
                }

                builder.append(textToShow);
                return builder.toString();

            } else {
                log("CONTACT Message type ALTER PARTICIPANTS");

                int privilege = message.getPrivilege();
                log("Privilege of the user: " + privilege);

                String fullNameTitle = getFullName(message.getHandleOfAction(), chatRoom);

                if(!fullNameTitle.isEmpty()){
                    if (fullNameTitle.trim().length() <= 0) {
                        NonContactInfo nonContact = dbH.findNonContactByHandle(message.getHandleOfAction() + "");
                        if (nonContact != null) {
                            fullNameTitle = nonContact.getFullName();
                        } else {
                            fullNameTitle = "Unknown name";
//                            log("3-Call for nonContactName: "+ message.getUserHandle());
//                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, message.getHandleOfAction());
//                        megaChatApi.getUserFirstname(message.getHandleOfAction(), listener);
//                        megaChatApi.getUserLastname(message.getHandleOfAction(), listener);
                        }
                    }
                }

                StringBuilder builder = new StringBuilder();
                builder.append(fullNameTitle + ": ");

                String textToShow = "";
                if (privilege != MegaChatRoom.PRIV_RM) {
                    log("Participant was added");
                    if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                        log("By me");
                        textToShow = String.format(context.getString(R.string.non_format_message_add_participant), fullNameTitle, megaChatApi.getMyFullname());
                    } else {
//                        textToShow = String.format(context.getString(R.string.message_add_participant), message.getHandleOfAction()+"");
                        log("By other");
                        String fullNameAction = getFullName(message.getUserHandle(), chatRoom);

                        if(!fullNameAction.isEmpty()){
                            if (fullNameAction.trim().length() <= 0) {
                                NonContactInfo nonContact = dbH.findNonContactByHandle(message.getUserHandle() + "");
                                if (nonContact != null) {
                                    fullNameAction = nonContact.getFullName();
                                } else {
                                    fullNameAction = "Unknown name";
                                    log("2-Call for nonContactName: " + message.getUserHandle());
//                                    ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, message.getUserHandle());
//                                    megaChatApi.getUserFirstname(message.getUserHandle(), listener);
//                                    megaChatApi.getUserLastname(message.getUserHandle(), listener);
                                }

                            }
                        }

                        textToShow = String.format(context.getString(R.string.non_format_message_add_participant), fullNameTitle, fullNameAction);

                    }
                }//END participant was added
                else {
                    log("Participant was removed or left");
                    if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                        textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), fullNameTitle, megaChatApi.getMyFullname());
                    } else {

                        if (message.getUserHandle() == message.getHandleOfAction()) {
                            log("The participant left the chat");

                            textToShow = String.format(context.getString(R.string.non_format_message_participant_left_group_chat), fullNameTitle);

                        } else {
                            log("The participant was removed");
                            String fullNameAction = getFullName(message.getUserHandle(), chatRoom);

                            if(!fullNameAction.isEmpty()){
                                if (fullNameAction.trim().length() <= 0) {
                                    NonContactInfo nonContact = dbH.findNonContactByHandle(message.getUserHandle() + "");
                                    if (nonContact != null) {
                                        fullNameAction = nonContact.getFullName();
                                    } else {
                                        fullNameAction = "Unknown name";
                                        log("4-Call for nonContactName: " + message.getUserHandle());
//                                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, message.getUserHandle());
//                                        megaChatApi.getUserFirstname(message.getUserHandle(), listener);
//                                        megaChatApi.getUserLastname(message.getUserHandle(), listener);
                                    }

                                }
                            }

                            textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), fullNameTitle, fullNameAction);
                        }
//                        textToShow = String.format(context.getString(R.string.message_remove_participant), message.getHandleOfAction()+"");
                    }
                } //END participant removed

                builder.append(textToShow);
                return builder.toString();

            } //END CONTACT MANAGEMENT MESSAGE
        }else if (message.getType() == MegaChatMessage.TYPE_PRIV_CHANGE) {
            log("PRIVILEGE CHANGE message");
            if (message.getHandleOfAction() == megaApi.getMyUser().getHandle()) {
                log("a moderator change my privilege");
                int privilege = message.getPrivilege();
                log("Privilege of the user: " + privilege);

                StringBuilder builder = new StringBuilder();
                builder.append(megaChatApi.getMyFullname() + ": ");

                String privilegeString = "";
                if (privilege == MegaChatRoom.PRIV_MODERATOR) {
                    privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_STANDARD) {
                    privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_RO) {
                    privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
                } else {
                    log("Change to other");
                    privilegeString = "Unknow";
                }

                String textToShow = "";

                if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                    log("I changed my Own permission");
                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), megaChatApi.getMyFullname(), privilegeString, megaChatApi.getMyFullname());
                } else {
                    log("I was change by someone");
                    String fullNameAction = getFullName(message.getUserHandle(), chatRoom);

                    if(!fullNameAction.isEmpty()){
                        if (fullNameAction.trim().length() <= 0) {
                            NonContactInfo nonContact = dbH.findNonContactByHandle(message.getUserHandle() + "");
                            if (nonContact != null) {
                                fullNameAction = nonContact.getFullName();
                            } else {
                                fullNameAction = "Unknown name";
                                log("5-Call for nonContactName: " + message.getUserHandle());
//                                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, message.getUserHandle());
//                                megaChatApi.getUserFirstname(message.getUserHandle(), listener);
//                                megaChatApi.getUserLastname(message.getUserHandle(), listener);
                            }

                        }
                    }

                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), megaChatApi.getMyFullname(), privilegeString, fullNameAction);
                }

                builder.append(textToShow);
                return builder.toString();
            } else {
                log("Participant privilege change!");
                log("Message type PRIVILEGE CHANGE - Message ID: " + message.getMsgId());

                String fullNameTitle = getFullName(message.getHandleOfAction(), chatRoom);

                if(!fullNameTitle.isEmpty()){
                    if (fullNameTitle.trim().length() <= 0) {
                        NonContactInfo nonContact = dbH.findNonContactByHandle(message.getHandleOfAction() + "");
                        if (nonContact != null) {
                            fullNameTitle = nonContact.getFullName();
                        } else {
                            fullNameTitle = "Unknown name";
                            log("6-Call for nonContactName: " + message.getUserHandle());
//                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, message.getHandleOfAction());
//                            megaChatApi.getUserFirstname(message.getHandleOfAction(), listener);
//                            megaChatApi.getUserLastname(message.getHandleOfAction(), listener);
                        }
                    }
                }

                StringBuilder builder = new StringBuilder();
                builder.append(fullNameTitle + ": ");

                int privilege = message.getPrivilege();
                String privilegeString = "";
                if (privilege == MegaChatRoom.PRIV_MODERATOR) {
                    privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_STANDARD) {
                    privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_RO) {
                    privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
                } else {
                    log("Change to other");
                    privilegeString = "Unknow";
                }

                String textToShow = "";
                if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                    log("The privilege was change by me");
                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), fullNameTitle, privilegeString, megaChatApi.getMyFullname());

                } else {
                    log("By other");
                    String fullNameAction = getFullName(message.getUserHandle(), chatRoom);

                    if(!fullNameTitle.isEmpty()){
                        if (fullNameAction.trim().length() <= 0) {
                            NonContactInfo nonContact = dbH.findNonContactByHandle(message.getUserHandle() + "");
                            if (nonContact != null) {
                                fullNameAction = nonContact.getFullName();
                            } else {
                                fullNameAction = "Unknown name";
                                log("8-Call for nonContactName: " + message.getUserHandle());
//                                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, message.getUserHandle());
//                                megaChatApi.getUserFirstname(message.getUserHandle(), listener);
//                                megaChatApi.getUserLastname(message.getUserHandle(), listener);
                            }
                        }
                    }

                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), fullNameTitle, privilegeString, fullNameAction);
                }

                builder.append(textToShow);
                return builder.toString();
            }
        }else {
            log("other type of messages");
            //OTHER TYPE OF MESSAGES
            if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                log("MY message!!:");
                log("MY message handle!!: " + message.getMsgId());

                StringBuilder builder = new StringBuilder();
                builder.append(megaChatApi.getMyFullname() + ": ");

                if (message.getType() == MegaChatMessage.TYPE_NORMAL) {
                    log("Message type NORMAL: " + message.getMsgId());

                    String messageContent = "";
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                    }

                    if (message.isEdited()) {
                        log("Message is edited");
                        String textToShow = messageContent + " " + context.getString(R.string.edited_message_text);
                        builder.append(textToShow);
                        return builder.toString();
                    } else if (message.isDeleted()) {
                        log("Message is deleted");

                        String textToShow = context.getString(R.string.text_deleted_message);
                        builder.append(textToShow);
                        return builder.toString();

                    } else {
                        builder.append(messageContent);
                        return builder.toString();
                    }
                }else if (message.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                    log("Message type TRUNCATE");

                    String textToShow = String.format(context.getString(R.string.history_cleared_by), toCDATA(megaChatApi.getMyFullname()));
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#00BFA5\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    }
                    catch (Exception e){}
                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }
                    builder.append(result);
                    return builder.toString();
                }else if (message.getType() == MegaChatMessage.TYPE_CHAT_TITLE) {
                    log("Message type TITLE CHANGE - Message ID: " + message.getMsgId());

                    String messageContent = message.getContent();
                    String textToShow = String.format(context.getString(R.string.non_format_change_title_messages), megaChatApi.getMyFullname(), messageContent);
                    builder.append(textToShow);
                    return builder.toString();

                }else if(message.getType() == MegaChatMessage.TYPE_CONTAINS_META){
                    MegaChatContainsMeta meta = message.getContainsMeta();
                    if(meta!=null && meta.getType()==MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW){
                       String text = meta.getRichPreview().getText();
                       builder.append(text);
                       return builder.toString();
                    }else{
                       return "";
                    }
                }else if(message.getType() == MegaChatMessage.TYPE_CALL_STARTED){
                    String textToShow = context.getResources().getString(R.string.call_started_messages);
                    builder.append(textToShow);
                    return builder.toString();
                }
                else if(message.getType() == MegaChatMessage.TYPE_CALL_ENDED){
                    String textToShow = "";
                    switch(message.getTermCode()){
                        case MegaChatMessage.END_CALL_REASON_ENDED:{

                            int hours = message.getDuration() / 3600;
                            int minutes = (message.getDuration() % 3600) / 60;
                            int seconds = message.getDuration() % 60;

                            textToShow = context.getString(R.string.call_ended_message);

                            if(hours != 0){
                                String textHours = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, hours, hours);
                                textToShow = textToShow + textHours;
                                if((minutes != 0)||(seconds != 0)){
                                    textToShow = textToShow+", ";
                                }
                            }

                            if(minutes != 0){
                                String textMinutes = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, minutes, minutes);
                                textToShow = textToShow + textMinutes;
                                if(seconds != 0){
                                    textToShow = textToShow+", ";
                                }
                            }

                            if(seconds != 0){
                                String textSeconds = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_seconds, seconds, seconds);
                                textToShow = textToShow + textSeconds;
                            }

                            try{
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                                textToShow = textToShow.replace("[C]", "");
                                textToShow = textToShow.replace("[/C]", "");
                            }catch (Exception e){
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_REJECTED:{

                            textToShow = String.format(context.getString(R.string.call_rejected_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_NO_ANSWER:{

                            textToShow = String.format(context.getString(R.string.call_not_answered_messages));

                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_FAILED:{

                            textToShow = String.format(context.getString(R.string.call_failed_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_CANCELLED:{

                            textToShow = String.format(context.getString(R.string.call_cancelled_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                    }

                    builder.append(textToShow);
                    return builder.toString();
                }
                else{
                    return "";
                }
            } else {
                log("Contact message!!");

                String fullNameTitle = getFullName(userHandle, chatRoom);

                if(!fullNameTitle.isEmpty()){
                    if (fullNameTitle.trim().length() <= 0) {
//                        String userHandleString = megaApi.userHandleToBase64(message.getUserHandle());
                        NonContactInfo nonContact = dbH.findNonContactByHandle(message.getUserHandle() + "");
                        if (nonContact != null) {
                            fullNameTitle = nonContact.getFullName();
                        } else {
                            fullNameTitle = "Unknown name";
//
//                                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, message.getUserHandle());
//                                megaChatApi.getUserFirstname(message.getUserHandle(), listener);
//                                megaChatApi.getUserLastname(message.getUserHandle(), listener);
                        }
                    }
                }

                StringBuilder builder = new StringBuilder();
                builder.append(fullNameTitle + ": ");

                if (message.getType() == MegaChatMessage.TYPE_NORMAL) {

                    String messageContent = "";
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                    }

                    if (message.isEdited()) {
                        log("Message is edited");

                        String textToShow = messageContent + " " + context.getString(R.string.edited_message_text);
                        builder.append(textToShow);
                        return builder.toString();
                    } else if (message.isDeleted()) {
                        log("Message is deleted");
                        String textToShow = "";
                        if(chatRoom.isGroup()){
                            textToShow = String.format(context.getString(R.string.non_format_text_deleted_message_by), fullNameTitle);
                        }
                        else{
                            textToShow = context.getString(R.string.text_deleted_message);
                        }

                        builder.append(textToShow);
                        return builder.toString();

                    } else {
                        builder.append(messageContent);
                        return builder.toString();

                    }
                } else if (message.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                    log("Message type TRUNCATE");

                    String textToShow = String.format(context.getString(R.string.non_format_history_cleared_by), fullNameTitle);
                    builder.append(textToShow);
                    return builder.toString();

                } else if (message.getType() == MegaChatMessage.TYPE_CHAT_TITLE) {
                    log("Message type CHANGE TITLE - Message ID: " + message.getMsgId());

                    String messageContent = message.getContent();

                    String textToShow = String.format(context.getString(R.string.non_format_change_title_messages), fullNameTitle, messageContent);
                    builder.append(textToShow);
                    return builder.toString();
                }else if (message.getType() == MegaChatMessage.TYPE_CONTAINS_META) {
                    MegaChatContainsMeta meta = message.getContainsMeta();
                    if(meta!=null && meta.getType()==MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW){
                        String text = meta.getRichPreview().getText();
                        builder.append(text);
                        return builder.toString();
                    }else{
                        return "";
                    }
                }else if(message.getType() == MegaChatMessage.TYPE_CALL_STARTED){
                    String textToShow = context.getResources().getString(R.string.call_started_messages);
                    builder.append(textToShow);
                    return builder.toString();
                }
                else if(message.getType() == MegaChatMessage.TYPE_CALL_ENDED){
                    String textToShow = "";
                    switch(message.getTermCode()){
                        case MegaChatMessage.END_CALL_REASON_ENDED:{

                            int hours = message.getDuration() / 3600;
                            int minutes = (message.getDuration() % 3600) / 60;
                            int seconds = message.getDuration() % 60;

                            textToShow = context.getString(R.string.call_ended_message);

                            if(hours != 0){
                                String textHours = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, hours, hours);
                                textToShow = textToShow + textHours;
                                if((minutes != 0)||(seconds != 0)){
                                    textToShow = textToShow+", ";
                                }
                            }

                            if(minutes != 0){
                                String textMinutes = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, minutes, minutes);
                                textToShow = textToShow + textMinutes;
                                if(seconds != 0){
                                    textToShow = textToShow+", ";
                                }
                            }

                            if(seconds != 0){
                                String textSeconds = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_seconds, seconds, seconds);
                                textToShow = textToShow + textSeconds;
                            }

                            try{
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                                textToShow = textToShow.replace("[C]", "");
                                textToShow = textToShow.replace("[/C]", "");
                            }catch (Exception e){
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_REJECTED:{

                            textToShow = String.format(context.getString(R.string.call_rejected_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_NO_ANSWER:{

                            textToShow = String.format(context.getString(R.string.call_missed_messages));

                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_FAILED:{

                            textToShow = String.format(context.getString(R.string.call_failed_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_CANCELLED:{

                            textToShow = String.format(context.getString(R.string.call_missed_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                    }

                    builder.append(textToShow);
                    return builder.toString();
                }
                else{
                    log("Type message: " + message.getType());
                    log("Message ID: " + message.getMsgId());
                    return "";
                }
            }
        }
    }

    public String createManagementString(AndroidMegaChatMessage androidMessage, MegaChatRoom chatRoom) {
        log("createManagementString");

        MegaChatMessage message = androidMessage.getMessage();
        return createManagementString(message, chatRoom);
    }

    public String getFirstName(long userHandle, MegaChatRoom chatRoom){
        log("getFullName: "+userHandle);
        int privilege = chatRoom.getPeerPrivilegeByHandle(userHandle);
        log("privilege is: "+privilege);
        if(privilege==MegaChatRoom.PRIV_UNKNOWN||privilege==MegaChatRoom.PRIV_RM){
            log("Not participant any more!");
            String handleString = megaApi.handleToBase64(userHandle);
            MegaUser contact = megaApi.getContact(handleString);
            if(contact!=null){
                if(contact.getVisibility()==MegaUser.VISIBILITY_VISIBLE){
                    log("Is contact!");
                    return getContactFirstName(userHandle);
                }
                else{
                    log("Old contact");
                    return getNonContactFirstName(userHandle);
                }
            }
            else{
                log("Non contact");
                return getNonContactFirstName(userHandle);
            }
        }
        else{
            log("Is participant");
            return getParticipantFirstName(userHandle, chatRoom);
        }
    }

    public String getFullName(long userHandle, long chatId){
        log("getFullName with chatID: "+userHandle);
        MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
        if(chat!=null){
            return getFullName(userHandle, chat);
        }
        else{
            log("Chat is NULL - error!");
        }
        return "";
    }

    public String getFullName(long userHandle, MegaChatRoom chatRoom){
        log("getFullName: "+userHandle);
        int privilege = chatRoom.getPeerPrivilegeByHandle(userHandle);
        log("privilege is: "+privilege);
        if(privilege==MegaChatRoom.PRIV_UNKNOWN||privilege==MegaChatRoom.PRIV_RM){
            log("Not participant any more!");
            String handleString = MegaApiJava.userHandleToBase64(userHandle);
            log("The user handle to find is: "+handleString);
            MegaUser contact = megaApi.getContact(handleString);
            if(contact!=null && contact.getVisibility()==MegaUser.VISIBILITY_VISIBLE){
                log("Is contact!");
                return getContactFullName(userHandle);
            }
            else{
                log("Non contact");
                return getNonContactFullName(userHandle);
            }
        }
        else{
            log("Is participant");
            return getParticipantFullName(userHandle, chatRoom);
        }
    }

    public String getContactFirstName(long userHandle){
        MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(userHandle));
        if(contactDB!=null){

            String name = contactDB.getName();

            if(name==null){
                name="";
            }

            if (name.trim().length() <= 0){
                String lastName = contactDB.getLastName();
                if(lastName==null){
                    lastName="";
                }
                if (lastName.trim().length() <= 0){
                    log("1- Full name empty");
                    log("2-Put email as fullname");
                    String mail = contactDB.getMail();
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
                else{
                    return lastName;
                }

            }
            else{
                return name;
            }
        }
        return "";
    }

    public String getContactFullName(long userHandle){
        log("getContactFullName");
        MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(userHandle));
        if(contactDB!=null){
            log("Contact DB found!");
            String name = contactDB.getName();
            String lastName = contactDB.getLastName();

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
                log("1- Full name empty");
                log("2-Put email as fullname");
                String mail = contactDB.getMail();
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
        return "";
    }

    public String getNonContactFirstName(long userHandle){
        NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle+"");

        if(nonContact!=null){

            String name = nonContact.getFirstName();

            if(name==null){
                name="";
            }

            if (name.trim().length() <= 0){
                String lastName = nonContact.getLastName();
                if(lastName==null){
                    lastName="";
                }
                if (lastName.trim().length() <= 0){
                    log("1- Full name empty");
                    log("2-Put email as fullname");
                    String mail = nonContact.getEmail();
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
                else{
                    return lastName;
                }

            }
            else{
                return name;
            }
        }
        return "";
    }

    public String getNonContactFullName(long userHandle){
        NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle+"");
        if(nonContact!=null) {
            String fullName = nonContact.getFullName();

            if (fullName != null && !fullName.trim().isEmpty()) {
                return fullName;
            }
            else {
                String email = nonContact.getEmail();
                if (email != null && !email.trim().isEmpty()) {
                    return email;
                }
            }
        }

        return "";
    }

    public String getParticipantFirstName(long userHandle, MegaChatRoom chatRoom){
        log("getParticipantFirstName: "+userHandle);
        String firstName = chatRoom.getPeerFirstnameByHandle(userHandle);

        if(firstName==null){
            firstName="";
        }

        if (firstName.trim().length() <= 0){
            String lastName = chatRoom.getPeerLastnameByHandle(userHandle);
            if(lastName==null){
                lastName="";
            }
            if (lastName.trim().length() <= 0){
                log("1- Full name empty");
                log("2-Put email as fullname");
                String mail = chatRoom.getPeerEmailByHandle(userHandle);
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
            else{
                return lastName;
            }

        }
        else{
            return firstName;
        }
    }

    public String getParticipantFullName(long userHandle, MegaChatRoom chatRoom){
        log("getParticipantFullName: "+userHandle);
        String fullName = chatRoom.getPeerFullnameByHandle(userHandle);

        if(fullName!=null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        else {
            log("1-Put email as fullname");
            String participantEmail = chatRoom.getPeerEmailByHandle(userHandle);
            return participantEmail;
        }
    }

    public String getMyFullName(){

        String fullName = megaChatApi.getMyFullname();

        if(fullName!=null){
            if(fullName.isEmpty()){
                log("1-Put MY email as fullname");
                String myEmail = megaChatApi.getMyEmail();
                String[] splitEmail = myEmail.split("[@._]");
                fullName = splitEmail[0];
                return fullName;
            }
            else{
                if (fullName.trim().length() <= 0){
                    log("2-Put MY email as fullname");
                    String myEmail = megaChatApi.getMyEmail();
                    String[] splitEmail = myEmail.split("[@._]");
                    fullName = splitEmail[0];
                    return fullName;
                }
                else{
                    return fullName;
                }
            }
        }
        else{
            log("3-Put MY  email as fullname");
            String myEmail = megaChatApi.getMyEmail();
            String[] splitEmail = myEmail.split("[@._]");
            fullName = splitEmail[0];
            return fullName;
        }
    }

    public void pickFileToSend(){
        log("pickFileToSend");
        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_MULTISELECT_FILE);
//        ArrayList<String> longArray = new ArrayList<String>();
//        for (int i=0; i<users.size(); i++){
//            longArray.add(users.get(i).getEmail());
//        }
//        intent.putStringArrayListExtra("SELECTED_CONTACTS", longArray);
        ((ChatActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_FILE);
    }

    public void saveForOfflineWithMessages(ArrayList<MegaChatMessage> messages, MegaChatRoom chatRoom){
        log("saveForOffline - multiple messages");
        for(int i=0; i<messages.size();i++){
            saveForOffline(messages.get(i).getMegaNodeList(), chatRoom);
        }
    }

    public void saveForOfflineWithAndroidMessages(ArrayList<AndroidMegaChatMessage> messages, MegaChatRoom chatRoom){
        log("saveForOffline - multiple messages");
        for(int i=0; i<messages.size();i++){
            saveForOffline(messages.get(i).getMessage().getMegaNodeList(), chatRoom);
        }
    }

    public void saveForOffline(MegaNodeList nodeList, MegaChatRoom chatRoom){

        File destination = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                if (context instanceof ChatActivityLollipop) {
                    ActivityCompat.requestPermissions(((ChatActivityLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_WRITE_STORAGE);
                }
                else if (context instanceof ChatFullScreenImageViewer){
                    ActivityCompat.requestPermissions(((ChatFullScreenImageViewer) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_WRITE_STORAGE);
                }
                else if (context instanceof PdfViewerActivityLollipop){
                    ActivityCompat.requestPermissions(((PdfViewerActivityLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_WRITE_STORAGE);
                }
                else if (context instanceof AudioVideoPlayerLollipop){
                    ActivityCompat.requestPermissions(((AudioVideoPlayerLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_WRITE_STORAGE);
                }
            }
        }

        Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
        for (int i = 0; i < nodeList.size(); i++) {

            MegaNode document = nodeList.get(i);
            if (document != null) {
                document = authorizeNodeIfPreview(document, chatRoom);
                destination = getOfflineParentFile(context, Constants.FROM_OTHERS, document, null);
                destination.mkdirs();

                log ("DESTINATION!!!!!: " + destination.getAbsolutePath());
                if (isFileAvailable(destination) && destination.isDirectory()){

                    File offlineFile = new File(destination, document.getName());
                    if (offlineFile.exists() && document.getSize() == offlineFile.length() && offlineFile.getName().equals(document.getName())){ //This means that is already available offline
                        log("File already exists!");
                        showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.file_already_exists));
                    }
                    else{
                        dlFiles.put(document, destination.getAbsolutePath());
                    }
                }
                else{
                    log("Destination ERROR");
                }
            }
        }

        double availableFreeSpace = Double.MAX_VALUE;
        try{
            StatFs stat = new StatFs(destination.getAbsolutePath());
            availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
        }
        catch(Exception ex){}

        for (MegaNode document : dlFiles.keySet()) {

            String path = dlFiles.get(document);

            if(availableFreeSpace <document.getSize()){
                Util.showErrorAlertDialog(context.getString(R.string.error_not_enough_free_space) + " (" + new String(document.getName()) + ")", false, ((ChatActivityLollipop) context));
                continue;
            }

            Intent service = new Intent(context, DownloadService.class);
            document = authorizeNodeIfPreview(document, chatRoom);
            String serializeString = document.serialize();
            log("serializeString: "+serializeString);
            service.putExtra(DownloadService.EXTRA_SERIALIZE_STRING, serializeString);
            service.putExtra(DownloadService.EXTRA_PATH, path);
            if (context instanceof AudioVideoPlayerLollipop || context instanceof PdfViewerActivityLollipop || context instanceof ChatFullScreenImageViewer){
                service.putExtra("fromMV", true);
            }
            context.startService(service);
        }

    }

    void showSnackbar(int type, String s) {
        if (context instanceof ChatFullScreenImageViewer){
            ((ChatFullScreenImageViewer) context).showSnackbar(type, s);
        }
        else if (context instanceof AudioVideoPlayerLollipop){
            ((AudioVideoPlayerLollipop) context).showSnackbar(type, s, -1);
        }
        else if (context instanceof PdfViewerActivityLollipop){
            ((PdfViewerActivityLollipop) context).showSnackbar(type, s, -1);
        }
        else if (context instanceof ChatActivityLollipop){
            ((ChatActivityLollipop) context).showSnackbar(type, s, -1);
        }
        else if (context instanceof NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).showSnackbar(type, s);
        }
    }

    void requestLocalFolder (long size, ArrayList<String> serializedNodes) {
        Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, context.getString(R.string.general_select));
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
        intent.setClass(context, FileStorageActivityLollipop.class);
        intent.putStringArrayListExtra(FileStorageActivityLollipop.EXTRA_SERIALIZED_NODES,serializedNodes);

        if(context instanceof ChatActivityLollipop){
            ((ChatActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof ChatFullScreenImageViewer){
            ((ChatFullScreenImageViewer) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof PdfViewerActivityLollipop){
            ((PdfViewerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof AudioVideoPlayerLollipop){
            ((AudioVideoPlayerLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
    }

    private void filePathDefault(String path, final ArrayList<MegaNode> nodeList){
        log("filePathDefault");
        File defaultPathF = new File(path);
        defaultPathF.mkdirs();
        checkSizeBeforeDownload(path, nodeList);
    }

    public void prepareForChatDownload(ArrayList<MegaNodeList> list){
        log("prepareForChatDownload 1");
        ArrayList<MegaNode> nodeList =  new ArrayList<>();
        MegaNodeList megaNodeList;
        for (int i= 0; i<list.size(); i++){
            megaNodeList = list.get(i);
            for (int j=0; j<megaNodeList.size(); j++){
                nodeList.add(megaNodeList.get(j));
            }
        }
        prepareForDownloadVersions(nodeList);
    }

    public void prepareForChatDownload(MegaNodeList list){
        ArrayList<MegaNode> nodeList = MegaApiJava.nodeListToArray(list);
        prepareForDownloadVersions(nodeList);
    }

    public void prepareForChatDownload(MegaNode node){
        log("prepareForChatDownload - node ");
        ArrayList<MegaNode> nodeList = new ArrayList<>();
        nodeList.add(node);
        prepareForDownloadVersions(nodeList);
    }

    private ArrayList<String> serializeNodes(ArrayList<MegaNode> nodeList) {
        ArrayList<String> serializedNodes = new ArrayList<>();
        for(MegaNode node : nodeList) {
            serializedNodes.add(node.serialize());
        }
        return serializedNodes;
    }

    private ArrayList<MegaNode> unSerializeNodes(ArrayList<String> serializedNodes) {
        ArrayList<MegaNode> nodeList = new ArrayList<>();
        if(serializedNodes != null) {
            for(String nodeString : serializedNodes) {
                nodeList.add(MegaNode.unserialize(nodeString));
            }
        }
        return nodeList;
    }

    public void prepareForDownload(Intent intent, String parentPath) {
        ArrayList<String> serializedNodes = intent.getStringArrayListExtra(FileStorageActivityLollipop.EXTRA_SERIALIZED_NODES);
        ArrayList<MegaNode> megaNodes = unSerializeNodes(serializedNodes);
        if (megaNodes.size() > 0) {
            checkSizeBeforeDownload(parentPath, megaNodes);
        }
    }

    private void prepareForDownloadVersions(final ArrayList<MegaNode> nodeList){
        log("prepareForDownloadLollipop: "+nodeList.size()+" files to download, ");
        long size = 0;
        for (int i = 0; i < nodeList.size(); i++) {
            size += nodeList.get(i).getSize();
        }
        log("Number of files: " + nodeList.size());

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        String downloadLocationDefaultPath = getDownloadLocation(context);

        if(!nodeList.isEmpty() && ChatUtil.isVoiceClip(nodeList.get(0).getName())){
            File vcFile = buildVoiceClipFile(context, nodeList.get(0).getName());
            checkSizeBeforeDownload(vcFile.getParentFile().getPath(), nodeList);
            return;
        }

        boolean askMe = Util.askMe(context);
        if (askMe){
            log("askMe");
            File[] fs = context.getExternalFilesDirs(null);
            final ArrayList<String> serializedNodes = serializeNodes(nodeList);
            if (fs.length > 1){
                if (fs[1] == null){
                    requestLocalFolder(size, serializedNodes);
                }
                else{
                    Dialog downloadLocationDialog;
                    String[] sdCardOptions = context.getResources().getStringArray(R.array.settings_storage_download_location_array);
                    AlertDialog.Builder b=new AlertDialog.Builder(context);

                    b.setTitle(context.getResources().getString(R.string.settings_storage_download_location));
                    final long sizeFinal = size;

                    b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch(which){
                                case 0:{
                                    requestLocalFolder(sizeFinal, serializedNodes);
                                    break;
                                }
                                case 1:{
                                    File[] fs = context.getExternalFilesDirs(null);
                                    if (fs.length > 1){
                                        String path = fs[1].getAbsolutePath();
                                        File defaultPathF = new File(path);
                                        defaultPathF.mkdirs();
                                        showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.general_save_to_device) + ": "  + defaultPathF.getAbsolutePath());
                                        checkSizeBeforeDownload(path, nodeList);
                                    }
                                    break;
                                }
                            }
                        }
                    });
                    b.setNegativeButton(context.getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    downloadLocationDialog = b.create();
                    downloadLocationDialog.show();
                }
            }
            else{
                requestLocalFolder(size, serializedNodes);
            }
        }
        else{
            log("NOT askMe");
            filePathDefault(downloadLocationDefaultPath,nodeList);
        }
    }

    public void checkSizeBeforeDownload(String parentPath, ArrayList<MegaNode> nodeList){
        //Variable size is incorrect for folders, it is always -1 -> sizeTemp calculates the correct size
        log("checkSizeBeforeDownload -  size: "+nodeList.size());

        final String parentPathC = parentPath;
        long sizeTemp=0;
        long[] hashes = new long[nodeList.size()];

        for (int i=0;i<nodeList.size();i++) {
            MegaNode node = nodeList.get(i);
            hashes[i] = node.getHandle();
            if(node!=null){
                sizeTemp = sizeTemp+node.getSize();
            }
        }

        final long sizeC = sizeTemp;
        log("the final size is: "+Util.getSizeString(sizeTemp));

        //Check if there is available space
        double availableFreeSpace = Double.MAX_VALUE;
        try {
            StatFs stat = new StatFs(parentPath);
            availableFreeSpace = stat.getAvailableBytes();
        } catch (Exception ex) { }

        log("availableFreeSpace: " + availableFreeSpace + "__ sizeToDownload: " + sizeC);
        if(availableFreeSpace < sizeC) {
            showSnackbar(Constants.NOT_SPACE_SNACKBAR_TYPE, null);
            log("Not enough space");
            return;
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        if(ChatUtil.isVoiceClip(nodeList.get(0).getName())){
            download(parentPath, nodeList);
            return;
        }


        String ask=dbH.getAttributes().getAskSizeDownload();

        if (ask==null) {
            ask="true";
        }

        if (ask.equals("false")) {
            log("SIZE: Do not ask before downloading");
            download(parentPathC, nodeList);
        }
        else{
            log("SIZE: Ask before downloading");
            if (sizeC>104857600) {
                log("Show size confirmation: "+sizeC);
                //Show alert
                if (context instanceof ChatActivityLollipop) {
                    ((ChatActivityLollipop) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
                else if (context instanceof ChatFullScreenImageViewer) {
                    ((ChatFullScreenImageViewer) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
                else if (context instanceof PdfViewerActivityLollipop) {
                    ((PdfViewerActivityLollipop) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
                else if (context instanceof AudioVideoPlayerLollipop) {
                    ((AudioVideoPlayerLollipop) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
                else if (context instanceof NodeAttachmentHistoryActivity) {
                    ((NodeAttachmentHistoryActivity) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
            }
            else {
                download(parentPathC, nodeList);
            }
        }
    }

    public void download(String parentPath, ArrayList<MegaNode> nodeList){
        log("download() ");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                if (context instanceof ManagerActivityLollipop) {
                    ActivityCompat.requestPermissions(((ManagerActivityLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_WRITE_STORAGE);
                }else if (context instanceof ChatFullScreenImageViewer){
                    ActivityCompat.requestPermissions(((ChatFullScreenImageViewer) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_WRITE_STORAGE);
                }else if (context instanceof ChatActivityLollipop){
                    ActivityCompat.requestPermissions(((ChatActivityLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_WRITE_STORAGE);
                }
                else if (context instanceof PdfViewerActivityLollipop){
                    ActivityCompat.requestPermissions(((PdfViewerActivityLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_WRITE_STORAGE);
                }
                else if (context instanceof AudioVideoPlayerLollipop){
                    ActivityCompat.requestPermissions(((AudioVideoPlayerLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_WRITE_STORAGE);
                }
            }
        }

        if (nodeList != null){
            if(nodeList.size() == 1){
                log("hashes.length == 1");
                MegaNode tempNode = nodeList.get(0);
                if (context instanceof ChatActivityLollipop) {
                    tempNode = authorizeNodeIfPreview(tempNode, ((ChatActivityLollipop) context).getChatRoom());
                }
                else if (context instanceof NodeAttachmentHistoryActivity) {
                    tempNode = authorizeNodeIfPreview(tempNode, ((NodeAttachmentHistoryActivity) context).getChatRoom());
                }
                if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
                    log("ISFILE");
                    String localPath = getLocalFile(context, tempNode.getName(), tempNode.getSize(), parentPath);

                    //Check if the file is already downloaded
                    MegaApplication app = ((MegaApplication) ((Activity)context).getApplication());
                    if(localPath != null){
                        log("localPath != null");
                        try {
                            log("download:Call to copyFile: localPath: ");
                            copyFile(new File(localPath), new File(parentPath, tempNode.getName()));

                            if(isVideoFile(parentPath+"/"+tempNode.getName())){
                                log("Is video!!!");
                                if (tempNode != null){
                                    if(!tempNode.hasThumbnail()){
                                        log("The video has not thumb");
                                        ThumbnailUtilsLollipop.createThumbnailVideo(context, localPath, megaApi, tempNode.getHandle());
                                    }
                                }
                            }
                            else{
                                log("NOT video!");
                            }
                        }
                        catch(Exception e) {
                            log("Exception!!");
                        }
                        boolean autoPlayEnabled = Boolean.parseBoolean(dbH.getAutoPlayEnabled());
                        if(!autoPlayEnabled){
                            log("auto play disabled");
                            Util.showSnackBar(context,Constants.SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded),-1);
                            return;
                        }

                        if(ChatUtil.isVoiceClip(nodeList.get(0).getName())) return;

                        if(MimeTypeList.typeForName(tempNode.getName()).isZip()){
                            log("MimeTypeList ZIP");
                            File zipFile = new File(localPath);

                            Intent intentZip = new Intent();
                            intentZip.setClass(context, ZipBrowserActivityLollipop.class);
                            intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, zipFile.getAbsolutePath());
                            intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_HANDLE_ZIP, tempNode.getHandle());

                            context.startActivity(intentZip);

                        }
                        else if (MimeTypeList.typeForName(tempNode.getName()).isVideoReproducible() || MimeTypeList.typeForName(tempNode.getName()).isAudio()) {
                            log("download:Video/Audio file");
                            if (context instanceof AudioVideoPlayerLollipop){
                                ((AudioVideoPlayerLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded), -1);
                            }
                            else {
                                File mediaFile = new File(localPath);

                                Intent mediaIntent;
                                boolean internalIntent;
                                boolean opusFile = false;
                                if (MimeTypeList.typeForName(tempNode.getName()).isVideoNotSupported() || MimeTypeList.typeForName(tempNode.getName()).isAudioNotSupported()){
                                    mediaIntent = new Intent(Intent.ACTION_VIEW);
                                    internalIntent = false;
                                    String[] s = tempNode.getName().split("\\.");
                                    if (s != null && s.length > 1 && s[s.length-1].equals("opus")) {
                                        opusFile = true;
                                    }
                                }
                                else {
                                    internalIntent = true;
                                    mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
                                }
                                mediaIntent.putExtra("isPlayList", false);
                                mediaIntent.putExtra("HANDLE", tempNode.getHandle());
                                mediaIntent.putExtra("adapterType", Constants.FROM_CHAT);
                                mediaIntent.putExtra(AudioVideoPlayerLollipop.PLAY_WHEN_READY,app.isActivityVisible());
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                    mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                else{
                                    mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                if (opusFile){
                                    mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
                                }
                                if (internalIntent) {
                                    context.startActivity(mediaIntent);
                                }
                                else {
                                    if (MegaApiUtils.isIntentAvailable(context, mediaIntent)){
                                        context.startActivity(mediaIntent);
                                    }
                                    else {
                                        showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.intent_not_available));
                                        Intent intentShare = new Intent(Intent.ACTION_SEND);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                            intentShare.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                        }
                                        else {
                                            intentShare.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                        }
                                        intentShare.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        if (MegaApiUtils.isIntentAvailable(context, intentShare)) {
                                            log("call to startActivity(intentShare)");
                                            context.startActivity(intentShare);
                                        }
                                    }
                                }
                            }
                        }
                        else if (MimeTypeList.typeForName(tempNode.getName()).isPdf()){
                            log("Pdf file");
                            if (context instanceof PdfViewerActivityLollipop){
                                ((PdfViewerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded), -1);
                            }
                            else {
                                File pdfFile = new File(localPath);

                                Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);
                                pdfIntent.putExtra("inside", true);
                                pdfIntent.putExtra("HANDLE", tempNode.getHandle());
                                pdfIntent.putExtra("adapterType", Constants.FROM_CHAT);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                    pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", pdfFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                else{
                                    pdfIntent.setDataAndType(Uri.fromFile(pdfFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                context.startActivity(pdfIntent);
                            }
                        }
                        else {
                            log("MimeTypeList other file");

                            if(context instanceof ChatFullScreenImageViewer){
                                ((ChatFullScreenImageViewer) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded));
                            }
                            else {
                                try {
                                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        viewIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                    } else {
                                        viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                    }
                                    viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    if (MegaApiUtils.isIntentAvailable(context, viewIntent)) {
                                        log("if isIntentAvailable");
                                        context.startActivity(viewIntent);
                                    } else {
                                        log("ELSE isIntentAvailable");
                                        Intent intentShare = new Intent(Intent.ACTION_SEND);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            intentShare.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                        } else {
                                            intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                        }
                                        intentShare.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        if (MegaApiUtils.isIntentAvailable(context, intentShare)) {
                                            log("call to startActivity(intentShare)");
                                            context.startActivity(intentShare);
                                        }
                                        showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded));
                                    }
                                }
                                catch (Exception e){
                                    showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded));
                                }
                            }

                        }
                        return;
                    }//localPath found
                    else{
                        log("download:localPath is NULL");
                    }
                }
            }

            for (int i=0; i<nodeList.size();i++) {
                MegaNode nodeToDownload = nodeList.get(i);
                if(nodeToDownload != null){
                    log("download:node NOT null is going to donwload");
                    Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
                    dlFiles.put(nodeToDownload, parentPath);

                    for (MegaNode document : dlFiles.keySet()) {
                        String path = dlFiles.get(document);
                        Intent service = new Intent(context, DownloadService.class);
                        if (context instanceof ChatActivityLollipop) {
                            nodeToDownload = authorizeNodeIfPreview(nodeToDownload, ((ChatActivityLollipop) context).getChatRoom());
                        }
                        else if (context instanceof NodeAttachmentHistoryActivity) {
                            nodeToDownload = authorizeNodeIfPreview(nodeToDownload, ((NodeAttachmentHistoryActivity) context).getChatRoom());
                        }
                        String serializeString = nodeToDownload.serialize();

                        if(ChatUtil.isVoiceClip(nodeList.get(0).getName())){
                            service.putExtra(DownloadService.EXTRA_OPEN_FILE, false);
                            service.putExtra(Constants.EXTRA_TRANSFER_TYPE, Constants.EXTRA_VOICE_CLIP );
                        }else if (context instanceof AudioVideoPlayerLollipop || context instanceof PdfViewerActivityLollipop || context instanceof ChatFullScreenImageViewer){
                            service.putExtra("fromMV", true);
                        }

                        log("serializeString: "+serializeString);
                        service.putExtra(DownloadService.EXTRA_SERIALIZE_STRING, serializeString);
                        service.putExtra(DownloadService.EXTRA_PATH, path);
                        service.putExtra(Constants.HIGH_PRIORITY_TRANSFER, true);
                        context.startService(service);


                    }
                }
                else {
                    log("node NOT fOUND!!!!!");
                }
            }
        }
    }

    public void importNode(long idMessage, long idChat) {
        log("importNode");
        ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
        MegaChatMessage m = getMegaChatMessage(context, megaChatApi, idChat, idMessage);

        if(m!=null){
            AndroidMegaChatMessage aMessage = new AndroidMegaChatMessage(m);
            messages.add(aMessage);
            importNodesFromAndroidMessages(messages);
        }
        else{
            log("Message cannot be recovered - null");
        }
    }

    public void importNodesFromMessages(ArrayList<MegaChatMessage> messages){
        log("importNodesFromMessages");

        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);

        long[] longArray = new long[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            longArray[i] = messages.get(i).getMsgId();
        }
        intent.putExtra("HANDLES_IMPORT_CHAT", longArray);

        if(context instanceof  NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER);
        }
    }

    public void importNodesFromAndroidMessages(ArrayList<AndroidMegaChatMessage> messages){
        log("importNodesFromAndroidMessages");

        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);

        long[] longArray = new long[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            longArray[i] = messages.get(i).getMessage().getMsgId();
        }
        intent.putExtra("HANDLES_IMPORT_CHAT", longArray);

        if(context instanceof  ChatActivityLollipop){
            ((ChatActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER);
        }
        else if(context instanceof  NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER);
        }
    }

    public void prepareMessageToForward(long idMessage, long idChat) {
        log("prepareMessageToForward");
        ArrayList<MegaChatMessage> messagesSelected = new ArrayList<>();
        MegaChatMessage m = getMegaChatMessage(context, megaChatApi, idChat, idMessage);

        if(m!=null){
            messagesSelected.add(m);

            prepareMessagesToForward(messagesSelected, idChat);
        }
        else{
            log("Message null");
        }
    }

    public void prepareAndroidMessagesToForward(ArrayList<AndroidMegaChatMessage> androidMessagesSelected, long idChat){
        ArrayList<MegaChatMessage> messagesSelected = new ArrayList<>();
        for(int i = 0; i<androidMessagesSelected.size(); i++){
            messagesSelected.add(androidMessagesSelected.get(i).getMessage());
        }
        prepareMessagesToForward(messagesSelected, idChat);
    }

    public void prepareMessagesToForward(ArrayList<MegaChatMessage> messagesSelected, long idChat){
        log ("prepareMessagesToForward");

        ArrayList<MegaChatMessage> messagesToImport = new ArrayList<>();
        long[] idMessages = new long[messagesSelected.size()];
        for(int i=0; i<messagesSelected.size();i++){
            idMessages[i] = messagesSelected.get(i).getMsgId();

            log("Type of message: "+ messagesSelected.get(i).getType());
            if((messagesSelected.get(i).getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT)||(messagesSelected.get(i).getType()==MegaChatMessage.TYPE_VOICE_CLIP)){
                if(messagesSelected.get(i).getUserHandle()!=megaChatApi.getMyUserHandle()){
                    //Node has to be imported
                    messagesToImport.add(messagesSelected.get(i));
                }
            }
        }

        if(messagesToImport.isEmpty()){
            log("Proceed to forward");
            forwardMessages(messagesSelected, idChat);
        }
        else{
            log("Proceed to import nodes to own Cloud");
            ChatImportToForwardListener listener = new ChatImportToForwardListener(Constants.MULTIPLE_FORWARD_MESSAGES, messagesSelected, messagesToImport.size(), context, this, idChat);
    
            MegaNode target = megaApi.getNodeByPath(Constants.CHAT_FOLDER, megaApi.getRootNode());
            if(target==null){
                log("Error no chat folder - return");
                return;
            }

            for(int j=0; j<messagesToImport.size();j++){
                MegaChatMessage message = messagesToImport.get(j);

                if(message!=null){

                    MegaNodeList nodeList = message.getMegaNodeList();

                    for(int i=0;i<nodeList.size();i++){
                        MegaNode document = nodeList.get(i);
                        if (document != null) {
                            log("DOCUMENT: " + document.getName() + "_" + document.getHandle());
                            document = authorizeNodeIfPreview(document, megaChatApi.getChatRoom(idChat));
                            megaApi.copyNode(document, target, listener);
                        }
                        else{
                            log("DOCUMENT: null");
                        }
                    }
                }
                else{
                    log("MESSAGE is null");
                    showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.messages_forwarded_error));
                }
            }
        }
    }

    public void forwardMessages(ArrayList<MegaChatMessage> messagesSelected, long idChat){
        log ("forwardMessages");

        long[] idMessages = new long[messagesSelected.size()];
        for(int i=0; i<messagesSelected.size();i++){
            idMessages[i] = messagesSelected.get(i).getMsgId();
        }

        Intent i = new Intent(context, ChatExplorerActivity.class);
        i.putExtra("ID_MESSAGES", idMessages);
        i.putExtra("ID_CHAT_FROM", idChat);
        i.setAction(Constants.ACTION_FORWARD_MESSAGES);
        if(context instanceof  ChatActivityLollipop){
            ((ChatActivityLollipop) context).startActivityForResult(i, Constants.REQUEST_CODE_SELECT_CHAT);
        }
        else if(context instanceof  NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(i, Constants.REQUEST_CODE_SELECT_CHAT);
        }
    }

    public MegaNode authorizeNodeIfPreview (MegaNode node, MegaChatRoom chatRoom) {
        if (chatRoom != null && chatRoom.isPreview()) {
            MegaNode nodeAuthorized = megaApi.authorizeChatNode(node, chatRoom.getAuthorizationToken());
            if (nodeAuthorized != null) {
                log("authorizeNodeIfPreview authorized");
                return nodeAuthorized;
            }
        }
        log("authorizeNodeIfPreview NOT authorized");
        return node;
    }

    public boolean isInAnonymousMode () {
        if (megaChatApi.getInitState() == MegaChatApi.INIT_ANONYMOUS) {
            return true;
        }

        return false;
    }

    public boolean isPreview (MegaChatRoom chatRoom) {
        if (chatRoom != null) {
            return chatRoom.isPreview();
        }

        return false;
    }

    public static void log(String message) {
        Util.log("ChatController", message);
    }
}
