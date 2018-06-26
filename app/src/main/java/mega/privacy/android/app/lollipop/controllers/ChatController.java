package mega.privacy.android.app.lollipop.controllers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
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
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatFullScreenImageViewer;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NonContactInfo;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaUser;

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

    public void deleteMessages(ArrayList<AndroidMegaChatMessage> messages, MegaChatRoom chat){
        log("deleteMessages: "+messages.size());
        if(messages!=null){
            for(int i=0; i<messages.size();i++){
                deleteMessage(messages.get(i).getMessage(), chat.getChatId());
            }
        }
    }

    public void deleteMessageById(long messageId, long chatId) {
        log("deleteMessage");
        MegaChatMessage message = megaChatApi.getMessage(chatId, messageId);
        if(message!=null){
            deleteMessage(message, chatId);
        }
    }

    public void deleteMessage(MegaChatMessage message, long chatId){
        log("deleteMessage");
        MegaChatMessage messageToDelete;
        if(message!=null){

            if(message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                log("Delete node attachment message");
                megaChatApi.revokeAttachmentMessage(chatId, message.getMsgId());
            }
            else{
                log("Delete normal message");
                messageToDelete = megaChatApi.deleteMessage(chatId, message.getMsgId());
                if(messageToDelete==null){
                    log("The message cannot be deleted");
                }
            }
        }
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
        megaChatApi.setChatTitle(chatid, title, (GroupChatInfoActivityLollipop) context);
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

            ChatSettings chatSettings = dbH.getChatSettings();
            if(chatSettings==null){

                chatPrefs = new ChatItemPreferences(Long.toString(chatHandle), Boolean.toString(false), "", "");
                dbH.setChatItemPreferences(chatPrefs);
            }
            else{
                String sound = chatSettings.getNotificationsSound();
                Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE);

                chatPrefs = new ChatItemPreferences(Long.toString(chatHandle), Boolean.toString(false), defaultRingtoneUri.toString(), sound);
                dbH.setChatItemPreferences(chatPrefs);
            }
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
            chatPrefs = new ChatItemPreferences(Long.toString(chatHandle), Boolean.toString(true), "", "");
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
        String text = createManagementString(androidMessage, chatRoom);
        text = text.substring(text.indexOf(":")+2);
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
        } else if (message.getType() == MegaChatMessage.TYPE_PRIV_CHANGE) {
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
                log("Message type PRIVILEGE CHANGE: " + message.getContent());

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
        } else {
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
                } else if (message.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                    log("Message type TRUNCATE");

                    String textToShow = String.format(context.getString(R.string.history_cleared_by), megaChatApi.getMyFullname());
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
                } else if (message.getType() == MegaChatMessage.TYPE_CHAT_TITLE) {
                    log("Message type TITLE CHANGE: " + message.getContent());

                    String messageContent = message.getContent();
                    String textToShow = String.format(context.getString(R.string.non_format_change_title_messages), megaChatApi.getMyFullname(), messageContent);
                    builder.append(textToShow);
                    return builder.toString();

                } else {
                    log("Type message: " + message.getType());
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
                    log("Message type CHANGE TITLE " + message.getContent());

                    String messageContent = message.getContent();

                    String textToShow = String.format(context.getString(R.string.non_format_change_title_messages), fullNameTitle, messageContent);
                    builder.append(textToShow);
                    return builder.toString();
                } else {
                    log("Type message: " + message.getType());
                    log("Content: " + message.getContent());
                    return "";
                }
            }
        }
    }

    public String createManagementString(AndroidMegaChatMessage androidMessage, MegaChatRoom chatRoom) {
        log("createManagementString with AndroidMessage");

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
            String handleString = megaApi.handleToBase64(userHandle);
            log("The user handle to find is: "+handleString);
            MegaUser contact = megaApi.getContact(handleString);
            if(contact!=null){
                if(contact.getVisibility()==MegaUser.VISIBILITY_VISIBLE){
                    log("Is contact!");
                    return getContactFullName(userHandle);
                }
                else{
                    log("Old contact");
                    return getNonContactFullName(userHandle);
                }
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
        if(nonContact!=null){
            String fullName = nonContact.getFullName();

            if(fullName!=null){
                if(fullName.isEmpty()){
                    log("1-Put email as fullname");
                    String email = nonContact.getEmail();
                    if(email!=null){
                        if(email.isEmpty()){
                            return "";
                        }
                        else{
                            if (email.trim().length() <= 0){
                                return "";
                            }
                            else{
                                return email;
                            }
                        }
                    }
                    else{
                        return "";
                    }
                }
                else{
                    if (fullName.trim().length() <= 0){
                        log("2-Put email as fullname");
                        String email = nonContact.getEmail();
                        if(email!=null){
                            if(email.isEmpty()){
                                return "";
                            }
                            else{
                                if (email.trim().length() <= 0){
                                    return "";
                                }
                                else{
                                    return email;
                                }
                            }
                        }
                        else{
                            return "";
                        }
                    }
                    else{
                        return fullName;
                    }
                }
            }
            else{
                log("3-Put email as fullname");
                String email = nonContact.getEmail();
                if(email!=null){
                    if(email.isEmpty()){
                        return "";
                    }
                    else{
                        if (email.trim().length() <= 0){
                            return "";
                        }
                        else{
                            return email;
                        }
                    }
                }
                else{
                    return "";
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

        if(fullName!=null){
            if(fullName.isEmpty()){
                log("1-Put email as fullname");
                String participantEmail = chatRoom.getPeerEmailByHandle(userHandle);
                return participantEmail;
            }
            else{
                if (fullName.trim().length() <= 0){
                    log("2-Put email as fullname");
                    String participantEmail = chatRoom.getPeerEmailByHandle(userHandle);
                    return participantEmail;
                }
                else{
                    return fullName;
                }
            }
        }
        else{
            log("3-Put email as fullname");
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

    public void saveForOffline(MegaChatMessage message){
        log("saveForOffline - message");

        File destination = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                ActivityCompat.requestPermissions(((ChatActivityLollipop) context),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.REQUEST_WRITE_STORAGE);
            }
        }

        MegaNodeList nodeList = message.getMegaNodeList();
        Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
        for (int i = 0; i < nodeList.size(); i++) {

            MegaNode document = nodeList.get(i);
            if (document != null) {

                if (Environment.getExternalStorageDirectory() != null){
                    destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/"+MegaApiUtils.createStringTree(document, context));
                }
                else{
                    destination = ((ChatActivityLollipop) context).getFilesDir();
                }

                destination.mkdirs();

                log ("DESTINATION!!!!!: " + destination.getAbsolutePath());
                if (destination.exists() && destination.isDirectory()){

                    File offlineFile = new File(destination, document.getName());
                    if (offlineFile.exists() && document.getSize() == offlineFile.length() && offlineFile.getName().equals(document.getName())){ //This means that is already available offline
                        log("File already exists!");
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
            String serializeString = document.serialize();
            log("serializeString: "+serializeString);
            service.putExtra(DownloadService.EXTRA_SERIALIZE_STRING, serializeString);
            service.putExtra(DownloadService.EXTRA_PATH, path);
            context.startService(service);
        }
    }

    public void saveForOffline(MegaNode node){
        log("saveForOffline - node");

        File destination = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                ActivityCompat.requestPermissions(((ChatActivityLollipop) context),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.REQUEST_WRITE_STORAGE);
            }
        }

        Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
        if (node != null) {

            if (Environment.getExternalStorageDirectory() != null){
                destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/"+MegaApiUtils.createStringTree(node, context));
            }
            else{
                destination = context.getFilesDir();
            }

            destination.mkdirs();

            log ("DESTINATION!!!!!: " + destination.getAbsolutePath());
            if (destination.exists() && destination.isDirectory()){

                File offlineFile = new File(destination, node.getName());
                if (offlineFile.exists() && node.getSize() == offlineFile.length() && offlineFile.getName().equals(node.getName())){ //This means that is already available offline
                    log("File already exists!");
                }
                else{
                    dlFiles.put(node, destination.getAbsolutePath());
                }
            }
            else{
                log("Destination ERROR");
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
                Util.showErrorAlertDialog(context.getString(R.string.error_not_enough_free_space) + " (" + new String(document.getName()) + ")", false, ((NodeAttachmentActivityLollipop) context));
                continue;
            }

            Intent service = new Intent(context, DownloadService.class);
            String serializeString = document.serialize();
            log("serializeString: "+serializeString);
            service.putExtra(DownloadService.EXTRA_SERIALIZE_STRING, serializeString);
            service.putExtra(DownloadService.EXTRA_PATH, path);
            context.startService(service);
        }

    }

    public void prepareForDownloadLollipop(ArrayList<MegaNode> nodeList){
        log("prepareForDownload: "+nodeList.size()+" files to download");

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        String downloadLocationDefaultPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.downloadDIR;
        prefs = dbH.getPreferences();
        if (prefs != null){
            log("prefs != null");
            if (prefs.getStorageAskAlways() != null){
                if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
                    log("askMe==false");
                    if (prefs.getStorageDownloadLocation() != null){
                        if (prefs.getStorageDownloadLocation().compareTo("") != 0){
                            downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
                        }
                    }
                }
            }
        }

        File defaultPathF = new File(downloadLocationDefaultPath);
        defaultPathF.mkdirs();
        checkSizeBeforeDownload(downloadLocationDefaultPath, nodeList);
    }

    public void prepareForChatDownload(MegaNodeList list){
        ArrayList<MegaNode> nodeList = MegaApiJava.nodeListToArray(list);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            prepareForDownloadLollipop(nodeList);
        }
        else{
            prepareForDownloadPreLollipop(nodeList);
        }
    }

    public void prepareForChatDownload(MegaNode node){
        ArrayList<MegaNode> nodeList = new ArrayList<>();
        nodeList.add(node);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            prepareForDownloadLollipop(nodeList);
        }
        else{
            prepareForDownloadPreLollipop(nodeList);
        }
    }

    public void prepareForDownloadPreLollipop(ArrayList<MegaNode> nodeList){
        log("prepareForDownloadPreLollipop: "+nodeList.size()+" files to download");

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        boolean advancedDevices=false;
        String downloadLocationDefaultPath = Util.downloadDIR;
        prefs = dbH.getPreferences();

        if (prefs != null){
            log("prefs != null");
            if (prefs.getStorageDownloadLocation() != null){
                if (prefs.getStorageDownloadLocation().compareTo("") != 0){
                    downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
                }
            }
        }

        File defaultPathF = new File(downloadLocationDefaultPath);
        defaultPathF.mkdirs();
        checkSizeBeforeDownload(downloadLocationDefaultPath, nodeList);
    }

    public void checkSizeBeforeDownload(String parentPath, ArrayList<MegaNode> nodeList){
        //Variable size is incorrect for folders, it is always -1 -> sizeTemp calculates the correct size
        log("checkSizeBeforeDownload - parentPath: "+parentPath+ " size: "+nodeList.size());

        final String parentPathC = parentPath;
        long sizeTemp=0;

        for (int i=0;i<nodeList.size();i++) {
            MegaNode node = nodeList.get(i);
            if(node!=null){
                sizeTemp = sizeTemp+node.getSize();
            }
        }

        final long sizeC = sizeTemp;
        log("the final size is: "+Util.getSizeString(sizeTemp));

        //Check if there is available space
        double availableFreeSpace = Double.MAX_VALUE;
        try{
            StatFs stat = new StatFs(parentPath);
            availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
        }
        catch(Exception ex){}

        log("availableFreeSpace: " + availableFreeSpace + "__ sizeToDownload: " + sizeC);
        if(availableFreeSpace < sizeC) {

            if(context instanceof ChatActivityLollipop){
                ((ChatActivityLollipop) context).showSnackbarNotSpace();
            }
            else if(context instanceof NodeAttachmentActivityLollipop){
                ((NodeAttachmentActivityLollipop) context).showSnackbarNotSpace();
            }
            else if(context instanceof ChatFullScreenImageViewer){
                ((ChatFullScreenImageViewer) context).showSnackbarNotSpace();
            }
            log("Not enough space");
            return;
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        String ask=dbH.getAttributes().getAskSizeDownload();

        if(ask==null){
            ask="true";
        }

        if(ask.equals("false")){
            log("SIZE: Do not ask before downloading");
            download(parentPathC, nodeList);
        }
        else{
            log("SIZE: Ask before downloading");
            if(sizeC>104857600){
                log("Show size confirmacion: "+sizeC);
                //Show alert
                if(context instanceof  ChatActivityLollipop){
                    ((ChatActivityLollipop) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
                else if(context instanceof  NodeAttachmentActivityLollipop){
                    ((NodeAttachmentActivityLollipop) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
                else if(context instanceof ChatFullScreenImageViewer){
                    ((ChatFullScreenImageViewer) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
            }
            else{
                download(parentPathC, nodeList);
            }
        }
    }

    public void download(String parentPath, ArrayList<MegaNode> nodeList){
        log("download-----------");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                if (context instanceof ManagerActivityLollipop) {
                    ActivityCompat.requestPermissions(((ManagerActivityLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_WRITE_STORAGE);
                }
                else if (context instanceof ChatFullScreenImageViewer){
                    ActivityCompat.requestPermissions(((ChatFullScreenImageViewer) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.REQUEST_WRITE_STORAGE);
                }
            }
        }

        if (nodeList != null){
            if(nodeList.size() == 1){
                log("hashes.length == 1");
                MegaNode tempNode = nodeList.get(0);

                if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
                    log("ISFILE");
                    String localPath = Util.getLocalFile(context, tempNode.getName(), tempNode.getSize(), parentPath);
                    //Check if the file is already downloaded
                    if(localPath != null){
                        log("localPath != null");
                        try {
                            log("Call to copyFile: localPath: "+localPath+" node name: "+tempNode.getName());
                            Util.copyFile(new File(localPath), new File(parentPath, tempNode.getName()));

                            if(Util.isVideoFile(parentPath+"/"+tempNode.getName())){
                                log("Is video!!!");
//								MegaNode videoNode = megaApi.getNodeByHandle(tempNode.getNodeHandle());
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

                        if(MimeTypeList.typeForName(tempNode.getName()).isZip()){
                            log("MimeTypeList ZIP");
                            File zipFile = new File(localPath);

                            Intent intentZip = new Intent();
                            intentZip.setClass(context, ZipBrowserActivityLollipop.class);
                            intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, zipFile.getAbsolutePath());
                            intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_HANDLE_ZIP, tempNode.getHandle());

                            context.startActivity(intentZip);

                        }
                        else if (MimeTypeList.typeForName(tempNode.getName()).isVideo()) {
                            log("Video file");
                            File videoFile = new File(localPath);

                            Intent videoIntent = new Intent(context, AudioVideoPlayerLollipop.class);
                            videoIntent.putExtra("HANDLE", tempNode.getHandle());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                videoIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", videoFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                            }
                            else{
                                videoIntent.setDataAndType(Uri.fromFile(videoFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                            }
                            videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            context.startActivity(videoIntent);
                        }
                        else if (MimeTypeList.typeForName(tempNode.getName()).isAudio()) {
                            log("Audio file");
                            File audioFile = new File(localPath);

                            Intent audioIntent = new Intent(context, AudioVideoPlayerLollipop.class);
                            audioIntent.putExtra("HANDLE", tempNode.getHandle());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                audioIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", audioFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                            }
                            else{
                                audioIntent.setDataAndType(Uri.fromFile(audioFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                            }
                            audioIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            context.startActivity(audioIntent);
                        }
                        else if (MimeTypeList.typeForName(tempNode.getName()).isPdf()){
                            log("Pdf file");
                            File pdfFile = new File(localPath);

                            Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);
                            pdfIntent.putExtra("APP", true);
                            pdfIntent.putExtra("HANDLE", tempNode.getHandle());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                            }
                            else{
                                pdfIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                            }
                            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            context.startActivity(pdfIntent);
                        }
                        else {
                            log("MimeTypeList other file");

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
                                    if(context instanceof  ChatActivityLollipop){
                                        ((ChatActivityLollipop) context).showSnackbar(context.getString(R.string.general_already_downloaded));
                                    }
                                    else if(context instanceof  NodeAttachmentActivityLollipop){
                                        ((NodeAttachmentActivityLollipop) context).showSnackbar(context.getString(R.string.general_already_downloaded));
                                    }
                                    else if(context instanceof ChatFullScreenImageViewer){
                                        ((ChatFullScreenImageViewer) context).showSnackbar(context.getString(R.string.general_already_downloaded));
                                    }
                                }
                            }
                            catch (Exception e){
                                if(context instanceof  ChatActivityLollipop){
                                    ((ChatActivityLollipop) context).showSnackbar(context.getString(R.string.general_already_downloaded));
                                }
                                else if(context instanceof  NodeAttachmentActivityLollipop){
                                    ((NodeAttachmentActivityLollipop) context).showSnackbar(context.getString(R.string.general_already_downloaded));
                                }
                                else if(context instanceof ChatFullScreenImageViewer){
                                    ((ChatFullScreenImageViewer) context).showSnackbar(context.getString(R.string.general_already_downloaded));
                                }
                            }
                        }
                        return;
                    }
                    else{
                        log("localPath is NULL");
                    }
                }
            }

            for (int i=0; i<nodeList.size();i++) {
                log("hashes.length more than 1");
                MegaNode nodeToDownload = nodeList.get(i);
                if(nodeToDownload != null){
                    log("node NOT null");
                    Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
                    dlFiles.put(nodeToDownload, parentPath);

                    for (MegaNode document : dlFiles.keySet()) {
                        String path = dlFiles.get(document);
                        log("path of the file: "+path);
                        log("start service");
                        Intent service = new Intent(context, DownloadService.class);
                        String serializeString = nodeToDownload.serialize();
                        log("serializeString: "+serializeString);
                        service.putExtra(DownloadService.EXTRA_SERIALIZE_STRING, serializeString);
                        service.putExtra(DownloadService.EXTRA_PATH, path);
                        context.startService(service);
                    }
                }
                else {
                    log("node NOT fOUND!!!!!");
                }
            }
        }
    }

    public static void log(String message) {
        Util.log("ChatController", message);
    }
}
