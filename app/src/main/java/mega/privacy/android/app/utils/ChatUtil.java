package mega.privacy.android.app.utils;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaHandleList;

public class ChatUtil {
    public static final int MAX_ALLOWED_CHARACTERS_AND_EMOJIS = 27;


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
    public static void log(String origin, String message) {
        MegaApiAndroid.log(MegaApiAndroid.LOG_LEVEL_WARNING, "[clientApp] "+ origin + ": " + message, origin);
    }

    public static String charWithoutInvalidCharacters(@Nullable final CharSequence text){
        String result = text.toString();
        int position = -1;
        for (int i = (text.length()-1); i >= 0; i--){
            if((!Character.isLetterOrDigit(text.charAt(i)))&&(!Character.isWhitespace(text.charAt(i)))){
                CharSequence lastChars = text.subSequence((i-2), i);
                int numEmojis = EmojiManager.getInstance().getNumEmojis(lastChars);
                if(numEmojis>0){
                    position = i;
                    break;
                }
            }else{
                position = i;
                break;
            }
        }

        if(position != -1){
            result = text.subSequence(0, position).toString();
        }
        return result;
    }

    public static int getMaxAllowed(@Nullable final CharSequence text){
        int numEmojis = EmojiManager.getInstance().getNumEmojis(text);
        if(numEmojis > 0){
            int realLenght = ((text.length() - (numEmojis*2)) + (numEmojis*4));
            if(realLenght>=MAX_ALLOWED_CHARACTERS_AND_EMOJIS){
                return text.length();
//                if(realLenght>=30){
//                    String newTitle = ChatUtil.charWithoutInvalidCharacters(text);
//                    return newTitle.length();
//                }else{
//                    return text.length();
//                }
            }else{
                return MAX_ALLOWED_CHARACTERS_AND_EMOJIS;
            }
        }else{
            return MAX_ALLOWED_CHARACTERS_AND_EMOJIS;
        }
    }

    public static String getFirstLetter(String title){
        String result = "";
        String resultTitle = EmojiUtilsShortcodes.emojify(title);
        resultTitle = resultTitle.trim();

        if(!resultTitle.isEmpty()){
            String lastEmoji = resultTitle.substring(0,2);
            int numEmojis = EmojiManager.getInstance().getNumEmojis(lastEmoji);
            if(numEmojis >0 ){
                result = lastEmoji;
            }else{
                result = String.valueOf(resultTitle.charAt(0));
                result = result.toUpperCase(Locale.getDefault());
            }
        }
        return result;
    }



    private static void log(String message) {
        log("ChatUtil", message);
    }

}
