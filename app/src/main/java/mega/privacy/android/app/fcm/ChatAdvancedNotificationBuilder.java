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
import static mega.privacy.android.app.utils.CallUtil.participatingInACall;
import static mega.privacy.android.app.utils.ChatUtil.getTitleChat;
import static mega.privacy.android.app.utils.Constants.ACTION_CHAT_NOTIFICATION_MESSAGE;
import static mega.privacy.android.app.utils.Constants.AVATAR_GROUP_CHAT_COLOR;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.CHAT_ID_OF_CURRENT_CALL;
import static mega.privacy.android.app.utils.Constants.CHAT_ID_OF_INCOMING_CALL;
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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.meeting.CallNotificationIntentService;
import mega.privacy.android.app.meeting.activity.MeetingActivity;
import mega.privacy.android.app.utils.CallUtil;
import mega.privacy.android.data.database.DatabaseHandler;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import timber.log.Timber;

public final class ChatAdvancedNotificationBuilder {

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

    private final long[] patternIncomingCall = {0, 1000, 1000, 1000, 1000, 1000, 1000};

    private static final Set<Integer> notificationIds = new HashSet<>();

    private final ChatController chatC;

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

    private void notify(int id, Notification notification) {
        notificationIds.add(id);
        notificationManager.notify(id, notification);
    }

