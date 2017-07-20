package mega.privacy.android.app.lollipop.megachat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;

import java.io.File;
import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatListItem;

public final class NotificationBuilder {

    private static final String GROUP_KEY = "Messenger";
    private static final String NOTIFICATION_ID = "com.stylingandroid.nougat.NOTIFICATION_ID";
    private static final int SUMMARY_ID = 0;

    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final SharedPreferences sharedPreferences;

    public static NotificationBuilder newInstance(Context context) {
        Context appContext = context.getApplicationContext();
        Context safeContext = ContextCompat.createDeviceProtectedStorageContext(appContext);
        if (safeContext == null) {
            safeContext = appContext;
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(safeContext);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(safeContext);
        return new NotificationBuilder(safeContext, notificationManager, sharedPreferences);
    }

    public NotificationBuilder(Context context,
                                NotificationManagerCompat notificationManager,
                                SharedPreferences sharedPreferences) {
        this.context = context.getApplicationContext();
        this.notificationManager = notificationManager;
        this.sharedPreferences = sharedPreferences;
    }


    public void sendBundledNotification(Uri uriParameter, MegaChatListItem item, String vibration, String email, String color) {
        Notification notification = buildNotification(uriParameter, item, vibration, GROUP_KEY, email, color);
        log("Notification id: "+getNotificationIdByHandle(item.getChatId()));
        notificationManager.notify(getNotificationIdByHandle(item.getChatId()), notification);
        Notification summary = buildSummary(GROUP_KEY);
        notificationManager.notify(SUMMARY_ID, summary);
    }

    public Notification buildNotification(Uri uriParameter, MegaChatListItem item, String vibration, String groupKey, String email, String color) {
        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notify_download)
                .setContentTitle(item.getTitle())
                .setContentText(item.getLastMessage())
                .setColor(ContextCompat.getColor(context,R.color.mega))
                .setAutoCancel(true)
                .setShowWhen(true)
                .setGroup(groupKey)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        //		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        //		StringBuilder[] events = {notificationContent, new StringBuilder("y trooo"), new StringBuilder("y moreee"), new StringBuilder("y yaaaa")};
        // Sets a title for the Inbox in expanded layout
        //		inboxStyle.setBigContentTitle("New messages:");

        //		String[] events = {"y trooo", "y moreee", "y yaaaa"};
        //// Moves events into the expanded layout
        //		inboxStyle.addLine(notificationContent);
        //		for (int i=0; i < events.length; i++) {
        //			inboxStyle.addLine(events[i]);
        //
        //		}
        // Moves the expanded layout object into the notification object.
        //		notificationBuilder.setStyle(inboxStyle);

        Bitmap largeIcon = setUserAvatar(item, email, color);
        if(largeIcon!=null){
            log("There is avatar!");
            notificationBuilder.setLargeIcon(largeIcon);
        }

        notificationBuilder.setSound(uriParameter);
        if(vibration!=null){
            if(vibration.equals("true")){
                notificationBuilder.setVibrate(new long[] {0, 1000});
            }
        }

        return notificationBuilder.build();
    }

    public Bitmap setUserAvatar(MegaChatListItem item, String contactMail, String color){
        log("setUserAvatar");

        if(item.isGroup()){
            return createDefaultAvatar(item, color);
        }
        else{
            File avatar = null;
            if (context.getExternalCacheDir() != null){
                avatar = new File(context.getExternalCacheDir().getAbsolutePath(), contactMail + ".jpg");
            }
            else{
                avatar = new File(context.getCacheDir().getAbsolutePath(), contactMail + ".jpg");
            }
            Bitmap bitmap = null;
            if (avatar.exists()){
                if (avatar.length() > 0){
                    BitmapFactory.Options bOpts = new BitmapFactory.Options();
                    bOpts.inPurgeable = true;
                    bOpts.inInputShareable = true;
                    bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                    if (bitmap == null) {
                        return createDefaultAvatar(item, color);
                    }
                    else{
                        return getCircleBitmap(bitmap);
                    }
                }
                else{
                    return createDefaultAvatar(item, color);
                }
            }
            else{
                return createDefaultAvatar(item, color);
            }
        }
    }

    private Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
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

    public Bitmap createDefaultAvatar(MegaChatListItem item, String color){
        log("createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        if(item.isGroup()){
            p.setColor(ContextCompat.getColor(context,R.color.divider_upgrade_account));
        }
        else{
            if(color!=null){
                log("The color to set the avatar is "+color);
                p.setColor(Color.parseColor(color));
            }
            else{
                log("Default color to the avatar");
                p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
            }
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);

        if(item.getTitle()!=null){
            if(!item.getTitle().isEmpty()){
                char title = item.getTitle().charAt(0);
                String firstLetter = new String(title+"");

                if(!firstLetter.equals("(")){

                    log("Draw letter: "+firstLetter);
                    Paint text = new Paint();
                    Typeface face = Typeface.SANS_SERIF;
                    text.setTypeface(face);
                    text.setAntiAlias(true);
                    text.setSubpixelText(true);
                    text.setStyle(Paint.Style.FILL);
                    text.setColor(Color.WHITE);
                    text.setTextSize(150);
                    text.setTextAlign(Paint.Align.CENTER);

                    Rect bounds = new Rect();
                    text.getTextBounds(firstLetter, 0, firstLetter.length(), bounds);
                    int x = (defaultAvatar.getWidth() - bounds.width())/2;
                    int y = (defaultAvatar.getHeight() + bounds.height())/2;
                    c.drawText(firstLetter.toUpperCase(Locale.getDefault()), x, y, text);
                }

            }
        }
        return defaultAvatar;
    }

    public Notification buildSummary(String groupKey) {
        return new NotificationCompat.Builder(context)
                .setContentTitle("Nougat Messenger")
                .setContentText("You have unread messages")
                .setSmallIcon(R.drawable.ic_stat_notify_download)
                .setShowWhen(true)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .build();
    }

//    public int getNotificationId() {
//        int id = sharedPreferences.getInt(NOTIFICATION_ID, SUMMARY_ID) + 1;
//        while (id == SUMMARY_ID) {
//            id++;
//        }
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putInt(NOTIFICATION_ID, id);
//        editor.apply();
//        return id;
//    }

    public int getNotificationIdByHandle(long chatHandle) {
        String handleString = MegaApiJava.handleToBase64(chatHandle);

        int id = sharedPreferences.getInt(handleString, -1);
        if (id == -1) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(handleString, (int)chatHandle);
            editor.apply();
            return (int)chatHandle;
        }
        else{
            return id;
        }
    }

    public static void log(String message) {
        Util.log("NotificationBuilder", message);
    }

}