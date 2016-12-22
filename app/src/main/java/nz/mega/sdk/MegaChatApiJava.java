package nz.mega.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import nz.mega.sdk.MegaApiJava;

public class MegaChatApiJava {
    MegaChatApi megaChatApi;
    static DelegateMegaChatLogger logger;

    // Error information but application will continue run.
    public final static int LOG_LEVEL_ERROR = MegaChatApi.LOG_LEVEL_ERROR;
    // Information representing errors in application but application will keep running
    public final static int LOG_LEVEL_WARNING = MegaChatApi.LOG_LEVEL_WARNING;
    // Mainly useful to represent current progress of application.
    public final static int LOG_LEVEL_INFO = MegaChatApi.LOG_LEVEL_INFO;
    public final static int LOG_LEVEL_VERBOSE = MegaChatApi.LOG_LEVEL_VERBOSE;
    // Informational logs, that are useful for developers. Only applicable if DEBUG is defined.
    public final static int LOG_LEVEL_DEBUG = MegaChatApi.LOG_LEVEL_DEBUG;
    public final static int LOG_LEVEL_MAX = MegaChatApi.LOG_LEVEL_MAX;

    static Set<DelegateMegaChatRequestListener> activeRequestListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaChatRequestListener>());
    static Set<DelegateMegaChatListener> activeChatListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaChatListener>());
    static Set<DelegateMegaChatRoomListener> activeChatRoomListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaChatRoomListener>());

    void runCallback(Runnable runnable) {
        runnable.run();
    }

    /**
     * Creates an instance of MegaChatApi to access to the chat-engine.
     *
     * @param megaApi Instance of MegaApi to be used by the chat-engine.
     * session will be discarded and MegaChatApi expects to have a login+fetchnodes before MegaChatApi::init
     */
    public MegaChatApiJava(MegaApiJava megaApi){
        megaChatApi = new MegaChatApi(megaApi.getMegaApi());
    }

    public void addChatRequestListener(MegaChatRequestListenerInterface listener)
    {
        megaChatApi.addChatRequestListener(createDelegateRequestListener(listener, false));
    }

    public void addChatListener(MegaChatListenerInterface listener)
    {
        megaChatApi.addChatListener(createDelegateChatListener(listener));
    }

    public void removeChatRequestListener(MegaChatRequestListenerInterface listener) {
        megaChatApi.removeChatRequestListener(createDelegateRequestListener(listener, false));
    }

    public void removeChatRoomListener(MegaChatRoomListenerInterface listener) {
        megaChatApi.removeChatRoomListener(createDelegateChatRoomListener(listener));
    }

    public void removeChatListener(MegaChatListenerInterface listener) {
        megaChatApi.removeChatListener(createDelegateChatListener(listener));
    }

    public int init(String sid)
    {
        return megaChatApi.init(sid);
    }

