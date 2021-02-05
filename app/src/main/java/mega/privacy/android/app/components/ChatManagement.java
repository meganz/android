package mega.privacy.android.app.components;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.listeners.ChatRoomListener;

import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

public class ChatManagement {

    private final ChatRoomListener chatRoomListener;

    private String pendingJoinLink;
    private List<String> joiningLinks = new ArrayList<>();
    private List<Long> leavingChatIds = new ArrayList<>();

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

    public void addJoiningLink(String joiningLink) {
        joiningLinks.add(joiningLink);
    }

    public void removeJoiningLink(String joiningLink) {
        joiningLinks.remove(joiningLink);
    }

    public boolean isAlreadyJoining(String joinLink) {
        return joiningLinks.contains(joinLink);
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
