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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NonContactInfo;
import mega.privacy.android.app.utils.Constants;
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

public final class AdvancedNotificationBuilder {

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

    public static AdvancedNotificationBuilder newInstance(Context context, MegaApiAndroid megaApi, MegaChatApiAndroid megaChatApi) {
        Context appContext = context.getApplicationContext();
        Context safeContext = ContextCompat.createDeviceProtectedStorageContext(appContext);
        if (safeContext == null) {
            safeContext = appContext;
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(safeContext);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(safeContext);
        return new AdvancedNotificationBuilder(safeContext, notificationManager, sharedPreferences, megaApi, megaChatApi);
    }

    public AdvancedNotificationBuilder(Context context, NotificationManagerCompat notificationManager, SharedPreferences sharedPreferences, MegaApiAndroid megaApi, MegaChatApiAndroid megaChatApi) {
        this.context = context.getApplicationContext();
        this.notificationManager = notificationManager;
        this.sharedPreferences = sharedPreferences;
        dbH = DatabaseHandler.getDbHandler(context);
        this.megaApi = megaApi;
        this.megaChatApi = megaChatApi;
    }

    public void updateNotification(long chatId, MegaChatMessage msg){
        log("updateNotification");
        MegaChatRoom chat = megaChatApi.getChatRoom(chatId);

        String notificationExists = getNotificationIdByChatHandle(chat.getChatId(), msg.getMsgId());
        if(notificationExists!=null){
            if(notificationExists.equals("-1")){
                log("No notif exists to update");
            }
            else{
                int notificationId = (notificationExists).hashCode();
                if(msg.isDeleted()){
                    if(chat.getUnreadCount()!=0){
                        MegaChatListItem item = megaChatApi.getChatListItem(chatId);
                        if(item.getLastMessageType()!=MegaChatMessage.TYPE_INVALID){
                            sendBundledNotification(null, null, chatId, item.getLastMessageId(), item.getLastMessage(), item.getLastMessageSender());
                        }
                        else{
                            sendBundledNotification(null, null, chatId, 999, context.getString(R.string.history_cleared_message), -1);
                        }
                    }
                    else{
                        notificationManager.cancel(notificationId);
                        removeNotification(chatId);
                        if(!isAnyNotificationShown()){
                            notificationManager.cancel(SUMMARY_ID);
                        }
                    }
                }
                else if(msg.isEdited()){
                    sendBundledNotification(null, null, chatId, msg.getMsgId(), msg.getContent(), msg.getUserHandle());
                }

            }
        }
        else{
            log("NULL - No notif exists to update");
        }
    }

    public void removeSeenNotification(long chatId, MegaChatMessage msg){
        log("removeNotification");
        MegaChatRoom chat = megaChatApi.getChatRoom(chatId);

        String notificationExists = getNotificationIdByChatHandleAndMessageId(chat.getChatId(), msg.getMsgId());
        if(notificationExists!=null){
            if(notificationExists.equals("-1")){
                log("No notif exists to update");
            }
            else{
                int notificationId = (notificationExists).hashCode();
                notificationManager.cancel(notificationId);
                removeNotification(chatId);
                if(!isAnyNotificationShown()){
                    notificationManager.cancel(SUMMARY_ID);
                }
            }
        }
        else{
            log("NULL-No notif exists to update");

            notificationExists = getNotificationIdByChatHandleAndMessageId(chat.getChatId(), 999);
            if(notificationExists!=null){
                if(notificationExists.equals("-1")){
                    log("No notif exists to update");
                }
                else{
                    int notificationId = (notificationExists).hashCode();
                    notificationManager.cancel(notificationId);
                    removeNotification(chatId);
                    if(!isAnyNotificationShown()){
                        notificationManager.cancel(SUMMARY_ID);
                    }
                }
            }
        }
    }

    public void sendBundledNotification(Uri uriParameter, String vibration, long chatId, MegaChatMessage msg) {
        log("(1) sendBundledNotification");
        String messageContent = "";

        if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

            MegaNodeList nodeList = msg.getMegaNodeList();
            if(nodeList != null) {
                if (nodeList.size() == 1) {
                    MegaNode node = nodeList.get(0);
                    log("Node Name: " + node.getName());
                    messageContent = node.getName();
                }
            }
        }
        else if(msg.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){

            long userCount  = msg.getUsersCount();

            if(userCount==1) {
                String name = "";
                name = msg.getUserName(0);
                if (name.trim().isEmpty()) {
                    name = msg.getUserEmail(0);
                }
                String email = msg.getUserEmail(0);
                log("Contact EMail: " + name);
                messageContent = email;
            }

        }
        else if(msg.getType()==MegaChatMessage.TYPE_TRUNCATE){
            log("Type TRUNCATE message");
            messageContent = context.getString(R.string.history_cleared_message);
        }
        else{
            messageContent = msg.getContent();
        }

        sendBundledNotification(uriParameter, vibration, chatId, msg.getMsgId(), messageContent, msg.getUserHandle());
    }

