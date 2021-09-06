package mega.privacy.android.app.components;

import android.content.Intent;

import androidx.preference.PreferenceManager;

import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.listeners.ChatRoomListener;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;

import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_JOINED_SUCCESSFULLY;
import static mega.privacy.android.app.constants.EventConstants.EVENT_NOT_OUTGOING_CALL;
import static mega.privacy.android.app.utils.CallUtil.clearIncomingCallNotification;
import static mega.privacy.android.app.utils.CallUtil.existsAnOngoingOrIncomingCall;
import static mega.privacy.android.app.utils.CallUtil.participatingInACall;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CALL_RINGING;
import static mega.privacy.android.app.utils.Constants.KEY_IS_SHOWED_WARNING_MESSAGE;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaChatCall.CALL_STATUS_USER_NO_PRESENT;

public class ChatManagement {

    private final ChatRoomListener chatRoomListener;
    private final MegaApplication app;

    // List of chat ids to control if a chat is already joining.
    private final List<Long> joiningChatIds = new ArrayList<>();
    // List of chat ids to control if a chat is already leaving.
    private final List<Long> leavingChatIds = new ArrayList<>();
    // List of chats with video activated in the call
    private final HashMap<Long, Boolean> hashMapVideo = new HashMap<>();
    // List of chats with speaker activated in the call
    private final HashMap<Long, Boolean> hashMapSpeaker = new HashMap<>();
    // List of outgoing calls
    private final HashMap<Long, Boolean> hashMapOutgoingCall = new HashMap<>();
    // List of chats in which a meeting is being opened via a link
    private final HashMap<Long, Boolean> hashOpeningMeetingLink = new HashMap<>();
    // List of group chats being participated in
    private final ArrayList<Long> currentActiveGroupChat = new ArrayList<>();
    // List of calls for which an incoming call notification has already been shown
    private final ArrayList<Long> notificationShown = new ArrayList<>();
    // If this has a valid value, means there is a pending chat link to join.
    private String pendingJoinLink;

    public ChatManagement() {
        chatRoomListener = new ChatRoomListener();
        app = MegaApplication.getInstance();
    }

    /**
     * Method for open a particular chatRoom.
     *
     * @param chatId The chat ID.
     * @return True, if it has been done correctly. False if not.
     */
    public boolean openChatRoom(long chatId) {
        closeChatRoom(chatId);
        return app.getMegaChatApi().openChatRoom(chatId, chatRoomListener);
    }

    /**
     * Method for close a particular chatRoom.
     *
     * @param chatId The chat ID.
     */
    private void closeChatRoom(long chatId) {
        app.getMegaChatApi().closeChatRoom(chatId, chatRoomListener);
    }

    public String getPendingJoinLink() {
        return pendingJoinLink;
    }

    public boolean isPendingJoinLink() {
        return !isTextEmpty(pendingJoinLink);
    }

    public void setPendingJoinLink(String link) {
        pendingJoinLink = link;
    }

    public void addJoiningChatId(long joiningChatId) {
        joiningChatIds.add(joiningChatId);
    }

    public void removeJoiningChatId(long joiningChatId) {
        if (joiningChatIds.remove(joiningChatId)) {
            app.sendBroadcast(new Intent(BROADCAST_ACTION_JOINED_SUCCESSFULLY));
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

    public boolean getSpeakerStatus(long chatId) {
        boolean entryExists = hashMapSpeaker.containsKey(chatId);
        if (entryExists) {
            return hashMapSpeaker.get(chatId);
        }

        setSpeakerStatus(chatId, false);
        return false;
    }

    public void setSpeakerStatus(long chatId, boolean speakerStatus) {
        hashMapSpeaker.put(chatId, speakerStatus);
    }

    public boolean getVideoStatus(long chatId) {
        boolean entryExists = hashMapVideo.containsKey(chatId);
        if (entryExists) {
            return hashMapVideo.get(chatId);
        }
        setVideoStatus(chatId, false);
        return false;
    }

    public void setVideoStatus(long chatId, boolean videoStatus) {
        hashMapVideo.put(chatId, videoStatus);
    }

    public boolean isOpeningMeetingLink(long chatId) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            boolean entryExists = hashOpeningMeetingLink.containsKey(chatId);
            if (entryExists) {
                return hashOpeningMeetingLink.get(chatId);
            }
        }
        return false;
    }

