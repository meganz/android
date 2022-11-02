package mega.privacy.android.app.meeting

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import mega.privacy.android.app.globalmanagement.CallChangesObserver
import mega.privacy.android.app.MegaApplication
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import mega.privacy.android.app.main.controllers.ChatController
import nz.mega.sdk.MegaChatCall
import timber.log.Timber
import mega.privacy.android.app.utils.CallUtil
import nz.mega.sdk.MegaChatRoom
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CHAT_TITLE_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_ENTER_IN_MEETING
import mega.privacy.android.app.constants.EventConstants.EVENT_REMOVE_CALL_NOTIFICATION
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

/**
 * Service to handle mega calls
 *
 * @property callChangesObserver [CallChangesObserver]
 * @property megaApi            [MegaApiAndroid]
 * @property megaChatApi        [MegaChatApiAndroid]
 * @property app                [MegaApplication]

 */
@AndroidEntryPoint
class CallService : Service() {

    @Inject
    lateinit var callChangesObserver: CallChangesObserver

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    var app: MegaApplication? = null

    private var currentChatId: Long = MEGACHAT_INVALID_HANDLE
    private var mBuilderCompat: NotificationCompat.Builder? = null
    private var mNotificationManager: NotificationManager? = null
    private var mBuilderCompatO: NotificationCompat.Builder? = null
    private val notificationChannelId = Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID

    /**
     * If is in meeting fragment.
     */
    private var isInMeeting = true

    private val callStatusObserver = Observer { call: MegaChatCall ->
        Timber.d("Call status is ${CallUtil.callStatusToString(call.status)}. Chat id id $currentChatId")

        when (call.status) {
            MegaChatCall.CALL_STATUS_USER_NO_PRESENT, MegaChatCall.CALL_STATUS_IN_PROGRESS -> updateNotificationContent()
            MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION, MegaChatCall.CALL_STATUS_DESTROYED -> removeNotification(
                call.chatid)
        }
    }

    private val callOnHoldObserver = Observer { _: MegaChatCall? -> checkAnotherActiveCall() }

    private val titleMeetingChangeObserver = Observer { chat: MegaChatRoom ->
        if (currentChatId == chat.chatId && chat.isGroup) {
            updateNotificationContent()
        }
    }

    private val removeNotificationObserver = Observer { callId: Long ->
        megaChatApi.getChatCallByCallId(callId)?.let { call ->
            removeNotification(call.chatid)
        }
    }

    private val callAnsweredInAnotherClientObserver = Observer { chatId: Long ->
        if (currentChatId == chatId) {
            stopSelf()
        }
    }

    private val isInMeetingObserver = Observer { isOpened: Boolean ->
        isInMeeting = isOpened
        updateNotificationContent()
    }

    /**
     * Service starts
     */
    override fun onCreate() {
        super.onCreate()
        app = application as MegaApplication

        mBuilderCompat = NotificationCompat.Builder(this, notificationChannelId)
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observeForever(callStatusObserver)
        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .observeForever(callOnHoldObserver)
        LiveEventBus.get(EVENT_CHAT_TITLE_CHANGE, MegaChatRoom::class.java)
            .observeForever(titleMeetingChangeObserver)
        LiveEventBus.get(EVENT_REMOVE_CALL_NOTIFICATION, Long::class.java)
            .observeForever(removeNotificationObserver)
        LiveEventBus.get(EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT, Long::class.java)
            .observeForever(callAnsweredInAnotherClientObserver)
        LiveEventBus.get(EVENT_ENTER_IN_MEETING, Boolean::class.java)
            .observeForever(isInMeetingObserver)
    }

    /**
     * Bind service
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Start service work
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        Timber.d("Starting Call service (flags: %d, startId: %d)", flags, startId)
        intent.extras?.let { extras ->
            currentChatId =
                extras.getLong(Constants.CHAT_ID, MEGACHAT_INVALID_HANDLE)
            Timber.d("Chat handle to call: $currentChatId")
        }

        if (currentChatId == MEGACHAT_INVALID_HANDLE) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (callChangesObserver.getOpenCallChatId() != currentChatId) {
            callChangesObserver.setOpenCallChatId(currentChatId)
        }

        showCallInProgressNotification()
        return START_NOT_STICKY
    }

    /**
     * Check if another call is active
     */
    private fun checkAnotherActiveCall() {
        val activeCall = CallUtil.isAnotherActiveCall(currentChatId)
        if (currentChatId == activeCall) {
            updateNotificationContent()
        } else {
            updateCall(activeCall)
        }
    }