//    public void init(boolean resumeSession, MegaChatRequestListenerInterface listener)
//    {
//        megaChatApi.init(createDelegateRequestListener(listener));
//    }

    public void connect()
    {
        megaChatApi.connect();
    }

    public void connect(MegaChatRequestListenerInterface listener)
    {
        megaChatApi.connect(createDelegateRequestListener(listener));
    }

    /**
     * @brief Logout of chat servers invalidating the session
     *
     * The associated request type with this request is MegaChatRequest::TYPE_LOGOUT
     *
     * After calling \c logout, the subsequent call to MegaChatApi::init expects to
     * have a new session created by MegaApi::login.
     *
     * @param listener MegaChatRequestListener to track this request
     */
    public void logout(MegaChatRequestListenerInterface listener){
        megaChatApi.logout(createDelegateRequestListener(listener));
    }

    /**
     * @brief Logout of chat servers without invalidating the session
     *
     * The associated request type with this request is MegaChatRequest::TYPE_LOGOUT
     *
     * After calling \c localLogout, the subsequent call to MegaChatApi::init expects to
     * have an already existing session created by MegaApi::fastLogin(session)
     *
     * @param listener MegaChatRequestListener to track this request
     */
    public void localLogout(MegaChatRequestListenerInterface listener){
        megaChatApi.localLogout(createDelegateRequestListener(listener));
    }

    /**
     * Creates a chat for one or more participants, allowing you to specify their
     * permissions and if the chat should be a group chat or not (when it is just for 2 participants).
     *
     * There are two types of chat: permanent an group. A permanent chat is between two people, and
     * participants can not leave it.
     *
     * The creator of the chat will have moderator level privilege and should not be included in the
     * list of peers.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_CREATE_CHATROOM
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getFlag - Returns if the new chat is a group chat or permanent chat
     * - MegaChatRequest::getMegaChatPeerList - List of participants and their privilege level
     *
     * Valid data in the MegaChatRequest object received in onRequestFinish when the error code
     * is MegaError::ERROR_OK:
     * - MegaChatRequest::getChatHandle - Returns the handle of the new chatroom
     *
     * @note If you are trying to create a chat with more than 1 other person, then it will be forced
     * to be a group chat.
     *
     * @note If peers list contains only one person, group chat is not set and a permament chat already
     * exists with that person, then this call will return the information for the existing chat, rather
     * than a new chat.
     *
     * @param group Flag to indicate if the chat is a group chat or not
     * @param peers MegaChatPeerList including other users and their privilege level
     * @param listener MegaChatRequestListener to track this request
     */
    public void createChat(boolean group, MegaChatPeerList peers, MegaChatRequestListenerInterface listener){
        megaChatApi.createChat(group, peers, createDelegateRequestListener(listener));
    }

    public void inviteToChat(long chatid, long userhandle, int privs)
    {
        megaChatApi.inviteToChat(chatid, userhandle, privs);
    }

    /**
     * Adds a user to an existing chat. To do this you must have the
     * moderator privilege in the chat, and the chat must be a group chat.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_INVITE_TO_CHATROOM
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getUserHandle - Returns the MegaChatHandle of the user to be invited
     * - MegaChatRequest::getPrivilege - Returns the privilege level wanted for the user
     *
     * On the onTransferFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ACCESS - If the logged in user doesn't have privileges to invite peers.
     * - MegaChatError::ERROR_NOENT - If there isn't any chat with the specified chatid.
     * - MegaChatError::ERROR_ARGS - If the chat is not a group chat (cannot invite peers)
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param userhandle MegaChatHandle that identifies the user
     * @param privs Privilege level for the new peers. Valid values are:
     * - MegaChatPeerList::PRIV_RO = 0
     * - MegaChatPeerList::PRIV_STANDARD = 2
     * - MegaChatPeerList::PRIV_MODERATOR = 3
     * @param listener MegaChatRequestListener to track this request
     */
    public void inviteToChat(long chatid, long userhandle, int privs, MegaChatRequestListenerInterface listener)
    {
        megaChatApi.inviteToChat(chatid, userhandle, privs, createDelegateRequestListener(listener));
    }

    /**
     * Remove another user from a chat. To remove a user you need to have the
     * operator/moderator privilege. Only groupchats can be left.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_REMOVE_FROM_CHATROOM
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getUserHandle - Returns the MegaChatHandle of the user to be removed
     *
     * On the onTransferFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ACCESS - If the logged in user doesn't have privileges to remove peers.
     * - MegaChatError::ERROR_NOENT - If there isn't any chat with the specified chatid.
     * - MegaChatError::ERROR_ARGS - If the chat is not a group chat (cannot remove peers)
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param uh MegaChatHandle that identifies the user.
     * @param listener MegaChatRequestListener to track this request
     */
    public void removeFromChat(long chatid, long uh, MegaChatRequestListenerInterface listener){
        megaChatApi.removeFromChat(chatid, uh,createDelegateRequestListener(listener));
    }

    /**
     * Leave a chatroom. Only groupchats can be left.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_REMOVE_FROM_CHATROOM
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     *
     * On the onTransferFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ACCESS - If the logged in user doesn't have privileges to remove peers.
     * - MegaChatError::ERROR_NOENT - If there isn't any chat with the specified chatid.
     * - MegaChatError::ERROR_ARGS - If the chat is not a group chat (cannot remove peers)
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param listener MegaChatRequestListener to track this request
     */
    public void leaveChat(long chatid, MegaChatRequestListenerInterface listener){
        megaChatApi.leaveChat(chatid,createDelegateRequestListener(listener));
    }

    /**
     * Allows a logged in operator/moderator to adjust the permissions on any other user
     * in their group chat. This does not work for a 1:1 chat.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_UPDATE_PEER_PERMISSIONS
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getUserHandle - Returns the MegaChatHandle of the user whose permission
     * is to be upgraded
     * - MegaChatRequest::getPrivilege - Returns the privilege level wanted for the user
     *
     * On the onTransferFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ACCESS - If the logged in user doesn't have privileges to update the privilege level.
     * - MegaChatError::ERROR_NOENT - If there isn't any chat with the specified chatid.
     * - MegaChatError::ERROR_ARGS - If the chatid or user handle are invalid
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param uh MegaChatHandle that identifies the user
     * @param privilege Privilege level for the existing peer. Valid values are:
     * - MegaChatPeerList::PRIV_RO = 0
     * - MegaChatPeerList::PRIV_STANDARD = 2
     * - MegaChatPeerList::PRIV_MODERATOR = 3
     * @param listener MegaChatRequestListener to track this request
     */
    public void updateChatPermissions(long chatid, long uh, int privilege, MegaChatRequestListenerInterface listener){
        megaChatApi.updateChatPermissions(chatid, uh, privilege, createDelegateRequestListener(listener));
    }

    /**
     * Get the online status of a user.
     *
     * It can be one of the following values:
     * - MegaChatApi::STATUS_OFFLINE = 1
     * The user appears as being offline
     *
     * - MegaChatApi::STATUS_BUSY = 2
     * The user is busy and don't want to be disturbed.
     *
     * - MegaChatApi::STATUS_AWAY = 3
     * The user is away and might not answer.
     *
     * - MegaChatApi::STATUS_ONLINE = 4
     * The user is connected and online.
     *
     * @param userhandle of the peer whose name is requested.
     * @return Online status of the user
     */
    public int getUserOnlineStatus(long userhandle){
        return megaChatApi.getUserOnlineStatus(userhandle);
    }

    /**
     * @brief Set your online status.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_SET_CHAT_STATUS
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaRequest::getNumber - Returns the new status of the user in chat.
     *
     * @param status Online status in the chat.
     *
     * It can be one of the following values:
     * - MegaChatApi::STATUS_OFFLINE = 1
     * The user appears as being offline
     *
     * - MegaChatApi::STATUS_BUSY = 2
     * The user is busy and don't want to be disturbed.
     *
     * - MegaChatApi::STATUS_AWAY = 3
     * The user is away and might not answer.
     *
     * - MegaChatApi::STATUS_ONLINE = 4
     * The user is connected and online.
     *
     * @param listener MegaChatRequestListener to track this request
     */
    public void setOnlineStatus(int status, MegaChatRequestListenerInterface listener){
        megaChatApi.setOnlineStatus(status, createDelegateRequestListener(listener));
    }

    public void setOnlineStatus(int status)
    {
        megaChatApi.setOnlineStatus(status);
    }

    /**
     * Get your online status.
     *
     * It can be one of the following values:
     * - MegaChatApi::STATUS_OFFLINE = 1
     * The user appears as being offline
     *
     * - MegaChatApi::STATUS_BUSY = 2
     * The user is busy and don't want to be disturbed.
     *
     * - MegaChatApi::STATUS_AWAY = 3
     * The user is away and might not answer.
     *
     * - MegaChatApi::STATUS_ONLINE = 4
     * The user is connected and online.
     */
    public int getOnlineStatus(){
        return megaChatApi.getOnlineStatus();
    }


    /**
     * Returns the current firstname of the user
     *
     * This function is useful to get the firstname of users who participated in a groupchat with
     * you but already left. If the user sent a message, you may want to show the name of the sender.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_GET_FIRSTNAME
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getUserHandle - Returns the handle of the user
     *
     * Valid data in the MegaChatRequest object received in onRequestFinish when the error code
     * is MegaError::ERROR_OK:
     * - MegaChatRequest::getText - Returns the firstname of the user
     *
     * @param userhandle Handle of the user whose name is requested.
     * @param listener MegaChatRequestListener to track this request
     */
    public void getUserFirstname(long userhandle, MegaChatRequestListenerInterface listener){
        megaChatApi.getUserFirstname(userhandle, createDelegateRequestListener(listener));
    }

    /**
     * Returns the current lastname of the user
     *
     * This function is useful to get the lastname of users who participated in a groupchat with
     * you but already left. If the user sent a message, you may want to show the name of the sender.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_GET_LASTNAME
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getUserHandle - Returns the handle of the user
     *
     * Valid data in the MegaChatRequest object received in onRequestFinish when the error code
     * is MegaError::ERROR_OK:
     * - MegaChatRequest::getText - Returns the lastname of the user
     *
     *
     * @param userhandle Handle of the user whose name is requested.
     * @param listener MegaChatRequestListener to track this request
     */
    public void getUserLastname(long userhandle, MegaChatRequestListenerInterface listener){
        megaChatApi.getUserLastname(userhandle, createDelegateRequestListener(listener));
    }

    /**
     * Returns the current email address of the user
     *
     * This function is useful to get the email address of users you are contact with.
     * Note that for any other user without contact relationship, this function will return NULL.
     *
     * You take the ownership of the returned value
     *
     * @param userhandle Handle of the user whose name is requested.
     * @return The email address of the contact, or NULL if not found.
     */
    public String getUserEmail(long userhandle){
        return megaChatApi.getUserEmail(userhandle);
    }

    public ArrayList<MegaChatRoom> getChatRooms()
    {
        return chatRoomListToArray(megaChatApi.getChatRooms());
    }

    /**
     * Get the MegaChatRoom for the 1on1 chat with the specified user
     *
     * If the 1on1 chat with the user specified doesn't exist, this function will
     * return NULL.
     *
     * It is needed to have successfully completed the \c MegaChatApi::init request
     * before calling this function.
     *
     * You take the ownership of the returned value
     *
     * @param userhandle MegaChatHandle that identifies the user
     * @return MegaChatRoom object for the specified \c userhandle
     */
    public MegaChatRoom getChatRoomByUser(long userhandle){
        return megaChatApi.getChatRoomByUser(userhandle);
    }

    /**
     * Get all chatrooms (1on1 and groupal) with limited information
     *
     * It is needed to have successfully completed the \c MegaChatApi::init request
     * before calling this function.
     *
     * Note that MegaChatListItem objects don't include as much information as
     * MegaChatRoom objects, but a limited set of data that is usually displayed
     * at the list of chatrooms, like the title of the chat or the unread count.
     *
     * You take the ownership of the returned value
     *
     * @return List of MegaChatListItemList objects with all chatrooms of this account.
     */
    public ArrayList<MegaChatListItem> getChatListItems(){
        return chatRoomListItemToArray(megaChatApi.getChatListItems());

    }

    /**
     * Get the MegaChatListItem that has a specific handle
     *
     * You can get the handle of the chatroom using MegaChatRoom::getChatId or
     * MegaChatListItem::getChatId.
     *
     * It is needed to have successfully completed the \c MegaChatApi::init request
     * before calling this function.
     *
     * Note that MegaChatListItem objects don't include as much information as
     * MegaChatRoom objects, but a limited set of data that is usually displayed
     * at the list of chatrooms, like the title of the chat or the unread count.
     *
     * You take the ownership of the returned value
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @return MegaChatListItem object for the specified \c chatid
     */
    public MegaChatListItem getChatListItem(long chatid){
        return megaChatApi.getChatListItem(chatid);
    }

    /**
     * Get the chat id for the 1on1 chat with the specified user
     *
     * If the 1on1 chat with the user specified doesn't exist, this function will
     * return MEGACHAT_INVALID_HANDLE.
     *
     * @param userhandle MegaChatHandle that identifies the user
     * @return MegaChatHandle that identifies the 1on1 chatroom
     */
    long getChatHandleByUser(long userhandle){
        return megaChatApi.getChatHandleByUser(userhandle);
    }

    /**
     * Get the MegaChatRoom that has a specific handle
     *
     * You can get the handle of a MegaChatRoom using MegaChatRoom::getChatId or
     * MegaChatListItem::getChatId.
     *
     * It is needed to have successfully completed the \c MegaChatApi::init request
     * before calling this function.
     *
     * You take the ownership of the returned value
     *
     * @return List of MegaChatRoom objects with all chatrooms of this account.
     */

    public MegaChatRoom getChatRoom(long chatid){
        return megaChatApi.getChatRoom(chatid);
    }

