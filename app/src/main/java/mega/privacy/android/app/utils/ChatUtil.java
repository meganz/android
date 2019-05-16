package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.io.File;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;

public class ChatUtil {

    /*Method to know if i'm participating in any A/V call*/
    public static boolean participatingInACall(MegaChatApiAndroid megaChatApi){
        boolean activeCall = false;
        if(megaChatApi!=null){
            MegaHandleList listCallsUserNoPresent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_USER_NO_PRESENT);
            MegaHandleList listCallsRingIn = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RING_IN);
            MegaHandleList listCalls = megaChatApi.getChatCalls();
            if((listCallsUserNoPresent!=null)&&(listCallsRingIn!=null)&&(listCalls!=null)){
                long totalCallsNotPresent = listCallsUserNoPresent.size() + listCallsRingIn.size();
                if(totalCallsNotPresent == listCalls.size()){
                    activeCall = false;
                }else{
                    activeCall = true;
                }
            }
        }
        return activeCall;
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
        if(megaChatApi!=null){
            long chatId = getChatCallInProgress(megaChatApi);
            MegaChatCall call = megaChatApi.getChatCall(chatId);
            if(call!=null){
                MegaApplication.setShowPinScreen(false);
                Intent intent = new Intent(context, ChatCallActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("chatHandle", chatId);
                intent.putExtra("callId", call.getId());
                context.startActivity(intent);
            }
        }
    }

    /*Method to know if a call is established*/
    public static boolean isEstablishedCall(MegaChatApiAndroid megaChatApi, long chatId){
        if(megaChatApi!=null){
            MegaChatCall call = megaChatApi.getChatCall(chatId);
            if(call != null){
                if((call.getStatus() <= MegaChatCall.CALL_STATUS_REQUEST_SENT) || (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING) || (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS)){
                    return true;
                }else{
                    return false;

                }
            }
        }
        return false;
    }

    /* Get the corresponding path, Mega Downloads or Mega Voice Notes*/
    public static String getDefaultLocationPath(Context context, boolean isVoiceNote){
        String locationPath;
        if(isVoiceNote){
            locationPath = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.voiceNotesDIR;
        }else{
            locationPath = Util.getDownloadLocation(context);
        }
        return locationPath;
    }

    /* Know if a voice note is downloaded in the Mega Voice Notes folder*/
    public static boolean isInMegaVoiceNotes(Context context, MegaNode node){
            String voiceNotesLocationDefaultPath = ChatUtil.getDefaultLocationPath(context, true);
            File f = new File(voiceNotesLocationDefaultPath, node.getName());
            boolean result = false;
            if((f.exists())&&(f.length() == node.getSize())){
                result = true;
            }
            return result;
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

    public static void log(String origin, String message) {
        MegaApiAndroid.log(MegaApiAndroid.LOG_LEVEL_WARNING, "[clientApp] "+ origin + ": " + message, origin);
    }

    private static void log(String message) {
        log("UtilChat", message);
    }

}
