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
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaUser;

public final class NotificationBuilder {

    private static final String GROUP_KEY = "Karere";
    private static final int SUMMARY_ID = 0;

    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final SharedPreferences sharedPreferences;
    DatabaseHandler dbH;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    private NotificationCompat.Builder mBuilderCompat;
    private Notification.Builder mBuilder;
    private NotificationManager mNotificationManager;

    public static NotificationBuilder newInstance(Context context, MegaApiAndroid megaApi, MegaChatApiAndroid megaChatApi) {
        Context appContext = context.getApplicationContext();
        Context safeContext = ContextCompat.createDeviceProtectedStorageContext(appContext);
        if (safeContext == null) {
            safeContext = appContext;
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(safeContext);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(safeContext);
        return new NotificationBuilder(safeContext, notificationManager, sharedPreferences, megaApi, megaChatApi);
    }

    public NotificationBuilder(Context context, NotificationManagerCompat notificationManager, SharedPreferences sharedPreferences, MegaApiAndroid megaApi, MegaChatApiAndroid megaChatApi) {
        this.context = context.getApplicationContext();
        this.notificationManager = notificationManager;
        this.sharedPreferences = sharedPreferences;
        dbH = DatabaseHandler.getDbHandler(context);
        this.megaApi = megaApi;
        this.megaChatApi = megaChatApi;
    }

    public void sendBundledNotification(Uri uriParameter, ArrayList<MegaChatListItem> unreadChats, String vibration, String email) {

        MegaChatListItem item = unreadChats.get(0);
        log("Last item: "+unreadChats.get(0));

        log("SDK android version: "+Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            log("more than Build.VERSION_CODES.N");

            String manufacturer = "xiaomi";
            if(!manufacturer.equalsIgnoreCase(Build.MANUFACTURER)) {
                Notification notification = buildNotification(uriParameter, item, vibration, GROUP_KEY, email);
                log("Notification id--- "+getNotificationIdByHandle(item.getChatId()));
                notificationManager.notify(getNotificationIdByHandle(item.getChatId()), notification);
                Notification summary = buildSummary(GROUP_KEY);
                notificationManager.notify(SUMMARY_ID, summary);
            }
            else{
                Notification notification = buildNotificationPreN(uriParameter, unreadChats, vibration, GROUP_KEY, email);
                log("Notification XIAOMI id: "+getNotificationIdByHandle(item.getChatId()));
                if(notification!=null){
                    notificationManager.notify(getNotificationIdByHandle(item.getChatId()), notification);
                }
            }
        }
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            log("more than Build.VERSION_CODES.LOLLIPOP");
            Notification notification = buildNotificationPreN(uriParameter, unreadChats, vibration, GROUP_KEY, email);
            log("Notification id: "+getNotificationIdByHandle(item.getChatId()));
            if(notification!=null){
                notificationManager.notify(Constants.NOTIFICATION_PRE_N_CHAT, notification);
            }
        }
        else{
            log("Android 4 - no bundled - no inbox style");
            Notification notification = buildNotification(uriParameter, item, vibration, GROUP_KEY, email);
            log("Notification id: "+getNotificationIdByHandle(item.getChatId()));
            notificationManager.notify(Constants.NOTIFICATION_PRE_N_CHAT, notification);
        }
    }

    public Notification buildNotificationPreN(Uri uriParameter, ArrayList<MegaChatListItem> unreadChats, String vibration, String groupKey, String email){
        log("buildNotificationPreN");

        if(unreadChats.size()>1){
            Intent intent = new Intent(context, ManagerActivityLollipop.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(Constants.ACTION_CHAT_SUMMARY);
            intent.putExtra("CHAT_ID", -1);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)unreadChats.get(0).getChatId() , intent, PendingIntent.FLAG_ONE_SHOT);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify_download)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            notificationBuilder.setColor(ContextCompat.getColor(context,R.color.mega))
                    .setShowWhen(true)
                    .setGroup(groupKey)
                    .setSound(defaultSoundUri);

            Spanned firstLine = null;


            for(int i =0; i<unreadChats.size();i++){

                MegaChatListItem itemToAdd = unreadChats.get(i);

                long lastMsgSender = itemToAdd.getLastMessageSender();
                String nameAction = getParticipantShortName(lastMsgSender);

                String lineToShow;
                if(nameAction.isEmpty()){
                    lineToShow = "<b>"+itemToAdd.getTitle()+": </b>"+itemToAdd.getLastMessage();
                }
                else{
                    lineToShow = "<b>"+itemToAdd.getTitle()+": </b>"+nameAction+": "+itemToAdd.getLastMessage();
                }

                Spanned notificationContent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notificationContent = Html.fromHtml(lineToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    notificationContent = Html.fromHtml(lineToShow);
                }

                inboxStyle.addLine(notificationContent);

                if(i==0){
                    firstLine = notificationContent;
                }
            }

