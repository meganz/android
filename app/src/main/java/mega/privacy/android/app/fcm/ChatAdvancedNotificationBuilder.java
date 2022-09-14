package mega.privacy.android.app.fcm;

import static android.view.View.GONE;
import static mega.privacy.android.app.utils.AvatarUtil.getColorAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getSpecificAvatarColor;
import static mega.privacy.android.app.utils.CallUtil.existsAnotherCall;
import static mega.privacy.android.app.utils.CallUtil.getCallNotificationId;
import static mega.privacy.android.app.utils.CallUtil.getCallsParticipating;
import static mega.privacy.android.app.utils.CallUtil.getImageAvatarCall;
import static mega.privacy.android.app.utils.CallUtil.getPendingIntentMeetingRinging;
import static mega.privacy.android.app.utils.CallUtil.isAnotherActiveCall;
import static mega.privacy.android.app.utils.CallUtil.milliSecondsToTimer;
import static mega.privacy.android.app.utils.CallUtil.participatingInACall;
import static mega.privacy.android.app.utils.ChatUtil.converterShortCodes;
import static mega.privacy.android.app.utils.ChatUtil.getNameContactAttachment;
import static mega.privacy.android.app.utils.ChatUtil.getTitleChat;
import static mega.privacy.android.app.utils.ChatUtil.getVoiceClipDuration;
import static mega.privacy.android.app.utils.ChatUtil.isEnableChatNotifications;
import static mega.privacy.android.app.utils.ChatUtil.isEnableGeneralChatNotifications;
import static mega.privacy.android.app.utils.ChatUtil.isVoiceClip;
import static mega.privacy.android.app.utils.Constants.ACTION_CHAT_NOTIFICATION_MESSAGE;
import static mega.privacy.android.app.utils.Constants.ACTION_CHAT_SUMMARY;
import static mega.privacy.android.app.utils.Constants.AVATAR_GROUP_CHAT_COLOR;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.CHAT_ID_OF_CURRENT_CALL;
import static mega.privacy.android.app.utils.Constants.CHAT_ID_OF_INCOMING_CALL;
import static mega.privacy.android.app.utils.Constants.INVALID_OPTION;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_GENERAL_PUSH_CHAT;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_MISSED_CALL;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_SUMMARY_CHAT;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.getCircleBitmap;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaChatMessage.TYPE_CALL_ENDED;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import mega.privacy.android.app.listeners.GetPeerAttributesListener;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.megachat.ChatSettings;
import mega.privacy.android.app.meeting.CallNotificationIntentService;
import mega.privacy.android.app.meeting.activity.MeetingActivity;
import mega.privacy.android.app.utils.CallUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.domain.entity.ChatRequest;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNodeList;
import timber.log.Timber;

public final class ChatAdvancedNotificationBuilder {

    private static final String GROUP_KEY = "Karere";
    private static final String THREE_BUTTONS = "THREE_BUTTONS";
    private static final String VERTICAL_TWO_BUTTONS = "VERTICAL_TWO_BUTTONS";
    private static final String HORIZONTAL_TWO_BUTTONS = "HORIZONTAL_TWO_BUTTONS";


    private static final int ONE_REQUEST_NEEDED = 1;
    private static final int TWO_REQUEST_NEEDED = 2;

    private final Context context;
    private NotificationManager notificationManager;
    DatabaseHandler dbH;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    private static final String STRING_FALSE = "false";
    private static final String STRING_TRUE = "true";

    private long[] patternIncomingCall = {0, 1000, 1000, 1000, 1000, 1000, 1000};

    private static Set<Integer> notificationIds = new HashSet<>();
    private static Set<Integer> callsNotificationIds = new HashSet<>();

    private String notificationChannelIdChatSimple = NOTIFICATION_CHANNEL_CHAT_ID;
    private String notificationChannelNameChatSimple = NOTIFICATION_CHANNEL_CHAT_NAME;
    private String notificationChannelIdChatSummaryV2 = NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2;
    private String notificationChannelIdChatSummaryNoVibrate = NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_ID;
    private String notificationChannelIdInProgressMissedCall = NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID;
    private String notificationChannelNameInProgressMissedCall = NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME;
    private String notificationChannelIdIncomingCall = NOTIFICATION_CHANNEL_INCOMING_CALLS_ID;
    private String notificationChannelNameIncomingCall = NOTIFICATION_CHANNEL_INCOMING_CALLS_NAME;

    private ChatRequest request;
    private boolean isUpdatingUserName;

    private ChatController chatC;

    public static ChatAdvancedNotificationBuilder newInstance(Context context) {
        Context appContext = context.getApplicationContext();
        Context safeContext = ContextCompat.createDeviceProtectedStorageContext(appContext);
        if (safeContext == null) {
            safeContext = appContext;
        }
        NotificationManager notificationManager = (NotificationManager) safeContext.getSystemService(Context.NOTIFICATION_SERVICE);

        return new ChatAdvancedNotificationBuilder(safeContext, notificationManager);
    }

    public ChatAdvancedNotificationBuilder(Context context, NotificationManager notificationManager) {
        this.context = context.getApplicationContext();
        this.notificationManager = notificationManager;

        dbH = DbHandlerModuleKt.getDbHandler();
        megaApi = MegaApplication.getInstance().getMegaApi();
        megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChatSummaryChannel(context);
        }

