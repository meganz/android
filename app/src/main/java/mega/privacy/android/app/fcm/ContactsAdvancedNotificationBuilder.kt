package mega.privacy.android.app.fcm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.contacts.ContactsActivity.Companion.getReceivedRequestsIntent
import mega.privacy.android.app.di.getDbHandler
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.icon.pack.R
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.getPendingIntentConsideringSingleActivityWithDestination
import mega.privacy.android.navigation.megaNavigator
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

class ContactsAdvancedNotificationBuilder(
    context: Context,
    private var notificationManager: NotificationManager?,
    var megaApi: MegaApiAndroid,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : MegaRequestListenerInterface {
    private val context: Context = context.applicationContext
    var dbH: DatabaseHandler? = getDbHandler()
    private var counter = 0
    private var email = ""
    private var firstName = ""
    private var lastName = ""

    private val notificationChannelIdSimple = Constants.NOTIFICATION_CHANNEL_CONTACTS_ID
    private val notificationChannelNameSimple = Constants.NOTIFICATION_CHANNEL_CONTACTS_NAME
    private val notificationChannelIdSummary = Constants.NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_ID
    private val notificationChannelNameSummary =
        Constants.NOTIFICATION_CHANNEL_CONTACTS_SUMMARY_NAME

    suspend fun showIncomingContactRequestNotification(contactRequests: List<ContactRequest>) {
        Timber.d("showIncomingContactRequestNotification")

        val manufacturer = "xiaomi"
        if (!manufacturer.equals(Build.MANUFACTURER, ignoreCase = true)) {
            Timber.d("POST Android N")
            newIncomingContactRequest(contactRequests)
        } else {
            Timber.d("XIAOMI POST Android N")
            generateIncomingNotificationPreN(contactRequests)
        }
    }

    fun showAcceptanceContactRequestNotification(email: String) {
        Timber.d("showAcceptanceContactRequestNotification")

        this.email = email
        counter = 0
        megaApi.getUserAttribute(email, MegaApiJava.USER_ATTR_FIRSTNAME, this)
        megaApi.getUserAttribute(email, MegaApiJava.USER_ATTR_LASTNAME, this)
        megaApi.getUserAvatar(email, buildAvatarFile("$email.jpg")?.absolutePath, this)
    }

    private suspend fun newIncomingContactRequest(contacts: List<ContactRequest>) {
        Timber.d("Number of incoming contact request: %s", contacts.size)

        for (i in contacts.indices.reversed()) {
            Timber.d("REQUEST: %s", i)
            val contactRequest = contacts[i]
            Timber.d("User sent: %s", contactRequest.sourceEmail)
            sendBundledNotificationIPC(contactRequest, i == 0)
        }
    }

    private suspend fun newAcceptanceContactRequest() {
        Timber.d("newAcceptanceContactRequest")

        val manufacturer = "xiaomi"
        if (!manufacturer.equals(Build.MANUFACTURER, ignoreCase = true)) {
            Timber.d("POST Android N")
            sendBundledNotificationAPC()
        } else {
            Timber.d("XIAOMI POST Android N")
            showSimpleNotificationAPC()
        }
    }

    private suspend fun sendBundledNotificationIPC(crToShow: ContactRequest, beep: Boolean) {
        Timber.d("sendBundledNotificationIPC")

        val notification = buildIPCNotification(crToShow, beep)

        val handleString = MegaApiJava.userHandleToBase64(crToShow.handle)
        val notificationId = handleString.hashCode()

        notificationManager?.notify(notificationId, notification)
        val summary = buildSummaryIPC()
        notificationManager?.notify(Constants.NOTIFICATION_SUMMARY_INCOMING_CONTACT, summary)
    }

    private suspend fun generateIncomingNotificationPreN(icr: List<ContactRequest>) {
        Timber.d("generateIncomingNotificationPreN")

        createNotificationChannel(
            notificationChannelIdSimple,
            notificationChannelNameSimple,
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationBuilder = NotificationCompat.Builder(context, notificationChannelIdSimple)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(
                ContextCompat.getColor(
                    context,
                    mega.privacy.android.app.R.color.red_600_red_300
                )
            )
            .setContentIntent(getIPCPendingIntent(1))
            .setAutoCancel(true)

        val inboxStyle = NotificationCompat.InboxStyle()

        notificationBuilder.setShowWhen(true)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        notificationBuilder.setSound(defaultSoundUri)
        notificationBuilder.setVibrate(longArrayOf(0, 500))

        notificationBuilder.setStyle(inboxStyle)

        notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH)

        for (i in icr.indices) {
            val contactRequest = icr[i]
            Timber.d("User sent: %s", contactRequest.sourceEmail)
            inboxStyle.addLine(contactRequest.sourceEmail)
        }

        if (icr.size == 1) {
            val largeIcon = createDefaultAvatar(icr[0].sourceEmail)
            notificationBuilder.setLargeIcon(largeIcon)
        } else {
            val count = icr.size.toString() + ""
            val largeIcon = createDefaultAvatar(count)
            notificationBuilder.setLargeIcon(largeIcon)
        }

        val textToShow = context.resources.getQuantityString(
            mega.privacy.android.app.R.plurals.plural_number_contact_request_notification,
            icr.size,
            icr.size
        )

        notificationBuilder.setContentTitle(
            context.resources
                .getString(mega.privacy.android.app.R.string.title_new_contact_request_notification)
        )
        notificationBuilder.setContentText(textToShow)
        inboxStyle.setSummaryText(textToShow)

        val notif = notificationBuilder.build()

        notificationManager?.notify(Constants.NOTIFICATION_SUMMARY_INCOMING_CONTACT, notif)
    }

    private suspend fun sendBundledNotificationAPC() {
        Timber.d("sendBundledNotificationAPC")

        val notification = buildAPCNotification()

        val notificationId = email.hashCode()

        notificationManager?.notify(notificationId, notification)
        val summary = buildSummaryAPC()
        notificationManager?.notify(Constants.NOTIFICATION_SUMMARY_ACCEPTANCE_CONTACT, summary)
    }

    private suspend fun buildIPCNotification(
        crToShow: ContactRequest?,
        beep: Boolean,
    ): Notification? {
        Timber.d("buildIPCNotification")

        val notificationContent: String?
        if (crToShow != null) {
            notificationContent = crToShow.sourceEmail
        } else {
            Timber.e("Return because the request is NULL")
            return null
        }

        createNotificationChannel(
            notificationChannelIdSimple,
            notificationChannelNameSimple,
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationBuilderO = NotificationCompat.Builder(context, notificationChannelIdSimple)
        notificationBuilderO
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(mega.privacy.android.app.R.string.title_contact_request_notification))
            .setContentText(notificationContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent))
            .setAutoCancel(true)
            .setContentIntent(getIPCPendingIntent(crToShow.handle.toInt()))
            .setGroup(GROUP_KEY_IPC)
            .setColor(
                ContextCompat.getColor(
                    context,
                    mega.privacy.android.app.R.color.red_600_red_300
                )
            )

        if (beep) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            notificationBuilderO.setSound(defaultSoundUri)
            notificationBuilderO.setVibrate(longArrayOf(0, 500))
        }

        val largeIcon = createDefaultAvatar(crToShow.sourceEmail)
        notificationBuilderO.setLargeIcon(largeIcon)

        notificationBuilderO.setPriority(NotificationManager.IMPORTANCE_HIGH)

        return notificationBuilderO.build()
    }

    private suspend fun buildAPCNotification(): Notification {
        Timber.d("buildAPCNotification")

        var title =
            context.getString(mega.privacy.android.app.R.string.title_acceptance_contact_request_notification)
        var fullName = "$firstName $lastName"

        if (firstName.trim { it <= ' ' }.isEmpty()) {
            fullName = lastName
        }

        if (!fullName.trim { it <= ' ' }.isEmpty()) {
            title = "$title: $fullName"
        }

        createNotificationChannel(
            notificationChannelIdSimple,
            notificationChannelNameSimple,
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationBuilderO = NotificationCompat.Builder(context, notificationChannelIdSimple)
        notificationBuilderO
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(email)
            .setAutoCancel(true)
            .setContentIntent(getAPCPendingIntent(email.hashCode()))
            .setGroup(GROUP_KEY_APC)
            .setColor(
                ContextCompat.getColor(
                    context,
                    mega.privacy.android.app.R.color.red_600_red_300
                )
            )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        notificationBuilderO.setSound(defaultSoundUri)
        notificationBuilderO.setVibrate(longArrayOf(0, 500))

        val largeIcon = setUserAvatar(email)
        notificationBuilderO.setLargeIcon(largeIcon)

        notificationBuilderO.setPriority(NotificationManager.IMPORTANCE_HIGH)

        return notificationBuilderO.build()
    }

    private fun createDefaultAvatar(email: String?): Bitmap {
        return AvatarUtil.getDefaultAvatar(
            AvatarUtil.getColorAvatar(megaApi.getContact(email)),
            email,
            Constants.AVATAR_SIZE,
            true,
            false
        )
    }

    private fun setUserAvatar(contactMail: String?): Bitmap {
        Timber.d("setUserAvatar")

        return AvatarUtil.getAvatarBitmap(contactMail)
            ?: createDefaultAvatar(contactMail)
    }

    private suspend fun buildSummaryIPC(): Notification {
        createNotificationChannel(
            notificationChannelIdSummary,
            notificationChannelNameSummary,
            NotificationManager.IMPORTANCE_HIGH
        )

        val notificationBuilderO = NotificationCompat.Builder(context, notificationChannelIdSummary)
        notificationBuilderO
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setShowWhen(true)
            .setGroup(GROUP_KEY_IPC)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(getIPCPendingIntent(1))
            .setColor(
                ContextCompat.getColor(
                    context,
                    mega.privacy.android.app.R.color.red_600_red_300
                )
            )

        return notificationBuilderO.build()
    }

    private suspend fun getIPCPendingIntent(requestCode: Int) =
        context.megaNavigator.getPendingIntentConsideringSingleActivityWithDestination<ContactsActivity, ContactsNavKey>(
            context = context,
            createPendingIntent = {
                PendingIntent.getActivity(
                    context,
                    requestCode,
                    getReceivedRequestsIntent(context),
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            },
            singleActivityDestination = { ContactsNavKey(navType = ContactsNavKey.NavType.ReceivedRequests) }
        )

    private suspend fun buildSummaryAPC(): Notification {
        createNotificationChannel(
            notificationChannelIdSummary,
            notificationChannelNameSummary,
            NotificationManager.IMPORTANCE_HIGH
        )

        val notificationBuilderO = NotificationCompat.Builder(context, notificationChannelIdSummary)
        notificationBuilderO
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setShowWhen(true)
            .setGroup(GROUP_KEY_APC)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(getAPCPendingIntent(2))
            .setColor(
                ContextCompat.getColor(
                    context,
                    mega.privacy.android.app.R.color.red_600_red_300
                )
            )

        return notificationBuilderO.build()
    }

    private suspend fun getAPCPendingIntent(requestCode: Int) =
        context.megaNavigator.getPendingIntentConsideringSingleActivityWithDestination<ContactsActivity, ContactsNavKey>(
            context = context,
            createPendingIntent = { intent ->
                PendingIntent.getActivity(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            },
            singleActivityDestination = { ContactsNavKey() }
        )

    fun removeAllIncomingContactNotifications() {
        notificationManager?.cancel(Constants.NOTIFICATION_SUMMARY_INCOMING_CONTACT)
    }

    private suspend fun showSimpleNotificationAPC() {
        var title =
            context.getString(mega.privacy.android.app.R.string.title_acceptance_contact_request_notification)
        var fullName = "$firstName $lastName"

        if (firstName.trim { it <= ' ' }.isEmpty()) {
            fullName = lastName
        }

        if (!fullName.trim { it <= ' ' }.isEmpty()) {
            title = "$title: $fullName"
        }

        createNotificationChannel(
            notificationChannelIdSimple,
            notificationChannelNameSimple,
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationBuilderO = NotificationCompat.Builder(context, notificationChannelIdSimple)
        notificationBuilderO
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentIntent(getAPCPendingIntent(email.hashCode()))
            .setAutoCancel(true).setTicker(title)
            .setContentTitle(title).setContentText(email)
            .setOngoing(false)
            .setColor(
                ContextCompat.getColor(
                    context,
                    mega.privacy.android.app.R.color.red_600_red_300
                )
            )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        notificationBuilderO.setSound(defaultSoundUri)
        notificationBuilderO.setVibrate(longArrayOf(0, 500))

        val largeIcon = setUserAvatar(email)
        notificationBuilderO.setLargeIcon(largeIcon)

        notificationBuilderO.setPriority(NotificationManager.IMPORTANCE_HIGH)

        notificationManager?.notify(
            Constants.NOTIFICATION_GENERAL_PUSH_CHAT,
            notificationBuilderO.build()
        )
    }

    private fun createNotificationChannel(id: String?, name: String?, importance: Int) {
        val channel = NotificationChannel(id, name, importance)
        channel.setShowBadge(true)
        if (notificationManager == null) {
            notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        }
        notificationManager?.createNotificationChannel(channel)
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish")

        counter++
        if (e.errorCode == MegaError.API_OK) {
            if (request.paramType == MegaApiJava.USER_ATTR_FIRSTNAME) {
                firstName = request.text
            } else if (request.paramType == MegaApiJava.USER_ATTR_LASTNAME) {
                lastName = request.text
            }
        }
        if (counter == 3) {
            applicationScope.launch { newAcceptanceContactRequest() }
            counter = 0
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
    }

    companion object {
        private const val GROUP_KEY_IPC = "IPCNotificationBuilder"
        private const val GROUP_KEY_APC = "APCNotificationBuilder"

        fun newInstance(
            context: Context,
            megaApi: MegaApiAndroid,
            @ApplicationScope applicationScope: CoroutineScope,
        ): ContactsAdvancedNotificationBuilder {
            val appContext = context.applicationContext
            var safeContext = ContextCompat.createDeviceProtectedStorageContext(appContext)
            if (safeContext == null) {
                safeContext = appContext
            }
            val notificationManager =
                safeContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

            return ContactsAdvancedNotificationBuilder(
                context = safeContext,
                notificationManager = notificationManager,
                megaApi = megaApi,
                applicationScope = applicationScope
            )
        }
    }
}
