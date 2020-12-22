package mega.privacy.android.app.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.listeners.CreateChatListener;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.InviteContactActivity;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatSession;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaUser;

import static android.content.Context.NOTIFICATION_SERVICE;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class CallUtil {

    public static final int MAX_PARTICIPANTS_IN_CALL = 20;

    /**
     * Retrieve if there's a call in progress that you're participating in.
     *
     * @return True if you're on a call in progress. Otherwise false.
     */
    public static boolean participatingInACall() {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        MegaHandleList listCallsRequestSent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_REQUEST_SENT);
        MegaHandleList listCallsRingIn = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RING_IN);
        MegaHandleList listCallsTerminating = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION);
        MegaHandleList listCallsUserNoPresent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_USER_NO_PRESENT);
        MegaHandleList listCallsDestroy = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_DESTROYED);
        MegaHandleList listCalls = megaChatApi.getChatCalls();

        if ((listCalls.size() - listCallsDestroy.size()) == 0) {
            logDebug("No calls in progress");
            return false;
        }

        logDebug("There is some call in progress");
        if ((listCalls.size() - listCallsDestroy.size()) == (listCallsUserNoPresent.size() + listCallsTerminating.size() + listCallsRingIn.size())) {
            logDebug("I'm not participating in any of the calls there");
            return false;
        }
        if (listCallsRequestSent.size() > 0) {
            logDebug("I'm doing a outgoing call");
            return true;
        }
        logDebug("I'm in a call in progress");
        return true;
    }

    /**
     * Retrieve if there's a call in progress that you're participating in or a incoming call.
     *
     * @return True if you're on a call in progress o exists a incoming call. Otherwise false.
     */
    public static boolean existsAnOgoingOrIncomingCall() {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MegaHandleList listCallsUserNoPresent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_USER_NO_PRESENT);
        MegaHandleList listCallsDestroy = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_DESTROYED);
        MegaHandleList listCalls = megaChatApi.getChatCalls();

        if ((listCalls.size() - listCallsDestroy.size()) == 0) {
            logDebug("No calls in progress");
            return false;
        }

        if ((listCalls.size() - listCallsDestroy.size()) == listCallsUserNoPresent.size()) {
            logDebug("I'm not participating in any of the calls there");
            return false;
        }

        return true;
    }

    /**
     * Retrieve the id of a chat that has a call in progress.
     *
     * @return A long data type. It's the id of chat.
     */
    public static long getChatCallInProgress() {
        ArrayList<Long> listCalls = new ArrayList<>();
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MegaHandleList listCallsRequestSent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_REQUEST_SENT);
        if (listCallsRequestSent != null && listCallsRequestSent.size() > 0) {
            for(int i = 0; i < listCallsRequestSent.size(); i++){
                listCalls.add(listCallsRequestSent.get(i));
            }
        }

        MegaHandleList listCallsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS);
        if (listCallsInProgress != null && listCallsInProgress.size() > 0) {
            for(int i = 0; i < listCallsInProgress.size(); i++){
                listCalls.add(listCallsInProgress.get(i));
            }
        }
        MegaHandleList listCallsJoining = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_JOINING);
        if (listCallsJoining != null && listCallsJoining.size() > 0) {
            for(int i = 0; i < listCallsJoining.size(); i++){
                listCalls.add(listCallsJoining.get(i));
            }
        }

        MegaHandleList listCallsInReconnecting = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RECONNECTING);
        if (listCallsInReconnecting != null && listCallsInReconnecting.size() > 0) {
            for(int i = 0; i < listCallsInReconnecting.size(); i++){
                listCalls.add(listCallsInReconnecting.get(i));
            }
        }

        if (!listCalls.isEmpty()) {
            for (Long idChat : listCalls) {
                if (!megaChatApi.getChatCall(idChat).isOnHold()) {
                    return idChat;
                }
            }
        }

        return MEGACHAT_INVALID_HANDLE;
    }

    /**
     * Open the call that is in progress
     *
     * @param context from which the action is done
     */
    public static void returnActiveCall(Context context) {
        ArrayList<Long> currentCalls = getCallsParticipating();

        for(Long chatIdCall:currentCalls){
            MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(chatIdCall);
            if(call != null){
                MegaApplication.setShowPinScreen(false);
                Intent intent = new Intent(context, ChatCallActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(CHAT_ID, call.getChatid());
                intent.putExtra(CALL_ID, call.getId());
                context.startActivity(intent);
                break;
            }
        }
    }

    /**
     *  Open the call that is in progress
     * @param context from which the action is done
     * @param chatId ID chat.
     */
    public static void returnCall(Context context, long chatId) {
        ArrayList<Long> currentCalls = getCallsParticipating();
        if(currentCalls == null || currentCalls.isEmpty())
            return;

        for(Long chatIdCall:currentCalls){
            if(chatIdCall == chatId){
                MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(chatId);
                MegaApplication.setShowPinScreen(false);
                Intent intent = new Intent(context, ChatCallActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(CHAT_ID, call.getChatid());
                intent.putExtra(CALL_ID, call.getId());
                context.startActivity(intent);
                return;
            }
        }
    }

    /**
     * Method to get the session of an individual call.
     *
     * @return The session.
     */
    public static MegaChatSession getSessionIndividualCall(MegaChatCall callChat) {
        if(callChat == null)
            return null;

        return callChat.getMegaChatSession(callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
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
        if (call == null || !MegaApplication.getCallLayoutStatus(call.getChatid()))
            return;

        if (call.getStatus() == MegaChatCall.CALL_STATUS_RECONNECTING) {
            activateChrono(false, callInProgressChrono, null);
            callInProgressLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.reconnecting_bar));
            callInProgressText.setText(context.getString(R.string.reconnecting_message));
        } else {

            callInProgressText.setText(context.getString(R.string.call_in_progress_layout));
            callInProgressLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.accentColor));

            if (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                activateChrono(true, callInProgressChrono, call);
            } else {
                activateChrono(false, callInProgressChrono, null);
            }
        }

        callInProgressLayout.setVisibility(View.VISIBLE);

        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop) context).changeToolbarLayoutElevation();
        }
        if (context instanceof ContactInfoActivityLollipop) {
            ((ContactInfoActivityLollipop) context).changeToolbarLayoutElevation();
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

        long chatIdInProgress = getChatCallInProgress();
        if (chatIdInProgress != MEGACHAT_INVALID_HANDLE) {
            createCallBanner(context, chatIdInProgress, callInProgressLayout, callInProgressChrono, callInProgressText);
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
        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop) context).changeToolbarLayoutElevation();
        }
        if (context instanceof ContactInfoActivityLollipop) {
            ((ContactInfoActivityLollipop) context).changeToolbarLayoutElevation();
        }
    }

    private static void createCallMenuItem(MegaChatCall call, final MenuItem returnCallMenuItem, final LinearLayout layoutCallMenuItem, final Chronometer chronometerMenuItem){
        Context context = MegaApplication.getInstance().getBaseContext();
        int callStatus = call.getStatus();
        layoutCallMenuItem.setBackground(ContextCompat.getDrawable(context, callStatus == MegaChatCall.CALL_STATUS_RECONNECTING ? R.drawable.reconnection_rounded : R.drawable.dark_rounded_chat_own_message));

        if(chronometerMenuItem != null && (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_JOINING)){
            if(chronometerMenuItem.getVisibility() == View.VISIBLE) return;
            chronometerMenuItem.setVisibility(View.VISIBLE);
            chronometerMenuItem.setBase(SystemClock.elapsedRealtime() - (call.getDuration() * 1000));
            chronometerMenuItem.start();
            chronometerMenuItem.setFormat(" %s");
        }else{
            if(chronometerMenuItem.getVisibility() == View.GONE) return;
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
            long chatIdInProgress = getChatCallInProgress();
            if (chatIdInProgress != MEGACHAT_INVALID_HANDLE) {
                MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(chatIdInProgress);
                createCallMenuItem(call, returnCallMenuItem, layoutCallMenuItem, chronometerMenuItem);
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
            if (context instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop) context).changeToolbarLayoutElevation();
            }
            if (context instanceof ContactInfoActivityLollipop) {
                ((ContactInfoActivityLollipop) context).changeToolbarLayoutElevation();
            }
        }
    }

    /**
     * Retrieve if the call was in a reconnecting state or not
     *
     * @param context          from which the action is done.
     * @param layout           Type RelativeLayout.
     * @param reconnectingText Type TextView.
     * @return True If the previous status of the call was reconnecting. Otherwise false.
     */
    public static boolean isAfterReconnecting(Context context, RelativeLayout layout, final TextView reconnectingText) {
        return layout != null && layout.getVisibility() == View.VISIBLE && reconnectingText.getText().toString().equals(context.getString(R.string.reconnecting_message));
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

        MegaChatCall call = megaChatApi.getChatCall(chatId);
        return (call.getStatus() <= MegaChatCall.CALL_STATUS_REQUEST_SENT) || (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING) || (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS);
    }

    /**
     * Method to activate or deactivate the chronometer of a call.
     *
     * @param activateChrono True, if it must be activated. False, if it must be deactivated.
     * @param chronometer    The cronometer.
     * @param call           The call.
     */
    public static void activateChrono(boolean activateChrono, final Chronometer chronometer, MegaChatCall call) {
        if (chronometer == null)
            return;

        if (!activateChrono) {
            chronometer.stop();
            chronometer.setVisibility(View.GONE);
            return;
        }

        if (call != null) {
            chronometer.setVisibility(View.VISIBLE);
            chronometer.setBase(SystemClock.elapsedRealtime() - (call.getDuration()* 1000));
            chronometer.start();
            chronometer.setFormat(" %s");
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

    public static void showErrorAlertDialogGroupCall(String message, final boolean finish, final Activity activity) {
        if (activity == null) {
            return;
        }

        try {
            android.app.AlertDialog.Builder dialogBuilder = getCustomAlertBuilder(activity, activity.getString(R.string.general_error_word), message, null);
            dialogBuilder.setPositiveButton(
                    activity.getString(android.R.string.ok),
                    (dialog, which) -> {
                        dialog.dismiss();
                        if (finish) {
                            activity.finishAndRemoveTask();
                        }
                    });
            dialogBuilder.setOnCancelListener(dialog -> {
                if (finish) {
                    activity.finishAndRemoveTask();
                }
            });

            android.app.AlertDialog dialog = dialogBuilder.create();
            dialog.show();
            brandAlertDialog(dialog);
        } catch (Exception ex) {
            showToast(activity, message);
        }
    }

    public static String callStatusToString(int status) {
        switch (status) {
            case MegaChatCall.CALL_STATUS_INITIAL:
                return "CALL_STATUS_INITIAL";
            case MegaChatCall.CALL_STATUS_HAS_LOCAL_STREAM:
                return "CALL_STATUS_HAS_LOCAL_STREAM";
            case MegaChatCall.CALL_STATUS_REQUEST_SENT:
                return "CALL_STATUS_REQUEST_SENT";
            case MegaChatCall.CALL_STATUS_RING_IN:
                return "CALL_STATUS_RING_IN";
            case MegaChatCall.CALL_STATUS_JOINING:
                return "CALL_STATUS_JOINING";
            case MegaChatCall.CALL_STATUS_IN_PROGRESS:
                return "CALL_STATUS_IN_PROGRESS";
            case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
                return "CALL_STATUS_TERMINATING_USER_PARTICIPATION";
            case MegaChatCall.CALL_STATUS_DESTROYED:
                return "CALL_STATUS_DESTROYED";
            case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
                return "CALL_STATUS_USER_NO_PRESENT";
            case MegaChatCall.CALL_STATUS_RECONNECTING:
                return "CALL_STATUS_RECONNECTING";
            default:
                return String.valueOf(status);
        }
    }

    public static String sessionStatusToString(int status) {
        switch (status) {
            case MegaChatSession.SESSION_STATUS_INVALID:
                return "SESSION_STATUS_INVALID";
            case MegaChatSession.SESSION_STATUS_INITIAL:
                return "SESSION_STATUS_INITIAL";
            case MegaChatSession.SESSION_STATUS_IN_PROGRESS:
                return "SESSION_STATUS_IN_PROGRESS";
            case MegaChatSession.SESSION_STATUS_DESTROYED:
                return "SESSION_STATUS_DESTROYED";
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
            if (context instanceof ContactInfoActivityLollipop) {
                ((ContactInfoActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            }
            return false;
        }
        return true;
    }

    public static void disableLocalCamera() {
        long idCall = isNecessaryDisableLocalCamera();
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        if (idCall == -1) return;

        megaChatApi.disableVideo(idCall, null);
    }

    public static long isNecessaryDisableLocalCamera() {
        long noVideo = -1;

        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        long chatIdCallInProgress = getChatCallInProgress();
        MegaChatCall callInProgress = megaChatApi.getChatCall(chatIdCallInProgress);
        if (callInProgress == null || !callInProgress.hasLocalVideo()) {
            return noVideo;
        }

        return chatIdCallInProgress;
    }

    /**
     * When there is a video call in progress with the video enabled of the current account logged-in,
     * alerts the user if they are sure they want to perform the action in which the camera is involved,
     * since their camera will be disabled in the call.
     *
     * @param activity      current Activity involved
     * @param action        the action to perform. These are the possibilities:
     *                      ACTION_TAKE_PICTURE, TAKE_PICTURE_PROFILE_CODE, ACTION_OPEN_QR
     * @param openScanQR    if the action is ACTION_OPEN_QR, it specifies whether to open the "Scan QR" section.
     *                      True if it should open the "Scan QR" section, false otherwise.
     */
    public static void showConfirmationOpenCamera(Activity activity, String action, boolean openScanQR) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    logDebug("Open camera and lost the camera in the call");
                    disableLocalCamera();
                    if (activity instanceof ChatActivityLollipop && action.equals(ACTION_TAKE_PICTURE)) {
                        ((ChatActivityLollipop) activity).controlCamera();
                    }
                    if (activity instanceof ManagerActivityLollipop) {
                        if (action.equals(ACTION_OPEN_QR)) {
                            ((ManagerActivityLollipop) activity).openQR(openScanQR);
                        } else if (action.equals(ACTION_TAKE_PICTURE)) {
                            takePicture(activity, TAKE_PHOTO_CODE);
                        } else if (action.equals(ACTION_TAKE_PROFILE_PICTURE)) {
                            takePicture(activity, TAKE_PICTURE_PROFILE_CODE);
                        }
                    }
                    if (activity instanceof AddContactActivityLollipop && action.equals(ACTION_OPEN_QR)) {
                        ((AddContactActivityLollipop) activity).initScanQR();
                    }
                    if (activity instanceof InviteContactActivity && action.equals(ACTION_OPEN_QR)) {
                        ((InviteContactActivity) activity).initScanQR();
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle);
        String message = activity.getString(R.string.confirmation_open_camera_on_chat);
        builder.setTitle(R.string.title_confirmation_open_camera_on_chat);
        builder.setMessage(message).setPositiveButton(R.string.context_open_link, dialogClickListener).setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    /**
     * Method to get the default avatar in calls.
     *
     * @param context    Context of the Activity.
     * @param chat       Chat room identifier where the call is.
     * @param peerId     User handle from whom the avatar is obtained.
     * @return Bitmap with the default avatar created.
     */
    public static Bitmap getDefaultAvatarCall(Context context, MegaChatRoom chat, long peerId) {
        return AvatarUtil.getDefaultAvatar(getColorAvatar(peerId), getUserNameCall(context, peerId),
                dp2px(AVATAR_SIZE_CALLS, ((ChatCallActivity) context).getOutMetrics()), true);
    }

    /**
     * Method to get the image avatar in calls.
     *
     * @param chat    Chat room identifier where the call is.
     * @param peerId  User handle from whom the avatar is obtained.
     * @return Bitmap with the image avatar created.
     */
    public static Bitmap getImageAvatarCall(MegaChatRoom chat, long peerId) {
        String mail = getUserMailCall(chat, peerId);
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
     * @param chat Chat room identifier.
     * @param peerId User handle from whom the email is obtained.
     * @return The email.
     */
    public static String getUserMailCall(MegaChatRoom chat, long peerId){
        if (peerId == MegaApplication.getInstance().getMegaChatApi().getMyUserHandle()) {
            return MegaApplication.getInstance().getMegaChatApi().getMyEmail();
        } else {
            return chat.getPeerEmailByHandle(peerId);
        }
    }

    /**
     * Method to get the name from a handle.
     *
     * @param context  Activity context.
     * @param peerId User handle from whom the name is obtained.
     * @return The name.
     */
    public static String getUserNameCall(Context context, long peerId){
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        if (peerId == megaChatApi.getMyUserHandle()) {
            return megaChatApi.getMyFullname();
        }

        String nickname = getNicknameContact(peerId);
        if (nickname != null) {
            return nickname;
        }

        return new ChatController(context).getParticipantFirstName(peerId);
    }

    /**
     * Method for finding out if the participant is me.
     *
     * @param peerId   The Peer ID.
     * @param clientId The Client ID.
     * @return True if it's me. Otherwise, False.
     */
    public static boolean isItMe(long chatId, long peerId, long clientId) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        return peerId == megaChatApi.getMyUserHandle() && clientId == megaChatApi.getMyClientidHandle(chatId);
    }

    /**
     * Method to get the call on hold if it's different than the current call.
     *
     * @param callId The current call ID.
     * @return The call on hold.
     */
    public static MegaChatCall getAnotherCallOnHold(long callId) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MegaHandleList listCallsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS);
        if (listCallsInProgress != null && listCallsInProgress.size() > 0) {
            for (int i = 0; i < listCallsInProgress.size(); i++) {
                MegaChatCall call = megaChatApi.getChatCall(listCallsInProgress.get(i));
                if (call != null && call.isOnHold() && call.getId() != callId) {
                    return call;
                }
            }
        }

        MegaHandleList listCallsJoining = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_JOINING);
        if (listCallsJoining != null && listCallsJoining.size() > 0) {
            for (int i = 0; i < listCallsJoining.size(); i++) {
                MegaChatCall call = megaChatApi.getChatCall(listCallsJoining.get(i));
                if (call != null && call.isOnHold() && call.getId() != callId) {
                    return call;
                }
            }
        }

        MegaHandleList listCallsReconnecting = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RECONNECTING);
        if (listCallsReconnecting != null && listCallsReconnecting.size() > 0) {
            for (int i = 0; i < listCallsReconnecting.size(); i++) {
                MegaChatCall call = megaChatApi.getChatCall(listCallsReconnecting.get(i));
                if (call != null && call.isOnHold() && call.getId() != callId) {
                    return call;
                }
            }
        }

        return null;
    }

    /**
     * Retrieve the calls I'm participating in.
     *
     * @return The list of chats IDs with call.
     */
    public static ArrayList<Long> getCallsParticipating() {
        ArrayList<Long> listCalls = new ArrayList<>();
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MegaHandleList listCallsRequestSent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_REQUEST_SENT);
        if (listCallsRequestSent != null && listCallsRequestSent.size() > 0) {
            for(int i = 0; i < listCallsRequestSent.size(); i++){
                listCalls.add(listCallsRequestSent.get(i));
            }
        }

        MegaHandleList listCallsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS);
        if (listCallsInProgress != null && listCallsInProgress.size() > 0) {
            for(int i = 0; i < listCallsInProgress.size(); i++){
                listCalls.add(listCallsInProgress.get(i));
            }
        }
        MegaHandleList listCallsJoining = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_JOINING);
        if (listCallsJoining != null && listCallsJoining.size() > 0) {
            for(int i = 0; i < listCallsJoining.size(); i++){
                listCalls.add(listCallsJoining.get(i));
            }
        }

        MegaHandleList listCallsInReconnecting = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RECONNECTING);
        if (listCallsInReconnecting != null && listCallsInReconnecting.size() > 0) {
            for(int i = 0; i < listCallsInReconnecting.size(); i++){
                listCalls.add(listCallsInReconnecting.get(i));
            }
        }

        if (listCalls.isEmpty())
            return null;

        return listCalls;
    }

    /**
     * Method for obtaining the height of the action bar.
     *
     * @return The height of actionbar.
     */
    public static int getActionBarHeight(Context context) {
        int actionBarHeight = 0;

        if (((ChatCallActivity) context).getSupportActionBar() != null) {
            actionBarHeight = ((ChatCallActivity) context).getSupportActionBar().getHeight();
        }

        if (actionBarHeight == 0) {
            final TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
            }
        }

        return actionBarHeight;
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
            logDebug("Current call ON HOLD, look for other");
            for (Long anotherChatId : chatsIDsWithCallActive) {
                if (anotherChatId != currentChatId) {
                    MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(anotherChatId);
                    if (call != null && !call.isOnHold()) {
                        logDebug("Another call ACTIVE");
                        return anotherChatId;
                    }
                }
            }
        }
        logDebug("Current call ACTIVE, look for other");
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

    public static PendingIntent getPendingIntentCall(Context context, long chatIdCallToAnswer, int requestCode) {
        Intent intentCall = new Intent(context, ChatCallActivity.class);
        intentCall.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentCall.putExtra(CHAT_ID, chatIdCallToAnswer);
        return PendingIntent.getActivity(context, requestCode, intentCall, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Method for knowing if the call start button should be enabled or not.
     *
     * @return True, if it should be enabled or false otherwise.
     */
    public static boolean isCallOptionEnabled() {
        return !participatingInACall();
    }
/**
     * Method to control when a call should be started, whether the chat room should be created or is already created.
     *
     * @param activity The Activity.
     * @param user    The mega User.
     */
    public static void startNewCall(Activity activity, MegaUser user) {
        if(user == null)
            return;

        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());

        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        if (chat == null) {
            ArrayList<MegaChatRoom> chats = new ArrayList<>();
            ArrayList<MegaUser> usersNoChat = new ArrayList<>();
            usersNoChat.add(user);
            CreateChatListener listener = new CreateChatListener(chats, usersNoChat, -1, activity, CreateChatListener.START_AUDIO_CALL);
            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, listener);
        } else if (megaChatApi.getChatCall(chat.getChatId()) != null) {
            Intent i = new Intent(activity, ChatCallActivity.class);
            i.putExtra(CHAT_ID, chat.getChatId());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
        } else if (isStatusConnected(activity, chat.getChatId())) {
            MegaApplication.setUserWaitingForCall(user.getHandle());
            startCallWithChatOnline(activity, chat);
        }
    }

    /**
     * Method to control if the chat is online in order to start a call.
     *
     * @param activity  The Activity.
     * @param chatRoom The chatRoom.
     */
    public static void startCallWithChatOnline(Activity activity, MegaChatRoom chatRoom) {
        if (checkPermissionsCall(activity, START_CALL_PERMISSIONS)) {
            MegaApplication.setCallLayoutStatus(chatRoom.getChatId(), false);
            MegaApplication.setSpeakerStatus(chatRoom.getChatId(), false);
            MegaApplication.getInstance().getMegaChatApi().startChatCall(chatRoom.getChatId(), false, (MegaChatRequestListenerInterface) activity);
            MegaApplication.setIsWaitingForCall(false);
        }
    }

    /**
     * Method for obtaining the necessary permissions in one call.
     *
     * @param activity       The activity.
     * @param typePermission The type of permission
     * @return True, if you have both permits. False, otherwise.
     */
    public static boolean checkPermissionsCall(Activity activity, int typePermission) {
        boolean hasCameraPermission = (ContextCompat.checkSelfPermission(MegaApplication.getInstance().getBaseContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
        if (!hasCameraPermission) {
            if(activity == null)
                return false;

            if (activity instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop) activity).setTypesCameraPermission(typePermission);
            }
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            return false;
        }

        boolean hasRecordAudioPermission = (ContextCompat.checkSelfPermission(MegaApplication.getInstance().getBaseContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
        if (!hasRecordAudioPermission) {
            if(activity == null)
                return false;

            if (activity instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop) activity).setTypesCameraPermission(typePermission);
            }
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO);
            return false;
        }

        return true;
    }

    /**
     * Checks if it cannot join to call because has reached the maximum number of participants.
     * If so, shows a snackbar with a warning.
     *
             * @param context   current Context
     * @param call      MegaChatCall to check
     * @param chat      MegaChatRoom to check
     * @return True if cannot joint to call, false otherwise
     */
    public static boolean canNotJoinCall(Context context, MegaChatCall call, MegaChatRoom chat) {
        if (call == null || call.getNumParticipants(MegaChatCall.ANY_FLAG) >= MAX_PARTICIPANTS_IN_CALL) {
            showSnackbar(context, context.getString(R.string.call_error_too_many_participants));
            return true;
        } else if (canNotStartCall(context, chat, true)) {
            showSnackbar(context, context.getString(R.string.call_error_too_many_participants_join));
            return true;
        }

        return false;
    }

    /**
     * Checks if it cannot start a call because has reached the maximum number of participants.
     * If so, shows a snackbar with a warning.
     *
     * @param context   current Context
     * @param chat      MegaChatRoom to check
     * @return True if cannot start a call, false otherwise
     */
    public static boolean canNotStartCall(Context context, MegaChatRoom chat) {
        return canNotStartCall(context, chat, false);
    }

    /**
     * Checks if it cannot start a call because has reached the maximum number of participants.
     * If so, shows a snackbar with a warning.
     *
     * @param context   current Context
     * @param chat      MegaChatRoom to check
     * @param joining   true if it is related to a join request, false otherwise
     * @return True if cannot start a call, false otherwise
     */
    public static boolean canNotStartCall(Context context, MegaChatRoom chat, boolean joining) {
        if (chat == null || (chat.isPublic() && chat.getPeerCount() + 1 > MAX_PARTICIPANTS_IN_CALL)) {
            if (!joining) {
                showSnackbar(context, context.getString(R.string.call_error_too_many_participants_start));
            }
            return true;
        }

        return false;
    }

    public static void addChecksForACall(long chatId, boolean speakerStatus){
        MegaApplication.setCallLayoutStatus(chatId, false);
        MegaApplication.setSpeakerStatus(chatId, speakerStatus);
    }

    /**
     * Method for removing the incoming call notification.
     *
     * @param callIdIncomingCall The call ID
     */
    public static void clearIncomingCallNotification(long callIdIncomingCall) {
        logDebug("Clear the notification in chat: " + callIdIncomingCall);
        if(callIdIncomingCall == MEGACHAT_INVALID_HANDLE)
            return;

        try {
            NotificationManager notificationManager = (NotificationManager) MegaApplication.getInstance().getBaseContext().getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(getCallNotificationId(callIdIncomingCall));
        } catch (Exception e) {
            logError("EXCEPTION", e);
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
}
