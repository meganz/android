package mega.privacy.android.app.fcm;

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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

public final class ContactsAdvancedNotificationBuilder implements MegaRequestListenerInterface {

    private static final String GROUP_KEY_IPC = "IPCNotificationBuilder";

    private static final String GROUP_KEY_APC = "APCNotificationBuilder";

    private final Context context;
    private NotificationManager notificationManager;
    private final SharedPreferences sharedPreferences;
    DatabaseHandler dbH;
    MegaApiAndroid megaApi;
    private int counter = 0;
    private String email = "";
    private String firstName = "";
    private String lastName = "";

    private NotificationCompat.Builder mBuilderCompat;

    public static ContactsAdvancedNotificationBuilder newInstance(Context context, MegaApiAndroid megaApi) {
        Context appContext = context.getApplicationContext();
        Context safeContext = ContextCompat.createDeviceProtectedStorageContext(appContext);
        if (safeContext == null) {
            safeContext = appContext;
        }
        NotificationManager notificationManager = (NotificationManager) safeContext.getSystemService(Context.NOTIFICATION_SERVICE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(safeContext);
        return new ContactsAdvancedNotificationBuilder(safeContext, notificationManager, sharedPreferences, megaApi);
    }

    public ContactsAdvancedNotificationBuilder(Context context, NotificationManager notificationManager, SharedPreferences sharedPreferences, MegaApiAndroid megaApi) {
        this.context = context.getApplicationContext();
        this.notificationManager = notificationManager;
        this.sharedPreferences = sharedPreferences;
        dbH = DatabaseHandler.getDbHandler(context);
        this.megaApi = megaApi;
    }

    public void showIncomingContactRequestNotification(){
        log("showIncomingContactRequestNotification");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            String manufacturer = "xiaomi";
            if(!manufacturer.equalsIgnoreCase(Build.MANUFACTURER)) {
                log("generateChatNotification:POST Android N");
                newIncomingContactRequest();
            }
            else{
                log("generateChatNotification:XIAOMI POST Android N");
//                generateChatNotificationPreN(request);
            }
        }
        else {
            log("generateChatNotification:PRE Android N");
//            generateChatNotificationPreN(request);
        }
    }