    public void setOpeningMeetingLink(long chatId, boolean isOpeningMeetingLink) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            hashOpeningMeetingLink.put(chatId, isOpeningMeetingLink);
        }
    }

    public boolean isRequestSent(long callId) {
        boolean entryExists = hashMapOutgoingCall.containsKey(callId);
        if (entryExists) {
            return hashMapOutgoingCall.get(callId);
        }

        return false;
    }

    public void setRequestSentCall(long callId, boolean isRequestSent) {
        if (isRequestSent(callId) == isRequestSent)
            return;

        hashMapOutgoingCall.put(callId, isRequestSent);
        if (!isRequestSent) {
            LiveEventBus.get(EVENT_NOT_OUTGOING_CALL, Long.class).post(callId);
        }
    }

    public void addCurrentGroupChat(long chatId) {
        if (currentActiveGroupChat.isEmpty() || !currentActiveGroupChat.contains(chatId)) {
            currentActiveGroupChat.add(chatId);
        }
    }

    public void addNotificationShown(long chatId) {
        if (isNotificationShown(chatId))
            return;

        notificationShown.add(chatId);
    }

    public void removeNotificationShown(long chatId) {
        if (isNotificationShown(chatId)) {
            notificationShown.remove(chatId);
        }
    }

    public boolean isNotificationShown(long chatId) {
        if (notificationShown.isEmpty())
            return false;

        return notificationShown.contains(chatId);
    }

    public void removeActiveChatAndNotificationShown(long chatId) {
        currentActiveGroupChat.remove(chatId);
        removeNotificationShown(chatId);
    }

    public void removeStatusVideoAndSpeaker(long chatId) {
        hashMapSpeaker.remove(chatId);
        hashMapVideo.remove(chatId);
    }

    /**
     * Method to control when a call has ended
     *
     * @param callId   Call ID
     * @param chatId   Chat ID
     */
    public void controlCallFinished(long callId, long chatId) {
        clearIncomingCallNotification(callId);
        removeValues(chatId);
        setRequestSentCall(callId, false);
    }

    /**
     * Method to remove the audio manager and reset video and speaker settings
     *
     * @param chatId Chat ID
     */
    private void removeValues(long chatId) {
        PreferenceManager.getDefaultSharedPreferences(MegaApplication.getInstance().getApplicationContext()).edit().remove(KEY_IS_SHOWED_WARNING_MESSAGE + chatId).apply();
        removeStatusVideoAndSpeaker(chatId);

        if (!existsAnOngoingOrIncomingCall()) {
            MegaApplication.getInstance().removeRTCAudioManager();
            MegaApplication.getInstance().removeRTCAudioManagerRingIn();
        } else if (participatingInACall()) {
            MegaApplication.getInstance().removeRTCAudioManagerRingIn();
        }
    }

    /**
     * Method to control whether or not to display the incoming call notification when I am added to a group and a call is in progress.
     *
     * @param item MegaChatListItem of the new group chat
     */
    public void checkActiveGroupChat(MegaChatListItem item) {
        if (currentActiveGroupChat.isEmpty() || !currentActiveGroupChat.contains(item.getChatId())) {
            currentActiveGroupChat.add(item.getChatId());
            
            MegaChatCall call = app.getMegaChatApi().getChatCall(item.getChatId());
            if (call == null) {
                logError("Call is null");
                return;
            }

            if(call.getStatus() == CALL_STATUS_USER_NO_PRESENT){
                checkToShowIncomingGroupCallNotification(call, item.getChatId());
            }
        }
    }

    /**
     * Method that displays the incoming call notification when I am added to a group that has a call in progress
     *
     * @param call   The call in this chat room
     * @param chatId The chat ID
     */
    public void checkToShowIncomingGroupCallNotification(MegaChatCall call, long chatId) {
        if(call.isRinging() || isNotificationShown(chatId))
            return;

        MegaChatRoom chatRoom = app.getMegaChatApi().getChatRoom(chatId);
        if (chatRoom == null) {
            logError("Chat is null");
            return;
        }

        if (isOpeningMeetingLink(chatId)) {
            logDebug("Opening meeting link, don't show notification");
            return;
        }

        logDebug("Show incoming call notification");
        app.showGroupCallNotification(chatId);
    }
}
