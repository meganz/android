package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiRange;
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.interfaces.MyChatFilesExisitListener;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ChatUtil {

    /*Method to know if i'm participating in any A/V call*/
    public static boolean participatingInACall(MegaChatApiAndroid megaChatApi) {
        logDebug("participatingInACall");
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
        logDebug("getChatCallInProgress()");
        long chatId = -1;
        if (megaChatApi != null) {
            MegaHandleList listCallsRequestSent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_REQUEST_SENT);
            if ((listCallsRequestSent != null) && (listCallsRequestSent.size() > 0)) {
                logDebug("Request Sent");
                //Return to request sent
                chatId = listCallsRequestSent.get(0);
            } else {
                logDebug("NOT Request Sent");
                MegaHandleList listCallsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS);
                if ((listCallsInProgress != null) && (listCallsInProgress.size() > 0)) {
                    //Return to in progress
                    logDebug("In progress");
                    chatId = listCallsInProgress.get(0);
                }
            }
        }
        return chatId;
    }

    /*Method to return to the call which I am participating*/
    public static void returnCall(Context context, MegaChatApiAndroid megaChatApi) {
        logDebug("returnCall()");
        if ((megaChatApi == null) || (megaChatApi.getChatCall(getChatCallInProgress(megaChatApi)) == null))
            return;
        long chatId = getChatCallInProgress(megaChatApi);
        MegaChatCall call = megaChatApi.getChatCall(chatId);
        MegaApplication.setShowPinScreen(false);
        Intent intent = new Intent(context, ChatCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.CHAT_ID, chatId);
        intent.putExtra(Constants.CALL_ID, call.getId());
        context.startActivity(intent);

    }

    /*Method to show or hide the "return the call" layout*/
    public static void showCallLayout(Context context, MegaChatApiAndroid megaChatApi, final RelativeLayout callInProgressLayout, final Chronometer callInProgressChrono) {
        if (megaChatApi == null || callInProgressLayout == null) return;
        logDebug("showCallLayout");

        if (!Util.isChatEnabled() || !(context instanceof ManagerActivityLollipop) || !participatingInACall(megaChatApi)) {
            callInProgressLayout.setVisibility(View.GONE);
            activateChrono(false, callInProgressChrono, null);
            return;
        }
        callInProgressLayout.setVisibility(View.VISIBLE);

        long chatId = getChatCallInProgress(megaChatApi);
        if (chatId == -1) return;
        MegaChatCall call = megaChatApi.getChatCall(chatId);
        if (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
            activateChrono(true, callInProgressChrono, call);
            return;
        }
        activateChrono(false, callInProgressChrono, null);
    }

    /*Method to know if a call is established*/
    public static boolean isEstablishedCall(MegaChatApiAndroid megaChatApi, long chatId) {

        if ((megaChatApi == null) || (megaChatApi.getChatCall(chatId) == null)) return false;
        MegaChatCall call = megaChatApi.getChatCall(chatId);
        if ((call.getStatus() <= MegaChatCall.CALL_STATUS_REQUEST_SENT) || (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING) || (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS))
            return true;
        return false;
    }

    public static boolean isVoiceClip(String name) {
        return MimeTypeList.typeForName(name).isAudioVoiceClip();
    }

    public static long getVoiceClipDuration(MegaNode node) {
        return node.getDuration() <= 0 ? 0 : node.getDuration() * 1000;
    }

    /* Get the height of the action bar */
    public static int getActionBarHeight(Activity activity, Resources resources) {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (activity != null && activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, resources.getDisplayMetrics());
        }
        return actionBarHeight;
    }

    public static int getMaxAllowed(@Nullable final CharSequence text) {
        int numEmojis = EmojiManager.getInstance().getNumEmojis(text);
        if (numEmojis > 0) {
            int realLenght = text.length() + (numEmojis * 2);
            if (realLenght >= MAX_ALLOWED_CHARACTERS_AND_EMOJIS) return text.length();
        }
        return MAX_ALLOWED_CHARACTERS_AND_EMOJIS;
    }

    public static String getFirstLetter(String title) {

        if(title.isEmpty()) return "";
        title = title.trim();
        if(title.length() == 1) return String.valueOf(title.charAt(0)).toUpperCase(Locale.getDefault());

        String resultTitle = EmojiUtilsShortcodes.emojify(title);
        List<EmojiRange> emojis = EmojiManager.getInstance().findAllEmojis(resultTitle);
        if(emojis.size() > 0 && emojis.get(0).start == 0) return resultTitle.substring(emojis.get(0).start, emojis.get(0).end);

        return String.valueOf(resultTitle.charAt(0)).toUpperCase(Locale.getDefault());
    }

    public static void showShareChatLinkDialog (final Context context, MegaChatRoom chat, final String chatLink) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        LayoutInflater inflater = null;
        if (context instanceof GroupChatInfoActivityLollipop) {
            inflater = ((GroupChatInfoActivityLollipop) context).getLayoutInflater();
        } else if (context instanceof ChatActivityLollipop) {
            inflater = ((ChatActivityLollipop) context).getLayoutInflater();
        }
        View v = inflater.inflate(R.layout.chat_link_share_dialog, null);
        builder.setView(v);
        final AlertDialog shareLinkDialog = builder.create();

        TextView nameGroup = (TextView) v.findViewById(R.id.group_name_text);
        nameGroup.setText(chat.getTitle());
        TextView chatLinkText = (TextView) v.findViewById(R.id.chat_link_text);
        chatLinkText.setText(chatLink);

        final boolean isModerator = chat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR ? true : false;

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.copy_button: {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", chatLink);
                        clipboard.setPrimaryClip(clip);
                        if (context instanceof GroupChatInfoActivityLollipop) {
                            ((GroupChatInfoActivityLollipop) context).showSnackbar(context.getString(R.string.chat_link_copied_clipboard));
                        } else if (context instanceof ChatActivityLollipop) {
                            ((ChatActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.chat_link_copied_clipboard), -1);

                        }
                        dismissShareChatLinkDialog(context, shareLinkDialog);
                        break;
                    }
                    case R.id.share_button: {
                        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, chatLink);
                        context.startActivity(Intent.createChooser(sharingIntent, context.getString(R.string.context_share)));
                        dismissShareChatLinkDialog(context, shareLinkDialog);
                        break;
                    }
                    case R.id.delete_button: {
                        if (isModerator) {
                            showConfirmationRemoveChatLink(context);
                        }
                        dismissShareChatLinkDialog(context, shareLinkDialog);
                        break;
                    }
                    case R.id.dismiss_button: {
                        dismissShareChatLinkDialog(context, shareLinkDialog);
                        break;
                    }
                }
            }
        };

        Button copyButton = (Button) v.findViewById(R.id.copy_button);
        copyButton.setOnClickListener(clickListener);
        Button shareButton = (Button) v.findViewById(R.id.share_button);
        shareButton.setOnClickListener(clickListener);
        Button deleteButton = (Button) v.findViewById(R.id.delete_button);
        if (isModerator) {
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.GONE);
        }
        deleteButton.setOnClickListener(clickListener);
        Button dismissButton = (Button) v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(clickListener);

        shareLinkDialog.setCancelable(false);
        shareLinkDialog.setCanceledOnTouchOutside(false);
        try {
            shareLinkDialog.show();
        } catch (Exception e) {
        }
    }

    private static void dismissShareChatLinkDialog(Context context, AlertDialog shareLinkDialog) {
        try {
            shareLinkDialog.dismiss();
            if (context instanceof ChatActivityLollipop) {
                ((ChatActivityLollipop) context).setShareLinkDialogDismissed(true);
            }
        } catch (Exception e) {
        }
    }

    public static void showConfirmationRemoveChatLink(final Context context) {
        logDebug("showConfirmationRemoveChatLink");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if (context instanceof GroupChatInfoActivityLollipop) {
                            ((GroupChatInfoActivityLollipop) context).removeChatLink();
                        } else if (context instanceof ChatActivityLollipop) {
                            ((ChatActivityLollipop) context).removeChatLink();
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context);
        builder.setTitle(R.string.action_delete_link);
        builder.setMessage(R.string.context_remove_chat_link_warning_text).setPositiveButton(R.string.delete_button, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public static MegaChatMessage getMegaChatMessage(Context context, MegaChatApiAndroid megaChatApi, long chatId, long messageId) {
        if (context instanceof NodeAttachmentHistoryActivity) {
            return megaChatApi.getMessageFromNodeHistory(chatId, messageId);
        } else {
            return megaChatApi.getMessage(chatId, messageId);
        }

    }

    public static void activateChrono(boolean activateChrono, final Chronometer chronometer, MegaChatCall callChat) {
        if (chronometer == null || callChat == null) return;
        if (activateChrono) {
            chronometer.setVisibility(View.VISIBLE);
            chronometer.setBase(SystemClock.elapsedRealtime() - (callChat.getDuration() * 1000));
            chronometer.start();
            chronometer.setFormat(" %s");
            return;
        }
        chronometer.stop();
        chronometer.setVisibility(View.GONE);
    }

    /**
     * Locks the device window in landscape mode.
     */
    public static void lockOrientationLandscape(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    /**
     * Locks the device window in reverse landscape mode.
     */
    public static void lockOrientationReverseLandscape(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    /**
     * Locks the device window in portrait mode.
     */
    public static void lockOrientationPortrait(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * Locks the device window in reverse portrait mode.
     */
    public static void lockOrientationReversePortrait(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
    }

    /**
     * Allows user to freely use portrait or landscape mode.
     */
    public static void unlockOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
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

    /**
     * To detect whether My Chat Files folder exist or not.
     * If no, store the passed data and process after the folder is created
     */
    public static <T> boolean existsMyChatFiles(T preservedData, MegaApiAndroid megaApi, MegaRequestListenerInterface requestListener, MyChatFilesExisitListener listener) {
        MegaNode parentNode = megaApi.getNodeByPath("/" + CHAT_FOLDER);
        if (parentNode == null) {
            megaApi.createFolder(CHAT_FOLDER, megaApi.getRootNode(), requestListener);
            listener.storedUnhandledData(preservedData);
            return false;
        }
        return true;
    }

    public static void showErrorAlertDialogGroupCall(String message, final boolean finish, final Activity activity){
        if(activity == null){
            return;
        }

        try{
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
        }catch(Exception ex){
            Util.showToast(activity, message);
        }
    }

    public static String callStatusToString(int status){
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
                return  String.valueOf(status);
        }
    }
}