    /**
     * Method to create Pending intent for return to a call
     *
     * @param call MegaChatCall
     * @param requestCode RequestCode
     * @return The pending intent to return to a call.
     */
    private fun getPendingIntent(call: MegaChatCall, requestCode: Int): PendingIntent? {
        var intentCall: PendingIntent? = null
        if (call.status == MegaChatCall.CALL_STATUS_USER_NO_PRESENT && call.isRinging) {
            intentCall =
                CallUtil.getPendingIntentMeetingRinging(this,
                    currentChatId,
                    requestCode)
        } else if (call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
            intentCall = if (isInMeeting) {
                PendingIntent.getBroadcast(this,
                    0,
                    Intent(""),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            } else {
                CallUtil.getPendingIntentMeetingInProgress(this,
                    currentChatId,
                    requestCode,
                    megaApi.isEphemeralPlusPlus)
            }
        }

        return intentCall
    }

    /**
     * Update the content of the notification
     */
    private fun updateNotificationContent() {
        Timber.d("Updating notification")
        megaChatApi.getChatRoom(currentChatId)?.let { chat ->
            megaChatApi.getChatCall(currentChatId)?.let { call ->
                val notificationId = CallUtil.getCallNotificationId(call.callId)
                val pendingIntent: PendingIntent? = getPendingIntent(call, notificationId + 1)

                val contentText =
                    if (call.status == MegaChatCall.CALL_STATUS_USER_NO_PRESENT && call.isRinging)
                        StringResourcesUtils.getString(R.string.title_notification_incoming_call)
                    else if (call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS && call.isOnHold)
                        StringResourcesUtils.getString(R.string.call_on_hold)
                    else if (call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS && !call.isOnHold)
                        StringResourcesUtils.getString(R.string.title_notification_call_in_progress)
                    else ""

                val title = ChatUtil.getTitleChat(chat)

                val largeIcon: Bitmap =
                    if (chat.isGroup)
                        createDefaultAvatar(MEGACHAT_INVALID_HANDLE, title)
                    else
                        setProfileContactAvatar(chat.getPeerHandle(0),
                            title,
                            ChatController(this@CallService).getParticipantEmail(chat.getPeerHandle(
                                0)))

                val actionIcon = R.drawable.ic_phone_white
                val actionPendingIntent = getPendingIntent(call, notificationId + 1)
                val actionTitle =
                    StringResourcesUtils.getString(R.string.button_notification_call_in_progress)

                val newNotification: Notification? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mBuilderCompatO?.clearActions()
                        mBuilderCompatO?.apply {
                            setContentTitle(title)
                            setContentIntent(pendingIntent)
                            setLargeIcon(largeIcon)
                            addAction(actionIcon,
                                actionTitle,
                                actionPendingIntent)

                            if (!TextUtil.isTextEmpty(contentText))
                                setContentText(contentText)
                        }

                        mBuilderCompatO?.build()
                    } else {
                        mBuilderCompat?.clearActions()
                        mBuilderCompat?.apply {
                            setContentTitle(title)
                            setContentIntent(pendingIntent)
                            setLargeIcon(largeIcon)
                            addAction(actionIcon,
                                actionTitle,
                                actionPendingIntent)

                            if (!TextUtil.isTextEmpty(contentText))
                                setContentText(contentText)
                        }

                        mBuilderCompat?.build()
                    }

                startForeground(notificationId, newNotification)
            }
        }
    }

