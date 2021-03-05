package mega.privacy.android.app.listeners;

import android.content.Context;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.MultipleForwardChatProcessor;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class CreateChatListener extends ChatBaseListener {

    public static final int START_AUDIO_CALL = 2;
    public static final int START_VIDEO_CALL = 3;
    public static final int SEND_MESSAGES = 6;
    public static final int SEND_FILE_EXPLORER_CONTENT = 7;

    private int counter;
    private int error;
    private ArrayList<MegaChatRoom> chats;
    private ArrayList<MegaUser> usersNoChat;
    private int action;

    private long[] handles;
    private int totalCounter;
    private long idChat = MEGACHAT_INVALID_HANDLE;

    public CreateChatListener(ArrayList<MegaChatRoom> chats, ArrayList<MegaUser> usersNoChat, long fileHandle, Context context, int action) {
        super(context);

        initializeValues(chats, usersNoChat, action);
    }

    public CreateChatListener(ArrayList<MegaChatRoom> chats, ArrayList<MegaUser> usersNoChat, long[] handles, Context context, int action, long idChat) {
        super(context);

        initializeValues(chats, usersNoChat, action);
        this.handles = handles;
        this.idChat = idChat;
    }

    public CreateChatListener(ArrayList<MegaChatRoom> chats, ArrayList<MegaUser> usersNoChat, long[] handles, Context context, int action) {
        super(context);

        initializeValues(chats, usersNoChat, action);
        this.handles = handles;
    }

    /**
     * Initializes the common values of all constructors.
     *
     * @param chats       List of existing chats.
     * @param usersNoChat List of contacts without chat.
     * @param action      Action to manage.
     */
    private void initializeValues(ArrayList<MegaChatRoom> chats, ArrayList<MegaUser> usersNoChat, int action) {
        this.counter = usersNoChat.size();
        this.totalCounter = chats != null && !chats.isEmpty() ? usersNoChat.size() + chats.size() : this.counter;
        this.chats = chats;
        this.usersNoChat = usersNoChat;
        this.action = action;
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
            case START_AUDIO_CALL:
            case START_VIDEO_CALL:
                if (e.getErrorCode() != MegaError.API_OK) {
                    showSnackbar(context, context.getString(R.string.create_chat_error));
                } else {
                    MegaApplication.setUserWaitingForCall(usersNoChat.get(0).getHandle());
                    MegaApplication.setIsWaitingForCall(true);
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
        }
    }
}