        chatC = new ChatController(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void sendBundledNotification(Uri uriParameter, String vibration, long chatId, List<Long> unreadHandleList) {
        MegaChatRoom chat = megaChatApi.getChatRoom(chatId);

        ArrayList<MegaChatMessage> unreadMessages = new ArrayList<>();
        for (int i = 0; i < unreadHandleList.size(); i++) {
            MegaChatMessage message = megaChatApi.getMessage(chatId, unreadHandleList.get(i));
            Timber.d("Chat: %d messagID: %d", chat.getChatId(), unreadHandleList.get(i));
            if (message != null) {
                unreadMessages.add(message);
            } else {
                Timber.w("Message cannot be recovered");
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

    private void notifyCall(int id, Notification notification) {
        callsNotificationIds.add(id);
        notificationManager.notify(id, notification);
    }

    public void buildNotificationPreN(Uri uriParameter, String vibration, ChatRequest request) {
        Timber.d("buildNotificationPreN");

        ArrayList<MegaChatListItem> chats = new ArrayList<>();

        for (Long chatHandle : request.getHandleList()) {
            MegaChatListItem chat = megaChatApi.getChatListItem(chatHandle);
            if (chat != null) {
                if (isEnableChatNotifications(chat.getChatId())) {
                    chats.add(chat);
                }
            } else {
                Timber.e("ERROR:chatNotRecovered:NULL");
                return;
            }
        }

        PendingIntent pendingIntent = null;

        if (chats.size() > 1) {
            Intent intent = new Intent(context, ManagerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(ACTION_CHAT_SUMMARY);
            intent.putExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);
            pendingIntent = PendingIntent.getActivity(context, (int) chats.get(0).getChatId(), intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

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
            Intent intent = new Intent(context, ManagerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
            intent.putExtra(CHAT_ID, chats.get(0).getChatId());
            pendingIntent = PendingIntent.getActivity(context, (int) chats.get(0).getChatId(), intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            Timber.e("ERROR:chatSIZE=0:return");
            return;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        notificationBuilder.setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

        notificationBuilder.setShowWhen(true);

        setSilentNotificationIfUpdatingUserName(uriParameter, vibration);

        if (uriParameter != null) {
            notificationBuilder.setSound(uriParameter);
        }

        if (STRING_TRUE.equals(vibration)) {
            notificationBuilder.setVibrate(new long[]{0, 500});
        }

        notificationBuilder.setStyle(inboxStyle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Use NotificationManager for devices running Android Nougat or above (API >= 24)
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        } else {
            // Otherwise, use NotificationCompat for devices running Android Marshmallow (API 23)
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        for (int i = 0; i < chats.size(); i++) {
            if (MegaApplication.getOpenChatId() != chats.get(i).getChatId()) {
                List<Long> handleListUnread = request.getPeersListByChatHandle().get(chats.get(i).getChatId());

                for (int j = 0; j < handleListUnread.size(); j++) {
                    Timber.d("Get message id: %d from chatId: %d", handleListUnread.get(j), chats.get(i).getChatId());
                    MegaChatMessage message = megaChatApi.getMessage(chats.get(i).getChatId(), handleListUnread.get(j));
                    if (message != null) {
                        String messageContent = converterShortCodes(getMessageContent(message));
                        String title = converterShortCodes(getTitleChat(chats.get(i)));
                        CharSequence cs;

                        if (chats.get(i).isGroup()) {
                            MegaChatRoom chat = megaChatApi.getChatRoom(chats.get(i).getChatId());
                            String nameAction = converterShortCodes(getSender(message, chat));

                            cs = nameAction + " @ " + title + ": " + messageContent;
                        } else {
                            cs = title + ": " + messageContent;
                        }

                        inboxStyle.addLine(cs);
                    } else {
                        Timber.w("Message NULL cannot be recovered");
                        break;
                    }
                }
            } else {
                Timber.d("Do not show notification - opened chat");
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

    private String getSender(MegaChatMessage msg, MegaChatRoom chatRoom) {
        if (chatRoom == null) return null;

        long lastMsgSender = msg.getUserHandle();
        String nameAction = chatC.getParticipantFirstName(lastMsgSender);

        if (isTextEmpty(nameAction)) {
            nameAction = context.getString(R.string.unknown_name_label);

            if (request != null) {
                MegaHandleList handleList = MegaHandleList.createInstance();
                handleList.addMegaHandle(msg.getUserHandle());
                megaChatApi.loadUserAttributes(chatRoom.getChatId(), handleList, new GetPeerAttributesListener(context, request));
            }
        }

        return nameAction;
    }

    /**
     * Checks if it is updating the name of the chat notification message.
     * If so, it silentiates the notification.
     *
     * @param uriParameter Uri which contains the sound of the notification
     * @param vibration    String which indicates if the notification should vibrate
     */
    private void setSilentNotificationIfUpdatingUserName(Uri uriParameter, String vibration) {
        if (isUpdatingUserName) {
            uriParameter = null;
            vibration = STRING_FALSE;
        }
    }

    private String getMessageContent(MegaChatMessage msg) {
        if (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || msg.getType() == MegaChatMessage.TYPE_VOICE_CLIP) {
            return checkMessageContentAttachmentOrVoiceClip(msg);
        } else if (msg.getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
            Timber.d("TYPE_CONTACT_ATTACHMENT");
            long userCount = msg.getUsersCount();
            String messageContent;

            if (userCount == 1) {
                messageContent = getNameContactAttachment(msg);
            } else {
                StringBuilder name = new StringBuilder("");
                name.append(msg.getUserName(0));
                for (int j = 1; j < userCount; j++) {
                    name.append(", " + msg.getUserName(j));
                }
                messageContent = name.toString();
            }

            return messageContent;
        } else if (msg.getType() == MegaChatMessage.TYPE_TRUNCATE) {
            Timber.d("TYPE_TRUNCATE");
            return context.getString(R.string.history_cleared_message);
        } else if (msg.getType() == MegaChatMessage.TYPE_CONTAINS_META) {
            Timber.d("TYPE_CONTAINS_META");
            return checkMessageContentMeta(msg);

        } else {
            Timber.d("OTHER");
            return msg.getContent();
        }
    }

    private String checkMessageContentAttachmentOrVoiceClip(MegaChatMessage message) {
        MegaNodeList nodeList = message.getMegaNodeList();
        if (nodeList == null || nodeList.size() < 1) return message.getContent();
        if (!isVoiceClip(nodeList.get(0).getName())) return nodeList.get(0).getName();
        long duration = getVoiceClipDuration(nodeList.get(0));
        return "\uD83C\uDF99 " + milliSecondsToTimer(duration);
    }

    private String checkMessageContentMeta(MegaChatMessage message) {
        MegaChatContainsMeta meta = message.getContainsMeta();
        if (meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
            return "\uD83D\uDCCD " + context.getString(R.string.title_geolocation_message);
        }
        return message.getContent();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public Notification buildNotification(Uri uriParameter, String vibration, String groupKey, MegaChatRoom chat, ArrayList<MegaChatMessage> unreadMessageList) {
        Intent intent = new Intent(context, ManagerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
        intent.putExtra(CHAT_ID, chat.getChatId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) chat.getChatId(), intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String title;
        int unreadMessages = chat.getUnreadCount();
        Timber.d("Unread messages: %d  chatID: %d", unreadMessages, chat.getChatId());
        if (unreadMessages != 0) {

            if (unreadMessages < 0) {
                unreadMessages = Math.abs(unreadMessages);
                Timber.d("Unread number: %s", unreadMessages);

                if (unreadMessages > 1) {
                    String numberString = "+" + unreadMessages;
                    title = getTitleChat(chat) + " (" + numberString + " " + context.getString(R.string.messages_chat_notification) + ")";
                } else {
                    title = getTitleChat(chat);
                }
            } else {

                if (unreadMessages > 1) {
                    String numberString = unreadMessages + "";
                    title = getTitleChat(chat) + " (" + numberString + " " + context.getString(R.string.messages_chat_notification) + ")";
                } else {
                    title = getTitleChat(chat);
                }
            }
        } else {
            title = getTitleChat(chat);
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
                    .setColor(ContextCompat.getColor(context, R.color.red_600_red_300));
            messagingStyleContentO = new NotificationCompat.MessagingStyle(getTitleChat(chat));
        } else {
            notificationBuilder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setAutoCancel(true)
                    .setShowWhen(true)
                    .setGroup(groupKey);

            notificationBuilder.setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

            messagingStyleContent = new Notification.MessagingStyle(getTitleChat(chat));
        }

        int sizeFor = (int) unreadMessageList.size() - 1;
        for (int i = sizeFor; i >= 0; i--) {
            MegaChatMessage msg = unreadMessageList.get(i);
            Timber.d("getMessage: chatID: %d %s", chat.getChatId(), unreadMessageList.get(i));
            if (msg != null) {
                String messageContent = msg.getType() == TYPE_CALL_ENDED ?
                        context.getString(R.string.missed_call_notification_title) : converterShortCodes(getMessageContent(msg));
                String sender = converterShortCodes(getSender(msg, chat));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    messagingStyleContentO.addMessage(messageContent, msg.getTimestamp(), sender);
                } else {
                    messagingStyleContent.addMessage(messageContent, msg.getTimestamp(), sender);
                }
            } else {
                Timber.w("ERROR:buildIPCNotification:messageNULL");
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

        if (lastMsg != null) {
            Timber.d("Last message ts: %s", lastMsg.getTimestamp());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilderO.setWhen(lastMsg.getTimestamp() * 1000);
            } else {
                notificationBuilder.setWhen(lastMsg.getTimestamp() * 1000);
            }
        }

        setSilentNotificationIfUpdatingUserName(uriParameter, vibration);

        if (uriParameter != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilderO.setSound(uriParameter);
            } else {
                notificationBuilder.setSound(uriParameter);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilderO.setChannelId(STRING_TRUE.equals(vibration) ? notificationChannelIdChatSummaryV2 : notificationChannelIdChatSummaryNoVibrate);
        } else {
            if (STRING_TRUE.equals(vibration)) {
                notificationBuilder.setVibrate(new long[]{0, 500});
            }
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            //API 25 = Android 7.1
            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
        } else {
            notificationBuilderO.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }

        Bitmap largeIcon = setUserAvatar(chat);
        if (largeIcon != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilderO.setLargeIcon(largeIcon);
            } else {
                notificationBuilder.setLargeIcon(largeIcon);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return notificationBuilderO.build();
        } else {
            return notificationBuilder.build();
        }
    }

    private Bitmap setUserAvatar(MegaChatRoom chat) {
        Timber.d("Chat ID: %s", chat.getChatId());
        if (!chat.isGroup()) {
            Bitmap bitmap = getImageAvatarCall(chat, chat.getPeerHandle(0));
            if (bitmap != null)
                return getCircleBitmap(bitmap);
        }
        return createDefaultAvatar(chat);
    }

    private Bitmap createDefaultAvatar(MegaChatRoom chat) {
        Timber.d("Chat ID: %s", chat.getChatId());

        int color;
        if (chat.isGroup()) {
            color = getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR);
        } else {
            color = getColorAvatar(chat.getPeerHandle(0));
        }

        return getDefaultAvatar(color, getTitleChat(chat), AVATAR_SIZE, true, true);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void createChatSummaryChannel(Context context) {
        String notificationChannelIdChatSummaryV2 = NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2;
        String notificationChannelNameChatSummary = NOTIFICATION_CHANNEL_CHAT_SUMMARY_NAME;
        String notificationChannelIdChatSummaryNoVibrate = NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_ID;
        String notificationChannelNameChatSummaryNoVibrate = NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_NAME;

        NotificationChannel channelWithVibration = new NotificationChannel(notificationChannelIdChatSummaryV2, notificationChannelNameChatSummary, NotificationManager.IMPORTANCE_HIGH);
        channelWithVibration.setShowBadge(true);
        channelWithVibration.setVibrationPattern(new long[]{0, 500});
        //green light
        channelWithVibration.enableLights(true);
        channelWithVibration.setLightColor(Color.rgb(0, 255, 0));
        //current ringtone for notification
        channelWithVibration.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), Notification.AUDIO_ATTRIBUTES_DEFAULT);

        NotificationChannel channelNoVibration = new NotificationChannel(notificationChannelIdChatSummaryNoVibrate, notificationChannelNameChatSummaryNoVibrate, NotificationManager.IMPORTANCE_HIGH);
        channelNoVibration.setShowBadge(true);
        channelNoVibration.enableVibration(false);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            //delete old channel otherwise the new settings don't work.
            NotificationChannel oldChannel = manager.getNotificationChannel(NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID);
            if (oldChannel != null) {
                manager.deleteNotificationChannel(NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID);
            }
            manager.createNotificationChannel(channelWithVibration);
            manager.createNotificationChannel(channelNoVibration);
        }
    }

    public Notification buildSummary(String groupKey, boolean beep) {
        Intent intent = new Intent(context, ManagerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_CHAT_SUMMARY);
        intent.putExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!beep) {
                NotificationChannel channel = new NotificationChannel(notificationChannelIdChatSimple, notificationChannelNameChatSimple, NotificationManager.IMPORTANCE_LOW);
                channel.setShowBadge(true);
                channel.enableVibration(false);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }

                NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdChatSimple);
                notificationBuilderO.setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

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
                if (chatSettings != null) {
                    if (chatSettings.getVibrationEnabled() != null && !chatSettings.getVibrationEnabled().isEmpty()) {
                        if (STRING_FALSE.equals(chatSettings.getVibrationEnabled())) {
                            vibrationEnabled = false;
                        }
                    }
                }

                NotificationCompat.Builder notificationBuilderO = null;

                if (vibrationEnabled) {
                    notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdChatSummaryV2);
                } else {
                    notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdChatSummaryNoVibrate);
                }

                notificationBuilderO.setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

                notificationBuilderO.setSmallIcon(R.drawable.ic_stat_notify)
                        .setShowWhen(true)
                        .setGroup(groupKey)
                        .setGroupSummary(true)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

                return notificationBuilderO.build();
            }
        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);

            notificationBuilder.setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

            notificationBuilder.setSmallIcon(R.drawable.ic_stat_notify)
                    .setShowWhen(true)
                    .setGroup(groupKey)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            return notificationBuilder.build();
        }
    }

    public void removeAllChatNotifications() {
        notificationManager.cancel(NOTIFICATION_SUMMARY_CHAT);
        notificationManager.cancel(NOTIFICATION_GENERAL_PUSH_CHAT);
        for (int id : notificationIds) {
            notificationManager.cancel(id);
        }
        notificationIds.clear();
    }

    /**
     * Method for knowing how many buttons need to be displayed in notifications.
     */
    private String getNumberButtons() {
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

            if (!callsActive.isEmpty()) {
                return VERTICAL_TWO_BUTTONS;
            }
        }

        return HORIZONTAL_TWO_BUTTONS;
    }

    /**
     * Method for obtaining the number of request required.
     *
     * @param type Type of button.
     * @return Number of request needed.
     */
    private int getNumberRequestNotifications(String type, long chatHandleInProgress) {
        switch (type) {
            case CallNotificationIntentService.ANSWER:
            case CallNotificationIntentService.END_ANSWER:
            case CallNotificationIntentService.END_JOIN:
                return TWO_REQUEST_NEEDED;

            case CallNotificationIntentService.HOLD_ANSWER:
            case CallNotificationIntentService.HOLD_JOIN:
                if (megaChatApi.getChatCall(chatHandleInProgress).isOnHold()) {
                    return ONE_REQUEST_NEEDED;
                }
                return TWO_REQUEST_NEEDED;

            default:
                return ONE_REQUEST_NEEDED;
        }
    }

    /**
     * Gets the determined PendingIntent for a particular notification.
     *
     * @param chatIdCallInProgress Chat ID with call in progress.
     * @param chatIdCallToAnswer   Chat ID with a incoming call.
     * @param type                 Type of answer.
     * @return The PendingIntent.
     */
    private PendingIntent getPendingIntent(long chatIdCallInProgress, long chatIdCallToAnswer, String type, int notificationId) {
        Intent intent = new Intent(context, CallNotificationIntentService.class);
        intent.putExtra(CHAT_ID_OF_CURRENT_CALL, chatIdCallInProgress);
        intent.putExtra(CHAT_ID_OF_INCOMING_CALL, chatIdCallToAnswer);
        intent.setAction(type);
        int requestCode = notificationId + getNumberRequestNotifications(type, chatIdCallInProgress);
        return PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Method for showing the incoming call notification, when exists another call in progress exists.
     *
     * @param callToAnswer   The call that is being received.
     * @param callInProgress The current call in progress.
     */
    private void showIncomingCallNotification(MegaChatCall callToAnswer, MegaChatCall callInProgress) {
        Timber.d("Call to answer ID: %d, Call in progress ID: %d", callToAnswer.getChatid(), callInProgress.getChatid());

        long chatIdCallToAnswer = callToAnswer.getChatid();
        long chatIdCallInProgress = callInProgress.getChatid();
        MegaChatRoom chatToAnswer = megaChatApi.getChatRoom(chatIdCallToAnswer);
        int notificationId = getCallNotificationId(callToAnswer.getCallId());
        boolean shouldVibrate = !participatingInACall();

        PendingIntent intentIgnore = getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, CallNotificationIntentService.IGNORE, notificationId);
        PendingIntent callScreen = getPendingIntentMeetingRinging(context, callToAnswer.getChatid(), notificationId + ONE_REQUEST_NEEDED);
        PendingIntent intentDecline = getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, CallNotificationIntentService.DECLINE, notificationId);

        Bitmap avatarIcon = setUserAvatar(chatToAnswer);

        //Collapsed
        RemoteViews collapsedViews = CallUtil.collapsedAndExpandedIncomingCallNotification(context, R.layout.layout_call_notifications, chatToAnswer, avatarIcon);

        //Expanded
        RemoteViews expandedView = CallUtil.collapsedAndExpandedIncomingCallNotification(context, R.layout.layout_call_notifications_expanded, chatToAnswer, avatarIcon);

        String numberButtons = getNumberButtons();

        if (!numberButtons.equals(THREE_BUTTONS) && !numberButtons.equals(VERTICAL_TWO_BUTTONS)) {
            collapsedViews.setViewVisibility(R.id.arrow, GONE);
            expandedView.setViewVisibility(R.id.arrow, GONE);
            expandedView.setViewVisibility(R.id.small_layout, View.VISIBLE);
            expandedView.setViewVisibility(R.id.big_layout, GONE);
            if (CallUtil.isOneToOneCall(chatToAnswer)) {
                expandedView.setTextViewText(R.id.decline_button_text, StringResourcesUtils.getString(R.string.contact_decline));
                expandedView.setTextViewText(R.id.answer_button_text, StringResourcesUtils.getString(R.string.answer_call_incoming));
                expandedView.setOnClickPendingIntent(R.id.decline_button_layout, intentDecline);
            } else {
                expandedView.setTextViewText(R.id.decline_button_text, StringResourcesUtils.getString(R.string.ignore_call_incoming));
                expandedView.setTextViewText(R.id.answer_button_text, StringResourcesUtils.getString(R.string.action_join));
                expandedView.setOnClickPendingIntent(R.id.decline_button_layout, intentIgnore);
            }
            PendingIntent intentAnswer = getPendingIntent(isAnotherActiveCall(chatIdCallInProgress), chatIdCallToAnswer, CallNotificationIntentService.ANSWER, notificationId);
            expandedView.setOnClickPendingIntent(R.id.answer_button_layout, intentAnswer);

        } else {
            collapsedViews.setViewVisibility(R.id.arrow, View.VISIBLE);
            collapsedViews.setOnClickPendingIntent(R.id.arrow, null);
            expandedView.setViewVisibility(R.id.arrow, View.VISIBLE);
            expandedView.setOnClickPendingIntent(R.id.arrow, null);
            expandedView.setViewVisibility(R.id.big_layout, View.VISIBLE);
            expandedView.setViewVisibility(R.id.small_layout, GONE);
            long callToHangUpChatId = existsAnotherCall(chatIdCallInProgress);
            if (CallUtil.isOneToOneCall(chatToAnswer)) {
                PendingIntent intentHoldAnswer = getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, CallNotificationIntentService.HOLD_ANSWER, notificationId);
                PendingIntent intentEndAnswer = getPendingIntent(callToHangUpChatId, chatIdCallToAnswer, CallNotificationIntentService.END_ANSWER, notificationId);

                expandedView.setTextViewText(R.id.first_button_text, StringResourcesUtils.getString(R.string.contact_decline));
                expandedView.setTextViewText(R.id.second_button_text, StringResourcesUtils.getString(R.string.hold_and_answer_call_incoming));
                expandedView.setTextViewText(R.id.third_button_text, StringResourcesUtils.getString(R.string.end_and_answer_call_incoming));
                expandedView.setOnClickPendingIntent(R.id.first_button_layout, intentDecline);
                expandedView.setOnClickPendingIntent(R.id.second_button_layout, intentHoldAnswer);
                expandedView.setOnClickPendingIntent(R.id.third_button_layout, intentEndAnswer);
            } else {
                PendingIntent intentHoldJoin = getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, CallNotificationIntentService.HOLD_JOIN, notificationId);
                PendingIntent intentEndJoin = getPendingIntent(callToHangUpChatId, chatIdCallToAnswer, CallNotificationIntentService.END_JOIN, notificationId);

                expandedView.setTextViewText(R.id.first_button_text, StringResourcesUtils.getString(R.string.ignore_call_incoming));
                expandedView.setTextViewText(R.id.second_button_text, StringResourcesUtils.getString(R.string.hold_and_join_call_incoming));
                expandedView.setTextViewText(R.id.third_button_text, StringResourcesUtils.getString(R.string.end_and_join_call_incoming));
                expandedView.setOnClickPendingIntent(R.id.first_button_layout, intentIgnore);
                expandedView.setOnClickPendingIntent(R.id.second_button_layout, intentHoldJoin);
                expandedView.setOnClickPendingIntent(R.id.third_button_layout, intentEndJoin);
            }

            if (numberButtons.equals(VERTICAL_TWO_BUTTONS)) {
                expandedView.setViewVisibility(R.id.second_button_layout, GONE);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Create a channel for android Oreo or higher
            String channelId = shouldVibrate ? notificationChannelIdIncomingCall :
                    NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_ID;

            String channelName = shouldVibrate ? notificationChannelNameIncomingCall :
                    NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_NAME;

            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            channel.setVibrationPattern(shouldVibrate ? patternIncomingCall : new long[]{0L});
            channel.enableLights(true);
            channel.enableVibration(shouldVibrate);
            channel.setDescription("");

            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }

            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, channelId);
            notificationBuilderO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomHeadsUpContentView(numberButtons.equals(HORIZONTAL_TWO_BUTTONS) ? expandedView : collapsedViews)
                    .setCustomContentView(collapsedViews)
                    .setCustomBigContentView(expandedView)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(callScreen, true)
                    .setShowWhen(true)
                    .setAutoCancel(false)
                    .setDeleteIntent(intentIgnore)
                    .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
                    .setPriority(NotificationManager.IMPORTANCE_HIGH);

            notifyCall(notificationId, notificationBuilderO.build());

        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomHeadsUpContentView(numberButtons.equals(HORIZONTAL_TWO_BUTTONS) ? expandedView : collapsedViews)
                    .setCustomContentView(collapsedViews)
                    .setCustomBigContentView(expandedView)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(callScreen, true)
                    .setShowWhen(true)
                    .setAutoCancel(false)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setDeleteIntent(intentIgnore)
                    .setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

            if (shouldVibrate) {
                notificationBuilder.setVibrate(patternIncomingCall);
            } else {
                notificationBuilder.setDefaults(Notification.DEFAULT_SOUND)
                        .setVibrate(new long[]{0L});
            }

            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);

            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }

            notifyCall(notificationId, notificationBuilder.build());
        }
    }

    /**
     * Method for showing a incoming group or one-to-one call notification, when no other call is in progress
     *
     * @param callToAnswer The call that is being received.
     */
    public void showOneCallNotification(MegaChatCall callToAnswer) {
        Timber.d("Call to answer ID: %s", callToAnswer.getChatid());

        long chatIdCallToAnswer = callToAnswer.getChatid();
        MegaChatRoom chatToAnswer = megaChatApi.getChatRoom(chatIdCallToAnswer);
        int notificationId = getCallNotificationId(callToAnswer.getCallId());

        PendingIntent intentIgnore = getPendingIntent(MEGACHAT_INVALID_HANDLE, chatIdCallToAnswer, CallNotificationIntentService.IGNORE, notificationId);
        PendingIntent callScreen = getPendingIntentMeetingRinging(context, callToAnswer.getChatid(), notificationId + ONE_REQUEST_NEEDED);

        Intent answerIntent;
        PendingIntent intentAnswer;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Notification trampoline restrictions
            answerIntent = new Intent(context, MeetingActivity.class);
            answerIntent.putExtra(CHAT_ID_OF_CURRENT_CALL, MEGACHAT_INVALID_HANDLE);
            answerIntent.putExtra(CHAT_ID_OF_INCOMING_CALL, callToAnswer.getChatid());
            answerIntent.setAction(CallNotificationIntentService.ANSWER);
            intentAnswer = PendingIntent.getActivity(context, notificationId + ONE_REQUEST_NEEDED, answerIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            answerIntent = new Intent(context, CallNotificationIntentService.class);
            answerIntent.putExtra(CHAT_ID_OF_CURRENT_CALL, MEGACHAT_INVALID_HANDLE);
            answerIntent.putExtra(CHAT_ID_OF_INCOMING_CALL, callToAnswer.getChatid());
            answerIntent.setAction(CallNotificationIntentService.ANSWER);
            intentAnswer = PendingIntent.getService(context, notificationId + ONE_REQUEST_NEEDED, answerIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        Bitmap avatarIcon = setUserAvatar(chatToAnswer);

        //Collapsed
        RemoteViews collapsedViews = CallUtil.collapsedAndExpandedIncomingCallNotification(context, R.layout.layout_call_notifications, chatToAnswer, avatarIcon);
        collapsedViews.setViewVisibility(R.id.arrow, GONE);

        //Expanded
        RemoteViews expandedView = CallUtil.collapsedAndExpandedIncomingCallNotification(context, R.layout.layout_call_notifications_expanded, chatToAnswer, avatarIcon);
        expandedView.setViewVisibility(R.id.arrow, GONE);
        expandedView.setViewVisibility(R.id.small_layout, View.VISIBLE);
        expandedView.setViewVisibility(R.id.big_layout, GONE);

        if (CallUtil.isOneToOneCall(chatToAnswer)) {
            PendingIntent intentDecline = getPendingIntent(MEGACHAT_INVALID_HANDLE, chatIdCallToAnswer, CallNotificationIntentService.DECLINE, notificationId);
            expandedView.setTextViewText(R.id.decline_button_text, StringResourcesUtils.getString(R.string.contact_decline));
            expandedView.setTextViewText(R.id.answer_button_text, StringResourcesUtils.getString(R.string.answer_call_incoming));
            expandedView.setOnClickPendingIntent(R.id.decline_button_layout, intentDecline);
        } else {
            expandedView.setTextViewText(R.id.decline_button_text, StringResourcesUtils.getString(R.string.ignore_call_incoming));
            expandedView.setTextViewText(R.id.answer_button_text, StringResourcesUtils.getString(R.string.action_join));
            expandedView.setOnClickPendingIntent(R.id.decline_button_layout, intentIgnore);
        }

        expandedView.setOnClickPendingIntent(R.id.answer_button_layout, intentAnswer);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Create a channel for android Oreo or higher
            NotificationChannel channel = new NotificationChannel(notificationChannelIdIncomingCall, notificationChannelNameIncomingCall, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("");
            channel.enableLights(true);
            channel.enableVibration(true);

            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdIncomingCall);
            notificationBuilderO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomHeadsUpContentView(expandedView)
                    .setCustomContentView(collapsedViews)
                    .setCustomBigContentView(expandedView)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(callScreen, true)
                    .setShowWhen(true)
                    .setAutoCancel(false)
                    .setDeleteIntent(intentIgnore)
                    .setVibrate(patternIncomingCall)
                    .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
                    .setPriority(NotificationManager.IMPORTANCE_HIGH);

            notifyCall(notificationId, notificationBuilderO.build());
        } else {
            long[] pattern = {0, 1000};
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomHeadsUpContentView(expandedView)
                    .setCustomContentView(collapsedViews)
                    .setCustomBigContentView(expandedView)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(callScreen, true)
                    .setShowWhen(true)
                    .setAutoCancel(false)
                    .setDeleteIntent(intentIgnore)
                    .setVibrate(pattern)
                    .setSound(defaultSoundUri)
                    .setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);

            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }

            Timber.w("Notify incoming call");
            notifyCall(notificationId, notificationBuilder.build());
        }
    }

    public void checkQueuedCalls(long incomingCallChatId) {
        Timber.d("Check several calls");

        MegaHandleList handleList = megaChatApi.getChatCalls();
        if (handleList == null || handleList.size() <= 1)
            return;

        ArrayList<Long> callsInProgress = getCallsParticipating();
        if (callsInProgress == null || callsInProgress.isEmpty()) {
            Timber.w("No calls in progress");
            return;
        }

        Timber.d("Number of calls in progress: %s", callsInProgress.size());
        MegaChatCall callInProgress = null;
        for (int i = 0; i < callsInProgress.size(); i++) {
            MegaChatCall call = megaChatApi.getChatCall(callsInProgress.get(i));
            if (call != null && !call.isOnHold()) {
                callInProgress = call;
                break;
            }
        }

        if (callInProgress == null) {
            MegaChatCall call = megaChatApi.getChatCall(callsInProgress.get(0));
            if (call != null && !call.isOnHold()) {
                callInProgress = call;
            }
        }

        MegaChatCall incomingCall = megaChatApi.getChatCall(incomingCallChatId);
        if (callInProgress != null && incomingCall != null && incomingCall.isRinging() && !incomingCall.isIgnored()) {
            MegaApplication.getChatManagement().addNotificationShown(incomingCall.getChatid());
            showIncomingCallNotification(incomingCall, callInProgress);
        }
    }

    public void showMissedCallNotification(long chatId, long chatCallId) {
        Timber.d("MISSED CALL Chat ID: %d, Call ID: %d", chatId, chatCallId);

        MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
        String notificationContent;
        if (chat.isGroup()) {
            notificationContent = getTitleChat(chat);
        } else {
            notificationContent = chatC.getParticipantFullName(chat.getPeerHandle(0));
        }

        String notificationCallId = MegaApiJava.userHandleToBase64(chatCallId);
        int notificationId = (notificationCallId).hashCode() + NOTIFICATION_MISSED_CALL;

        Intent intent = new Intent(context, ManagerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
        intent.putExtra(CHAT_ID, chat.getChatId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) chat.getChatId(), intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        long[] pattern = {0, 1000};

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                    .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
                    .setPriority(NotificationManager.IMPORTANCE_HIGH);

            if (!isTextEmpty(chatC.getParticipantEmail(chat.getPeerHandle(0)))) {

                Bitmap largeIcon = setUserAvatar(chat);
                if (largeIcon != null) {
                    notificationBuilderO.setLargeIcon(largeIcon);
                }
            }

            notify(notificationId, notificationBuilderO.build());
        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(context.getString(R.string.missed_call_notification_title))
                    .setContentText(notificationContent)
                    .setAutoCancel(true)
                    .setVibrate(pattern)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);

            notificationBuilder.setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use NotificationManager for devices running Android Nougat or above (API >= 24)
                notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
            } else {
                // Otherwise, use NotificationCompat for devices running Android Marshmallow (API 23)
                notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
            }

            if (!isTextEmpty(chatC.getParticipantEmail(chat.getPeerHandle(0)))) {

                Bitmap largeIcon = setUserAvatar(chat);
                if (largeIcon != null) {
                    notificationBuilder.setLargeIcon(largeIcon);
                }
            }

            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }

            notify(notificationId, notificationBuilder.build());
        }
    }

    public void generateChatNotification(ChatRequest request) {
        Timber.d("generateChatNotification");
        this.request = request;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            newGenerateChatNotification(request);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            String manufacturer = "xiaomi";
            if (!manufacturer.equalsIgnoreCase(Build.MANUFACTURER)) {
                Timber.d("POST Android N");
                newGenerateChatNotification(request);
            } else {
                Timber.d("XIAOMI POST Android N");
                generateChatNotificationPreN(request);
            }
        } else {
            Timber.d("PRE Android N");
            generateChatNotificationPreN(request);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void newGenerateChatNotification(ChatRequest request) {
        Timber.d("newGenerateChatNotification");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean beep = request.getFlag();
            Timber.d("Should beep: %s", beep);

            List<Long> chatHandleList = request.getHandleList();
            Timber.d("chats size: %s", chatHandleList.size());
            ArrayList<MegaChatListItem> chats = new ArrayList<>();


            for (Long chatHandle : chatHandleList) {
                chats.add(megaChatApi.getChatListItem(chatHandle));
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

            long lastChatId;
            if (!(chats.isEmpty())) {
                lastChatId = chats.get(0).getChatId();
            } else {
                Timber.e("Chats empty, remove all chat notifications");
                removeAllChatNotifications();
                return;
            }

            checkShowChatNotifications(lastChatId, beep, request, chats);
        } else {
            boolean beep = request.getFlag();
            Timber.d("Should beep: %s", beep);

            ArrayList<MegaChatListItem> chats = new ArrayList<>();
            for (Long chatHandle : request.getHandleList()) {
                chats.add(megaChatApi.getChatListItem(chatHandle));
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
                    Timber.e("ERROR:chatsEMPTY:return");
                    return;
                }
            } else {
                Timber.e("ERROR:chatsNULL:return");
                return;
            }

            checkShowChatNotifications(lastChatId, beep, request, chats);
            Notification summary = buildSummary(GROUP_KEY, request.getFlag());
            notificationManager.notify(NOTIFICATION_SUMMARY_CHAT, summary);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkShowChatNotifications(long lastChatId, boolean beep, ChatRequest request, ArrayList<MegaChatListItem> chats) {
        if (MegaApplication.getOpenChatId() != lastChatId) {
            Timber.d("Generate chat notification for: %d chats", chats.size());
            for (int i = 0; i < chats.size(); i++) {
                if (megaApi.isChatNotifiable(chats.get(i).getChatId()) && MegaApplication.getOpenChatId() != chats.get(i).getChatId()) {
                    List<Long> handleListUnread = request.getPeersListByChatHandle().get(chats.get(i).getChatId());
                    showChatNotification(chats.get(i).getChatId(), handleListUnread, beep);
                    beep = false;
                }
            }
        } else {
            Timber.d("Mute for the last chat");
        }
    }

    public void generateChatNotificationPreN(ChatRequest request) {
        Timber.d("generateChatNotificationPreN");
        boolean beep = request.getFlag();
        Timber.d("Should beep: %s", beep);

        List<Long> chatHandleList = request.getHandleList();
        Timber.d("chats size: %s", chatHandleList.size());
        ArrayList<MegaChatListItem> chats = new ArrayList<>();


        for (Long chatHandle : chatHandleList) {
            chats.add(megaChatApi.getChatListItem(chatHandle));
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

        Timber.d("Generate chat notification for: %d chats", chats.size());
        if (!chats.isEmpty() && isEnableGeneralChatNotifications()) {
            showChatNotificationPreN(request, beep, chats.get(0).getChatId());
        } else {
            removeAllChatNotifications();
        }
    }

    public void showChatNotificationPreN(ChatRequest request, boolean beep, long lastChatId) {
        Timber.d("Beep: %s, Last Chat ID: %d", beep, lastChatId);

        if (beep) {
            ChatSettings chatSettings = dbH.getChatSettings();
            if (chatSettings != null) {
                checkNotificationsSoundPreN(request, beep, lastChatId);
                return;
            }
        }

        buildNotificationPreN(beep ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : null, beep ? STRING_TRUE : STRING_FALSE, request);
    }

    public void checkNotificationsSoundPreN(ChatRequest request, boolean beep, long lastChatId) {
        Timber.d("Beep: %s, Last Chat ID: %d", beep, lastChatId);

        ChatSettings chatSettings = dbH.getChatSettings();
        Timber.d("Notifications OFF for this chat");

        if (chatSettings.getNotificationsSound() == null) {
            Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
            buildNotificationPreN(defaultSoundUri, chatSettings.getVibrationEnabled(), request);
        } else if (chatSettings.getNotificationsSound().equals(INVALID_OPTION)) {
            buildNotificationPreN(null, chatSettings.getVibrationEnabled(), request);
        } else {
            String soundString = chatSettings.getNotificationsSound();
            Uri uri = Uri.parse(soundString);
            Timber.d("Uri: %s", uri);

            if (STRING_TRUE.equals(soundString) || isTextEmpty(soundString)) {
                Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                buildNotificationPreN(defaultSoundUri, chatSettings.getVibrationEnabled(), request);
            } else if (soundString.equals(INVALID_OPTION)) {
                buildNotificationPreN(null, chatSettings.getVibrationEnabled(), request);
            } else {
                Ringtone sound = RingtoneManager.getRingtone(context, uri);
                if (sound == null) {
                    buildNotificationPreN(null, chatSettings.getVibrationEnabled(), request);
                } else {
                    buildNotificationPreN(uri, chatSettings.getVibrationEnabled(), request);
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean showChatNotification(long chatid, List<Long> handleListUnread, boolean beep) {
        Timber.d("Beep: %s", beep);
        if (beep) {
            ChatSettings chatSettings = dbH.getChatSettings();
            if (chatSettings != null) {
                checkNotificationsSound(chatid, handleListUnread, beep);
                return true;
            }
        }

        sendBundledNotification(beep ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : null, beep ? STRING_TRUE : STRING_FALSE, chatid, handleListUnread);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkNotificationsSound(long chatid, List<Long> handleListUnread, boolean beep) {
        Timber.d("Chat ID: %d, Beep: %s", chatid, beep);

        ChatSettings chatSettings = dbH.getChatSettings();
        removeAllChatNotifications();
        Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);

        if (chatSettings == null ||
                chatSettings.getNotificationsSound() == null ||
                chatSettings.getNotificationsSound().equals(INVALID_OPTION)) {
            defaultSoundUri = null;
        } else if (chatSettings.getNotificationsSound() != null) {
            String soundString = chatSettings.getNotificationsSound();
            Uri uri = Uri.parse(soundString);

            if (STRING_TRUE.equals(soundString) || isTextEmpty(soundString)) {
                defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
            } else if (soundString.equals(INVALID_OPTION) || RingtoneManager.getRingtone(context, uri) == null) {
                defaultSoundUri = null;
            } else {
                defaultSoundUri = uri;
            }
        }

        sendBundledNotification(defaultSoundUri, chatSettings == null ? STRING_TRUE : chatSettings.getVibrationEnabled(), chatid, handleListUnread);
    }

    public void setIsUpdatingUserName() {
        isUpdatingUserName = true;
    }
}
