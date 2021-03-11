package mega.privacy.android.app.components;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.listeners.ChatRoomListener;

import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_JOINED_SUCCESSFULLY;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

public class ChatManagement {

    private final ChatRoomListener chatRoomListener;

    // If this has a valid value, means there is a pending chat link to join.
    private String pendingJoinLink;
    // List of chat ids to control if a chat is already joining.
    private final List<Long> joiningChatIds = new ArrayList<>();
    // List of chat ids to control if a chat is already leaving.
    private final List<Long> leavingChatIds = new ArrayList<>();

    public ChatManagement() {
        chatRoomListener = new ChatRoomListener();
    }

    /**
     * Method for open a particular chatRoom.
     *
     * @param chatId The chat ID.
     * @return True, if it has been done correctly. False if not.
     */
    public boolean openChatRoom(long chatId) {
        closeChatRoom(chatId);
        return MegaApplication.getInstance().getMegaChatApi().openChatRoom(chatId, chatRoomListener);
    }

    /**
     * Method for close a particular chatRoom.
     *
     * @param chatId The chat ID.
     */
    private void closeChatRoom(long chatId) {
        MegaApplication.getInstance().getMegaChatApi().closeChatRoom(chatId, chatRoomListener);
    }

    public void setPendingJoinLink(String link) {
        pendingJoinLink = link;
    }

    public String getPendingJoinLink() {
        return pendingJoinLink;
    }

    public boolean isPendingJoinLink() {
        return !isTextEmpty(pendingJoinLink);
    }

    public void addJoiningChatId(long joiningChatId) {
        joiningChatIds.add(joiningChatId);
    }

    public void removeJoiningChatId(long joiningChatId) {
        if (joiningChatIds.remove(joiningChatId)) {
            MegaApplication.getInstance().sendBroadcast(new Intent(BROADCAST_ACTION_JOINED_SUCCESSFULLY));
        }
    }

    public boolean isAlreadyJoining(long joinChatId) {
        return joiningChatIds.contains(joinChatId);
    }

    public void addLeavingChatId(long leavingChatId) {
        leavingChatIds.add(leavingChatId);
    }

    public void removeLeavingChatId(long leavingChatId) {
        leavingChatIds.remove(leavingChatId);
    }

    public boolean isAlreadyLeaving(long leaveChatId) {
        return leavingChatIds.contains(leaveChatId);
    }
}
