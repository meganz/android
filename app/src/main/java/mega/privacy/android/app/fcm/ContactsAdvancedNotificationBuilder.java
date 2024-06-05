package mega.privacy.android.app.fcm;

import static mega.privacy.android.app.utils.AvatarUtil.getAvatarBitmap;
import static mega.privacy.android.app.utils.AvatarUtil.getColorAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.Constants.ACTION_IPC;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_CONTACTS_SECTION;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CONTACTS_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CONTACTS_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_GENERAL_PUSH_CHAT;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_SUMMARY_ACCEPTANCE_CONTACT;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_SUMMARY_INCOMING_CONTACT;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.domain.entity.contacts.ContactRequest;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;

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
        dbH = DbHandlerModuleKt.getDbHandler();
        this.megaApi = megaApi;
    }

    public void showIncomingContactRequestNotification(List<ContactRequest> contactRequests) {
        Timber.d("showIncomingContactRequestNotification");

        String manufacturer = "xiaomi";
        if (!manufacturer.equalsIgnoreCase(Build.MANUFACTURER)) {
            Timber.d("POST Android N");
            newIncomingContactRequest(contactRequests);
        } else {
            Timber.d("XIAOMI POST Android N");
            generateIncomingNotificationPreN(contactRequests);
        }
    }

    public void showAcceptanceContactRequestNotification(String email) {
        Timber.d("showAcceptanceContactRequestNotification");

        this.email = email;
        counter = 0;
        megaApi.getUserAttribute(email, MegaApiJava.USER_ATTR_FIRSTNAME, this);
        megaApi.getUserAttribute(email, MegaApiJava.USER_ATTR_LASTNAME, this);
        megaApi.getUserAvatar(email, buildAvatarFile(email + ".jpg").getAbsolutePath(), this);

    }

    public void newIncomingContactRequest(List<ContactRequest> contacts) {
        Timber.d("Number of incoming contact request: %s", contacts.size());

        for (int i = contacts.size() - 1; i >= 0; i--) {
            Timber.d("REQUEST: %s", i);
            ContactRequest contactRequest = contacts.get(i);
            Timber.d("User sent: %s", contactRequest.getSourceEmail());
            sendBundledNotificationIPC(contactRequest, i == 0);
        }
    }

    public void newAcceptanceContactRequest() {
        Timber.d("newAcceptanceContactRequest");

        String manufacturer = "xiaomi";
        if (!manufacturer.equalsIgnoreCase(Build.MANUFACTURER)) {
            Timber.d("POST Android N");
            sendBundledNotificationAPC();
        } else {
            Timber.d("XIAOMI POST Android N");
            showSimpleNotificationAPC();
        }
    }

    public void sendBundledNotificationIPC(ContactRequest crToShow, boolean beep) {
        Timber.d("sendBundledNotificationIPC");

        Notification notification = buildIPCNotification(crToShow, beep);

        String handleString = MegaApiJava.userHandleToBase64(crToShow.getHandle());
        int notificationId = handleString.hashCode();

        notificationManager.notify(notificationId, notification);
        Notification summary = buildSummaryIPC(GROUP_KEY_IPC);
        notificationManager.notify(NOTIFICATION_SUMMARY_INCOMING_CONTACT, summary);
    }

    public void generateIncomingNotificationPreN(List<ContactRequest> icr) {
        Timber.d("generateIncomingNotificationPreN");

        createNotificationChannel(
                notificationChannelIdSimple,
                notificationChannelNameSimple,
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, notificationChannelIdSimple)
                .setSmallIcon(mega.privacy.android.icon.pack.R.drawable.ic_stat_notify)
                .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
                .setContentIntent(getIPCPendingIntent(1))
                .setAutoCancel(true);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        notificationBuilder.setShowWhen(true);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSound(defaultSoundUri);
        notificationBuilder.setVibrate(new long[]{0, 500});

        notificationBuilder.setStyle(inboxStyle);

        notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);

        for (int i = 0; i < icr.size(); i++) {
            ContactRequest contactRequest = icr.get(i);
            Timber.d("User sent: %s", contactRequest.getSourceEmail());
            inboxStyle.addLine(contactRequest.getSourceEmail());
        }

        if (icr.size() == 1) {
            Bitmap largeIcon = createDefaultAvatar(icr.get(0).getSourceEmail());
            notificationBuilder.setLargeIcon(largeIcon);
        } else {

            String count = icr.size() + "";
            Bitmap largeIcon = createDefaultAvatar(count);
            notificationBuilder.setLargeIcon(largeIcon);
        }

        String textToShow = context.getResources().getQuantityString(R.plurals.plural_number_contact_request_notification, icr.size(), icr.size());

        notificationBuilder.setContentTitle(context.getResources().getString(R.string.title_new_contact_request_notification));
        notificationBuilder.setContentText(textToShow);
        inboxStyle.setSummaryText(textToShow);

        Notification notif = notificationBuilder.build();

        notificationManager.notify(NOTIFICATION_SUMMARY_INCOMING_CONTACT, notif);
    }

    public void sendBundledNotificationAPC() {
        Timber.d("sendBundledNotificationAPC");

        Notification notification = buildAPCNotification();

        int notificationId = email.hashCode();

        notificationManager.notify(notificationId, notification);
        Notification summary = buildSummaryAPC(GROUP_KEY_APC);
        notificationManager.notify(NOTIFICATION_SUMMARY_ACCEPTANCE_CONTACT, summary);
    }

    public Notification buildIPCNotification(ContactRequest crToShow, boolean beep) {
        Timber.d("buildIPCNotification");

        String notificationContent;
        if (crToShow != null) {
            notificationContent = crToShow.getSourceEmail();
        } else {
            Timber.e("Return because the request is NULL");
            return null;
        }

        createNotificationChannel(
                notificationChannelIdSimple,
                notificationChannelNameSimple,
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdSimple);
        notificationBuilderO
                .setSmallIcon(mega.privacy.android.icon.pack.R.drawable.ic_stat_notify)
                .setContentTitle(context.getString(R.string.title_contact_request_notification))
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .setAutoCancel(true)
                .setContentIntent(getIPCPendingIntent((int) crToShow.getHandle()))
                .setGroup(GROUP_KEY_IPC)
                .setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

        if (beep) {
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notificationBuilderO.setSound(defaultSoundUri);
            notificationBuilderO.setVibrate(new long[]{0, 500});
        }

        Bitmap largeIcon = createDefaultAvatar(crToShow.getSourceEmail());
        notificationBuilderO.setLargeIcon(largeIcon);

        notificationBuilderO.setPriority(NotificationManager.IMPORTANCE_HIGH);

        return notificationBuilderO.build();

    }

    public Notification buildAPCNotification() {
        Timber.d("buildAPCNotification");

        String title = context.getString(R.string.title_acceptance_contact_request_notification);
        String fullName = firstName + " " + lastName;

        if (firstName.trim().isEmpty()) {
            fullName = lastName;
        }

        if (!fullName.trim().isEmpty()) {
            title = title + ": " + fullName;
        }

        createNotificationChannel(
                notificationChannelIdSimple,
                notificationChannelNameSimple,
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdSimple);
        notificationBuilderO
                .setSmallIcon(mega.privacy.android.icon.pack.R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(email)
                .setAutoCancel(true)
                .setContentIntent(getAPCPendingIntent(email.hashCode()))
                .setGroup(GROUP_KEY_APC)
                .setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilderO.setSound(defaultSoundUri);
        notificationBuilderO.setVibrate(new long[]{0, 500});

        Bitmap largeIcon = setUserAvatar(email);
        notificationBuilderO.setLargeIcon(largeIcon);

        notificationBuilderO.setPriority(NotificationManager.IMPORTANCE_HIGH);

        return notificationBuilderO.build();

    }

    private Bitmap createDefaultAvatar(String email) {
        return getDefaultAvatar(getColorAvatar(megaApi.getContact(email)), email, AVATAR_SIZE, true, false);
    }

    public Bitmap setUserAvatar(String contactMail) {
        Timber.d("setUserAvatar");

        Bitmap bitmap = getAvatarBitmap(contactMail);
        if (bitmap == null) {
            return createDefaultAvatar(contactMail);
        }

        return bitmap;
    }

    public Notification buildSummaryIPC(String groupKey) {
        createNotificationChannel(
                notificationChannelIdSummary,
                notificationChannelNameSummary,
                NotificationManager.IMPORTANCE_HIGH
        );

        NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdSummary);
        notificationBuilderO
                .setSmallIcon(mega.privacy.android.icon.pack.R.drawable.ic_stat_notify)
                .setShowWhen(true)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(getIPCPendingIntent(1))
                .setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

        return notificationBuilderO.build();
    }

    private PendingIntent getIPCPendingIntent(int requestCode) {
        Intent intent = getBaseIntent();
        intent.setAction(ACTION_IPC);
        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public Notification buildSummaryAPC(String groupKey) {
        createNotificationChannel(
                notificationChannelIdSummary,
                notificationChannelNameSummary,
                NotificationManager.IMPORTANCE_HIGH
        );

        NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdSummary);
        notificationBuilderO
                .setSmallIcon(mega.privacy.android.icon.pack.R.drawable.ic_stat_notify)
                .setShowWhen(true)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(getAPCPendingIntent(2))
                .setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

        return notificationBuilderO.build();
    }

    private PendingIntent getAPCPendingIntent(int requestCode) {
        Intent intent = getBaseIntent();
        intent.setAction(ACTION_OPEN_CONTACTS_SECTION);
        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private Intent getBaseIntent() {
        Intent intent = new Intent(context, ManagerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    public void removeAllIncomingContactNotifications() {
        notificationManager.cancel(NOTIFICATION_SUMMARY_INCOMING_CONTACT);
    }

    public void showSimpleNotificationAPC() {
        String title = context.getString(R.string.title_acceptance_contact_request_notification);
        String fullName = firstName + " " + lastName;

        if (firstName.trim().isEmpty()) {
            fullName = lastName;
        }

        if (!fullName.trim().isEmpty()) {
            title = title + ": " + fullName;
        }

        createNotificationChannel(
                notificationChannelIdSimple,
                notificationChannelNameSimple,
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(context, notificationChannelIdSimple);
        notificationBuilderO
                .setSmallIcon(mega.privacy.android.icon.pack.R.drawable.ic_stat_notify)
                .setContentIntent(getAPCPendingIntent(email.hashCode()))
                .setAutoCancel(true).setTicker(title)
                .setContentTitle(title).setContentText(email)
                .setOngoing(false)
                .setColor(ContextCompat.getColor(context, R.color.red_600_red_300));

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilderO.setSound(defaultSoundUri);
        notificationBuilderO.setVibrate(new long[]{0, 500});

        Bitmap largeIcon = setUserAvatar(email);
        notificationBuilderO.setLargeIcon(largeIcon);

        notificationBuilderO.setPriority(NotificationManager.IMPORTANCE_HIGH);

        notificationManager.notify(NOTIFICATION_GENERAL_PUSH_CHAT, notificationBuilderO.build());
    }

    private void createNotificationChannel(String id, String name, int importance) {
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setShowBadge(true);
        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.d("onRequestFinish");

        counter++;
        if (e.getErrorCode() == MegaError.API_OK) {

            if (request.getParamType() == MegaApiJava.USER_ATTR_FIRSTNAME) {
                firstName = request.getText();
            } else if (request.getParamType() == MegaApiJava.USER_ATTR_LASTNAME) {
                lastName = request.getText();
            }
        }
        if (counter == 3) {
            newAcceptanceContactRequest();
            counter = 0;
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
