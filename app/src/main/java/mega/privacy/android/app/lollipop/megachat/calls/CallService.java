package mega.privacy.android.app.lollipop.megachat.calls;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class CallService extends Service{

    MegaApplication app;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    private long currentChatId;

    private Notification.Builder mBuilder;
    private NotificationCompat.Builder mBuilderCompat;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilderCompatO;

    private String notificationChannelId = NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID;
    private String notificationChannelName = NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME;

    private BroadcastReceiver chatCallUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            long chatIdReceived = intent.getLongExtra(UPDATE_CHAT_CALL_ID, MEGACHAT_INVALID_HANDLE);
            if (chatIdReceived == MEGACHAT_INVALID_HANDLE || chatIdReceived != currentChatId) {
                logError("Invalid call or different call:: "+chatIdReceived);
                return;
            }

            if (intent.getAction().equals(ACTION_CALL_STATUS_UPDATE)) {
                int callStatus = intent.getIntExtra(UPDATE_CALL_STATUS, INVALID_CALL_STATUS);
                logDebug("Call status " + callStatusToString(callStatus)+" current chat = "+currentChatId);
                switch (callStatus) {
                    case MegaChatCall.CALL_STATUS_REQUEST_SENT:
                    case MegaChatCall.CALL_STATUS_RING_IN:
                    case MegaChatCall.CALL_STATUS_JOINING:
                    case MegaChatCall.CALL_STATUS_IN_PROGRESS:
                        updateNotificationContent();
                        break;
                    case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
                    case MegaChatCall.CALL_STATUS_DESTROYED:
                        removeNotification();
                        break;
                }
            }

            if (intent.getAction().equals(ACTION_CHANGE_CALL_ON_HOLD)) {
                checkAnotherActiveCall();
            }
        }
    };

    public void onCreate() {
        super.onCreate();
        app = (MegaApplication) getApplication();
        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            mBuilder = new Notification.Builder(this);
        mBuilderCompat = new NotificationCompat.Builder(this);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        IntentFilter filter = new IntentFilter(ACTION_CALL_STATUS_UPDATE);
        filter.addAction(ACTION_CHANGE_CALL_ON_HOLD);
        registerReceiver(chatCallUpdateReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            currentChatId = extras.getLong(CHAT_ID, MEGACHAT_INVALID_HANDLE);
            logDebug("Chat handle to call: " + currentChatId);
        }

        if (MegaApplication.getOpenCallChatId() != currentChatId) {
            MegaApplication.setOpenCallChatId(currentChatId);
        }
        showCallInProgressNotification();
        return START_NOT_STICKY;
    }

    private void checkAnotherActiveCall(){
        long activeCall = isAnotherActiveCall(currentChatId);
        if(currentChatId == activeCall){
            updateNotificationContent();
        }else{
            updateCall(activeCall);
        }
    }

    private void updateNotificationContent() {
        logDebug("Updating notification");
        Notification notif;
        int notificationId = getCurrentCallNotifId();

        MegaChatCall call = megaChatApi.getChatCall(currentChatId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (call == null) {
                mBuilderCompatO.setContentText(getString(R.string.action_notification_call_in_progress));
            } else {

                if (call.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                    mBuilderCompatO.setContentText(getString(R.string.outgoing_call_starting));
                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    mBuilderCompatO.setContentText(getString(R.string.title_notification_incoming_call));
                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING || call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    mBuilderCompatO.setContentText(getString(call.isOnHold() ? R.string.call_on_hold : R.string.title_notification_call_in_progress));
                }
            }
            notif = mBuilderCompatO.build();
        } else {
            if (call == null) {
                mBuilderCompat.setContentText(getString(R.string.action_notification_call_in_progress));
            } else {
                if (call.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                    mBuilderCompat.setContentText(getString(R.string.outgoing_call_starting));
                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    mBuilderCompat.setContentText(getString(R.string.title_notification_incoming_call));
                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING || call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    mBuilderCompat.setContentText(getString(call.isOnHold() ? R.string.call_on_hold : R.string.title_notification_call_in_progress));
                }
            }
            notif = mBuilderCompat.build();
        }
        startForeground(notificationId, notif);
    }

    private void showCallInProgressNotification() {
        logDebug("Showing the notification");
        int notificationId = getCurrentCallNotifId();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(channel);

            PendingIntent intentCall = getPendingIntentCall(this, currentChatId, notificationId+1);

            mBuilderCompatO = new NotificationCompat.Builder(this, notificationChannelId);
            mBuilderCompatO.setSmallIcon(R.drawable.ic_stat_notify);
            mBuilderCompatO.addAction(R.drawable.ic_phone_white, getString(R.string.button_notification_call_in_progress), intentCall);
            mBuilderCompatO.setContentIntent(intentCall);
            mBuilderCompatO.setAutoCancel(false);
            mBuilderCompatO.setOngoing(false);
            mBuilderCompatO.setColor(ContextCompat.getColor(this, R.color.mega));

            String title;
            String email;
            long userHandle;
            MegaChatRoom chat = megaChatApi.getChatRoom(currentChatId);
            if (chat != null) {
                title = getTitleChat(chat);

                if (chat.isGroup()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Bitmap largeIcon = createDefaultAvatar(-1, title);
                        if (largeIcon != null) {
                            mBuilderCompatO.setLargeIcon(largeIcon);
                        }
                    }
                } else {
                    userHandle = chat.getPeerHandle(0);
                    email = chat.getPeerEmail(0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Bitmap largeIcon = setProfileContactAvatar(userHandle, title, email);
                        if (largeIcon != null) {
                            mBuilderCompatO.setLargeIcon(largeIcon);
                        }
                    }
                }

                mBuilderCompatO.setContentTitle(title);
                updateNotificationContent();

            } else {
                mBuilderCompatO.setContentTitle(getString(R.string.title_notification_call_in_progress));
                mBuilderCompatO.setContentText(getString(R.string.action_notification_call_in_progress));
                startForeground(notificationId, mBuilderCompatO.build());
            }

        } else {

            mBuilderCompat = new NotificationCompat.Builder(this);
            mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

            PendingIntent intentCall = getPendingIntentCall(this, currentChatId, notificationId+1);

            mBuilderCompat = new NotificationCompat.Builder(this, notificationChannelId);
            mBuilderCompat.setSmallIcon(R.drawable.ic_stat_notify);
            mBuilderCompat.addAction(R.drawable.ic_phone_white, getString(R.string.button_notification_call_in_progress), intentCall);
            mBuilderCompat.setContentIntent(intentCall);
            mBuilderCompat.setAutoCancel(false);
            mBuilderCompat.setOngoing(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilderCompat.setColor(ContextCompat.getColor(this, R.color.mega));
            }

            String title;
            String email;
            long userHandle;
            MegaChatRoom chat = megaChatApi.getChatRoom(currentChatId);
            if (chat != null) {
                title = getTitleChat(chat);

                if (chat.isGroup()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Bitmap largeIcon = createDefaultAvatar(-1, title);
                        if (largeIcon != null) {
                            mBuilderCompat.setLargeIcon(largeIcon);
                        }
                    }
                } else {
                    userHandle = chat.getPeerHandle(0);
                    email = chat.getPeerEmail(0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Bitmap largeIcon = setProfileContactAvatar(userHandle, title, email);
                        if (largeIcon != null) {
                            mBuilderCompat.setLargeIcon(largeIcon);
                        }
                    }
                }

                mBuilderCompat.setContentTitle(title);
                updateNotificationContent();

            } else {
                mBuilderCompat.setContentTitle(getString(R.string.title_notification_call_in_progress));
                mBuilderCompat.setContentText(getString(R.string.action_notification_call_in_progress));
                startForeground(notificationId, mBuilderCompat.build());
            }
        }
    }

    private void updateCall(long newChatIdCall) {
        stopForeground(true);
        mNotificationManager.cancel(getCurrentCallNotifId());
        currentChatId = newChatIdCall;
        if (MegaApplication.getOpenCallChatId() != currentChatId) {
            MegaApplication.setOpenCallChatId(currentChatId);
        }
        showCallInProgressNotification();
    }

    private void removeNotification() {
        ArrayList<Long> listCalls = getCallsParticipating();
        if(listCalls == null || listCalls.size() == 0){
            MegaHandleList listCallsRingIn = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RING_IN);
            if (listCallsRingIn == null || listCallsRingIn.size() == 0) {
                stopForeground(true);
                mNotificationManager.cancel(getCurrentCallNotifId());
                stopSelf();
                return;
            }

            updateCall(listCallsRingIn.get(listCallsRingIn.size() - 1));
            return;
        }

        for(long chatCall:listCalls){
            if(chatCall != currentChatId){
                updateCall(chatCall);
                return;
            }
        }
        stopForeground(true);
        mNotificationManager.cancel(getCurrentCallNotifId());
        stopSelf();
    }

    public Bitmap setProfileContactAvatar(long userHandle, String fullName, String email) {
        Bitmap bitmap = null;
        File avatar = buildAvatarFile(getApplicationContext(), email + ".jpg");

        if (isFileAvailable(avatar)) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                bitmap = getCircleBitmap(bitmap);
                if (bitmap != null) {
                    return bitmap;
                } else {
                    return createDefaultAvatar(userHandle, fullName);
                }
            } else {
                return createDefaultAvatar(userHandle, fullName);
            }
        } else {
            return createDefaultAvatar(userHandle, fullName);
        }
    }

    private Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

    private Bitmap createDefaultAvatar(long userHandle, String fullName) {
        int color;
        if (userHandle != -1) {
            color = getColorAvatar(userHandle);
        } else {
            color = getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR);
        }

        return getDefaultAvatar(color, fullName, AVATAR_SIZE, true);
    }

    private int getCurrentCallNotifId(){
        return (MegaApiJava.userHandleToBase64(currentChatId)).hashCode();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(chatCallUpdateReceiver);
        mNotificationManager.cancel(getCurrentCallNotifId());
        MegaApplication.setOpenCallChatId(-1);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
