package mega.privacy.android.app.utils

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.view.MenuItem
import android.view.View
import android.widget.Chronometer
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.DisableAudioVideoCallListener
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.meeting.WaitingRoomActivity
import mega.privacy.android.app.presentation.openlink.OpenLinkActivity
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.AvatarUtil.getAvatarBitmap
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.AvatarUtil.getUserAvatar
import mega.privacy.android.app.utils.CallUtil.activateChrono
import mega.privacy.android.app.utils.ChatUtil.getStatusBitmap
import mega.privacy.android.app.utils.ChatUtil.getTitleChat
import mega.privacy.android.app.utils.ContactUtil.getNicknameContact
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.Util.isScreenInPortrait
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.domain.entity.StorageState
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatSession
import timber.log.Timber

object CallUtil {

    /**
     * Method for opening the Meeting Activity when the meeting is outgoing or in progress call
     *
     * @param context            Context
     * @param chatId             chat ID
     * @param meetingName        Meeting Name
     * @param link               Meeting's link
     */
    @JvmStatic
    fun openMeetingToJoin(
        context: Context,
        chatId: Long,
        meetingName: String?,
        link: String?,
        publicChatHandle: Long,
        isRejoin: Boolean,
        isWaitingRoom: Boolean,
    ) {
        Timber.d("Open join a meeting screen:: chatId = %s", chatId)
        MegaApplication.getChatManagement().setOpeningMeetingLink(chatId, true)
        val intent: Intent
        if (isWaitingRoom) {
            intent = Intent(context, WaitingRoomActivity::class.java)
            intent.putExtra(WaitingRoomActivity.EXTRA_CHAT_ID, chatId)
            intent.putExtra(WaitingRoomActivity.EXTRA_CHAT_LINK, link)
        } else {
            intent = Intent(context, MeetingActivity::class.java)
            if (isRejoin) {
                intent.action = MeetingActivity.MEETING_ACTION_JOIN
                intent.putExtra(MeetingActivity.MEETING_PUBLIC_CHAT_HANDLE, publicChatHandle)
            } else {
                intent.action = MeetingActivity.MEETING_ACTION_JOIN
            }
            intent.putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
            intent.putExtra(MeetingActivity.MEETING_NAME, meetingName)
            intent.data = Uri.parse(link)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Method for opening the Meeting Activity when the meeting is outgoing or in progress call
     *
     * @param context            Context
     * @param chatId             chat ID
     */
    @JvmStatic
    fun openMeetingRinging(context: Context, chatId: Long) {
        Timber.d("Open incoming call screen. Chat id is %s", chatId)
        MegaApplication.getInstance().openCallService(chatId)
        val meetingIntent = Intent(context, MeetingActivity::class.java)
        meetingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        meetingIntent.action = MeetingActivity.MEETING_ACTION_RINGING
        meetingIntent.putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
        context.startActivity(meetingIntent)
    }

    /**
     * Method for opening the Meeting Activity when the meeting is outgoing or in progress call
     *
     * @param context               Context
     * @param chatId                Chat ID
     * @param isSessionOnRecording  True if call is already being recorded of False otherwise.
     */
    @JvmStatic
    @JvmOverloads
    fun openMeetingInProgress(
        context: Context,
        chatId: Long,
        isNewTask: Boolean,
        isSessionOnRecording: Boolean? = false,
    ) {
        Timber.d("Open in progress call screen. Chat id is %s", chatId)
        if (isNewTask) {
            MegaApplication.getInstance().openCallService(chatId)
        }

        val meetingIntent = Intent(context, MeetingActivity::class.java)
        meetingIntent.action = MeetingActivity.MEETING_ACTION_IN
        meetingIntent.putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
        meetingIntent.putExtra(
            MeetingActivity.MEETING_IS_GUEST,
            MegaApplication.getInstance().megaApi.isEphemeralPlusPlus,
        )
        if (isNewTask) {
            Timber.d("New task")
            meetingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            meetingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        context.startActivity(meetingIntent)
    }

    /**
     * Method for opening the Meeting Activity when the meeting is outgoing or in progress call with audio or video enable.
     *
     * @param context            Context
     * @param chatId             chat ID
     * @param isAudioEnable      it the audio is ON
     * @param isVideoEnable      it the video is ON
     */
    @JvmStatic
    fun openMeetingWithAudioOrVideo(
        context: Context,
        chatId: Long,
        isAudioEnable: Boolean,
        isVideoEnable: Boolean,
    ) {
        Timber.d("Open call with audio or video. Chat id is %s", chatId)
        MegaApplication.getInstance().openCallService(chatId)
        val meetingIntent = Intent(context, MeetingActivity::class.java)
        meetingIntent.action = MeetingActivity.MEETING_ACTION_IN
        meetingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        meetingIntent.putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
        meetingIntent.putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, isAudioEnable)
        meetingIntent.putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, isVideoEnable)
        context.startActivity(meetingIntent)
    }

    /**
     * Method for opening the Meeting Activity in guest mode
     *
     * @param context            Context
     * @param meetingName        Meeting Name
     * @param chatId             chat ID
     * @param link               Meeting's link
     */
    @JvmStatic
    fun openMeetingGuestMode(
        context: Context,
        meetingName: String?,
        chatId: Long,
        link: String?,
        chatRequestHandler: MegaChatRequestHandler,
        isWaitingRoom: Boolean,
    ) {
        Timber.d("Open meeting in guest mode. Chat id is %s", chatId)
        MegaApplication.getChatManagement().setOpeningMeetingLink(chatId, true)
        chatRequestHandler.setIsLoginRunning(true)
        val intent: Intent
        if (isWaitingRoom) {
            intent = Intent(context, WaitingRoomActivity::class.java)
            intent.putExtra(WaitingRoomActivity.EXTRA_CHAT_ID, chatId)
            intent.putExtra(WaitingRoomActivity.EXTRA_CHAT_LINK, link)
        } else {
            intent = Intent(context, MeetingActivity::class.java)
            intent.action = MeetingActivity.MEETING_ACTION_GUEST
            if (!meetingName.isNullOrBlank()) {
                intent.putExtra(MeetingActivity.MEETING_NAME, meetingName)
            }
            intent.putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
            intent.putExtra(MeetingActivity.MEETING_IS_GUEST, true)
            intent.data = Uri.parse(link)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Retrieve if there's a call in progress that you're participating in. use [IsParticipatingInChatCallUseCase] instead
     *
     * @return True if you're on a call in progress. Otherwise false.
     */
    @JvmStatic
    @Deprecated("Use IsParticipatingInChatCallUseCase instead")
    fun participatingInACall(): Boolean {
        val megaChatApi = MegaApplication.getInstance().megaChatApi
        val listCallsInitial = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_INITIAL)
        val listCallsConnecting = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_CONNECTING)
        val listCallsJoining = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_JOINING)
        val listCallsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS)

