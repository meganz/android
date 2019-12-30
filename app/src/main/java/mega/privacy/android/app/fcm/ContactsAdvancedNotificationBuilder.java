package mega.privacy.android.app.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

public final class ContactsAdvancedNotificationBuilder implements MegaRequestListenerInterface {

    private static final String GROUP_KEY_IPC = "IPCNotificationBuilder";
    private static final String GROUP_KEY_APC = "APCNotificationBuilder";

    private final Context context;
    private NotificationManager notificationManager;
    DatabaseHandler dbH;
    MegaApiAndroid megaApi;
    private int counter = 0;
    private String email = "";
    private String firstName = "";
    private String lastName = "";

    private NotificationCompat.Builder mBuilderCompat;

    private String notificationChannelIdSimple = NOTIFICATION_CHANNEL_CONTACTS_ID;
    private String notificationChannelNameSimple = NOTIFICATION_CHANNEL_CONTACTS_NAME;
    private String notificationChannelIdSummary = NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_ID;
    private String notificationChannelNameSummary = NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_NAME;

    public static ContactsAdvancedNotificationBuilder newInstance(Context context, MegaApiAndroid megaApi) {
        Context appContext = context.getApplicationContext();
        Context safeContext = ContextCompat.createDeviceProtectedStorageContext(appContext);
        if (safeContext == null) {
            safeContext = appContext;
        }
        NotificationManager notificationManager = (NotificationManager) safeContext.getSystemService(Context.NOTIFICATION_SERVICE);

        return new ContactsAdvancedNotificationBuilder(safeContext, notificationManager, megaApi);
    }

    public ContactsAdvancedNotificationBuilder(Context context, NotificationManager notificationManager, MegaApiAndroid megaApi) {
        this.context = context.getApplicationContext();
        this.notificationManager = notificationManager;
        dbH = DatabaseHandler.getDbHandler(context);
        this.megaApi = megaApi;
    }

    public void showIncomingContactRequestNotification(){
        logDebug("showIncomingContactRequestNotification");

        ArrayList<MegaContactRequest> icr = megaApi.getIncomingContactRequests();
        if(icr==null){
            logWarning("Number of requests: NULL");
            return;
        }

        ArrayList<MegaContactRequest> finalIcr = new ArrayList<MegaContactRequest>();
        Date currentDate = new Date(System.currentTimeMillis());

        //Just show cr of the last 14 days
        for(int i=0; i< icr.size();i++){
            MegaContactRequest cr = icr.get(i);

            long ts = cr.getModificationTime()*1000;
            Date crDate = new Date(ts);
            long diff = currentDate.getTime() - crDate.getTime();
//            float days = (diff / (1000*60*60*24));
            long diffDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
            if(diffDays<14){
                finalIcr.add(cr);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            String manufacturer = "xiaomi";
            if(!manufacturer.equalsIgnoreCase(Build.MANUFACTURER)) {
                logDebug("POST Android N");
                newIncomingContactRequest(finalIcr);
            }
            else{
                logDebug("XIAOMI POST Android N");
                generateIncomingNotificationPreN(finalIcr);
            }
        }
        else {
            logDebug("PRE Android N");
            generateIncomingNotificationPreN(finalIcr);
        }
    }

    public void showAcceptanceContactRequestNotification(String email){
        logDebug("showAcceptanceContactRequestNotification");

        this.email = email;
        counter=0;
        megaApi.getUserAttribute(email, MegaApiJava.USER_ATTR_FIRSTNAME, this);
        megaApi.getUserAttribute(email, MegaApiJava.USER_ATTR_LASTNAME, this);
        megaApi.getUserAvatar(email, buildAvatarFile(context, email + ".jpg").getAbsolutePath(), this);

    }

    public void newIncomingContactRequest(ArrayList<MegaContactRequest> contacts){
        logDebug("Number of incoming contact request: " + contacts.size());

        for(int i=contacts.size()-1;i>=0;i--)
        {
            logDebug("REQUEST: " + i);
            MegaContactRequest contactRequest = contacts.get(i);
            logDebug("User sent: " + contactRequest.getSourceEmail());
            if(i==0){
                sendBundledNotificationIPC(contactRequest, true);
            }
            else{
                sendBundledNotificationIPC(contactRequest, false);
            }
        }
    }

    public void newAcceptanceContactRequest(){
        logDebug("newAcceptanceContactRequest");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            String manufacturer = "xiaomi";
            if(!manufacturer.equalsIgnoreCase(Build.MANUFACTURER)) {
                logDebug("POST Android N");
                sendBundledNotificationAPC();
            }
            else{
                logDebug("XIAOMI POST Android N");
                showSimpleNotificationAPC();
            }
        }
        else {
            logDebug("PRE Android N");
            showSimpleNotificationAPC();
        }
    }

