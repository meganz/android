package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import androidx.core.content.ContextCompat;

import android.os.Vibrator;
import android.view.View;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.InviteContactActivity;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatSession;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

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

    public static boolean inACall(){
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
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MegaHandleList listCallsRequestSent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_REQUEST_SENT);
        if (listCallsRequestSent != null && listCallsRequestSent.size() > 0) {
            return listCallsRequestSent.get(0);
        }

        MegaHandleList listCallsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS);
        if (listCallsInProgress != null && listCallsInProgress.size() > 0) {
            return listCallsInProgress.get(0);
        }

        MegaHandleList listCallsInReconnecting = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RECONNECTING);
        if (listCallsInReconnecting != null && listCallsInReconnecting.size() > 0) {
            return listCallsInReconnecting.get(0);
        }
        return -1;
    }

    /**
     * Open the call that is in progress
     *
     * @param context from which the action is done
     */
    public static void returnCall(Context context) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        if (megaChatApi.getChatCall(getChatCallInProgress()) == null)
            return;

        long chatId = getChatCallInProgress();
        MegaChatCall call = megaChatApi.getChatCall(chatId);
        MegaApplication.setShowPinScreen(false);
        Intent intent = new Intent(context, ChatCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(CHAT_ID, chatId);
        intent.putExtra(CALL_ID, call.getId());
        context.startActivity(intent);
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

        if (!participatingInACall()) {
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

    public static void setAudioManagerValues(AudioManager audioManager, MediaPlayer mediaPlayer, Vibrator vibrator, int callStatus, MegaHandleList listCallsRequest, MegaHandleList listCallsRing) {
        logDebug("Call status: " + callStatusToString(callStatus));

        if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
            if (listCallsRing != null && listCallsRing.size() > 0) {
                logDebug("There was also an incoming call (stop incoming call sound)");
                stopAudioSignals(vibrator, mediaPlayer);
            }

            outgoingCallSound(audioManager, mediaPlayer);

        } else if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
            if (listCallsRequest == null || listCallsRequest.size() < 1) {
                logDebug("I'm not calling");
                if (listCallsRing != null && listCallsRing.size() > 1) {
                    logDebug("There is another incoming call (stop the sound of the previous incoming call)");
                    stopAudioSignals(vibrator, mediaPlayer);
                }

                incomingCallSound(audioManager, mediaPlayer);
            }

            checkVibration(audioManager, vibrator);
        }
    }

    private static void outgoingCallSound(AudioManager audioManager, MediaPlayer mediaPlayer) {
        if (audioManager == null || mediaPlayer == null || (mediaPlayer != null && mediaPlayer.isPlaying()))
            return;

        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL), 0);
        Resources res = MegaApplication.getInstance().getBaseContext().getResources();
        AssetFileDescriptor afd = res.openRawResourceFd(R.raw.outgoing_voice_video_call);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
        mediaPlayer.setLooping(true);
        try {
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            logDebug("Preparing mediaPlayer");
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            logError("Error preparing mediaPlayer", e);
            return;
        }
        logDebug("*************Start outgoing call sound");
        mediaPlayer.start();
    }

    private static void incomingCallSound(AudioManager audioManager, MediaPlayer mediaPlayer) {
        if (audioManager == null || mediaPlayer == null || (mediaPlayer != null && mediaPlayer.isPlaying()))
            return;

        audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamVolume(AudioManager.STREAM_RING), 0);
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (ringtoneUri == null) return;
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
        mediaPlayer.setLooping(true);

        try {
            mediaPlayer.setDataSource(MegaApplication.getInstance().getBaseContext(), ringtoneUri);
            logDebug("Preparing mediaPlayer");
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            logError("Error preparing mediaPlayer", e);
            return;
        }
        logDebug("Start incoming call sound");
        mediaPlayer.start();
    }

    private static void checkVibration(AudioManager audioManager, Vibrator vibrator) {
        logDebug("Ringer mode: " + audioManager.getRingerMode() + ", Stream volume: " + audioManager.getStreamVolume(AudioManager.STREAM_RING)+", Voice call volume: "+audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));

        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
            if (vibrator == null || !vibrator.hasVibrator()) return;
            stopVibration(vibrator);
            return;
        }

        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
            startVibration(vibrator);
            return;
        }

        if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0 || audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) == 0) {
            return;
        }
        startVibration(vibrator);
    }


    private static void startVibration(Vibrator vibrator) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        logDebug("Vibration begins");
        long[] pattern = {0, 1000, 500, 500, 1000};
        vibrator.vibrate(pattern, 0);
    }

    public static void stopAudioSignals(Vibrator vibrator, MediaPlayer mediaPlayer) {
        logDebug("Stop sound and vibration");
        stopSound(mediaPlayer);
        stopVibration(vibrator);
    }

    private static void stopSound(MediaPlayer mediaPlayer) {
        try {
            if (mediaPlayer != null) {
                logDebug("Stopping sound...");
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
            }
        } catch (Exception e) {
            logWarning("Exception stopping player", e);
        }
    }

    private static void stopVibration(Vibrator vibrator) {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                logDebug("Canceling vibration...");
                vibrator.cancel();
            }
        } catch (Exception e) {
            logWarning("Exception canceling vibrator", e);
        }
    }


}
