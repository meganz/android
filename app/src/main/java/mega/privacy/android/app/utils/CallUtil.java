package mega.privacy.android.app.utils;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.view.View.GONE;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_ACTION_GUEST;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_ACTION_IN;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_ACTION_JOIN;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_ACTION_RINGING;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_AUDIO_ENABLE;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_CHAT_ID;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_IS_GUEST;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_NAME;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_PUBLIC_CHAT_HANDLE;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_VIDEO_ENABLE;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.AvatarUtil.getAvatarBitmap;
import static mega.privacy.android.app.utils.AvatarUtil.getColorAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getUserAvatar;
import static mega.privacy.android.app.utils.ChatUtil.getStatusBitmap;
import static mega.privacy.android.app.utils.ChatUtil.getTitleChat;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_QR;
import static mega.privacy.android.app.utils.Constants.ACTION_TAKE_PICTURE;
import static mega.privacy.android.app.utils.Constants.ACTION_TAKE_PROFILE_PICTURE;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CALL_IN_PROGRESS;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CALL_OUTGOING;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE_CALLS;
import static mega.privacy.android.app.utils.Constants.INVALID_TYPE_PERMISSIONS;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CALL_IN_PROGRESS;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA;
import static mega.privacy.android.app.utils.Constants.REQUEST_RECORD_AUDIO;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.TAKE_PHOTO_CODE;
import static mega.privacy.android.app.utils.Constants.TAKE_PICTURE_PROFILE_CODE;
import static mega.privacy.android.app.utils.ContactUtil.getNicknameContact;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static mega.privacy.android.app.utils.Util.takePicture;
import static mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions;
import static mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaChatCall.CALL_STATUS_USER_NO_PRESENT;
import static nz.mega.sdk.MegaChatCall.CALL_STATUS_WAITING_ROOM;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.legacycontact.AddContactActivity;
import mega.privacy.android.app.main.megachat.AppRTCAudioManager;
import mega.privacy.android.app.meeting.activity.MeetingActivity;
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway;
import mega.privacy.android.app.meeting.listeners.DisableAudioVideoCallListener;
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity;
import mega.privacy.android.app.presentation.extensions.StorageStateExtensionsKt;
import mega.privacy.android.app.presentation.meeting.WaitingRoomActivity;
import mega.privacy.android.app.presentation.openlink.OpenLinkActivity;
import mega.privacy.android.domain.entity.StorageState;
import mega.privacy.android.domain.usecase.call.AreThereOngoingVideoCallsUseCase;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatSession;
import nz.mega.sdk.MegaHandleList;
import timber.log.Timber;

public class CallUtil {

