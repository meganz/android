package nz.mega.sdk;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import nz.mega.sdk.MegaApiJava;

public class MegaChatApiJava {
    MegaChatApi megaChatApi;

    static Set<DelegateMegaChatRequestListener> activeRequestListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaChatRequestListener>());
    static Set<DelegateMegaChatListener> activeChatListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaChatListener>());

    void runCallback(Runnable runnable) {
        runnable.run();
    }

    /**
     * Creates an instance of MegaChatApi to access to the chat-engine.
     *
     * @param megaApi Instance of MegaApi to be used by the chat-engine.
     * @param resumeSession Boolean indicating if you're going to resume a session. If false, any existing
     * session will be discarded and MegaChatApi expects to have a login+fetchnodes before MegaChatApi::init
     */
    public MegaChatApiJava(MegaApiJava megaApi, boolean resumeSession){
        megaChatApi = new MegaChatApi(megaApi.getMegaApi(), resumeSession);
    }

    public void addChatRequestListener(MegaChatRequestListenerInterface listener)
    {
        megaChatApi.addChatRequestListener(createDelegateRequestListener(listener, false));
    }

    public void addChatListener(MegaChatListenerInterface listener)
    {
        megaChatApi.addChatListener(createDelegateChatListener(listener));
    }

    public void init()
    {
        megaChatApi.init();
    }

    public void init(MegaChatRequestListenerInterface listener)
    {
        megaChatApi.init(createDelegateRequestListener(listener));
    }

    public void connect()
    {
        megaChatApi.connect();
    }

    public void connect(MegaChatRequestListenerInterface listener)
    {
        megaChatApi.connect(createDelegateRequestListener(listener));
    }

    public void setOnlineStatus(int status)
    {
        megaChatApi.setOnlineStatus(status);
    }

    public void setOnlineStatus(int status, MegaChatRequestListenerInterface listener)
    {
        megaChatApi.setOnlineStatus(status, createDelegateRequestListener(listener));
    }

    public MegaChatRoomList getChatRooms()
    {
        return megaChatApi.getChatRooms();
    }

    public void intiveToChat(long chatid, long userhandle, int privs)
    {
        megaChatApi.inviteToChat(chatid, userhandle, privs);
    }

    public void intiveToChat(long chatid, long userhandle, int privs, MegaChatRequestListenerInterface listener)
    {
        megaChatApi.inviteToChat(chatid, userhandle, privs, createDelegateRequestListener(listener));
    }

    public void removeFromChat(long chatid, long userhandle)
    {
        megaChatApi.removeFromChat(chatid, userhandle);
    }

    public void removeFromChat(long chatid, long userhandle, MegaChatRequestListenerInterface listener)
    {
        megaChatApi.removeFromChat(chatid, userhandle, createDelegateRequestListener(listener));
    }

    public void updateChatPermissions(long chatid, long userhandle, int privilege)
    {
        megaChatApi.updateChatPermissions(chatid, userhandle, privilege);
    }

    public void updateChatPermissions(long chatid, long userhandle, int privilege, MegaChatRequestListenerInterface listener)
    {
        megaChatApi.updateChatPermissions(chatid, userhandle, privilege, createDelegateRequestListener(listener));
    }

    public void setChatTitle(long chatid, String title)
    {
        megaChatApi.setChatTitle(chatid, title);
    }

    public void setChatTitle(long chatid, String title, MegaChatRequestListenerInterface listener)
    {
        megaChatApi.setChatTitle(chatid, title, createDelegateRequestListener(listener));
    }

    public void truncateChat(long chatid, long messageid)
    {
        megaChatApi.truncateChat(chatid, messageid);
    }

    public void truncateChat(long chatid, long messageid, MegaChatRequestListenerInterface listener)
    {
        megaChatApi.truncateChat(chatid, messageid, createDelegateRequestListener(listener));
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

    private MegaChatListener createDelegateChatListener(MegaChatListenerInterface listener) {
        DelegateMegaChatListener delegateListener = new DelegateMegaChatListener(this, listener);
        activeChatListeners.add(delegateListener);
        return delegateListener;
    }

    void privateFreeRequestListener(DelegateMegaChatRequestListener listener) {
        activeRequestListeners.remove(listener);
    }
};
