package mega.privacy.android.app.fcm;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import java.util.HashSet;
import java.util.Set;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.megachat.calls.CallNotificationIntentService;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNodeList;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.CallUtil.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public final class ChatAdvancedNotificationBuilder {

    private static final String GROUP_KEY = "Karere";
    private static final int THREE_BUTTONS = 3;
    private static final int TWO_BUTTONS = 2;

    private final Context context;
    private NotificationManager notificationManager;
    DatabaseHandler dbH;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    private static final String STRING_FALSE = "false";
    private static final String STRING_TRUE = "true";

    private long[] patternIncomingCall = {0, 1000, 1000, 1000, 1000, 1000, 1000};

    private static Set<Integer> notificationIds = new HashSet<>();

    private String notificationChannelIdChatSimple = NOTIFICATION_CHANNEL_CHAT_ID;
    private String notificationChannelNameChatSimple = NOTIFICATION_CHANNEL_CHAT_NAME;
    private String notificationChannelIdChatSummaryV2 = NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2;
    private String notificationChannelIdChatSummaryNoVibrate = NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_ID;
    private String notificationChannelIdInProgressMissedCall = NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID;
    private String notificationChannelNameInProgressMissedCall = NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME;
    private String notificationChannelIdIncomingCall = NOTIFICATION_CHANNEL_INCOMING_CALLS_ID;
    private String notificationChannelNameIncomingCall = NOTIFICATION_CHANNEL_INCOMING_CALLS_NAME;

    public static ChatAdvancedNotificationBuilder newInstance(Context context, MegaApiAndroid megaApi, MegaChatApiAndroid megaChatApi) {
        Context appContext = context.getApplicationContext();
        Context safeContext = ContextCompat.createDeviceProtectedStorageContext(appContext);
        if (safeContext == null) {
            safeContext = appContext;
        }
        NotificationManager notificationManager = (NotificationManager) safeContext.getSystemService(Context.NOTIFICATION_SERVICE);

        return new ChatAdvancedNotificationBuilder(safeContext, notificationManager, megaApi, megaChatApi);
    }

    public ChatAdvancedNotificationBuilder(Context context, NotificationManager notificationManager, MegaApiAndroid megaApi, MegaChatApiAndroid megaChatApi) {
        this.context = context.getApplicationContext();
        this.notificationManager = notificationManager;

        dbH = DatabaseHandler.getDbHandler(context);
        this.megaApi = megaApi;
        this.megaChatApi = megaChatApi;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChatSummaryChannel(context);
        }
    }

    public void sendBundledNotification(Uri uriParameter, String vibration, long chatId, MegaHandleList unreadHandleList) {
        logDebug("sendBundledNotification");
        MegaChatRoom chat = megaChatApi.getChatRoom(chatId);

        ArrayList<MegaChatMessage> unreadMessages = new ArrayList<>();
        for(int i=0;i<unreadHandleList.size();i++){
            MegaChatMessage message = megaChatApi.getMessage(chatId, unreadHandleList.get(i));
            logDebug("Chat: " + chat.getChatId() + " messagID: " + unreadHandleList.get(i));
            if(message!=null) {
                unreadMessages.add(message);
            } else {
                logWarning("Message cannot be recovered");
            }
        }

        Collections.sort(unreadMessages, new Comparator<MegaChatMessage>() {
            public int compare(MegaChatMessage c1, MegaChatMessage c2) {
                long timestamp1 = c1.getTimestamp();
                long timestamp2 = c2.getTimestamp();

                long result = timestamp2 - timestamp1;
                return (int) result;
            }
        });

        Notification notification = buildNotification(uriParameter, vibration, GROUP_KEY, chat, unreadMessages);

        String chatString = MegaApiJava.userHandleToBase64(chat.getChatId());

        int notificationId = chatString.hashCode();
        notify(notificationId, notification);
    }

    private void notify(int id, Notification notification) {
        notificationIds.add(id);
        notificationManager.notify(id, notification);
    }

    public void buildNotificationPreN(Uri uriParameter, String vibration, MegaChatRequest request) {
        logDebug("buildNotificationPreN");

        MegaHandleList chatHandleList = request.getMegaHandleList();

        ArrayList<MegaChatListItem> chats = new ArrayList<>();
        for (int i = 0; i < chatHandleList.size(); i++) {
            MegaChatListItem chat = megaChatApi.getChatListItem(chatHandleList.get(i));
            if (chat != null) {
                chats.add(chat);
            } else {
                logError("ERROR:chatNotRecovered:NULL");
                return;
            }
        }

        PendingIntent pendingIntent = null;

        if (chats.size() > 1) {
            Intent intent = new Intent(context, ManagerActivityLollipop.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(ACTION_CHAT_SUMMARY);
            intent.putExtra("CHAT_ID", -1);
            pendingIntent = PendingIntent.getActivity(context, (int) chats.get(0).getChatId(), intent, PendingIntent.FLAG_ONE_SHOT);

            //Order by last interaction
            Collections.sort(chats, new Comparator<MegaChatListItem>() {

                public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                    long timestamp1 = c1.getLastTimestamp();
                    long timestamp2 = c2.getLastTimestamp();

                    long result = timestamp2 - timestamp1;
                    return (int) result;
                }
            });
        } else if (chats.size() == 1) {
            Intent intent = new Intent(context, ManagerActivityLollipop.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
            intent.putExtra("CHAT_ID", chats.get(0).getChatId());
            pendingIntent = PendingIntent.getActivity(context, (int) chats.get(0).getChatId(), intent, PendingIntent.FLAG_ONE_SHOT);
        } else {
            logError("ERROR:chatSIZE=0:return");
            return;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(ContextCompat.getColor(context, R.color.mega));
        }

        notificationBuilder.setShowWhen(true);

        if (uriParameter != null) {
            notificationBuilder.setSound(uriParameter);
        }

        if (STRING_TRUE.equals(vibration)) {
            notificationBuilder.setVibrate(new long[]{0, 500});
        }

        notificationBuilder.setStyle(inboxStyle);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            //API 25 = Android 7.1
            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
        } else {
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }

//        notificationBuilder.setFullScreenIntent(pendingIntent, true);

        for (int i = 0; i < chats.size(); i++) {
            if (MegaApplication.getOpenChatId() != chats.get(i).getChatId()) {
                MegaHandleList handleListUnread = request.getMegaHandleListByChat(chats.get(i).getChatId());

                for (int j = 0; j < handleListUnread.size(); j++) {
                    logDebug("Get message id: " + handleListUnread.get(j) + " from chatId: " + chats.get(i).getChatId());
                    MegaChatMessage message = megaChatApi.getMessage(chats.get(i).getChatId(), handleListUnread.get(j));
                    if (message != null) {

                        String messageContent = "";

                        if (message.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || message.getType() == MegaChatMessage.TYPE_VOICE_CLIP) {
                            logDebug("TYPE_NODE_ATTACHMENT || TYPE_VOICE_CLIP");
                            messageContent = checkMessageContentAttachmentOrVoiceClip(message);
                        } else if (message.getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                            logDebug("TYPE_CONTACT_ATTACHMENT");

                            long userCount = message.getUsersCount();

                            if (userCount == 1) {
                                String name = "";
                                name = message.getUserName(0);
                                if (name.trim().isEmpty()) {
                                    name = message.getUserName(0);
                                }
                                String email = message.getUserName(0);
                                messageContent = email;
                            } else {
                                StringBuilder name = new StringBuilder("");
                                name.append(message.getUserName(0));
                                for (int k = 1; k < userCount; k++) {
                                    name.append(", " + message.getUserName(k));
                                }
                                messageContent = name.toString();
                            }
                        } else if (message.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                            logDebug("TYPE_TRUNCATE");

                            messageContent = context.getString(R.string.history_cleared_message);

                        } else if (message.getType() == MegaChatMessage.TYPE_CONTAINS_META) {
                            logDebug("TYPE_CONTAINS_META");
                            messageContent = checkMessageContentMeta(message);
                        } else {
                            logDebug("OTHER");
                            messageContent = message.getContent();
                        }

                        CharSequence cs = " ";
                        String title = chats.get(i).getTitle();
                        if (chats.get(i).isGroup()) {
                            long lastMsgSender = message.getUserHandle();

                            MegaChatRoom chatRoom = megaChatApi.getChatRoom(chats.get(i).getChatId());
                            String nameAction = chatRoom.getPeerFirstnameByHandle(lastMsgSender);
                            if (nameAction == null) {
                                nameAction = "";
                            }

                            if (nameAction.trim().length() <= 0) {
                                ChatController cC = new ChatController(context);
                                nameAction = cC.getFirstName(lastMsgSender, chatRoom);
                            }

                            cs = nameAction + " @ " + title + ": " + messageContent;
                        } else {
                            cs = title + ": " + messageContent;
                        }

                        inboxStyle.addLine(cs);
                    } else {
                        logWarning("Message NULL cannot be recovered");
                        break;
                    }
                }
            } else {
                logDebug("Do not show notification - opened chat");
            }
        }

        String textToShow = context.getResources().getQuantityString(R.plurals.plural_number_messages_chat_notification, (int) chats.size(), chats.size());

        notificationBuilder.setContentTitle("MEGA");
        notificationBuilder.setContentText(textToShow);
        inboxStyle.setSummaryText(textToShow);

        Notification notif = notificationBuilder.build();

        if (notif != null) {
            notificationManager.notify(NOTIFICATION_SUMMARY_CHAT, notif);
        } else {
            notificationManager.cancel(NOTIFICATION_SUMMARY_CHAT);
        }
    }

    private String checkMessageContentAttachmentOrVoiceClip(MegaChatMessage message) {
        MegaNodeList nodeList = message.getMegaNodeList();
        if (nodeList == null || nodeList.size() < 1) return message.getContent();
        if (!isVoiceClip(nodeList.get(0).getName())) return nodeList.get(0).getName();
        long duration = getVoiceClipDuration(nodeList.get(0));
        return "\uD83C\uDF99 " + milliSecondsToTimer(duration);
    }

    private String checkMessageContentMeta(MegaChatMessage message){
        MegaChatContainsMeta meta = message.getContainsMeta();
        if(meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
            return  "\uD83D\uDCCD " + context.getString(R.string.title_geolocation_message);
        }
        return message.getContent();
    }

    public Notification buildNotification(Uri uriParameter, String vibration, String groupKey, MegaChatRoom chat, ArrayList<MegaChatMessage> unreadMessageList) {
        logDebug("buildChatNotification");
        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
        intent.putExtra("CHAT_ID", chat.getChatId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) chat.getChatId(), intent, PendingIntent.FLAG_ONE_SHOT);

        String title;
        int unreadMessages = chat.getUnreadCount();
        logDebug("Unread messages: " + unreadMessages + "  chatID: " + chat.getChatId());
        if (unreadMessages != 0) {

            if (unreadMessages < 0) {
                unreadMessages = Math.abs(unreadMessages);
                logDebug("Unread number: " + unreadMessages);

                if (unreadMessages > 1) {
                    String numberString = "+" + unreadMessages;
                    title = chat.getTitle() + " (" + numberString + " " + context.getString(R.string.messages_chat_notification) + ")";
                } else {
                    title = chat.getTitle();
                }
            } else {

                if (unreadMessages > 1) {
                    String numberString = unreadMessages + "";
                    title = chat.getTitle() + " (" + numberString + " " + context.getString(R.string.messages_chat_notification) + ")";
                } else {
                    title = chat.getTitle();
                }
            }
        } else {
            title = chat.getTitle();
        }

        title = converterShortCodes(title);
        NotificationCompat.Builder notificationBuilderO = null;
        Notification.Builder notificationBuilder = null;
        Notification.MessagingStyle messagingStyleContent = null;
        NotificationCompat.MessagingStyle messagingStyleContentO = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelIdChatSimple, notificationChannelNameChatSimple, NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(true);
            channel.enableVibration(false);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }

            notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdChatSimple);
            notificationBuilderO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setAutoCancel(true)
                    .setShowWhen(true)
                    .setGroup(groupKey)
                    .setColor(ContextCompat.getColor(context, R.color.mega));
            messagingStyleContentO = new NotificationCompat.MessagingStyle(chat.getTitle());
        } else {
            notificationBuilder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setAutoCancel(true)
                    .setShowWhen(true)
                    .setGroup(groupKey);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notificationBuilder.setColor(ContextCompat.getColor(context, R.color.mega));
            }

            messagingStyleContent = new Notification.MessagingStyle(chat.getTitle());
        }

        int sizeFor = (int) unreadMessageList.size() - 1;
        for (int i = sizeFor; i >= 0; i--) {
            MegaChatMessage msg = unreadMessageList.get(i);
            logDebug("getMessage: chatID: " + chat.getChatId() + " " + unreadMessageList.get(i));
            String messageContent = "";

            if (msg != null) {
                if (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || msg.getType() == MegaChatMessage.TYPE_VOICE_CLIP) {
                    messageContent = checkMessageContentAttachmentOrVoiceClip(msg);
                } else if (msg.getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                    logDebug("TYPE_CONTACT_ATTACHMENT");

                    long userCount = msg.getUsersCount();

                    if (userCount == 1) {
                        String name = "";
                        name = msg.getUserName(0);
                        if (name.trim().isEmpty()) {
                            name = msg.getUserName(0);
                        }
                        String email = msg.getUserName(0);
                        messageContent = email;
                    } else {
                        StringBuilder name = new StringBuilder("");
                        name.append(msg.getUserName(0));
                        for (int j = 1; j < userCount; j++) {
                            name.append(", " + msg.getUserName(j));
                        }
                        messageContent = name.toString();
                    }
                } else if (msg.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                    logDebug("TYPE_TRUNCATE");
                    messageContent = context.getString(R.string.history_cleared_message);
                } else if (msg.getType() == MegaChatMessage.TYPE_CONTAINS_META) {
                    logDebug("TYPE_CONTAINS_META");
                    messageContent = checkMessageContentMeta(msg);

                } else {
                    logDebug("OTHER");
                    messageContent = msg.getContent();
                }

                messageContent = converterShortCodes(messageContent);
                String sender = chat.getPeerFirstnameByHandle(msg.getUserHandle());
                sender = converterShortCodes(sender);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    messagingStyleContentO.addMessage(messageContent, msg.getTimestamp(), sender);
                } else {
                    messagingStyleContent.addMessage(messageContent, msg.getTimestamp(), sender);
                }
            } else {
                logWarning("ERROR:buildIPCNotification:messageNULL");
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            messagingStyleContentO.setConversationTitle(title);

            notificationBuilderO.setStyle(messagingStyleContentO)
                    .setContentIntent(pendingIntent);
        } else {
            messagingStyleContent.setConversationTitle(title);

            notificationBuilder.setStyle(messagingStyleContent)
                    .setContentIntent(pendingIntent);
        }

        //Set when on notification
        int size = (int) unreadMessageList.size();

        MegaChatMessage lastMsg = unreadMessageList.get(0);

        if(lastMsg!=null){
            logDebug("Last message ts: " + lastMsg.getTimestamp());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilderO.setWhen(lastMsg.getTimestamp() * 1000);
            } else {
                notificationBuilder.setWhen(lastMsg.getTimestamp() * 1000);
            }
        }

        if (uriParameter != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilderO.setSound(uriParameter);
            } else {
                notificationBuilder.setSound(uriParameter);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!STRING_FALSE.equals(vibration)) {
                //use the channel with vibration
                notificationBuilderO.setChannelId(notificationChannelIdChatSummaryV2);
            }
        } else {
            if (STRING_TRUE.equals(vibration)) {
                notificationBuilder.setVibrate(new long[]{0, 500});
            }
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            //API 25 = Android 7.1
            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilderO.setPriority(NotificationManager.IMPORTANCE_HIGH);
            } else {
                notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
            }
        }

