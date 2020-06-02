package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.core.content.ContextCompat;

import android.util.TypedValue;
import android.view.View;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.InviteContactActivity;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatSession;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class CallUtil {

    /**
     * Retrieve if there's a call in progress that you're participating in.
     *
     * @return True if you're on a call in progress. Otherwise false.
     */
    public static boolean participatingInACall() {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        MegaHandleList listCallsRequestSent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_REQUEST_SENT);
        MegaHandleList listCallsUserNoPresent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_USER_NO_PRESENT);
        MegaHandleList listCallsRingIn = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RING_IN);
        MegaHandleList listCallsDestroy = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_DESTROYED);
        MegaHandleList listCalls = megaChatApi.getChatCalls();

        if ((listCalls.size() - listCallsDestroy.size()) == 0) {
            logDebug("No calls in progress");
            return false;
        }

        logDebug("There is some call in progress");
        if ((listCalls.size() - listCallsDestroy.size()) == (listCallsUserNoPresent.size() + listCallsRingIn.size())) {
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
            if(call != null && !call.isOnHold()){
                MegaApplication.setShowPinScreen(false);
                Intent intent = new Intent(context, ChatCallActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(CHAT_ID, call.getChatid());
                intent.putExtra(CALL_ID, call.getId());
                context.startActivity(intent);
            }
        }
    }
    /**
     * Open the call that is in progress
     *
     * @param context from which the action is done
     */
    public static void returnCall(Context context, long chatId) {
        ArrayList<Long> currentCalls = getCallsParticipating();

        for(Long chatIdCall:currentCalls){
            if(chatIdCall == chatId){
                MegaChatCall call = MegaApplication.getInstance().getMegaChatApi().getChatCall(chatId);
                MegaApplication.setShowPinScreen(false);
                Intent intent = new Intent(context, ChatCallActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(CHAT_ID, call.getChatid());
                intent.putExtra(CALL_ID, call.getId());
                context.startActivity(intent);
            }
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
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        if (callInProgressLayout == null){
            return;
        }
        ArrayList<Long> currentChatCallsList = getCallsParticipating();

        if (!participatingInACall() || currentChatCallsList == null) {
            callInProgressLayout.setVisibility(View.GONE);
            activateChrono(false, callInProgressChrono, null);
            return;
        }

        ArrayList<MegaChatCall> callsActive = new ArrayList<>();

        for (Long chatIdCall : currentChatCallsList) {
            MegaChatCall current = megaChatApi.getChatCall(chatIdCall);
            if (current != null) {
                if (!current.isOnHold()) {
                    callsActive.add(current);
                }
            }
        }
        if (callsActive.isEmpty()) {
            callInProgressLayout.setVisibility(View.GONE);
            activateChrono(false, callInProgressChrono, null);
            return;
        }

        long chatId = getChatCallInProgress();
        if (chatId == -1){
            return;
        }

        MegaChatCall call = megaChatApi.getChatCall(chatId);
        if (call.getStatus() == MegaChatCall.CALL_STATUS_RECONNECTING) {
            logDebug("Displayed the Reconnecting call layout");
            activateChrono(false, callInProgressChrono, null);
            callInProgressLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.reconnecting_bar));
            callInProgressText.setText(context.getString(R.string.reconnecting_message));

        } else {
            logDebug("Displayed the layout to return to the call");
            callInProgressText.setText(context.getString(R.string.call_in_progress_layout));
            callInProgressLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.accentColor));

            if (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                activateChrono(true, callInProgressChrono, call);
            } else {
                activateChrono(false, callInProgressChrono, null);
            }
        }

        callInProgressLayout.setVisibility(View.VISIBLE);
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

    private static void disableLocalCamera() {
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
        return AvatarUtil.getDefaultAvatar(getColorAvatar(peerId), getUserNameCall(chat, peerId), px2dp(AVATAR_SIZE_CALLS, ((ChatCallActivity) context).getOutMetrics()), true);
    }

    /**
     * Method to get the image avatar in calls.
     *
     * @param context Context of the Activity.
     * @param chat    Chat room identifier where the call is.
     * @param peerId  User handle from whom the avatar is obtained.
     * @return Bitmap with the image avatar created.
     */
    public static Bitmap getImageAvatarCall(Context context, MegaChatRoom chat, long peerId) {
        /*Avatar*/
        String mail = getUserMailCall(chat, peerId);
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();

        if (peerId == megaChatApi.getMyUserHandle() || megaApi.getContact(mail) != null) {
            Bitmap bitmap = getImageAvatar(mail);
            if(bitmap != null){
                return bitmap;
            }

            if (peerId != megaChatApi.getMyUserHandle()) {
                megaApi.getUserAvatar(megaApi.getContact(mail), buildAvatarFile(context, mail + ".jpg").getAbsolutePath(), ((ChatCallActivity) context));
            }

        } else if (megaApi.getContact(mail) == null) {
            String userHandleEncoded = MegaApiAndroid.userHandleToBase64(peerId);
            megaApi.getUserAvatar(userHandleEncoded, buildAvatarFile(context, peerId + ".jpg").getAbsolutePath(), ((ChatCallActivity) context));
        }
        return null;
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
     * @param chat   Chat room identifier.
     * @param peerId User handle from whom the name is obtained.
     * @return The name.
     */
    public static String getUserNameCall(MegaChatRoom chat, long peerId){
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        if (peerId == megaChatApi.getMyUserHandle()) {
            return megaChatApi.getMyFullname();
        }

        String nickname = getNicknameContact(peerId);
        if (nickname != null) {
            return nickname;
        }

        String name = chat.getPeerFirstnameByHandle(peerId);
        if(name != null){
            return name;
        }
        return  chat.getPeerEmailByHandle(peerId);
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
        int actionBarHeight = ((ChatCallActivity) context).getSupportActionBar().getHeight();
        if (actionBarHeight != 0) {
            return actionBarHeight;
        }

        final TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
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
                    if (!call.isOnHold()) {
                        logDebug("Another call ACTIVE");
                        return anotherChatId;
                    }
                }
            }
        }
        logDebug("Current call ACTIVE, look for other");
        return currentChatId;
    }

}