    public void sendBundledNotificationIPC(MegaContactRequest crToShow, boolean beep) {
        logDebug("sendBundledNotificationIPC");

        Notification notification = buildIPCNotification(crToShow, beep);

        String handleString = MegaApiJava.userHandleToBase64(crToShow.getHandle());
        int notificationId = handleString.hashCode();

        notificationManager.notify(notificationId, notification);
        Notification summary = buildSummaryIPC(GROUP_KEY_IPC);
        notificationManager.notify(NOTIFICATION_SUMMARY_INCOMING_CONTACT, summary);
    }

    public void generateIncomingNotificationPreN(ArrayList<MegaContactRequest> icr){
        logDebug("generateIncomingNotificationPreN");

        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_IPC);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        notificationBuilder.setShowWhen(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            notificationBuilder.setColor(ContextCompat.getColor(context,R.color.mega));
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSound(defaultSoundUri);
        notificationBuilder.setVibrate(new long[] {0, 500});

        notificationBuilder.setStyle(inboxStyle);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1){
            //API 25 = Android 7.1
            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
        }
        else{
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }

        for(int i=0;i<icr.size();i++)
        {
            MegaContactRequest contactRequest = icr.get(i);
            logDebug("User sent: " + contactRequest.getSourceEmail());
            inboxStyle.addLine(contactRequest.getSourceEmail());
        }

        if(icr.size()==1){
            Bitmap largeIcon = createDefaultAvatar(icr.get(0).getSourceEmail());
            if(largeIcon!=null){
                notificationBuilder.setLargeIcon(largeIcon);
            }
        }
        else{

            String count = icr.size() + "";
            Bitmap largeIcon = createDefaultAvatar(count);
            if(largeIcon!=null){
                notificationBuilder.setLargeIcon(largeIcon);
            }
        }

        String textToShow = context.getResources().getQuantityString(R.plurals.plural_number_contact_request_notification, icr.size(), icr.size());

        notificationBuilder.setContentTitle(context.getResources().getString(R.string.title_new_contact_request_notification));
        notificationBuilder.setContentText(textToShow);
        inboxStyle.setSummaryText(textToShow);

        Notification notif = notificationBuilder.build();

