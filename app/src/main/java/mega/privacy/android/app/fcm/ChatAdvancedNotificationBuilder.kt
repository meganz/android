package mega.privacy.android.app.fcm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Build
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import mega.privacy.android.app.MegaApplication.Companion.getChatManagement
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.di.getDbHandler
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.meeting.CallNotificationIntentService
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.getPendingIntentConsideringSingleActivityWithDestination
import mega.privacy.android.navigation.megaNavigator
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRoom
import timber.log.Timber

class ChatAdvancedNotificationBuilder(
    context: Context,
    private var notificationManager: NotificationManager?,
) {
    private val context: Context = context.applicationContext
    var dbH: DatabaseHandler? = getDbHandler()
    var megaApi: MegaApiAndroid? = getInstance().megaApi
    var megaChatApi: MegaChatApiAndroid = getInstance().getMegaChatApi()

    private val chatC: ChatController = ChatController(context)

    private val numberButtons: String
        /**
         * Method for knowing how many buttons need to be displayed in notifications.
         */
        get() {
            val currentCalls = CallUtil.getCallsParticipating()

            if (CallUtil.participatingInACall() && currentCalls != null) {
                val callsOnHold = ArrayList<MegaChatCall?>()
                val callsActive = ArrayList<MegaChatCall?>()

                for (currentCall in currentCalls) {
                    val current = megaChatApi.getChatCall(currentCall)

                    current?.let {
                        if (current.isOnHold) {
                            callsOnHold.add(current)
                        } else {
                            callsActive.add(current)
                        }
                    }
                }

                if ((!callsActive.isEmpty() && callsOnHold.isEmpty()) || (callsActive.isEmpty() && !callsOnHold.isEmpty())) {
                    return THREE_BUTTONS
                }

                if (!callsActive.isEmpty()) {
                    return VERTICAL_TWO_BUTTONS
                }
            }

            return HORIZONTAL_TWO_BUTTONS
        }

    private fun notify(id: Int, notification: Notification?) {
        notificationIds.add(id)
        notificationManager?.notify(id, notification)
    }

    private fun notifyCall(id: Int, notification: Notification?) {
        if (notificationManager == null) {
            notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        }
        notificationManager?.notify(id, notification)
    }

    private fun setUserAvatar(chat: MegaChatRoom): Bitmap {
        Timber.d("Chat ID: %s", chat.chatId)
        if (!chat.isGroup) {
            val bitmap = CallUtil.getImageAvatarCall(chat.getPeerHandle(0))
            if (bitmap != null) return Util.getCircleBitmap(bitmap)
        }
        return createDefaultAvatar(chat)
    }

    private fun createDefaultAvatar(chat: MegaChatRoom): Bitmap {
        Timber.d("Chat ID: %s", chat.chatId)
        val color: Int = if (chat.isGroup) {
            AvatarUtil.getSpecificAvatarColor(Constants.AVATAR_GROUP_CHAT_COLOR)
        } else {
            AvatarUtil.getColorAvatar(chat.getPeerHandle(0))
        }

        return AvatarUtil.getDefaultAvatar(
            color,
            ChatUtil.getTitleChat(chat),
            Constants.AVATAR_SIZE,
            true,
            true
        )
    }

    /**
     * Method for obtaining the number of request required.
     *
     * @param type Type of button.
     * @return Number of request needed.
     */
    private fun getNumberRequestNotifications(type: String, chatHandleInProgress: Long) =
        when (type) {
            CallNotificationIntentService.ANSWER,
            CallNotificationIntentService.END_ANSWER,
            CallNotificationIntentService.END_JOIN,
                -> TWO_REQUEST_NEEDED

            CallNotificationIntentService.HOLD_ANSWER,
            CallNotificationIntentService.HOLD_JOIN,
                -> if (megaChatApi.getChatCall(chatHandleInProgress).isOnHold) {
                ONE_REQUEST_NEEDED
            } else {
                TWO_REQUEST_NEEDED
            }

            else -> ONE_REQUEST_NEEDED
        }

    /**
     * Gets the determined PendingIntent for a particular notification.
     *
     * @param chatIdCallInProgress Chat ID with call in progress.
     * @param chatIdCallToAnswer   Chat ID with a incoming call.
     * @param type                 Type of answer.
     * @return The PendingIntent.
     */
    private fun getPendingIntent(
        chatIdCallInProgress: Long,
        chatIdCallToAnswer: Long,
        type: String,
        notificationId: Int,
    ): PendingIntent? {
        val intent = Intent(context, CallNotificationIntentService::class.java)
        intent.putExtra(Constants.CHAT_ID_OF_CURRENT_CALL, chatIdCallInProgress)
        intent.putExtra(Constants.CHAT_ID_OF_INCOMING_CALL, chatIdCallToAnswer)
        intent.setAction(type)
        val requestCode = notificationId + getNumberRequestNotifications(type, chatIdCallInProgress)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Method to show the incoming call notification, when there is another call in progress.
     *
     * @param callToAnswer   The call that is being received.
     * @param callInProgress The current call in progress.
     */
    private fun showIncomingCallNotification(
        callToAnswer: MegaChatCall,
        callInProgress: MegaChatCall,
    ) {
        Timber.d(
            "Call to answer ID: %d, Call in progress ID: %d",
            callToAnswer.chatid,
            callInProgress.chatid
        )

        val chatIdCallToAnswer = callToAnswer.chatid
        val chatIdCallInProgress = callInProgress.chatid
        val chatToAnswer = megaChatApi.getChatRoom(chatIdCallToAnswer)
        val notificationId = CallUtil.getCallNotificationId(callToAnswer.callId)
        val shouldVibrate = !CallUtil.participatingInACall()

        val intentIgnore = getPendingIntent(
            chatIdCallInProgress,
            chatIdCallToAnswer,
            CallNotificationIntentService.IGNORE,
            notificationId
        )
        val callScreen = CallUtil.getPendingIntentMeetingRinging(
            context,
            callToAnswer.chatid,
            notificationId + ONE_REQUEST_NEEDED
        )
        val intentDecline = getPendingIntent(
            chatIdCallInProgress,
            chatIdCallToAnswer,
            CallNotificationIntentService.DECLINE,
            notificationId
        )

        val avatarIcon = setUserAvatar(chatToAnswer)

        //Collapsed
        val collapsedViews = CallUtil.collapsedAndExpandedIncomingCallNotification(
            context,
            R.layout.layout_call_notifications,
            chatToAnswer,
            avatarIcon
        )

        //Expanded
        val expandedView = CallUtil.collapsedAndExpandedIncomingCallNotification(
            context,
            R.layout.layout_call_notifications_expanded,
            chatToAnswer,
            avatarIcon
        )

        val numberButtons = this.numberButtons

        if (numberButtons != THREE_BUTTONS && numberButtons != VERTICAL_TWO_BUTTONS) {
            collapsedViews.setViewVisibility(R.id.arrow, View.GONE)
            expandedView.setViewVisibility(R.id.arrow, View.GONE)
            expandedView.setViewVisibility(R.id.small_layout, View.VISIBLE)
            expandedView.setViewVisibility(R.id.big_layout, View.GONE)
            if (CallUtil.isOneToOneCall(chatToAnswer)) {
                expandedView.setTextViewText(
                    R.id.decline_button_text,
                    context.getString(R.string.contact_decline)
                )
                expandedView.setTextViewText(
                    R.id.answer_button_text,
                    context.getString(R.string.answer_call_incoming)
                )
                expandedView.setOnClickPendingIntent(R.id.decline_button_layout, intentDecline)
            } else {
                expandedView.setTextViewText(
                    R.id.decline_button_text,
                    context.getString(R.string.ignore_call_incoming)
                )
                expandedView.setTextViewText(
                    R.id.answer_button_text,
                    context.getString(R.string.action_join)
                )
                expandedView.setOnClickPendingIntent(R.id.decline_button_layout, intentIgnore)
            }
            val intentAnswer = getPendingIntent(
                CallUtil.isAnotherActiveCall(chatIdCallInProgress),
                chatIdCallToAnswer,
                CallNotificationIntentService.ANSWER,
                notificationId
            )
            expandedView.setOnClickPendingIntent(R.id.answer_button_layout, intentAnswer)
        } else {
            collapsedViews.setViewVisibility(R.id.arrow, View.VISIBLE)
            collapsedViews.setOnClickPendingIntent(R.id.arrow, null)
            expandedView.setViewVisibility(R.id.arrow, View.VISIBLE)
            expandedView.setOnClickPendingIntent(R.id.arrow, null)
            expandedView.setViewVisibility(R.id.big_layout, View.VISIBLE)
            expandedView.setViewVisibility(R.id.small_layout, View.GONE)
            val callToHangUpChatId = CallUtil.existsAnotherCall(chatIdCallInProgress)
            if (CallUtil.isOneToOneCall(chatToAnswer)) {
                val intentHoldAnswer = getPendingIntent(
                    chatIdCallInProgress,
                    chatIdCallToAnswer,
                    CallNotificationIntentService.HOLD_ANSWER,
                    notificationId
                )
                val intentEndAnswer = getPendingIntent(
                    callToHangUpChatId,
                    chatIdCallToAnswer,
                    CallNotificationIntentService.END_ANSWER,
                    notificationId
                )

                expandedView.setTextViewText(
                    R.id.first_button_text,
                    context.getString(R.string.contact_decline)
                )
                expandedView.setTextViewText(
                    R.id.second_button_text,
                    context.getString(R.string.hold_and_answer_call_incoming)
                )
                expandedView.setTextViewText(
                    R.id.third_button_text,
                    context.getString(R.string.end_and_answer_call_incoming)
                )
                expandedView.setOnClickPendingIntent(R.id.first_button_layout, intentDecline)
                expandedView.setOnClickPendingIntent(R.id.second_button_layout, intentHoldAnswer)
                expandedView.setOnClickPendingIntent(R.id.third_button_layout, intentEndAnswer)
            } else {
                val intentHoldJoin = getPendingIntent(
                    chatIdCallInProgress,
                    chatIdCallToAnswer,
                    CallNotificationIntentService.HOLD_JOIN,
                    notificationId
                )
                val intentEndJoin = getPendingIntent(
                    callToHangUpChatId,
                    chatIdCallToAnswer,
                    CallNotificationIntentService.END_JOIN,
                    notificationId
                )

                expandedView.setTextViewText(
                    R.id.first_button_text,
                    context.getString(R.string.ignore_call_incoming)
                )
                expandedView.setTextViewText(
                    R.id.second_button_text,
                    context.getString(R.string.hold_and_join_call_incoming)
                )
                expandedView.setTextViewText(
                    R.id.third_button_text,
                    context.getString(R.string.end_and_join_call_incoming)
                )
                expandedView.setOnClickPendingIntent(R.id.first_button_layout, intentIgnore)
                expandedView.setOnClickPendingIntent(R.id.second_button_layout, intentHoldJoin)
                expandedView.setOnClickPendingIntent(R.id.third_button_layout, intentEndJoin)
            }

            if (numberButtons == VERTICAL_TWO_BUTTONS) {
                expandedView.setViewVisibility(R.id.second_button_layout, View.GONE)
            }
        }

        val contentView =
            if (numberButtons == HORIZONTAL_TWO_BUTTONS) expandedView else collapsedViews
        //Create a channel for android Oreo or higher
        val channelId =
            if (shouldVibrate) Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_ID else Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_ID


        if (notificationManager == null) {
            notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        }

        val notificationBuilderO = NotificationCompat.Builder(context, channelId)
        notificationBuilderO
            .setSmallIcon(mega.privacy.android.icon.pack.R.drawable.ic_stat_notify)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomHeadsUpContentView(contentView)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedView)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(callScreen)
            .setShowWhen(true)
            .setAutoCancel(false)
            .setDeleteIntent(intentIgnore)
            .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
            .setPriority(NotificationManager.IMPORTANCE_HIGH)

        notifyCall(notificationId, notificationBuilderO.build())
    }

    /**
     * Method for showing a incoming group or one-to-one call notification, when no other call is in progress
     *
     * @param callToAnswer The call that is being received.
     */
    fun showOneCallNotification(callToAnswer: MegaChatCall) {
        Timber.d("Call to answer ID: %s", callToAnswer.chatid)

        val chatIdCallToAnswer = callToAnswer.chatid
        val chatToAnswer = megaChatApi.getChatRoom(chatIdCallToAnswer)
        val notificationId = CallUtil.getCallNotificationId(callToAnswer.callId)

        val intentIgnore = getPendingIntent(
            MegaChatApiJava.MEGACHAT_INVALID_HANDLE,
            chatIdCallToAnswer,
            CallNotificationIntentService.IGNORE,
            notificationId
        )
        val callScreen = CallUtil.getPendingIntentMeetingRinging(
            context,
            callToAnswer.chatid,
            notificationId + ONE_REQUEST_NEEDED
        )

        val answerIntent: Intent?
        val intentAnswer: PendingIntent?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Notification trampoline restrictions
            answerIntent = Intent(context, MeetingActivity::class.java)
            answerIntent.putExtra(
                Constants.CHAT_ID_OF_CURRENT_CALL,
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
            answerIntent.putExtra(Constants.CHAT_ID_OF_INCOMING_CALL, callToAnswer.chatid)
            answerIntent.setAction(CallNotificationIntentService.ANSWER)
            intentAnswer = PendingIntent.getActivity(
                context,
                notificationId + ONE_REQUEST_NEEDED,
                answerIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            answerIntent = Intent(context, CallNotificationIntentService::class.java)
            answerIntent.putExtra(
                Constants.CHAT_ID_OF_CURRENT_CALL,
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
            answerIntent.putExtra(Constants.CHAT_ID_OF_INCOMING_CALL, callToAnswer.chatid)
            answerIntent.setAction(CallNotificationIntentService.ANSWER)
            intentAnswer = PendingIntent.getService(
                context,
                notificationId + ONE_REQUEST_NEEDED,
                answerIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val avatarIcon = setUserAvatar(chatToAnswer)

        //Collapsed
        val collapsedViews = CallUtil.collapsedAndExpandedIncomingCallNotification(
            context,
            R.layout.layout_call_notifications,
            chatToAnswer,
            avatarIcon
        )
        collapsedViews.setViewVisibility(R.id.arrow, View.GONE)

        //Expanded
        val expandedView = CallUtil.collapsedAndExpandedIncomingCallNotification(
            context,
            R.layout.layout_call_notifications_expanded,
            chatToAnswer,
            avatarIcon
        )
        expandedView.setViewVisibility(R.id.arrow, View.GONE)
        expandedView.setViewVisibility(R.id.small_layout, View.VISIBLE)
        expandedView.setViewVisibility(R.id.big_layout, View.GONE)

        if (CallUtil.isOneToOneCall(chatToAnswer)) {
            val intentDecline = getPendingIntent(
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE,
                chatIdCallToAnswer,
                CallNotificationIntentService.DECLINE,
                notificationId
            )
            expandedView.setTextViewText(
                R.id.decline_button_text,
                context.getString(R.string.contact_decline)
            )
            expandedView.setTextViewText(
                R.id.answer_button_text,
                context.getString(R.string.answer_call_incoming)
            )
            expandedView.setOnClickPendingIntent(R.id.decline_button_layout, intentDecline)
        } else {
            expandedView.setTextViewText(
                R.id.decline_button_text,
                context.getString(R.string.ignore_call_incoming)
            )
            expandedView.setTextViewText(
                R.id.answer_button_text,
                context.getString(R.string.action_join)
            )
            expandedView.setOnClickPendingIntent(R.id.decline_button_layout, intentIgnore)
        }

        expandedView.setOnClickPendingIntent(R.id.answer_button_layout, intentAnswer)

        if (notificationManager == null) {
            notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        }

        val notificationBuilderO =
            NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_ID)
        notificationBuilderO
            .setSmallIcon(mega.privacy.android.icon.pack.R.drawable.ic_stat_notify)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomHeadsUpContentView(expandedView)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedView)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(callScreen)
            .setShowWhen(true)
            .setAutoCancel(false)
            .setDeleteIntent(intentIgnore)
            .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
            .setPriority(NotificationManager.IMPORTANCE_HIGH)

        notifyCall(notificationId, notificationBuilderO.build())
    }

    fun checkQueuedCalls(incomingCallChatId: Long) {
        Timber.d("Check several calls")

        val handleList = megaChatApi.chatCalls
        if (handleList == null || handleList.size() <= 1) return

        val callsInProgress = CallUtil.getCallsParticipating()
        if (callsInProgress == null || callsInProgress.isEmpty()) {
            Timber.w("No calls in progress")
            return
        }

        Timber.d("Number of calls in progress: %s", callsInProgress.size)
        var callInProgress: MegaChatCall? = null
        for (i in callsInProgress.indices) {
            val call = megaChatApi.getChatCall(callsInProgress[i])
            if (call != null && !call.isOnHold) {
                callInProgress = call
                break
            }
        }

        if (callInProgress == null) {
            val call = megaChatApi.getChatCall(callsInProgress[0])
            if (call != null && !call.isOnHold) {
                callInProgress = call
            }
        }

        val incomingCall = megaChatApi.getChatCall(incomingCallChatId)
        if (callInProgress != null && incomingCall != null && incomingCall.isRinging && !incomingCall.isIgnored) {
            getChatManagement().addNotificationShown(incomingCall.chatid)
            showIncomingCallNotification(incomingCall, callInProgress)
        }
    }

    suspend fun showMissedCallNotification(chatId: Long, chatCallId: Long) {
        Timber.d("MISSED CALL Chat ID: %d, Call ID: %d", chatId, chatCallId)

        val chat = megaChatApi.getChatRoom(chatId)
        val notificationContent = if (chat.isGroup) {
            ChatUtil.getTitleChat(chat)
        } else {
            chatC.getParticipantFullName(chat.getPeerHandle(0))
        }

        val notificationCallId = MegaApiJava.userHandleToBase64(chatCallId)
        val notificationId = (notificationCallId).hashCode() + Constants.NOTIFICATION_MISSED_CALL
        val pendingIntent =
            context.megaNavigator.getPendingIntentConsideringSingleActivityWithDestination<ManagerActivity, ChatNavKey>(
                context = context,
                createPendingIntent = { intent ->
                    intent.also {
                        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        it.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE)
                        it.putExtra(Constants.CHAT_ID, chat.chatId)
                    }

                    PendingIntent.getActivity(
                        context,
                        chat.chatId.toInt(),
                        intent,
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                    )
                },
                singleActivityDestination = { ChatNavKey(chatId = chatId) }
            )

        val pattern = longArrayOf(0, 1000)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (notificationManager == null) {
            notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        }
        val notificationBuilderO = NotificationCompat.Builder(
            context,
            Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID
        )
        notificationBuilderO
            .setSmallIcon(mega.privacy.android.icon.pack.R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(R.string.missed_call_notification_title))
            .setContentText(notificationContent)
            .setAutoCancel(true)
            .setVibrate(pattern)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
            .setPriority(NotificationManager.IMPORTANCE_HIGH)

        if (!TextUtil.isTextEmpty(chatC.getParticipantEmail(chat.getPeerHandle(0)))) {
            val largeIcon = setUserAvatar(chat)
            notificationBuilderO.setLargeIcon(largeIcon)
        }

        notify(notificationId, notificationBuilderO.build())
    }

    companion object {
        private const val THREE_BUTTONS = "THREE_BUTTONS"
        private const val VERTICAL_TWO_BUTTONS = "VERTICAL_TWO_BUTTONS"
        private const val HORIZONTAL_TWO_BUTTONS = "HORIZONTAL_TWO_BUTTONS"


        private const val ONE_REQUEST_NEEDED = 1
        private const val TWO_REQUEST_NEEDED = 2

        private val notificationIds: MutableSet<Int?> = HashSet<Int?>()

        fun newInstance(context: Context): ChatAdvancedNotificationBuilder {
            val appContext = context.applicationContext
            var safeContext = ContextCompat.createDeviceProtectedStorageContext(appContext)
            if (safeContext == null) {
                safeContext = appContext
            }
            val notificationManager =
                safeContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

            return ChatAdvancedNotificationBuilder(safeContext, notificationManager)
        }
    }
}
