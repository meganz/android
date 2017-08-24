/*
 * (c) 2013-2015 by Mega Limited, Auckland, New Zealand
 *
 * This file is part of the MEGA SDK - Client Access Engine.
 *
 * Applications using the MEGA API must present a valid application key
 * and comply with the the rules set forth in the Terms of Service.
 *
 * The MEGA SDK is distributed in the hope that it will be useful,\
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * @copyright Simplified (2-clause) BSD License.
 * You should have received a copy of the license along with this
 * program.
 */
package nz.mega.sdk;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Java Application Programming Interface (API) to access MEGA SDK services on a MEGA account or shared public folder.
 * <p>
 * An appKey must be specified to use the MEGA SDK. Generate an appKey for free here: <br>
 * - https://mega.co.nz/#sdk
 * <p>
 * Save on data usage and start up time by enabling local node caching. This can be enabled by passing a local path
 * in the constructor. Local node caching prevents the need to download the entire file system each time the MegaApiJava
 * object is logged in.
 * <p>
 * To take advantage of local node caching, the application needs to save the session key after login
 * (MegaApiJava.dumpSession()) and use it to login during the next session. A persistent local node cache will only be
 * loaded if logging in with a session key.
 * Local node caching is also recommended in order to enhance security as it prevents the account password from being
 * stored by the application.
 * <p>
 * To access MEGA services using the MEGA SDK, an object of this class (MegaApiJava) needs to be created and one of the
 * MegaApiJava.login() options used to log into a MEGA account or a public folder. If the login request succeeds,
 * call MegaApiJava.fetchNodes() to get the account's file system from MEGA. Once the file system is retrieved, all other
 * requests including file management and transfers can be used.
 * <p>
 * After using MegaApiJava.logout() you can reuse the same MegaApi object to log in to another MEGA account or a public
 * folder.
 */
public class MegaApiJava {
    MegaApi megaApi;
    MegaGfxProcessor gfxProcessor;
    static DelegateMegaLogger logger;

    void runCallback(Runnable runnable) {
        runnable.run();
    }