//        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();

//        if(chat.isGroup()){
//
//            if(msgUserHandle!=-1){
//                String nameAction = getParticipantShortName(msgUserHandle);
//
//                if(nameAction.isEmpty()){
//                    notificationBuilder.setContentText(msgContent);
//                    bigTextStyle.bigText(msgContent);
//                }
//                else{
//                    String source = "<b>"+nameAction+": </b>"+msgContent;
//
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        notificationContent = Html.fromHtml(source,Html.FROM_HTML_MODE_LEGACY);
//                    } else {
//                        notificationContent = Html.fromHtml(source);
//                    }
//                    notificationBuilder.setContentText(notificationContent);
//                    bigTextStyle.bigText(notificationContent);
//                }
//            }
//            else{
//                notificationBuilder.setContentText(msgContent);
//                bigTextStyle.bigText(msgContent);
//            }
//
//        }
//        else{
//            notificationBuilder.setContentText(msgContent);
//            bigTextStyle.bigText(msgContent);
//        }

        Bitmap largeIcon = setUserAvatar(chat);
        if(largeIcon!=null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilderO.setLargeIcon(largeIcon);
            }
            else{
                notificationBuilder.setLargeIcon(largeIcon);
            }
        }

//        notificationBuilder.setStyle(bigTextStyle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return notificationBuilderO.build();
        }
        else{
            return notificationBuilder.build();
        }
    }

    private Bitmap setUserAvatar(MegaChatRoom chat){
        logDebug("Chat ID: " + chat.getChatId());
        if(!chat.isGroup()) {
            File avatar = buildAvatarFile(context, chat.getPeerEmail(0) + ".jpg");
            if (isFileAvailable(avatar) && avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap != null)
                    return getCircleBitmap(bitmap);
            }
        }
        return createDefaultAvatar(chat);
    }

    private Bitmap createDefaultAvatar(MegaChatRoom chat){
        logDebug("Chat ID: " + chat.getChatId());

        int color;
        if(chat.isGroup()){
            color = getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR);
        }else{
            color = getColorAvatar(chat.getPeerHandle(0));
        }

        return getDefaultAvatar(color, chat.getTitle(), AVATAR_SIZE, true, true);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void createChatSummaryChannel(Context context) {
        String notificationChannelIdChatSummaryV2 = NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2;
        String notificationChannelNameChatSummary = NOTIFICATION_CHANNEL_CHAT_SUMMARY_NAME;
        String notificationChannelIdChatSummaryNoVibrate = NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_ID;
        String notificationChannelNameChatSummaryNoVibrate = NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_NAME;

        NotificationChannel channelWithVibration = new NotificationChannel(notificationChannelIdChatSummaryV2,notificationChannelNameChatSummary,NotificationManager.IMPORTANCE_HIGH);
        channelWithVibration.setShowBadge(true);
        channelWithVibration.setVibrationPattern(new long[] {0,500});
        //green light
        channelWithVibration.enableLights(true);
        channelWithVibration.setLightColor(Color.rgb(0,255,0));
        //current ringtone for notification
        channelWithVibration.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),Notification.AUDIO_ATTRIBUTES_DEFAULT);

        NotificationChannel channelNoVibration = new NotificationChannel(notificationChannelIdChatSummaryNoVibrate,notificationChannelNameChatSummaryNoVibrate,NotificationManager.IMPORTANCE_HIGH);
        channelNoVibration.setShowBadge(true);
        channelNoVibration.enableVibration(false);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            //delete old channel otherwise the new settings don't work.
            NotificationChannel oldChannel = manager.getNotificationChannel(NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID);
            if(oldChannel != null) {
                manager.deleteNotificationChannel(NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID);
            }
            manager.createNotificationChannel(channelWithVibration);
            manager.createNotificationChannel(channelNoVibration);
        }
    }

    public Notification buildSummary (String groupKey, boolean beep){
        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_CHAT_SUMMARY);
        intent.putExtra("CHAT_ID", -1);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 , intent, PendingIntent.FLAG_ONE_SHOT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!beep) {
                NotificationChannel channel = new NotificationChannel(notificationChannelIdChatSimple, notificationChannelNameChatSimple, NotificationManager.IMPORTANCE_LOW);
                channel.setShowBadge(true);
                channel.enableVibration(false);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }

                NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdChatSimple);
                notificationBuilderO.setColor(ContextCompat.getColor(context, R.color.mega));

                notificationBuilderO.setSmallIcon(R.drawable.ic_stat_notify)
                        .setShowWhen(true)
                        .setGroup(groupKey)
                        .setGroupSummary(true)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setVibrate(null);

                return notificationBuilderO.build();
            } else {
                boolean vibrationEnabled = true;
                ChatSettings chatSettings = dbH.getChatSettings();
                if (chatSettings != null){
                    if (chatSettings.getVibrationEnabled()!=null && !chatSettings.getVibrationEnabled().isEmpty()){
                        if (STRING_FALSE.equals(chatSettings.getVibrationEnabled())){
                            vibrationEnabled = false;
                        }
                    }
                }

                NotificationCompat.Builder notificationBuilderO = null;
                if (vibrationEnabled){
                    notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdChatSummaryV2);
                }
                else{
                    notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdChatSummaryNoVibrate);
                }

                notificationBuilderO.setColor(ContextCompat.getColor(context, R.color.mega));

                notificationBuilderO.setSmallIcon(R.drawable.ic_stat_notify)
                        .setShowWhen(true)
                        .setGroup(groupKey)
                        .setGroupSummary(true)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

                return notificationBuilderO.build();
            }
        }
        else{
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notificationBuilder.setColor(ContextCompat.getColor(context, R.color.mega));
            }

            notificationBuilder.setSmallIcon(R.drawable.ic_stat_notify)
                    .setShowWhen(true)
                    .setGroup(groupKey)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            return notificationBuilder.build();
        }
    }

    public void removeAllChatNotifications(){
        logDebug("removeAllChatNotifications");
        notificationManager.cancel(NOTIFICATION_SUMMARY_CHAT);
        notificationManager.cancel(NOTIFICATION_GENERAL_PUSH_CHAT);
        for(int id : notificationIds) {
            notificationManager.cancel(id);
        }
        notificationIds.clear();
        notificationManager.cancel(KeepAliveService.NEW_MESSAGE_NOTIFICATION_ID);
    }

    private String getFullName(MegaChatRoom chat) {
        String fullName = getNicknameContact(chat.getPeerHandle(0));
        if (fullName == null) {
            fullName = chat.getPeerFullname(0);
        }

        return fullName;
    }

    /**
     * Method for knowing how many buttons need to be displayed in notifications.
     *
     */
    private int getNumberButtons(){
        ArrayList<Long> currentCalls = getCallsParticipating();

        if (participatingInACall() && currentCalls != null) {

            ArrayList<MegaChatCall> callsOnHold = new ArrayList<>();
            ArrayList<MegaChatCall> callsActive = new ArrayList<>();
            for (Long currentCall : currentCalls) {
                MegaChatCall current = megaChatApi.getChatCall(currentCall);
                if (current != null) {
                    if (current.isOnHold()) {
                        callsOnHold.add(current);
                    } else {
                        callsActive.add(current);
                    }
                }
            }

            if ((!callsActive.isEmpty() && callsOnHold.isEmpty()) || (callsActive.isEmpty() && !callsOnHold.isEmpty())) {
                return THREE_BUTTONS;
            }
        }
        return TWO_BUTTONS;
    }

    /**
     * Method for obtaining the number of request required.
     *
     * @param type Type of button.
     * @return Number of request needed.
     */
    private int getNumberRequestNotifications(String type, long chatHandleInProgress) {
        switch (type) {
            case CallNotificationIntentService.DECLINE:
            case CallNotificationIntentService.IGNORE:
                return 1;

            case CallNotificationIntentService.ANSWER:
            case CallNotificationIntentService.END_ANSWER:
            case CallNotificationIntentService.END_JOIN:
                return 2;

            case CallNotificationIntentService.HOLD_ANSWER:
            case CallNotificationIntentService.HOLD_JOIN:
                if (megaChatApi.getChatCall(chatHandleInProgress).isOnHold()) {
                    return 1;
                }
                return 2;

            default:
                return 1;
        }
    }

    /**
     * Obtain the PendingIntent determined for a particular notification.
     *
     * @param chatIdCallInProgress Chat ID with call in progress.
     * @param chatIdCallToAnswer   Chat ID with a incoming call.
     * @param notificationId       notification ID.
     * @param type                 Type of answer.
     * @return The PendingIntent.
     */
    private PendingIntent getPendingIntent(long chatIdCallInProgress, long chatIdCallToAnswer, int notificationId, String type) {
        Intent intent = new Intent(context, CallNotificationIntentService.class);
        intent.putExtra(CHAT_ID_OF_CURRENT_CALL, chatIdCallInProgress);
        intent.putExtra(CHAT_ID_OF_INCOMING_CALL, chatIdCallToAnswer);
        intent.setAction(type);
        int requestCode = notificationId + getNumberRequestNotifications(type, chatIdCallInProgress);
        return PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void showIncomingCallNotification(MegaChatCall callToAnswer, MegaChatCall callInProgress) {
        logDebug("Call to answer ID: " + callToAnswer.getChatid() + ", Call in progress ID: " + callInProgress.getChatid());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1){

            long chatIdCallToAnswer = callToAnswer.getChatid();
            long chatIdCallInProgress = callInProgress.getChatid();

            MegaChatRoom chatToAnswer = megaChatApi.getChatRoom(chatIdCallToAnswer);

            int notificationId = (MegaApiJava.userHandleToBase64(callToAnswer.getId())).hashCode();

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //Create a channel for android Oreo or higher
                NotificationChannel channel = new NotificationChannel(notificationChannelIdIncomingCall, notificationChannelNameIncomingCall, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("");
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.setShowBadge(true);
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }else {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }

                    /*Customize notifications*/
            Bitmap avatarIcon = setUserAvatar(chatToAnswer);
            String titleChat;
            String titleCall;
            if (chatToAnswer.isGroup()) {
                titleChat = chatToAnswer.getTitle();
                titleCall = context.getString(R.string.title_notification_incoming_group_call);
            } else {
                titleChat = getFullName(chatToAnswer);
                titleCall = context.getString(R.string.title_notification_incoming_individual_call);
            }

            RemoteViews notificationLayout = new RemoteViews(context.getPackageName(), R.layout.layout_call_notifications);
            notificationLayout.setTextViewText(R.id.chat_title, titleChat);
            notificationLayout.setTextViewText(R.id.call_title, titleCall);
            if (avatarIcon != null) {
                notificationLayout.setImageViewBitmap(R.id.avatar_image, avatarIcon);
            }
            if(getNumberButtons() == TWO_BUTTONS){

                notificationLayout.setViewVisibility(R.id.small_layout, View.VISIBLE);
                notificationLayout.setViewVisibility(R.id.big_layout, View.GONE);

                if(chatToAnswer.isGroup()){
                    notificationLayout.setTextViewText(R.id.decline_button_text, context.getString(R.string.ignore_call_incoming).toUpperCase());
                    notificationLayout.setTextViewText(R.id.answer_button_text, context.getString(R.string.action_join).toUpperCase());

                    notificationLayout.setOnClickPendingIntent(R.id.decline_button_text, getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, notificationId, CallNotificationIntentService.IGNORE));
                    notificationLayout.setOnClickPendingIntent(R.id.answer_button_text, getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, notificationId, CallNotificationIntentService.ANSWER));

                }else{
                    notificationLayout.setTextViewText(R.id.decline_button_text, context.getString(R.string.contact_decline).toUpperCase());
                    notificationLayout.setTextViewText(R.id.answer_button_text, context.getString(R.string.answer_call_incoming).toUpperCase());

                    notificationLayout.setOnClickPendingIntent(R.id.decline_button_text, getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, notificationId, CallNotificationIntentService.DECLINE));
                    notificationLayout.setOnClickPendingIntent(R.id.answer_button_text, getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, notificationId, CallNotificationIntentService.ANSWER));
                }

            }else{
                notificationLayout.setViewVisibility(R.id.big_layout, View.VISIBLE);
                notificationLayout.setViewVisibility(R.id.small_layout, View.GONE);

                if(chatToAnswer.isGroup()){
                    notificationLayout.setTextViewText(R.id.first_button_text, context.getString(R.string.ignore_call_incoming).toUpperCase());
                    notificationLayout.setTextViewText(R.id.second_button_text, context.getString(R.string.hold_and_join_call_incoming).toUpperCase());
                    notificationLayout.setTextViewText(R.id.third_button_text, context.getString(R.string.end_and_join_call_incoming).toUpperCase());

                    notificationLayout.setOnClickPendingIntent(R.id.first_button_text, getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, notificationId, CallNotificationIntentService.IGNORE));
                    notificationLayout.setOnClickPendingIntent(R.id.second_button_text, getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, notificationId, CallNotificationIntentService.HOLD_JOIN));
                    notificationLayout.setOnClickPendingIntent(R.id.third_button_text, getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, notificationId, CallNotificationIntentService.END_JOIN));

                }else{
                    notificationLayout.setTextViewText(R.id.first_button_text, context.getString(R.string.contact_decline).toUpperCase());
                    notificationLayout.setTextViewText(R.id.second_button_text, context.getString(R.string.hold_and_answer_call_incoming).toUpperCase());
                    notificationLayout.setTextViewText(R.id.third_button_text, context.getString(R.string.end_and_answer_call_incoming).toUpperCase());

                    notificationLayout.setOnClickPendingIntent(R.id.first_button_text, getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, notificationId, CallNotificationIntentService.DECLINE));
                    notificationLayout.setOnClickPendingIntent(R.id.second_button_text, getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, notificationId, CallNotificationIntentService.HOLD_ANSWER));
                    notificationLayout.setOnClickPendingIntent(R.id.third_button_text, getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, notificationId, CallNotificationIntentService.END_ANSWER));
                }
            }

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, notificationChannelIdIncomingCall);
            notificationBuilder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());
            notificationBuilder.setCustomContentView(notificationLayout);
            notificationBuilder.setContentIntent(null);
            notificationBuilder.setCustomBigContentView(notificationLayout);
            notificationBuilder.setSmallIcon(R.drawable.ic_stat_notify);
            notificationBuilder.setColor(ContextCompat.getColor(context, R.color.mega));
            notificationBuilder.setAutoCancel(true);
            notificationBuilder.setVibrate(patternIncomingCall);
            notificationBuilder.setDeleteIntent(getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, notificationId, CallNotificationIntentService.IGNORE));


            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                //API 25 = Android 7.1
                notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
            } else {
                notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
            }


            notify(notificationId, notificationBuilder.build());
        }
        else{
            logWarning("Not supported incoming call notification: " + Build.VERSION.SDK_INT);
        }
    }

    /**
     * The Group incoming call notification, just one call at a time.
     *
     * @param callToAnswer The call that is being received.
     */
    public void showIncomingGroupCallNotification(MegaChatCall callToAnswer) {
        logDebug("Call to answer ID: " + callToAnswer.getChatid());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {

            MegaChatRoom chatToAnswer = megaChatApi.getChatRoom(callToAnswer.getChatid());
            int notificationId = (MegaApiJava.userHandleToBase64(callToAnswer.getId())).hashCode();

            Intent ignoreIntent = new Intent(context, CallNotificationIntentService.class);
            ignoreIntent.putExtra(CHAT_ID_OF_CURRENT_CALL, MEGACHAT_INVALID_HANDLE);
            ignoreIntent.putExtra(CHAT_ID_OF_INCOMING_CALL, callToAnswer.getChatid());

            ignoreIntent.setAction(CallNotificationIntentService.IGNORE);
            int requestCodeIgnore = notificationId + 1;
            PendingIntent pendingIntentIgnore = PendingIntent.getService(context, requestCodeIgnore, ignoreIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            Intent answerIntent = new Intent(context, CallNotificationIntentService.class);
            answerIntent.putExtra(CHAT_ID_OF_CURRENT_CALL, MEGACHAT_INVALID_HANDLE);
            answerIntent.putExtra(CHAT_ID_OF_INCOMING_CALL, callToAnswer.getChatid());

            answerIntent.setAction(CallNotificationIntentService.ANSWER);
            int requestCodeAnswer = notificationId + 1;
            PendingIntent pendingIntentAnswer = PendingIntent.getService(context, requestCodeAnswer, answerIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                logDebug("Oreo");

                //Create a channel for android Oreo or higher
                NotificationChannel channel = new NotificationChannel(notificationChannelIdIncomingCall, notificationChannelNameIncomingCall, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("");
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.setShowBadge(true);
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);

            } else {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            }

            /*Customize notification*/
            Bitmap avatarIcon = setUserAvatar(chatToAnswer);
            String titleChat;
            String titleCall;
            if (chatToAnswer.isGroup()) {
                titleChat = chatToAnswer.getTitle();
                titleCall = context.getString(R.string.title_notification_incoming_group_call);
            } else {
                titleChat = getFullName(chatToAnswer);
                titleCall = context.getString(R.string.title_notification_incoming_individual_call);
            }

            /*Normal*/
            RemoteViews notificationLayout = new RemoteViews(context.getPackageName(), R.layout.layout_group_call_notification);
            notificationLayout.setTextViewText(R.id.chat_title, titleChat);
            notificationLayout.setTextViewText(R.id.call_title, titleCall);
            if (avatarIcon != null) {
                notificationLayout.setImageViewBitmap(R.id.avatar_image, avatarIcon);
            }
            notificationLayout.setTextViewText(R.id.answer_button_text, context.getString(R.string.answer_call_incoming).toUpperCase());
            notificationLayout.setTextViewText(R.id.ignore_button_text, context.getString(R.string.ignore_call_incoming).toUpperCase());

            notificationLayout.setOnClickPendingIntent(R.id.answer_button_layout, pendingIntentAnswer);
            notificationLayout.setOnClickPendingIntent(R.id.ignore_button_layout, pendingIntentIgnore);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, notificationChannelIdIncomingCall);
            notificationBuilder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());
            notificationBuilder.setCustomContentView(notificationLayout);
            notificationBuilder.setContentIntent(null);
            notificationBuilder.setCustomBigContentView(notificationLayout);
            notificationBuilder.setSmallIcon(R.drawable.ic_stat_notify);
            notificationBuilder.setColor(ContextCompat.getColor(context, R.color.mega));
            notificationBuilder.setAutoCancel(true);
            notificationBuilder.setVibrate(patternIncomingCall);
            notificationBuilder.setDeleteIntent(pendingIntentIgnore);

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                //API 25 = Android 7.1
                notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
            } else {
                notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
            }

            notify(notificationId, notificationBuilder.build());
        } else {
            logWarning("Not supported incoming call notification: " + Build.VERSION.SDK_INT);
        }
    }

    public void checkOneGroupCall(long chatId){
        MegaChatCall groupCallIncoming = megaChatApi.getChatCall(chatId);
        showIncomingGroupCallNotification(groupCallIncoming);
    }

    public void checkQueuedCalls(){
        logDebug("checkQueuedCalls");

        MegaHandleList handleList = megaChatApi.getChatCalls();
        if(handleList!=null){
            long numberOfCalls = handleList.size();
            logDebug("Number of calls in progress: " + numberOfCalls);
            if (numberOfCalls>1){
                logDebug("MORE than one call in progress: " + numberOfCalls);
                MegaChatCall callInProgress = null;
                MegaChatCall callIncoming = null;

                for(int i=0; i<handleList.size(); i++){
                    MegaChatCall call = megaChatApi.getChatCall(handleList.get(i));
                    if(call!=null){
                        logDebug("Call ChatID: " + call.getChatid() + ", Status: " + call.getStatus());
                        if((call.getStatus()>=MegaChatCall.CALL_STATUS_IN_PROGRESS) && (call.getStatus()<MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION)){
                            callInProgress = call;
                            logDebug("FOUND Call in progress: " + callInProgress.getChatid());
                            break;
                        }
                    }
                }

                if(callInProgress==null){
                    long openCallChatId = MegaApplication.getOpenCallChatId();
                    logDebug("openCallId: " + openCallChatId);
                    if(openCallChatId!=-1){
                        MegaChatCall possibleCall = megaChatApi.getChatCall(openCallChatId);
                        if(possibleCall.getStatus()<MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION){
                            callInProgress = possibleCall;
                            logDebug("FOUND Call activity shown: " + callInProgress.getChatid());
                        }
                    }
                }

                for(int i=0; i<handleList.size(); i++){
                    MegaChatCall call = megaChatApi.getChatCall(handleList.get(i));
                    if(call!=null){

                        if(call.getStatus()<MegaChatCall.CALL_STATUS_IN_PROGRESS && (!call.isIgnored())){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if(notificationManager == null){
                                    notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                }

                                StatusBarNotification[] notifs = notificationManager.getActiveNotifications();
                                boolean shown=false;

                                long chatCallId = call.getId();
                                String notificationCallId = MegaApiJava.userHandleToBase64(chatCallId);
                                int notificationId = (notificationCallId).hashCode();

                                logDebug("Active Notifications: " + notifs.length);
                                for(int k = 0; k< notifs.length; k++){
                                    if(notifs[k].getId()==notificationId){
                                        logDebug("Notification for this call already shown");
                                        shown = true;
                                        break;
                                    }
                                }

                                if(!shown){
                                    if(callInProgress.getId()!=call.getId()){
                                        callIncoming = call;
                                        logDebug("FOUND Call incoming and NOT shown and NOT ignored: " + callIncoming.getChatid());
                                        break;
                                    }
                                }
                            }
                            else{
                                callIncoming = call;
                                logDebug("FOUND Call incoming and NOT shown and NOT ignored: " + callIncoming.getChatid());
                                break;
                            }
                        }
                    }
                }

                if(callInProgress!=null){
                    if(callIncoming!=null){
                        showIncomingCallNotification(callIncoming, callInProgress);
                    } else {
                        logError("ERROR:callIncoming is NULL");
                    }
                } else {
                    logWarning("callInProgress NOT found");
                }
            } else {
                logDebug("No calls to launch");
            }
        }
    }

    public void showMissedCallNotification(MegaChatCall call) {
        logDebug("Chat ID: " + call.getChatid() + ", Call ID: " + call.getId());

        MegaChatRoom chat = megaChatApi.getChatRoom(call.getChatid());
        String notificationContent;
        if (chat.isGroup()) {
            notificationContent = chat.getTitle();
        } else {
            notificationContent = getFullName(chat);
        }

        long chatCallId = call.getId();
        String notificationCallId = MegaApiJava.userHandleToBase64(chatCallId);
        int notificationId = (notificationCallId).hashCode() + NOTIFICATION_MISSED_CALL;

        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
        intent.putExtra("CHAT_ID", chat.getChatId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)chat.getChatId() , intent, PendingIntent.FLAG_ONE_SHOT);

        long[] pattern = {0, 1000};

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(notificationChannelIdInProgressMissedCall, notificationChannelNameInProgressMissedCall, NotificationManager.IMPORTANCE_HIGH);
            channel.setShowBadge(true);
            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdInProgressMissedCall);
            notificationBuilderO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(context.getString(R.string.missed_call_notification_title))
                    .setContentText(notificationContent)
                    .setAutoCancel(true)
                    .setVibrate(pattern)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)
                    .setColor(ContextCompat.getColor(context, R.color.mega))
                    .setPriority(NotificationManager.IMPORTANCE_HIGH);

            if (chat.getPeerEmail(0) != null) {

                Bitmap largeIcon = setUserAvatar(chat);
                if (largeIcon != null) {
                    notificationBuilderO.setLargeIcon(largeIcon);
                }
            }

            notify(notificationId, notificationBuilderO.build());
        }
        else {

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(context.getString(R.string.missed_call_notification_title))
                    .setContentText(notificationContent)
                    .setAutoCancel(true)
                    .setVibrate(pattern)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notificationBuilder.setColor(ContextCompat.getColor(context, R.color.mega));
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                //API 25 = Android 7.1
                notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
            } else {
                notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
            }

            if (chat.getPeerEmail(0) != null) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Bitmap largeIcon = setUserAvatar(chat);
                    if (largeIcon != null) {
                        notificationBuilder.setLargeIcon(largeIcon);
                    }
                }
            }

            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }

            notify(notificationId, notificationBuilder.build());
        }
    }

    public void generateChatNotification(MegaChatRequest request){
        logDebug("generateChatNotification");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            newGenerateChatNotification(request);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            String manufacturer = "xiaomi";
            if(!manufacturer.equalsIgnoreCase(Build.MANUFACTURER)) {
                logDebug("POST Android N");
                newGenerateChatNotification(request);
            }
            else{
                logDebug("XIAOMI POST Android N");
                generateChatNotificationPreN(request);
            }
        }
        else {
            logDebug("PRE Android N");
            generateChatNotificationPreN(request);
        }
    }

    public void newGenerateChatNotification(MegaChatRequest request){
        logDebug("newGenerateChatNotification");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean beep = request.getFlag();
            logDebug("Should beep: " + beep);

            MegaHandleList chatHandleList = request.getMegaHandleList();
            logDebug("chats size: " + chatHandleList.size());
            ArrayList<MegaChatListItem> chats = new ArrayList<>();
            for (int i = 0; i < chatHandleList.size(); i++) {
                MegaChatListItem chat = megaChatApi.getChatListItem(chatHandleList.get(i));
                chats.add(chat);
            }

            //Order by last interaction
            Collections.sort(chats, new Comparator<MegaChatListItem>() {

                public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                    long timestamp1 = c1.getLastTimestamp();
                    long timestamp2 = c2.getLastTimestamp();

                    long result = timestamp2 - timestamp1;
                    return (int) result;
                }
            });
            //Check if the last chat notification is enabled

            long lastChatId = -1;
            if (chats != null) {
                if (!(chats.isEmpty())) {
                    lastChatId = chats.get(0).getChatId();
                } else {
                    logError("ERROR:chatsEMPTY:removeAllChatNotifications");
                    removeAllChatNotifications();
                    return;
                }
            } else {
                logError("ERROR:chatsNULL:removeAllChatNotifications");
                removeAllChatNotifications();
                return;
            }

            logDebug("Generate chat notification for: " + chats.size() + " chats");

            boolean showNotif = false;

            if (MegaApplication.getOpenChatId() != lastChatId) {

                MegaHandleList handleListUnread = request.getMegaHandleListByChat(lastChatId);

                showNotif = shouldShowChatNotification(lastChatId, handleListUnread, beep);

                if (!showNotif) {
                    logDebug("Muted chat - do not show notification");
                }
            }

            logDebug("Generate chat notification for: " + chats.size() + " chats");
            if (showNotif) {
                for (int i = 0; i < chats.size(); i++) {
                    if (MegaApplication.getOpenChatId() != chats.get(i).getChatId()) {

                        MegaHandleList handleListUnread = request.getMegaHandleListByChat(chats.get(i).getChatId());

                        boolean showN = shouldCheckNotificationsSound(chats.get(i).getChatId(), handleListUnread, beep);
                        if (showN) {
                            showChatNotification(chats.get(i).getChatId(), handleListUnread, beep);
                            if (beep) {
                                beep = false;
                            }
                        }
                    } else {
                        logDebug("Do not show notification - opened chat");
                    }
                }
            } else {
                logDebug("Mute for the last chat");
            }
        }
        else{
            boolean beep = request.getFlag();
            logDebug("Should beep: " + beep);

            MegaHandleList chatHandleList = request.getMegaHandleList();
            ArrayList<MegaChatListItem> chats = new ArrayList<>();
            for (int i = 0; i < chatHandleList.size(); i++) {
                MegaChatListItem chat = megaChatApi.getChatListItem(chatHandleList.get(i));
                chats.add(chat);
            }

            //Order by last interaction
            Collections.sort(chats, new Comparator<MegaChatListItem>() {

                public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                    long timestamp1 = c1.getLastTimestamp();
                    long timestamp2 = c2.getLastTimestamp();

                    long result = timestamp2 - timestamp1;
                    return (int) result;
                }
            });


            //Check if the last chat notification is enabled

            long lastChatId = -1;
            if (chats != null) {
                if (!(chats.isEmpty())) {
                    lastChatId = chats.get(0).getChatId();
                } else {
                    logError("ERROR:chatsEMPTY:return");
                    return;
                }
            } else {
                logError("ERROR:chatsNULL:return");
                return;
            }

            logDebug("Generate chat notification for: " + chats.size() + " chats");

            boolean showNotif = false;

            if (MegaApplication.getOpenChatId() != lastChatId) {

                MegaHandleList handleListUnread = request.getMegaHandleListByChat(lastChatId);

                showNotif = shouldShowChatNotification(lastChatId, handleListUnread, beep);

                if (!showNotif) {
                    logDebug("Muted chat - do not show notification");
                }
            }

            if (showNotif) {
                for (int i = 0; i < chats.size(); i++) {
                    if (MegaApplication.getOpenChatId() != chats.get(i).getChatId()) {

                        MegaHandleList handleListUnread = request.getMegaHandleListByChat(chats.get(i).getChatId());

                        boolean showN = shouldCheckNotificationsSound(chats.get(i).getChatId(), handleListUnread, beep);
                        if (showN) {
                            showChatNotification(chats.get(i).getChatId(), handleListUnread, beep);
                            if (beep) {
                                beep = false;
                            }
                        }
                    } else {
                        logDebug("Do not show notification - opened chat");
                    }
                }

                Notification summary = buildSummary(GROUP_KEY, request.getFlag());
                notificationManager.notify(NOTIFICATION_SUMMARY_CHAT, summary);
            } else {
                logDebug("Mute for the last chat");
            }
        }
    }

    public void generateChatNotificationPreN(MegaChatRequest request){
        logDebug("generateChatNotificationPreN");
        boolean beep = request.getFlag();
        logDebug("Should beep: " + beep);

        MegaHandleList chatHandleList = request.getMegaHandleList();
        logDebug("size chatHandleList: " + chatHandleList.size());
        ArrayList<MegaChatListItem> chats = new ArrayList<>();
        for(int i=0; i<chatHandleList.size(); i++){
            MegaChatListItem chat = megaChatApi.getChatListItem(chatHandleList.get(i));
            chats.add(chat);
        }

        //Order by last interaction
        Collections.sort(chats, new Comparator<MegaChatListItem> (){

            public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                long timestamp1 = c1.getLastTimestamp();
                long timestamp2 = c2.getLastTimestamp();

                long result = timestamp2 - timestamp1;
                return (int)result;
            }
        });

        logDebug("Generate chat notification for: " + chats.size() + " chats");
        long lastChatId = -1;
        if(chats!=null && (!(chats.isEmpty()))){
            lastChatId = chats.get(0).getChatId();
            showChatNotificationPreN(request, beep, lastChatId);
        }else{
            removeAllChatNotifications();
        }
    }

    public void showChatNotificationPreN(MegaChatRequest request, boolean beep, long lastChatId){
        logDebug("Beep: " + beep + ", Last Chat ID: " + lastChatId);

        if(beep){
            ChatSettings chatSettings = dbH.getChatSettings();

            if (chatSettings != null) {

                if (chatSettings.getNotificationsEnabled()==null){
                    logDebug("getNotificationsEnabled NULL --> Notifications ON");
                    checkNotificationsSoundPreN(request, beep, lastChatId);
                }
                else{
                    if (STRING_TRUE.equals(chatSettings.getNotificationsEnabled())) {
                        logDebug("Notifications ON for all chats");
                        checkNotificationsSoundPreN(request, beep, lastChatId);
                    } else {
                        logDebug("Notifications OFF");
                    }
                }

            } else {
                logDebug("Notifications DEFAULT ON");

                Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                buildNotificationPreN(defaultSoundUri2, STRING_TRUE, request);
            }
        }
        else{
            buildNotificationPreN(null, STRING_FALSE, request);
        }
    }

    public void checkNotificationsSoundPreN(MegaChatRequest request, boolean beep, long lastChatId) {
        logDebug("Beep: " + beep + ", Last Chat ID: " + lastChatId);

        ChatSettings chatSettings = dbH.getChatSettings();
        ChatItemPreferences chatItemPreferences = dbH.findChatPreferencesByHandle(String.valueOf(lastChatId));

        if (chatItemPreferences == null || chatItemPreferences.getNotificationsEnabled() == null || chatItemPreferences.getNotificationsEnabled().isEmpty() || STRING_TRUE.equals(chatItemPreferences.getNotificationsEnabled())) {
            logDebug("Notifications OFF for this chat");

            if (chatSettings.getNotificationsSound() == null){
                logWarning("Notification sound is NULL");
                Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                buildNotificationPreN(defaultSoundUri, chatSettings.getVibrationEnabled(), request);
            }
            else if(chatSettings.getNotificationsSound().equals("-1")){
                logDebug("Silent notification Notification sound -1");
                buildNotificationPreN(null, chatSettings.getVibrationEnabled(), request);
            }
            else{
                String soundString = chatSettings.getNotificationsSound();
                Uri uri = Uri.parse(soundString);
                logDebug("Uri: " + uri);

                if (STRING_TRUE.equals(soundString) || "".equals(soundString)) {

                    Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                    buildNotificationPreN(defaultSoundUri, chatSettings.getVibrationEnabled(), request);
                } else if (soundString.equals("-1")) {
                    logDebug("Silent notification");
                    buildNotificationPreN(null, chatSettings.getVibrationEnabled(), request);
                } else {
                    Ringtone sound = RingtoneManager.getRingtone(context, uri);
                    if (sound == null) {
                        logWarning("Sound is null");
                        buildNotificationPreN(null, chatSettings.getVibrationEnabled(), request);
                    } else {
                        buildNotificationPreN(uri, chatSettings.getVibrationEnabled(), request);
                    }
                }
            }

        } else {
            logDebug("Notifications OFF for this chat");
        }
    }

    public boolean showChatNotification(long chatid, MegaHandleList handleListUnread, boolean beep){
        logDebug("Beep: " + beep);

        if(beep){

            ChatSettings chatSettings = dbH.getChatSettings();
            if (chatSettings != null) {
                if (chatSettings.getNotificationsEnabled()==null){
                    logDebug("getNotificationsEnabled NULL --> Notifications ON");

                    return checkNotificationsSound(chatid, handleListUnread, beep);
                }
                else{
                    if (STRING_TRUE.equals(chatSettings.getNotificationsEnabled())) {
                        logDebug("Notifications ON for all chats");

                        return checkNotificationsSound(chatid, handleListUnread, beep);
                    } else {
                        logDebug("Notifications OFF");
                        return false;
                    }
                }

            } else {
                logDebug("Notifications DEFAULT ON");

                Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                sendBundledNotification(defaultSoundUri2, STRING_TRUE, chatid, handleListUnread);
                return true;
            }
        }
        else{
            sendBundledNotification(null, STRING_FALSE, chatid, handleListUnread);
            return true;
        }
    }

    public boolean shouldShowChatNotification(long chatid, MegaHandleList handleListUnread, boolean beep){
        logDebug("Chat ID: " + chatid + ", Beep: " + beep);

        if(beep){

            ChatSettings chatSettings = dbH.getChatSettings();
            if (chatSettings != null) {
                if (chatSettings.getNotificationsEnabled()==null){
                    logDebug("getNotificationsEnabled NULL --> Notifications ON");

                    return shouldCheckNotificationsSound(chatid, handleListUnread, beep);
                }
                else{
                    if (STRING_TRUE.equals(chatSettings.getNotificationsEnabled())) {
                        logDebug("Notifications ON for all chats");

                        return shouldCheckNotificationsSound(chatid, handleListUnread, beep);
                    } else {
                        logDebug("Notifications OFF");
                        return false;
                    }
                }

            } else {
                logDebug("Notifications DEFAULT ON");
                return true;
            }
        }
        else{
            return true;
        }
    }

    public boolean checkNotificationsSound(long chatid, MegaHandleList handleListUnread, boolean beep){
        logDebug("Chat ID: " + chatid + ", Beep: " + beep);

        ChatSettings chatSettings = dbH.getChatSettings();
        ChatItemPreferences chatItemPreferences = dbH.findChatPreferencesByHandle(String.valueOf(chatid));

        if (chatItemPreferences == null || chatItemPreferences.getNotificationsEnabled() == null || chatItemPreferences.getNotificationsEnabled().isEmpty() ||STRING_TRUE.equals(chatItemPreferences.getNotificationsEnabled())) {
            logDebug("checkNotificationsSound: Notifications ON for this chat");

            removeAllChatNotifications();

            if (chatSettings.getNotificationsSound() == null){
                logWarning("Notification sound is NULL");
                Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                sendBundledNotification(defaultSoundUri, chatSettings.getVibrationEnabled(), chatid, handleListUnread);
            }
            else if(chatSettings.getNotificationsSound().equals("-1")){
                logDebug("Silent notification Notification sound -1");
                sendBundledNotification(null, chatSettings.getVibrationEnabled(), chatid, handleListUnread);
            }
            else{
                String soundString = chatSettings.getNotificationsSound();
                Uri uri = Uri.parse(soundString);
                logDebug("Uri: " + uri);

                if (STRING_TRUE.equals(soundString) || "".equals(soundString)) {

                    Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                    sendBundledNotification(defaultSoundUri, chatSettings.getVibrationEnabled(), chatid, handleListUnread);
                } else if (soundString.equals("-1")) {
                    logDebug("Silent notification");
                    sendBundledNotification(null, chatSettings.getVibrationEnabled(), chatid, handleListUnread);
                } else {
                    Ringtone sound = RingtoneManager.getRingtone(context, uri);
                    if (sound == null) {
                        logWarning("Sound is null");
                        sendBundledNotification(null, chatSettings.getVibrationEnabled(), chatid, handleListUnread);
                    } else {
                        sendBundledNotification(uri, chatSettings.getVibrationEnabled(), chatid, handleListUnread);
                    }
                }
            }
            return true;
        } else {
            logDebug("Notifications OFF for this chat");
            return false;
        }
    }

    public boolean shouldCheckNotificationsSound(long chatid, MegaHandleList handleListUnread, boolean beep){
        logDebug("Chat ID: " + chatid + ", Beep: " + beep);

        ChatSettings chatSettings = dbH.getChatSettings();
        ChatItemPreferences chatItemPreferences = dbH.findChatPreferencesByHandle(String.valueOf(chatid));

        if (chatItemPreferences == null || chatItemPreferences.getNotificationsEnabled() == null || chatItemPreferences.getNotificationsEnabled().isEmpty() ||STRING_TRUE.equals(chatItemPreferences.getNotificationsEnabled())) {
            logDebug("Notifications ON for this chat");
            return true;
        } else {
            logDebug("Notifications OFF for this chat");
            return false;
        }
    }
}
