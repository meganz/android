package mega.privacy.android.app.lollipop.megachat.calls;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatCallListenerInterface;
import nz.mega.sdk.MegaChatRoom;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Util.context;

public class CallService extends Service implements MegaChatCallListenerInterface {

    MegaApplication app;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    long chatId;

    private Notification.Builder mBuilder;
    private NotificationCompat.Builder mBuilderCompat;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilderCompatO;

    private String notificationChannelId = Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID;
    private String notificationChannelName = Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME;

    public void onCreate() {
        super.onCreate();
        LogUtil.logDebug("onCreate");

        app = (MegaApplication) getApplication();
        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();
        megaChatApi.addChatCallListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            mBuilder = new Notification.Builder(this);
        mBuilderCompat = new NotificationCompat.Builder(this);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.logDebug("onStartCommand");

        if (intent == null) {
            stopSelf();
        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            chatId = extras.getLong(Constants.CHAT_ID, -1);
            LogUtil.logDebug("Chat handle to call: " + chatId);
        }

        if (MegaApplication.getOpenCallChatId() != chatId) {
            MegaApplication.setOpenCallChatId(chatId);
        }
        showCallInProgressNotification();
        return START_NOT_STICKY;
    }

    @Override
    public void onChatCallUpdate(MegaChatApiJava api, MegaChatCall call) {
        if (call.getChatid() == chatId) {
            if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
                int callStatus = call.getStatus();
                LogUtil.logDebug("STATUS: " + callStatus);

                switch (callStatus) {
                    case MegaChatCall.CALL_STATUS_REQUEST_SENT:
                    case MegaChatCall.CALL_STATUS_RING_IN:
                    case MegaChatCall.CALL_STATUS_JOINING:
                    case MegaChatCall.CALL_STATUS_IN_PROGRESS: {
                        updateNotificationContent(call);
                        break;
                    }
                    case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
                    case MegaChatCall.CALL_STATUS_DESTROYED: {
                        stopForeground(true);
                        mNotificationManager.cancel(Constants.NOTIFICATION_CALL_IN_PROGRESS);
                        stopSelf();
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    }

    private void updateNotificationContent(MegaChatCall call) {
        LogUtil.logDebug("updateNotification");
        Notification notif;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (call == null) {
                mBuilderCompatO.setContentText(getString(R.string.action_notification_call_in_progress));
            } else {
                if (call.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                    mBuilderCompatO.setContentText(getString(R.string.outgoing_call_starting));
                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    mBuilderCompatO.setContentText(getString(R.string.title_notification_incoming_call));
                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING || call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    mBuilderCompatO.setContentText(getString(R.string.title_notification_call_in_progress));
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
                    mBuilderCompat.setContentText(getString(R.string.title_notification_call_in_progress));
                }
            }
            notif = mBuilderCompat.build();
        }
        startForeground(Constants.NOTIFICATION_CALL_IN_PROGRESS, notif);
    }

    private void showCallInProgressNotification() {
        LogUtil.logDebug("showCallInProgressNotification");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(channel);
            Intent intent = new Intent(this, ChatCallActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(Constants.CHAT_ID, chatId);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

            mBuilderCompatO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .addAction(R.drawable.ic_phone_white, getString(R.string.button_notification_call_in_progress), pendingIntent)
                    .setOngoing(false)
                    .setColor(ContextCompat.getColor(this, R.color.mega));

            String title;
            String email;
            long userHandle;
            MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
            if (chat != null) {
                title = chat.getTitle();

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
                MegaChatCall call = megaChatApi.getChatCall(chatId);
                updateNotificationContent(call);

            } else {
                mBuilderCompatO.setContentTitle(getString(R.string.title_notification_call_in_progress));
                mBuilderCompatO.setContentText(getString(R.string.action_notification_call_in_progress));
                Notification notif = mBuilderCompatO.build();
                startForeground(Constants.NOTIFICATION_CALL_IN_PROGRESS, notif);
            }

        } else {

            mBuilderCompat = new NotificationCompat.Builder(this);
            mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent intent = new Intent(this, ChatCallActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(Constants.CHAT_ID, chatId);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilderCompat
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .addAction(R.drawable.ic_phone_white, getString(R.string.button_notification_call_in_progress), pendingIntent)
                    .setOngoing(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilderCompat.setColor(ContextCompat.getColor(this, R.color.mega));
            }

            String title;
            String email;
            long userHandle;
            MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
            if (chat != null) {
                title = chat.getTitle();

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
                MegaChatCall call = megaChatApi.getChatCall(chatId);
                updateNotificationContent(call);

            } else {
                mBuilderCompat.setContentTitle(getString(R.string.title_notification_call_in_progress));
                mBuilderCompat.setContentText(getString(R.string.action_notification_call_in_progress));
                Notification notif = mBuilderCompat.build();
                startForeground(Constants.NOTIFICATION_CALL_IN_PROGRESS, notif);
            }
        }
    }

    public Bitmap setProfileContactAvatar(long userHandle, String fullName, String email) {
        Bitmap bitmap = null;
        File avatar = buildAvatarFile(context, email + ".jpg");

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

    public Bitmap createDefaultAvatar(long userHandle, String fullName) {
        LogUtil.logDebug("createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint paintText = new Paint();
        Paint paintCircle = new Paint();

        if (userHandle != -1) {
            String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(userHandle));
            if (color != null) {
                LogUtil.logDebug("The color to set the avatar is " + color);
                paintCircle.setColor(Color.parseColor(color));
            } else {
                LogUtil.logDebug("Default color to the avatar");
                paintCircle.setColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
            }
        } else {
            paintCircle.setColor(ContextCompat.getColor(context, R.color.divider_upgrade_account));
        }

        paintText.setColor(Color.WHITE);
        paintText.setTextSize(150);
        paintCircle.setAntiAlias(true);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);
        Typeface face = Typeface.SANS_SERIF;
        paintText.setTypeface(face);
        paintText.setAntiAlias(true);
        paintText.setSubpixelText(true);
        paintText.setStyle(Paint.Style.FILL);

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth() / 2;
        else
            radius = defaultAvatar.getHeight() / 2;

        c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, paintCircle);

        if (fullName != null) {
            if (!fullName.isEmpty()) {
                char title = fullName.charAt(0);
                String firstLetter = new String(title + "");

                if (!firstLetter.equals("(")) {

                    LogUtil.logDebug("Draw letter: " + firstLetter);
                    Rect bounds = new Rect();

                    paintText.getTextBounds(firstLetter, 0, firstLetter.length(), bounds);
                    int xPos = (c.getWidth() / 2);
                    int yPos = (int) ((c.getHeight() / 2) - ((paintText.descent() + paintText.ascent() / 2)) + 20);
                    c.drawText(firstLetter.toUpperCase(Locale.getDefault()), xPos, yPos, paintText);
                }

            }
        }
        return defaultAvatar;
    }

    @Override
    public void onDestroy() {
        LogUtil.logDebug("onDestroy");

        if (megaChatApi != null) {
            megaChatApi.removeChatCallListener(this);
        }

        mNotificationManager.cancel(Constants.NOTIFICATION_CALL_IN_PROGRESS);

        MegaApplication.setOpenCallChatId(-1);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