    static Set<DelegateMegaRequestListener> activeRequestListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaRequestListener>());
    static Set<DelegateMegaTransferListener> activeTransferListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaTransferListener>());
    static Set<DelegateMegaGlobalListener> activeGlobalListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaGlobalListener>());
    static Set<DelegateMegaListener> activeMegaListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaListener>());
    static Set<DelegateMegaTreeProcessor> activeMegaTreeProcessors = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaTreeProcessor>());

    // Order options for getChildren
    public final static int ORDER_NONE = MegaApi.ORDER_NONE;
    public final static int ORDER_DEFAULT_ASC = MegaApi.ORDER_DEFAULT_ASC;
    public final static int ORDER_DEFAULT_DESC = MegaApi.ORDER_DEFAULT_DESC;
    public final static int ORDER_SIZE_ASC = MegaApi.ORDER_SIZE_ASC;
    public final static int ORDER_SIZE_DESC = MegaApi.ORDER_SIZE_DESC;
    public final static int ORDER_CREATION_ASC = MegaApi.ORDER_CREATION_ASC;
    public final static int ORDER_CREATION_DESC = MegaApi.ORDER_CREATION_DESC;
    public final static int ORDER_MODIFICATION_ASC = MegaApi.ORDER_MODIFICATION_ASC;
    public final static int ORDER_MODIFICATION_DESC = MegaApi.ORDER_MODIFICATION_DESC;
    public final static int ORDER_ALPHABETICAL_ASC = MegaApi.ORDER_ALPHABETICAL_ASC;
    public final static int ORDER_ALPHABETICAL_DESC = MegaApi.ORDER_ALPHABETICAL_DESC;

    public final static int ATTR_TYPE_THUMBNAIL = MegaApi.ATTR_TYPE_THUMBNAIL;
    public final static int ATTR_TYPE_PREVIEW = MegaApi.ATTR_TYPE_PREVIEW;

    public final static int USER_ATTR_AVATAR = MegaApi.USER_ATTR_AVATAR;
    public final static int USER_ATTR_FIRSTNAME = MegaApi.USER_ATTR_FIRSTNAME;
    public final static int USER_ATTR_LASTNAME = MegaApi.USER_ATTR_LASTNAME;
    public final static int USER_ATTR_AUTHRING = MegaApi.USER_ATTR_AUTHRING;
    public final static int USER_ATTR_LAST_INTERACTION = MegaApi.USER_ATTR_LAST_INTERACTION;

    // Very severe error event that will presumably lead the application to abort.
    public final static int LOG_LEVEL_FATAL = MegaApi.LOG_LEVEL_FATAL;
    // Error information but application will continue run.
    public final static int LOG_LEVEL_ERROR = MegaApi.LOG_LEVEL_ERROR;
    // Information representing errors in application but application will keep running
    public final static int LOG_LEVEL_WARNING = MegaApi.LOG_LEVEL_WARNING;
    // Mainly useful to represent current progress of application.
    public final static int LOG_LEVEL_INFO = MegaApi.LOG_LEVEL_INFO;
    // Informational logs, that are useful for developers. Only applicable if DEBUG is defined.
    public final static int LOG_LEVEL_DEBUG = MegaApi.LOG_LEVEL_DEBUG;
    public final static int LOG_LEVEL_MAX = MegaApi.LOG_LEVEL_MAX;

    public final static int EVENT_FEEDBACK = 0;
    public final static int EVENT_DEBUG = EVENT_FEEDBACK + 1;
    public final static int EVENT_INVALID = EVENT_DEBUG + 1;

    public final static int PAYMENT_METHOD_BALANCE = MegaApi.PAYMENT_METHOD_BALANCE;
    public final static int PAYMENT_METHOD_PAYPAL = MegaApi.PAYMENT_METHOD_PAYPAL;
    public final static int PAYMENT_METHOD_ITUNES = MegaApi.PAYMENT_METHOD_ITUNES;
    public final static int PAYMENT_METHOD_GOOGLE_WALLET = MegaApi.PAYMENT_METHOD_GOOGLE_WALLET;
    public final static int PAYMENT_METHOD_BITCOIN = MegaApi.PAYMENT_METHOD_BITCOIN;
    public final static int PAYMENT_METHOD_UNIONPAY = MegaApi.PAYMENT_METHOD_UNIONPAY;
    public final static int PAYMENT_METHOD_FORTUMO = MegaApi.PAYMENT_METHOD_FORTUMO;
    public final static int PAYMENT_METHOD_CREDIT_CARD = MegaApi.PAYMENT_METHOD_CREDIT_CARD;
    public final static int PAYMENT_METHOD_CENTILI = MegaApi.PAYMENT_METHOD_CENTILI;

    public final static int TRANSFER_METHOD_NORMAL = MegaApi.TRANSFER_METHOD_NORMAL;
    public final static int TRANSFER_METHOD_ALTERNATIVE_PORT = MegaApi.TRANSFER_METHOD_ALTERNATIVE_PORT;
    public final static int TRANSFER_METHOD_AUTO = MegaApi.TRANSFER_METHOD_AUTO;
    public final static int TRANSFER_METHOD_AUTO_NORMAL = MegaApi.TRANSFER_METHOD_AUTO_NORMAL;
    public final static int TRANSFER_METHOD_AUTO_ALTERNATIVE = MegaApi.TRANSFER_METHOD_AUTO_ALTERNATIVE;


    MegaApi getMegaApi()
    {
        return megaApi;
    }

    /**
     * Constructor suitable for most applications.
     *
     * @param appKey
     *            AppKey of your application.
     *            Generate an AppKey for free here: https://mega.co.nz/#sdk
     *
     * @param basePath
     *            Base path to store the local cache.
     *            If you pass null to this parameter, the SDK won't use any local cache.
     */
    public MegaApiJava(String appKey, String basePath) {
        megaApi = new MegaApi(appKey, basePath);
    }

    /**
     * MegaApi Constructor that allows use of a custom GFX processor.
     * <p>
     * The SDK attaches thumbnails and previews to all uploaded images. To generate them, it needs a graphics processor.
     * You can build the SDK with one of the provided built-in graphics processors. If none are available
     * in your app, you can implement the MegaGfxProcessor interface to provide a custom processor. Please
     * read the documentation of MegaGfxProcessor carefully to ensure that your implementation is valid.
     *
     * @param appKey
     *            AppKey of your application.
     *            Generate an AppKey for free here: https://mega.co.nz/#sdk
     *
     * @param userAgent
     *            User agent to use in network requests.
     *            If you pass null to this parameter, a default user agent will be used.
     *
     * @param basePath
     *            Base path to store the local cache.
     *            If you pass null to this parameter, the SDK won't use any local cache.
     *
     * @param gfxProcessor
     *            Image processor. The SDK will use it to generate previews and thumbnails.
     *            If you pass null to this parameter, the SDK will try to use the built-in image processors.
     *
     */
    public MegaApiJava(String appKey, String userAgent, String basePath, MegaGfxProcessor gfxProcessor) {
        this.gfxProcessor = gfxProcessor;
        megaApi = new MegaApi(appKey, gfxProcessor, basePath, userAgent);
    }

    /**
     * Constructor suitable for most applications.
     *
     * @param appKey
     *            AppKey of your application.
     *            Generate an AppKey for free here: https://mega.co.nz/#sdk
     */
    public MegaApiJava(String appKey) {
        megaApi = new MegaApi(appKey);
    }

    /****************************************************************************************************/
    // LISTENER MANAGEMENT
    /****************************************************************************************************/

    /**
     * Register a listener to receive all events (requests, transfers, global, synchronization).
     * <p>
     * You can use MegaApiJava.removeListener() to stop receiving events.
     *
     * @param listener
     *            Listener that will receive all events (requests, transfers, global, synchronization).
     */
    public void addListener(MegaListenerInterface listener) {
        megaApi.addListener(createDelegateMegaListener(listener));
    }

    /**
     * Register a listener to receive all events about requests.
     * <p>
     * You can use MegaApiJava.removeRequestListener() to stop receiving events.
     *
     * @param listener
     *            Listener that will receive all events about requests.
     */
    public void addRequestListener(MegaRequestListenerInterface listener) {
        megaApi.addRequestListener(createDelegateRequestListener(listener, false));
    }

    /**
     * Register a listener to receive all events about transfers.
     * <p>
     * You can use MegaApiJava.removeTransferListener() to stop receiving events.
     *
     * @param listener
     *            Listener that will receive all events about transfers.
     */
    public void addTransferListener(MegaTransferListenerInterface listener) {
        megaApi.addTransferListener(createDelegateTransferListener(listener, false));
    }

    /**
     * Register a listener to receive global events.
     * <p>
     * You can use MegaApiJava.removeGlobalListener() to stop receiving events.
     *
     * @param listener
     *            Listener that will receive global events.
     */
    public void addGlobalListener(MegaGlobalListenerInterface listener) {
        megaApi.addGlobalListener(createDelegateGlobalListener(listener));
    }

    /**
     * Unregister a listener.
     * <p>
     * Stop receiving events from the specified listener.
     *
     * @param listener
     *            Object that is unregistered.
     */
    public void removeListener(MegaListenerInterface listener) {
        ArrayList<DelegateMegaListener> listenersToRemove = new ArrayList<DelegateMegaListener>();

        synchronized (activeMegaListeners) {
            Iterator<DelegateMegaListener> it = activeMegaListeners.iterator();
            while (it.hasNext()) {
                DelegateMegaListener delegate = it.next();
                if (delegate.getUserListener() == listener) {
                    listenersToRemove.add(delegate);
                    it.remove();
                }
            }
        }

        for (int i=0;i<listenersToRemove.size();i++){
            megaApi.removeListener(listenersToRemove.get(i));
        }
    }

    /**
     * Unregister a MegaRequestListener.
     * <p>
     * Stop receiving events from the specified listener.
     *
     * @param listener
     *            Object that is unregistered.
     */
    public void removeRequestListener(MegaRequestListenerInterface listener) {
        ArrayList<DelegateMegaRequestListener> listenersToRemove = new ArrayList<DelegateMegaRequestListener>();
        synchronized (activeRequestListeners) {
            Iterator<DelegateMegaRequestListener> it = activeRequestListeners.iterator();
            while (it.hasNext()) {
                DelegateMegaRequestListener delegate = it.next();
                if (delegate.getUserListener() == listener) {
                    listenersToRemove.add(delegate);
                    it.remove();
                }
            }
        }

        for (int i=0;i<listenersToRemove.size();i++){
            megaApi.removeRequestListener(listenersToRemove.get(i));
        }
    }

    /**
     * Unregister a MegaTransferListener.
     * <p>
     * Stop receiving events from the specified listener.
     *
     * @param listener
     *            Object that is unregistered.
     */
    public void removeTransferListener(MegaTransferListenerInterface listener) {
        ArrayList<DelegateMegaTransferListener> listenersToRemove = new ArrayList<DelegateMegaTransferListener>();

        synchronized (activeTransferListeners) {
            Iterator<DelegateMegaTransferListener> it = activeTransferListeners.iterator();
            while (it.hasNext()) {
                DelegateMegaTransferListener delegate = it.next();
                if (delegate.getUserListener() == listener) {
                    listenersToRemove.add(delegate);
                    it.remove();
                }
            }
        }

        for (int i=0;i<listenersToRemove.size();i++){
            megaApi.removeTransferListener(listenersToRemove.get(i));
        }
    }

    /**
     * Unregister a MegaGlobalListener.
     * <p>
     * Stop receiving events from the specified listener.
     *
     * @param listener
     *            Object that is unregistered.
     */
    public void removeGlobalListener(MegaGlobalListenerInterface listener) {
        ArrayList<DelegateMegaGlobalListener> listenersToRemove = new ArrayList<DelegateMegaGlobalListener>();

        synchronized (activeGlobalListeners) {
            Iterator<DelegateMegaGlobalListener> it = activeGlobalListeners.iterator();
            while (it.hasNext()) {
                DelegateMegaGlobalListener delegate = it.next();
                if (delegate.getUserListener() == listener) {
                    listenersToRemove.add(delegate);
                    it.remove();
                }
            }
        }

        for (int i=0;i<listenersToRemove.size();i++){
            megaApi.removeGlobalListener(listenersToRemove.get(i));
        }
    }

    /****************************************************************************************************/
    // UTILS
    /****************************************************************************************************/

    /**
     * Generates a private key based on the access password.
     * <p>
     * This is a time consuming operation (particularly for low-end mobile devices). As the resulting key is
     * required to log in, this function allows to do this step in a separate function. You should run this function
     * in a background thread, to prevent UI hangs. The resulting key can be used in MegaApiJava.fastLogin().
     *
     * @param password
     *            Access password.
     * @return Base64-encoded private key.
     * @deprecated Legacy function soon to be removed.
     */
    @Deprecated public String getBase64PwKey(String password) {
        return megaApi.getBase64PwKey(password);
    }

    /**
     * Generates a hash based in the provided private key and email.
     * <p>
     * This is a time consuming operation (especially for low-end mobile devices). Since the resulting key is
     * required to log in, this function allows to do this step in a separate function. You should run this function
     * in a background thread, to prevent UI hangs. The resulting key can be used in MegaApiJava.fastLogin().
     *
     * @param base64pwkey
     *            Private key returned by MegaApiJava.getBase64PwKey().
     * @return Base64-encoded hash.
     * @deprecated Legacy function soon to be removed.
     */
    @Deprecated public String getStringHash(String base64pwkey, String inBuf) {
        return megaApi.getStringHash(base64pwkey, inBuf);
    }

    /**
     * Converts a Base32-encoded user handle (JID) to a MegaHandle.
     * <p>
     * @param base32Handle
     *            Base32-encoded handle (JID).
     * @return User handle.
     */
    public static long base32ToHandle(String base32Handle) {
        return MegaApi.base32ToHandle(base32Handle);
    }

    /**
     * Converts a Base64-encoded node handle to a MegaHandle.
     * <p>
     * The returned value can be used to recover a MegaNode using MegaApiJava.getNodeByHandle().
     * You can revert this operation using MegaApiJava.handleToBase64().
     *
     * @param base64Handle
     *            Base64-encoded node handle.
     * @return Node handle.
     */
    public static long base64ToHandle(String base64Handle) {
        return MegaApi.base64ToHandle(base64Handle);
    }

    /**
     * Converts a MegaHandle to a Base64-encoded string.
     * <p>
     * You can revert this operation using MegaApiJava.base64ToHandle().
     *
     * @param handle
     *            to be converted.
     * @return Base64-encoded node handle.
     */
    public static String handleToBase64(long handle) {
        return MegaApi.handleToBase64(handle);
    }

    /**
     * Converts a MegaHandle to a Base64-encoded string.
     * <p>
     * You take the ownership of the returned value.
     * You can revert this operation using MegaApiJava.base64ToHandle().
     *
     * @param handle
     *            handle to be converted.
     * @return Base64-encoded user handle.
     */
    public static String userHandleToBase64(long handle) {
        return MegaApi.userHandleToBase64(handle);
    }

    /**
     * Add entropy to internal random number generators.
     * <p>
     * It's recommended to call this function with random data to
     * enhance security.
     *
     * @param data
     *            Byte array with random data.
     * @param size
     *            Size of the byte array (in bytes).
     */
    public static void addEntropy(String data, long size) {
        MegaApi.addEntropy(data, size);
    }

    /**
     * Reconnect and retry all transfers.
     */
    public void reconnect() {
        megaApi.retryPendingConnections(true, true);
    }

    /**
     * Retry all pending requests.
     * <p>
     * When requests fails they wait some time before being retried. That delay grows exponentially if the request
     * fails again. For this reason, and since this request is very lightweight, it's recommended to call it with
     * the default parameters on every user interaction with the application. This will prevent very big delays
     * completing requests.
     */
    public void retryPendingConnections() {
        megaApi.retryPendingConnections();
    }

    /****************************************************************************************************/
    // REQUESTS
    /****************************************************************************************************/

    /**
     * Log in to a MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_LOGIN.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getEmail() - Returns the first parameter. <br>
     * - MegaRequest.getPassword() - Returns the second parameter.
     * <p>
     * If the email/password are not valid the error code provided in onRequestFinish() is
     * MegaError.API_ENOENT.
     *
     * @param email
     *            Email of the user.
     * @param password
     *            Password.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void login(String email, String password, MegaRequestListenerInterface listener) {
        megaApi.login(email, password, createDelegateRequestListener(listener));
    }

    /**
     * Log in to a MEGA account.
     * <p>
     * @param email
     *            Email of the user.
     * @param password
     *            Password.
     */
    public void login(String email, String password) {
        megaApi.login(email, password);
    }

    /**
     * Log in to a public folder using a folder link.
     * <p>
     * After a successful login, you should call MegaApiJava.fetchNodes() to get filesystem and
     * start working with the folder.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_LOGIN.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getEmail() - Returns the string "FOLDER". <br>
     * - MegaRequest.getLink() - Returns the public link to the folder.
     *
     * @param megaFolderLink
     *            link to a folder in MEGA.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void loginToFolder(String megaFolderLink, MegaRequestListenerInterface listener) {
        megaApi.loginToFolder(megaFolderLink, createDelegateRequestListener(listener));
    }

    /**
     * Log in to a public folder using a folder link.
     * <p>
     * After a successful login, you should call MegaApiJava.fetchNodes() to get filesystem and
     * start working with the folder.
     *
     * @param megaFolderLink
     *            link to a folder in MEGA.
     */
    public void loginToFolder(String megaFolderLink) {
        megaApi.loginToFolder(megaFolderLink);
    }

    /**
     * Log in to a MEGA account using precomputed keys.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_LOGIN.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getEmail() - Returns the first parameter. <br>
     * - MegaRequest.getPassword() - Returns the second parameter. <br>
     * - MegaRequest.getPrivateKey() - Returns the third parameter.
     * <p>
     * If the email/stringHash/base64pwKey are not valid the error code provided in onRequestFinish() is
     * MegaError.API_ENOENT.
     *
     * @param email
     *            Email of the user.
     * @param stringHash
     *            Hash of the email returned by MegaApiJava.getStringHash().
     * @param base64pwkey
     *            Private key calculated using MegaApiJava.getBase64PwKey().
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void fastLogin(String email, String stringHash, String base64pwkey, MegaRequestListenerInterface listener) {
        megaApi.fastLogin(email, stringHash, base64pwkey, createDelegateRequestListener(listener));
    }

    /**
     * Log in to a MEGA account using precomputed keys.
     *
     * @param email
     *            Email of the user.
     * @param stringHash
     *            Hash of the email returned by MegaApiJava.getStringHash().
     * @param base64pwkey
     *            Private key calculated using MegaApiJava.getBase64PwKey().
     */
    public void fastLogin(String email, String stringHash, String base64pwkey) {
        megaApi.fastLogin(email, stringHash, base64pwkey);
    }

    /**
     * Log in to a MEGA account using a session key.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_LOGIN.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getSessionKey() - Returns the session key.
     *
     * @param session
     *            Session key previously dumped with MegaApiJava.dumpSession().
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void fastLogin(String session, MegaRequestListenerInterface listener) {
        megaApi.fastLogin(session, createDelegateRequestListener(listener));
    }

    /**
     * Log in to a MEGA account using a session key.
     *
     * @param session
     *            Session key previously dumped with MegaApiJava.dumpSession().
     */
    public void fastLogin(String session) {
        megaApi.fastLogin(session);
    }

    /**
     * Close a MEGA session.
     *
     * All clients using this session will be automatically logged out.
     * <p>
     * You can get session information using MegaApiJava.getExtendedAccountDetails().
     * Then use MegaAccountDetails.getNumSessions and MegaAccountDetails.getSession
     * to get session info.
     * MegaAccountSession.getHandle provides the handle that this function needs.
     * <p>
     * If you use mega.INVALID_HANDLE, all sessions except the current one will be closed.
     *
     * @param sessionHandle
     *            of the session. Use mega.INVALID_HANDLE to cancel all sessions except the current one.
     * @param listener
     *            MegaRequestListenerInterface to track this request.
     */
    public void killSession(long sessionHandle, MegaRequestListenerInterface listener) {
        megaApi.killSession(sessionHandle, createDelegateRequestListener(listener));
    }

    /**
     * Close a MEGA session.
     * <p>
     * All clients using this session will be automatically logged out.
     * <p>
     * You can get session information using MegaApiJava.getExtendedAccountDetails().
     * Then use MegaAccountDetails.getNumSessions and MegaAccountDetails.getSession
     * to get session info.
     * MegaAccountSession.getHandle provides the handle that this function needs.
     * <p>
     * If you use mega.INVALID_HANDLE, all sessions except the current one will be closed.
     *
     * @param sessionHandle
     *            of the session. Use mega.INVALID_HANDLE to cancel all sessions except the current one.
     */
    public void killSession(long sessionHandle) {
        megaApi.killSession(sessionHandle);
    }

    /**
     * Get data about the logged account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_USER_DATA.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getName() - Returns the name of the logged user. <br>
     * - MegaRequest.getPassword() - Returns the the public RSA key of the account, Base64-encoded. <br>
     * - MegaRequest.getPrivateKey() - Returns the private RSA key of the account, Base64-encoded.
     *
     * @param listener
     *            MegaRequestListenerInterface to track this request.
     */
    public void getUserData(MegaRequestListenerInterface listener) {
        megaApi.getUserData(createDelegateRequestListener(listener));
    }

    /**
     * Get data about the logged account.
     *
     */
    public void getUserData() {
        megaApi.getUserData();
    }

    /**
     * Get data about a contact.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_USER_DATA.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest.getEmail - Returns the email of the contact
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getText() - Returns the XMPP ID of the contact. <br>
     * - MegaRequest.getPassword() - Returns the public RSA key of the contact, Base64-encoded.
     *
     * @param user
     *            Contact to get the data.
     * @param listener
     *            MegaRequestListenerInterface to track this request.
     */
    public void getUserData(MegaUser user, MegaRequestListenerInterface listener) {
        megaApi.getUserData(user, createDelegateRequestListener(listener));
    }

    /**
     * Get data about a contact.
     *
     * @param user
     *            Contact to get the data.
     */
    public void getUserData(MegaUser user) {
        megaApi.getUserData(user);
    }

    /**
     * Get data about a contact.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_USER_DATA.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getEmail() - Returns the email or the Base64 handle of the contact.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getText() - Returns the XMPP ID of the contact. <br>
     * - MegaRequest.getPassword() - Returns the public RSA key of the contact, Base64-encoded.
     *
     * @param user
     *            Email or Base64 handle of the contact.
     * @param listener
     *            MegaRequestListenerInterface to track this request.
     */
    public void getUserData(String user, MegaRequestListenerInterface listener) {
        megaApi.getUserData(user, createDelegateRequestListener(listener));
    }

    /**
     * Get data about a contact.
     *
     * @param user
     *            Email or Base64 handle of the contact.
     */
    public void getUserData(String user) {
        megaApi.getUserData(user);
    }

    /**
     * Returns the current session key.
     * <p>
     * You have to be logged in to get a valid session key. Otherwise,
     * this function returns null.
     *
     * @return Current session key.
     */
    public String dumpSession() {
        return megaApi.dumpSession();
    }

    /**
     * Returns the current XMPP session key.
     * <p>
     * You have to be logged in to get a valid session key. Otherwise,
     * this function returns null.
     *
     * @return Current XMPP session key.
     */
    public String dumpXMPPSession() {
        return megaApi.dumpXMPPSession();
    }

    /**
     * Initialize the creation of a new MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CREATE_ACCOUNT.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getEmail() - Returns the email for the account. <br>
     * - MegaRequest.getPassword() - Returns the password for the account. <br>
     * - MegaRequest.getName() - Returns the name of the user. <br>
     * <p>
     * If this request succeed, a confirmation email will be sent to the users.
     * If an account with the same email already exists, you will get the error code
     * MegaError.API_EEXIST in onRequestFinish().
     *
     * @param email
     *            Email for the account.
     * @param password
     *            Password for the account.
     * @param name
     *            Name of the user.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void createAccount(String email, String password, String name, MegaRequestListenerInterface listener) {
        megaApi.createAccount(email, password, name, createDelegateRequestListener(listener));
    }

    /**
     * Initialize the creation of a new MEGA account.
     *
     * @param email
     *            Email for the account.
     * @param password
     *            Password for the account.
     * @param name
     *            Name of the user.
     */
    public void createAccount(String email, String password, String name) {
        megaApi.createAccount(email, password, name);
    }

    /**
     * Initialize the creation of a new MEGA account, with firstname and lastname
     *
     * The associated request type with this request is MegaRequest::TYPE_CREATE_ACCOUNT.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the email for the account
     * - MegaRequest::getPassword - Returns the password for the account
     * - MegaRequest::getName - Returns the firstname of the user
     * - MegaRequest::getText - Returns the lastname of the user
     *
     * If this request succeed, a confirmation email will be sent to the users.
     * If an account with the same email already exists, you will get the error code
     * MegaError::API_EEXIST in onRequestFinish
     *
     * @param email Email for the account
     * @param password Password for the account
     * @param firstname Firstname of the user
     * @param lastname Lastname of the user
     * @param listener MegaRequestListenerInterface to track this request
     */
    public void createAccount(String email, String password, String firstname, String lastname, MegaRequestListenerInterface listener){
        megaApi.createAccount(email, password, firstname, lastname, createDelegateRequestListener(listener));
    }


    /**
     * Initialize the creation of a new MEGA account with precomputed keys.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CREATE_ACCOUNT.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getEmail() - Returns the email for the account. <br>
     * - MegaRequest.getPrivateKey() - Returns the private key calculated with MegaApiJava.getBase64PwKey(). <br>
     * - MegaRequest.getName() - Returns the name of the user.
     * <p>
     * If this request succeed, a confirmation email will be sent to the users.
     * If an account with the same email already exists, you will get the error code
     * MegaError.API_EEXIST in onRequestFinish().
     *
     * @param email
     *            Email for the account.
     * @param base64pwkey
     *            Private key calculated with MegMegaApiJavaaApi.getBase64PwKey().
     * @param name
     *            Name of the user.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void fastCreateAccount(String email, String base64pwkey, String name, MegaRequestListenerInterface listener) {
        megaApi.fastCreateAccount(email, base64pwkey, name, createDelegateRequestListener(listener));
    }

    /**
     * Initialize the creation of a new MEGA account with precomputed keys.
     *
     * @param email
     *            Email for the account.
     * @param base64pwkey
     *            Private key calculated with MegaApiJava.getBase64PwKey().
     * @param name
     *            Name of the user.
     */
    public void fastCreateAccount(String email, String base64pwkey, String name) {
        megaApi.fastCreateAccount(email, base64pwkey, name);
    }

    /**
     * Get information about a confirmation link.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_QUERY_SIGNUP_LINK.
     * Valid data in the MegaRequest object received on all callbacks: <br>
     * - MegaRequest.getLink() - Returns the confirmation link.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getEmail() - Return the email associated with the confirmation link. <br>
     * - MegaRequest.getName() - Returns the name associated with the confirmation link.
     *
     * @param link
     *            Confirmation link.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void querySignupLink(String link, MegaRequestListenerInterface listener) {
        megaApi.querySignupLink(link, createDelegateRequestListener(listener));
    }

    /**
     * Get information about a confirmation link.
     *
     * @param link
     *            Confirmation link.
     */
    public void querySignupLink(String link) {
        megaApi.querySignupLink(link);
    }

    /**
     * Confirm a MEGA account using a confirmation link and the user password.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CONFIRM_ACCOUNT
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getLink() - Returns the confirmation link. <br>
     * - MegaRequest.getPassword() - Returns the password.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getEmail() - Email of the account. <br>
     * - MegaRequest.getName() - Name of the user.
     *
     * @param link
     *            Confirmation link.
     * @param password
     *            Password for the account.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void confirmAccount(String link, String password, MegaRequestListenerInterface listener) {
        megaApi.confirmAccount(link, password, createDelegateRequestListener(listener));
    }

    /**
     * Confirm a MEGA account using a confirmation link and the user password.
     *
     * @param link
     *            Confirmation link.
     * @param password
     *            Password for the account.
     */
    public void confirmAccount(String link, String password) {
        megaApi.confirmAccount(link, password);
    }

    /**
     * Confirm a MEGA account using a confirmation link and a precomputed key.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CONFIRM_ACCOUNT
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getLink() - Returns the confirmation link. <br>
     * - MegaRequest.getPrivateKey() - Returns the base64pwkey parameter.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getEmail() - Email of the account. <br>
     * - MegaRequest.getName() - Name of the user.
     *
     * @param link
     *            Confirmation link.
     * @param base64pwkey
     *            Private key precomputed with MegaApiJava.getBase64PwKey().
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void fastConfirmAccount(String link, String base64pwkey, MegaRequestListenerInterface listener) {
        megaApi.fastConfirmAccount(link, base64pwkey, createDelegateRequestListener(listener));
    }

    /**
     * Confirm a MEGA account using a confirmation link and a precomputed key.
     *
     * @param link
     *            Confirmation link.
     * @param base64pwkey
     *            Private key precomputed with MegaApiJava.getBase64PwKey().
     */
    public void fastConfirmAccount(String link, String base64pwkey) {
        megaApi.fastConfirmAccount(link, base64pwkey);
    }

    /**
     * Initialize the reset of the existing password, with and without the Master Key.
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_RECOVERY_LINK.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the email for the account
     * - MegaRequest::getFlag - Returns whether the user has a backup of the master key or not.
     *
     * If this request succeed, a recovery link will be sent to the user.
     * If no account is registered under the provided email, you will get the error code
     * MegaError::API_EEXIST in onRequestFinish
     *
     * @param email Email used to register the account whose password wants to be reset.
     * @param hasMasterKey True if the user has a backup of the master key. Otherwise, false.
     * @param listener MegaRequestListener to track this request
     */

    public void resetPassword(String email, boolean hasMasterKey, MegaRequestListenerInterface listener){
        megaApi.resetPassword(email, hasMasterKey, createDelegateRequestListener(listener));
    }

    /**
     * Get information about a recovery link created by MegaApi::resetPassword.
     *
     * The associated request type with this request is MegaRequest::TYPE_QUERY_RECOVERY_LINK
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getLink - Returns the recovery link
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getEmail - Return the email associated with the link
     * - MegaRequest::getFlag - Return whether the link requires masterkey to reset password.
     *
     * @param link Recovery link (#recover)
     * @param listener MegaRequestListener to track this request
     */
    public void queryResetPasswordLink(String link, MegaRequestListenerInterface listener){
        megaApi.queryResetPasswordLink(link, createDelegateRequestListener(listener));
    }

    /**
     * Set a new password for the account pointed by the recovery link.
     *
     * Recovery links are created by calling MegaApi::resetPassword and may or may not
     * require to provide the Master Key.
     *
     * @see The flag of the MegaRequest::TYPE_QUERY_RECOVERY_LINK in MegaApi::queryResetPasswordLink.
     *
     * The associated request type with this request is MegaRequest::TYPE_CONFIRM_ACCOUNT
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getLink - Returns the recovery link
     * - MegaRequest::getPassword - Returns the new password
     * - MegaRequest::getPrivateKey - Returns the Master Key, when provided
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getEmail - Return the email associated with the link
     * - MegaRequest::getFlag - Return whether the link requires masterkey to reset password.
     *
     * @param link The recovery link sent to the user's email address.
     * @param newPwd The new password to be set.
     * @param masterKey Base64-encoded string containing the master key (optional).
     * @param listener MegaRequestListener to track this request
     */

    public void confirmResetPassword(String link, String newPwd, String masterKey, MegaRequestListenerInterface listener){
        megaApi.confirmResetPassword(link, newPwd, masterKey, createDelegateRequestListener(listener));
    }

    /**
     * Initialize the cancellation of an account.
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_CANCEL_LINK.
     *
     * If this request succeed, a cancellation link will be sent to the email address of the user.
     * If no user is logged in, you will get the error code MegaError::API_EACCESS in onRequestFinish().
     *
     * @see MegaApi::confirmCancelAccount
     *
     * @param listener MegaRequestListener to track this request
     */
    public void cancelAccount(MegaRequestListenerInterface listener){
        megaApi.cancelAccount(createDelegateRequestListener(listener));
    }

    /**
     * Get information about a cancel link created by MegaApi::cancelAccount.
     *
     * The associated request type with this request is MegaRequest::TYPE_QUERY_RECOVERY_LINK
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getLink - Returns the cancel link
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getEmail - Return the email associated with the link
     *
     * @param link Cancel link (#cancel)
     * @param listener MegaRequestListener to track this request
     */
    public void queryCancelLink(String link, MegaRequestListenerInterface listener){
        megaApi.queryCancelLink(link, createDelegateRequestListener(listener));
    }

    /**
     * Effectively parks the user's account without creating a new fresh account.
     *
     * The contents of the account will then be purged after 60 days. Once the account is
     * parked, the user needs to contact MEGA support to restore the account.
     *
     * The associated request type with this request is MegaRequest::TYPE_CONFIRM_CANCEL_LINK.
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getLink - Returns the recovery link
     * - MegaRequest::getPassword - Returns the new password
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getEmail - Return the email associated with the link
     *
     * @param link Cancellation link sent to the user's email address;
     * @param pwd Password for the account.
     * @param listener MegaRequestListener to track this request
     */

    public void confirmCancelAccount(String link, String pwd, MegaRequestListenerInterface listener){
        megaApi.confirmCancelAccount(link, pwd, createDelegateRequestListener(listener));
    }

    /**
     * Initialize the change of the email address associated to the account.
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_CHANGE_EMAIL_LINK.
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getEmail - Returns the email for the account
     *
     * If this request succeed, a change-email link will be sent to the specified email address.
     * If no user is logged in, you will get the error code MegaError::API_EACCESS in onRequestFinish().
     *
     * @param email The new email to be associated to the account.
     * @param listener MegaRequestListener to track this request
     */

    public void changeEmail(String email, MegaRequestListenerInterface listener){
        megaApi.changeEmail(email, createDelegateRequestListener(listener));
    }

    /**
     * Get information about a change-email link created by MegaApi::changeEmail.
     *
     * If no user is logged in, you will get the error code MegaError::API_EACCESS in onRequestFinish().
     *
     * The associated request type with this request is MegaRequest::TYPE_QUERY_RECOVERY_LINK
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getLink - Returns the change-email link
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getEmail - Return the email associated with the link
     *
     * @param link Change-email link (#verify)
     * @param listener MegaRequestListener to track this request
     */

    public void queryChangeEmailLink(String link, MegaRequestListenerInterface listener){
        megaApi.queryChangeEmailLink(link, createDelegateRequestListener(listener));
    }

    /**
     * Effectively changes the email address associated to the account.
     *
     * The associated request type with this request is MegaRequest::TYPE_CONFIRM_CHANGE_EMAIL_LINK.
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getLink - Returns the change-email link
     * - MegaRequest::getPassword - Returns the password
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getEmail - Return the email associated with the link
     *
     * @param link Change-email link sent to the user's email address.
     * @param pwd Password for the account.
     * @param listener MegaRequestListener to track this request
     */
    public void confirmChangeEmail(String link, String pwd, MegaRequestListenerInterface listener){
        megaApi.confirmChangeEmail(link, pwd, createDelegateRequestListener(listener));
    }

    /**
     * Set proxy settings.
     * <p>
     * The SDK will start using the provided proxy settings as soon as this function returns.
     *
     * @param proxySettings
     *            settings.
     * @see MegaProxy
     */
    public void setProxySettings(MegaProxy proxySettings) {
        megaApi.setProxySettings(proxySettings);
    }

    /**
     * Try to detect the system's proxy settings.
     *
     * Automatic proxy detection is currently supported on Windows only.
     * On other platforms, this function will return a MegaProxy object
     * of type MegaProxy.PROXY_NONE.
     *
     * @return MegaProxy object with the detected proxy settings.
     */
    public MegaProxy getAutoProxySettings() {
        return megaApi.getAutoProxySettings();
    }

    /**
     * Check if the MegaApi object is logged in.
     *
     * @return 0 if not logged in. Otherwise, a number >= 0.
     */
    public int isLoggedIn() {
        return megaApi.isLoggedIn();
    }

    /**
     * Returns the email of the currently open account.
     *
     * If the MegaApi object is not logged in or the email is not available,
     * this function returns null.
     *
     * @return Email of the account.
     */
    public String getMyEmail() {
        return megaApi.getMyEmail();
    }

    /**
     * Returns the user handle of the currently open account
     *
     * If the MegaApi object isn't logged in,
     * this function returns null
     *
     * @return User handle of the account
     */
    public String getMyUserHandle() {
        return megaApi.getMyUserHandle();
    }

    /**
     * Get the MegaUser of the currently open account
     *
     * If the MegaApi object isn't logged in, this function returns NULL.
     *
     * You take the ownership of the returned value
     *
     * @note The visibility of your own user is unhdefined and shouldn't be used.
     * @return MegaUser of the currently open account, otherwise NULL
     */

    public MegaUser getMyUser(){
        return megaApi.getMyUser();
    }

    /**
     * Returns the XMPP JID of the currently open account
     *
     * If the MegaApi object isn't logged in,
     * this function returns null
     *
     * @return XMPP JID of the current account
     */
    public String getMyXMPPJid() {
        return megaApi.getMyXMPPJid();
    }


    /**
     * Returns the fingerprint of the signing key of the currently open account
     *
     * If the MegaApi object isn't logged in or there's no signing key available,
     * this function returns NULL
     *
     * You take the ownership of the returned value.
     * Use delete [] to free it.
     *
     * @return Fingerprint of the signing key of the current account
     */
    public String getMyFingerprint(){
        return megaApi.getMyFingerprint();
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
     *            - MegaApiJava.LOG_LEVEL_FATAL = 0. <br>
     *            - MegaApiJava.LOG_LEVEL_ERROR = 1. <br>
     *            - MegaApiJava.LOG_LEVEL_WARNING = 2. <br>
     *            - MegaApiJava.LOG_LEVEL_INFO = 3. <br>
     *            - MegaApiJava.LOG_LEVEL_DEBUG = 4. <br>
     *            - MegaApiJava.LOG_LEVEL_MAX = 5.
     */
    public static void setLogLevel(int logLevel) {
        MegaApi.setLogLevel(logLevel);
    }

    /**
     * Set a MegaLogger implementation to receive SDK logs.
     * <p>
     * Logs received by this objects depends on the active log level.
     * By default, it is MegaApiJava.LOG_LEVEL_INFO. You can change it
     * using MegaApiJava.setLogLevel().
     *
     * @param megaLogger
     *            MegaLogger implementation.
     */
    public static void setLoggerObject(MegaLoggerInterface megaLogger) {
        DelegateMegaLogger newLogger = new DelegateMegaLogger(megaLogger);
        MegaApi.setLoggerObject(newLogger);
        logger = newLogger;
    }

    /**
     * Send a log to the logging system.
     * <p>
     * This log will be received by the active logger object (MegaApiJava.setLoggerObject()) if
     * the log level is the same or lower than the active log level (MegaApiJava.setLogLevel()).
     *
     * @param logLevel
     *            Log level for this message.
     * @param message
     *            Message for the logging system.
     * @param filename
     *            Origin of the log message.
     * @param line
     *            Line of code where this message was generated.
     */
    public static void log(int logLevel, String message, String filename, int line) {
        MegaApi.log(logLevel, message, filename, line);
    }

    /**
     * Send a log to the logging system.
     * <p>
     * This log will be received by the active logger object (MegaApiJava.setLoggerObject()) if
     * the log level is the same or lower than the active log level (MegaApiJava.setLogLevel()).
     *
     * @param logLevel
     *            Log level for this message.
     * @param message
     *            Message for the logging system.
     * @param filename
     *            Origin of the log message.
     */
    public static void log(int logLevel, String message, String filename) {
        MegaApi.log(logLevel, message, filename);
    }

    /**
     * Send a log to the logging system.
     * <p>
     * This log will be received by the active logger object (MegaApiJava.setLoggerObject()) if
     * the log level is the same or lower than the active log level (MegaApiJava.setLogLevel()).
     *
     * @param logLevel
     *            Log level for this message.
     * @param message
     *            Message for the logging system.
     */
    public static void log(int logLevel, String message) {
        MegaApi.log(logLevel, message);
    }

    /**
     * Create a folder in the MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CREATE_FOLDER
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getParentHandle() - Returns the handle of the parent folder. <br>
     * - MegaRequest.getName() - Returns the name of the new folder.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getNodeHandle() - Handle of the new folder.
     *
     * @param name
     *            Name of the new folder.
     * @param parent
     *            Parent folder.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void createFolder(String name, MegaNode parent, MegaRequestListenerInterface listener) {
        megaApi.createFolder(name, parent, createDelegateRequestListener(listener));
    }

    /**
     * Create a folder in the MEGA account.
     *
     * @param name
     *            Name of the new folder.
     * @param parent
     *            Parent folder.
     */
    public void createFolder(String name, MegaNode parent) {
        megaApi.createFolder(name, parent);
    }

    /**
     * Move a node in the MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_MOVE
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node to move. <br>
     * - MegaRequest.getParentHandle() - Returns the handle of the new parent for the node.
     *
     * @param node
     *            Node to move.
     * @param newParent
     *            New parent for the node.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void moveNode(MegaNode node, MegaNode newParent, MegaRequestListenerInterface listener) {
        megaApi.moveNode(node, newParent, createDelegateRequestListener(listener));
    }

    /**
     * Move a node in the MEGA account.
     *
     * @param node
     *            Node to move.
     * @param newParent
     *            New parent for the node.
     */
    public void moveNode(MegaNode node, MegaNode newParent) {
        megaApi.moveNode(node, newParent);
    }

    /**
     * Copy a node in the MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_COPY
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node to copy. <br>
     * - MegaRequest.getParentHandle() - Returns the handle of the new parent for the new node. <br>
     * - MegaRequest.getPublicMegaNode() - Returns the node to copy (if it is a public node).
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getNodeHandle() - Handle of the new node.
     *
     * @param node
     *            Node to copy.
     * @param newParent
     *            Parent for the new node.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void copyNode(MegaNode node, MegaNode newParent, MegaRequestListenerInterface listener) {
        megaApi.copyNode(node, newParent, createDelegateRequestListener(listener));
    }

    /**
     * Copy a node in the MEGA account.
     * <p>
     * @param node
     *            Node to copy.
     * @param newParent
     *            Parent for the new node.
     */
    public void copyNode(MegaNode node, MegaNode newParent) {
        megaApi.copyNode(node, newParent);
    }

    /**
     * Copy a node in the MEGA account changing the file name.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_COPY
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node to copy. <br>
     * - MegaRequest.getParentHandle() - Returns the handle of the new parent for the new node. <br>
     * - MegaRequest.getPublicMegaNode() - Returns the node to copy. <br>
     * - MegaRequest.getName() - Returns the name for the new node.
     *
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getNodeHandle() - Handle of the new node.
     *
     * @param node
     *            Node to copy.
     * @param newParent
     *            Parent for the new node.
     * @param newName
     *            Name for the new node. <br>
     *
     *            This parameter is only used if the original node is a file and it is not a public node,
     *            otherwise, it's ignored.
     *
     * @param listener
     *            MegaRequestListenerInterface to track this request.
     */
    public void copyNode(MegaNode node, MegaNode newParent, String newName, MegaRequestListenerInterface listener) {
        megaApi.copyNode(node, newParent, newName, createDelegateRequestListener(listener));
    }

    /**
     * Copy a node in the MEGA account changing the file name.
     *
     * @param node
     *            Node to copy.
     * @param newParent
     *            Parent for the new node.
     * @param newName
     *            Name for the new node. <br>
     *
     *            This parameter is only used if the original node is a file and it is not a public node,
     *            otherwise, it is ignored.
     *
     */
    public void copyNode(MegaNode node, MegaNode newParent, String newName) {
        megaApi.copyNode(node, newParent, newName);
    }

    /**
     * Rename a node in the MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_RENAME
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node to rename. <br>
     * - MegaRequest.getName() - Returns the new name for the node.
     *
     * @param node
     *            Node to modify.
     * @param newName
     *            New name for the node.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void renameNode(MegaNode node, String newName, MegaRequestListenerInterface listener) {
        megaApi.renameNode(node, newName, createDelegateRequestListener(listener));
    }

    /**
     * Rename a node in the MEGA account.
     *
     * @param node
     *            Node to modify.
     * @param newName
     *            New name for the node.
     */
    public void renameNode(MegaNode node, String newName) {
        megaApi.renameNode(node, newName);
    }

    /**
     * Remove a node from the MEGA account.
     * <p>
     * This function does not move the node to the Rubbish Bin, it fully removes the node. To move
     * the node to the Rubbish Bin use MegaApiJava.moveNode().
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_REMOVE
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node to remove.
     *
     * @param node
     *            Node to remove.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void remove(MegaNode node, MegaRequestListenerInterface listener) {
        megaApi.remove(node, createDelegateRequestListener(listener));
    }

    /**
     * Remove a node from the MEGA account.
     *
     * @param node
     *            Node to remove
     */
    public void remove(MegaNode node) {
        megaApi.remove(node);
    }

    /**
     * Clean the Rubbish Bin in the MEGA account
     *
     * This function effectively removes every node contained in the Rubbish Bin. In order to
     * avoid accidental deletions, you might want to warn the user about the action.
     *
     * The associated request type with this request is MegaRequest::TYPE_CLEAN_RUBBISH_BIN. This
     * request returns MegaError::API_ENOENT if the Rubbish bin is already empty.
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
    public void cleanRubbishBin(MegaRequestListenerInterface listener) {
        megaApi.cleanRubbishBin(createDelegateRequestListener(listener));
    }

    /**
     * Clean the Rubbish Bin in the MEGA account
     *
     */
    public void cleanRubbishBin() {
        megaApi.cleanRubbishBin();
    }

    /**
     * Send a node to the Inbox of another MEGA user using a MegaUser.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_COPY
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node to send. <br>
     * - MegaRequest.getEmail() - Returns the email of the user that receives the node.
     *
     * @param node
     *            Node to send.
     * @param user
     *            User that receives the node.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void sendFileToUser(MegaNode node, MegaUser user, MegaRequestListenerInterface listener) {
        megaApi.sendFileToUser(node, user, createDelegateRequestListener(listener));
    }

    /**
     * Send a node to the Inbox of another MEGA user using a MegaUser.
     *
     * @param node
     *            Node to send.
     * @param user
     *            User that receives the node.
     */
    public void sendFileToUser(MegaNode node, MegaUser user) {
        megaApi.sendFileToUser(node, user);
    }

    /**
     *
     * The associated request type with this request is MegaRequest::TYPE_COPY
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node to send
     * - MegaRequest::getEmail - Returns the email of the user that receives the node
     *
     * @param node Node to send
     * @param email Email of the user that receives the node
     * @param listener MegaRequestListener to track this request
     */
    public void sendFileToUser(MegaNode node, String email, MegaRequestListenerInterface listener){
        megaApi.sendFileToUser(node, email, createDelegateRequestListener(listener));
    }

    /**
     * Share or stop sharing a folder in MEGA with another user using a MegaUser.
     * <p>
     * To share a folder with an user, set the desired access level in the level parameter. If you
     * want to stop sharing a folder use the access level MegaShare.ACCESS_UNKNOWN.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_COPY
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the folder to share. <br>
     * - MegaRequest.getEmail() - Returns the email of the user that receives the shared folder. <br>
     * - MegaRequest.getAccess() - Returns the access that is granted to the user.
     *
     * @param node
     *            The folder to share. It must be a non-root folder.
     * @param user
     *            User that receives the shared folder.
     * @param level
     *            Permissions that are granted to the user. <br>
     *            Valid values for this parameter: <br>
     *            - MegaShare.ACCESS_UNKNOWN = -1.
     *            Stop sharing a folder with this user. <br>
     *
     *            - MegaShare.ACCESS_READ = 0. <br>
     *            - MegaShare.ACCESS_READWRITE = 1. <br>
     *            - MegaShare.ACCESS_FULL = 2. <br>
     *            - MegaShare.ACCESS_OWNER = 3.
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void share(MegaNode node, MegaUser user, int level, MegaRequestListenerInterface listener) {
        megaApi.share(node, user, level, createDelegateRequestListener(listener));
    }

    /**
     * Share or stop sharing a folder in MEGA with another user using a MegaUser.
     * <p>
     * To share a folder with an user, set the desired access level in the level parameter. If you
     * want to stop sharing a folder use the access level MegaShare.ACCESS_UNKNOWN.
     *
     * @param node
     *            The folder to share. It must be a non-root folder.
     * @param user
     *            User that receives the shared folder.
     * @param level
     *            Permissions that are granted to the user. <br>
     *            Valid values for this parameter: <br>
     *            - MegaShare.ACCESS_UNKNOWN = -1.
     *            Stop sharing a folder with this user.
     *
     *            - MegaShare.ACCESS_READ = 0. <br>
     *            - MegaShare.ACCESS_READWRITE = 1. <br>
     *            - MegaShare.ACCESS_FULL = 2. <br>
     *            - MegaShare.ACCESS_OWNER = 3.
     *
     */
    public void share(MegaNode node, MegaUser user, int level) {
        megaApi.share(node, user, level);
    }

    /**
     * Share or stop sharing a folder in MEGA with another user using his email.
     * <p>
     * To share a folder with an user, set the desired access level in the level parameter. If you
     * want to stop sharing a folder use the access level MegaShare.ACCESS_UNKNOWN.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_COPY
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the folder to share. <br>
     * - MegaRequest.getEmail() - Returns the email of the user that receives the shared folder. <br>
     * - MegaRequest.getAccess() - Returns the access that is granted to the user.
     *
     * @param node
     *            The folder to share. It must be a non-root folder.
     * @param email
     *            Email of the user that receives the shared folder. If it does not have a MEGA account,
     *            the folder will be shared anyway and the user will be invited to register an account.
     * @param level
     *            Permissions that are granted to the user. <br>
     *            Valid values for this parameter: <br>
     *            - MegaShare.ACCESS_UNKNOWN = -1.
     *            Stop sharing a folder with this user. <br>
     *
     *            - MegaShare.ACCESS_READ = 0. <br>
     *            - MegaShare.ACCESS_READWRITE = 1. <br>
     *            - MegaShare.ACCESS_FULL = 2. <br>
     *            - MegaShare.ACCESS_OWNER = 3.
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void share(MegaNode node, String email, int level, MegaRequestListenerInterface listener) {
        megaApi.share(node, email, level, createDelegateRequestListener(listener));
    }

    /**
     * Share or stop sharing a folder in MEGA with another user using his email.
     * <p>
     * To share a folder with an user, set the desired access level in the level parameter. If you
     * want to stop sharing a folder use the access level MegaShare.ACCESS_UNKNOWN.
     *
     * @param node
     *            The folder to share. It must be a non-root folder.
     * @param email
     *            Email of the user that receives the shared folder. If it does not have a MEGA account, the folder will be shared anyway
     *            and the user will be invited to register an account.
     * @param level
     *            Permissions that are granted to the user. <br>
     *            Valid values for this parameter: <br>
     *            - MegaShare.ACCESS_UNKNOWN = -1. <br>
     *            Stop sharing a folder with this user.
     *
     *            - MegaShare.ACCESS_READ = 0. <br>
     *            - MegaShare.ACCESS_READWRITE = 1. <br>
     *            - MegaShare.ACCESS_FULL = 2. <br>
     *            - MegaShare.ACCESS_OWNER = 3.
     */
    public void share(MegaNode node, String email, int level) {
        megaApi.share(node, email, level);
    }

    /**
     * Import a public link to the account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_IMPORT_LINK
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getLink() - Returns the public link to the file. <br>
     * - MegaRequest.getParentHandle() - Returns the folder that receives the imported file.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getNodeHandle() - Handle of the new node in the account.
     *
     * @param megaFileLink
     *            Public link to a file in MEGA.
     * @param parent
     *            Parent folder for the imported file.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void importFileLink(String megaFileLink, MegaNode parent, MegaRequestListenerInterface listener) {
        megaApi.importFileLink(megaFileLink, parent, createDelegateRequestListener(listener));
    }

    /**
     * Import a public link to the account.
     *
     * @param megaFileLink
     *            Public link to a file in MEGA.
     * @param parent
     *            Parent folder for the imported file.
     */
    public void importFileLink(String megaFileLink, MegaNode parent) {
        megaApi.importFileLink(megaFileLink, parent);
    }

    /**
     * Get a MegaNode from a public link to a file.
     * <p>
     * A public node can be imported using MegaApiJava.copy() or downloaded using MegaApiJava.startDownload().
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_PUBLIC_NODE
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getLink() - Returns the public link to the file.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getPublicMegaNode() - Public MegaNode corresponding to the public link.
     *
     * @param megaFileLink
     *            Public link to a file in MEGA.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void getPublicNode(String megaFileLink, MegaRequestListenerInterface listener) {
        megaApi.getPublicNode(megaFileLink, createDelegateRequestListener(listener));
    }

    /**
     * Get a MegaNode from a public link to a file.
     * <p>
     * A public node can be imported using MegaApiJava.copy() or downloaded using MegaApiJava.startDownload().
     *
     * @param megaFileLink
     *            Public link to a file in MEGA.
     */
    public void getPublicNode(String megaFileLink) {
        megaApi.getPublicNode(megaFileLink);
    }

    /**
     * Get the thumbnail of a node.
     * <p>
     * If the node does not have a thumbnail, the request fails with the MegaError.API_ENOENT
     * error code.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node. <br>
     * - MegaRequest.getFile() - Returns the destination path. <br>
     * - MegaRequest.getParamType() - Returns MegaApiJava.ATTR_TYPE_THUMBNAIL.
     *
     * @param node
     *            Node to get the thumbnail.
     * @param dstFilePath
     *            Destination path for the thumbnail.
     *            If this path is a local folder, it must end with a '\' or '/' character and (Base64-encoded handle + "0.jpg")
     *            will be used as the file name inside that folder. If the path does not finish with
     *            one of these characters, the file will be downloaded to a file in that path.
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void getThumbnail(MegaNode node, String dstFilePath, MegaRequestListenerInterface listener) {
        megaApi.getThumbnail(node, dstFilePath, createDelegateRequestListener(listener));
    }

    /**
     * Get the thumbnail of a node.
     * <p>
     * If the node does not have a thumbnail the request fails with the MegaError.API_ENOENT
     * error code.
     *
     * @param node
     *            Node to get the thumbnail.
     * @param dstFilePath
     *            Destination path for the thumbnail.
     *            If this path is a local folder, it must end with a '\' or '/' character and (Base64-encoded handle + "0.jpg")
     *            will be used as the file name inside that folder. If the path does not finish with
     *            one of these characters, the file will be downloaded to a file in that path.
     */
    public void getThumbnail(MegaNode node, String dstFilePath) {
        megaApi.getThumbnail(node, dstFilePath);
    }

    /**
     * Get the preview of a node.
     * <p>
     * If the node does not have a preview the request fails with the MegaError.API_ENOENT
     * error code.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node. <br>
     * - MegaRequest.getFile() - Returns the destination path. <br>
     * - MegaRequest.getParamType() - Returns MegaApiJava.ATTR_TYPE_PREVIEW.
     *
     * @param node
     *            Node to get the preview.
     * @param dstFilePath
     *            Destination path for the preview.
     *            If this path is a local folder, it must end with a '\' or '/' character and (Base64-encoded handle + "1.jpg")
     *            will be used as the file name inside that folder. If the path does not finish with
     *            one of these characters, the file will be downloaded to a file in that path.
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void getPreview(MegaNode node, String dstFilePath, MegaRequestListenerInterface listener) {
        megaApi.getPreview(node, dstFilePath, createDelegateRequestListener(listener));
    }

    /**
     * Get the preview of a node.
     * <p>
     * If the node does not have a preview the request fails with the MegaError.API_ENOENT
     * error code.
     *
     * @param node
     *            Node to get the preview.
     * @param dstFilePath
     *            Destination path for the preview.
     *            If this path is a local folder, it must end with a '\' or '/' character and (Base64-encoded handle + "1.jpg")
     *            will be used as the file name inside that folder. If the path does not finish with
     *            one of these characters, the file will be downloaded to a file in that path.
     */
    public void getPreview(MegaNode node, String dstFilePath) {
        megaApi.getPreview(node, dstFilePath);
    }

    /**
     * Get the avatar of a MegaUser.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getFile() - Returns the destination path. <br>
     * - MegaRequest.getEmail() - Returns the email of the user.
     *
     * @param user
     *            MegaUser to get the avatar.
     * @param dstFilePath
     *            Destination path for the avatar. It has to be a path to a file, not to a folder.
     *            If this path is a local folder, it must end with a '\' or '/' character and (email + "0.jpg")
     *            will be used as the file name inside that folder. If the path does not finish with
     *            one of these characters, the file will be downloaded to a file in that path.
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void getUserAvatar(MegaUser user, String dstFilePath, MegaRequestListenerInterface listener) {
        megaApi.getUserAvatar(user, dstFilePath, createDelegateRequestListener(listener));
    }

    /**
     * Get the avatar of a MegaUser.
     *
     * @param user
     *            MegaUser to get the avatar.
     * @param dstFilePath
     *            Destination path for the avatar. It has to be a path to a file, not to a folder.
     *            If this path is a local folder, it must end with a '\' or '/' character and (email + "0.jpg")
     *            will be used as the file name inside that folder. If the path does not finish with
     *            one of these characters, the file will be downloaded to a file in that path.
     */
    public void getUserAvatar(MegaUser user, String dstFilePath) {
        megaApi.getUserAvatar(user, dstFilePath);
    }

    /**
     * Get the avatar of any user in MEGA
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getFile - Returns the destination path
     * - MegaRequest::getEmail - Returns the email or the handle of the user (the provided one as parameter)
     *
     * @param user email_or_user Email or user handle (Base64 encoded) to get the avatar. If this parameter is
     * set to null, the avatar is obtained for the active account
     * @param dstFilePath Destination path for the avatar. It has to be a path to a file, not to a folder.
     * If this path is a local folder, it must end with a '\' or '/' character and (email + "0.jpg")
     * will be used as the file name inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
    public void getUserAvatar(String email_or_handle, String dstFilePath, MegaRequestListenerInterface listener) {
        megaApi.getUserAvatar(email_or_handle, dstFilePath, createDelegateRequestListener(listener));
    }

    /**
     * Get the avatar of any user in MEGA
     *
     * @param user email_or_user Email or user handle (Base64 encoded) to get the avatar. If this parameter is
     * set to null, the avatar is obtained for the active account
     * @param dstFilePath Destination path for the avatar. It has to be a path to a file, not to a folder.
     * If this path is a local folder, it must end with a '\' or '/' character and (email + "0.jpg")
     * will be used as the file name inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     */
    public void getUserAvatar(String email_or_handle, String dstFilePath) {
        megaApi.getUserAvatar(email_or_handle, dstFilePath);
    }

    /**
     * Get the avatar of the active account
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getFile - Returns the destination path
     * - MegaRequest::getEmail - Returns the email of the user
     *
     * @param dstFilePath Destination path for the avatar. It has to be a path to a file, not to a folder.
     * If this path is a local folder, it must end with a '\' or '/' character and (email + "0.jpg")
     * will be used as the file name inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
    public void getUserAvatar(String dstFilePath, MegaRequestListenerInterface listener) {
        megaApi.getUserAvatar(dstFilePath, createDelegateRequestListener(listener));
    }

    /**
     * Get the avatar of the active account
     *
     * @param dstFilePath Destination path for the avatar. It has to be a path to a file, not to a folder.
     * If this path is a local folder, it must end with a '\' or '/' character and (email + "0.jpg")
     * will be used as the file name inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     */
    public void getUserAvatar(String dstFilePath) {
        megaApi.getUserAvatar(dstFilePath);
    }

    /**
     * Get the default color for the avatar.
     *
     * This color should be used only when the user doesn't have an avatar.
     *
     * You take the ownership of the returned value.
     *
     * @param user MegaUser to get the color of the avatar. If this parameter is set to NULL, the color
     *  is obtained for the active account.
     * @return The RGB color as a string with 3 components in hex: #RGB. Ie. "#FF6A19"
     * If the user is not found, this function returns NULL.
     */
    public String getUserAvatarColor(MegaUser user){
        return megaApi.getUserAvatarColor(user);
    }

    /**
     * Get the default color for the avatar.
     *
     * This color should be used only when the user doesn't have an avatar.
     *
     * You take the ownership of the returned value.
     *
     * @param userhandle User handle (Base64 encoded) to get the avatar. If this parameter is
     * set to NULL, the avatar is obtained for the active account
     * @return The RGB color as a string with 3 components in hex: #RGB. Ie. "#FF6A19"
     * If the user is not found (invalid userhandle), this function returns NULL.
     */
    public String getUserAvatarColor(String userhandle){
        return megaApi.getUserAvatarColor(userhandle);
    }

    /**
     * Get an attribute of a MegaUser.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getParamType() - Returns the attribute type.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getText() - Returns the value of the attribute.
     *
     * @param user MegaUser to get the attribute
     * @param type Attribute type. Valid values are: <br>
     * MegaApi.USER_ATTR_FIRSTNAME = 1 Get the firstname of the user. <br>
     * MegaApi.USER_ATTR_LASTNAME = 2 Get the lastname of the user.
     *
     * @param listener MegaRequestListenerInterface to track this request.
     */
    public void getUserAttribute(MegaUser user, int type, MegaRequestListenerInterface listener) {
        megaApi.getUserAttribute(user, type, createDelegateRequestListener(listener));
    }

    /**
     * Get an attribute of a MegaUser.
     *
     * @param user MegaUser to get the attribute.
     * @param type Attribute type. Valid values are: <br>
     * MegaApi.USER_ATTR_FIRSTNAME = 1 Get the firstname of the user. <br>
     * MegaApi.USER_ATTR_LASTNAME = 2 Get the lastname of the user.
     */
    public void getUserAttribute(MegaUser user, int type) {
        megaApi.getUserAttribute(user, type);
    }

    /**
     * Get an attribute of any user in MEGA.
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type
     * - MegaRequest::getEmail - Returns the email or the handle of the user (the provided one as parameter)
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getText - Returns the value of the attribute
     *
     * @param user email_or_user Email or user handle (Base64 encoded) to get the attribute.
     * If this parameter is set to NULL, the attribute is obtained for the active account.
     * @param type Attribute type
     *
     * Valid values are:
     *
     * MegaApi::USER_ATTR_FIRSTNAME = 1
     * Get the firstname of the user
     * MegaApi::USER_ATTR_LASTNAME = 2
     * Get the lastname of the user
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
    public void getUserAttribute(String email_or_handle, int type, MegaRequestListenerInterface listener) {
        megaApi.getUserAttribute(email_or_handle, type, createDelegateRequestListener(listener));
    }

    /**
     * Get an attribute of any user in MEGA.
     *
     * @param user email_or_user Email or user handle (Base64 encoded) to get the attribute.
     * If this parameter is set to NULL, the attribute is obtained for the active account.
     * @param type Attribute type
     *
     * Valid values are:
     *
     * MegaApi::USER_ATTR_FIRSTNAME = 1
     * Get the firstname of the user
     * MegaApi::USER_ATTR_LASTNAME = 2
     * Get the lastname of the user
     */
    public void getUserAttribute(String email_or_handle, int type) {
        megaApi.getUserAttribute(email_or_handle, type);
    }

    /**
     * Get an attribute of the current account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_ATTR_USER.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getParamType() - Returns the attribute type.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getText() - Returns the value of the attribute.
     *
     * @param type Attribute type. Valid values are: <br>
     *
     * MegaApi.USER_ATTR_FIRSTNAME = 1 Get the firstname of the user. <br>
     * MegaApi.USER_ATTR_LASTNAME = 2 Get the lastname of the user.
     *
     * @param listener MegaRequestListenerInterface to track this request.
     */
    public void getUserAttribute(int type, MegaRequestListenerInterface listener) {
        megaApi.getUserAttribute(type, createDelegateRequestListener(listener));
    }

    /**
     * Get an attribute of the current account.
     *
     * @param type Attribute type. Valid values are: <br>
     * MegaApi.USER_ATTR_FIRSTNAME = 1 Get the firstname of the user. <br>
     * MegaApi.USER_ATTR_LASTNAME = 2 Get the lastname of the user.
     */
    public void getUserAttribute(int type) {
        megaApi.getUserAttribute(type);
    }

    /**
     * Cancel the retrieval of a thumbnail.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CANCEL_ATTR_FILE.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node. <br>
     * - MegaRequest.getParamType() - Returns MegaApiJava.ATTR_TYPE_THUMBNAIL.
     *
     * @param node
     *            Node to cancel the retrieval of the thumbnail.
     * @param listener
     *            MegaRequestListener to track this request.
     * @see #getThumbnail(MegaNode node, String dstFilePath)
     */
    public void cancelGetThumbnail(MegaNode node, MegaRequestListenerInterface listener) {
        megaApi.cancelGetThumbnail(node, createDelegateRequestListener(listener));
    }

    /**
     * Cancel the retrieval of a thumbnail.
     *
     * @param node
     *            Node to cancel the retrieval of the thumbnail.
     * @see #getThumbnail(MegaNode node, String dstFilePath)
     */
    public void cancelGetThumbnail(MegaNode node) {
        megaApi.cancelGetThumbnail(node);
    }

    /**
     * Cancel the retrieval of a preview.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CANCEL_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle - Returns the handle of the node. <br>
     * - MegaRequest.getParamType - Returns MegaApiJava.ATTR_TYPE_PREVIEW.
     *
     * @param node
     *            Node to cancel the retrieval of the preview.
     * @param listener
     *            MegaRequestListener to track this request.
     * @see MegaApi#getPreview(MegaNode node, String dstFilePath)
     */
    public void cancelGetPreview(MegaNode node, MegaRequestListenerInterface listener) {
        megaApi.cancelGetPreview(node, createDelegateRequestListener(listener));
    }

    /**
     * Cancel the retrieval of a preview.
     *
     * @param node
     *            Node to cancel the retrieval of the preview.
     * @see MegaApi#getPreview(MegaNode node, String dstFilePath)
     */
    public void cancelGetPreview(MegaNode node) {
        megaApi.cancelGetPreview(node);
    }

    /**
     * Set the thumbnail of a MegaNode.
     *
     * The associated request type with this request is MegaRequest.TYPE_SET_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node. <br>
     * - MegaRequest.getFile() - Returns the source path. <br>
     * - MegaRequest.getParamType() - Returns MegaApiJava.ATTR_TYPE_THUMBNAIL.
     *
     * @param node
     *            MegaNode to set the thumbnail.
     * @param srcFilePath
     *            Source path of the file that will be set as thumbnail.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void setThumbnail(MegaNode node, String srcFilePath, MegaRequestListenerInterface listener) {
        megaApi.setThumbnail(node, srcFilePath, createDelegateRequestListener(listener));
    }

    /**
     * Set the thumbnail of a MegaNode.
     *
     * @param node
     *            MegaNode to set the thumbnail.
     * @param srcFilePath
     *            Source path of the file that will be set as thumbnail.
     */
    public void setThumbnail(MegaNode node, String srcFilePath) {
        megaApi.setThumbnail(node, srcFilePath);
    }

    /**
     * Set the preview of a MegaNode.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_SET_ATTR_FILE.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node. <br>
     * - MegaRequest.getFile() - Returns the source path. <br>
     * - MegaRequest.getParamType() - Returns MegaApiJava.ATTR_TYPE_PREVIEW.
     *
     * @param node
     *            MegaNode to set the preview.
     * @param srcFilePath
     *            Source path of the file that will be set as preview.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void setPreview(MegaNode node, String srcFilePath, MegaRequestListenerInterface listener) {
        megaApi.setPreview(node, srcFilePath, createDelegateRequestListener(listener));
    }

    /**
     * Set the preview of a MegaNode.
     *
     * @param node
     *            MegaNode to set the preview.
     * @param srcFilePath
     *            Source path of the file that will be set as preview.
     */
    public void setPreview(MegaNode node, String srcFilePath) {
        megaApi.setPreview(node, srcFilePath);
    }

    /**
     * Set the avatar of the MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_SET_ATTR_USER.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getFile() - Returns the source path.
     *
     * @param srcFilePath
     *            Source path of the file that will be set as avatar.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void setAvatar(String srcFilePath, MegaRequestListenerInterface listener) {
        megaApi.setAvatar(srcFilePath, createDelegateRequestListener(listener));
    }

    /**
     * Set the avatar of the MEGA account.
     *
     * @param srcFilePath
     *            Source path of the file that will be set as avatar.
     */
    public void setAvatar(String srcFilePath) {
        megaApi.setAvatar(srcFilePath);
    }

    /**
     * Set an attribute of the current user.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_SET_ATTR_USER.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getParamType() - Returns the attribute type. <br>
     * - MegaRequest.getFile() - Returns the new value for the attribute.
     *
     * @param type
     *            Attribute type. Valid values are: <br>
     *
     *            USER_ATTR_FIRSTNAME = 1.
     *            Change the firstname of the user. <br>
     *            USER_ATTR_LASTNAME = 2.
     *            Change the lastname of the user.
     * @param value
     *            New attribute value.
     * @param listener
     *            MegaRequestListenerInterface to track this request.
     */
    public void setUserAttribute(int type, String value, MegaRequestListenerInterface listener) {
        megaApi.setUserAttribute(type, value, createDelegateRequestListener(listener));
    }

    /**
     * Set an attribute of the current user.
     *
     * @param type
     *            Attribute type. Valid values are: <br>
     *
     *            USER_ATTR_FIRSTNAME = 1.
     *            Change the firstname of the user. <br>
     *            USER_ATTR_LASTNAME = 2.
     *            Change the lastname of the user. <br>
     * @param value
     *            New attribute value.
     */
    public void setUserAttribute(int type, String value) {
        megaApi.setUserAttribute(type, value);
    }

    /**
     * Set a custom attribute for the node
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_NODE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node that receive the attribute
     * - MegaRequest::getName - Returns the name of the custom attribute
     * - MegaRequest::getText - Returns the tezt for the attribute
     *
     * The attribute name must be an UTF8 string with between 1 and 7 bytes
     * If the attribute already has a value, it will be replaced
     * If value is null, the attribute will be removed from the node
     *
     * @param node Node that will receive the attribute
     * @param attrName Name of the custom attribute.
     * The length of this parameter must be between 1 and 7 UTF8 bytes
     * @param value Value for the attribute
     * @param listener MegaRequestListenerInterface to track this request
     */
    public void setCustomNodeAttribute(MegaNode node, String attrName, String value, MegaRequestListenerInterface listener) {
        megaApi.setCustomNodeAttribute(node, attrName, value, createDelegateRequestListener(listener));
    }

    /**
     * Set a custom attribute for the node
     *
     * The attribute name must be an UTF8 string with between 1 and 7 bytes
     * If the attribute already has a value, it will be replaced
     * If value is null, the attribute will be removed from the node
     *
     * @param node Node that will receive the attribute
     * @param attrName Name of the custom attribute.
     * The length of this parameter must be between 1 and 7 UTF8 bytes
     * @param value Value for the attribute
     */
    public void setCustomNodeAttribute(MegaNode node, String attrName, String value) {
        megaApi.setCustomNodeAttribute(node, attrName, value);
    }

    /**
     * Set the duration of audio/video files as a node attribute.
     *
     * To remove the existing duration, set it to MegaNode::INVALID_DURATION.
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_NODE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node that receive the attribute
     * - MegaRequest::getNumber - Returns the number of seconds for the node
     * - MegaRequest::getFlag - Returns true (official attribute)
     * - MegaRequest::getParamType - Returns MegaApi::NODE_ATTR_DURATION
     *
     * @param node Node that will receive the information.
     * @param duration Length of the audio/video in seconds.
     * @param listener MegaRequestListener to track this request
     */
    public void setNodeDuration(MegaNode node, int duration,  MegaRequestListenerInterface listener){
        megaApi.setNodeDuration(node, duration, createDelegateRequestListener(listener));
    }

    /**
     * Set the GPS coordinates of image files as a node attribute.
     *
     * To remove the existing coordinates, set both the latitude and longitud to
     * the value MegaNode::INVALID_COORDINATE.
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_NODE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node that receive the attribute
     * - MegaRequest::getFlag - Returns true (official attribute)
     * - MegaRequest::getParamType - Returns MegaApi::NODE_ATTR_COORDINATES
     * - MegaRequest::getNumDetails - Returns the longitude, scaled to integer in the range of [0, 2^24]
     * - MegaRequest::getTransferTag() - Returns the latitude, scaled to integer in the range of [0, 2^24)
     *
     * @param node Node that will receive the information.
     * @param latitude Latitude in signed decimal degrees notation
     * @param longitude Longitude in signed decimal degrees notation
     * @param listener MegaRequestListener to track this request
     */
    public void setNodeCoordinates(MegaNode node, double latitude, double longitude,  MegaRequestListenerInterface listener){
        megaApi.setNodeCoordinates(node, latitude, longitude, createDelegateRequestListener(listener));
    }

    /**
     * Generate a public link of a file/folder in MEGA.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_EXPORT
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the node. <br>
     * - MegaRequest.getAccess() - Returns true.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getLink() - Public link.
     *
     * @param node
     *            MegaNode to get the public link.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void exportNode(MegaNode node, MegaRequestListenerInterface listener) {
        megaApi.exportNode(node, createDelegateRequestListener(listener));
    }

    /**
     * Generate a public link of a file/folder in MEGA.
     *
     * @param node
     *            MegaNode to get the public link.
     */
    public void exportNode(MegaNode node) {
        megaApi.exportNode(node);
    }

    /**
     * Generate a temporary public link of a file/folder in MEGA
     *
     * The associated request type with this request is MegaRequest::TYPE_EXPORT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node
     * - MegaRequest::getAccess - Returns true
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getLink - Public link
     *
     * @param node MegaNode to get the public link
     * @param expireTime Unix timestamp until the public link will be valid
     * @param listener MegaRequestListener to track this request
     *
     * @note A Unix timestamp represents the number of seconds since 00:00 hours, Jan 1, 1970 UTC
     */

    public void exportNode(MegaNode node, int expireTime, MegaRequestListenerInterface listener) {
        megaApi.exportNode(node, expireTime, createDelegateRequestListener(listener));
    }

    /**
     * Stop sharing a file/folder.
     *
     * The associated request type with this request is MegaRequest.TYPE_EXPORT.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle - Returns the handle of the node. <br>
     * - MegaRequest.getAccess - Returns false.
     *
     * @param node
     *            MegaNode to stop sharing.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void disableExport(MegaNode node, MegaRequestListenerInterface listener) {
        megaApi.disableExport(node, createDelegateRequestListener(listener));
    }

    /**
     * Stop sharing a file/folder.
     *
     * @param node
     *            MegaNode to stop sharing.
     */
    public void disableExport(MegaNode node) {
        megaApi.disableExport(node);
    }

    /**
     * Fetch the filesystem in MEGA.
     * <p>
     * The MegaApi object must be logged in in an account or a public folder
     * to successfully complete this request.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_FETCH_NODES.
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void fetchNodes(MegaRequestListenerInterface listener) {
        megaApi.fetchNodes(createDelegateRequestListener(listener));
    }

    /**
     * Fetch the filesystem in MEGA.
     * <p>
     * The MegaApi object must be logged in in an account or a public folder
     * to successfully complete this request.
     */
    public void fetchNodes() {
        megaApi.fetchNodes();
    }

    /**
     * Get details about the MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_ACCOUNT_DETAILS.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getMegaAccountDetails() - Details of the MEGA account.
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void getAccountDetails(MegaRequestListenerInterface listener) {
        megaApi.getAccountDetails(createDelegateRequestListener(listener));
    }

    /**
     * Get details about the MEGA account.
     */
    public void getAccountDetails() {
        megaApi.getAccountDetails();
    }

    /**
     * Get details about the MEGA account.
     * <p>
     * This function allows to optionally get data about sessions, transactions and purchases related to the account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_ACCOUNT_DETAILS.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getMegaAccountDetails() - Details of the MEGA account.
     *
     * @param sessions
     *              Boolean. Get sessions history if true. Do not get sessions history if false.
     * @param purchases
     *              Boolean. Get purchase history if true. Do not get purchase history if false.
     * @param transactions
     *              Boolean. Get transactions history if true. Do not get transactions history if false.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void getExtendedAccountDetails(boolean sessions, boolean purchases, boolean transactions, MegaRequestListenerInterface listener) {
        megaApi.getExtendedAccountDetails(sessions, purchases, transactions, createDelegateRequestListener(listener));
    }

    /**
     * Get details about the MEGA account.
     *
     * This function allows to optionally get data about sessions, transactions and purchases related to the account.
     *
     * @param sessions
     *              Boolean. Get sessions history if true. Do not get sessions history if false.
     * @param purchases
     *              Boolean. Get purchase history if true. Do not get purchase history if false.
     * @param transactions
     *              Boolean. Get transactions history if true. Do not get transactions history if false.
     */
    public void getExtendedAccountDetails(boolean sessions, boolean purchases, boolean transactions) {
        megaApi.getExtendedAccountDetails(sessions, purchases, transactions);
    }

    /**
     * Get details about the MEGA account.
     *
     * This function allows to optionally get data about sessions and purchases related to the account.
     *
     * @param sessions
     *              Boolean. Get sessions history if true. Do not get sessions history if false.
     * @param purchases
     *              Boolean. Get purchase history if true. Do not get purchase history if false.
     */
    public void getExtendedAccountDetails(boolean sessions, boolean purchases) {
        megaApi.getExtendedAccountDetails(sessions, purchases);
    }

    /**
     * Get details about the MEGA account.
     *
     * This function allows to optionally get data about sessions related to the account.
     *
     * @param sessions
     *              Boolean. Get sessions history if true. Do not get sessions history if false.
     */
    public void getExtendedAccountDetails(boolean sessions) {
        megaApi.getExtendedAccountDetails(sessions);
    }

    /**
     * Get details about the MEGA account.
     *
     */
    public void getExtendedAccountDetails() {
        megaApi.getExtendedAccountDetails();
    }

    /**
     * Get the available pricing plans to upgrade a MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_PRICING.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getPricing() - MegaPricing object with all pricing plans.
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void getPricing(MegaRequestListenerInterface listener) {
        megaApi.getPricing(createDelegateRequestListener(listener));
    }

    /**
     * Get the available pricing plans to upgrade a MEGA account.
     */
    public void getPricing() {
        megaApi.getPricing();
    }

    /**
     * Get the payment id for an upgrade.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_PAYMENT_ID
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the product.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getLink() - Payment link.
     *
     * @param productHandle
     *            Handle of the product (see MegaApiJava.getPricing()).
     * @param listener
     *            MegaRequestListener to track this request.
     * @see #getPricing()
     */
    public void getPaymentId(long productHandle, MegaRequestListenerInterface listener) {
        megaApi.getPaymentId(productHandle, createDelegateRequestListener(listener));
    }

    /**
     * Get the payment URL for an upgrade.
     *
     * @param productHandle
     *            Handle of the product (see MegaApiJava.getPricing()).
     *
     * @see #getPricing()
     */
    public void getPaymentId(long productHandle) {
        megaApi.getPaymentId(productHandle);
    }

    /**
     * Upgrade an account.
     *
     * @param productHandle Product handle to purchase.
     * It is possible to get all pricing plans with their product handles using
     * MegaApi.getPricing().
     *
     * The associated request type with this request is MegaRequest.TYPE_UPGRADE_ACCOUNT.
     *
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the product. <br>
     * - MegaRequest.getNumber() - Returns the payment method.
     *
     * @param paymentMethod Payment method.
     * Valid values are: <br>
     * - MegaApi.PAYMENT_METHOD_BALANCE = 0.
     * Use the account balance for the payment. <br>
     *
     * - MegaApi.PAYMENT_METHOD_CREDIT_CARD = 8.
     * Complete the payment with your credit card. Use MegaApi.creditCardStore to add
     * a credit card to your account.
     *
     * @param listener MegaRequestListener to track this request.
     */
    public void upgradeAccount(long productHandle, int paymentMethod, MegaRequestListenerInterface listener) {
        megaApi.upgradeAccount(productHandle, paymentMethod, createDelegateRequestListener(listener));
    }

    /**
     * Upgrade an account.
     *
     * @param productHandle Product handle to purchase.
     * It is possible to get all pricing plans with their product handles using
     * MegaApi.getPricing().
     *
     * @param paymentMethod Payment method.
     * Valid values are: <br>
     * - MegaApi.PAYMENT_METHOD_BALANCE = 0.
     * Use the account balance for the payment. <br>
     *
     * - MegaApi.PAYMENT_METHOD_CREDIT_CARD = 8.
     * Complete the payment with your credit card. Use MegaApi.creditCardStore() to add
     * a credit card to your account.
     */
    public void upgradeAccount(long productHandle, int paymentMethod) {
        megaApi.upgradeAccount(productHandle, paymentMethod);
    }

    /**
     * Send the Google Play receipt after a correct purchase of a subscription.
     *
     * @param receipt
     *            String The complete receipt from Google Play.
     * @param listener
     *            MegaRequestListener to track this request.
     *
     */
    public void submitPurchaseReceipt(String receipt, MegaRequestListenerInterface listener) {
        megaApi.submitPurchaseReceipt(receipt, createDelegateRequestListener(listener));
    }

    /**
     * Send the Google Play receipt after a correct purchase of a subscription.
     *
     * @param receipt
     *            String The complete receipt from Google Play.
     *
     */
    public void submitPurchaseReceipt(String receipt) {
        megaApi.submitPurchaseReceipt(receipt);
    }

    /**
     * Store a credit card.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CREDIT_CARD_STORE.
     *
     * @param address1 Billing address.
     * @param address2 Second line of the billing address (optional).
     * @param city City of the billing address.
     * @param province Province of the billing address.
     * @param country Country of the billing address.
     * @param postalcode Postal code of the billing address.
     * @param firstname Firstname of the owner of the credit card.
     * @param lastname Lastname of the owner of the credit card.
     * @param creditcard Credit card number. Only digits, no spaces nor dashes.
     * @param expire_month Expire month of the credit card. Must have two digits ("03" for example).
     * @param expire_year Expire year of the credit card. Must have four digits ("2010" for example).
     * @param cv2 Security code of the credit card (3 digits).
     * @param listener MegaRequestListener to track this request.
     */
    public void creditCardStore(String address1, String address2, String city, String province, String country, String postalcode, String firstname, String lastname, String creditcard, String expire_month, String expire_year, String cv2, MegaRequestListenerInterface listener) {
        megaApi.creditCardStore(address1, address2, city, province, country, postalcode, firstname, lastname, creditcard, expire_month, expire_year, cv2, createDelegateRequestListener(listener));
    }

    /**
     * Store a credit card.
     *
     * @param address1 Billing address.
     * @param address2 Second line of the billing address (optional).
     * @param city City of the billing address.
     * @param province Province of the billing address.
     * @param country Country of the billing address.
     * @param postalcode Postal code of the billing address.
     * @param firstname Firstname of the owner of the credit card.
     * @param lastname Lastname of the owner of the credit card.
     * @param creditcard Credit card number. Only digits, no spaces nor dashes.
     * @param expire_month Expire month of the credit card. Must have two digits ("03" for example).
     * @param expire_year Expire year of the credit card. Must have four digits ("2010" for example).
     * @param cv2 Security code of the credit card (3 digits).
     */
    public void creditCardStore(String address1, String address2, String city, String province, String country, String postalcode, String firstname, String lastname, String creditcard, String expire_month, String expire_year, String cv2) {
        megaApi.creditCardStore(address1, address2, city, province, country, postalcode, firstname, lastname, creditcard, expire_month, expire_year, cv2);
    }

    /**
     * Get the credit card subscriptions of the account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getNumber() - Number of credit card subscriptions.
     *
     * @param listener MegaRequestListener to track this request.
     */
    public void creditCardQuerySubscriptions(MegaRequestListenerInterface listener) {
        megaApi.creditCardQuerySubscriptions(createDelegateRequestListener(listener));
    }

    /**
     * Get the credit card subscriptions of the account.
     *
     */
    public void creditCardQuerySubscriptions() {
        megaApi.creditCardQuerySubscriptions();
    }

    /**
     * Cancel credit card subscriptions of the account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CREDIT_CARD_CANCEL_SUBSCRIPTIONS.
     *
     * @param reason Reason for the cancellation. It can be null.
     * @param listener MegaRequestListener to track this request.
     */
    public void creditCardCancelSubscriptions(String reason, MegaRequestListenerInterface listener) {
        megaApi.creditCardCancelSubscriptions(reason, createDelegateRequestListener(listener));
    }

    /**
     * Cancel credit card subscriptions of the account.
     *
     * @param reason Reason for the cancellation. It can be null.
     *
     */
    public void creditCardCancelSubscriptions(String reason) {
        megaApi.creditCardCancelSubscriptions(reason);
    }

    /**
     * Get the available payment methods.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_GET_PAYMENT_METHODS.
     * <p>
     * Valid data in the MegaRequest object received in onRequestFinish() when the error code
     * is MegaError.API_OK: <br>
     * - MegaRequest.getNumber() - Bitfield with available payment methods.
     *
     * To identify if a payment method is available, the following check can be performed: <br>
     * (request.getNumber() & (1 << MegaApiJava.PAYMENT_METHOD_CREDIT_CARD) != 0).
     *
     * @param listener MegaRequestListener to track this request.
     */
    public void getPaymentMethods(MegaRequestListenerInterface listener) {
        megaApi.getPaymentMethods(createDelegateRequestListener(listener));
    }

    /**
     * Get the available payment methods.
     */
    public void getPaymentMethods() {
        megaApi.getPaymentMethods();
    }

    /**
     * Export the master key of the account.
     * <p>
     * The returned value is a Base64-encoded string.
     * <p>
     * With the master key, it's possible to start the recovery of an account when the
     * password is lost: <br>
     * - https://mega.co.nz/#recovery.
     *
     * @return Base64-encoded master key.
     */
    public String exportMasterKey() {
        return megaApi.exportMasterKey();
    }

    /**
     * Change the password of the MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CHANGE_PW
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getPassword - Returns the old password. <br>
     * - MegaRequest.getNewPassword - Returns the new password.
     *
     * @param oldPassword
     *            Old password.
     * @param newPassword
     *            New password.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void changePassword(String oldPassword, String newPassword, MegaRequestListenerInterface listener) {
        megaApi.changePassword(oldPassword, newPassword, createDelegateRequestListener(listener));
    }

    /**
     * Change the password of the MEGA account.
     *
     * @param oldPassword
     *            Old password.
     * @param newPassword
     *            New password.
     */
    public void changePassword(String oldPassword, String newPassword) {
        megaApi.changePassword(oldPassword, newPassword);
    }

    /**
     * Invite another person to be your MEGA contact.
     * <p>
     * The user does not need to be registered with MEGA. If the email is not associated with
     * a MEGA account, an invitation email will be sent with the text in the "message" parameter.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_INVITE_CONTACT.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getEmail() - Returns the email of the contact. <br>
     * - MegaRequest.getText() - Returns the text of the invitation.
     *
     * @param email Email of the new contact.
     * @param message Message for the user (can be null).
     * @param action Action for this contact request. Valid values are: <br>
     * - MegaContactRequest.INVITE_ACTION_ADD = 0. <br>
     * - MegaContactRequest.INVITE_ACTION_DELETE = 1. <br>
     * - MegaContactRequest.INVITE_ACTION_REMIND = 2.
     *
     * @param listener MegaRequestListenerInterface to track this request.
     */
    public void inviteContact(String email, String message, int action, MegaRequestListenerInterface listener) {
        megaApi.inviteContact(email, message, action, createDelegateRequestListener(listener));
    }

    /**
     * Invite another person to be your MEGA contact.
     * <p>
     * The user does not need to be registered on MEGA. If the email is not associated with
     * a MEGA account, an invitation email will be sent with the text in the "message" parameter.
     *
     * @param email Email of the new contact.
     * @param message Message for the user (can be null).
     * @param action Action for this contact request. Valid values are: <br>
     * - MegaContactRequest.INVITE_ACTION_ADD = 0. <br>
     * - MegaContactRequest.INVITE_ACTION_DELETE = 1. <br>
     * - MegaContactRequest.INVITE_ACTION_REMIND = 2.
     */
    public void inviteContact(String email, String message, int action) {
        megaApi.inviteContact(email, message, action);
    }

    /**
     * Reply to a contact request.
     *
     * @param request Contact request. You can get your pending contact requests using
     *                MegaApi.getIncomingContactRequests().
     * @param action Action for this contact request. Valid values are: <br>
     * - MegaContactRequest.REPLY_ACTION_ACCEPT = 0. <br>
     * - MegaContactRequest.REPLY_ACTION_DENY = 1. <br>
     * - MegaContactRequest.REPLY_ACTION_IGNORE = 2. <br>
     *
     * The associated request type with this request is MegaRequest.TYPE_REPLY_CONTACT_REQUEST.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getNodeHandle() - Returns the handle of the contact request. <br>
     * - MegaRequest.getNumber() - Returns the action. <br>
     *
     * @param listener MegaRequestListenerInterface to track this request.
     */
    public void replyContactRequest(MegaContactRequest request, int action, MegaRequestListenerInterface listener) {
        megaApi.replyContactRequest(request, action, createDelegateRequestListener(listener));
    }

    /**
     * Reply to a contact request.
     *
     * @param request Contact request. You can get your pending contact requests using MegaApi.getIncomingContactRequests()
     * @param action Action for this contact request. Valid values are: <br>
     * - MegaContactRequest.REPLY_ACTION_ACCEPT = 0. <br>
     * - MegaContactRequest.REPLY_ACTION_DENY = 1. <br>
     * - MegaContactRequest.REPLY_ACTION_IGNORE = 2.
     */
    public void replyContactRequest(MegaContactRequest request, int action) {
        megaApi.replyContactRequest(request, action);
    }

    /**
     * Remove a contact to the MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_REMOVE_CONTACT.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getEmail() - Returns the email of the contact.
     *
     * @param user
     *            Email of the contact.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void removeContact(MegaUser user, MegaRequestListenerInterface listener) {
        megaApi.removeContact(user, createDelegateRequestListener(listener));
    }

    /**
     * Remove a contact to the MEGA account.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_REMOVE_CONTACT.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getEmail() - Returns the email of the contact.
     * @param user
     *            Email of the contact.
     */
    public void removeContact(MegaUser user) {
        megaApi.removeContact(user);
    }

    /**
     * Logout of the MEGA account.
     *
     * The associated request type with this request is MegaRequest.TYPE_LOGOUT
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void logout(MegaRequestListenerInterface listener) {
        megaApi.logout(createDelegateRequestListener(listener));
    }

    /**
     * Logout of the MEGA account.
     */
    public void logout() {
        megaApi.logout();
    }

    /**
     * Logout of the MEGA account without invalidating the session.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_LOGOUT.
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void localLogout(MegaRequestListenerInterface listener) {
        megaApi.localLogout(createDelegateRequestListener(listener));
    }

    /**
     * Logout of the MEGA account without invalidating the session.
     *
     */
    public void localLogout() {
        megaApi.localLogout();
    }

    /**
     * @brief Invalidate the existing cache and create a fresh one
     */
    public void invalidateCache(){
        //megaApi.invalidateCache();
    }

    /**
     * Submit feedback about the app.
     * <p>
     * The User-Agent is used to identify the app. It can be set in MegaApiJava.MegaApi().
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_REPORT_EVENT.
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getParamType() - Returns MegaApiJava.EVENT_FEEDBACK. <br>
     * - MegaRequest.getText() - Returns the comment about the app. <br>
     * - MegaRequest.getNumber() - Returns the rating for the app.
     *
     * @param rating
     *            Integer to rate the app. Valid values: from 1 to 5.
     * @param comment
     *            Comment about the app.
     * @param listener
     *            MegaRequestListener to track this request.
     * @deprecated This function is for internal usage of MEGA apps. This feedback
     *             is sent to MEGA servers.
     *
     */
    @Deprecated public void submitFeedback(int rating, String comment, MegaRequestListenerInterface listener) {
        megaApi.submitFeedback(rating, comment, createDelegateRequestListener(listener));
    }

    /**
     * Submit feedback about the app.
     * <p>
     * The User-Agent is used to identify the app. It can be set in MegaApiJava.MegaApi().
     *
     * @param rating
     *            Integer to rate the app. Valid values: from 1 to 5.
     * @param comment
     *            Comment about the app.
     * @deprecated This function is for internal usage of MEGA apps. This feedback
     *             is sent to MEGA servers.
     *
     */
    @Deprecated public void submitFeedback(int rating, String comment) {
        megaApi.submitFeedback(rating, comment);
    }

    /**
     * Send a debug report.
     * <p>
     * The User-Agent is used to identify the app. It can be set in MegaApiJava.MegaApi()
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_REPORT_EVENT
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getParamType() - Returns MegaApiJava.EVENT_DEBUG. <br>
     * - MegaRequest.getText() - Returns the debug message.
     *
     * @param text
     *            Debug message
     * @param listener
     *            MegaRequestListener to track this request.
     * @deprecated This function is for internal usage of MEGA apps. This feedback
     *             is sent to MEGA servers.
     */
    @Deprecated public void reportDebugEvent(String text, MegaRequestListenerInterface listener) {
        megaApi.reportDebugEvent(text, createDelegateRequestListener(listener));
    }

    /**
     * Send a debug report.
     * <p>
     * The User-Agent is used to identify the app. It can be set in MegaApiJava.MegaApi().
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_REPORT_EVENT
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getParamType() - Returns MegaApiJava.EVENT_DEBUG. <br>
     * - MegaRequest.getText() - Returns the debug message.
     *
     * @param text
     *            Debug message.
     * @deprecated This function is for internal usage of MEGA apps. This feedback
     *             is sent to MEGA servers.
     */
    @Deprecated public void reportDebugEvent(String text) {
        megaApi.reportDebugEvent(text);
    }

    /**
     * Use HTTPS communications only
     *
     * The default behavior is to use HTTP for transfers and the persistent connection
     * to wait for external events. Those communications don't require HTTPS because
     * all transfer data is already end-to-end encrypted and no data is transmitted
     * over the connection to wait for events (it's just closed when there are new events).
     *
     * This feature should only be enabled if there are problems to contact MEGA servers
     * through HTTP because otherwise it doesn't have any benefit and will cause a
     * higher CPU usage.
     *
     * See MegaApi::usingHttpsOnly
     *
     * @param httpsOnly True to use HTTPS communications only
     */
    public void useHttpsOnly(boolean httpsOnly) {
        megaApi.useHttpsOnly(httpsOnly);
    }

    /**
     * Check if the SDK is using HTTPS communications only
     *
     * The default behavior is to use HTTP for transfers and the persistent connection
     * to wait for external events. Those communications don't require HTTPS because
     * all transfer data is already end-to-end encrypted and no data is transmitted
     * over the connection to wait for events (it's just closed when there are new events).
     *
     * See MegaApi::useHttpsOnly
     *
     * @return True if the SDK is using HTTPS communications only. Otherwise false.
     */
    public boolean usingHttpsOnly() {
        return megaApi.usingHttpsOnly();
    }

    /****************************************************************************************************/
    // TRANSFERS
    /****************************************************************************************************/

    /**
     * Upload a file.
     *
     * @param localPath
     *            path of the file.
     * @param parent
     *            node for the file in the MEGA account.
     * @param listener
     *            MegaTransferListener to track this transfer.
     */
    public void startUpload(String localPath, MegaNode parent, MegaTransferListenerInterface listener) {
        megaApi.startUpload(localPath, parent, createDelegateTransferListener(listener));
    }

    /**
     * Upload a file.
     *
     * @param localPath
     *            path of the file.
     * @param parent
     *            node for the file in the MEGA account.
     */
    public void startUpload(String localPath, MegaNode parent) {
        megaApi.startUpload(localPath, parent);
    }

    /**
     * Upload a file with a custom modification time.
     *
     * @param localPath
     *            Local path of the file.
     * @param parent
     *            Parent node for the file in the MEGA account.
     * @param mtime
     *            Custom modification time for the file in MEGA (in seconds since the epoch).
     * @param listener
     *            MegaTransferListener to track this transfer.
     */
    public void startUpload(String localPath, MegaNode parent, long mtime, MegaTransferListenerInterface listener) {
        megaApi.startUpload(localPath, parent, mtime, createDelegateTransferListener(listener));
    }

    /**
     * Upload a file with a custom modification time.
     *
     * @param localPath
     *            Local path of the file.
     * @param parent
     *            Parent node for the file in the MEGA account.
     * @param mtime
     *            Custom modification time for the file in MEGA (in seconds since the epoch).
     */
    public void startUpload(String localPath, MegaNode parent, long mtime) {
        megaApi.startUpload(localPath, parent, mtime);
    }

    /**
     * Upload a file with a custom name.
     *
     * @param localPath
     *            Local path of the file.
     * @param parent
     *            Parent node for the file in the MEGA account.
     * @param fileName
     *            Custom file name for the file in MEGA.
     * @param listener
     *            MegaTransferListener to track this transfer.
     */
    public void startUpload(String localPath, MegaNode parent, String fileName, MegaTransferListenerInterface listener) {
        megaApi.startUpload(localPath, parent, fileName, createDelegateTransferListener(listener));
    }

    /**
     * Upload a file with a custom name.
     *
     * @param localPath
     *            Local path of the file.
     * @param parent
     *            Parent node for the file in the MEGA account.
     * @param fileName
     *            Custom file name for the file in MEGA.
     */
    public void startUpload(String localPath, MegaNode parent, String fileName) {
        megaApi.startUpload(localPath, parent, fileName);
    }

    /**
     * Upload a file with a custom name and a custom modification time.
     *
     * @param localPath
     *            Local path of the file.
     * @param parent
     *            Parent node for the file in the MEGA account.
     * @param fileName
     *            Custom file name for the file in MEGA.
     * @param mtime
     *            Custom modification time for the file in MEGA (in seconds since the epoch).
     * @param listener
     *            MegaTransferListener to track this transfer.
     */
    public void startUpload(String localPath, MegaNode parent, String fileName, long mtime, MegaTransferListenerInterface listener) {
        megaApi.startUpload(localPath, parent, fileName, mtime, createDelegateTransferListener(listener));
    }

    /**
     * Upload a file with a custom name and a custom modification time.
     *
     * @param localPath
     *            Local path of the file.
     * @param parent
     *            Parent node for the file in the MEGA account.
     * @param fileName
     *            Custom file name for the file in MEGA.
     * @param mtime
     *            Custom modification time for the file in MEGA (in seconds since the epoch).
     */
    public void startUpload(String localPath, MegaNode parent, String fileName, long mtime) {
        megaApi.startUpload(localPath, parent, fileName, mtime);
    }

    /**
     * Download a file from MEGA.
     *
     * @param node
     *            MegaNode that identifies the file.
     * @param localPath
     *            Destination path for the file.
     *            If this path is a local folder, it must end with a '\' or '/' character and the file name
     *            in MEGA will be used to store a file inside that folder. If the path does not finish with
     *            one of these characters, the file will be downloaded to a file in that path.
     *
     * @param listener
     *            MegaTransferListener to track this transfer.
     */
    public void startDownload(MegaNode node, String localPath, MegaTransferListenerInterface listener) {
        megaApi.startDownload(node, localPath, createDelegateTransferListener(listener));
    }

    /**
     * Download a file from MEGA.
     *
     * @param node
     *            MegaNode that identifies the file.
     * @param localPath
     *            Destination path for the file.
     *            If this path is a local folder, it must end with a '\' or '/' character and the file name
     *            in MEGA will be used to store a file inside that folder. If the path does not finish with
     *            one of these characters, the file will be downloaded to a file in that path.
     */
    public void startDownload(MegaNode node, String localPath) {
        megaApi.startDownload(node, localPath);
    }

    /**
     * Start a streaming download.
     * <p>
     * Streaming downloads do not save the downloaded data into a local file. It is provided
     * in MegaTransferListener.onTransferUpdate() in a byte buffer. The pointer is returned by
     * MegaTransfer.getLastBytes() and the size of the buffer by MegaTransfer.getDeltaSize().
     * <p>
     * The same byte array is also provided in the callback MegaTransferListener.onTransferData for
     * compatibility with other programming languages. Only the MegaTransferListener passed to this function
     * will receive MegaTransferListener.onTransferData() callbacks. MegaTransferListener objects registered
     * with MegaApiJava.addTransferListener() will not receive them for performance reasons.
     *
     * @param node
     *            MegaNode that identifies the file (public nodes are not supported yet).
     * @param startPos
     *            First byte to download from the file.
     * @param size
     *            Size of the data to download.
     * @param listener
     *            MegaTransferListener to track this transfer.
     */
    public void startStreaming(MegaNode node, long startPos, long size, MegaTransferListenerInterface listener) {
        megaApi.startStreaming(node, startPos, size, createDelegateTransferListener(listener));
    }

    /**
     * Cancel a transfer.
     * <p>
     * When a transfer is cancelled, it will finish and will provide the error code
     * MegaError.API_EINCOMPLETE in MegaTransferListener.onTransferFinish() and
     * MegaListener.onTransferFinish().
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CANCEL_TRANSFER
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getTransferTag() - Returns the tag of the cancelled transfer (MegaTransfer.getTag).
     *
     * @param transfer
     *            MegaTransfer object that identifies the transfer.
     *            You can get this object in any MegaTransferListener callback or any MegaListener callback
     *            related to transfers.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void cancelTransfer(MegaTransfer transfer, MegaRequestListenerInterface listener) {
        megaApi.cancelTransfer(transfer, createDelegateRequestListener(listener));
    }

    /**
     * Cancel a transfer.
     *
     * @param transfer
     *            MegaTransfer object that identifies the transfer.
     *            You can get this object in any MegaTransferListener callback or any MegaListener callback
     *            related to transfers.
     */
    public void cancelTransfer(MegaTransfer transfer) {
        megaApi.cancelTransfer(transfer);
    }

    /**
     * Cancel the transfer with a specific tag.
     * <p>
     * When a transfer is cancelled, it will finish and will provide the error code
     * MegaError.API_EINCOMPLETE in MegaTransferListener.onTransferFinish() and
     * MegaListener.onTransferFinish().
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CANCEL_TRANSFER
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getTransferTag() - Returns the tag of the cancelled transfer (MegaTransfer.getTag).
     *
     * @param transferTag
     *            tag that identifies the transfer.
     *            You can get this tag using MegaTransfer.getTag().
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void cancelTransferByTag(int transferTag, MegaRequestListenerInterface listener) {
        megaApi.cancelTransferByTag(transferTag, createDelegateRequestListener(listener));
    }

    /**
     * Cancel the transfer with a specific tag.
     *
     * @param transferTag
     *            tag that identifies the transfer.
     *            You can get this tag using MegaTransfer.getTag().
     */
    public void cancelTransferByTag(int transferTag) {
        megaApi.cancelTransferByTag(transferTag);
    }

    /**
     * Cancel all transfers of the same type.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_CANCEL_TRANSFERS
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getParamType() - Returns the first parameter.
     *
     * @param direction
     *            Type of transfers to cancel.
     *            Valid values are: <br>
     *            - MegaTransfer.TYPE_DOWNLOAD = 0. <br>
     *            - MegaTransfer.TYPE_UPLOAD = 1.
     *
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void cancelTransfers(int direction, MegaRequestListenerInterface listener) {
        megaApi.cancelTransfers(direction, createDelegateRequestListener(listener));
    }

    /**
     * Cancel all transfers of the same type.
     *
     * @param direction
     *            Type of transfers to cancel.
     *            Valid values are: <br>
     *            - MegaTransfer.TYPE_DOWNLOAD = 0. <br>
     *            - MegaTransfer.TYPE_UPLOAD = 1.
     */
    public void cancelTransfers(int direction) {
        megaApi.cancelTransfers(direction);
    }

    /**
     * Pause/resume all transfers.
     * <p>
     * The associated request type with this request is MegaRequest.TYPE_PAUSE_TRANSFERS
     * Valid data in the MegaRequest object received on callbacks: <br>
     * - MegaRequest.getFlag() - Returns the first parameter.
     *
     * @param pause
     *            true to pause all transfers / false to resume all transfers.
     * @param listener
     *            MegaRequestListener to track this request.
     */
    public void pauseTransfers(boolean pause, MegaRequestListenerInterface listener) {
        megaApi.pauseTransfers(pause, createDelegateRequestListener(listener));
    }

    /**
     * Pause/resume all transfers.
     *
     * @param pause
     *            true to pause all transfers / false to resume all transfers.
     */
    public void pauseTransfers(boolean pause) {
        megaApi.pauseTransfers(pause);
    }

    /**
     * Pause/resume all transfers in one direction (uploads or downloads)
     *
     * The associated request type with this request is MegaRequest::TYPE_PAUSE_TRANSFERS
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getFlag - Returns the first parameter
     * - MegaRequest::getNumber - Returns the direction of the transfers to pause/resume
     *
     * @param pause true to pause transfers / false to resume transfers
     * @param direction Direction of transfers to pause/resume
     * Valid values for this parameter are:
     * - MegaTransfer::TYPE_DOWNLOAD = 0
     * - MegaTransfer::TYPE_UPLOAD = 1
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
    public void pauseTransfers(boolean pause, int direction, MegaRequestListenerInterface listener) {
        megaApi.pauseTransfers(pause, direction, createDelegateRequestListener(listener));
    }

    /**
     * Pause/resume all transfers in one direction (uploads or downloads)
     *
     * @param pause true to pause transfers / false to resume transfers
     * @param direction Direction of transfers to pause/resume
     * Valid values for this parameter are:
     * - MegaTransfer::TYPE_DOWNLOAD = 0
     * - MegaTransfer::TYPE_UPLOAD = 1
     */
    public void pauseTransfers(boolean pause, int direction) {
        megaApi.pauseTransfers(pause, direction);
    }

    /**
     * Enable the resumption of transfers
     *
     * This function enables the cache of transfers, so they can be resumed later.
     * Additionally, if a previous cache already exists (from previous executions),
     * then this function also resumes the existing cached transfers.
     *
     * Cached downloads expire after 10 days since the last time they were active.
     * Cached uploads expire after 24 hours since the last time they were active.
     * Cached transfers related to files that have been modified since they were
     * added to the cache are discarded, since the file has changed.
     *
     * A log in or a log out automatically disables this feature.
     *
     * When the MegaApi object is logged in, the cache of transfers is identified
     * and protected using the session and the master key, so transfers won't
     * be resumable using a different session or a different account. The
     * recommended way of using this function to resume transfers for an account
     * is calling it in the callback onRequestFinish related to MegaApi::fetchNodes
     *
     * When the MegaApi object is not logged in, it's still possible to use this
     * feature. However, since there isn't any available data to identify
     * and protect the cache, a default identifier and key are used. To improve
     * the protection of the transfer cache and allow the usage of this feature
     * with several non logged in instances of MegaApi at once without clashes,
     * it's possible to set a custom identifier for the transfer cache in the
     * optional parameter of this function. If that parameter is used, the
     * encryption key for the transfer cache will be derived from it.
     *
     */
    public void enableTransferResumption(){
        megaApi.enableTransferResumption();
    }

    /**
     * Enable the resumption of transfers
     *
     * This function enables the cache of transfers, so they can be resumed later.
     * Additionally, if a previous cache already exists (from previous executions),
     * then this function also resumes the existing cached transfers.
     *
     * Cached downloads expire after 10 days since the last time they were active.
     * Cached uploads expire after 24 hours since the last time they were active.
     * Cached transfers related to files that have been modified since they were
     * added to the cache are discarded, since the file has changed.
     *
     * A log in or a log out automatically disables this feature.
     *
     * When the MegaApi object is logged in, the cache of transfers is identified
     * and protected using the session and the master key, so transfers won't
     * be resumable using a different session or a different account. The
     * recommended way of using this function to resume transfers for an account
     * is calling it in the callback onRequestFinish related to MegaApi::fetchNodes
     *
     * When the MegaApi object is not logged in, it's still possible to use this
     * feature. However, since there isn't any available data to identify
     * and protect the cache, a default identifier and key are used. To improve
     * the protection of the transfer cache and allow the usage of this feature
     * with several non logged in instances of MegaApi at once without clashes,
     * it's possible to set a custom identifier for the transfer cache in the
     * optional parameter of this function. If that parameter is used, the
     * encryption key for the transfer cache will be derived from it.
     *
     * @param loggedOutId Identifier for a non logged in instance of MegaApi.
     * It doesn't have any effect if MegaApi is logged in.
     */
    public void enableTransferResumption(String loggedOutId){
        megaApi.enableTransferResumption(loggedOutId);
    }

    /**
     * Disable the resumption of transfers
     *
     * This function disables the resumption of transfers and also deletes
     * the transfer cache if it exists. See also MegaApi.enableTransferResumption.
     *
     */
    public void disableTransferResumption(){
        megaApi.disableTransferResumption();
    }

    /**
     * Disable the resumption of transfers
     *
     * This function disables the resumption of transfers and also deletes
     * the transfer cache if it exists. See also MegaApi.enableTransferResumption.
     *
     * @param loggedOutId Identifier for a non logged in instance of MegaApi.
     * It doesn't have any effect if MegaApi is logged in.
     */
    public void disableTransferResumption(String loggedOutId){
        megaApi.disableTransferResumption(loggedOutId);
    }

    /**
     * Returns the state (paused/unpaused) of transfers
     * @param direction Direction of transfers to check
     * Valid values for this parameter are:
     * - MegaTransfer::TYPE_DOWNLOAD = 0
     * - MegaTransfer::TYPE_UPLOAD = 1
     *
     * @return true if transfers on that direction are paused, false otherwise
     */
    public boolean areTransfersPaused(int direction) {
        return megaApi.areTransfersPaused(direction);
    }


    /**
     * Set the upload speed limit.
     * <p>
     * The limit will be applied on the server side when starting a transfer. Thus the limit won't be
     * applied for already started uploads and it's applied per storage server.
     *
     * @param bpslimit
     *            -1 to automatically select the limit, 0 for no limit, otherwise the speed limit
     *            in bytes per second.
     */
    public void setUploadLimit(int bpslimit) {
        megaApi.setUploadLimit(bpslimit);
    }

    /**
     * Set the transfer method for downloads
     *
     * Valid methods are:
     * - TRANSFER_METHOD_NORMAL = 0
     * HTTP transfers using port 80. Data is already encrypted.
     *
     * - TRANSFER_METHOD_ALTERNATIVE_PORT = 1
     * HTTP transfers using port 8080. Data is already encrypted.
     *
     * - TRANSFER_METHOD_AUTO = 2
     * The SDK selects the transfer method automatically
     *
     * - TRANSFER_METHOD_AUTO_NORMAL = 3
     * The SDK selects the transfer method automatically starting with port 80.
     *
     *  - TRANSFER_METHOD_AUTO_ALTERNATIVE = 4
     * The SDK selects the transfer method automatically starting with alternative port 8080.
     *
     * @param method Selected transfer method for downloads
     */
    public void setDownloadMethod(int method) {
        megaApi.setDownloadMethod(method);
    }

    /**
     * Set the transfer method for uploads
     *
     * Valid methods are:
     * - TRANSFER_METHOD_NORMAL = 0
     * HTTP transfers using port 80. Data is already encrypted.
     *
     * - TRANSFER_METHOD_ALTERNATIVE_PORT = 1
     * HTTP transfers using port 8080. Data is already encrypted.
     *
     * - TRANSFER_METHOD_AUTO = 2
     * The SDK selects the transfer method automatically
     *
     * - TRANSFER_METHOD_AUTO_NORMAL = 3
     * The SDK selects the transfer method automatically starting with port 80.
     *
     * - TRANSFER_METHOD_AUTO_ALTERNATIVE = 4
     * The SDK selects the transfer method automatically starting with alternative port 8080.
     *
     * @param method Selected transfer method for uploads
     */
    public void setUploadMethod(int method) {
        megaApi.setUploadMethod(method);
    }

    /**
     * Get the active transfer method for downloads
     *
     * Valid values for the return parameter are:
     * - TRANSFER_METHOD_NORMAL = 0
     * HTTP transfers using port 80. Data is already encrypted.
     *
     * - TRANSFER_METHOD_ALTERNATIVE_PORT = 1
     * HTTP transfers using port 8080. Data is already encrypted.
     *
     * - TRANSFER_METHOD_AUTO = 2
     * The SDK selects the transfer method automatically
     *
     * - TRANSFER_METHOD_AUTO_NORMAL = 3
     * The SDK selects the transfer method automatically starting with port 80.
     *
     * - TRANSFER_METHOD_AUTO_ALTERNATIVE = 4
     * The SDK selects the transfer method automatically starting with alternative port 8080.
     *
     * @return Active transfer method for downloads
     */
    public int getDownloadMethod() {
        return megaApi.getDownloadMethod();
    }

    /**
     * Get the active transfer method for uploads
     *
     * Valid values for the return parameter are:
     * - TRANSFER_METHOD_NORMAL = 0
     * HTTP transfers using port 80. Data is already encrypted.
     *
     * - TRANSFER_METHOD_ALTERNATIVE_PORT = 1
     * HTTP transfers using port 8080. Data is already encrypted.
     *
     * - TRANSFER_METHOD_AUTO = 2
     * The SDK selects the transfer method automatically
     *
     * - TRANSFER_METHOD_AUTO_NORMAL = 3
     * The SDK selects the transfer method automatically starting with port 80.
     *
     * - TRANSFER_METHOD_AUTO_ALTERNATIVE = 4
     * The SDK selects the transfer method automatically starting with alternative port 8080.
     *
     * @return Active transfer method for uploads
     */
    public int getUploadMethod() {
        return megaApi.getUploadMethod();
    }

    /**
     * Get all active transfers.
     *
     * @return List with all active transfers.
     */
    public ArrayList<MegaTransfer> getTransfers() {
        return transferListToArray(megaApi.getTransfers());
    }

    /**
     * Get all active transfers based on the type.
     *
     * @param type
     *            MegaTransfer.TYPE_DOWNLOAD || MegaTransfer.TYPE_UPLOAD.
     *
     * @return List with all active download or upload transfers.
     */
    public ArrayList<MegaTransfer> getTransfers(int type) {
        return transferListToArray(megaApi.getTransfers(type));
    }

    /**
     * Get the transfer with a transfer tag.
     * <p>
     * MegaTransfer.getTag() can be used to get the transfer tag.
     *
     * @param transferTag
     *            tag to check.
     * @return MegaTransfer object with that tag, or null if there is not any
     *         active transfer with it.
     *
     */
    public MegaTransfer getTransferByTag(int transferTag) {
        return megaApi.getTransferByTag(transferTag);
    }

    /**
     * Get a list of transfers that belong to a folder transfer
     *
     * This function provides the list of transfers started in the context
     * of a folder transfer.
     *
     * If the tag in the parameter doesn't belong to a folder transfer,
     * this function returns an empty list.
     *
     * The transfers provided by this function are the ones that are added to the
     * transfer queue when this function is called. Finished transfers, or transfers
     * not added to the transfer queue yet (for example, uploads that are waiting for
     * the creation of the parent folder in MEGA) are not returned by this function.
     *
     * @param transferTag Tag of the folder transfer to check
     * @return List of transfers in the context of the selected folder transfer
     * @see MegaTransfer::isFolderTransfer, MegaTransfer::getFolderTransferTag
     */
    public ArrayList<MegaTransfer> getChildTransfers(int transferTag) {
        return transferListToArray(megaApi.getChildTransfers(transferTag));
    }

    /**
     * Force a loop of the SDK thread.
     *
     * @deprecated This function is only here for debugging purposes. It will probably
     *             be removed in future updates.
     */
    @Deprecated public void update() {
        megaApi.update();
    }

    /**
     * Check if the SDK is waiting for the server.
     *
     * @return true if the SDK is waiting for the server to complete a request.
     */
    public boolean isWaiting() {
        return megaApi.isWaiting();
    }

    /**
     * Get the number of pending uploads.
     *
     * @return Pending uploads.
     * @deprecated Function related to statistics will be reviewed in future updates to
     *             provide more data and avoid race conditions. They could change or be removed in the current form.
     */
    @Deprecated public int getNumPendingUploads() {
        return megaApi.getNumPendingUploads();
    }

    /**
     * Get the number of pending downloads.
     *
     * @return Pending downloads
     * @deprecated Function related to statistics will be reviewed in future updates to
     *             provide more data and avoid race conditions. They could change or be removed in the current form.
     */
    @Deprecated public int getNumPendingDownloads() {
        return megaApi.getNumPendingDownloads();
    }

    /**
     * Get the number of queued uploads since the last call to MegaApiJava.resetTotalUploads().
     *
     * @return Number of queued uploads since the last call to MegaApiJava.resetTotalUploads().
     * @deprecated Function related to statistics will be reviewed in future updates to
     *             provide more data and avoid race conditions. They could change or be removed in the current form.
     */
    @Deprecated public int getTotalUploads() {
        return megaApi.getTotalUploads();
    }

    /**
     * Get the number of queued uploads since the last call to MegaApiJava.resetTotalDownloads().
     *
     * @return Number of queued uploads since the last call to MegaApiJava.resetTotalDownloads().
     * @deprecated Function related to statistics will be reviewed in future updates. They
     *             could change or be removed in the current form.
     */
    @Deprecated public int getTotalDownloads() {
        return megaApi.getTotalDownloads();
    }

    /**
     * Reset the number of total downloads.
     * <p>
     * This function resets the number returned by MegaApiJava.getTotalDownloads().
     *
     * @deprecated Function related to statistics will be reviewed in future updates to
     *             provide more data and avoid race conditions. They could change or be removed in the current form.
     *
     */
    @Deprecated public void resetTotalDownloads() {
        megaApi.resetTotalDownloads();
    }

    /**
     * Reset the number of total uploads.
     * <p>
     * This function resets the number returned by MegaApiJava.getTotalUploads().
     *
     * @deprecated Function related to statistics will be reviewed in future updates to
     *             provide more data and avoid race conditions. They could change or be removed in the current form.
     */
    @Deprecated public void resetTotalUploads() {
        megaApi.resetTotalUploads();
    }

    /**
     * Get the total downloaded bytes since the creation of the MegaApi object.
     *
     * @return Total downloaded bytes since the creation of the MegaApi object.
     * @deprecated Function related to statistics will be reviewed in future updates to
     *             provide more data and avoid race conditions. They could change or be removed in the current form.
     */
    @Deprecated public long getTotalDownloadedBytes() {
        return megaApi.getTotalDownloadedBytes();
    }

    /**
     * Get the total uploaded bytes since the creation of the MegaApi object.
     *
     * @return Total uploaded bytes since the creation of the MegaApi object.
     * @deprecated Function related to statistics will be reviewed in future updates to
     *             provide more data and avoid race conditions. They could change or be removed in the current form.
     *
     */
    @Deprecated public long getTotalUploadedBytes() {
        return megaApi.getTotalUploadedBytes();
    }

    /**
     * Update the number of pending downloads/uploads.
     * <p>
     * This function forces a count of the pending downloads/uploads. It could
     * affect the return value of MegaApiJava.getNumPendingDownloads() and
     * MegaApiJava.getNumPendingUploads().
     *
     * @deprecated Function related to statistics will be reviewed in future updates to
     *             provide more data and avoid race conditions. They could change or be removed in the current form.
     *
     */
    @Deprecated public void updateStats() {
        megaApi.updateStats();
    }

    /**
     * Starts an unbuffered download of a node (file) from the user's MEGA account.
     *
     * @param node The MEGA node to download.
     * @param startOffset long. The byte to start from.
     * @param size long. Size of the download.
     * @param outputStream The output stream object to use for this download.
     * @param listener MegaRequestListener to track this request.
     */
    public void startUnbufferedDownload(MegaNode node, long startOffset, long size, OutputStream outputStream, MegaTransferListenerInterface listener) {
        DelegateMegaTransferListener delegateListener = new DelegateOutputMegaTransferListener(this, outputStream, listener, true);
        activeTransferListeners.add(delegateListener);
        megaApi.startStreaming(node, startOffset, size, delegateListener);
    }

    /**
     * Starts an unbuffered download of a node (file) from the user's MEGA account.
     *
     * @param node The MEGA node to download.
     * @param outputStream The output stream object to use for this download.
     * @param listener MegaRequestListener to track this request.
     */
    public void startUnbufferedDownload(MegaNode node, OutputStream outputStream, MegaTransferListenerInterface listener) {
        startUnbufferedDownload(node, 0, node.getSize(), outputStream, listener);
    }

    /****************************************************************************************************/
    // FILESYSTEM METHODS
    /****************************************************************************************************/

    /**
     * Get the number of child nodes.
     * <p>
     * If the node does not exist in MEGA or is not a folder,
     * this function returns 0.
     * <p>
     * This function does not search recursively, only returns the direct child nodes.
     *
     * @param parent
     *            Parent node.
     * @return Number of child nodes.
     */
    public int getNumChildren(MegaNode parent) {
        return megaApi.getNumChildren(parent);
    }

    /**
     * Get the number of child files of a node.
     * <p>
     * If the node does not exist in MEGA or is not a folder,
     * this function returns 0.
     * <p>
     * This function does not search recursively, only returns the direct child files.
     *
     * @param parent
     *            Parent node.
     * @return Number of child files.
     */
    public int getNumChildFiles(MegaNode parent) {
        return megaApi.getNumChildFiles(parent);
    }

    /**
     * Get the number of child folders of a node.
     * <p>
     * If the node does not exist in MEGA or is not a folder,
     * this function returns 0.
     * <p>
     * This function does not search recursively, only returns the direct child folders.
     *
     * @param parent
     *            Parent node.
     * @return Number of child folders.
     */
    public int getNumChildFolders(MegaNode parent) {
        return megaApi.getNumChildFolders(parent);
    }

    /**
     * Get all children of a MegaNode.
     * <p>
     * If the parent node does not exist or it is not a folder, this function
     * returns null.
     *
     * @param parent
     *            Parent node.
     * @param order
     *            Order for the returned list.
     *            Valid values for this parameter are: <br>
     *            - MegaApiJava.ORDER_NONE = 0.
     *            Undefined order. <br>
     *
     *            - MegaApiJava.ORDER_DEFAULT_ASC = 1.
     *            Folders first in alphabetical order, then files in the same order. <br>
     *
     *            - MegaApiJava.ORDER_DEFAULT_DESC = 2.
     *            Files first in reverse alphabetical order, then folders in the same order. <br>
     *
     *            - MegaApiJava.ORDER_SIZE_ASC = 3.
     *            Sort by size, ascending. <br>
     *
     *            - MegaApiJava.ORDER_SIZE_DESC = 4.
     *            Sort by size, descending. <br>
     *
     *            - MegaApiJava.ORDER_CREATION_ASC = 5.
     *            Sort by creation time in MEGA, ascending. <br>
     *
     *            - MegaApiJava.ORDER_CREATION_DESC = 6
     *            Sort by creation time in MEGA, descending <br>
     *
     *            - MegaApiJava.ORDER_MODIFICATION_ASC = 7.
     *            Sort by modification time of the original file, ascending. <br>
     *
     *            - MegaApiJava.ORDER_MODIFICATION_DESC = 8.
     *            Sort by modification time of the original file, descending. <br>
     *
     *            - MegaApiJava.ORDER_ALPHABETICAL_ASC = 9.
     *            Sort in alphabetical order, ascending. <br>
     *
     *            - MegaApiJava.ORDER_ALPHABETICAL_DESC = 10.
     *            Sort in alphabetical order, descending.
     * @return List with all child MegaNode objects.
     */
    public ArrayList<MegaNode> getChildren(MegaNode parent, int order) {
        return nodeListToArray(megaApi.getChildren(parent, order));
    }

    /**
     * Get all children of a MegaNode.
     * <p>
     * If the parent node does not exist or if it is not a folder, this function.
     * returns null.
     *
     * @param parent
     *            Parent node.
     *
     * @return List with all child MegaNode objects.
     */
    public ArrayList<MegaNode> getChildren(MegaNode parent) {
        return nodeListToArray(megaApi.getChildren(parent));
    }

    /**
     * Get the current index of the node in the parent folder for a specific sorting order.
     * <p>
     * If the node does not exist or it does not have a parent node (because it's a root node)
     * this function returns -1.
     *
     * @param node
     *            Node to check.
     * @param order
     *            Sorting order to use.
     * @return Index of the node in its parent folder.
     */
    public int getIndex(MegaNode node, int order) {
        return megaApi.getIndex(node, order);
    }

    /**
     * Get the current index of the node in the parent folder.
     * <p>
     * If the node does not exist or it does not have a parent node (because it's a root node)
     * this function returns -1.
     *
     * @param node
     *            Node to check.
     *
     * @return Index of the node in its parent folder.
     */
    public int getIndex(MegaNode node) {
        return megaApi.getIndex(node);
    }

    /**
     * Get the child node with the provided name.
     * <p>
     * If the node does not exist, this function returns null.
     *
     * @param parent
     *            node.
     * @param name
     *            of the node.
     * @return The MegaNode that has the selected parent and name.
     */
    public MegaNode getChildNode(MegaNode parent, String name) {
        return megaApi.getChildNode(parent, name);
    }

    /**
     * Get the parent node of a MegaNode.
     * <p>
     * If the node does not exist in the account or
     * it is a root node, this function returns null.
     *
     * @param node
     *            MegaNode to get the parent.
     * @return The parent of the provided node.
     */
    public MegaNode getParentNode(MegaNode node) {
        return megaApi.getParentNode(node);
    }

    /**
     * Get the path of a MegaNode.
     * <p>
     * If the node does not exist, this function returns null.
     * You can recover the node later using MegaApi.getNodeByPath()
     * unless the path contains names with '/', '\' or ':' characters.
     *
     * @param node
     *            MegaNode for which the path will be returned.
     * @return The path of the node.
     */
    public String getNodePath(MegaNode node) {
        return megaApi.getNodePath(node);
    }

    /**
     * Get the MegaNode in a specific path in the MEGA account.
     * <p>
     * The path separator character is '/'. <br>
     * The Inbox root node is //in/. <br>
     * The Rubbish root node is //bin/.
     * <p>
     * Paths with names containing '/', '\' or ':' are not compatible
     * with this function.
     *
     * @param path
     *            Path to check.
     * @param baseFolder
     *            Base node if the path is relative.
     * @return The MegaNode object in the path, otherwise null.
     */
    public MegaNode getNodeByPath(String path, MegaNode baseFolder) {
        return megaApi.getNodeByPath(path, baseFolder);
    }

    /**
     * Get the MegaNode in a specific path in the MEGA account.
     * <p>
     * The path separator character is '/'. <br>
     * The Inbox root node is //in/. <br>
     * The Rubbish root node is //bin/.
     * <p>
     * Paths with names containing '/', '\' or ':' are not compatible
     * with this function.
     *
     * @param path
     *            Path to check.
     *
     * @return The MegaNode object in the path, otherwise null.
     */
    public MegaNode getNodeByPath(String path) {
        return megaApi.getNodeByPath(path);
    }

    /**
     * Get the MegaNode that has a specific handle.
     * <p>
     * You can get the handle of a MegaNode using MegaNode.getHandle(). The same handle
     * can be got in a Base64-encoded string using MegaNode.getBase64Handle(). Conversions
     * between these formats can be done using MegaApiJava.base64ToHandle() and MegaApiJava.handleToBase64().
     *
     * @param handle
     *            Node handle to check.
     * @return MegaNode object with the handle, otherwise null.
     */
    public MegaNode getNodeByHandle(long handle) {
        return megaApi.getNodeByHandle(handle);
    }

    /**
     * Get the MegaContactRequest that has a specific handle.
     * <p>
     * You can get the handle of a MegaContactRequest using MegaContactRequestgetHandle().
     * You take the ownership of the returned value.
     *
     * @param handle Contact request handle to check.
     * @return MegaContactRequest object with the handle, otherwise null.
     */
    public MegaContactRequest getContactRequestByHandle(long handle) {
        return megaApi.getContactRequestByHandle(handle);
    }

    /**
     * Get all contacts of this MEGA account.
     *
     * @return List of MegaUser object with all contacts of this account.
     */
    public ArrayList<MegaUser> getContacts() {
        return userListToArray(megaApi.getContacts());
    }

    /**
     * Get the MegaUser that has a specific email address.
     * <p>
     * You can get the email of a MegaUser using MegaUser.getEmail().
     *
     * @param email
     *            Email address to check.
     * @return MegaUser that has the email address, otherwise null.
     */
    public MegaUser getContact(String email) {
        return megaApi.getContact(email);
    }

    /**
     * Get a list with all inbound shares from one MegaUser.
     *
     * @param user
     *            MegaUser sharing folders with this account.
     * @return List of MegaNode objects that this user is sharing with this account.
     */
    public ArrayList<MegaNode> getInShares(MegaUser user) {
        return nodeListToArray(megaApi.getInShares(user));
    }

    /**
     * Get a list with all inbound shares.
     *
     * @return List of MegaNode objects that other users are sharing with this account.
     */
    public ArrayList<MegaNode> getInShares() {
        return nodeListToArray(megaApi.getInShares());
    }

    /**
     * Get a list with all active inboud sharings
     *
     * You take the ownership of the returned value
     *
     * @return List of MegaShare objects that other users are sharing with this account
     */
    public ArrayList<MegaShare> getInSharesList() {
        return shareListToArray(megaApi.getInSharesList());
    }

    /**
     * Check if a MegaNode is being shared.
     * <p>
     * For nodes that are being shared, you can get a a list of MegaShare
     * objects using MegaApiJava.getOutShares().
     *
     * @param node
     *            Node to check.
     * @return true is the MegaNode is being shared, otherwise false.
     */
    public boolean isShared(MegaNode node) {
        return megaApi.isShared(node);
    }

    /**
     * Check if a MegaNode is being shared with other users
     *
     * For nodes that are being shared, you can get a list of MegaShare
     * objects using MegaApi::getOutShares
     *
     * @param node Node to check
     * @return true is the MegaNode is being shared, otherwise false
     * @deprecated This function is intended for debugging and internal purposes and will be probably removed in future updates.
     * Use MegaNode::isOutShare instead
     */
    public boolean isOutShare(MegaNode node) {
        return megaApi.isOutShare(node);
    }

    /**
     * Check if a MegaNode belong to another User, but it is shared with you
     *
     * For nodes that are being shared, you can get a list of MegaNode
     * objects using MegaApi::getInShares
     *
     * @param node Node to check
     * @return true is the MegaNode is being shared, otherwise false
     * @deprecated This function is intended for debugging and internal purposes and will be probably removed in future updates.
     * Use MegaNode::isInShare instead
     */
    public boolean isInShare(MegaNode node) {
        return megaApi.isInShare(node);
    }

    /**
     * Check if a MegaNode is pending to be shared with another User. This situation
     * happens when a node is to be shared with a User which is not a contact yet.
     *
     * For nodes that are pending to be shared, you can get a list of MegaNode
     * objects using MegaApi::getPendingShares
     *
     * @param node Node to check
     * @return true is the MegaNode is pending to be shared, otherwise false
     */
    public boolean isPendingShare(MegaNode node) {
        return megaApi.isPendingShare(node);
    }

    /**
     * Get a list with all active outbound shares.
     *
     * @return List of MegaShare objects.
     */
    public ArrayList<MegaShare> getOutShares() {
        return shareListToArray(megaApi.getOutShares());
    }

    /**
     * Get a list with the active outbound shares for a MegaNode.
     * <p>
     * If the node does not exist in the account, this function returns an empty list.
     *
     * @param node
     *            MegaNode to check.
     * @return List of MegaShare objects.
     */
    public ArrayList<MegaShare> getOutShares(MegaNode node) {
        return shareListToArray(megaApi.getOutShares(node));
    }

    /**
     * Get a list with all pending outbound shares.
     *
     * @return List of MegaShare objects.
     */
    public ArrayList<MegaShare> getPendingOutShares() {
        return shareListToArray(megaApi.getPendingOutShares());
    }

    /**
     * Get a list with all pending outbound shares.
     *
     * @param node MegaNode to check.
     * @return List of MegaShare objects.
     */
    public ArrayList<MegaShare> getPendingOutShares(MegaNode node) {
        return shareListToArray(megaApi.getPendingOutShares(node));
    }

    /**
     * Get a list with all public links
     *
     * You take the ownership of the returned value
     *
     * @return List of MegaNode objects that are shared with everyone via public link
     */
    public ArrayList<MegaNode> getPublicLinks() {
        return nodeListToArray(megaApi.getPublicLinks());
    }

    /**
     * Get a list with all incoming contact requests.
     *
     * @return List of MegaContactRequest objects.
     */
    public ArrayList<MegaContactRequest> getIncomingContactRequests() {
        return contactRequestListToArray(megaApi.getIncomingContactRequests());
    }

    /**
     * Get a list with all outgoing contact requests.
     *
     * @return List of MegaContactRequest objects.
     */
    public ArrayList<MegaContactRequest> getOutgoingContactRequests() {
        return contactRequestListToArray(megaApi.getOutgoingContactRequests());
    }

    /**
     * Get the access level of a MegaNode.
     *
     * @param node
     *            MegaNode to check.
     * @return Access level of the node.
     *         Valid values are: <br>
     *         - MegaShare.ACCESS_OWNER. <br>
     *         - MegaShare.ACCESS_FULL. <br>
     *         - MegaShare.ACCESS_READWRITE. <br>
     *         - MegaShare.ACCESS_READ. <br>
     *         - MegaShare.ACCESS_UNKNOWN.
     */
    public int getAccess(MegaNode node) {
        return megaApi.getAccess(node);
    }

    /**
     * Get the size of a node tree.
     * <p>
     * If the MegaNode is a file, this function returns the size of the file.
     * If it's a folder, this function returns the sum of the sizes of all nodes
     * in the node tree.
     *
     * @param node
     *            Parent node.
     * @return Size of the node tree.
     */
    public long getSize(MegaNode node) {
        return megaApi.getSize(node);
    }

    /**
     * Get a Base64-encoded fingerprint for a local file.
     * <p>
     * The fingerprint is created taking into account the modification time of the file
     * and file contents. This fingerprint can be used to get a corresponding node in MEGA
     * using MegaApiJava.getNodeByFingerprint().
     * <p>
     * If the file can't be found or can't be opened, this function returns null.
     *
     * @param filePath
     *            Local file path.
     * @return Base64-encoded fingerprint for the file.
     */
    public String getFingerprint(String filePath) {
        return megaApi.getFingerprint(filePath);
    }

    /**
     * Get a Base64-encoded fingerprint for a node.
     * <p>
     * If the node does not exist or does not have a fingerprint, this function returns null.
     *
     * @param node
     *            Node for which we want to get the fingerprint.
     * @return Base64-encoded fingerprint for the file.
     */
    public String getFingerprint(MegaNode node) {
        return megaApi.getFingerprint(node);
    }

    /**
     * Returns a node with the provided fingerprint.
     * <p>
     * If there is not any node in the account with that fingerprint, this function returns null.
     *
     * @param fingerprint
     *            Fingerprint to check.
     * @return MegaNode object with the provided fingerprint.
     */
    public MegaNode getNodeByFingerprint(String fingerprint) {
        return megaApi.getNodeByFingerprint(fingerprint);
    }

    /**
     * Returns a node with the provided fingerprint in a preferred parent folder.
     * <p>
     * If there is not any node in the account with that fingerprint, this function returns null.
     *
     * @param fingerprint
     *            Fingerprint to check.
     * @param preferredParent
     *            Preferred parent if several matches are found.
     * @return MegaNode object with the provided fingerprint.
     */
    public MegaNode getNodeByFingerprint(String fingerprint, MegaNode preferredParent) {
        return megaApi.getNodeByFingerprint(fingerprint, preferredParent);
    }

    /**
     * Returns all nodes that have a fingerprint
     *
     * If there isn't any node in the account with that fingerprint, this function returns an empty MegaNodeList.
     *
     * @param fingerprint Fingerprint to check
     * @return List of nodes with the same fingerprint
     */
    public ArrayList<MegaNode> getNodesByFingerprint(String fingerprint) {
        return nodeListToArray(megaApi.getNodesByFingerprint(fingerprint));
    }

    /**
     * Returns a node with the provided fingerprint that can be exported
     *
     * If there isn't any node in the account with that fingerprint, this function returns null.
     * If a file name is passed in the second parameter, it's also checked if nodes with a matching
     * fingerprint has that name. If there isn't any matching node, this function returns null.
     * This function ignores nodes that are inside the Rubbish Bin because public links to those nodes
     * can't be downloaded.
     *
     * @param fingerprint Fingerprint to check
     * @param name Name that the node should have
     * @return Exportable node that meet the requirements
     */
    public MegaNode getExportableNodeByFingerprint(String fingerprint, String name) {
        return megaApi.getExportableNodeByFingerprint(fingerprint, name);
    }

    /**
     * Returns a node with the provided fingerprint that can be exported
     *
     * If there isn't any node in the account with that fingerprint, this function returns null.
     * This function ignores nodes that are inside the Rubbish Bin because public links to those nodes
     * can't be downloaded.
     *
     * @param fingerprint Fingerprint to check
     * @return Exportable node that meet the requirements
     */
    public MegaNode getExportableNodeByFingerprint(String fingerprint) {
        return megaApi.getExportableNodeByFingerprint(fingerprint);
    }


    /**
     * Check if the account already has a node with the provided fingerprint.
     * <p>
     * A fingerprint for a local file can be generated using MegaApiJava.getFingerprint().
     *
     * @param fingerprint
     *            Fingerprint to check.
     * @return true if the account contains a node with the same fingerprint.
     */
    public boolean hasFingerprint(String fingerprint) {
        return megaApi.hasFingerprint(fingerprint);
    }

    /**
     * getCRC Get the CRC of a file
     *
     * The CRC of a file is a hash of its contents.
     * If you need a more realiable method to check files, use fingerprint functions
     * (MegaApi::getFingerprint, MegaApi::getNodeByFingerprint) that also takes into
     * account the size and the modification time of the file to create the fingerprint.
     *
     * @param filePath Local file path
     * @return Base64-encoded CRC of the file
     */
    public String getCRC(String filePath) {
        return megaApi.getCRC(filePath);
    }

    /**
     * Get the CRC from a fingerprint
     *
     * @param fingerprint fingerprint from which we want to get the CRC
     * @return Base64-encoded CRC from the fingerprint
     */
    public String getCRCFromFingerprint(String fingerprint) {
        return megaApi.getCRCFromFingerprint(fingerprint);
    }

    /**
     * getCRC Get the CRC of a node
     *
     * The CRC of a node is a hash of its contents.
     * If you need a more realiable method to check files, use fingerprint functions
     * (MegaApi::getFingerprint, MegaApi::getNodeByFingerprint) that also takes into
     * account the size and the modification time of the node to create the fingerprint.
     *
     * @param node Node for which we want to get the CRC
     * @return Base64-encoded CRC of the node
     */
    public String getCRC(MegaNode node) {
        return megaApi.getCRC(node);
    }

    /**
     * getNodeByCRC Returns a node with the provided CRC
     *
     * If there isn't any node in the selected folder with that CRC, this function returns NULL.
     * If there are several nodes with the same CRC, anyone can be returned.
     *
     * @param crc CRC to check
     * @param parent Parent node to scan. It must be a folder.
     * @return  Node with the selected CRC in the selected folder, or NULL
     * if it's not found.
     */
    public MegaNode getNodeByCRC(String crc, MegaNode parent) {
        return megaApi.getNodeByCRC(crc, parent);
    }

    /**
     * Check if a node has an access level.
     *
     * @param node
     *            Node to check.
     * @param level
     *            Access level to check.
     *            Valid values for this parameter are: <br>
     *            - MegaShare.ACCESS_OWNER. <br>
     *            - MegaShare.ACCESS_FULL. <br>
     *            - MegaShare.ACCESS_READWRITE. <br>
     *            - MegaShare.ACCESS_READ.
     * @return MegaError object with the result.
     *         Valid values for the error code are: <br>
     *         - MegaError.API_OK - The node has the required access level. <br>
     *         - MegaError.API_EACCESS - The node does not have the required access level. <br>
     *         - MegaError.API_ENOENT - The node does not exist in the account. <br>
     *         - MegaError.API_EARGS - Invalid parameters.
     */
    public MegaError checkAccess(MegaNode node, int level) {
        return megaApi.checkAccess(node, level);
    }

    /**
     * Check if a node can be moved to a target node.
     *
     * @param node
     *            Node to check.
     * @param target
     *            Target for the move operation.
     * @return MegaError object with the result.
     *         Valid values for the error code are: <br>
     *         - MegaError.API_OK - The node can be moved to the target. <br>
     *         - MegaError.API_EACCESS - The node can't be moved because of permissions problems. <br>
     *         - MegaError.API_ECIRCULAR - The node can't be moved because that would create a circular linkage. <br>
     *         - MegaError.API_ENOENT - The node or the target does not exist in the account. <br>
     *         - MegaError.API_EARGS - Invalid parameters.
     */
    public MegaError checkMove(MegaNode node, MegaNode target) {
        return megaApi.checkMove(node, target);
    }

    /**
     * Check if the MEGA filesystem is available in the local computer
     *
     * This function returns true after a successful call to MegaApi::fetchNodes,
     * otherwise it returns false
     *
     * @return True if the MEGA filesystem is available
     */
    public boolean isFilesystemAvailable() {
        return megaApi.isFilesystemAvailable();
    }

    /**
     * Returns the root node of the account.
     * <p>
     * If you haven't successfully called MegaApiJava.fetchNodes() before,
     * this function returns null.
     *
     * @return Root node of the account.
     */
    public MegaNode getRootNode() {
        return megaApi.getRootNode();
    }

    /**
     * Returns the inbox node of the account.
     * <p>
     * If you haven't successfully called MegaApiJava.fetchNodes() before,
     * this function returns null.
     *
     * @return Inbox node of the account.
     */
    public MegaNode getInboxNode() {
        return megaApi.getInboxNode();
    }

    /**
     * Returns the rubbish node of the account.
     * <p>
     * If you haven't successfully called MegaApiJava.fetchNodes() before,
     * this function returns null.
     *
     * @return Rubbish node of the account.
     */
    public MegaNode getRubbishNode() {
        return megaApi.getRubbishNode();
    }

    /**
     * Get the time (in seconds) during which transfers will be stopped due to a bandwidth overquota
     * @return Time (in seconds) during which transfers will be stopped, otherwise 0
     */
    public long getBandwidthOverquotaDelay() {
        return megaApi.getBandwidthOverquotaDelay();
    }

    /**
     * Search nodes containing a search string in their name.
     * <p>
     * The search is case-insensitive.
     *
     * @param parent
     *            The parent node of the tree to explore.
     * @param searchString
     *            Search string. The search is case-insensitive.
     * @param recursive
     *            true if you want to search recursively in the node tree.
     *            false if you want to search in the children of the node only.
     *
     * @return List of nodes that contain the desired string in their name.
     */
    public ArrayList<MegaNode> search(MegaNode parent, String searchString, boolean recursive) {
        return nodeListToArray(megaApi.search(parent, searchString, recursive));
    }

    /**
     * Search nodes containing a search string in their name.
     * <p>
     * The search is case-insensitive.
     *
     * @param parent
     *            The parent node of the tree to explore.
     * @param searchString
     *            Search string. The search is case-insensitive.
     *
     * @return List of nodes that contain the desired string in their name.
     */
    public ArrayList<MegaNode> search(MegaNode parent, String searchString) {
        return nodeListToArray(megaApi.search(parent, searchString));
    }

    /**
     * Search nodes containing a search string in their name
     *
     * The search is case-insensitive.
     *
     * The search will consider every accessible node for the account:
     *  - Cloud drive
     *  - Inbox
     *  - Rubbish bin
     *  - Incoming shares from other users
     *
     * You take the ownership of the returned value.
     *
     * @param searchString Search string. The search is case-insensitive
     *
     * @return List of nodes that contain the desired string in their name
     */
    public ArrayList<MegaNode> search(String searchString) {
        return nodeListToArray(megaApi.search(searchString));
    }

    /**
     * Process a node tree using a MegaTreeProcessor implementation.
     *
     * @param parent
     *            The parent node of the tree to explore.
     * @param processor
     *            MegaTreeProcessor that will receive callbacks for every node in the tree.
     * @param recursive
     *            true if you want to recursively process the whole node tree.
     *            false if you want to process the children of the node only.
     *
     * @return true if all nodes were processed. false otherwise (the operation can be
     *         cancelled by MegaTreeProcessor.processMegaNode()).
     */
    public boolean processMegaTree(MegaNode parent, MegaTreeProcessorInterface processor, boolean recursive) {
        DelegateMegaTreeProcessor delegateListener = new DelegateMegaTreeProcessor(this, processor);
        activeMegaTreeProcessors.add(delegateListener);
        boolean result = megaApi.processMegaTree(parent, delegateListener, recursive);
        activeMegaTreeProcessors.remove(delegateListener);
        return result;
    }

    /**
     * Process a node tree using a MegaTreeProcessor implementation.
     *
     * @param parent
     *            The parent node of the tree to explore.
     * @param processor
     *            MegaTreeProcessor that will receive callbacks for every node in the tree.
     *
     * @return true if all nodes were processed. false otherwise (the operation can be
     *         cancelled by MegaTreeProcessor.processMegaNode()).
     */
    public boolean processMegaTree(MegaNode parent, MegaTreeProcessorInterface processor) {
        DelegateMegaTreeProcessor delegateListener = new DelegateMegaTreeProcessor(this, processor);
        activeMegaTreeProcessors.add(delegateListener);
        boolean result = megaApi.processMegaTree(parent, delegateListener);
        activeMegaTreeProcessors.remove(delegateListener);
        return result;
    }

    /**
     * Returns a MegaNode that can be downloaded with any instance of MegaApi
     *
     * This function only allows to authorize file nodes.
     *
     * You can use MegaApi::startDownload with the resulting node with any instance
     * of MegaApi, even if it's logged into another account, a public folder, or not
     * logged in.
     *
     * If the first parameter is a public node or an already authorized node, this
     * function returns a copy of the node, because it can be already downloaded
     * with any MegaApi instance.
     *
     * If the node in the first parameter belongs to the account or public folder
     * in which the current MegaApi object is logged in, this funtion returns an
     * authorized node.
     *
     * If the first parameter is NULL or a node that is not a public node, is not
     * already authorized and doesn't belong to the current MegaApi, this function
     * returns NULL.
     *
     * You take the ownership of the returned value.
     *
     * @param node MegaNode to authorize
     * @return Authorized node, or NULL if the node can't be authorized or is not a file
     */
    public MegaNode authorizeNode(MegaNode node){
        return megaApi.authorizeNode(node);
    }

    /**
     * Get the SDK version.
     *
     * @return SDK version.
     */
    public String getVersion() {
        return megaApi.getVersion();
    }

    /**
     * Get the User-Agent header used by the SDK.
     *
     * @return User-Agent used by the SDK.
     */
    public String getUserAgent() {
        return megaApi.getUserAgent();
    }

    /**
     * Changes the API URL.
     *
     * @param apiURL The API URL to change.
     * @param disablepkp boolean. Disable public key pinning if true. Do not disable public key pinning if false.
     */
    public void changeApiUrl(String apiURL, boolean disablepkp) {
        megaApi.changeApiUrl(apiURL, disablepkp);
    }

    /**
     * Changes the API URL.
     * <p>
     * Please note, this method does not disable public key pinning.
     *
     * @param apiURL The API URL to change.
     */
    public void changeApiUrl(String apiURL) {
        megaApi.changeApiUrl(apiURL);
    }

    /**
     * Keep retrying when public key pinning fails
     *
     * By default, when the check of the MEGA public key fails, it causes an automatic
     * logout. Pass false to this function to disable that automatic logout and
     * keep the SDK retrying the request.
     *
     * Even if the automatic logout is disabled, a request of the type MegaRequest::TYPE_LOGOUT
     * will be automatically created and callbacks (onRequestStart, onRequestFinish) will
     * be sent. However, logout won't be really executed and in onRequestFinish the error code
     * for the request will be MegaError::API_EINCOMPLETE
     *
     * @param enable true to keep retrying failed requests due to a fail checking the MEGA public key
     * or false to perform an automatic logout in that case
     */
    public void retrySSLerrors(boolean enable) {
        megaApi.retrySSLerrors(enable);
    }

    /**
     * Enable / disable the public key pinning
     *
     * Public key pinning is enabled by default for all sensible communications.
     * It is strongly discouraged to disable this feature.
     *
     * @param enable true to keep public key pinning enabled, false to disable it
     */
    public void setPublicKeyPinning(boolean enable) {
        megaApi.setPublicKeyPinning(enable);
    }

    /**
     * Make a name suitable for a file name in the local filesystem.
     * <p>
     * This function escapes (%xx) forbidden characters in the local filesystem if needed.
     * You can revert this operation using MegaApiJava.unescapeFsIncompatible().
     *
     * @param name
     *            Name to convert.
     * @return Converted name.
     */
    public String escapeFsIncompatible(String name) {
        return megaApi.escapeFsIncompatible(name);
    }

    /**
     * Unescape a file name escaped with MegaApiJava.escapeFsIncompatible().
     *
     * @param localName
     *            Escaped name to convert.
     * @return Converted name.
     */
    public String unescapeFsIncompatible(String localName) {
        return megaApi.unescapeFsIncompatible(localName);
    }

    /**
     * Create a thumbnail for an image.
     *
     * @param imagePath Image path.
     * @param dstPath Destination path for the thumbnail (including the file name).
     * @return true if the thumbnail was successfully created, otherwise false.
     */
    public boolean createThumbnail(String imagePath, String dstPath) {
        return megaApi.createThumbnail(imagePath, dstPath);
    }

    /**
     * Create a preview for an image.
     *
     * @param imagePath Image path.
     * @param dstPath Destination path for the preview (including the file name).
     * @return true if the preview was successfully created, otherwise false.
     */
    public boolean createPreview(String imagePath, String dstPath) {
        return megaApi.createPreview(imagePath, dstPath);
    }

    /**
     * Convert a Base64 string to Base32.
     * <p>
     * If the input pointer is null, this function will return null.
     * If the input character array is not a valid base64 string
     * the effect is undefined.
     *
     * @param base64
     *            null-terminated Base64 character array.
     * @return null-terminated Base32 character array.
     */
    public static String base64ToBase32(String base64) {
        return MegaApi.base64ToBase32(base64);
    }

    /**
     * Convert a Base32 string to Base64.
     *
     * If the input pointer is null, this function will return null.
     * If the input character array is not a valid base32 string
     * the effect is undefined.
     *
     * @param base32
     *            null-terminated Base32 character array.
     * @return null-terminated Base64 character array.
     */
    public static String base32ToBase64(String base32) {
        return MegaApi.base32ToBase64(base32);
    }

    /**
     * Recursively remove all local files/folders inside a local path
     * @param path Local path of a folder to start the recursive deletion
     * The folder itself is not deleted
     */
    public static void removeRecursively(String localPath) {
        MegaApi.removeRecursively(localPath);
    }

    /**
     * Check if the connection with MEGA servers is OK
     *
     * It can briefly return false even if the connection is good enough when
     * some storage servers are temporarily not available or the load of API
     * servers is high.
     *
     * @return true if the connection is perfectly OK, otherwise false
     */
    public boolean isOnline() {
        return megaApi.isOnline();
    }

    /**
     * Register a token for push notifications
     *
     * This function attach a token to the current session, which is intended to get push notifications
     * on mobile platforms like Android and iOS.
     *
     * The associated request type with this request is MegaRequest::TYPE_REGISTER_PUSH_NOTIFICATION
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getText - Returns the token provided.
     * - MegaRequest::getNumber - Returns the device type provided.
     *
     * @param deviceType Integer id for the provider. 1 for Android, 2 for iOS
     * @param token Character array representing the token to be registered.
     * @param listener MegaRequestListenerInterface to track this request
     */
    public void registerPushNotifications(int deviceType, String token, MegaRequestListenerInterface listener) {
        //megaApi.registerPushNotifications(deviceType, token, createDelegateRequestListener(listener));
    }

    /**
     * Register a token for push notifications
     *
     * This function attach a token to the current session, which is intended to get push notifications
     * on mobile platforms like Android and iOS.
     *
     * @param deviceType Integer id for the provider. 1 for Android, 2 for iOS
     * @param token Character array representing the token to be registered.
     */
    public void registerPushNotifications(int deviceType, String token) {
        //megaApi.registerPushNotifications(deviceType, token);
    }


    /****************************************************************************************************/
    // INTERNAL METHODS
    /****************************************************************************************************/
    private MegaRequestListener createDelegateRequestListener(MegaRequestListenerInterface listener) {
        DelegateMegaRequestListener delegateListener = new DelegateMegaRequestListener(this, listener, true);
        activeRequestListeners.add(delegateListener);
        return delegateListener;
    }

    private MegaRequestListener createDelegateRequestListener(MegaRequestListenerInterface listener, boolean singleListener) {
        DelegateMegaRequestListener delegateListener = new DelegateMegaRequestListener(this, listener, singleListener);
        activeRequestListeners.add(delegateListener);
        return delegateListener;
    }

    private MegaTransferListener createDelegateTransferListener(MegaTransferListenerInterface listener) {
        DelegateMegaTransferListener delegateListener = new DelegateMegaTransferListener(this, listener, true);
        activeTransferListeners.add(delegateListener);
        return delegateListener;
    }

    private MegaTransferListener createDelegateTransferListener(MegaTransferListenerInterface listener, boolean singleListener) {
        DelegateMegaTransferListener delegateListener = new DelegateMegaTransferListener(this, listener, singleListener);
        activeTransferListeners.add(delegateListener);
        return delegateListener;
    }

    private MegaGlobalListener createDelegateGlobalListener(MegaGlobalListenerInterface listener) {
        DelegateMegaGlobalListener delegateListener = new DelegateMegaGlobalListener(this, listener);
        activeGlobalListeners.add(delegateListener);
        return delegateListener;
    }

    private MegaListener createDelegateMegaListener(MegaListenerInterface listener) {
        DelegateMegaListener delegateListener = new DelegateMegaListener(this, listener);
        activeMegaListeners.add(delegateListener);
        return delegateListener;
    }

    void privateFreeRequestListener(DelegateMegaRequestListener listener) {
        activeRequestListeners.remove(listener);
    }

    void privateFreeTransferListener(DelegateMegaTransferListener listener) {
        activeTransferListeners.remove(listener);
    }

    static ArrayList<MegaNode> nodeListToArray(MegaNodeList nodeList) {
        if (nodeList == null) {
            return null;
        }

        ArrayList<MegaNode> result = new ArrayList<MegaNode>(nodeList.size());
        for (int i = 0; i < nodeList.size(); i++) {
            result.add(nodeList.get(i).copy());
        }

        return result;
    }

    static ArrayList<MegaShare> shareListToArray(MegaShareList shareList) {
        if (shareList == null) {
            return null;
        }

        ArrayList<MegaShare> result = new ArrayList<MegaShare>(shareList.size());
        for (int i = 0; i < shareList.size(); i++) {
            result.add(shareList.get(i).copy());
        }

        return result;
    }

    static ArrayList<MegaContactRequest> contactRequestListToArray(MegaContactRequestList contactRequestList) {
        if (contactRequestList == null) {
            return null;
        }

        ArrayList<MegaContactRequest> result = new ArrayList<MegaContactRequest>(contactRequestList.size());
        for(int i=0; i<contactRequestList.size(); i++) {
            result.add(contactRequestList.get(i).copy());
        }

        return result;
    }

    static ArrayList<MegaTransfer> transferListToArray(MegaTransferList transferList) {
        if (transferList == null) {
            return null;
        }

        ArrayList<MegaTransfer> result = new ArrayList<MegaTransfer>(transferList.size());
        for (int i = 0; i < transferList.size(); i++) {
            result.add(transferList.get(i).copy());
        }

        return result;
    }

    static ArrayList<MegaUser> userListToArray(MegaUserList userList) {

        if (userList == null) {
            return null;
        }

        ArrayList<MegaUser> result = new ArrayList<MegaUser>(userList.size());
        for (int i = 0; i < userList.size(); i++) {
            result.add(userList.get(i).copy());
        }

        return result;
    }
}