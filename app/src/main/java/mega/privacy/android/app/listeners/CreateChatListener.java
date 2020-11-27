package mega.privacy.android.app.listeners;

import android.content.Context;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.MultipleForwardChatProcessor;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.MESSAGE_SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class CreateChatListener extends ChatBaseListener {

    public static final int SEND_FILE = 1;
    public static final int START_AUDIO_CALL = 2;
    public static final int START_VIDEO_CALL = 3;
    public static final int SEND_FILES = 4;
    public static final int SEND_CONTACTS = 5;
    public static final int SEND_MESSAGES = 6;
    public static final int SEND_FILE_EXPLORER_CONTENT = 7;
    public static final int SEND_LINK = 8;

    private int counter;
    private int error;
    private String message;
    private ArrayList<MegaChatRoom> chats;
    private ArrayList<MegaUser> usersNoChat;
    private long fileHandle;
    private int action;

    private long[] handles;
    private int totalCounter;
    private long idChat;
    private String link;

    public CreateChatListener(ArrayList<MegaChatRoom> chats, ArrayList<MegaUser> usersNoChat, long fileHandle, Context context, int action) {
        super(context);
        this.counter = usersNoChat.size();
        this.chats = chats;
        this.usersNoChat = usersNoChat;
        this.fileHandle = fileHandle;
        this.action = action;
    }

    public CreateChatListener(ArrayList<MegaChatRoom> chats, ArrayList<MegaUser> usersNoChat, long[] handles, Context context, int action, long idChat) {
        super(context);
        this.counter = usersNoChat.size();
        if (chats != null && !chats.isEmpty()) {
            this.totalCounter = usersNoChat.size() + chats.size();
        } else {
            this.totalCounter = this.counter;
        }
        this.chats = chats;
        this.usersNoChat = usersNoChat;
        this.handles = handles;
        this.action = action;
        this.idChat = idChat;
    }

    public CreateChatListener(ArrayList<MegaChatRoom> chats, ArrayList<MegaUser> usersNoChat, long[] handles, Context context, int action) {
        super(context);
        this.counter = usersNoChat.size();
        if (chats != null && !chats.isEmpty()) {
            this.totalCounter = usersNoChat.size() + chats.size();
        } else {
            this.totalCounter = this.counter;
        }
        this.chats = chats;
        this.usersNoChat = usersNoChat;
        this.handles = handles;
        this.action = action;
        this.idChat = MEGACHAT_INVALID_HANDLE;
    }

    public CreateChatListener(ArrayList<MegaChatRoom> chats, ArrayList<MegaUser> usersNoChat, String link, Context context, int action) {
        super(context);
        this.counter = usersNoChat.size();

        if (chats != null && !chats.isEmpty()) {
            this.totalCounter = usersNoChat.size() + chats.size();
        } else {
            this.totalCounter = this.counter;
        }

        this.chats = chats;
        this.usersNoChat = usersNoChat;
        this.link = link;
        this.action = action;
        this.idChat = MEGACHAT_INVALID_HANDLE;
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logDebug("Error code: " + e.getErrorCode());

        if (request.getType() != MegaChatRequest.TYPE_CREATE_CHATROOM) return;

        counter--;

        if (e.getErrorCode() != MegaError.API_OK) {
            error++;
        } else {
            if (chats == null) {
                chats = new ArrayList<>();
            }
            MegaChatRoom chat = api.getChatRoom(request.getChatHandle());
            if (chat != null) {
                chats.add(chat);
            }
        }

        if (counter > 0) return;

        switch (action) {
            case SEND_FILE:
                if (usersNoChat.size() == error && (chats == null || chats.isEmpty())) {
                    //All send files fail
                    if (context instanceof ManagerActivityLollipop || context instanceof ContactInfoActivityLollipop) {
                        message = context.getResources().getString(R.string.number_no_sent, error);
                    } else {
                        message = context.getResources().getQuantityString(R.plurals.num_files_not_send, handles.length, totalCounter);
                    }

                    showSnackbar(context, message);
                } else {
                    ChatController.sendFileToChatsFromContacts(context, chats, fileHandle);
                }
                break;

            case START_AUDIO_CALL:
            case START_VIDEO_CALL:
                if (e.getErrorCode() != MegaError.API_OK) {
                    showSnackbar(context, context.getString(R.string.create_chat_error));
                } else {
                    MegaApplication.setUserWaitingForCall(usersNoChat.get(0).getHandle());
                    MegaApplication.setIsWaitingForCall(true);
                }
                break;

            case SEND_FILES:
                if ((usersNoChat.size() == error) && (chats == null || chats.isEmpty())) {
                    //All send files fail; Show error
                    showSnackbar(context, context.getResources().getQuantityString(R.plurals.num_files_not_send, handles.length, totalCounter));
                } else {
                    //Send files
                    new ChatController(context).checkIfNodesAreMineAndAttachNodes(handles, getChatHandles());
                }
                break;

            case SEND_CONTACTS:
                if ((usersNoChat.size() == error) && (chats == null || chats.isEmpty())) {
                    //All send contacts fail; Show error
                    showSnackbar(context, context.getResources().getQuantityString(R.plurals.num_contacts_not_send, handles.length, totalCounter));
                } else {
                    new ChatController(context).sendContactsToChats(getChatHandles(), handles);
                }
                break;

            case SEND_MESSAGES:
                if ((usersNoChat.size() == error) && (chats == null || chats.isEmpty())) {
                    //All send messages fail; Show error
                    showSnackbar(context, context.getResources().getQuantityString(R.plurals.num_messages_not_send, handles.length, totalCounter));
                } else {
                    //Send messages
                    long[] chatHandles = new long[chats.size()];
                    for (int i = 0; i < chats.size(); i++) {
                        chatHandles[i] = chats.get(i).getChatId();
                    }
                    MultipleForwardChatProcessor forwardChatProcessor = new MultipleForwardChatProcessor(context, chatHandles, handles, idChat);
                    forwardChatProcessor.forward(api.getChatRoom(idChat));
                }
                break;

            case SEND_FILE_EXPLORER_CONTENT:
                if ((usersNoChat.size() == error) && (chats == null || chats.isEmpty())) {
                    //All send messages fail; Show error
                    showSnackbar(context, context.getResources().getString(R.string.content_not_send, totalCounter));
                } else {
                    //Send content
                    if (context instanceof FileExplorerActivityLollipop) {
                        ((FileExplorerActivityLollipop) context).sendToChats(chats);
                    }
                }
                break;

            case SEND_LINK:
                if (usersNoChat.size() == error && (chats == null || chats.isEmpty())) {
                    //All send messages fail; Show error
                    showSnackbar(context, context.getResources().getString(R.string.content_not_send, totalCounter));
                } else {
                    ChatController.sendLinkToChats(context, getChatHandles(), link);
                }
        }
    }

    private long[] getChatHandles() {
        long[] chatHandles = new long[chats.size()];

        for (int i = 0; i < chats.size(); i++) {
            chatHandles [i] = chats.get(i).getChatId();
        }

        return chatHandles;
    }
}