    public void sendBundledNotification(Uri uriParameter, String vibration, long chatId, long msgId, String msgContent, long msgUserHandle) {
        log("sendBundledNotification: "+msgUserHandle+"_"+msgContent+"_"+msgId);
        MegaChatRoom chat = megaChatApi.getChatRoom(chatId);

        log("SDK android version: "+Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            log("sendBundledNotification: POST Android N");

            String manufacturer = "xiaomi";
            if(!manufacturer.equalsIgnoreCase(Build.MANUFACTURER)) {

                Notification notification = buildNotification(uriParameter, vibration, GROUP_KEY, chat, msgContent, msgUserHandle);

                int notificationId = (setNotificationId(chat.getChatId(), msgId)).hashCode();
                notificationManager.notify(notificationId, notification);
                Notification summary = buildSummary(GROUP_KEY);
                notificationManager.notify(SUMMARY_ID, summary);
            }
            else{
                log("sendBundledNotification: XIAOMI POST Android N");
                Notification notification = buildNotificationPreN(uriParameter, vibration);

                if(notification!=null){
                    notificationManager.notify(Constants.NOTIFICATION_PRE_N_CHAT, notification);
                }
                else{
                    notificationManager.cancel(Constants.NOTIFICATION_PRE_N_CHAT);
                }
            }
        }
        else {
            log("sendBundledNotification: PRE Android N");
            Notification notification = buildNotificationPreN(uriParameter, vibration);

            if(notification!=null){
                notificationManager.notify(Constants.NOTIFICATION_PRE_N_CHAT, notification);
            }
            else{
                notificationManager.cancel(Constants.NOTIFICATION_PRE_N_CHAT);
            }
        }

    }

    public Notification buildNotificationPreN(Uri uriParameter, String vibration){
        log("buildNotificationPreN");
        ArrayList<MegaChatListItem> unreadChats = megaChatApi.getUnreadChatListItems();
        Collections.sort(unreadChats, new Comparator<MegaChatListItem>() {
            public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                long timestamp1 = c1.getLastTimestamp();
                long timestamp2 = c2.getLastTimestamp();

                long result = timestamp2 - timestamp1;
                return (int) result;
            }
        });

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

            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            notificationBuilder.setColor(ContextCompat.getColor(context,R.color.mega))
                    .setShowWhen(true)
                    .setSound(defaultSoundUri);

            Spanned firstLine = null;