    /**
     * Method for opening the Meeting Activity when the meeting is outgoing or in progress call
     *
     * @param context            Context
     * @param chatId             chat ID
     * @param meetingName        Meeting Name
     * @param link               Meeting's link
     */
    public static void openMeetingToJoin(Context context, long chatId, String meetingName, String link, long publicChatHandle, boolean isRejoin, boolean isWaitingRoom) {
        Timber.d("Open join a meeting screen:: chatId = %s", chatId);
        MegaApplication.getChatManagement().setOpeningMeetingLink(chatId, true);
        Intent intent;
        if (isWaitingRoom) {
            intent = new Intent(context, WaitingRoomActivity.class);
            intent.putExtra(WaitingRoomActivity.EXTRA_CHAT_ID, chatId);
            intent.putExtra(WaitingRoomActivity.EXTRA_CHAT_LINK, link);
        } else {
            intent = new Intent(context, MeetingActivity.class);
            if (isRejoin) {
                intent.setAction(MEETING_ACTION_JOIN);
                intent.putExtra(MEETING_PUBLIC_CHAT_HANDLE, publicChatHandle);
            } else {
                intent.setAction(MEETING_ACTION_JOIN);
            }
            intent.putExtra(MEETING_CHAT_ID, chatId);
            intent.putExtra(MEETING_NAME, meetingName);
            intent.setData(Uri.parse(link));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Method for opening the Meeting Activity when the meeting is outgoing or in progress call
     *
     * @param context            Context
     * @param chatId             chat ID
     */
    public static void openMeetingRinging(Context context, long chatId) {
        Timber.d("Open incoming call screen. Chat id is %s", chatId);
        MegaApplication.getInstance().openCallService(chatId);
        Intent meetingIntent = new Intent(context, MeetingActivity.class);
        meetingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        meetingIntent.setAction(MEETING_ACTION_RINGING);
        meetingIntent.putExtra(MEETING_CHAT_ID, chatId);
        context.startActivity(meetingIntent);
    }

    /**
     * Method for opening the Meeting Activity when the meeting is outgoing or in progress call
     *
     * @param context               Context
     * @param chatId                Chat ID
     * @param isSessionOnRecording  True if call is already being recorded of False otherwise.
     */
    public static void openMeetingInProgress(Context context, long chatId, boolean isNewTask, Boolean isSessionOnRecording) {
        Timber.d("Open in progress call screen. Chat id is %s", chatId);
        if (isNewTask) {
            MegaApplication.getInstance().openCallService(chatId);
        }

        Intent meetingIntent = new Intent(context, MeetingActivity.class);
        meetingIntent.setAction(MEETING_ACTION_IN);
        meetingIntent.putExtra(MEETING_CHAT_ID, chatId);
        meetingIntent.putExtra(MEETING_IS_GUEST, MegaApplication.getInstance().getMegaApi().isEphemeralPlusPlus());
        if (isNewTask) {
            Timber.d("New task");
            meetingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            meetingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        context.startActivity(meetingIntent);
    }

    /**
     * Method for opening the Meeting Activity when the meeting is outgoing or in progress call
     *
     * @param context               Context
     * @param chatId                Chat ID
     */
    public static void openMeetingInProgress(Context context, long chatId, boolean isNewTask) {
        openMeetingInProgress(context, chatId, isNewTask, false);
    }

    /**
     * Method for opening the Meeting Activity when the meeting is outgoing or in progress call with audio or video enable.
     *
     * @param context            Context
     * @param chatId             chat ID
     * @param isAudioEnable      it the audio is ON
     * @param isVideoEnable      it the video is ON
     */
    public static void openMeetingWithAudioOrVideo(Context context, long chatId, boolean isAudioEnable, boolean isVideoEnable) {
        Timber.d("Open call with audio or video. Chat id is %s", chatId);
        MegaApplication.getInstance().openCallService(chatId);
        Intent meetingIntent = new Intent(context, MeetingActivity.class);
        meetingIntent.setAction(MEETING_ACTION_IN);
        meetingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        meetingIntent.putExtra(MEETING_CHAT_ID, chatId);
        meetingIntent.putExtra(MEETING_AUDIO_ENABLE, isAudioEnable);
        meetingIntent.putExtra(MEETING_VIDEO_ENABLE, isVideoEnable);
        context.startActivity(meetingIntent);
    }

    /**
     * Method for opening the Meeting Activity in guest mode
     *
     * @param context            Context
     * @param meetingName        Meeting Name
     * @param chatId             chat ID
     * @param link               Meeting's link
     */
    public static void openMeetingGuestMode(Context context, String meetingName, long chatId, String link, MegaChatRequestHandler chatRequestHandler, boolean isWaitingRoom) {
        Timber.d("Open meeting in guest mode. Chat id is %s", chatId);
        MegaApplication.getChatManagement().setOpeningMeetingLink(chatId, true);
        chatRequestHandler.setIsLoggingRunning(true);
        Intent intent;
        if (isWaitingRoom) {
            intent = new Intent(context, WaitingRoomActivity.class);
            intent.putExtra(WaitingRoomActivity.EXTRA_CHAT_ID, chatId);
            intent.putExtra(WaitingRoomActivity.EXTRA_CHAT_LINK, link);
        } else {
            intent = new Intent(context, MeetingActivity.class);
            intent.setAction(MEETING_ACTION_GUEST);
            if (!isTextEmpty(meetingName)) {
                intent.putExtra(MEETING_NAME, meetingName);
            }
            intent.putExtra(MEETING_CHAT_ID, chatId);
            intent.putExtra(MEETING_IS_GUEST, true);
            intent.setData(Uri.parse(link));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Retrieve if there's a call in progress that you're participating in. use [IsParticipatingInChatCallUseCase] instead
     *
     * @return True if you're on a call in progress. Otherwise false.
     */
    @Deprecated
    public static boolean participatingInACall() {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MegaHandleList listCallsInitial = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_INITIAL);
        MegaHandleList listCallsConnecting = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_CONNECTING);
        MegaHandleList listCallsJoining = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_JOINING);
        MegaHandleList listCallsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS);

        return listCallsInitial.size() > 0 || listCallsConnecting.size() > 0 || listCallsJoining.size() > 0 || listCallsInProgress.size() > 0;
    }

    /**
     * Retrieve if there's a call in progress that you're participating in or a incoming call.
     *
     * @return True if you're on a call in progress o exists a incoming call. Otherwise false.
     */
    public static boolean existsAnOngoingOrIncomingCall() {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MegaHandleList listCallsUserNoPresent = megaChatApi.getChatCalls(CALL_STATUS_USER_NO_PRESENT);
        MegaHandleList listCallsUserTerminatingUserParticipation = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION);
        MegaHandleList listCallsDestroy = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_DESTROYED);
        MegaHandleList listCalls = megaChatApi.getChatCalls();

        if ((listCalls.size() - listCallsDestroy.size()) == 0) {
            Timber.d("No calls in progress");
            return false;
        }

        if ((listCalls.size() - listCallsDestroy.size()) == (listCallsUserNoPresent.size() + listCallsUserTerminatingUserParticipation.size())) {
            Timber.d("I'm not participating in any of the calls there");
            return false;
        }