            notificationBuilder.setSound(uriParameter);
            if(vibration!=null){
                if(vibration.equals("true")){
                    notificationBuilder.setVibrate(new long[] {0, 1000});
                }
            }

            String textToShow = context.getResources().getQuantityString(R.plurals.plural_number_messages_chat_notification, (int)unreadChats.size(), unreadChats.size());
            inboxStyle.setBigContentTitle(textToShow);

            notificationBuilder.setContentTitle(textToShow);
            notificationBuilder.setContentText(firstLine);

            //Moves the expanded layout object into the notification object.
            notificationBuilder.setStyle(inboxStyle);

            return notificationBuilder.build();
        }
        else if (unreadChats.size()==1){
            return buildNotification(uriParameter, unreadChats.get(0), vibration, GROUP_KEY, email);
        }
        else{
            return null;
        }
    }

    public Notification buildNotification(Uri uriParameter, MegaChatListItem item, String vibration, String groupKey, String email) {
        log("buildNotification");
        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
        intent.putExtra("CHAT_ID", item.getChatId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)item.getChatId() , intent, PendingIntent.FLAG_ONE_SHOT);

        String title;
        int unreadMessages = item.getUnreadCount();
        log("Unread messages: "+unreadMessages);
        if(unreadMessages!=0){

            if(unreadMessages<0){
                unreadMessages = Math.abs(unreadMessages);
                log("unread number: "+unreadMessages);

                if(unreadMessages>1){
                    String numberString = "+"+unreadMessages;
                    title = item.getTitle() + " (" + numberString + " " + context.getString(R.string.messages_chat_notification) + ")";
                }
                else{
                    title = item.getTitle();
                }
            }
            else{

                if(unreadMessages>1){
                    String numberString = unreadMessages+"";
                    title = item.getTitle() + " (" + numberString + " " + context.getString(R.string.messages_chat_notification) + ")";
                }
                else{
                    title = item.getTitle();
                }
            }
        }
        else{
            title = item.getTitle();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            log("Notification pre lollipop");

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify_download)
                    .setContentTitle(title)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            if(item.isGroup()){

                long lastMsgSender = item.getLastMessageSender();
                String nameAction = getParticipantShortName(lastMsgSender);

                if(nameAction.isEmpty()){
                    notificationBuilder.setContentText(item.getLastMessage());
                }
                else{
                    String source = nameAction+": "+item.getLastMessage();
                    notificationBuilder.setContentText(source);
                }
            }
            else{
                notificationBuilder.setContentText(item.getLastMessage());
            }
            notificationBuilder.setSound(uriParameter);
            if(vibration!=null){
                if(vibration.equals("true")){
                    notificationBuilder.setVibrate(new long[] {0, 1000});
                }

            }
            return notificationBuilder.build();
        }
        else{

            log("Notification POST lollipop");

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            Spanned notificationContent;

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify_download)
                    .setContentTitle(title)
                    .setColor(ContextCompat.getColor(context,R.color.mega))
                    .setAutoCancel(true)
                    .setShowWhen(true)
                    .setGroup(groupKey)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);

            if(item.isGroup()){

                long lastMsgSender = item.getLastMessageSender();
                String nameAction = getParticipantShortName(lastMsgSender);

                if(nameAction.isEmpty()){
                    notificationBuilder.setContentText(item.getLastMessage());
                }
                else{
                    String source = "<b>"+nameAction+": </b>"+item.getLastMessage();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        notificationContent = Html.fromHtml(source,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        notificationContent = Html.fromHtml(source);
                    }
                    notificationBuilder.setContentText(notificationContent);
                }
            }
            else{
                notificationBuilder.setContentText(item.getLastMessage());
            }

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

            Bitmap largeIcon = setUserAvatar(item, email);
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
    }

    public String getParticipantShortName(long userHandle){
        log("getParticipantShortName");

        MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(userHandle));
        if (contactDB != null) {

            String participantFirstName = contactDB.getName();

            if(participantFirstName==null){
                participantFirstName="";
            }

            if (participantFirstName.trim().length() <= 0){
                String participantLastName = contactDB.getLastName();

                if(participantLastName == null){
                    participantLastName="";
                }

                if (participantLastName.trim().length() <= 0){
                    String stringHandle = MegaApiJava.handleToBase64(userHandle);
                    MegaUser megaContact = megaApi.getContact(stringHandle);
                    if(megaContact!=null){
                        return megaContact.getEmail();
                    }
                    else{
                        return "Unknown name";
                    }
                }
                else{
                    return participantLastName;
                }
            }
            else{
                return participantFirstName;
            }
        } else {
            log("Find non contact!");

            NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle+"");

            if(nonContact!=null){
                String nonContactFirstName = nonContact.getFirstName();

                if(nonContactFirstName==null){
                    nonContactFirstName="";
                }

                if (nonContactFirstName.trim().length() <= 0){
                    String nonContactLastName = nonContact.getLastName();

                    if(nonContactLastName == null){
                        nonContactLastName="";
                    }

                    if (nonContactLastName.trim().length() <= 0){
                        log("Ask for email of a non contact");
                    }
                    else{
                        return nonContactLastName;
                    }
                }
                else{
                    return nonContactFirstName;
                }
            }
            else{
                log("Ask for non contact info");
            }

            return "";
        }
    }

    public Bitmap setUserAvatar(MegaChatListItem item, String contactMail){
        log("setUserAvatar");

        if(item.isGroup()){
            return createDefaultAvatar(item);
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
                        return createDefaultAvatar(item);
                    }
                    else{
                        return Util.getCircleBitmap(bitmap);
                    }
                }
                else{
                    return createDefaultAvatar(item);
                }
            }
            else{
                return createDefaultAvatar(item);
            }
        }
    }

    public Bitmap createDefaultAvatar(MegaChatListItem item){
        log("createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint paintText = new Paint();
        Paint paintCircle = new Paint();

        paintText.setColor(Color.WHITE);
        paintText.setTextSize(150);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);
        Typeface face = Typeface.SANS_SERIF;
        paintText.setTypeface(face);
        paintText.setAntiAlias(true);
        paintText.setSubpixelText(true);
        paintText.setStyle(Paint.Style.FILL);

        if(item.isGroup()){
            paintCircle.setColor(ContextCompat.getColor(context,R.color.divider_upgrade_account));
        }
        else{
            String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(item.getPeerHandle()));
            if(color!=null){
                log("The color to set the avatar is "+color);
                paintCircle.setColor(Color.parseColor(color));
                paintCircle.setAntiAlias(true);
            }
            else{
                log("Default color to the avatar");
                paintCircle.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
                paintCircle.setAntiAlias(true);
            }
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius,paintCircle);

        if(item.getTitle()!=null){
            if(!item.getTitle().isEmpty()){
                char title = item.getTitle().charAt(0);
                String firstLetter = new String(title+"");

                if(!firstLetter.equals("(")){

                    log("Draw letter: "+firstLetter);
                    Rect bounds = new Rect();

                    paintText.getTextBounds(firstLetter,0,firstLetter.length(),bounds);
                    int xPos = (c.getWidth()/2);
                    int yPos = (int)((c.getHeight()/2)-((paintText.descent()+paintText.ascent()/2))+20);
                    c.drawText(firstLetter.toUpperCase(Locale.getDefault()), xPos, yPos, paintText);
                }

            }
        }
        return defaultAvatar;
    }

    public Notification buildSummary(String groupKey) {
        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Constants.ACTION_CHAT_SUMMARY);
        intent.putExtra("CHAT_ID", -1);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 , intent, PendingIntent.FLAG_ONE_SHOT);

        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notify_download)
                .setShowWhen(true)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setColor(ContextCompat.getColor(context,R.color.mega))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
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

    public void showSimpleNotification(){
        log("showSimpleNotification");

        mBuilderCompat = new NotificationCompat.Builder(context);

        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Constants.ACTION_CHAT_SUMMARY);
        intent.putExtra("CHAT_ID", -1);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 , intent, PendingIntent.FLAG_ONE_SHOT);

        mBuilderCompat
                .setSmallIcon(R.drawable.ic_stat_notify_download)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true).setTicker("Chat activity")
                .setColor(ContextCompat.getColor(context,R.color.mega))
                .setContentTitle("Chat activity").setContentText("You may have new messages")
                .setOngoing(false);

        mNotificationManager.notify(Constants.NOTIFICATION_GENERAL_PUSH_CHAT, mBuilderCompat.build());
    }

    public static void log(String message) {
        Util.log("NotificationBuilder", message);
    }

}
