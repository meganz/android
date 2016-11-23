package mega.privacy.android.app.lollipop.controllers;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ContactChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;

public class ChatController {

    Context context;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;
    MegaPreferences prefs = null;

    public ChatController(Context context){
        log("ChatController created");
        this.context = context;
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }
        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public void leaveChat(MegaChatRoom chat){
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.leaveChat(chat.getChatId(), (ManagerActivityLollipop) context);
        }
        else if(context instanceof GroupChatInfoActivityLollipop){
            megaChatApi.leaveChat(chat.getChatId(), (GroupChatInfoActivityLollipop) context);
        }
        else if(context instanceof ChatActivityLollipop){
            megaChatApi.leaveChat(chat.getChatId(), (ChatActivityLollipop) context);
        }
    }

    public void leaveChat(long chatId){
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.leaveChat(chatId, (ManagerActivityLollipop) context);
        }
        else if(context instanceof GroupChatInfoActivityLollipop){
            megaChatApi.leaveChat(chatId, (GroupChatInfoActivityLollipop) context);
        }
        else if(context instanceof ChatActivityLollipop){
            megaChatApi.leaveChat(chatId, (ChatActivityLollipop) context);
        }
    }

    public void clearHistory(MegaChatRoom chat){
        log("clearHistory: "+chat.getTitle());
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.clearChatHistory(chat.getChatId(), (ManagerActivityLollipop) context);
        }
        else if(context instanceof ChatActivityLollipop){
            megaChatApi.clearChatHistory(chat.getChatId(), (ChatActivityLollipop) context);
        }
        else if(context instanceof ContactChatInfoActivityLollipop){
            megaChatApi.clearChatHistory(chat.getChatId(), (ContactChatInfoActivityLollipop) context);
        }
        else if(context instanceof GroupChatInfoActivityLollipop){
            megaChatApi.clearChatHistory(chat.getChatId(), (GroupChatInfoActivityLollipop) context);
        }
    }

    public void clearHistory(long chatId){
        log("clearHistory: "+chatId);
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (ManagerActivityLollipop) context);
        }
        else if(context instanceof ChatActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (ChatActivityLollipop) context);
        }
        else if(context instanceof ContactChatInfoActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (ContactChatInfoActivityLollipop) context);
        }
        else if(context instanceof GroupChatInfoActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (GroupChatInfoActivityLollipop) context);
        }
    }

    public void deleteMessages(ArrayList<AndroidMegaChatMessage> messages, MegaChatRoom chat){
        log("deleteMessages: "+messages.size());
        MegaChatMessage messageToDelete;
        if(messages!=null){
            for(int i=0; i<messages.size();i++){
                messageToDelete = megaChatApi.deleteMessage(chat.getChatId(), messages.get(i).getMessage().getMsgId());
                if(messageToDelete==null){
                    log("The message cannot be deleted");
                }
            }
        }
    }

    public void alterParticipantsPermissions(long chatid, long uh, int privilege){
        log("alterParticipantsPermissions: "+uh);
        megaChatApi.updateChatPermissions(chatid, uh, privilege, (GroupChatInfoActivityLollipop) context);
    }

    public void removeParticipant(long chatid, long uh){
        log("removeParticipant: "+uh);
        if(context==null){
            log("Context is NULL");
        }
        megaChatApi.removeFromChat(chatid, uh, (GroupChatInfoActivityLollipop) context);
    }

    public void changeTitle(long chatid, String title){
        megaChatApi.setChatTitle(chatid, title, (GroupChatInfoActivityLollipop) context);
    }

    public static void log(String message) {
        Util.log("ChatController", message);
    }
}