        if(notif!=null){
            notificationManager.notify(NOTIFICATION_SUMMARY_INCOMING_CONTACT, notif);
        }
        else{
            notificationManager.cancel(NOTIFICATION_SUMMARY_INCOMING_CONTACT);
        }
    }

    public void sendBundledNotificationAPC() {
        logDebug("sendBundledNotificationAPC");

        Notification notification = buildAPCNotification();

        int notificationId = email.hashCode();

        notificationManager.notify(notificationId, notification);
        Notification summary = buildSummaryAPC(GROUP_KEY_APC);
        notificationManager.notify(NOTIFICATION_SUMMARY_ACCEPTANCE_CONTACT, summary);
    }

    public Notification buildIPCNotification(MegaContactRequest crToShow, boolean beep) {
        logDebug("buildIPCNotification");

        String notificationContent;
        if(crToShow!=null){
            notificationContent = crToShow.getSourceEmail();
        }
        else{
            logError("Return because the request is NULL");
            return null;
        }

        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_IPC);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)crToShow.getHandle() , intent, PendingIntent.FLAG_ONE_SHOT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(notificationChannelIdSimple, notificationChannelNameSimple, NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(true);
            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdSimple);
            notificationBuilderO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(context.getString(R.string.title_contact_request_notification))
                    .setContentText(notificationContent)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setGroup(GROUP_KEY_IPC)
                    .setColor(ContextCompat.getColor(context, R.color.mega));

            if (beep) {
                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                notificationBuilderO.setSound(defaultSoundUri);
                notificationBuilderO.setVibrate(new long[]{0, 500});
            }

            Bitmap largeIcon = createDefaultAvatar(crToShow.getSourceEmail());
            if (largeIcon != null) {
                notificationBuilderO.setLargeIcon(largeIcon);
            }

            notificationBuilderO.setPriority(NotificationManager.IMPORTANCE_HIGH);

            return notificationBuilderO.build();
        }
        else {

            Notification.Builder notificationBuilder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(context.getString(R.string.title_contact_request_notification))
                    .setContentText(notificationContent)
                    .setStyle(new Notification.BigTextStyle().bigText(notificationContent))
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setGroup(GROUP_KEY_IPC);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notificationBuilder.setColor(ContextCompat.getColor(context, R.color.mega));
            }

            if (beep) {
                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                notificationBuilder.setSound(defaultSoundUri);
                notificationBuilder.setVibrate(new long[]{0, 500});
            }

            Bitmap largeIcon = createDefaultAvatar(crToShow.getSourceEmail());
            if (largeIcon != null) {
                notificationBuilder.setLargeIcon(largeIcon);
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                //API 25 = Android 7.1
                notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
            } else {
                notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
            }

            return notificationBuilder.build();
        }
    }

    public Notification buildAPCNotification() {
        logDebug("buildAPCNotification");

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
        intent.setAction(ACTION_OPEN_CONTACTS_SECTION);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)email.hashCode() , intent, PendingIntent.FLAG_ONE_SHOT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(notificationChannelIdSimple, notificationChannelNameSimple, NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(true);
            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdSimple);
            notificationBuilderO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(title)
                    .setContentText(email)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setGroup(GROUP_KEY_APC)
                    .setColor(ContextCompat.getColor(context, R.color.mega));

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notificationBuilderO.setSound(defaultSoundUri);
            notificationBuilderO.setVibrate(new long[]{0, 500});

            Bitmap largeIcon = setUserAvatar(email);
            if (largeIcon != null) {
                notificationBuilderO.setLargeIcon(largeIcon);
            }

            notificationBuilderO.setPriority(NotificationManager.IMPORTANCE_HIGH);

            return notificationBuilderO.build();
        }
        else {

            Notification.Builder notificationBuilder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(title)
                    .setContentText(email)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setGroup(GROUP_KEY_APC);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notificationBuilder.setColor(ContextCompat.getColor(context, R.color.mega));
            }

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notificationBuilder.setSound(defaultSoundUri);
            notificationBuilder.setVibrate(new long[]{0, 500});

            Bitmap largeIcon = setUserAvatar(email);
            if (largeIcon != null) {
                notificationBuilder.setLargeIcon(largeIcon);
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                //API 25 = Android 7.1
                notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
            } else {
                notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
            }

            return notificationBuilder.build();
        }
    }

    private Bitmap createDefaultAvatar(String email){
        MegaUser contact = megaApi.getContact(email);
        int color = colorAvatar(context, megaApi, contact);
        return getDefaultAvatar(context, color, email, AVATAR_SIZE, true, false);
    }

    public Bitmap setUserAvatar(String contactMail){
        logDebug("setUserAvatar");

        File avatar = buildAvatarFile(context, contactMail + ".jpg");
        Bitmap bitmap = null;
        if (isFileAvailable(avatar)){
            if (avatar.length() > 0){
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    return createDefaultAvatar(contactMail);
                }
                else{
                    return getCircleBitmap(bitmap);
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
        intent.setAction(ACTION_IPC);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1 , intent, PendingIntent.FLAG_ONE_SHOT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(notificationChannelIdSummary, notificationChannelNameSummary, NotificationManager.IMPORTANCE_HIGH);
            channel.setShowBadge(true);
            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdSummary);
            notificationBuilderO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setShowWhen(true)
                    .setGroup(groupKey)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setColor(ContextCompat.getColor(context, R.color.mega));

            return notificationBuilderO.build();
        }
        else {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setColor(ContextCompat.getColor(context, R.color.mega));
            }
            builder.setSmallIcon(R.drawable.ic_stat_notify)
                    .setShowWhen(true)
                    .setGroup(groupKey)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setColor(ContextCompat.getColor(context, R.color.mega));
            }

            return builder.build();
        }
    }

    public Notification buildSummaryAPC(String groupKey) {
        Intent intent = new Intent(context, ManagerActivityLollipop.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(ACTION_OPEN_CONTACTS_SECTION);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 2 , intent, PendingIntent.FLAG_ONE_SHOT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelIdSummary, notificationChannelNameSummary, NotificationManager.IMPORTANCE_HIGH);
            channel.setShowBadge(true);
            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdSummary);
            notificationBuilderO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setShowWhen(true)
                    .setGroup(groupKey)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setColor(ContextCompat.getColor(context, R.color.mega));

            return notificationBuilderO.build();
        }
        else {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

            builder.setSmallIcon(R.drawable.ic_stat_notify)
                    .setShowWhen(true)
                    .setGroup(groupKey)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setColor(ContextCompat.getColor(context, R.color.mega));
            }

            return builder.build();
        }
    }

    public void removeAllIncomingContactNotifications(){
        notificationManager.cancel(NOTIFICATION_SUMMARY_INCOMING_CONTACT);
    }

    public void removeAllAcceptanceContactNotifications(){
        notificationManager.cancel(NOTIFICATION_SUMMARY_ACCEPTANCE_CONTACT);
    }

    public void showSimpleNotificationAPC(){
        logDebug("showSimpleNotificationAPC");
    
        Intent myService = new Intent(context, IncomingMessageService.class);
        context.stopService(myService);
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
        intent.setAction(ACTION_OPEN_CONTACTS_SECTION);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)email.hashCode() , intent, PendingIntent.FLAG_ONE_SHOT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelIdSimple, notificationChannelNameSimple, NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(true);
            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdSimple);
            notificationBuilderO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true).setTicker(title)
                    .setContentTitle(title).setContentText(email)
                    .setOngoing(false)
                    .setColor(ContextCompat.getColor(context, R.color.mega));

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notificationBuilderO.setSound(defaultSoundUri);
            notificationBuilderO.setVibrate(new long[]{0, 500});

            Bitmap largeIcon = setUserAvatar(email);
            if (largeIcon != null) {
                notificationBuilderO.setLargeIcon(largeIcon);
            }

            notificationBuilderO.setPriority(NotificationManager.IMPORTANCE_HIGH);

            notificationManager.notify(NOTIFICATION_GENERAL_PUSH_CHAT, notificationBuilderO.build());
        }
        else {

            mBuilderCompat = new NotificationCompat.Builder(context);

            if(notificationManager == null){
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }

            mBuilderCompat
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true).setTicker(title)
                    .setContentTitle(title).setContentText(email)
                    .setOngoing(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilderCompat.setColor(ContextCompat.getColor(context, R.color.mega));
            }

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mBuilderCompat.setSound(defaultSoundUri);
            mBuilderCompat.setVibrate(new long[]{0, 500});

            Bitmap largeIcon = setUserAvatar(email);
            if (largeIcon != null) {
                mBuilderCompat.setLargeIcon(largeIcon);
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                //API 25 = Android 7.1
                mBuilderCompat.setPriority(Notification.PRIORITY_HIGH);
            } else {
                mBuilderCompat.setPriority(NotificationManager.IMPORTANCE_HIGH);
            }

            notificationManager.notify(NOTIFICATION_GENERAL_PUSH_CHAT, mBuilderCompat.build());
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish");

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
            newAcceptanceContactRequest();
            counter = 0;
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
