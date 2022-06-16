package mega.privacy.android.app.main.controllers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.StatFs;

import android.text.Html;
import android.text.Spanned;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.ChatNotificationsPreferencesActivity;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.listeners.CopyListener;
import mega.privacy.android.app.listeners.ExportListener;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.listeners.TruncateHistoryListener;
import mega.privacy.android.app.main.FileExplorerActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.main.megachat.ArchivedChatsActivity;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.main.megachat.ChatExplorerActivity;
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity;
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.main.megachat.NonContactInfo;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static nz.mega.sdk.MegaApiJava.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class ChatController {

    private final Context context;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private DatabaseHandler dbH;
    private ExportListener exportListener;

    public ChatController(Context context){
        logDebug("ChatController created");
        this.context = context;

        if (megaApi == null){
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        if (megaChatApi == null){
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public static Intent getSelectChatsToAttachContactIntent(Context context, MegaUser contact) {
        logDebug("selectChatsToAttachContact");

        long[] longArray = new long[1];
        longArray[0] = contact.getHandle();

        Intent intent = new Intent(context, ChatExplorerActivity.class);
        intent.putExtra(USER_HANDLES, longArray);
        return intent;
    }

    public void selectChatsToAttachContacts (ArrayList<MegaUser> contacts) {
        long[] longArray = new long[contacts.size()];

        for (int i=0; i<contacts.size(); i++) {
            longArray[i] = contacts.get(i).getHandle();
        }

        Intent i = new Intent(context, ChatExplorerActivity.class);
        i.putExtra(USER_HANDLES, longArray);

        if(context instanceof ManagerActivity){
            ((ManagerActivity) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
    }

    public void clearHistory(MegaChatRoom chat){
        logDebug("Chat ID: " + chat.getChatId());
        clearHistory(chat.getChatId());
    }

    public void clearHistory(long chatId){
        logDebug("Chat ID: " + chatId);
        dbH.removePendingMessageByChatId(chatId);
        megaChatApi.clearChatHistory(chatId, new TruncateHistoryListener(context));
    }

    public void archiveChat(long chatId){
        logDebug("Chat ID: " + chatId);
        if(context instanceof ManagerActivity){
            megaChatApi.archiveChat(chatId, true, (ManagerActivity) context);
        }
    }

    public void archiveChat(MegaChatListItem chatItem){
        logDebug("Chat ID: " + chatItem.getChatId());
        if(context instanceof ManagerActivity){
            megaChatApi.archiveChat(chatItem.getChatId(), !chatItem.isArchived(), (ManagerActivity) context);
        }
        else if(context instanceof ArchivedChatsActivity){
            megaChatApi.archiveChat(chatItem.getChatId(), !chatItem.isArchived(), (ArchivedChatsActivity) context);
        }
    }

    public void archiveChat(MegaChatRoom chat){
        logDebug("Chat ID: " + chat.getChatId());
        if(context instanceof GroupChatInfoActivity){

            megaChatApi.archiveChat(chat.getChatId(), !chat.isArchived(),(GroupChatInfoActivity) context);
        }
        else if(context instanceof ChatActivity){
            megaChatApi.archiveChat(chat.getChatId(), !chat.isArchived(),(ChatActivity) context);
        }
    }

    public void archiveChats(ArrayList<MegaChatListItem> chats){
        logDebug("Chat ID: " + chats.size());
        if(context instanceof ManagerActivity){
            for(int i=0; i<chats.size(); i++){
                megaChatApi.archiveChat(chats.get(i).getChatId(), !chats.get(i).isArchived(),null);
            }
        }
        else if(context instanceof ArchivedChatsActivity){
            for(int i=0; i<chats.size(); i++){
                megaChatApi.archiveChat(chats.get(i).getChatId(), !chats.get(i).isArchived(), null);
            }
        }
    }

    public void deleteMessages(ArrayList<MegaChatMessage> messages, MegaChatRoom chat){
        logDebug("Messages to delete: " + messages.size());
        for (int i = 0; i < messages.size(); i++) {
            deleteMessage(messages.get(i), chat.getChatId());
        }
    }

    public void deleteAndroidMessages(ArrayList<AndroidMegaChatMessage> messages, MegaChatRoom chat){
        logDebug("Messages to delete: " + messages.size());
        for (int i = 0; i < messages.size(); i++) {
            deleteMessage(messages.get(i).getMessage(), chat.getChatId());
        }
    }

    public void deleteMessageById(long messageId, long chatId) {
        logDebug("Message ID: " + messageId + ", Chat ID: " + chatId);
        MegaChatMessage message = getMegaChatMessage(context, megaChatApi, chatId, messageId);

        if(message!=null){
            deleteMessage(message, chatId);
        }
    }

    public void deleteMessage(MegaChatMessage message, long chatId) {
        logDebug("Message : " + message.getMsgId() + ", Chat ID: " + chatId);
        MegaChatMessage messageToDelete;
        if (message.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || message.getType() == MegaChatMessage.TYPE_VOICE_CLIP) {
            logDebug("Delete node attachment message or voice clip message");
            if (message.getType() == MegaChatMessage.TYPE_VOICE_CLIP &&
                    message.getMegaNodeList() != null && message.getMegaNodeList().size() > 0 &&
                    message.getMegaNodeList().get(0) != null) {
                deleteOwnVoiceClip(context, message.getMegaNodeList().get(0).getName());
            }
            messageToDelete = megaChatApi.revokeAttachmentMessage(chatId, message.getMsgId());
        } else {
            logDebug("Delete normal message with status = " + message.getStatus());
            if (message.getStatus() == MegaChatMessage.STATUS_SENDING && message.getMsgId() == INVALID_HANDLE) {
                messageToDelete = megaChatApi.deleteMessage(chatId, message.getTempId());
            } else {
                messageToDelete = megaChatApi.deleteMessage(chatId, message.getMsgId());
            }
        }

        if (messageToDelete == null) {
            logDebug("The message cannot be deleted");
        } else if (context instanceof ChatActivity) {
            logDebug("The message has been deleted");
            ((ChatActivity) context).updatingRemovedMessage(message);
        }
    }

    /*
     * Delete a voice note from local storage
     */
    public static void deleteOwnVoiceClip(Context mContext, String nameFile) {
        logDebug("deleteOwnVoiceClip");
        File localFile = buildVoiceClipFile(mContext, nameFile);
        if (!isFileAvailable(localFile)) return;
        localFile.delete();
    }

    public void alterParticipantsPermissions(long chatid, long uh, int privilege){
        logDebug("Chat ID: " + chatid + ", User (uh): " + uh + ", Priv: " + privilege);
        megaChatApi.updateChatPermissions(chatid, uh, privilege, (GroupChatInfoActivity) context);
    }

    public void removeParticipant(long chatid, long uh){
        logDebug("Chat ID: " + chatid + ", User (uh): " + uh);
        if(context==null){
            logWarning("Context is NULL");
        }
        megaChatApi.removeFromChat(chatid, uh, (GroupChatInfoActivity) context);
    }

    public void changeTitle(long chatid, String title){
        if(context instanceof GroupChatInfoActivity){
            megaChatApi.setChatTitle(chatid, title, (GroupChatInfoActivity) context);
        }
    }

    /**
     * Method to silence notifications for all chats or for a specific chat.
     *
     * @param option     The selected mute option.
     */
    public void muteChat(String option) {
        if (context instanceof ChatNotificationsPreferencesActivity)
            return;

        switch (option) {
            case NOTIFICATIONS_ENABLED:
                showSnackbar(context, context.getString(R.string.success_unmuting_a_chat));
                break;

            case NOTIFICATIONS_DISABLED:
                showSnackbar(context, context.getString(R.string.notifications_are_already_muted));
                break;

            case NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING:
            case NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING:
                showSnackbar(context, getCorrectStringDependingOnCalendar(option));
                break;

            default:
                String text = getMutedPeriodString(option);
                if (!isTextEmpty(text)) {
                    showSnackbar(context, context.getString(R.string.success_muting_a_chat_for_specific_time, text));
                }
        }
    }

    public String createSingleManagementString(AndroidMegaChatMessage androidMessage, MegaChatRoom chatRoom) {
        logDebug("Message ID: " + androidMessage.getMessage().getMsgId() + ", Chat ID: " + chatRoom.getChatId());

        String text = createManagementString(androidMessage.getMessage(), chatRoom);
        if((text!=null) && (!text.isEmpty())){
            text = text.substring(text.indexOf(":")+2);
            String strEdited = " " + StringResourcesUtils.getString(R.string.edited_message_text);
            if (text.contains(strEdited)) {
                int index = text.indexOf(strEdited);
                text = text.substring(0, index);
            }
        }else{
            text = "";

        }
        return text;
    }


    public String createManagementString(MegaChatMessage message, MegaChatRoom chatRoom) {
        logDebug("MessageID: " + message.getMsgId() + ", Chat ID: " + chatRoom.getChatId());
        long userHandle = message.getUserHandle();

        if (message.getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) {
            logDebug("ALTER PARTICIPANT MESSAGE!!");

            if (message.getHandleOfAction() == megaApi.getMyUser().getHandle()) {
                logDebug("Me alter participant");

                StringBuilder builder = new StringBuilder();
                builder.append(megaChatApi.getMyFullname()).append(": ");

                int privilege = message.getPrivilege();
                logDebug("Privilege of me: " + privilege);
                String textToShow;
                String fullNameAction = getParticipantFullName(message.getUserHandle());

                if (privilege != MegaChatRoom.PRIV_RM) {
                    logDebug("I was added");
                    textToShow = String.format(context.getString(R.string.non_format_message_add_participant), megaChatApi.getMyFullname(), fullNameAction);
                } else {
                    logDebug("I was removed or left");
                    if (message.getUserHandle() == message.getHandleOfAction()) {
                        logDebug("I left the chat");
                        textToShow = String.format(context.getString(R.string.non_format_message_participant_left_group_chat), megaChatApi.getMyFullname());

                    } else {
                        textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), megaChatApi.getMyFullname(), fullNameAction);
                    }
                }

                builder.append(textToShow);
                return builder.toString();

            } else {
                logDebug("CONTACT Message type ALTER PARTICIPANTS");

                int privilege = message.getPrivilege();
                logDebug("Privilege of the user: " + privilege);

                String fullNameTitle = getParticipantFullName(message.getHandleOfAction());

                StringBuilder builder = new StringBuilder();
                builder.append(fullNameTitle).append(": ");

                String textToShow = "";
                if (privilege != MegaChatRoom.PRIV_RM) {
                    logDebug("Participant was added");
                    if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                        logDebug("By me");
                        textToShow = String.format(context.getString(R.string.non_format_message_add_participant), fullNameTitle, megaChatApi.getMyFullname());
                    } else {
                        logDebug("By other");
                        String fullNameAction = getParticipantFullName(message.getUserHandle());
                        textToShow = String.format(context.getString(R.string.non_format_message_add_participant), fullNameTitle, fullNameAction);
                    }
                }//END participant was added
                else {
                    logDebug("Participant was removed or left");
                    if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                        textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), fullNameTitle, megaChatApi.getMyFullname());
                    } else {

                        if (message.getUserHandle() == message.getHandleOfAction()) {
                            logDebug("The participant left the chat");

                            textToShow = String.format(context.getString(R.string.non_format_message_participant_left_group_chat), fullNameTitle);

                        } else {
                            logDebug("The participant was removed");
                            String fullNameAction = getParticipantFullName(message.getUserHandle());
                            textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), fullNameTitle, fullNameAction);
                        }
                    }
                } //END participant removed

                builder.append(textToShow);
                return builder.toString();

            } //END CONTACT MANAGEMENT MESSAGE
        }else if (message.getType() == MegaChatMessage.TYPE_PRIV_CHANGE) {
            logDebug("PRIVILEGE CHANGE message");
            if (message.getHandleOfAction() == megaApi.getMyUser().getHandle()) {
                logDebug("A moderator change my privilege");
                int privilege = message.getPrivilege();
                logDebug("Privilege of the user: " + privilege);

                StringBuilder builder = new StringBuilder();
                builder.append(megaChatApi.getMyFullname()).append(": ");

                String privilegeString = "";
                if (privilege == MegaChatRoom.PRIV_MODERATOR) {
                    privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_STANDARD) {
                    privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_RO) {
                    privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
                } else {
                    logDebug("Change to other");
                    privilegeString = "Unknow";
                }

                String textToShow;

                if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                    logDebug("I changed my own permission");
                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), megaChatApi.getMyFullname(), privilegeString, megaChatApi.getMyFullname());
                } else {
                    logDebug("My permission was changed by someone");
                    String fullNameAction = getParticipantFullName(message.getUserHandle());
                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), megaChatApi.getMyFullname(), privilegeString, fullNameAction);
                }

                builder.append(textToShow);
                return builder.toString();
            } else {
                logDebug("Participant privilege change!");
                logDebug("Message type PRIVILEGE CHANGE - Message ID: " + message.getMsgId());

                String fullNameTitle = getParticipantFullName(message.getHandleOfAction());
                StringBuilder builder = new StringBuilder();
                builder.append(fullNameTitle).append(": ");

                int privilege = message.getPrivilege();
                String privilegeString = "";
                if (privilege == MegaChatRoom.PRIV_MODERATOR) {
                    privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_STANDARD) {
                    privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_RO) {
                    privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
                } else {
                    logDebug("Change to other");
                    privilegeString = "Unknow";
                }

                String textToShow;
                if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                    logDebug("The privilege was change by me");
                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), fullNameTitle, privilegeString, megaChatApi.getMyFullname());

                } else {
                    logDebug("By other");
                    String fullNameAction = getParticipantFullName(message.getUserHandle());
                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), fullNameTitle, privilegeString, fullNameAction);
                }

                builder.append(textToShow);
                return builder.toString();
            }
        }else {
            logDebug("Other type of messages");
            //OTHER TYPE OF MESSAGES
            if (megaApi.getMyUser() != null && megaApi.getMyUser().getHandle() == message.getUserHandle()) {
                logDebug("MY message ID: " + message.getMsgId());

                StringBuilder builder = new StringBuilder();
                builder.append(megaChatApi.getMyFullname()).append(": ");

                if (message.getType() == MegaChatMessage.TYPE_NORMAL) {
                    logDebug("Message type NORMAL");

                    String messageContent = "";
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                    }

                    if (message.isEdited()) {
                        logDebug("Message is edited");
                        String textToShow = messageContent + " " + context.getString(R.string.edited_message_text);
                        builder.append(textToShow);
                        return builder.toString();
                    } else if (message.isDeleted()) {
                        logDebug("Message is deleted");

                        String textToShow = context.getString(R.string.text_deleted_message);
                        builder.append(textToShow);
                        return builder.toString();

                    } else {
                        builder.append(messageContent);
                        return builder.toString();
                    }
                }else if (message.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                    logDebug("Message type TRUNCATE");

                    String textToShow = String.format(context.getString(R.string.history_cleared_by), toCDATA(megaChatApi.getMyFullname()));
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#00BFA5\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    }
                    catch (Exception e){
                        logError("Exception: " + e.getMessage());
                    }
                    Spanned result;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }
                    builder.append(result);
                    return builder.toString();
                }else if (message.getType() == MegaChatMessage.TYPE_CHAT_TITLE) {
                    logDebug("Message type TITLE CHANGE - Message ID: " + message.getMsgId());

                    String messageContent = message.getContent();
                    String textToShow = String.format(context.getString(R.string.non_format_change_title_messages), megaChatApi.getMyFullname(), messageContent);
                    builder.append(textToShow);
                    return builder.toString();

                }else if(message.getType() == MegaChatMessage.TYPE_CONTAINS_META){
                    MegaChatContainsMeta meta = message.getContainsMeta();
                    if (meta != null) {
                        if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW) {
                            String text = meta.getRichPreview().getText();
                            builder.append(text);
                            return builder.toString();
                        } else if (isGeolocation(message)) {
                            String text = message.getContainsMeta().getTextMessage();
                            builder.append(text);
                            return builder.toString();
                        }
                    }

                    return "";
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

                            textToShow = chatRoom.isGroup() ? context.getString(R.string.group_call_ended_message) :
                                    context.getString(R.string.call_ended_message);

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
                                logError("Exception: " + e.getMessage());
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
                                logError("Exception: " + e.getMessage());
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
                                logError("Exception: " + e.getMessage());
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
                                logError("Exception: " + e.getMessage());
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
                logDebug("Contact message!!");

                String fullNameTitle = getParticipantFullName(userHandle);
                StringBuilder builder = new StringBuilder();
                builder.append(fullNameTitle).append(": ");

                if (message.getType() == MegaChatMessage.TYPE_NORMAL) {

                    String messageContent = "";
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                    }

                    if (message.isEdited()) {
                        logDebug("Message is edited");

                        String textToShow = messageContent + " " + context.getString(R.string.edited_message_text);
                        builder.append(textToShow);
                        return builder.toString();
                    } else if (message.isDeleted()) {
                        logDebug("Message is deleted");
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
                    logDebug("Message type TRUNCATE");

                    String textToShow = String.format(context.getString(R.string.non_format_history_cleared_by), fullNameTitle);
                    builder.append(textToShow);
                    return builder.toString();

                } else if (message.getType() == MegaChatMessage.TYPE_CHAT_TITLE) {
                    logDebug("Message type CHANGE TITLE - Message ID: " + message.getMsgId());

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
                        case MegaChatMessage.END_CALL_REASON_ENDED: {

                            int hours = message.getDuration() / 3600;
                            int minutes = (message.getDuration() % 3600) / 60;
                            int seconds = message.getDuration() % 60;

                            textToShow = chatRoom.isGroup() ? context.getString(R.string.group_call_ended_message) :
                                    context.getString(R.string.call_ended_message);

                            if (hours != 0) {
                                String textHours = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, hours, hours);
                                textToShow = textToShow + textHours;
                                if ((minutes != 0) || (seconds != 0)) {
                                    textToShow = textToShow + ", ";
                                }
                            }

                            if (minutes != 0) {
                                String textMinutes = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, minutes, minutes);
                                textToShow = textToShow + textMinutes;
                                if (seconds != 0) {
                                    textToShow = textToShow + ", ";
                                }
                            }

                            if (seconds != 0) {
                                String textSeconds = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_seconds, seconds, seconds);
                                textToShow = textToShow + textSeconds;
                            }

                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                                textToShow = textToShow.replace("[C]", "");
                                textToShow = textToShow.replace("[/C]", "");
                            } catch (Exception e) {
                                logError("Exception: " + e.getMessage());
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_REJECTED: {

                            textToShow = String.format(context.getString(R.string.call_rejected_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                                logError("Exception: " + e.getMessage());
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_NO_ANSWER: {

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
                        case MegaChatMessage.END_CALL_REASON_FAILED: {

                            textToShow = String.format(context.getString(R.string.call_failed_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                                logError("Exception: " + e.getMessage());
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_CANCELLED: {

                            textToShow = String.format(context.getString(R.string.call_missed_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                                logError("Exception: " + e.getMessage());
                            }

                            break;
                        }
                    }

                    builder.append(textToShow);
                    return builder.toString();
                }
                else{
                    logDebug("Message type: " + message.getType());
                    logDebug("Message ID: " + message.getMsgId());
                    return "";
                }
            }
        }
    }

    public String createManagementString(AndroidMegaChatMessage androidMessage, MegaChatRoom chatRoom) {
        logDebug("Message ID: " + androidMessage.getMessage().getMsgId() + ", Chat ID: " + chatRoom.getChatId());

        MegaChatMessage message = androidMessage.getMessage();
        return createManagementString(message, chatRoom);
    }

    /**
     * Gets a partcipant's name (not contact).
     * If the participant has a first name, it returns the first name.
     * If the participant has a last name, it returns the last name.
     * Otherwise, it returns the email.
     *
     * @param userHandle    participant's identifier
     * @return The participant's name.
     */
    private String getNonContactFirstName(long userHandle) {
        NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle + "");
        if (nonContact == null) {
            return "";
        }

        String name = nonContact.getFirstName();

        if (isTextEmpty(name)) {
            name = nonContact.getLastName();
        }

        if (isTextEmpty(name)) {
            name = nonContact.getEmail();
        }

        return name;
    }

    /**
     * Gets a partcipant's full name (not contact).
     * If the participant has a full name, it returns the full name.
     * Otherwise, it returns the email.
     *
     * @param userHandle    participant's identifier
     * @return The participant's full name.
     */
    private String getNonContactFullName(long userHandle) {
        NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle + "");
        if (nonContact == null) {
            return "";
        }

        String fullName = nonContact.getFullName();

        if (isTextEmpty(fullName)) {
            fullName = nonContact.getEmail();
        }

        return fullName;
    }

    /**
     * Gets a partcipant's email (not contact).
     *
     * @param userHandle    participant's identifier
     * @return The participant's email.
     */
    private String getNonContactEmail(long userHandle) {
        NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle + "");
        return nonContact != null ? nonContact.getEmail() : "";
    }

    public String getMyFullName() {

        String fullName = megaChatApi.getMyFullname();

        if (fullName != null) {
            if (fullName.isEmpty()) {
                logDebug("Put MY email as fullname");
                String myEmail = megaChatApi.getMyEmail();
                String[] splitEmail = myEmail.split("[@._]");
                fullName = splitEmail[0];
            } else {
                if (fullName.trim().length() <= 0) {
                    logDebug("Put MY email as fullname");
                    String myEmail = megaChatApi.getMyEmail();
                    String[] splitEmail = myEmail.split("[@._]");
                    fullName = splitEmail[0];
                }
            }
        } else {
            logDebug("Put MY email as fullname");
            String myEmail = megaChatApi.getMyEmail();
            String[] splitEmail = myEmail.split("[@._]");
            fullName = splitEmail[0];
        }

        return fullName;
    }

    public void saveForOfflineWithMessages(ArrayList<MegaChatMessage> messages,
                                           MegaChatRoom chatRoom,
                                           SnackbarShower snackbarShower) {
        logDebug("Save for offline multiple messages");
        for(int i=0; i<messages.size();i++){
            saveForOffline(messages.get(i).getMegaNodeList(), chatRoom, false, snackbarShower);
        }
    }

    public void saveForOfflineWithAndroidMessages(ArrayList<AndroidMegaChatMessage> messages,
                                                  MegaChatRoom chatRoom,
                                                  SnackbarShower snackbarShower) {
        logDebug("Save for offline multiple messages");
        for(int i=0; i<messages.size();i++){
            saveForOffline(messages.get(i).getMessage().getMegaNodeList(), chatRoom, false,
                    snackbarShower);
        }
    }

    public void saveForOffline(MegaNodeList nodeList, MegaChatRoom chatRoom,
                               boolean fromMediaViewer, SnackbarShower snackbarShower) {
        File destination = null;

        if (MegaApplication.getInstance().getStorageState() == STORAGE_STATE_PAYWALL) {
            showOverDiskQuotaPaywallWarning();
            return;
        }

        Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
        for (int i = 0; i < nodeList.size(); i++) {

            MegaNode document = nodeList.get(i);
            if (document != null) {
                document = authorizeNodeIfPreview(document, chatRoom);
                destination = getOfflineParentFile(context, FROM_OTHERS, document, null);
                destination.mkdirs();

                logDebug("DESTINATION: " + destination.getAbsolutePath());
                if (isFileAvailable(destination) && destination.isDirectory()){

                    File offlineFile = new File(destination, document.getName());
                    if (offlineFile.exists() && document.getSize() == offlineFile.length() && offlineFile.getName().equals(document.getName())){ //This means that is already available offline
                        logWarning("File already exists!");
                        snackbarShower.showSnackbar(SNACKBAR_TYPE,
                                getString(R.string.file_already_exists), MEGACHAT_INVALID_HANDLE);
                    }
                    else{
                        dlFiles.put(document, destination.getAbsolutePath());
                    }
                }
                else{
                    logError("Destination ERROR");
                }
            }
        }

        double availableFreeSpace = Double.MAX_VALUE;
        try{
            StatFs stat = new StatFs(destination.getAbsolutePath());
            availableFreeSpace = (double)stat.getAvailableBlocksLong() * (double)stat.getBlockSize();
        }
        catch(Exception ex){
            logError("Exception: " + ex.getMessage());
        }

        for (MegaNode document : dlFiles.keySet()) {

            String path = dlFiles.get(document);

            if (availableFreeSpace <document.getSize()) {
                showErrorAlertDialog(
                        getString(R.string.location_label,
                                getString(R.string.error_not_enough_free_space),
                                document.getName()),
                        false, ((Activity) context));
                continue;
            }

            Intent service = new Intent(context, DownloadService.class);
            document = authorizeNodeIfPreview(document, chatRoom);
            String serializeString = document.serialize();
            logDebug("serializeString: " + serializeString);
            service.putExtra(Constants.EXTRA_SERIALIZE_STRING, serializeString);
            service.putExtra(DownloadService.EXTRA_PATH, path);
            service.putExtra(DownloadService.EXTRA_DOWNLOAD_FOR_OFFLINE, true);
            if (fromMediaViewer) {
                service.putExtra("fromMV", true);
            }
            context.startService(service);
        }

    }

    public void importNode(long idMessage, long idChat, int typeImport) {
        logDebug("Message ID: " + idMessage + ", Chat ID: " + idChat);
        ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
        MegaChatMessage m = getMegaChatMessage(context, megaChatApi, idChat, idMessage);

        if(m!=null){
            AndroidMegaChatMessage aMessage = new AndroidMegaChatMessage(m);
            messages.add(aMessage);
            importNodesFromAndroidMessages(messages, typeImport);
        }
        else{
            logWarning("Message cannot be recovered - null");
        }
    }

    public void importNodesFromMessages(ArrayList<MegaChatMessage> messages){
        logDebug("importNodesFromMessages");

        Intent intent = new Intent(context, FileExplorerActivity.class);
        intent.setAction(FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER);

        long[] longArray = new long[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            longArray[i] = messages.get(i).getMsgId();
        }
        intent.putExtra("HANDLES_IMPORT_CHAT", longArray);

        if(context instanceof  NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
        }
    }

    public void importNodesFromAndroidMessages(ArrayList<AndroidMegaChatMessage> messages, int typeImport){
        logDebug("importNodesFromAndroidMessages");

        Intent intent = new Intent(context, FileExplorerActivity.class);
        intent.setAction(FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER);

        long[] longArray = new long[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            longArray[i] = messages.get(i).getMessage().getMsgId();
        }
        intent.putExtra("HANDLES_IMPORT_CHAT", longArray);

        if(context instanceof ChatActivity){
            if (typeImport == IMPORT_TO_SHARE_OPTION) {
                ((ChatActivity) context).importNodeToShare(messages, exportListener);
            } else {
                ((ChatActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
            }
        }
        else if(context instanceof  NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
        }
    }

    /**
     * Method to prepare selected messages that are of type TYPE_NODE_ATTACHMENT or type TYPE_VOICE_CLIP to be imported and shared.
     *
     * @param androidMessagesSelected The selected messages.
     * @param idChat                  The chat ID.
     */
    public void prepareMessagesToShare(ArrayList<AndroidMegaChatMessage> androidMessagesSelected, long idChat) {
        if (androidMessagesSelected == null || androidMessagesSelected.isEmpty())
            return;

        logDebug("Number of messages: " + androidMessagesSelected.size() + ",Chat ID: " + idChat);
        ArrayList<MegaChatMessage> messagesToImport = new ArrayList<>();
        ArrayList<MegaChatMessage> messagesSelected = new ArrayList<>();
        for (AndroidMegaChatMessage androidMsg : androidMessagesSelected) {
            messagesSelected.add(androidMsg.getMessage());
            int type = androidMsg.getMessage().getType();
            if (type == MegaChatMessage.TYPE_NODE_ATTACHMENT || type == MegaChatMessage.TYPE_VOICE_CLIP) {
                messagesToImport.add(androidMsg.getMessage());
            }

        }

        if (!messagesToImport.isEmpty() && context instanceof ChatActivity) {
            ((ChatActivity) context).storedUnhandledData(messagesSelected, messagesToImport);
            ((ChatActivity) context).setExportListener(exportListener);
            ((ChatActivity) context).handleStoredData();
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
        logDebug("Number of messages: " + messagesSelected.size() + ",Chat ID: " + idChat);

        ArrayList<MegaChatMessage> messagesToImport = new ArrayList<>();
        long[] idMessages = new long[messagesSelected.size()];
        for(int i=0; i<messagesSelected.size();i++){
            idMessages[i] = messagesSelected.get(i).getMsgId();

            logDebug("Type of message: "+ messagesSelected.get(i).getType());
            if((messagesSelected.get(i).getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT)||(messagesSelected.get(i).getType()==MegaChatMessage.TYPE_VOICE_CLIP)){
                if(messagesSelected.get(i).getUserHandle()!=megaChatApi.getMyUserHandle()){
                    //Node has to be imported
                    messagesToImport.add(messagesSelected.get(i));
                }
            }
        }

        if(messagesToImport.isEmpty()){
            forwardMessages(messagesSelected, idChat);
        }
        else{
            if (context instanceof ChatActivity) {
                ((ChatActivity) context).storedUnhandledData(messagesSelected, messagesToImport);
                ((ChatActivity) context).handleStoredData();
            } else if (context instanceof NodeAttachmentHistoryActivity) {
                ((NodeAttachmentHistoryActivity) context).storedUnhandledData(messagesSelected, messagesToImport);
                if (existsMyChatFilesFolder()) {
                    ((NodeAttachmentHistoryActivity) context).setMyChatFilesFolder(getMyChatFilesFolder());
                    ((NodeAttachmentHistoryActivity) context).handleStoredData();
                } else {
                    megaApi.getMyChatFilesFolder(new GetAttrUserListener(context));
                }
            }
        }
    }

    /**
     * Method for copying nodes to My Chat Files folder.
     *
     * @param snackbarShower The interface to show snackbar.
     * @param myChatFilesFolder The node myChatFilesFolder.
     * @param messagesSelected  The list of selected msgs.
     * @param messagesToImport  The list of messages to import.
     * @param idChat            The chat ID.
     * @param typeImport        IMPORT_TO_SHARE_OPTION, indicates that the node will be shared.
     *                          FORWARD_ONLY_OPTION, indicates that the node will be forwarded.
     */
    public void proceedWithForwardOrShare(SnackbarShower snackbarShower, MegaNode myChatFilesFolder,
                                          ArrayList<MegaChatMessage> messagesSelected,
                                          ArrayList<MegaChatMessage> messagesToImport,
                                          long idChat, int typeImport) {
        CopyListener listener;
        if (typeImport == IMPORT_TO_SHARE_OPTION) {
            if(exportListener == null){
                listener = new CopyListener(CopyListener.MULTIPLE_IMPORT_CONTACT_MESSAGES,
                        messagesSelected, messagesToImport.size(), context, snackbarShower, this, idChat);
            }else{
                listener = new CopyListener(CopyListener.MULTIPLE_IMPORT_CONTACT_MESSAGES,
                        messagesSelected, messagesToImport.size(), context, snackbarShower, this,
                        idChat, exportListener);
            }

        } else {
            listener = new CopyListener(CopyListener.MULTIPLE_FORWARD_MESSAGES, messagesSelected,
                    messagesToImport.size(), context, snackbarShower, this, idChat);
        }

        int errors = 0;

        for (MegaChatMessage msgToImport : messagesToImport) {
            if (msgToImport == null) {
                logWarning("MESSAGE is null");
                errors++;
            } else {
                MegaNodeList nodeList = msgToImport.getMegaNodeList();
                for (int i = 0; i < nodeList.size(); i++) {
                    MegaNode document = nodeList.get(i);
                    if (document != null) {
                        logDebug("DOCUMENT to copy: " + document.getHandle());
                        document = authorizeNodeIfPreview(document, megaChatApi.getChatRoom(idChat));
                        megaApi.copyNode(document, myChatFilesFolder, listener);
                    }
                }
            }
        }

        if (errors > 0) {
            if (typeImport == IMPORT_TO_SHARE_OPTION) {
                showSnackbar(context, getString(R.string.number_no_imported_from_chat, errors));
            } else {
                showSnackbar(context, getQuantityString(R.plurals.messages_forwarded_partial_error, errors, errors));
            }
        }
    }

    public void forwardMessages(ArrayList<MegaChatMessage> messagesSelected, long idChat){
        logDebug("Number of messages: " + messagesSelected.size() + ", Chat ID: " + idChat);

        long[] idMessages = new long[messagesSelected.size()];
        for(int i=0; i<messagesSelected.size();i++){
            idMessages[i] = messagesSelected.get(i).getMsgId();
        }

        Intent i = new Intent(context, ChatExplorerActivity.class);
        i.putExtra(ID_MESSAGES, idMessages);
        i.putExtra("ID_CHAT_FROM", idChat);
        i.setAction(ACTION_FORWARD_MESSAGES);
        if(context instanceof ChatActivity){
            ((ChatActivity) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
        else if(context instanceof  NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
    }

    public MegaNode authorizeNodeIfPreview (MegaNode node, MegaChatRoom chatRoom) {
        if (chatRoom != null && chatRoom.isPreview()) {
            MegaNode nodeAuthorized = megaApi.authorizeChatNode(node, chatRoom.getAuthorizationToken());
            if (nodeAuthorized != null) {
                logDebug("Authorized");
                return nodeAuthorized;
            }
        }
        logDebug("NOT authorized");
        return node;
    }

    public boolean isInAnonymousMode () {
        return megaChatApi.getInitState() == MegaChatApi.INIT_ANONYMOUS;
    }

    public boolean isPreview (MegaChatRoom chatRoom) {
        if (chatRoom != null) {
            return chatRoom.isPreview();
        }

        return false;
    }

    /**
     * Stores in DB the user's attributes of a non contact.
     *
     * @param peerHandle    identifier of the user to save
     */
    public void setNonContactAttributesInDB (long peerHandle) {
        DatabaseHandler dbH = MegaApplication.getInstance().getDbH();
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        String firstName = megaChatApi.getUserFirstnameFromCache(peerHandle);
        if (!isTextEmpty(firstName)) {
            dbH.setNonContactFirstName(firstName, peerHandle + "");
        }

        String lastName = megaChatApi.getUserLastnameFromCache(peerHandle);
        if (!isTextEmpty(lastName)) {
            dbH.setNonContactLastName(lastName, peerHandle + "");
        }

        String email = megaChatApi.getUserEmailFromCache(peerHandle);
        if (!isTextEmpty(email)) {
            dbH.setNonContactEmail(email, peerHandle + "");
        }
    }

    /**
     * Gets the participant's first name.
     * If the participant has an alias, it returns the alias.
     * If the participant has a first name, it returns the first name.
     * If the participant has a last name, it returns the last name.
     * Otherwise, it returns the email.
     *
     * @param userHandle    participant's identifier
     * @return The participant's first name
     */
    public String getParticipantFirstName(long userHandle) {
        String firstName = getFirstNameDB(userHandle);

        if (isTextEmpty(firstName)) {
            firstName = getNonContactFirstName(userHandle);
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserFirstnameFromCache(userHandle);
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserLastnameFromCache(userHandle);
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserEmailFromCache(userHandle);
        }

        return firstName;
    }

    /**
     * Gets the participant's full name.
     * If the participant has an alias, it returns the alias.
     * If the participant has a full name, it returns the full name.
     * Otherwise, it returns the email.
     *
     * @param handle    participant's identifier
     * @return The participant's full name.
     */
    public String getParticipantFullName(long handle) {
        String fullName = getContactNameDB(handle);

        if (isTextEmpty(fullName)) {
            fullName = getNonContactFullName(handle);
        }

        if (isTextEmpty(fullName)) {
            fullName = megaChatApi.getUserFullnameFromCache(handle);
        }

        if (isTextEmpty(fullName)) {
            fullName = megaChatApi.getUserEmailFromCache(handle);
        }

        return fullName;
    }

    /**
     * Gets the participant's email.
     *
     * @param handle    participant's identifier
     * @return The participant's email.
     */
    public String getParticipantEmail(long handle) {
        String email = getContactEmailDB(handle);

        if (isTextEmpty(email)) {
            email = getNonContactEmail(handle);
        }

        if (isTextEmpty(email)) {
            email = megaChatApi.getUserEmailFromCache(handle);
        }

        return email;
    }

    public void setExportListener(ExportListener exportListener) {
        this.exportListener = exportListener;
    }
}