    public void showAcceptanceContactRequestNotification(String email){
        log("showAcceptanceContactRequestNotification");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            String manufacturer = "xiaomi";
            if(!manufacturer.equalsIgnoreCase(Build.MANUFACTURER)) {
                log("generateChatNotification:POST Android N");
                newAcceptanceContactRequest(email);
            }
            else{
                log("generateChatNotification:XIAOMI POST Android N");
//                generateChatNotificationPreN(request);
            }
        }
        else {
            log("generateChatNotification:PRE Android N");
//            generateChatNotificationPreN(request);
        }
    }

    public void newIncomingContactRequest(){
        log("newIncomingContactRequest");

        ArrayList<MegaContactRequest> contacts = megaApi.getIncomingContactRequests();
        if(contacts!=null)
        {
            log("Number of requests: "+contacts.size());
            for(int i=0;i<contacts.size();i++)
            {
                log("-----------------REQUEST: "+i);
                MegaContactRequest contactRequest = contacts.get(i);
                log("user sent: "+contactRequest.getSourceEmail());
                if(i==0){
                    sendBundledNotificationIPC(contactRequest, true);
                }
                else{
                    sendBundledNotificationIPC(contactRequest, false);
                }
            }
        }
        else{
            log("Number of requests: NULL");
        }
    }

    public void newAcceptanceContactRequest(String email){
        log("newAcceptanceContactRequest");
        this.email = email;
        counter=0;
        megaApi.getUserAttribute(email, MegaApiJava.USER_ATTR_FIRSTNAME, this);
        megaApi.getUserAttribute(email, MegaApiJava.USER_ATTR_LASTNAME, this);
        megaApi.getUserAvatar(email, context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg", this);
    }

    public void sendBundledNotificationIPC(MegaContactRequest crToShow, boolean beep) {
        log("sendBundledNotificationIPC");

        Notification notification = buildIPCNotification(crToShow, beep);

        String handleString = MegaApiJava.userHandleToBase64(crToShow.getHandle());
        int notificationId = handleString.hashCode();

        notificationManager.notify(notificationId, notification);
        Notification summary = buildSummaryIPC(GROUP_KEY_IPC);
        notificationManager.notify(Constants.NOTIFICATION_SUMMARY_INCOMING_CONTACT, summary);
    }

    public void sendBundledNotificationAPC() {
        log("sendBundledNotificationAPC");

        Notification notification = buildAPCNotification();

        int notificationId = email.hashCode();

        notificationManager.notify(notificationId, notification);
        Notification summary = buildSummaryAPC(GROUP_KEY_APC);
        notificationManager.notify(Constants.NOTIFICATION_SUMMARY_ACCEPTANCE_CONTACT, summary);
    }

    public void buildNotificationPreN(Uri uriParameter, String vibration, MegaChatRequest request){
        log("buildNotificationPreN");

        MegaHandleList chatHandleList = request.getMegaHandleList();

        ArrayList<MegaChatListItem> chats = new ArrayList<>();
        for(int i=0; i<chatHandleList.size(); i++){
//            MegaChatListItem chat = megaChatApi.getChatListItem(chatHandleList.get(i));
//            if(chat!=null){
//                chats.add(chat);
//            }
//            else{
//                log("ERROR:chatNotRecovered:NULL");
//            }
        }

        PendingIntent pendingIntent = null;

        if(chats.size()>1){
            Intent intent = new Intent(context, ManagerActivityLollipop.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(Constants.ACTION_CHAT_SUMMARY);
            intent.putExtra("CHAT_ID", -1);
            pendingIntent = PendingIntent.getActivity(context, (int)chats.get(0).getChatId() , intent, PendingIntent.FLAG_ONE_SHOT);

            //Order by last interaction
            Collections.sort(chats, new Comparator<MegaChatListItem> (){

                public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                    long timestamp1 = c1.getLastTimestamp();
                    long timestamp2 = c2.getLastTimestamp();

                    long result = timestamp2 - timestamp1;
                    return (int)result;
                }
            });
        }
        else if (chats.size()==1){
            Intent intent = new Intent(context, ManagerActivityLollipop.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
            intent.putExtra("CHAT_ID", chats.get(0).getChatId());
            pendingIntent = PendingIntent.getActivity(context, (int)chats.get(0).getChatId() , intent, PendingIntent.FLAG_ONE_SHOT);
        }
        else {
            log("ERROR:chatSIZE=0:return");
            return;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notify_download)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        notificationBuilder.setColor(ContextCompat.getColor(context,R.color.mega))
                .setShowWhen(true);

        if(uriParameter!=null){
            notificationBuilder.setSound(uriParameter);
        }

        if(vibration!=null){
            if(vibration.equals("true")){
                notificationBuilder.setVibrate(new long[] {0, 500});
            }
        }

        notificationBuilder.setStyle(inboxStyle);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1){
            //API 25 = Android 7.1
            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
        }
        else{
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }

//        notificationBuilder.setFullScreenIntent(pendingIntent, true);

        int unreadCount = 0;

        for(int i=0; i<chats.size(); i++){
            if(unreadCount<8){

            }
            else{
                break;
            }
        }

        String textToShow = context.getResources().getQuantityString(R.plurals.plural_number_messages_chat_notification, (int)chats.size(), chats.size());

        notificationBuilder.setContentTitle("MEGA");
        notificationBuilder.setContentText(textToShow);
        inboxStyle.setSummaryText(textToShow);

        Notification notif = notificationBuilder.build();


    }

    public Notification buildIPCNotification(MegaContactRequest crToShow, boolean beep) {
        log("buildIPCNotification");

        String notificationContent;
        if(crToShow!=null){
            notificationContent = crToShow.getSourceEmail();
        }
        else{
            log("Return because the request is NULL");
            return null;
        }

        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Constants.ACTION_IPC);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)crToShow.getHandle() , intent, PendingIntent.FLAG_ONE_SHOT);

        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notify_download)
                .setContentTitle(context.getString(R.string.title_contact_request_notification))
                .setContentText(notificationContent)
                .setStyle(new Notification.BigTextStyle().bigText(notificationContent))
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context,R.color.mega))
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY_IPC);

        if(beep){
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notificationBuilder.setSound(defaultSoundUri);
            notificationBuilder.setVibrate(new long[] {0, 500});
        }

        Bitmap largeIcon = createDefaultAvatar(crToShow.getSourceEmail());
        if(largeIcon!=null){
            notificationBuilder.setLargeIcon(largeIcon);
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1){
            //API 25 = Android 7.1
            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
        }
        else{
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }

        return notificationBuilder.build();
    }

    public Notification buildAPCNotification() {
        log("buildAPCNotification");

        String title = context.getString(R.string.title_acceptance_contact_request_notification);
        String fullName = "";

        if (firstName.trim().length() <= 0){
            fullName = lastName;
        }
        else{
            fullName = firstName + " " + lastName;
        }

        if (fullName.trim().length() > 0){
            title = title + ": "+fullName;
        }

        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Constants.ACTION_IPC);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)email.hashCode() , intent, PendingIntent.FLAG_ONE_SHOT);

        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notify_download)
                .setContentTitle(title)
                .setContentText(email)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context,R.color.mega))
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY_APC);


        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSound(defaultSoundUri);
        notificationBuilder.setVibrate(new long[] {0, 500});

        Bitmap largeIcon = setUserAvatar(email);
        if(largeIcon!=null){
            notificationBuilder.setLargeIcon(largeIcon);
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1){
            //API 25 = Android 7.1
            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
        }
        else{
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }

        return notificationBuilder.build();
    }

    public Bitmap createDefaultAvatar(String email){
        log("createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint paintText = new Paint();
        Paint paintCircle = new Paint();

        MegaUser contact = megaApi.getContact(email);
        if(contact!=null){
            String color = megaApi.getUserAvatarColor(contact);
            if(color!=null){
                log("The color to set the avatar is "+color);
                paintCircle.setColor(Color.parseColor(color));
            }
            else{
                log("Default color to the avatar");
                paintCircle.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
            }
        }
        else{
            paintCircle.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
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
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, paintCircle);

        if(email!=null){
            if(!email.isEmpty()){
                char title = email.charAt(0);
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

    public Bitmap setUserAvatar(String contactMail){
        log("setUserAvatar");

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
                    return createDefaultAvatar(contactMail);
                }
                else{
                    return Util.getCircleBitmap(bitmap);
                }
            }
            else{
                return createDefaultAvatar(contactMail);
            }
        }
        else{
            return createDefaultAvatar(contactMail);
        }
    }

    public Notification buildSummaryIPC(String groupKey) {
        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Constants.ACTION_IPC);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1 , intent, PendingIntent.FLAG_ONE_SHOT);

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

    public Notification buildSummaryAPC(String groupKey) {
        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Constants.ACTION_IPC);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 2 , intent, PendingIntent.FLAG_ONE_SHOT);

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
//        int id = sharedPreferences.getInt(NOTIFICATION_ID, SUMMARY_ID_IPC) + 1;
//        while (id == SUMMARY_ID_IPC) {
//            id++;
//        }
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putInt(NOTIFICATION_ID, id);
//        editor.apply();
//        return id;
//    }

    public String getNotificationIdByChatHandle(long chatHandle, long messageId) {
        String chatString = MegaApiJava.userHandleToBase64(chatHandle);

        String id = sharedPreferences.getString(chatString, "-1");
        if(id!=null && (!id.equals("-1")))
            return chatString;
        else
            return null;
    }