        return listCallsInitial.size() > 0 || listCallsConnecting.size() > 0 ||
                listCallsJoining.size() > 0 || listCallsInProgress.size() > 0
    }

    /**
     * Retrieve if there's a call in progress that you're participating in or a incoming call.
     *
     * @return True if you're on a call in progress o exists a incoming call. Otherwise false.
     */
    @JvmStatic
    fun existsAnOngoingOrIncomingCall(): Boolean {
        val megaChatApi = MegaApplication.getInstance().megaChatApi
        val listCallsUserNoPresent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_USER_NO_PRESENT)
        val listCallsUserTerminatingUserParticipation =
            megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION)
        val listCallsDestroy = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_DESTROYED)
        val listCalls = megaChatApi.chatCalls

        if (listCalls.size() - listCallsDestroy.size() == 0L) {
            Timber.d("No calls in progress")
            return false
        }

        if (listCalls.size() - listCallsDestroy.size() ==
            listCallsUserNoPresent.size() + listCallsUserTerminatingUserParticipation.size()
        ) {
            Timber.d("I'm not participating in any of the calls there")
            return false
        }

        return true
    }

    /**
     * Opens the call that is in progress.
     *
     * @param context               From which the action is done.
     * @param isSessionOnRecording  True if call is already being recorded of False otherwise.
     */
    @JvmStatic
    @JvmOverloads
    fun returnActiveCall(context: Context, isSessionOnRecording: Boolean? = false) {
        val currentCalls = getCallsParticipating()

        if (!currentCalls.isNullOrEmpty()) {
            for (chatIdCall in currentCalls) {
                val call = MegaApplication.getInstance().megaChatApi.getChatCall(chatIdCall)
                if (call != null) {
                    openMeetingInProgress(context, chatIdCall, false, isSessionOnRecording)
                    break
                }
            }
        }
    }

    /**
     * Opens the call that is in progress.
     *
     * @param context               From which the action is done.
     * @param chatId                Chat ID.
     */
    @JvmStatic
    fun returnCall(context: Context, chatId: Long) {
        val currentCalls = getCallsParticipating()
        if (currentCalls.isNullOrEmpty()) return

        for (chatIdCall in currentCalls) {
            if (chatIdCall == chatId) {
                openMeetingInProgress(context, chatId, false)
                return
            }
        }
    }

    /**
     * Method to know if I am participating in the call with another client
     *
     * @param call The MegaChatCall
     * @return True, if I am participating. False, if not
     */
    @JvmStatic
    fun CheckIfIAmParticipatingWithAnotherClient(call: MegaChatCall): Boolean {
        val listPeers = call.peeridParticipants
        if (listPeers != null && listPeers.size() > 0) {
            for (i in 0 until listPeers.size()) {
                if (listPeers[i] == MegaApplication.getInstance().megaApi.myUserHandleBinary) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Method to get the session of an individual call.
     *
     * @return The session.
     */
    @JvmStatic
    fun getSessionIndividualCall(callChat: MegaChatCall?): MegaChatSession? {
        if (callChat == null) return null

        return callChat.getMegaChatSession(callChat.sessionsClientid[0])
    }

    /**
     * Method for knowing if the session is on hold.
     *
     * @return True if it's on hold. False if it's not.
     */
    @JvmStatic
    fun isSessionOnHold(chatId: Long): Boolean {
        val chat = MegaApplication.getInstance().megaChatApi.getChatRoom(chatId)
        if (chat == null || chat.isGroup) return false

        val session = getSessionIndividualCall(
            MegaApplication.getInstance().megaChatApi.getChatCall(chatId)
        ) ?: return false

        return session.isOnHold
    }

    private fun createCallBanner(
        context: Context,
        chatId: Long,
        callInProgressLayout: RelativeLayout,
        callInProgressChrono: Chronometer?,
        callInProgressText: TextView,
    ) {
        val megaChatApi = MegaApplication.getInstance().megaChatApi

        val call = megaChatApi.getChatCall(chatId) ?: return

        callInProgressText.text = context.getString(R.string.call_in_progress_layout)
        callInProgressLayout.setBackgroundColor(
            ColorUtils.getThemeColor(context, com.google.android.material.R.attr.colorSecondary)
        )

        if (MegaApplication.getChatManagement().isRequestSent(call.callId)) {
            activateChrono(false, callInProgressChrono, null)
        } else {
            activateChrono(true, callInProgressChrono, call)
        }

        callInProgressLayout.visibility = View.VISIBLE

        if (context is ContactInfoActivity) {
            context.changeToolbarLayoutElevation()
        }
    }

    /**
     * Show or hide the "Tap to return to call" banner
     *
     * @param context              from which the action is done
     * @param callInProgressLayout RelativeLayout to be shown or hidden
     * @param callInProgressChrono Chronometer of the banner to be updated.
     * @param callInProgressText   Text of the banner to be updated
     */
    @JvmStatic
    fun showCallLayout(
        context: Context,
        callInProgressLayout: RelativeLayout?,
        callInProgressChrono: Chronometer?,
        callInProgressText: TextView,
    ) {
        if (callInProgressLayout == null) {
            return
        }

        val currentChatCallsList = getCallsParticipating()
        if (!participatingInACall() || currentChatCallsList == null || !isScreenInPortrait(context)) {
            hideCallInProgressLayout(context, callInProgressLayout, callInProgressChrono)
            return
        }

        val currentCallInProgress = getCallInProgress()
        if (currentCallInProgress != null) {
            createCallBanner(
                context,
                currentCallInProgress.chatid,
                callInProgressLayout,
                callInProgressChrono,
                callInProgressText,
            )
            return
        }

        val calls = getCallsParticipating()
        if (!calls.isNullOrEmpty()) {
            for (chatId in calls) {
                val call = MegaApplication.getInstance().megaChatApi.getChatCall(chatId)
                if (call != null && call.isOnHold) {
                    createCallBanner(
                        context,
                        chatId,
                        callInProgressLayout,
                        callInProgressChrono,
                        callInProgressText,
                    )
                    break
                }
            }
            return
        }

        hideCallInProgressLayout(context, callInProgressLayout, callInProgressChrono)
    }

    /**
     * This method is used to hide the current call banner.
     *
     * @param context              The Activity context.
     * @param callInProgressLayout RelativeLayout to be hidden
     * @param callInProgressChrono Chronometer of the banner.
     */
    private fun hideCallInProgressLayout(
        context: Context,
        callInProgressLayout: RelativeLayout,
        callInProgressChrono: Chronometer?,
    ) {
        callInProgressLayout.visibility = View.GONE
        activateChrono(false, callInProgressChrono, null)
        if (context is ContactInfoActivity) {
            context.changeToolbarLayoutElevation()
        }
    }

    private fun createCallMenuItem(
        call: MegaChatCall,
        returnCallMenuItem: MenuItem,
        layoutCallMenuItem: LinearLayout,
        chronometerMenuItem: Chronometer?,
    ) {
        val context = MegaApplication.getInstance().baseContext
        val callStatus = call.status
        layoutCallMenuItem.background =
            ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message)

        if (chronometerMenuItem == null) return

        if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS ||
            callStatus == MegaChatCall.CALL_STATUS_JOINING
        ) {
            if (chronometerMenuItem.visibility == View.VISIBLE) return
            chronometerMenuItem.visibility = View.VISIBLE
            chronometerMenuItem.base = SystemClock.elapsedRealtime() - call.duration * 1000
            chronometerMenuItem.start()
            chronometerMenuItem.format = " %s"
        } else {
            if (chronometerMenuItem.visibility == View.GONE) return
            chronometerMenuItem.stop()
            chronometerMenuItem.visibility = View.GONE
        }
        returnCallMenuItem.isVisible = true
    }

    /**
     * This method shows or hides the toolbar icon to return a call when a call is in progress
     * and it is in Cloud Drive section, Recents section, Incoming section, Outgoing section or in the chats list.
     *
     * @param returnCallMenuItem  The MenuItem.
     * @param layoutCallMenuItem  The layout of MenuItem.
     * @param chronometerMenuItem The chronometer.
     */
    @JvmStatic
    fun setCallMenuItem(
        returnCallMenuItem: MenuItem,
        layoutCallMenuItem: LinearLayout,
        chronometerMenuItem: Chronometer?,
    ) {
        val context = MegaApplication.getInstance().baseContext
        if (!isScreenInPortrait(context) && participatingInACall()) {
            val currentCall = getCallInProgress()
            if (currentCall != null) {
                createCallMenuItem(
                    currentCall,
                    returnCallMenuItem,
                    layoutCallMenuItem,
                    chronometerMenuItem,
                )
                return
            }

            val calls = getCallsParticipating()
            if (!calls.isNullOrEmpty()) {
                for (chatId in calls) {
                    val call = MegaApplication.getInstance().megaChatApi.getChatCall(chatId)
                    if (call != null && call.isOnHold) {
                        createCallMenuItem(
                            call,
                            returnCallMenuItem,
                            layoutCallMenuItem,
                            chronometerMenuItem,
                        )
                        break
                    }
                }
                return
            }
        }
        hideCallMenuItem(chronometerMenuItem, returnCallMenuItem)
    }

    /**
     * This method is used to hide the current call menu item.
     *
     * @param chronometerMenuItem Chronometer of the MenuItem.
     * @param returnCallMenuItem  MenuItem to be hidden.
     */
    @JvmStatic
    fun hideCallMenuItem(chronometerMenuItem: Chronometer?, returnCallMenuItem: MenuItem?) {
        chronometerMenuItem?.stop()
        returnCallMenuItem?.isVisible = false
    }

    /**
     * This method is used to hide the current call banner and update the toolbar elevation.
     *
     * @param context              The Activity context.
     * @param callInProgressChrono Chronometer of the banner.
     * @param callInProgressLayout RelativeLayout to be hidden.
     */
    @JvmStatic
    fun hideCallWidget(
        context: Context,
        callInProgressChrono: Chronometer?,
        callInProgressLayout: RelativeLayout?,
    ) {
        if (callInProgressChrono != null) {
            activateChrono(false, callInProgressChrono, null)
        }
        if (callInProgressLayout != null && callInProgressLayout.visibility == View.VISIBLE) {
            callInProgressLayout.visibility = View.GONE
            if (context is ContactInfoActivity) {
                context.changeToolbarLayoutElevation()
            }
        }
    }

    /**
     * Method to activate or deactivate the chronometer of a call without displaying the chronometer separator.
     *
     * @param activateChrono True, if it must be activated. False, if it must be deactivated
     * @param chronometer    The chronometer
     * @param call           The MegaChatCall
     */
    @JvmStatic
    fun activateChrono(activateChrono: Boolean, chronometer: Chronometer?, call: MegaChatCall?) {
        activateChrono(activateChrono, chronometer, call, false)
    }

    /**
     * Method to activate or deactivate the chronometer of a call.
     *
     * @param activateChrono                   True, if it must be activated. False, if it must be deactivated.
     * @param chronometer                      The chronometer
     * @param call                             The MegaChatCall
     * @param isNecessaryToShowChronoSeparator True, if the chronometer separator needs to be shown. False, otherwise
     */
    @JvmStatic
    fun activateChrono(
        activateChrono: Boolean,
        chronometer: Chronometer?,
        call: MegaChatCall?,
        isNecessaryToShowChronoSeparator: Boolean,
    ) {
        if (chronometer == null) return

        if (!activateChrono) {
            chronometer.stop()
            chronometer.visibility = View.GONE
            return
        }

        if (call != null) {
            chronometer.base = SystemClock.elapsedRealtime() - call.duration * 1000
            chronometer.start()
            chronometer.format = if (isNecessaryToShowChronoSeparator) "· %s" else " %s"
            chronometer.visibility = View.VISIBLE
        }
    }

    @JvmStatic
    fun milliSecondsToTimer(milliseconds: Long): String {
        val minutesString: String
        val secondsString: String
        var finalTime = ""
        val hours = (milliseconds / (1000 * 60 * 60)).toInt()
        val minutes = (milliseconds % (1000 * 60 * 60) / (1000 * 60)).toInt()
        val seconds = (milliseconds % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()

        minutesString = if (minutes < 10) "0$minutes" else "$minutes"
        secondsString = if (seconds < 10) "0$seconds" else "$seconds"
        if (hours > 0) {
            finalTime = if (hours < 10) "0$hours:" else "$hours:"
        }
        return "$finalTime$minutesString:$secondsString"
    }

    @JvmStatic
    fun callStatusToString(status: Int): String = when (status) {
        MegaChatCall.CALL_STATUS_INITIAL -> "CALL_STATUS_INITIAL"
        MegaChatCall.CALL_STATUS_USER_NO_PRESENT -> "CALL_STATUS_USER_NO_PRESENT"
        MegaChatCall.CALL_STATUS_CONNECTING -> "CALL_STATUS_CONNECTING"
        MegaChatCall.CALL_STATUS_JOINING -> "CALL_STATUS_JOINING"
        MegaChatCall.CALL_STATUS_IN_PROGRESS -> "CALL_STATUS_IN_PROGRESS"
        MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION -> "CALL_STATUS_TERMINATING_USER_PARTICIPATION"
        MegaChatCall.CALL_STATUS_DESTROYED -> "CALL_STATUS_DESTROYED"
        else -> status.toString()
    }

    @JvmStatic
    fun isStatusConnected(context: Context, chatId: Long): Boolean {
        val megaChatApi = MegaApplication.getInstance().megaChatApi
        return checkConnection(context) &&
                megaChatApi.connectionState == MegaChatApi.CONNECTED &&
                megaChatApi.getChatConnectionState(chatId) == MegaChatApi.CHAT_CONNECTION_ONLINE
    }

    @JvmStatic
    fun checkConnection(context: Context): Boolean {
        if (!isOnline(context)) {
            if (context is ContactInfoActivity) {
                context.showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    context.getString(R.string.error_server_connection_problem),
                    -1,
                )
            }
            return false
        }
        return true
    }

    /**
     * Enabling or disabling local video in a call
     *
     * @param isEnabled True, if video should be enabled. False, if video should be disabled.
     * @param chatId    Chat ID of the call
     * @param listener  MegaChatRequestListenerInterface
     */
    @JvmStatic
    fun enableOrDisableLocalVideo(
        isEnabled: Boolean,
        chatId: Long,
        listener: MegaChatRequestListenerInterface,
    ) {
        if (isEnabled) {
            MegaApplication.getInstance().megaChatApi.enableVideo(chatId, listener)
        } else {
            MegaApplication.getInstance().megaChatApi.disableVideo(chatId, listener)
        }
    }

    /**
     * Method to get the call in progress that is not on hold.
     *
     * @return MegaChatCall the call in progress
     */
    @JvmStatic
    fun getCallInProgress(): MegaChatCall? {
        val listCalls = getCallsParticipating()
        if (!listCalls.isNullOrEmpty()) {
            for (chatId in listCalls) {
                val call = MegaApplication.getInstance().megaChatApi.getChatCall(chatId)
                if (call != null && !call.isOnHold) {
                    return call
                }
            }
        }

        return null
    }

    @JvmStatic
    fun disableLocalCamera() {
        val call = getCallInProgress()
        if (call != null) {
            enableOrDisableLocalVideo(
                false,
                call.chatid,
                DisableAudioVideoCallListener(MegaApplication.getInstance()),
            )
        }
    }

    /**
     * This function determines whether there are ongoing video calls.
     *
     * @return Long. The chat ID.
     */
    @JvmStatic
    @Deprecated("Use AreThereOngoingVideoCallsUseCase instead.")
    fun isNecessaryDisableLocalCamera(): Long {
        val call = getCallInProgress()
        if (call == null || !call.hasLocalVideo()) {
            return MEGACHAT_INVALID_HANDLE
        }

        return call.chatid
    }

    /**
     * When there is a video call in progress with the video enabled of the current account logged-in,
     * alerts the user if they are sure they want to perform the action in which the camera is involved,
     * since their camera will be disabled in the call.
     *
     * @param activity   current Activity involved
     * @param action     the action to perform. These are the possibilities:
     *                   ACTION_TAKE_PICTURE, TAKE_PICTURE_PROFILE_CODE, ACTION_OPEN_QR
     * @param openScanQR if the action is ACTION_OPEN_QR, it specifies whether to open the "Scan QR" section.
     *                   True if it should open the "Scan QR" section, false otherwise.
     */
    @JvmStatic
    @Deprecated("Use OpenCameraConfirmationDialogRoute instead.")
    fun showConfirmationOpenCamera(activity: Activity, action: String, openScanQR: Boolean) {
        val dialogClickListener =
            DialogInterface.OnClickListener { _, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        Timber.d("Open camera and lost the camera in the call")
                        disableLocalCamera()
                        if (activity is AddContactActivity && action == Constants.ACTION_OPEN_QR) {
                            activity.initScanQR()
                        }
                    }

                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
            }
        val builder = MaterialAlertDialogBuilder(
            activity,
            R.style.ThemeOverlay_Mega_MaterialAlertDialog,
        )
        val message = activity.getString(R.string.confirmation_open_camera_on_chat)
        builder.setTitle(R.string.title_confirmation_open_camera_on_chat)
        builder.setMessage(message)
            .setPositiveButton(R.string.context_open_link, dialogClickListener)
            .setNegativeButton(
                mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button,
                dialogClickListener,
            )
            .show()
    }

    /**
     * Get default avatar call
     *
     * @param context
     * @param peerId
     * @param username
     */
    @JvmStatic
    fun getDefaultAvatarCall(context: Context, peerId: Long, username: String): Bitmap =
        AvatarUtil.getDefaultAvatar(
            colorAvatar = getColorAvatar(handle = peerId),
            textAvatar = username,
            textSize = dp2px(
                Constants.AVATAR_SIZE_CALLS.toFloat(),
                context.resources.displayMetrics
            ),
            isList = true,
        )

    /**
     * Method to get the image avatar in calls.
     *
     * @param peerId User handle from whom the avatar is obtained.
     * @return Bitmap with the image avatar created.
     */
    @JvmStatic
    fun getImageAvatarCall(peerId: Long): Bitmap? {
        val mail = getUserMailCall(peerId)
        val megaChatApi = MegaApplication.getInstance().megaChatApi

        val userHandleString = MegaApiAndroid.userHandleToBase64(peerId)
        val myUserHandleEncoded = MegaApiAndroid.userHandleToBase64(megaChatApi.myUserHandle)
        if (userHandleString == myUserHandleEncoded) {
            return getAvatarBitmap(mail)
        }

        return if (mail.isNullOrBlank()) {
            getAvatarBitmap(userHandleString)
        } else {
            getUserAvatar(userHandleString, mail)
        }
    }

    /**
     * Method to get the email from a handle.
     *
     * @param peerId User handle from whom the email is obtained.
     * @return The email.
     */
    @JvmStatic
    fun getUserMailCall(peerId: Long): String? {
        val megaChatApi = MegaApplication.getInstance().megaChatApi
        return if (peerId == megaChatApi.myUserHandle) {
            megaChatApi.myEmail
        } else {
            megaChatApi.getUserEmailFromCache(peerId)
        }
    }

    /**
     * Get user name call
     *
     * @param peerId
     * @param chatController
     * @param megaChatApi
     */
    @JvmStatic
    fun getUserNameCall(
        peerId: Long,
        chatController: ChatController,
        megaChatApi: MegaChatApiAndroid,
    ): String? {
        if (peerId == megaChatApi.myUserHandle) {
            return megaChatApi.myFullname
        }

        val nickname = getNicknameContact(peerId)
        if (nickname != null) {
            return nickname
        }

        return chatController.getParticipantFullName(peerId)
    }

    /**
     * Retrieve the calls I'm participating in.
     *
     * @return The list of chats IDs with call.
     */
    @JvmStatic
    fun getCallsParticipating(): ArrayList<Long>? {
        val listCalls = ArrayList<Long>()
        val megaChatApi = MegaApplication.getInstance().megaChatApi

        val listCallsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS)
        if (listCallsInProgress != null && listCallsInProgress.size() > 0) {
            for (i in 0 until listCallsInProgress.size()) {
                listCalls.add(listCallsInProgress[i])
            }
        }
        val listCallsJoining = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_JOINING)
        if (listCallsJoining != null && listCallsJoining.size() > 0) {
            for (i in 0 until listCallsJoining.size()) {
                listCalls.add(listCallsJoining[i])
            }
        }

        val listCallsInInitialState = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_INITIAL)
        if (listCallsInInitialState != null && listCallsInInitialState.size() > 0) {
            for (i in 0 until listCallsInInitialState.size()) {
                listCalls.add(listCallsInInitialState[i])
            }
        }
        val listCallsConnecting = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_CONNECTING)
        if (listCallsConnecting != null && listCallsConnecting.size() > 0) {
            for (i in 0 until listCallsConnecting.size()) {
                listCalls.add(listCallsConnecting[i])
            }
        }

        if (listCalls.isEmpty()) return null

        return listCalls
    }

    /**
     * Method to retrieve the chat ID with an active call.
     *
     * @param currentChatId The chat ID with call.
     */
    @JvmStatic
    fun isAnotherActiveCall(currentChatId: Long): Long {
        val chatsIDsWithCallActive = getCallsParticipating()
        if (chatsIDsWithCallActive.isNullOrEmpty()) {
            return currentChatId
        }

        val currentCall = MegaApplication.getInstance().megaChatApi.getChatCall(currentChatId)
        if (currentCall != null && currentCall.isOnHold) {
            Timber.d("Current call ON HOLD, look for other")
            for (anotherChatId in chatsIDsWithCallActive) {
                if (anotherChatId != currentChatId) {
                    val call = MegaApplication.getInstance().megaChatApi.getChatCall(anotherChatId)
                    if (call != null && !call.isOnHold) {
                        Timber.d("Another call ACTIVE")
                        return anotherChatId
                    }
                }
            }
        }
        Timber.d("Current call ACTIVE, look for other")
        return currentChatId
    }

    /**
     * Method to check if there is a call and that it is not on hold before answering it.
     *
     * @param currentChatId The current call.
     * @return The call in progress.
     */
    @JvmStatic
    fun existsAnotherCall(currentChatId: Long): Long {
        val chatsIDsWithCallActive = getCallsParticipating()
        if (chatsIDsWithCallActive.isNullOrEmpty()) {
            return currentChatId
        }
        for (anotherChatId in chatsIDsWithCallActive) {
            if (anotherChatId != currentChatId) {
                val call = MegaApplication.getInstance().megaChatApi.getChatCall(anotherChatId)
                if (call != null && !call.isOnHold) {
                    return anotherChatId
                }
            }
        }
        return currentChatId
    }

    @JvmStatic
    fun getPendingIntentMeetingInProgress(
        context: Context,
        chatIdCallToAnswer: Long,
        requestCode: Int,
        isGuest: Boolean,
    ): PendingIntent {
        val intentMeeting = Intent(context, MeetingActivity::class.java)
        intentMeeting.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intentMeeting.action = MeetingActivity.MEETING_ACTION_IN
        intentMeeting.putExtra(MeetingActivity.MEETING_CHAT_ID, chatIdCallToAnswer)
        intentMeeting.putExtra(MeetingActivity.MEETING_IS_GUEST, isGuest)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intentMeeting,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    @JvmStatic
    fun getPendingIntentMeetingRinging(
        context: Context,
        chatIdCallToAnswer: Long,
        requestCode: Int,
    ): PendingIntent {
        val intentMeeting = Intent(context.applicationContext, MeetingActivity::class.java)
        intentMeeting.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intentMeeting.action = MeetingActivity.MEETING_ACTION_RINGING
        intentMeeting.putExtra(MeetingActivity.MEETING_CHAT_ID, chatIdCallToAnswer)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intentMeeting,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Check Camera Permission
     *
     * @param activity Current activity
     * @return True, if granted. False, if not granted
     */
    @JvmStatic
    fun checkCameraPermission(activity: Activity?): Boolean {
        val hasCameraPermission = hasPermissions(
            MegaApplication.getInstance().baseContext,
            Manifest.permission.CAMERA,
        )
        if (!hasCameraPermission) {
            if (activity == null) return false

            requestPermission(activity, Constants.REQUEST_CAMERA, Manifest.permission.CAMERA)
            return false
        }

        return true
    }

    /**
     * Check Audio Permission
     *
     * @param activity Current activity
     * @return True, if granted. False, if not granted
     */
    @JvmStatic
    fun checkAudioPermission(activity: Activity?): Boolean {
        val hasRecordAudioPermission = hasPermissions(
            MegaApplication.getInstance().baseContext,
            Manifest.permission.RECORD_AUDIO,
        )
        if (!hasRecordAudioPermission) {
            if (activity == null) return false

            requestPermission(
                activity,
                Constants.REQUEST_RECORD_AUDIO,
                Manifest.permission.RECORD_AUDIO,
            )
            return false
        }

        return true
    }

    /**
     * Method for obtaining the necessary permissions in one call.
     *
     * @param activity Current activity
     * @return True, if you have both permits. False, otherwise.
     */
    @JvmStatic
    fun checkPermissionsCall(activity: Activity?): Boolean {
        if (!checkAudioPermission(activity)) {
            return false
        }

        return checkCameraPermission(activity)
    }

    @JvmStatic
    fun addChecksForACall(chatId: Long, speakerStatus: Boolean) {
        MegaApplication.getChatManagement().setSpeakerStatus(chatId, speakerStatus)
    }

    /**
     * Method for removing the incoming call notification.
     *
     * @param callIdIncomingCall The call ID
     */
    @JvmStatic
    fun clearIncomingCallNotification(callIdIncomingCall: Long) {
        Timber.d("Clear the notification in call: %s", callIdIncomingCall)
        if (callIdIncomingCall == MEGACHAT_INVALID_HANDLE) return

        try {
            val notificationManager = MegaApplication.getInstance()
                .baseContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(getCallNotificationId(callIdIncomingCall))
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * Method for getting the call notification ID.
     *
     * @param callId The call ID.
     * @return The notification ID.
     */
    @JvmStatic
    fun getCallNotificationId(callId: Long): Int {
        val notificationCallId = MegaApiAndroid.userHandleToBase64(callId)
        return notificationCallId.hashCode() + Constants.NOTIFICATION_CALL_IN_PROGRESS
    }

    /**
     * Method to check if the chat is online
     *
     * @param newState The state of chat
     * @param chatRoom The MegaChatRoom
     * @return True, if the chat is connected and a call can be started. False, otherwise
     */
    @JvmStatic
    fun isChatConnectedInOrderToInitiateACall(newState: Int, chatRoom: MegaChatRoom?): Boolean =
        newState == MegaChatApi.CHAT_CONNECTION_ONLINE &&
                chatRoom != null &&
                chatRoom.getPeerHandle(0) != MEGACHAT_INVALID_HANDLE &&
                chatRoom.getPeerHandle(0) == MegaApplication.userWaitingForCall

    /**
     * Method to display a dialogue informing the user that he/she cannot start or join a meeting while on a call in progress.
     *
     * @param context            Context of Activity
     * @param message            String with the text to show in the dialogue
     */
    @JvmStatic
    fun showConfirmationInACall(context: Context, message: String) {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setMessage(message)
            .setPositiveButton(
                mega.privacy.android.shared.resources.R.string.general_ok,
            ) { _, _ ->
                if (context is OpenLinkActivity) {
                    returnActiveCall(context)
                }
            }
            .show()
    }

    /**
     * Method to know if a meeting has ended use [IsMeetingEndUseCase]
     *
     * @param chatRequest [MegaChatRequest]
     * @return True, if the meeting is finished. False, if not.
     */
    @JvmStatic
    @Deprecated("Use IsMeetingEndUseCase instead")
    fun isMeetingEnded(chatRequest: MegaChatRequest): Boolean =
        !MegaChatApi.hasChatOptionEnabled(
            MegaChatApi.CHAT_OPTION_WAITING_ROOM,
            chatRequest.privilege,
        ) && (chatRequest.megaHandleList == null ||
                chatRequest.megaHandleList[0] == MEGACHAT_INVALID_HANDLE)

    /**
     * Method to know if I am participating in this meeting use [CheckInThisMeetingUseCase]
     *
     * @param chatId Chat ID of the meeting
     * @return True, f I am participating in this meeting. False, if not.
     */
    @JvmStatic
    @Deprecated("Use CheckInThisMeetingUseCase instead")
    fun amIParticipatingInThisMeeting(chatId: Long): Boolean {
        val call = MegaApplication.getInstance().megaChatApi.getChatCall(chatId)
        return call != null &&
                call.status != MegaChatCall.CALL_STATUS_DESTROYED &&
                call.status != MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION &&
                call.status != MegaChatCall.CALL_STATUS_USER_NO_PRESENT
    }

    @JvmStatic
    fun joinMeetingOrReturnCall(
        context: Context,
        chatId: Long,
        link: String?,
        titleChat: String?,
        alreadyExist: Boolean,
        publicChatHandle: Long,
        isWaitingRoom: Boolean,
    ) {
        val call = MegaApplication.getInstance().megaChatApi.getChatCall(chatId)
        if (call == null ||
            call.status == MegaChatCall.CALL_STATUS_USER_NO_PRESENT ||
            call.status == MegaChatCall.CALL_STATUS_WAITING_ROOM
        ) {
            Timber.d("Call id: %d. It's a meeting, open to join", chatId)
            openMeetingToJoin(
                context,
                chatId,
                titleChat,
                link,
                if (alreadyExist) publicChatHandle else MEGACHAT_INVALID_HANDLE,
                alreadyExist,
                isWaitingRoom,
            )
        } else {
            Timber.d("Call id: %d. Return to call", chatId)
            returnCall(context, chatId)
        }
    }

    /**
     * Method that performs the necessary actions when there is an outgoing call or incoming call.
     *
     * @param chatId           Chat ID
     * @param callId           Call ID
     * @param typeAudioManager audio Manager type
     */
    @JvmStatic
    fun ongoingCall(
        rtcAudioManagerGateway: RTCAudioManagerGateway,
        chatId: Long,
        callId: Long,
        typeAudioManager: Int,
    ) {
        val rtcAudioManager = rtcAudioManagerGateway.audioManager
        if (rtcAudioManager != null && rtcAudioManager.typeAudioManager == typeAudioManager) return

        val chatRoom = MegaApplication.getInstance().megaChatApi.getChatRoom(chatId)
        if (chatRoom == null) {
            Timber.e("The chat does not exist")
            return
        }

        Timber.d("Controlling outgoing/in progress call")
        var resolvedTypeAudioManager = typeAudioManager
        if (resolvedTypeAudioManager == Constants.AUDIO_MANAGER_CALL_OUTGOING &&
            (chatRoom.isMeeting || chatRoom.isGroup)
        ) {
            clearIncomingCallNotification(callId)
            resolvedTypeAudioManager = Constants.AUDIO_MANAGER_CALL_IN_PROGRESS
        }

        MegaApplication.getInstance().createOrUpdateAudioManager(
            MegaApplication.getChatManagement().getSpeakerStatus(chatId),
            resolvedTypeAudioManager,
        )
    }

    /**
     * Check if an incoming call is a one-to-one call
     *
     * @param chatRoom MegaChatRoom of the call
     * @return True, if it is a one-to-one call. False, if it is a group call or meeting
     */
    @JvmStatic
    fun isOneToOneCall(chatRoom: MegaChatRoom): Boolean = !chatRoom.isGroup && !chatRoom.isMeeting

    /**
     * Get incoming call notification title
     *
     * @param chatRoom MegaChatRoom of the call
     * @return Notification title
     */
    @JvmStatic
    fun getIncomingCallNotificationTitle(chatRoom: MegaChatRoom, context: Context): String =
        context.getString(
            if (isOneToOneCall(chatRoom)) {
                R.string.title_notification_incoming_individual_audio_call
            } else {
                R.string.title_notification_incoming_group_call
            }
        )

    /**
     * Method to create collapsed or expanded remote views for a customised incoming call notification.
     *
     * @param layoutId     ID of layout
     * @param chatToAnswer MegaChatRoom of the call
     * @param avatarIcon   Bitmap with the chat Avatar
     * @return The RemoteViews created
     */
    @JvmStatic
    fun collapsedAndExpandedIncomingCallNotification(
        context: Context,
        layoutId: Int,
        chatToAnswer: MegaChatRoom,
        avatarIcon: Bitmap?,
    ): RemoteViews {
        val statusIcon: Bitmap? = if (isOneToOneCall(chatToAnswer)) {
            getStatusBitmap(
                MegaApplication.getInstance().megaChatApi
                    .getUserOnlineStatus(chatToAnswer.getPeerHandle(0))
            )
        } else {
            null
        }
        val titleChat = getTitleChat(chatToAnswer)
        val titleCall = getIncomingCallNotificationTitle(chatToAnswer, context)

        val views = RemoteViews(context.packageName, layoutId)
        views.setTextViewText(R.id.chat_title, titleChat)
        views.setTextViewText(R.id.call_title, titleCall)

        if (avatarIcon == null) {
            views.setViewVisibility(R.id.avatar_layout, View.GONE)
        } else {
            views.setImageViewBitmap(R.id.avatar_image, avatarIcon)
            views.setViewVisibility(R.id.avatar_layout, View.VISIBLE)
        }

        if (statusIcon != null) {
            views.setImageViewBitmap(R.id.chat_status, statusIcon)
            views.setViewVisibility(R.id.chat_status, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.chat_status, View.GONE)
        }

        return views
    }

    /**
     * Method to control when an attempt is made to initiate a call from a contact option
     *
     * @param context            The Activity context
     * @return True, if the call can be started. False, otherwise.
     */
    @JvmStatic
    fun canCallBeStartedFromContactOption(context: Activity): Boolean {
        if (getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return false
        }

        if (participatingInACall()) {
            showConfirmationInACall(context, context.getString(R.string.ongoing_call_content))
            return false
        }

        return checkPermissionsCall(context)
    }

    /**
     * Method to find out if device's notification settings are enabled
     *
     * @return True, if they are enabled. False, if they are not.
     */
    @JvmStatic
    fun areNotificationsSettingsEnabled(): Boolean {
        val notificationManager = MegaApplication.getInstance().applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.areNotificationsEnabled()
    }
}
