package mega.privacy.android.app.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;

import static android.view.View.GONE;

public class ChatUtil {

    /*Method to know if i'm participating in any A/V call*/
    public static boolean participatingInACall(MegaChatApiAndroid megaChatApi){
        log("participatingInACall");
        if(megaChatApi == null )return false;
        MegaHandleList listCallsRequestSent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_REQUEST_SENT);
        MegaHandleList listCallsUserNoPresent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_USER_NO_PRESENT);
        MegaHandleList listCallsRingIn = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RING_IN);
        MegaHandleList listCallsDestroy = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_DESTROYED);

        MegaHandleList listCalls = megaChatApi.getChatCalls();
        log("participatingInACall: No calls in progress ");


        if((listCalls.size() - listCallsDestroy.size()) == 0)return false;
        log("participatingInACall:There is some call in progress");

        if((listCalls.size() - listCallsDestroy.size()) == (listCallsUserNoPresent.size() + listCallsRingIn.size())) {
            log("participatingInACall:I'm not participating in any of the calls there");
            return false;
        }
        if(listCallsRequestSent.size()>0){
            log("participatingInACall: I'm doing a outgoing call");
            return true;
        }
        log("participatingInACall:I'm in a call in progress");
        return true;

    }

    /*Method to know the chat id which A / V call I am participating in*/
    public static long getChatCallInProgress(MegaChatApiAndroid megaChatApi){
        log("getChatCallInProgress()");
        long chatId = -1;
        if(megaChatApi!=null){
            MegaHandleList listCallsRequestSent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_REQUEST_SENT);
            if((listCallsRequestSent!=null) && (listCallsRequestSent.size() > 0)){
                log("getChatCallInProgress: Request Sent");
                //Return to request sent
                chatId = listCallsRequestSent.get(0);
            }else{
                log("getChatCallInProgress: NOT Request Sent");
                MegaHandleList listCallsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS);
                if((listCallsInProgress!=null) && (listCallsInProgress.size() > 0)){
                    //Return to in progress
                    log("getChatCallInProgress: In progress");
                    chatId = listCallsInProgress.get(0);
                }
            }
        }
        return chatId;
    }

    /*Method to return to the call which I am participating*/
    public static void returnCall(Context context, MegaChatApiAndroid megaChatApi){
        log("returnCall()");
        if((megaChatApi == null) || (megaChatApi.getChatCall(getChatCallInProgress(megaChatApi)) == null)) return;
        long chatId = getChatCallInProgress(megaChatApi);
        MegaChatCall call = megaChatApi.getChatCall(chatId);
        MegaApplication.setShowPinScreen(false);
        Intent intent = new Intent(context, ChatCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("chatHandle", chatId);
        intent.putExtra("callId", call.getId());
        context.startActivity(intent);

    }

    /*MEthod to show or hide the "return the call" layout*/
    public static void showCallLayout(Context context, MegaChatApiAndroid megaChatApi, RelativeLayout callInProgressLayout, Chronometer callInProgressChrono){
        if(!Util.isChatEnabled() || megaChatApi == null || !(context instanceof ManagerActivityLollipop) || !participatingInACall(megaChatApi) ){
            callInProgressLayout.setVisibility(View.GONE);
            activateChrono(false, callInProgressChrono, null);
            return;
        }
        callInProgressLayout.setVisibility(View.VISIBLE);

        long chatId = getChatCallInProgress(megaChatApi);
        if(chatId == -1)return;
        MegaChatCall call = megaChatApi.getChatCall(chatId);
        if(call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
            activateChrono(true, callInProgressChrono, call);
            return;
        }
        activateChrono(false, callInProgressChrono, null);
    }

    /*Method to know if a call is established*/
    public static boolean isEstablishedCall(MegaChatApiAndroid megaChatApi, long chatId){

        if((megaChatApi == null) || (megaChatApi.getChatCall(chatId) == null)) return false;
        MegaChatCall call = megaChatApi.getChatCall(chatId);
        if((call.getStatus() <= MegaChatCall.CALL_STATUS_REQUEST_SENT) || (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING) || (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS)) return true;
        return false;
    }
    public static void log(String origin, String message) {
        MegaApiAndroid.log(MegaApiAndroid.LOG_LEVEL_WARNING, "[clientApp] "+ origin + ": " + message, origin);
    }

    public static void showShareChatLinkDialog (final Context context, MegaChatRoom chat, final String chatLink) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        LayoutInflater inflater = null;
        if (context instanceof GroupChatInfoActivityLollipop) {
            inflater = ((GroupChatInfoActivityLollipop) context).getLayoutInflater();
        }
        else if (context instanceof ChatActivityLollipop){
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
                        }
                        else if (context instanceof ChatActivityLollipop) {
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
        }
        else {
            deleteButton.setVisibility(View.GONE);
        }
        deleteButton.setOnClickListener(clickListener);
        Button dismissButton = (Button) v.findViewById(R.id.dismiss_button);
        dismissButton.setOnClickListener(clickListener);

        shareLinkDialog.setCancelable(false);
        shareLinkDialog.setCanceledOnTouchOutside(false);
        try {
            shareLinkDialog.show();
        }catch (Exception e){}
    }

    static void dismissShareChatLinkDialog(Context context, AlertDialog shareLinkDialog) {
        try {
            shareLinkDialog.dismiss();
            if (context instanceof ChatActivityLollipop) {
                ((ChatActivityLollipop) context).setShareLinkDialogDismissed(true);
            }
        } catch (Exception e) {}
    }

    public static void showConfirmationRemoveChatLink (final Context context){
        log("showConfirmationRemoveChatLink");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if (context instanceof GroupChatInfoActivityLollipop) {
                            ((GroupChatInfoActivityLollipop) context).removeChatLink();
                        }
                        else if (context instanceof ChatActivityLollipop){
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

    public static void activateChrono(boolean activateChrono, Chronometer chronometer, MegaChatCall callChat){
        if(activateChrono && callChat!=null && chronometer!=null && chronometer.getVisibility() == GONE){
            chronometer.setVisibility(View.VISIBLE);
            chronometer.setBase(SystemClock.elapsedRealtime() - (callChat.getDuration()*1000));
            chronometer.start();
            chronometer.setFormat(" %s");
        }else if(chronometer!=null){
            chronometer.stop();
            chronometer.setVisibility(View.GONE);
        }
    }

    private static void log(String message) {
        log("UtilChat", message);
    }

}
