package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class CallUtil {

    /*Method to know if i'm participating in any A/V call*/
    public static boolean participatingInACall(MegaChatApiAndroid megaChatApi) {
        if (megaChatApi == null) return false;
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

    /*Method to know the chat id which A / V call I am participating in*/
    public static long getChatCallInProgress(MegaChatApiAndroid megaChatApi) {
        if (megaChatApi != null) {
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
        }
        return -1;
    }

    /*Method to return to the call which I am participating*/
    public static void returnCall(Context context, MegaChatApiAndroid megaChatApi) {
        if ((megaChatApi == null) || (megaChatApi.getChatCall(getChatCallInProgress(megaChatApi)) == null))
            return;
        long chatId = getChatCallInProgress(megaChatApi);
        MegaChatCall call = megaChatApi.getChatCall(chatId);
        MegaApplication.setShowPinScreen(false);
        Intent intent = new Intent(context, ChatCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(CHAT_ID, chatId);
        intent.putExtra(CALL_ID, call.getId());
        context.startActivity(intent);

    }

    /*Method to show or hide the "Tap to return to call" banner*/
    public static void showCallLayout(Context context, MegaChatApiAndroid megaChatApi, final RelativeLayout callInProgressLayout, final Chronometer callInProgressChrono, final TextView callInProgressText) {
        if (megaChatApi == null || callInProgressLayout == null) return;
        if (!participatingInACall(megaChatApi)) {
            callInProgressLayout.setVisibility(View.GONE);
            activateChrono(false, callInProgressChrono, null);
            return;
        }

        long chatId = getChatCallInProgress(megaChatApi);
        if (chatId == -1) return;

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

    /*Method to know if I come from a call reconnection to show the layout of returning to the call*/
    public static boolean isAfterReconnecting(Context context, RelativeLayout layout, final TextView reconnectingText) {
        return layout != null && layout.getVisibility() == View.VISIBLE && reconnectingText.getText().toString().equals(context.getString(R.string.reconnecting_message));
    }

    /*Method to know if a call is established*/
    public static boolean isEstablishedCall(MegaChatApiAndroid megaChatApi, long chatId) {

        if ((megaChatApi == null) || (megaChatApi.getChatCall(chatId) == null)) return false;
        MegaChatCall call = megaChatApi.getChatCall(chatId);
        return (call.getStatus() <= MegaChatCall.CALL_STATUS_REQUEST_SENT) || (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING) || (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS);
    }

    public static void activateChrono(boolean activateChrono, final Chronometer chronometer, MegaChatCall callChat) {
        if (chronometer == null) return;
        if (!activateChrono) {
            chronometer.stop();
            chronometer.setVisibility(View.GONE);
            return;
        }
        if (callChat != null) {
            chronometer.setVisibility(View.VISIBLE);
            chronometer.setBase(SystemClock.elapsedRealtime() - (callChat.getDuration() * 1000));
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

        finalTime = finalTime + minutesString + ":" + secondsString;
        return finalTime;
    }

    public static void showErrorAlertDialogGroupCall(String message, final boolean finish, final Activity activity) {
        if (activity == null) {
            return;
        }

        try {
            android.app.AlertDialog.Builder dialogBuilder = getCustomAlertBuilder(activity, activity.getString(R.string.general_error_word), message, null);
            dialogBuilder.setPositiveButton(
                    activity.getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (finish) {
                                activity.finishAndRemoveTask();
                            }
                        }
                    });
            dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (finish) {
                        activity.finishAndRemoveTask();
                    }
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
            case MegaChatCall.CALL_STATUS_INITIAL: {
                return "CALL_STATUS_INITIAL";
            }
            case MegaChatCall.CALL_STATUS_HAS_LOCAL_STREAM: {
                return "CALL_STATUS_HAS_LOCAL_STREAM";
            }
            case MegaChatCall.CALL_STATUS_REQUEST_SENT: {
                return "CALL_STATUS_REQUEST_SENT";
            }
            case MegaChatCall.CALL_STATUS_RING_IN: {
                return "CALL_STATUS_RING_IN";
            }
            case MegaChatCall.CALL_STATUS_JOINING: {
                return "CALL_STATUS_JOINING";
            }
            case MegaChatCall.CALL_STATUS_IN_PROGRESS: {
                return "CALL_STATUS_IN_PROGRESS";
            }
            case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION: {
                return "CALL_STATUS_TERMINATING_USER_PARTICIPATION";
            }
            case MegaChatCall.CALL_STATUS_DESTROYED: {
                return "CALL_STATUS_DESTROYED";
            }
            case MegaChatCall.CALL_STATUS_USER_NO_PRESENT: {
                return "CALL_STATUS_USER_NO_PRESENT";
            }
            case MegaChatCall.CALL_STATUS_RECONNECTING: {
                return "CALL_STATUS_RECONNECTING";
            }
            default:
                return String.valueOf(status);
        }
    }

    public static boolean isStatusConnected(Context context, MegaChatApiAndroid megaChatApi, long chatId) {
        return checkConnection(context) && megaChatApi != null && megaChatApi.getConnectionState() == MegaChatApi.CONNECTED && megaChatApi.getChatConnectionState(chatId) == MegaChatApi.CHAT_CONNECTION_ONLINE;
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

    private static void disableLocalCamera(MegaChatApiAndroid megaChatApi) {
        if (megaChatApi == null) return;
        long idCall = isNecessaryDisableLocalCamera(megaChatApi);
        if (idCall != -1)
            megaChatApi.disableVideo(idCall, null);
    }

    public static long isNecessaryDisableLocalCamera(MegaChatApiAndroid megaChatApi) {
        long noVideo = -1;
        if (megaChatApi == null) return noVideo;
        long chatIdCallInProgress = getChatCallInProgress(megaChatApi);
        MegaChatCall callInProgress = megaChatApi.getChatCall(chatIdCallInProgress);
        if (callInProgress == null || !callInProgress.hasLocalVideo()) return noVideo;
        return chatIdCallInProgress;
    }

    public static void showConfirmationOpenCamera(Activity activity, MegaChatApiAndroid megaChatApi, String action) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE: {
                    logDebug("Open camera and lost the camera in the call");
                    disableLocalCamera(megaChatApi);
                    if (activity instanceof ChatActivityLollipop && action.equals(ACTION_TAKE_PICTURE))
                        ((ChatActivityLollipop) activity).controlCamera();
                    if (activity instanceof ManagerActivityLollipop) {
                        if (action.equals(ACTION_OPEN_QR))
                            ((ManagerActivityLollipop) activity).openQA();
                        if (action.equals(ACTION_TAKE_PICTURE)) takePicture(activity);

                    }
                    break;
                }
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle);
        String message = activity.getString(R.string.confirmation_open_camera_on_chat);
        builder.setTitle(R.string.title_confirmation_open_camera_on_chat);
        builder.setMessage(message).setPositiveButton(R.string.context_open_link, dialogClickListener).setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

}