            SpannableStringBuilder notificationContent = new SpannableStringBuilder();
            for(int i =0; i<unreadChats.size();i++){

                if(i!=0){
                    notificationContent.append("\n");
                }

                MegaChatListItem itemToAdd = unreadChats.get(i);

                long lastMsgSender = itemToAdd.getLastMessageSender();
                String nameAction = getParticipantShortName(lastMsgSender);
                String lineToShow;
                if(itemToAdd.getUnreadCount()<=1){
                    if(itemToAdd.getLastMessageType()!=MegaChatMessage.TYPE_INVALID){

                        if(nameAction.isEmpty()){
                            lineToShow = "<b>"+itemToAdd.getTitle()+": </b>"+itemToAdd.getLastMessage();
                        }
                        else{
                            lineToShow = "<b>"+itemToAdd.getTitle()+": </b>"+nameAction+": "+itemToAdd.getLastMessage();
                        }

                    }
                    else{
                        if(nameAction.isEmpty()){
                            lineToShow = "<b>"+itemToAdd.getTitle()+": </b>"+context.getString(R.string.history_cleared_message);
                        }
                        else{
                            lineToShow = "<b>"+itemToAdd.getTitle()+": </b>"+nameAction+": "+context.getString(R.string.history_cleared_message);
                        }

                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        notificationContent.append(Html.fromHtml(lineToShow,Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        notificationContent.append(Html.fromHtml(lineToShow));
                    }

                    if(i==0){
                        firstLine = notificationContent;
                    }
                }
                else{

                    if(nameAction.isEmpty()){
                        lineToShow = "<b>"+itemToAdd.getTitle()+": </b>"+itemToAdd.getLastMessage();
                    }
                    else{
                        lineToShow = "<b>"+itemToAdd.getTitle()+": </b>"+nameAction+": "+itemToAdd.getLastMessage();
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        notificationContent.append(Html.fromHtml(lineToShow,Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        notificationContent.append(Html.fromHtml(lineToShow));
                    }

                    if(i==0){
                        firstLine = notificationContent;
                    }
                }

            }

            notificationBuilder.setSound(uriParameter);
            if(vibration!=null){
                if(vibration.equals("true")){
                    notificationBuilder.setVibrate(new long[] {0, 1000});
                }
            }
            String textToShow = String.format(context.getString(R.string.number_messages_chat_notification), unreadChats.size());
            bigTextStyle.setBigContentTitle(textToShow);
            bigTextStyle.bigText(notificationContent);

            notificationBuilder.setContentTitle(textToShow);
            notificationBuilder.setContentText(firstLine);

            //Moves the expanded layout object into the notification object.
            notificationBuilder.setStyle(bigTextStyle);

            return notificationBuilder.build();
        }
        else if (unreadChats.size()==1){
            return buildSingleNotificationPreN(uriParameter, unreadChats.get(0), vibration);
        }
        else{
            return null;
        }
    }

    public Notification buildNotification(Uri uriParameter, String vibration, String groupKey, MegaChatRoom chat, String msgContent, long msgUserHandle) {
        log("buildNotification");
        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
        intent.putExtra("CHAT_ID", chat.getChatId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)chat.getChatId() , intent, PendingIntent.FLAG_ONE_SHOT);

        String title;
        int unreadMessages = chat.getUnreadCount();
        log("Unread messages: "+unreadMessages);
        if(unreadMessages!=0){

            if(unreadMessages<0){
                unreadMessages = Math.abs(unreadMessages);
                log("unread number: "+unreadMessages);

                if(unreadMessages>1){
                    String numberString = "+"+unreadMessages;
                    title = chat.getTitle() + " (" + numberString + " " + context.getString(R.string.messages_chat_notification) + ")";
                }
                else{
                    title = chat.getTitle();
                }
            }
            else{

                if(unreadMessages>1){
                    String numberString = unreadMessages+"";
                    title = chat.getTitle() + " (" + numberString + " " + context.getString(R.string.messages_chat_notification) + ")";
                }
                else{
                    title = chat.getTitle();
                }
            }
        }
        else{
            title = chat.getTitle();
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Spanned notificationContent;

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notify_download)
                .setContentTitle(title)
                .setColor(ContextCompat.getColor(context,R.color.mega))
                .setAutoCancel(true)
                .setShowWhen(true)
                .setGroup(groupKey)
//                .setSortKey(String.valueOf(System.currentTimeMillis()))
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();

        if(chat.isGroup()){

            if(msgUserHandle!=-1){
                String nameAction = getParticipantShortName(msgUserHandle);

                if(nameAction.isEmpty()){
                    notificationBuilder.setContentText(msgContent);
                    bigTextStyle.bigText(msgContent);
                }
                else{
                    String source = "<b>"+nameAction+": </b>"+msgContent;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        notificationContent = Html.fromHtml(source,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        notificationContent = Html.fromHtml(source);
                    }
                    notificationBuilder.setContentText(notificationContent);
                    bigTextStyle.bigText(notificationContent);
                }
            }
            else{
                notificationBuilder.setContentText(msgContent);
                bigTextStyle.bigText(msgContent);
            }

        }
        else{
            notificationBuilder.setContentText(msgContent);
            bigTextStyle.bigText(msgContent);
        }

        Bitmap largeIcon = setUserAvatar(chat);
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
        notificationBuilder.setStyle(bigTextStyle);

        return notificationBuilder.build();

    }

    public Notification buildSingleNotificationPreN(Uri uriParameter, MegaChatListItem item, String vibration) {
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

            MegaChatRoom chatRoom = megaChatApi.getChatRoom(item.getChatId());
            Bitmap largeIcon = setUserAvatar(chatRoom);
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

    public Bitmap setUserAvatar(MegaChatRoom chat){
        log("setUserAvatar");

        if(chat.isGroup()){
            return createDefaultAvatar(chat);
        }
        else{

            String contactMail = chat.getPeerEmail(0);

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
                        return createDefaultAvatar(chat);
                    }
                    else{
                        return Util.getCircleBitmap(bitmap);
                    }
                }
                else{
                    return createDefaultAvatar(chat);
                }
            }
            else{
                return createDefaultAvatar(chat);
            }
        }
    }

    public Bitmap createDefaultAvatar(MegaChatRoom chat){
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

        if(chat.isGroup()){
            paintCircle.setColor(ContextCompat.getColor(context,R.color.divider_upgrade_account));
        }
        else{
            String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(chat.getPeerHandle(0)));
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

        if(chat.getTitle()!=null){
            if(!chat.getTitle().isEmpty()){
                char title = chat.getTitle().charAt(0);
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

    public String getNotificationIdByChatHandle(long chatHandle, long messageId) {
        String chatString = MegaApiJava.userHandleToBase64(chatHandle);

        String id = sharedPreferences.getString(chatString, "-1");
        return id;
    }

    public String getNotificationIdByChatHandleAndMessageId(long chatHandle, long messageId) {
        String chatString = MegaApiJava.userHandleToBase64(chatHandle);
        String messageString = MegaApiJava.userHandleToBase64(messageId);

        String id = sharedPreferences.getString(chatString, "-1");
        if(id.equals(messageString))
            return id;
        else
            return null;

    }

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

    public void removeAllChatNotifications(){
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            notificationManager.cancel(entry.getKey().hashCode());
        }
        notificationManager.cancel(SUMMARY_ID);
        sharedPreferences.edit().clear().commit();
        notificationManager.cancel(Constants.NOTIFICATION_GENERAL_PUSH_CHAT);
        notificationManager.cancel(Constants.NOTIFICATION_PRE_N_CHAT);
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
        Util.log("AdvancedNotificationBuilder", message);
    }

}