        return true;
    }

    /**
     * Retrieve the id of a chat that has a call in progress different that current one
     * replace by GetAnotherCallParticipatingUseCase
     *
     * @param currentChatId the chat ID of the current call
     * @return A long data type. It's the id of chat
     */
    @Deprecated
    public static long getAnotherCallParticipating(Long currentChatId) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MegaHandleList listCallsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS);
        if (listCallsInProgress != null && listCallsInProgress.size() > 0) {
            for (int i = 0; i < listCallsInProgress.size(); i++) {
                if (listCallsInProgress.get(i) != currentChatId) {
                    return listCallsInProgress.get(i);
                }
            }
        }

        MegaHandleList listCallsJoining = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_JOINING);
        if (listCallsJoining != null && listCallsJoining.size() > 0) {
            for (int i = 0; i < listCallsJoining.size(); i++) {
                if (listCallsJoining.get(i) != currentChatId) {
                    return listCallsJoining.get(i);
                }
            }
        }

        MegaHandleList listCallsConnecting = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_CONNECTING);
        if (listCallsConnecting != null && listCallsConnecting.size() > 0) {
            for (int i = 0; i < listCallsConnecting.size(); i++) {
                if (listCallsConnecting.get(i) != currentChatId) {
                    return listCallsConnecting.get(i);
                }
            }
        }

        return MEGACHAT_INVALID_HANDLE;
    }

    /**
     * Opens the call that is in progress.
     *
     * @param context               From which the action is done.
     * @param isSessionOnRecording  True if call is already being recorded of False otherwise.
     */
    public static void returnActiveCall(Context context, Boolean isSessionOnRecording) {
        ArrayList<Long> currentCalls = getCallsParticipating();

        if (currentCalls != null && !currentCalls.isEmpty()) {
            for (Long chatIdCall : currentCalls) {
                MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(chatIdCall);
                if (call != null) {
                    openMeetingInProgress(context, chatIdCall, false, isSessionOnRecording);
                    break;
                }
            }
        }
    }

    /**
     * Opens the call that is in progress.
     *
     * @param context               From which the action is done.
     */
    public static void returnActiveCall(Context context) {
        returnActiveCall(context, false);
    }

    /**
     * Opens the call that is in progress.
     *
     * @param context               From which the action is done.
     * @param chatId                Chat ID.
     */
    public static void returnCall(Context context, long chatId) {
        ArrayList<Long> currentCalls = getCallsParticipating();
        if (currentCalls == null || currentCalls.isEmpty())
            return;

        for (Long chatIdCall : currentCalls) {
            if (chatIdCall == chatId) {
                openMeetingInProgress(context, chatId, false);
                return;
            }
        }
    }

    /**
     * Method to know if I am participating in the call with another client
     *
     * @param call The MegaChatCall
     * @return True, if I am participating. False, if not
     */
    public static boolean CheckIfIAmParticipatingWithAnotherClient(MegaChatCall call) {
        MegaHandleList listPeers = call.getPeeridParticipants();
        if (listPeers != null && listPeers.size() > 0) {
            for (int i = 0; i < listPeers.size(); i++) {
                if (listPeers.get(i) == MegaApplication.getInstance().getMegaApi().getMyUserHandleBinary())
                    return true;
            }
        }

        return false;
    }

    /**
     * Method to get the session of an individual call.
     *
     * @return The session.
     */
    public static MegaChatSession getSessionIndividualCall(MegaChatCall callChat) {
        if (callChat == null)
            return null;

        return callChat.getMegaChatSession(callChat.getSessionsClientid().get(0));
    }

    /**
     * Method for knowing if the session is on hold.
     *
     * @return True if it's on hold. False if it's not.
     */
    public static boolean isSessionOnHold(long chatId) {
        MegaChatRoom chat = MegaApplication.getInstance().getMegaChatApi().getChatRoom(chatId);
        if (chat == null || chat.isGroup())
            return false;

        MegaChatSession session = getSessionIndividualCall(MegaApplication.getInstance().getMegaChatApi().getChatCall(chatId));
        if (session == null)
            return false;

        return session.isOnHold();
    }

    private static void createCallBanner(Context context, long chatId, final RelativeLayout callInProgressLayout, final Chronometer callInProgressChrono, final TextView callInProgressText) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        MegaChatCall call = megaChatApi.getChatCall(chatId);
        if (call == null)
            return;

        callInProgressText.setText(context.getString(R.string.call_in_progress_layout));
        callInProgressLayout.setBackgroundColor(ColorUtils.getThemeColor(context, com.google.android.material.R.attr.colorSecondary));

        if (MegaApplication.getChatManagement().isRequestSent(call.getCallId())) {
            activateChrono(false, callInProgressChrono, null);
        } else {
            activateChrono(true, callInProgressChrono, call);
        }

        callInProgressLayout.setVisibility(View.VISIBLE);

        if (context instanceof ManagerActivity) {
            ((ManagerActivity) context).changeAppBarElevation(true,
                    ManagerActivity.ELEVATION_CALL_IN_PROGRESS);
        }
        if (context instanceof ContactInfoActivity) {
            ((ContactInfoActivity) context).changeToolbarLayoutElevation();
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
    public static void showCallLayout(Context context, final RelativeLayout callInProgressLayout, final Chronometer callInProgressChrono, final TextView callInProgressText) {
        if (callInProgressLayout == null) {
            return;
        }

        ArrayList<Long> currentChatCallsList = getCallsParticipating();
        if (!participatingInACall() || currentChatCallsList == null || !isScreenInPortrait(context)) {
            hideCallInProgressLayout(context, callInProgressLayout, callInProgressChrono);
            return;
        }

        MegaChatCall currentCallInProgress = getCallInProgress();
        if (currentCallInProgress != null) {
            createCallBanner(context, currentCallInProgress.getChatid(), callInProgressLayout, callInProgressChrono, callInProgressText);
            return;
        }

        ArrayList<Long> calls = getCallsParticipating();
        if (calls != null && !calls.isEmpty()) {
            for (long chatId : calls) {
                MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(chatId);
                if (call != null && call.isOnHold()) {
                    createCallBanner(context, chatId, callInProgressLayout, callInProgressChrono, callInProgressText);
                    break;
                }
            }
            return;
        }

        hideCallInProgressLayout(context, callInProgressLayout, callInProgressChrono);
    }

    /**
     * This method is used to hide the current call banner.
     *
     * @param context              The Activity context.
     * @param callInProgressLayout RelativeLayout to be hidden
     * @param callInProgressChrono Chronometer of the banner.
     */
    private static void hideCallInProgressLayout(Context context, final RelativeLayout callInProgressLayout, final Chronometer callInProgressChrono) {
        callInProgressLayout.setVisibility(View.GONE);
        activateChrono(false, callInProgressChrono, null);
        if (context instanceof ManagerActivity) {
            ((ManagerActivity) context).changeAppBarElevation(false,
                    ManagerActivity.ELEVATION_CALL_IN_PROGRESS);
        }
        if (context instanceof ContactInfoActivity) {
            ((ContactInfoActivity) context).changeToolbarLayoutElevation();
        }
    }

    private static void createCallMenuItem(MegaChatCall call, final MenuItem returnCallMenuItem, final LinearLayout layoutCallMenuItem, final Chronometer chronometerMenuItem) {
        Context context = MegaApplication.getInstance().getBaseContext();
        int callStatus = call.getStatus();
        layoutCallMenuItem.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));

        if (chronometerMenuItem == null)
            return;

        if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_JOINING) {
            if (chronometerMenuItem.getVisibility() == View.VISIBLE) return;
            chronometerMenuItem.setVisibility(View.VISIBLE);
            chronometerMenuItem.setBase(SystemClock.elapsedRealtime() - (call.getDuration() * 1000));
            chronometerMenuItem.start();
            chronometerMenuItem.setFormat(" %s");
        } else {
            if (chronometerMenuItem.getVisibility() == View.GONE) return;
            chronometerMenuItem.stop();
            chronometerMenuItem.setVisibility(View.GONE);
        }
        returnCallMenuItem.setVisible(true);
    }

    /**
     * This method shows or hides the toolbar icon to return a call when a call is in progress
     * and it is in Cloud Drive section, Recents section, Incoming section, Outgoing section or in the chats list.
     *
     * @param returnCallMenuItem  The MenuItem.
     * @param layoutCallMenuItem  The layout of MenuItem.
     * @param chronometerMenuItem The chronometer.
     */
    public static void setCallMenuItem(final MenuItem returnCallMenuItem, final LinearLayout layoutCallMenuItem, final Chronometer chronometerMenuItem) {
        Context context = MegaApplication.getInstance().getBaseContext();
        if (!isScreenInPortrait(context) && participatingInACall()) {
            MegaChatCall currentCall = getCallInProgress();
            if (currentCall != null) {
                createCallMenuItem(currentCall, returnCallMenuItem, layoutCallMenuItem, chronometerMenuItem);
                return;
            }

            ArrayList<Long> calls = getCallsParticipating();
            if (calls != null && !calls.isEmpty()) {
                for (long chatId : calls) {
                    MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(chatId);
                    if (call != null && call.isOnHold()) {
                        createCallMenuItem(call, returnCallMenuItem, layoutCallMenuItem, chronometerMenuItem);
                        break;
                    }
                }
                return;
            }

        }
        hideCallMenuItem(chronometerMenuItem, returnCallMenuItem);
    }

    /**
     * This method is used to hide the current call menu item.
     *
     * @param chronometerMenuItem Chronometer of the MenuItem.
     * @param returnCallMenuItem  MenuItem to be hidden.
     */
    public static void hideCallMenuItem(final Chronometer chronometerMenuItem, final MenuItem returnCallMenuItem) {
        if (chronometerMenuItem != null) {
            chronometerMenuItem.stop();
        }
        if (returnCallMenuItem != null) {
            returnCallMenuItem.setVisible(false);
        }
    }

    /**
     * This method is used to hide the current call banner and update the toolbar elevation.
     *
     * @param context              The Activity context.
     * @param callInProgressChrono Chronometer of the banner.
     * @param callInProgressLayout RelativeLayout to be hidden.
     */
    public static void hideCallWidget(Context context, final Chronometer callInProgressChrono, final RelativeLayout callInProgressLayout) {
        if (callInProgressChrono != null) {
            activateChrono(false, callInProgressChrono, null);
        }
        if (callInProgressLayout != null && callInProgressLayout.getVisibility() == View.VISIBLE) {
            callInProgressLayout.setVisibility(View.GONE);
            if (context instanceof ManagerActivity) {
                ((ManagerActivity) context).changeAppBarElevation(false,
                        ManagerActivity.ELEVATION_CALL_IN_PROGRESS);
            }
            if (context instanceof ContactInfoActivity) {
                ((ContactInfoActivity) context).changeToolbarLayoutElevation();
            }
        }
    }

    /**
     * Know if a call in a specific chat is established.
     *
     * @param chatId Id of a chat room that has a call.
     * @return True if the call is established. Otherwise false.
     */
    public static boolean isEstablishedCall(long chatId) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        if (megaChatApi.getChatCall(chatId) == null) return false;

        return megaChatApi.getChatCall(chatId).getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS;
    }

    /**
     * Method to activate or deactivate the chronometer of a call without displaying the chronometer separator.
     *
     * @param activateChrono True, if it must be activated. False, if it must be deactivated
     * @param chronometer    The chronometer
     * @param call           The MegaChatCall
     */
    public static void activateChrono(boolean activateChrono, final Chronometer chronometer, MegaChatCall call) {
        activateChrono(activateChrono, chronometer, call, false);
    }

    /**
     * Method to activate or deactivate the chronometer of a call.
     *
     * @param activateChrono                   True, if it must be activated. False, if it must be deactivated.
     * @param chronometer                      The chronometer
     * @param call                             The MegaChatCall
     * @param isNecessaryToShowChronoSeparator True, if the chronometer separator needs to be shown. False, otherwise
     */
    public static void activateChrono(boolean activateChrono, final Chronometer chronometer, MegaChatCall call, boolean isNecessaryToShowChronoSeparator) {
        if (chronometer == null)
            return;

        if (!activateChrono) {
            chronometer.stop();
            chronometer.setVisibility(View.GONE);
            return;
        }

        if (call != null) {
            chronometer.setBase(SystemClock.elapsedRealtime() - (call.getDuration() * 1000));
            chronometer.start();
            chronometer.setFormat(isNecessaryToShowChronoSeparator ? "· %s" : " %s");
            chronometer.setVisibility(View.VISIBLE);
        }
    }

    public static String milliSecondsToTimer(long milliseconds) {
        String minutesString;
        String secondsString;
        String finalTime = "";
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        if (minutes < 10) {
            minutesString = "0" + minutes;
        } else {
            minutesString = "" + minutes;
        }
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }
        if (hours > 0) {
            if (hours < 10) {
                finalTime = "0" + hours + ":";
            } else {
                finalTime = "" + hours + ":";
            }
        }
        return finalTime + minutesString + ":" + secondsString;
    }

    public static String callStatusToString(int status) {
        switch (status) {
            case MegaChatCall.CALL_STATUS_INITIAL:
                return "CALL_STATUS_INITIAL";
            case CALL_STATUS_USER_NO_PRESENT:
                return "CALL_STATUS_USER_NO_PRESENT";
            case MegaChatCall.CALL_STATUS_CONNECTING:
                return "CALL_STATUS_CONNECTING";
            case MegaChatCall.CALL_STATUS_JOINING:
                return "CALL_STATUS_JOINING";
            case MegaChatCall.CALL_STATUS_IN_PROGRESS:
                return "CALL_STATUS_IN_PROGRESS";
            case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
                return "CALL_STATUS_TERMINATING_USER_PARTICIPATION";
            case MegaChatCall.CALL_STATUS_DESTROYED:
                return "CALL_STATUS_DESTROYED";
            default:
                return String.valueOf(status);
        }
    }

    public static boolean isStatusConnected(Context context, long chatId) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        return checkConnection(context) && megaChatApi.getConnectionState() == MegaChatApi.CONNECTED && megaChatApi.getChatConnectionState(chatId) == MegaChatApi.CHAT_CONNECTION_ONLINE;
    }

    public static boolean checkConnection(Context context) {
        if (!isOnline(context)) {
            if (context instanceof ContactInfoActivity) {
                ((ContactInfoActivity) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            }
            return false;
        }
        return true;
    }

    /**
     * Enabling or disabling local video in a call
     *
     * @param isEnabled True, if video should be enabled. False, if video should be disabled.
     * @param chatId    Chat ID of the call
     * @param listener  MegaChatRequestListenerInterface
     */
    public static void enableOrDisableLocalVideo(boolean isEnabled, long chatId, MegaChatRequestListenerInterface listener) {
        if (isEnabled) {
            MegaApplication.getInstance().getMegaChatApi().enableVideo(chatId, listener);
        } else {
            MegaApplication.getInstance().getMegaChatApi().disableVideo(chatId, listener);
        }
    }

    /**
     * Method to get the call in progress that is not on hold.
     *
     * @return MegaChatCall the call in progress
     */
    public static MegaChatCall getCallInProgress() {
        ArrayList<Long> listCalls = CallUtil.getCallsParticipating();
        if (listCalls != null && listCalls.size() > 0) {
            for (Long chatId : listCalls) {
                MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(chatId);
                if (call != null && !call.isOnHold()) {
                    return call;
                }
            }
        }

        return null;
    }

    public static void disableLocalCamera() {
        MegaChatCall call = getCallInProgress();
        if (call != null) {
            enableOrDisableLocalVideo(false, call.getChatid(), new DisableAudioVideoCallListener(MegaApplication.getInstance()));
        }
    }

    /**
     * This function determines whether there are ongoing video calls.
     *
     * @return Long. The chat ID.
     * @deprecated <p> Use {@link AreThereOngoingVideoCallsUseCase} instead.
     */
    @Deprecated
    public static long isNecessaryDisableLocalCamera() {
        MegaChatCall call = getCallInProgress();
        if (call == null || !call.hasLocalVideo()) {
            return MEGACHAT_INVALID_HANDLE;
        }

        return call.getChatid();
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
     * @deprecated <p> Use OpenCameraConfirmationDialogRoute instead.
     */
    @Deprecated
    public static void showConfirmationOpenCamera(Activity activity, String action, boolean openScanQR) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    Timber.d("Open camera and lost the camera in the call");
                    disableLocalCamera();
                    if (activity instanceof ManagerActivity) {
                        switch (action) {
                            case ACTION_OPEN_QR:
                                ((ManagerActivity) activity).openQR(openScanQR);
                                break;
                            case ACTION_TAKE_PICTURE:
                                takePicture(activity, TAKE_PHOTO_CODE);
                                break;
                            case ACTION_TAKE_PROFILE_PICTURE:
                                takePicture(activity, TAKE_PICTURE_PROFILE_CODE);
                                break;
                        }
                    }
                    if (activity instanceof AddContactActivity && action.equals(ACTION_OPEN_QR)) {
                        ((AddContactActivity) activity).initScanQR();
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
        String message = activity.getString(R.string.confirmation_open_camera_on_chat);
        builder.setTitle(R.string.title_confirmation_open_camera_on_chat);
        builder.setMessage(message).setPositiveButton(R.string.context_open_link, dialogClickListener)
                .setNegativeButton(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button, dialogClickListener).show();
    }

    /**
     * Method to get the default avatar in calls.
     *
     * @param context Context of the Activity.
     * @param peerId  User handle from whom the avatar is obtained.
     * @return Bitmap with the default avatar created.
     */
    public static Bitmap getDefaultAvatarCall(Context context, long peerId) {
        return AvatarUtil.getDefaultAvatar(getColorAvatar(peerId), getUserNameCall(context, peerId),
                dp2px(AVATAR_SIZE_CALLS, context.getResources().getDisplayMetrics()), true);
    }

    /**
     * Method to get the image avatar in calls.
     *
     * @param peerId User handle from whom the avatar is obtained.
     * @return Bitmap with the image avatar created.
     */
    public static Bitmap getImageAvatarCall(long peerId) {
        String mail = getUserMailCall(peerId);
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        String userHandleString = MegaApiAndroid.userHandleToBase64(peerId);
        String myUserHandleEncoded = MegaApiAndroid.userHandleToBase64(megaChatApi.getMyUserHandle());
        if (userHandleString.equals(myUserHandleEncoded)) {
            return getAvatarBitmap(mail);
        }

        return isTextEmpty(mail) ? getAvatarBitmap(userHandleString)
                : getUserAvatar(userHandleString, mail);
    }

    /**
     * Method to get the email from a handle.
     *
     * @param peerId User handle from whom the email is obtained.
     * @return The email.
     */
    public static String getUserMailCall(long peerId) {
        if (peerId == MegaApplication.getInstance().getMegaChatApi().getMyUserHandle()) {
            return MegaApplication.getInstance().getMegaChatApi().getMyEmail();
        } else {
            return MegaApplication.getInstance().getMegaChatApi().getUserEmailFromCache(peerId);
        }
    }

    /**
     * Method to get the name from a handle.
     *
     * @param context Activity context.
     * @param peerId  User handle from whom the name is obtained.
     * @return The name.
     */
    public static String getUserNameCall(Context context, long peerId) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        if (peerId == megaChatApi.getMyUserHandle()) {
            return megaChatApi.getMyFullname();
        }

        String nickname = getNicknameContact(peerId);
        if (nickname != null) {
            return nickname;
        }

        return new ChatController(context).getParticipantFullName(peerId);
    }

    /**
     * Retrieve the calls I'm participating in.
     *
     * @return The list of chats IDs with call.
     */
    public static ArrayList<Long> getCallsParticipating() {
        ArrayList<Long> listCalls = new ArrayList<>();
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        MegaHandleList listCallsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS);
        if (listCallsInProgress != null && listCallsInProgress.size() > 0) {
            for (int i = 0; i < listCallsInProgress.size(); i++) {
                listCalls.add(listCallsInProgress.get(i));
            }
        }
        MegaHandleList listCallsJoining = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_JOINING);
        if (listCallsJoining != null && listCallsJoining.size() > 0) {
            for (int i = 0; i < listCallsJoining.size(); i++) {
                listCalls.add(listCallsJoining.get(i));
            }
        }

        MegaHandleList listCallsInInitialState = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_INITIAL);
        if (listCallsInInitialState != null && listCallsInInitialState.size() > 0) {
            for (int i = 0; i < listCallsInInitialState.size(); i++) {
                listCalls.add(listCallsInInitialState.get(i));
            }
        }
        MegaHandleList listCallsConnecting = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_CONNECTING);
        if (listCallsConnecting != null && listCallsConnecting.size() > 0) {
            for (int i = 0; i < listCallsConnecting.size(); i++) {
                listCalls.add(listCallsConnecting.get(i));
            }
        }

        if (listCalls.isEmpty())
            return null;

        return listCalls;
    }

    /**
     * Method to retrieve the chat ID with an active call.
     *
     * @param currentChatId The chat ID with call.
     */
    public static long isAnotherActiveCall(long currentChatId) {
        ArrayList<Long> chatsIDsWithCallActive = getCallsParticipating();
        if (chatsIDsWithCallActive == null || chatsIDsWithCallActive.isEmpty()) {
            return currentChatId;
        }

        MegaChatCall currentCall = MegaApplication.getInstance().getMegaChatApi().getChatCall(currentChatId);
        if (currentCall != null && currentCall.isOnHold()) {
            Timber.d("Current call ON HOLD, look for other");
            for (Long anotherChatId : chatsIDsWithCallActive) {
                if (anotherChatId != currentChatId) {
                    MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(anotherChatId);
                    if (call != null && !call.isOnHold()) {
                        Timber.d("Another call ACTIVE");
                        return anotherChatId;
                    }
                }
            }
        }
        Timber.d("Current call ACTIVE, look for other");
        return currentChatId;
    }

    /**
     * Method to check if there is a call and that it is not on hold before answering it.
     *
     * @param currentChatId The current call.
     * @return The call in progress.
     */
    public static long existsAnotherCall(long currentChatId) {
        ArrayList<Long> chatsIDsWithCallActive = getCallsParticipating();
        if (chatsIDsWithCallActive == null || chatsIDsWithCallActive.isEmpty()) {
            return currentChatId;
        }
        for (Long anotherChatId : chatsIDsWithCallActive) {
            if (anotherChatId != currentChatId) {
                MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(anotherChatId);
                if (call != null && !call.isOnHold()) {
                    return anotherChatId;
                }
            }
        }
        return currentChatId;
    }

    public static PendingIntent getPendingIntentMeetingInProgress(Context context, long chatIdCallToAnswer, int requestCode, boolean isGuest) {
        Intent intentMeeting = new Intent(context, MeetingActivity.class);
        intentMeeting.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentMeeting.setAction(MEETING_ACTION_IN);
        intentMeeting.putExtra(MEETING_CHAT_ID, chatIdCallToAnswer);
        intentMeeting.putExtra(MEETING_IS_GUEST, isGuest);
        return PendingIntent.getActivity(context, requestCode, intentMeeting, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent getPendingIntentMeetingRinging(Context context, long chatIdCallToAnswer, int requestCode) {
        Intent intentMeeting = new Intent(context.getApplicationContext(), MeetingActivity.class);
        intentMeeting.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentMeeting.setAction(MEETING_ACTION_RINGING);
        intentMeeting.putExtra(MEETING_CHAT_ID, chatIdCallToAnswer);
        return PendingIntent.getActivity(context, requestCode, intentMeeting, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Check Camera Permission
     *
     * @param activity Current activity
     * @return True, if granted. False, if not granted
     */
    public static boolean checkCameraPermission(Activity activity) {
        boolean hasCameraPermission = hasPermissions(MegaApplication.getInstance().getBaseContext(), Manifest.permission.CAMERA);
        if (!hasCameraPermission) {
            if (activity == null)
                return false;

            if (activity instanceof ManagerActivity) {
                ((ManagerActivity) activity).setTypesCameraPermission(INVALID_TYPE_PERMISSIONS);
            }
            requestPermission(activity, REQUEST_CAMERA, Manifest.permission.CAMERA);
            return false;
        }

        return true;
    }

    /**
     * Check Audio Permission
     *
     * @param activity Current activity
     * @return True, if granted. False, if not granted
     */
    public static boolean checkAudioPermission(Activity activity) {
        boolean hasRecordAudioPermission = hasPermissions(MegaApplication.getInstance().getBaseContext(), Manifest.permission.RECORD_AUDIO);
        if (!hasRecordAudioPermission) {
            if (activity == null)
                return false;

            if (activity instanceof ManagerActivity) {
                ((ManagerActivity) activity).setTypesCameraPermission(INVALID_TYPE_PERMISSIONS);
            }
            requestPermission(activity, REQUEST_RECORD_AUDIO, Manifest.permission.RECORD_AUDIO);
            return false;
        }

        return true;
    }

    /**
     * Method for obtaining the necessary permissions in one call.
     *
     * @param activity Current activity
     * @return True, if you have both permits. False, otherwise.
     */
    public static boolean checkPermissionsCall(Activity activity) {
        if (!checkAudioPermission(activity)) {
            return false;
        }

        return checkCameraPermission(activity);
    }

    public static void addChecksForACall(long chatId, boolean speakerStatus) {
        MegaApplication.getChatManagement().setSpeakerStatus(chatId, speakerStatus);
    }

    /**
     * Method for removing the incoming call notification.
     *
     * @param callIdIncomingCall The call ID
     */
    public static void clearIncomingCallNotification(long callIdIncomingCall) {
        Timber.d("Clear the notification in call: %s", callIdIncomingCall);
        if (callIdIncomingCall == MEGACHAT_INVALID_HANDLE)
            return;

        try {
            NotificationManager notificationManager = (NotificationManager) MegaApplication.getInstance().getBaseContext().getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null)
                notificationManager.cancel(getCallNotificationId(callIdIncomingCall));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    /**
     * Method for getting the call notification ID.
     *
     * @param callId The call ID.
     * @return The notification ID.
     */
    public static int getCallNotificationId(long callId) {
        String notificationCallId = MegaApiAndroid.userHandleToBase64(callId);
        return notificationCallId.hashCode() + NOTIFICATION_CALL_IN_PROGRESS;
    }

    /**
     * Method to check if the chat is online
     *
     * @param newState The state of chat
     * @param chatRoom The MegaChatRoom
     * @return True, if the chat is connected and a call can be started. False, otherwise
     */
    public static boolean isChatConnectedInOrderToInitiateACall(int newState, MegaChatRoom chatRoom) {
        return newState == MegaChatApi.CHAT_CONNECTION_ONLINE
                && chatRoom != null && chatRoom.getPeerHandle(0) != MEGACHAT_INVALID_HANDLE &&
                chatRoom.getPeerHandle(0) == MegaApplication.getUserWaitingForCall();
    }

    /**
     * Method to display a dialogue informing the user that he/she cannot start or join a meeting while on a call in progress.
     *
     * @param context            Context of Activity
     * @param message            String with the text to show in the dialogue
     */
    public static void showConfirmationInACall(Context context, String message) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setMessage(message)
                .setPositiveButton(mega.privacy.android.shared.resources.R.string.general_ok, (dialog, which) -> {
                    if (context instanceof OpenLinkActivity) {
                        returnActiveCall(context);
                    }
                })
                .show();
    }

    /**
     * Method to know if a meeting has ended use [IsMeetingEndUseCase]
     *
     * @param chatRequest [MegaChatRequest]
     * @return True, if the meeting is finished. False, if not.
     */
    @Deprecated
    public static boolean isMeetingEnded(MegaChatRequest chatRequest) {
        return !MegaChatApi.hasChatOptionEnabled(
                MegaChatApi.CHAT_OPTION_WAITING_ROOM,
                chatRequest.getPrivilege()
        ) && (chatRequest.getMegaHandleList() == null
                || chatRequest.getMegaHandleList().get(0) == MEGACHAT_INVALID_HANDLE);
    }

    /**
     * Method to know if I am participating in this meeting use [CheckInThisMeetingUseCase]
     *
     * @param chatId Chat ID of the meeting
     * @return True, f I am participating in this meeting. False, if not.
     */
    @Deprecated
    public static boolean amIParticipatingInThisMeeting(long chatId) {
        MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(chatId);
        return call != null && call.getStatus() != MegaChatCall.CALL_STATUS_DESTROYED &&
                call.getStatus() != MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION &&
                call.getStatus() != CALL_STATUS_USER_NO_PRESENT;
    }

    public static void joinMeetingOrReturnCall(Context context, long chatId, String link, String titleChat, boolean alreadyExist, long publicChatHandle, boolean isWaitingRoom) {
        MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(chatId);
        if (call == null || call.getStatus() == CALL_STATUS_USER_NO_PRESENT || call.getStatus() == CALL_STATUS_WAITING_ROOM) {
            Timber.d("Call id: %d. It's a meeting, open to join", chatId);
            CallUtil.openMeetingToJoin(context, chatId, titleChat, link, alreadyExist ? publicChatHandle : MEGACHAT_INVALID_HANDLE, alreadyExist, isWaitingRoom);
        } else {
            Timber.d("Call id: %d. Return to call", chatId);
            returnCall(context, chatId);
        }
    }

    /**
     * Method that performs the necessary actions when there is an outgoing call or incoming call.
     *
     * @param chatId           Chat ID
     * @param callId           Call ID
     * @param typeAudioManager audio Manager type
     */
    public static void ongoingCall(RTCAudioManagerGateway rtcAudioManagerGateway, long chatId, long callId, int typeAudioManager) {
        AppRTCAudioManager rtcAudioManager = rtcAudioManagerGateway.getAudioManager();
        if (rtcAudioManager != null && rtcAudioManager.getTypeAudioManager() == typeAudioManager)
            return;

        MegaChatRoom chatRoom = MegaApplication.getInstance().getMegaChatApi().getChatRoom(chatId);
        if (chatRoom == null) {
            Timber.e("The chat does not exist");
            return;
        }

        Timber.d("Controlling outgoing/in progress call");
        if (typeAudioManager == AUDIO_MANAGER_CALL_OUTGOING && (chatRoom.isMeeting() || chatRoom.isGroup())) {
            clearIncomingCallNotification(callId);
            typeAudioManager = AUDIO_MANAGER_CALL_IN_PROGRESS;
        }

        MegaApplication.getInstance().createOrUpdateAudioManager(MegaApplication.getChatManagement().getSpeakerStatus(chatId), typeAudioManager);
    }

    /**
     * Check if an incoming call is a one-to-one call
     *
     * @param chatRoom MegaChatRoom of the call
     * @return True, if it is a one-to-one call. False, if it is a group call or meeting
     */
    public static boolean isOneToOneCall(MegaChatRoom chatRoom) {
        return !chatRoom.isGroup() && !chatRoom.isMeeting();
    }

    /**
     * Get incoming call notification title
     *
     * @param chatRoom MegaChatRoom of the call
     * @return Notification title
     */
    public static String getIncomingCallNotificationTitle(MegaChatRoom chatRoom, Context context) {
        return context.getString(isOneToOneCall(chatRoom)
                ? R.string.title_notification_incoming_individual_audio_call
                : R.string.title_notification_incoming_group_call);
    }

    /**
     * Method to create collapsed or expanded remote views for a customised incoming call notification.
     *
     * @param layoutId     ID of layout
     * @param chatToAnswer MegaChatRoom of the call
     * @param avatarIcon   Bitmap with the chat Avatar
     * @return The RemoteViews created
     */
    public static RemoteViews collapsedAndExpandedIncomingCallNotification(Context context, int layoutId, MegaChatRoom chatToAnswer, Bitmap avatarIcon) {
        Bitmap statusIcon = CallUtil.isOneToOneCall(chatToAnswer) ? getStatusBitmap(MegaApplication.getInstance().getMegaChatApi().getUserOnlineStatus(chatToAnswer.getPeerHandle(0))) : null;
        String titleChat = getTitleChat(chatToAnswer);
        String titleCall = CallUtil.getIncomingCallNotificationTitle(chatToAnswer, context);

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
        views.setTextViewText(R.id.chat_title, titleChat);
        views.setTextViewText(R.id.call_title, titleCall);

        if (avatarIcon == null) {
            views.setViewVisibility(R.id.avatar_layout, GONE);
        } else {
            views.setImageViewBitmap(R.id.avatar_image, avatarIcon);
            views.setViewVisibility(R.id.avatar_layout, View.VISIBLE);
        }

        if (statusIcon != null) {
            views.setImageViewBitmap(R.id.chat_status, statusIcon);
            views.setViewVisibility(R.id.chat_status, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.chat_status, GONE);
        }

        return views;
    }

    /**
     * Method to control when an attempt is made to initiate a call from a contact option
     *
     * @param context            The Activity context
     * @return True, if the call can be started. False, otherwise.
     */
    public static boolean canCallBeStartedFromContactOption(Activity context) {
        if (StorageStateExtensionsKt.getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning();
            return false;
        }

        if (CallUtil.participatingInACall()) {
            showConfirmationInACall(context, context.getString(R.string.ongoing_call_content));
            return false;
        }

        return checkPermissionsCall(context);
    }

    /**
     * Method to find out if device's notification settings are enabled
     *
     * @return True, if they are enabled. False, if they are not.
     */
    public static boolean areNotificationsSettingsEnabled() {
        NotificationManager notificationManager = (NotificationManager) MegaApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager.areNotificationsEnabled();
    }
}