//    public String getNotificationIdByChatHandleAndMessageId(long chatHandle, long messageId) {
//        String chatString = MegaApiJava.userHandleToBase64(chatHandle);
//        String messageString = MegaApiJava.userHandleToBase64(messageId);
//
//        String id = sharedPreferences.getString(chatString, "-1");
//        if(id.equals(messageString))
//            return chatString;
//        else
//            return null;
//
//    }

    public String setNotificationId(long chatHandle, long messageId) {
        String chatString = MegaApiJava.userHandleToBase64(chatHandle);
        String messageString = MegaApiJava.userHandleToBase64(messageId);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(chatString, messageString);
        editor.apply();
        return chatString;
    }

    public void removeNotification(long chatHandle){
        String chatString = MegaApiJava.userHandleToBase64(chatHandle);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(chatString);
        editor.apply();
    }

    public boolean isAnyNotificationShown(){
        if((sharedPreferences.getAll()).isEmpty())
            return false;
        return true;
    }

    public void removeAllIncomingContactNotifications(){
        notificationManager.cancel(Constants.NOTIFICATION_SUMMARY_INCOMING_CONTACT);
    }

    public void removeAllAcceptanceContactNotifications(){
        notificationManager.cancel(Constants.NOTIFICATION_SUMMARY_ACCEPTANCE_CONTACT);
    }

    public void showSimpleNotification(){
        log("showSimpleNotification");

        mBuilderCompat = new NotificationCompat.Builder(context);

        if(notificationManager == null){
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

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

        notificationManager.notify(Constants.NOTIFICATION_GENERAL_PUSH_CHAT, mBuilderCompat.build());
    }

    public void showChatNotificationPreN(MegaChatRequest request, boolean beep, long lastChatId){
        log("showChatNotification");

        if(beep){
            ChatSettings chatSettings = dbH.getChatSettings();

            if (chatSettings != null) {

                if (chatSettings.getNotificationsEnabled()==null){
                    log("getNotificationsEnabled NULL --> Notifications ON");
                    checkNotificationsSoundPreN(request, beep, lastChatId);
                }
                else{
                    if (chatSettings.getNotificationsEnabled().equals("true")) {
                        log("Notifications ON for all chats");
                        checkNotificationsSoundPreN(request, beep, lastChatId);
                    } else {
                        log("Notifications OFF");
                    }
                }

            } else {
                log("Notifications DEFAULT ON");

                Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                buildNotificationPreN(defaultSoundUri2, "true", request);
            }
        }
        else{
            buildNotificationPreN(null, "false", request);
        }
    }

    public void checkNotificationsSoundPreN(MegaChatRequest request, boolean beep, long lastChatId) {
        log("checkNotificationsSound: " + beep);

        ChatSettings chatSettings = dbH.getChatSettings();
        ChatItemPreferences chatItemPreferences = dbH.findChatPreferencesByHandle(String.valueOf(lastChatId));

        if (chatItemPreferences == null) {
            log("No preferences for this item");

            if (chatSettings.getNotificationsSound() == null){
                log("Notification sound is NULL");
                Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                buildNotificationPreN(defaultSoundUri, chatSettings.getVibrationEnabled(), request);
            }
            else if(chatSettings.getNotificationsSound().equals("-1")){
                log("Silent notification Notification sound -1");
                buildNotificationPreN(null, chatSettings.getVibrationEnabled(), request);
            }
            else{
                String soundString = chatSettings.getNotificationsSound();
                Uri uri = Uri.parse(soundString);
                log("Uri: " + uri);

                if (soundString.equals("true") || soundString.equals("")) {

                    Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                    buildNotificationPreN(defaultSoundUri, chatSettings.getVibrationEnabled(), request);
                } else if (soundString.equals("-1")) {
                    log("Silent notification");
                    buildNotificationPreN(null, chatSettings.getVibrationEnabled(), request);
                } else {
                    Ringtone sound = RingtoneManager.getRingtone(context, uri);
                    if (sound == null) {
                        log("Sound is null");
                        buildNotificationPreN(null, chatSettings.getVibrationEnabled(), request);
                    } else {
                        buildNotificationPreN(uri, chatSettings.getVibrationEnabled(), request);
                    }
                }
            }

        } else {
            log("Preferences FOUND for this item");
            if (chatItemPreferences.getNotificationsEnabled() == null || chatItemPreferences.getNotificationsEnabled().isEmpty() || chatItemPreferences.getNotificationsEnabled().equals("true")) {
                log("Notifications ON for this chat");
                String soundString = chatItemPreferences.getNotificationsSound();

                if (soundString.equals("true")||soundString.isEmpty()) {
                    Uri defaultSoundUri2 = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    buildNotificationPreN(defaultSoundUri2, chatSettings.getVibrationEnabled(), request);
                } else if (soundString.equals("-1")) {
                    log("Silent notification");
                    buildNotificationPreN(null, chatSettings.getVibrationEnabled(), request);
                } else {
                    Uri uri = Uri.parse(soundString);
                    log("Uri: " + uri);
                    Ringtone sound = RingtoneManager.getRingtone(context, uri);
                    if (sound == null) {
                        log("Sound is null");
                        buildNotificationPreN(null, chatSettings.getVibrationEnabled(), request);
                    } else {
                        buildNotificationPreN(uri, chatSettings.getVibrationEnabled(), request);

                    }
                }
            } else {
                log("Notifications OFF for this chats");
            }
        }
    }

    public static void log(String message) {
        Util.log("ChatAdvancedNotificationBuilder", message);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish");

        counter++;
        if (e.getErrorCode() == MegaError.API_OK){

            if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME){
                firstName = request.getText();
            }
            else if(request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
                lastName = request.getText();
            }
        }
        if(counter==3){
            sendBundledNotificationAPC();
            counter = 0;
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