/*
    /**
     * @brief Returns the handle of the user.
     *
     * @return For outgoing messages, it returns the handle of the target user.
     * For incoming messages, it returns the handle of the sender.
     *
    public long getUserHandle()
    {

    }
*/

    public void removeFromChat(long chatid, long userhandle)
    {
        megaChatApi.removeFromChat(chatid, userhandle);
    }

    public void truncateChat(long chatid, long messageid)
    {
        megaChatApi.truncateChat(chatid, messageid);
    }

    public void truncateChat(long chatid, long messageid, MegaChatRequestListenerInterface listener)
    {
        megaChatApi.truncateChat(chatid, messageid, createDelegateRequestListener(listener));
    }

    /**
     * Allows a logged in operator/moderator to clear the entire history of a chat
     *
     * The latest message gets overridden with a management message.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_TRUNCATE_HISTORY
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     *
     * On the onTransferFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ACCESS - If the logged in user doesn't have privileges to truncate the chat history
     * - MegaChatError::ERROR_NOENT - If there isn't any chat with the specified chatid.
     * - MegaChatError::ERROR_ARGS - If the chatid or user handle are invalid
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param listener MegaChatRequestListener to track this request
     */
    public void clearChatHistory(long chatid, MegaChatRequestListenerInterface listener){
        megaChatApi.clearChatHistory(chatid, createDelegateRequestListener(listener));
    }

    /**
     * Allows to set the title of a group chat
     *
     * Only participants with privilege level MegaChatPeerList::PRIV_MODERATOR are allowed to
     * set the title of a chat.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_EDIT_CHATROOM_NAME
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getText - Returns the title of the chat.
     *
     * On the onTransferFinish error, the error code associated to the MegaChatError can be:
     * - MegaChatError::ERROR_ACCESS - If the logged in user doesn't have privileges to invite peers.
     * - MegaChatError::ERROR_ARGS - If there's a title and it's not Base64url encoded.
     *
     * Valid data in the MegaChatRequest object received in onRequestFinish when the error code
     * is MegaError::ERROR_OK:
     * - MegaChatRequest::getText - Returns the title of the chat that was actually saved.
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param title Null-terminated character string with the title that wants to be set. If the
     * title is longer than 30 characters, it will be truncated to that maximum length.
     * @param listener MegaChatRequestListener to track this request
     */
    public void setChatTitle(long chatid, String title, MegaChatRequestListenerInterface listener){
        megaChatApi.setChatTitle(chatid, title, createDelegateRequestListener(listener));
    }

    /**
     * This method should be called when a chat is opened
     *
     * The second parameter is the listener that will receive notifications about
     * events related to the specified chatroom.
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param listener MegaChatRoomListener to track events on this chatroom
     *
     * @return True if success, false if the chatroom was not found.
     */