    private void notifyCall(int id, Notification notification) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        notificationManager.notify(id, notification);
    }

    private Bitmap setUserAvatar(MegaChatRoom chat) {
        Timber.d("Chat ID: %s", chat.getChatId());
        if (!chat.isGroup()) {
            Bitmap bitmap = getImageAvatarCall(chat.getPeerHandle(0));
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
        NotificationChannel channelWithVibration = new NotificationChannel(NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2, NOTIFICATION_CHANNEL_CHAT_SUMMARY_NAME, NotificationManager.IMPORTANCE_HIGH);
        channelWithVibration.setShowBadge(true);
        channelWithVibration.setVibrationPattern(new long[]{0, 500});
        //green light
        channelWithVibration.enableLights(true);
        channelWithVibration.setLightColor(Color.rgb(0, 255, 0));
        //current ringtone for notification
        channelWithVibration.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), Notification.AUDIO_ATTRIBUTES_DEFAULT);

        NotificationChannel channelNoVibration = new NotificationChannel(NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_ID, NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_NAME, NotificationManager.IMPORTANCE_HIGH);
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
     * Method to show the incoming call notification, when there is another call in progress.
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
                expandedView.setTextViewText(R.id.decline_button_text, context.getString(R.string.contact_decline));
                expandedView.setTextViewText(R.id.answer_button_text, context.getString(R.string.answer_call_incoming));
                expandedView.setOnClickPendingIntent(R.id.decline_button_layout, intentDecline);
            } else {
                expandedView.setTextViewText(R.id.decline_button_text, context.getString(R.string.ignore_call_incoming));
                expandedView.setTextViewText(R.id.answer_button_text, context.getString(R.string.action_join));
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

                expandedView.setTextViewText(R.id.first_button_text, context.getString(R.string.contact_decline));
                expandedView.setTextViewText(R.id.second_button_text, context.getString(R.string.hold_and_answer_call_incoming));
                expandedView.setTextViewText(R.id.third_button_text, context.getString(R.string.end_and_answer_call_incoming));
                expandedView.setOnClickPendingIntent(R.id.first_button_layout, intentDecline);
                expandedView.setOnClickPendingIntent(R.id.second_button_layout, intentHoldAnswer);
                expandedView.setOnClickPendingIntent(R.id.third_button_layout, intentEndAnswer);
            } else {
                PendingIntent intentHoldJoin = getPendingIntent(chatIdCallInProgress, chatIdCallToAnswer, CallNotificationIntentService.HOLD_JOIN, notificationId);
                PendingIntent intentEndJoin = getPendingIntent(callToHangUpChatId, chatIdCallToAnswer, CallNotificationIntentService.END_JOIN, notificationId);

                expandedView.setTextViewText(R.id.first_button_text, context.getString(R.string.ignore_call_incoming));
                expandedView.setTextViewText(R.id.second_button_text, context.getString(R.string.hold_and_join_call_incoming));
                expandedView.setTextViewText(R.id.third_button_text, context.getString(R.string.end_and_join_call_incoming));
                expandedView.setOnClickPendingIntent(R.id.first_button_layout, intentIgnore);
                expandedView.setOnClickPendingIntent(R.id.second_button_layout, intentHoldJoin);
                expandedView.setOnClickPendingIntent(R.id.third_button_layout, intentEndJoin);
            }

            if (numberButtons.equals(VERTICAL_TWO_BUTTONS)) {
                expandedView.setViewVisibility(R.id.second_button_layout, GONE);
            }
        }

        RemoteViews contentView = numberButtons.equals(HORIZONTAL_TWO_BUTTONS) ? expandedView : collapsedViews;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Create a channel for android Oreo or higher
            String channelId = shouldVibrate ? NOTIFICATION_CHANNEL_INCOMING_CALLS_ID :
                    NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_ID;

            String channelName = shouldVibrate ? NOTIFICATION_CHANNEL_INCOMING_CALLS_NAME :
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
                    .setCustomHeadsUpContentView(contentView)
                    .setCustomContentView(collapsedViews)
                    .setCustomBigContentView(expandedView)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setContentIntent(callScreen)
                    .setShowWhen(true)
                    .setAutoCancel(false)
                    .setDeleteIntent(intentIgnore)
                    .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
                    .setPriority(NotificationManager.IMPORTANCE_HIGH);

            notifyCall(notificationId, notificationBuilderO.build());

        } else {
            long[] noVibrationPattern = new long[]{0L};
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomHeadsUpContentView(contentView)
                    .setCustomContentView(collapsedViews)
                    .setCustomBigContentView(expandedView)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setContentIntent(callScreen)
                    .setShowWhen(true)
                    .setAutoCancel(false)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setDeleteIntent(intentIgnore)
                    .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVibrate(shouldVibrate ? patternIncomingCall : noVibrationPattern);

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
            expandedView.setTextViewText(R.id.decline_button_text, context.getString(R.string.contact_decline));
            expandedView.setTextViewText(R.id.answer_button_text, context.getString(R.string.answer_call_incoming));
            expandedView.setOnClickPendingIntent(R.id.decline_button_layout, intentDecline);
        } else {
            expandedView.setTextViewText(R.id.decline_button_text, context.getString(R.string.ignore_call_incoming));
            expandedView.setTextViewText(R.id.answer_button_text, context.getString(R.string.action_join));
            expandedView.setOnClickPendingIntent(R.id.decline_button_layout, intentIgnore);
        }

        expandedView.setOnClickPendingIntent(R.id.answer_button_layout, intentAnswer);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Create a channel for android Oreo or higher
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_INCOMING_CALLS_ID, NOTIFICATION_CHANNEL_INCOMING_CALLS_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("");
            channel.enableLights(true);
            channel.enableVibration(true);

            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_INCOMING_CALLS_ID);
            notificationBuilderO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomHeadsUpContentView(expandedView)
                    .setCustomContentView(collapsedViews)
                    .setCustomBigContentView(expandedView)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setContentIntent(callScreen)
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
                    .setContentIntent(callScreen)
                    .setShowWhen(true)
                    .setAutoCancel(false)
                    .setDeleteIntent(intentIgnore)
                    .setVibrate(pattern)
                    .setSound(defaultSoundUri)
                    .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
                    .setPriority(Notification.PRIORITY_HIGH);

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
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID, NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setShowBadge(true);
            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            notificationManager.createNotificationChannel(channel);
            NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID);
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

            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);

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
}
