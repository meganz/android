package mega.privacy.android.app.utils;

import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaHandleList;

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

    public static void log(String origin, String message) {
        MegaApiAndroid.log(MegaApiAndroid.LOG_LEVEL_WARNING, "[clientApp] "+ origin + ": " + message, origin);
    }

    private static void log(String message) {
        log("UtilChat", message);
    }

}