//    public boolean openChatRoom(long chatid, MegaChatRoomListenerInterface listener){
    public boolean openChatRoom(long chatid, MegaChatRoomListenerInterface listener){

        return megaChatApi.openChatRoom(chatid, createDelegateChatRoomListener(listener));
    }

    /**
     * This method should be called when a chat is closed.
     *
     * It automatically unregisters the listener to stop receiving the related events.
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param listener MegaChatRoomListener to be unregistered.
     */
    public void closeChatRoom(long chatid, MegaChatRoomListenerInterface listener){

        DelegateMegaChatRoomListener listenerToDelete=null;

        Iterator<DelegateMegaChatRoomListener> itr = activeChatRoomListeners.iterator();
        while(itr.hasNext()) {
            DelegateMegaChatRoomListener item = itr.next();
            if(item.getUserListener() == listener){
                listenerToDelete = item;
                itr.remove();
                break;
            }
        }

        megaChatApi.closeChatRoom(chatid, listenerToDelete);
    }

    /**
     * Initiates fetching more history of the specified chatroom.
     *
     * The loaded messages will be notified one by one through the MegaChatRoomListener
     * specified at MegaChatApi::openChatRoom (and through any other listener you may have
     * registered by calling MegaChatApi::addChatRoomListener).
     *
     * The corresponding callback is MegaChatRoomListener::onMessageLoaded.
     *
     * @note The actual number of messages loaded can be less than \c count. One reason is
     * the history being shorter than requested, the other is due to internal protocol
     * messages that are not intended to be displayed to the user. Additionally, if the fetch
     * is local and there's no more history locally available, the number of messages could be
     * lower too (and the next call to MegaChatApi::loadMessages will fetch messages from server).
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param count The number of requested messages to load.
     *
     * @return Return the source of the messages that is going to be fetched. The possible values are:
     *   - MegaChatApi::SOURCE_NONE = 0: there's no more history available (not even int the server)
     *   - MegaChatApi::SOURCE_LOCAL: messages will be fetched locally (RAM or DB)
     *   - MegaChatApi::SOURCE_REMOTE: messages will be requested to the server. Expect some delay
     *
     * The value MegaChatApi::SOURCE_REMOTE can be used to show a progress bar accordingly when network operation occurs.
     */
    public int loadMessages(long chatid, int count){
        return megaChatApi.loadMessages(chatid, count);
    }

    /**
     * Returns the MegaChatMessage specified from the chat room.
     *
     * Only the messages that are already loaded and notified
     * by MegaChatRoomListener::onMessageLoaded can be requested. For any
     * other message, this function will return NULL.
     *
     * You take the ownership of the returned value.
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param msgid MegaChatHandle that identifies the message
     * @return The MegaChatMessage object, or NULL if not found.
     */
    public MegaChatMessage getMessage(long chatid, long msgid){
        return megaChatApi.getMessage(chatid, msgid);
    }

    /**
     * Sends a new message to the specified chatroom
     *
     * The MegaChatMessage object returned by this function includes a message transaction id,
     * That id is not the definitive id, which will be assigned by the server. You can obtain the
     * temporal id with MegaChatMessage::getTempId()
     *
     * When the server confirms the reception of the message, the MegaChatRoomListener::onMessageUpdate
     * is called, including the definitive id and the new status: MegaChatMessage::STATUS_SERVER_RECEIVED.
     * At this point, the app should refresh the message identified by the temporal id and move it to
     * the final position in the history, based on the reported index in the callback.
     *
     * If the message is rejected by the server, the message will keep its temporal id and will have its
     * a message id set to INVALID_HANDLE.
     *
     * You take the ownership of the returned value.
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param msg Content of the message
     * application-specific type like link, share, picture etc.) @see MegaChatMessage::Type.
     *
     * @return MegaChatMessage that will be sent. The message id is not definitive, but temporal.
     */
    public MegaChatMessage sendMessage(long chatid, String msg){
        return megaChatApi.sendMessage(chatid, msg);
    }

    /**
     * Edits an existing message
     *
     * Message's edits are only allowed during a short timeframe, usually 1 hour.
     * Message's deletions are equivalent to message's edits, but with empty content.
     *
     * There is only one pending edit for not-yet confirmed edits. Therefore, this function will
     * discard previous edits that haven't been notified via MegaChatRoomListener::onMessageUpdate
     * where the message has MegaChatMessage::hasChanged(MegaChatMessage::CHANGE_TYPE_CONTENT).
     *
     * If the edits is rejected... // TODO:
     *
     * You take the ownership of the returned value.
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param msgid MegaChatHandle that identifies the message
     * @param msg New content of the message
     *
     * @return MegaChatMessage that will be modified. NULL if the message cannot be edited (too old)
     */
    public MegaChatMessage editMessage(long chatid, long msgid, String msg){
        return megaChatApi.editMessage(chatid, msgid, msg);
    }

    /**
     * Deletes an existing message
     *
     * You take the ownership of the returned value.
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param msgid MegaChatHandle that identifies the message
     *
     * @return MegaChatMessage that will be deleted. NULL if the message cannot be deleted (too old)
     */
    public MegaChatMessage deleteMessage(long chatid, long msgid){
        return megaChatApi.deleteMessage(chatid, msgid);
    }

    /**
     * Sets the last-seen-by-us pointer to the specified message
     *
     * The last-seen-by-us pointer is persisted in the account, so every client will
     * be aware of the last-seen message.
     *
     * @param chatid MegaChatHandle that identifies the chat room
     * @param msgid MegaChatHandle that identifies the message
     *
     * @return False if the \c chatid is invalid or the message is older
     * than last-seen-by-us message. True if success.
     */
    public boolean setMessageSeen(long chatid, long msgid){
        return megaChatApi.setMessageSeen(chatid, msgid);
    }

    /**
     * Returns the last-seen-by-us message
     *
     * @param chatid MegaChatHandle that identifies the chat room
     *
     * @return The last-seen-by-us MegaChatMessage, or NULL if error.
     */
    public MegaChatMessage getLastMessageSeen(long chatid){
        return megaChatApi.getLastMessageSeen(chatid);
    }

    /**
     * Set the active log level.
     * <p>
     * This function sets the log level of the logging system. If you set a log listener using
     * MegaApiJava.setLoggerObject(), you will receive logs with the same or a lower level than
     * the one passed to this function.
     *
     * @param logLevel
     *            Active log level. These are the valid values for this parameter: <br>
     *                Valid values are:
     * - MegaChatApi::LOG_LEVEL_ERROR   = 1
     * - MegaChatApi::LOG_LEVEL_WARNING = 2
     * - MegaChatApi::LOG_LEVEL_INFO    = 3
     * - MegaChatApi::LOG_LEVEL_VERBOSE = 4
     * - MegaChatApi::LOG_LEVEL_DEBUG   = 5
     * - MegaChatApi::LOG_LEVEL_MAX     = 6
     *            - MegaApiJava.LOG_LEVEL_FATAL = 0. <br>
     *            - MegaApiJava.LOG_LEVEL_ERROR = 1. <br>
     *            - MegaApiJava.LOG_LEVEL_WARNING = 2. <br>
     *            - MegaApiJava.LOG_LEVEL_INFO = 3. <br>
     *            - MegaApiJava.LOG_LEVEL_DEBUG = 4. <br>
     *            - MegaApiJava.LOG_LEVEL_MAX = 5.
     */
    public static void setLogLevel(int logLevel) {
        MegaChatApi.setLogLevel(logLevel);
    }

    /**
     * Set a MegaLogger implementation to receive SDK logs.
     * <p>
     * Logs received by this objects depends on the active log level.
     * By default, it is MegaApiJava.LOG_LEVEL_INFO. You can change it
     * using MegaApiJava.setLogLevel().
     *
     * @param megaLogger
     *            MegaChatLogger implementation.
     */
    public static void setLoggerObject(MegaChatLoggerInterface megaLogger) {
        DelegateMegaChatLogger newLogger = new DelegateMegaChatLogger(megaLogger);
        MegaChatApi.setLoggerObject(newLogger);
        logger = newLogger;
    }

    private MegaChatRequestListener createDelegateRequestListener(MegaChatRequestListenerInterface listener) {
        DelegateMegaChatRequestListener delegateListener = new DelegateMegaChatRequestListener(this, listener, true);
        activeRequestListeners.add(delegateListener);
        return delegateListener;
    }

    private MegaChatRequestListener createDelegateRequestListener(MegaChatRequestListenerInterface listener, boolean singleListener) {
        DelegateMegaChatRequestListener delegateListener = new DelegateMegaChatRequestListener(this, listener, singleListener);
        activeRequestListeners.add(delegateListener);
        return delegateListener;
    }

    private MegaChatRoomListener createDelegateChatRoomListener(MegaChatRoomListenerInterface listener) {
        DelegateMegaChatRoomListener delegateListener = new DelegateMegaChatRoomListener(this, listener);
        activeChatRoomListeners.add(delegateListener);
        return delegateListener;
    }

    private MegaChatListener createDelegateChatListener(MegaChatListenerInterface listener) {
        DelegateMegaChatListener delegateListener = new DelegateMegaChatListener(this, listener);
        activeChatListeners.add(delegateListener);
        return delegateListener;
    }

    void privateFreeRequestListener(DelegateMegaChatRequestListener listener) {
        activeRequestListeners.remove(listener);
    }

    static ArrayList<MegaChatRoom> chatRoomListToArray(MegaChatRoomList chatRoomList) {

        if (chatRoomList == null) {
            return null;
        }

        ArrayList<MegaChatRoom> result = new ArrayList<MegaChatRoom>((int)chatRoomList.size());
        for (int i = 0; i < chatRoomList.size(); i++) {
            result.add(chatRoomList.get(i).copy());
        }

        return result;
    }

    static ArrayList<MegaChatListItem> chatRoomListItemToArray(MegaChatListItemList chatRoomItemList) {

        if (chatRoomItemList == null) {
            return null;
        }

        ArrayList<MegaChatListItem> result = new ArrayList<MegaChatListItem>((int)chatRoomItemList.size());
        for (int i = 0; i < chatRoomItemList.size(); i++) {
            result.add(chatRoomItemList.get(i).copy());
        }

        return result;
    }
};