    /**
     * Show call in progress notification
     */
    private fun showCallInProgressNotification() {
        Timber.d("Showing the notification")
        val notificationId = currentCallNotificationId
        if (notificationId == Constants.INVALID_CALL)
            return

        megaChatApi.getChatRoom(currentChatId)?.let { chat ->
            megaChatApi.getChatCall(currentChatId)?.let { call ->
                val title = ChatUtil.getTitleChat(chat)
                val colorNotification = ContextCompat.getColor(this@CallService,
                    R.color.red_600_red_300)
                val smallIcon = R.drawable.ic_stat_notify
                val largeIcon: Bitmap =
                    if (chat.isGroup)
                        createDefaultAvatar(MEGACHAT_INVALID_HANDLE, title)
                    else
                        setProfileContactAvatar(chat.getPeerHandle(0),
                            title,
                            ChatController(this@CallService).getParticipantEmail(chat.getPeerHandle(
                                0)))
                val actionIcon = R.drawable.ic_phone_white
                val actionPendingIntent = getPendingIntent(call, notificationId + 1)
                val actionTitle =
                    StringResourcesUtils.getString(R.string.button_notification_call_in_progress)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(notificationChannelId,
                        Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT)

                    channel.apply {
                        setShowBadge(true)
                        setSound(null, null)
                    }

                    mNotificationManager =
                        this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    mNotificationManager?.createNotificationChannel(channel)
                    mBuilderCompatO = NotificationCompat.Builder(this, notificationChannelId)

                    mBuilderCompatO?.apply {
                        setSmallIcon(smallIcon)
                        setAutoCancel(false)
                        addAction(actionIcon,
                            actionTitle,
                            actionPendingIntent)
                        setOngoing(false)
                        color = colorNotification
                    }

                    mBuilderCompatO?.apply {
                        setLargeIcon(largeIcon)
                        setContentTitle(title)
                    }
                } else {
                    mBuilderCompat = NotificationCompat.Builder(this, notificationChannelId)
                    mNotificationManager =
                        this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                    mBuilderCompat?.apply {
                        setSmallIcon(smallIcon)
                        setAutoCancel(false)
                        addAction(actionIcon,
                            actionTitle,
                            actionPendingIntent)
                        setOngoing(false)
                        color = colorNotification
                    }

                    mBuilderCompat?.apply {
                        setLargeIcon(largeIcon)
                        setContentTitle(title)
                    }
                }
                updateNotificationContent()
            }
        }
    }

    /**
     * Method to update the MegaChatCall
     *
     * @param newChatIdCall Chat id.
     */
    private fun updateCall(newChatIdCall: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        cancelNotification()
        currentChatId = newChatIdCall
        if (callChangesObserver.getOpenCallChatId() != currentChatId) {
            callChangesObserver.setOpenCallChatId(currentChatId)
        }

        showCallInProgressNotification()
    }

    /**
     * Method to remove the notification
     *
     * @param chatId Chat id.
     */
    private fun removeNotification(chatId: Long) {
        val listCalls = CallUtil.getCallsParticipating()
        if (listCalls == null || listCalls.size == 0) {
            stopNotification(chatId)
            return
        }

        for (chatCall in listCalls) {
            if (chatCall != currentChatId) {
                updateCall(chatCall)
                return
            }
        }

        stopNotification(currentChatId)
    }

    /**
     * Method for cancelling a notification that is being displayed.
     *
     * @param chatId That chat ID of a call.
     */
    private fun stopNotification(chatId: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        mNotificationManager?.cancel(getCallNotificationId(chatId))
        stopSelf()
    }

    /**
     * Method to get a contact's avatar
     *
     * @param userHandle User handle.
     * @param fullName User name.
     * @param email User email.
     * @return A Bitmap with the avatar.
     */
    fun setProfileContactAvatar(userHandle: Long, fullName: String, email: String): Bitmap {
        val avatar =
            buildAvatarFile(
                this,
                email + FileUtil.JPG_EXTENSION
            )
        if (FileUtil.isFileAvailable(avatar)) {
            if (avatar != null && avatar.exists() && avatar.length() > 0) {
                val avatarBitmap =
                    BitmapFactory.decodeFile(avatar.absolutePath, BitmapFactory.Options())
                if (avatarBitmap != null) {
                    return getCircleBitmap(avatarBitmap)
                }

                avatar.delete()
            }
        }

        return createDefaultAvatar(userHandle, fullName)
    }

    /**
     * Get the bitmap as a circle
     *
     * @param bitmap User avatar bitmap
     * @return the bitmap as a circle
     */
    private fun getCircleBitmap(bitmap: Bitmap?): Bitmap {
        val output = Bitmap.createBitmap(bitmap!!.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val color = Color.RED
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        bitmap.recycle()
        return output
    }

    /**
     * Create default avatar
     *
     * @param userHandle User handle.
     * @param fullName User name.
     * @return A bitmap with the avatar.
     */
    private fun createDefaultAvatar(userHandle: Long, fullName: String): Bitmap {
        val color = if (userHandle != MEGACHAT_INVALID_HANDLE) {
            AvatarUtil.getColorAvatar(userHandle)
        } else {
            AvatarUtil.getSpecificAvatarColor(Constants.AVATAR_GROUP_CHAT_COLOR)
        }
        return AvatarUtil.getDefaultAvatar(color, fullName, Constants.AVATAR_SIZE, true)
    }

    /**
     * Method for getting the call notification ID from the chat ID.
     *
     * @return call notification ID.
     */
    private val currentCallNotificationId: Int
        get() {
            val call = megaChatApi.getChatCall(currentChatId)
                ?: return Constants.INVALID_CALL
            return CallUtil.getCallNotificationId(call.callId)
        }

    /**
     * Method for cancel notification
     */
    private fun cancelNotification() {
        val notificationId = currentCallNotificationId
        if (notificationId == Constants.INVALID_CALL) return

        mNotificationManager?.cancel(notificationId)
    }

    /**
     * Method to get the notification id of a particular call
     *
     * @param chatId That chat ID of the call.
     * @return The id of the notification.
     */
    private fun getCallNotificationId(chatId: Long): Int {
        return MegaApiJava.userHandleToBase64(chatId).hashCode()
    }

    /**
     * Service ends
     */
    override fun onDestroy() {
        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .removeObserver(callStatusObserver)
        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .removeObserver(callOnHoldObserver)
        LiveEventBus.get(EVENT_CHAT_TITLE_CHANGE, MegaChatRoom::class.java)
            .removeObserver(titleMeetingChangeObserver)
        LiveEventBus.get(EVENT_REMOVE_CALL_NOTIFICATION, Long::class.java)
            .removeObserver(removeNotificationObserver)
        LiveEventBus.get(EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT, Long::class.java)
            .removeObserver(callAnsweredInAnotherClientObserver)
        LiveEventBus.get(EVENT_ENTER_IN_MEETING, Boolean::class.java)
            .removeObserver(isInMeetingObserver)
        cancelNotification()

        callChangesObserver.setOpenCallChatId(MEGACHAT_INVALID_HANDLE)
        super.onDestroy()
    }
}