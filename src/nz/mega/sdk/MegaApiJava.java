package nz.mega.sdk;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Allows to control a MEGA account or a shared folder
 *
 * You must provide an appKey to use this SDK. You can generate an appKey for your app for free here:
 * - https://mega.co.nz/#sdk
 *
 * You can enable local node caching by passing a local path in the constructor of this class. That saves many data usage
 * and many time starting your app because the entire filesystem won't have to be downloaded each time. The persistent
 * node cache will only be loaded by logging in with a session key. To take advantage of this feature, apart of passing the
 * local path to the constructor, your application have to save the session key after login (MegaApi::dumpSession) and use
 * it to log in the next time. This is highly recommended also to enhance the security, because in this was the access password
 * doesn't have to be stored by the application.
 *
 * To access MEGA using this SDK, you have to create an object of this class and use one of the MegaApi::login options (to log in
 * to a MEGA account or a public folder). If the login request succeed, call MegaApi::fetchNodes to get the filesystem in MEGA.
 * After that, you can use all other requests, manage the files and start transfers.
 *
 * After using MegaApi::logout you can reuse the same MegaApi object to log in to another MEGA account or a public folder.
 *
 */
public class MegaApiJava
{
	MegaApi megaApi;
	MegaGfxProcessor gfxProcessor;
	static DelegateMegaLogger logger;
	
	void runCallback(Runnable runnable)
	{
		runnable.run();
	}
	
	static Set<DelegateMegaRequestListener> activeRequestListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaRequestListener>());
	static Set<DelegateMegaTransferListener> activeTransferListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaTransferListener>());
	static Set<DelegateMegaGlobalListener> activeGlobalListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaGlobalListener>());
	static Set<DelegateMegaListener> activeMegaListeners = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaListener>());
	static Set<DelegateMegaTreeProcessor> activeMegaTreeProcessors = Collections.synchronizedSet(new LinkedHashSet<DelegateMegaTreeProcessor>());
	
	//Order options for getChildren
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
	
	public final static int LOG_LEVEL_FATAL = 0;	// Very severe error event that will presumably lead the application to abort.
	public final static int LOG_LEVEL_ERROR = LOG_LEVEL_FATAL + 1;	// Error information but will continue application to keep running.
	public final static int LOG_LEVEL_WARNING = LOG_LEVEL_ERROR + 1;	// Information representing errors in application but application will keep running
	public final static int LOG_LEVEL_INFO = LOG_LEVEL_WARNING + 1;	// Mainly useful to represent current progress of application.
	public final static int LOG_LEVEL_DEBUG = LOG_LEVEL_INFO + 1;	// Informational logs, that are useful for developers. Only applicable if DEBUG is defined.
	public final static int LOG_LEVEL_MAX = LOG_LEVEL_DEBUG + 1;
	
	public final static int EVENT_FEEDBACK = 0;
	public final static int EVENT_DEBUG = EVENT_FEEDBACK + 1;
	public final static int EVENT_INVALID = EVENT_DEBUG + 1;
	
	public final static int PAYMENT_METHOD_BALANCE = MegaApi.PAYMENT_METHOD_BALANCE;
	public final static int PAYMENT_METHOD_CREDIT_CARD = MegaApi.PAYMENT_METHOD_CREDIT_CARD;
	public final static int PAYMENT_METHOD_PAYPAL = MegaApi.PAYMENT_METHOD_PAYPAL;
	public final static int PAYMENT_METHOD_ITUNES = MegaApi.PAYMENT_METHOD_ITUNES;
	public final static int PAYMENT_METHOD_BITCOIN = MegaApi.PAYMENT_METHOD_BITCOIN;
	public final static int PAYMENT_METHOD_UNIONPAY = MegaApi.PAYMENT_METHOD_UNIONPAY;
	public final static int PAYMENT_METHOD_FORTUMO = MegaApi.PAYMENT_METHOD_FORTUMO;
	public final static int PAYMENT_METHOD_GOOGLE_WALLET = MegaApi.PAYMENT_METHOD_GOOGLE_WALLET;
	
	/**
     * Constructor suitable for most applications
     * 
     * @param appKey AppKey of your application
     * You can generate your AppKey for free here:
     * - https://mega.co.nz/#sdk
     *
     * @param basePath Base path to store the local cache
     * If you pass NULL to this parameter, the SDK won't use any local cache.
     *
     */
	public MegaApiJava(String appKey, String basePath)
	{
		megaApi = new MegaApi(appKey, basePath);
	}

	/**
     * MegaApi Constructor that allows to use a custom GFX processor
     * The SDK attach thumbnails and previews to all uploaded images. To generate them, it needs a graphics processor.
     * You can build the SDK with one of the provided built-in graphics processors. If none of them is available
     * in your app, you can implement the MegaGfxProcessor interface to provide your custom processor. Please
     * read the documentation of MegaGfxProcessor carefully to ensure that your implementation is valid.
     * 
     * @param appKey AppKey of your application
     * You can generate your AppKey for free here:
     * - https://mega.co.nz/#sdk
     *
     * @param userAgent User agent to use in network requests
     * If you pass NULL to this parameter, a default user agent will be used
     * 
     * @param basePath Base path to store the local cache
     * If you pass NULL to this parameter, the SDK won't use any local cache.
     *
     * @param gfxProcessor Image processor. The SDK will use it to generate previews and thumbnails
     * If you pass NULL to this parameter, the SDK will try to use the built-in image processors.
     * 
     */
	public MegaApiJava(String appKey, String userAgent, String basePath, MegaGfxProcessor gfxProcessor)
	{
		this.gfxProcessor = gfxProcessor;
		megaApi = new MegaApi(appKey, gfxProcessor, basePath, userAgent);
	}
	
	/**
     * Constructor suitable for most applications
     * 
     * @param appKey AppKey of your application
     * You can generate your AppKey for free here:
     * - https://mega.co.nz/#sdk
     *
     */
	public MegaApiJava(String appKey)
	{
		megaApi = new MegaApi(appKey);
	}
	
	/****************************************************************************************************/
	//LISTENER MANAGEMENT
	/****************************************************************************************************/
	
	/**
     * Register a listener to receive all events (requests, transfers, global, synchronization)
     *
     * You can use MegaApi::removeListener to stop receiving events.
     *
     * @param listener Listener that will receive all events (requests, transfers, global, synchronization)
     */
	public void addListener(MegaListenerInterface listener)
	{
		megaApi.addListener(createDelegateMegaListener(listener));
	}

	/**
     * Register a listener to receive all events about requests
     *
     * You can use MegaApi::removeRequestListener to stop receiving events.
     *
     * @param listener Listener that will receive all events about requests
     */
	public void addRequestListener(MegaRequestListenerInterface listener)
	{
		megaApi.addRequestListener(createDelegateRequestListener(listener, false));
	}
	
	/**
     * Register a listener to receive all events about transfers
     *
     * You can use MegaApi::removeTransferListener to stop receiving events.
     *
     * @param listener Listener that will receive all events about transfers
     */
	public void addTransferListener(MegaTransferListenerInterface listener)
	{
		megaApi.addTransferListener(createDelegateTransferListener(listener, false));
	}

	/**
     * Register a listener to receive global events
     *
     * You can use MegaApi::removeGlobalListener to stop receiving events.
     *
     * @param listener Listener that will receive global events
     */
	public void addGlobalListener(MegaGlobalListenerInterface listener)
	{
		megaApi.addGlobalListener(createDelegateGlobalListener(listener));
	}

	/**
     * Unregister a listener
     *
     * This listener won't receive more events.
     *
     * @param listener Object that is unregistered
     */
	public void removeListener(MegaListenerInterface listener)
	{
		synchronized(activeMegaListeners)
		{
			Iterator<DelegateMegaListener> it = activeMegaListeners.iterator();
			while(it.hasNext())
			{
				DelegateMegaListener delegate = it.next();
				if(delegate.getUserListener()==listener)
				{
					megaApi.removeListener(delegate);
					it.remove();
				}
			}
		}		
	}

	/**
     * Unregister a MegaRequestListener
     *
     * This listener won't receive more events.
     *
     * @param listener Object that is unregistered
     */
	public void removeRequestListener(MegaRequestListenerInterface listener)
	{
		synchronized(activeRequestListeners)
		{
			Iterator<DelegateMegaRequestListener> it = activeRequestListeners.iterator();
			while(it.hasNext())
			{
				DelegateMegaRequestListener delegate = it.next();
				if(delegate.getUserListener()==listener)
				{	
					megaApi.removeRequestListener(delegate);
					it.remove();
				}
			}
		}
	}

	/**
     * Unregister a MegaTransferListener
     *
     * This listener won't receive more events.
     *
     * @param listener Object that is unregistered
     */
	public void removeTransferListener(MegaTransferListenerInterface listener)
	{
		synchronized(activeTransferListeners)
		{
			Iterator<DelegateMegaTransferListener> it = activeTransferListeners.iterator();
			while(it.hasNext())
			{
				DelegateMegaTransferListener delegate = it.next();
				if(delegate.getUserListener()==listener)
				{
					megaApi.removeTransferListener(delegate);
					it.remove();
				}
			}
		}
	}

	/**
     * Unregister a MegaGlobalListener
     *
     * This listener won't receive more events.
     *
     * @param listener Object that is unregistered
     */
	public void removeGlobalListener(MegaGlobalListenerInterface listener)
	{
		synchronized(activeGlobalListeners)
		{
			Iterator<DelegateMegaGlobalListener> it = activeGlobalListeners.iterator();
			while(it.hasNext())
			{
				DelegateMegaGlobalListener delegate = it.next();
				if(delegate.getUserListener()==listener)
				{
					megaApi.removeGlobalListener(delegate);
					it.remove();
				}
			}
		}		
	}
	
	/****************************************************************************************************/
	//UTILS
	/****************************************************************************************************/
	
	/**
     * Generates a private key based on the access password
     *
     * This is a time consuming operation (specially for low-end mobile devices). Since the resulting key is
     * required to log in, this function allows to do this step in a separate function. You should run this function
     * in a background thread, to prevent UI hangs. The resulting key can be used in MegaApi::fastLogin
     *
     * @param password Access password
     * @return Base64-encoded private key
     */
	public String getBase64PwKey(String password)
	{
		return megaApi.getBase64PwKey(password);
	}

	/**
     * Generates a hash based in the provided private key and email
     *
     * This is a time consuming operation (specially for low-end mobile devices). Since the resulting key is
     * required to log in, this function allows to do this step in a separate function. You should run this function
     * in a background thread, to prevent UI hangs. The resulting key can be used in MegaApi::fastLogin
     *
     * @param base64pwkey Private key returned by MegaApi::getBase64PwKey
     * @return Base64-encoded hash
     */
	public String getStringHash(String base64pwkey, String inBuf)
	{
		return megaApi.getStringHash(base64pwkey, inBuf);
	}
	
	 /**
     * Converts a Base32-encoded user handle (JID) to a MegaHandle
     *
     * @param base32Handle Base32-encoded handle (JID)
     * @return User handle
     */
	public static long base32ToHandle(String base32Handle) {
		return MegaApi.base32ToHandle(base32Handle);
	}

	/**
     * Converts a Base64-encoded node handle to a MegaHandle
     *
     * The returned value can be used to recover a MegaNode using MegaApi::getNodeByHandle
     * You can revert this operation using MegaApi::handleToBase64
     *
     * @param base64Handle Base64-encoded node handle
     * @return Node handle
     */
	public static long base64ToHandle(String base64Handle)
	{
		return MegaApi.base64ToHandle(base64Handle);
	}
	
	/**
     * Converts a MegaHandle to a Base64-encoded string
     *
     * You can revert this operation using MegaApi::base64ToHandle
     *
     * @param handle to be converted
     * @return Base64-encoded node handle
     */
	public static String handleToBase64(long handle){
		return MegaApi.handleToBase64(handle);
	}
	
	/**
     * Converts a MegaHandle to a Base64-encoded string
     *
     * You take the ownership of the returned value
     * You can revert this operation using MegaApi::base64ToHandle
     *
     * @param User handle to be converted
     * @return Base64-encoded user handle
     */
	public static String userHandleToBase64(long handle) {
		return MegaApi.userHandleToBase64(handle);
	}
	
	/**
     * Add entropy to internal random number generators
     *
     * It's recommended to call this function with random data specially to
     * enhance security,
     *
     * @param data Byte array with random data
     * @param size Size of the byte array (in bytes)
     */
	public static void addEntropy(String data, long size){
		MegaApi.addEntropy(data, size);
	}
	
	/**
     * Reconnect and retry also transfers
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void reconnect(){
		megaApi.retryPendingConnections(true, true);
	}

	/**
     * Retry all pending requests
     *
     * When requests fails they wait some time before being retried. That delay grows exponentially if the request
     * fails again. For this reason, and since this request is very lightweight, it's recommended to call it with
     * the default parameters on every user interaction with the application. This will prevent very big delays
     * completing requests.
     */
	public void retryPendingConnections()
	{
		megaApi.retryPendingConnections();
	}

	
	/****************************************************************************************************/
	//REQUESTS
	/****************************************************************************************************/
	
	/**
     * Log in to a MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_LOGIN.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the first parameter
     * - MegaRequest::getPassword - Returns the second parameter
     *
     * If the email/password aren't valid the error code provided in onRequestFinish is
     * MegaError::API_ENOENT.
     *
     * @param email Email of the user
     * @param password Password
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void login(String email, String password, MegaRequestListenerInterface listener)
	{
		megaApi.login(email, password, createDelegateRequestListener(listener));
	}

	/**
     * Log in to a MEGA account
     *
     * @param email Email of the user
     * @param password Password
     */
	public void login(String email, String password)
	{
		megaApi.login(email, password);
	}
	
	/**
     * Log in to a public folder using a folder link
     *
     * After a successful login, you should call MegaApi::fetchNodes to get filesystem and
     * start working with the folder.
     *
     * The associated request type with this request is MegaRequest::TYPE_LOGIN.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Retuns the string "FOLDER"
     * - MegaRequest::getLink - Returns the public link to the folder
     *
     * @param Public link to a folder in MEGA
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void loginToFolder(String megaFolderLink, MegaRequestListenerInterface listener)
	{
		megaApi.loginToFolder(megaFolderLink, createDelegateRequestListener(listener));
	}

	/**
     * Log in to a public folder using a folder link
     *
     * After a successful login, you should call MegaApi::fetchNodes to get filesystem and
     * start working with the folder.
     *
     * @param Public link to a folder in MEGA
     */
	public void loginToFolder(String megaFolderLink)
	{
		megaApi.loginToFolder(megaFolderLink);
	}
	
	/**
     * Log in to a MEGA account using precomputed keys
     *
     * The associated request type with this request is MegaRequest::TYPE_LOGIN.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the first parameter
     * - MegaRequest::getPassword - Returns the second parameter
     * - MegaRequest::getPrivateKey - Returns the third parameter
     *
     * If the email/stringHash/base64pwKey aren't valid the error code provided in onRequestFinish is
     * MegaError::API_ENOENT.
     *
     * @param email Email of the user
     * @param stringHash Hash of the email returned by MegaApi::getStringHash
     * @param base64pwkey Private key calculated using MegaApi::getBase64PwKey
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void fastLogin(String email, String stringHash, String base64pwkey, MegaRequestListenerInterface listener)
	{
		megaApi.fastLogin(email, stringHash, base64pwkey, createDelegateRequestListener(listener));
	}

	/**
     * Log in to a MEGA account using precomputed keys
     *
     * @param email Email of the user
     * @param stringHash Hash of the email returned by MegaApi::getStringHash
     * @param base64pwkey Private key calculated using MegaApi::getBase64PwKey
     */
	public void fastLogin(String email, String stringHash, String base64pwkey)
	{
		megaApi.fastLogin(email, stringHash, base64pwkey);
	}
	
	/**
     * Log in to a MEGA account using a session key
     *
     * The associated request type with this request is MegaRequest::TYPE_LOGIN.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getSessionKey - Returns the session key
     *
     * @param session Session key previously dumped with MegaApi::dumpSession
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void fastLogin(String session, MegaRequestListenerInterface listener)
	{
		megaApi.fastLogin(session, createDelegateRequestListener(listener));
	}

	/**
     * Log in to a MEGA account using a session key
     *
     * @param session Session key previously dumped with MegaApi::dumpSession
     */
	public void fastLogin(String session)
	{
		megaApi.fastLogin(session);
	}
	
	/**
     * Close a MEGA session
     *
     * All clients using this session will be automatically logged out.
     *
     * You can get session information using MegaApi::getExtendedAccountDetails.
     * Then use MegaAccountDetails::getNumSessions and MegaAccountDetails::getSession
     * to get session info.
     * MegaAccountSession::getHandle provides the handle that this function needs.
     *
     * If you use mega::INVALID_HANDLE, all sessions except the current one will be closed
     *
     * @param Handle of the session. Use mega::INVALID_HANDLE to cancel all sessions except the current one
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void killSession(long sessionHandle, MegaRequestListenerInterface listener) {
		megaApi.killSession(sessionHandle, createDelegateRequestListener(listener));
	}
	
	/**
     * Close a MEGA session
     *
     * All clients using this session will be automatically logged out.
     *
     * You can get session information using MegaApi::getExtendedAccountDetails.
     * Then use MegaAccountDetails::getNumSessions and MegaAccountDetails::getSession
     * to get session info.
     * MegaAccountSession::getHandle provides the handle that this function needs.
     *
     * If you use mega::INVALID_HANDLE, all sessions except the current one will be closed
     *
     * @param Handle of the session. Use mega::INVALID_HANDLE to cancel all sessions except the current one
     */
	public void killSession(long sessionHandle) {
		megaApi.killSession(sessionHandle);
	}
	
	/**
     * Get data about the logged account
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_USER_DATA.
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getName - Returns the name of the logged user
     * - MegaRequest::getPassword - Returns the the public RSA key of the account, Base64-encoded
     * - MegaRequest::getPrivateKey - Returns the private RSA key of the account, Base64-encoded
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void getUserData(MegaRequestListenerInterface listener) 
	{
		megaApi.getUserData(createDelegateRequestListener(listener));
	}

	/**
     * Get data about the logged account
     * 
     */
	public void getUserData() 
	{
		megaApi.getUserData();
	}

	/**
     * Get data about a contact
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_USER_DATA.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the email of the contact
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getText - Returns the XMPP ID of the contact
     * - MegaRequest::getPassword - Returns the public RSA key of the contact, Base64-encoded
     *
     * @param user Contact to get the data
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void getUserData(MegaUser user, MegaRequestListenerInterface listener) {
		megaApi.getUserData(user, createDelegateRequestListener(listener));
	}

	/**
     * Get data about a contact
     *
     * @param user Contact to get the data
     */
	public void getUserData(MegaUser user) {
		megaApi.getUserData(user);
	}

	/**
     * Get data about a contact
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_USER_DATA.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the email or the Base64 handle of the contact
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getText - Returns the XMPP ID of the contact
     * - MegaRequest::getPassword - Returns the public RSA key of the contact, Base64-encoded
     *
     * @param user Email or Base64 handle of the contact
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void getUserData(String user, MegaRequestListenerInterface listener) {
		megaApi.getUserData(user, createDelegateRequestListener(listener));
	}

	/**
     * Get data about a contact
     *
     * @param user Email or Base64 handle of the contact
     */
	public void getUserData(String user) {
		megaApi.getUserData(user);
	}
	
	/**
     * Returns the current session key
     *
     * You have to be logged in to get a valid session key. Otherwise,
     * this function returns NULL.
     *
     * @return Current session key
     */
	public String dumpSession() {
		return megaApi.dumpSession();
	}
	
	/**
     * Returns the current XMPP session key
     *
     * You have to be logged in to get a valid session key. Otherwise,
     * this function returns NULL.
     *
     * @return Current XMPP session key
     */
	 public String dumpXMPPSession() {
		 return megaApi.dumpXMPPSession();
	 }
	
	/**
     * Initialize the creation of a new MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_CREATE_ACCOUNT.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the email for the account
     * - MegaRequest::getPassword - Returns the password for the account
     * - MegaRequest::getName - Returns the name of the user
     *
     * If this request succeed, a confirmation email will be sent to the users.
     * If an account with the same email already exists, you will get the error code
     * MegaError::API_EEXIST in onRequestFinish
     *
     * @param email Email for the account
     * @param password Password for the account
     * @param name Name of the user
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void createAccount(String email, String password, String name, MegaRequestListenerInterface listener)
	{
		megaApi.createAccount(email, password, name, createDelegateRequestListener(listener));
	}

	/**
     * Initialize the creation of a new MEGA account
     *
     * @param email Email for the account
     * @param password Password for the account
     * @param name Name of the user
     */
	public void createAccount(String email, String password, String name)
	{
		megaApi.createAccount(email, password, name);
	}

	/**
     * Initialize the creation of a new MEGA account with precomputed keys
     *
     * The associated request type with this request is MegaRequest::TYPE_CREATE_ACCOUNT.
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the email for the account
     * - MegaRequest::getPrivateKey - Returns the private key calculated with MegaApi::getBase64PwKey
     * - MegaRequest::getName - Returns the name of the user
     *
     * If this request succeed, a confirmation email will be sent to the users.
     * If an account with the same email already exists, you will get the error code
     * MegaError::API_EEXIST in onRequestFinish
     *
     * @param email Email for the account
     * @param base64pwkey Private key calculated with MegaApi::getBase64PwKey
     * @param name Name of the user
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void fastCreateAccount(String email, String base64pwkey, String name, MegaRequestListenerInterface listener)
	{
		megaApi.fastCreateAccount(email, base64pwkey, name, createDelegateRequestListener(listener));
	}

	/**
     * Initialize the creation of a new MEGA account with precomputed keys
     *
     * @param email Email for the account
     * @param base64pwkey Private key calculated with MegaApi::getBase64PwKey
     * @param name Name of the user
     */
	public void fastCreateAccount(String email, String base64pwkey, String name)
	{
		megaApi.fastCreateAccount(email, base64pwkey, name);
	}

	/**
     * Get information about a confirmation link
     *
     * The associated request type with this request is MegaRequest::TYPE_QUERY_SIGNUP_LINK.
     * Valid data in the MegaRequest object received on all callbacks:
     * - MegaRequest::getLink - Returns the confirmation link
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getEmail - Return the email associated with the confirmation link
     * - MegaRequest::getName - Returns the name associated with the confirmation link
     *
     * @param link Confirmation link
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void querySignupLink(String link, MegaRequestListenerInterface listener)
	{
		megaApi.querySignupLink(link, createDelegateRequestListener(listener));
	}
	
	/**
     * Get information about a confirmation link
     *
     * @param link Confirmation link
     */
	public void querySignupLink(String link)
	{
		megaApi.querySignupLink(link);
	}

	/**
     * Confirm a MEGA account using a confirmation link and the user password
     *
     * The associated request type with this request is MegaRequest::TYPE_CONFIRM_ACCOUNT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getLink - Returns the confirmation link
     * - MegaRequest::getPassword - Returns the password
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getEmail - Email of the account
     * - MegaRequest::getName - Name of the user
     *
     * @param link Confirmation link
     * @param password Password for the account
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void confirmAccount(String link, String password, MegaRequestListenerInterface listener)
	{
		megaApi.confirmAccount(link, password, createDelegateRequestListener(listener));
	}

	/**
     * Confirm a MEGA account using a confirmation link and the user password
     *
     * @param link Confirmation link
     * @param password Password for the account
     */
	public void confirmAccount(String link, String password)
	{
		megaApi.confirmAccount(link, password);
	}

	/**
     * Confirm a MEGA account using a confirmation link and a precomputed key
     *
     * The associated request type with this request is MegaRequest::TYPE_CONFIRM_ACCOUNT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getLink - Returns the confirmation link
     * - MegaRequest::getPrivateKey - Returns the base64pwkey parameter
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getEmail - Email of the account
     * - MegaRequest::getName - Name of the user
     *
     * @param link Confirmation link
     * @param base64pwkey Private key precomputed with MegaApi::getBase64PwKey
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void fastConfirmAccount(String link, String base64pwkey, MegaRequestListenerInterface listener)
	{
		megaApi.fastConfirmAccount(link, base64pwkey, createDelegateRequestListener(listener));
	}

	/**
     * Confirm a MEGA account using a confirmation link and a precomputed key
     *
     * @param link Confirmation link
     * @param base64pwkey Private key precomputed with MegaApi::getBase64PwKey
     */
	public void fastConfirmAccount(String link, String base64pwkey)
	{
		megaApi.fastConfirmAccount(link, base64pwkey);
	}
	
	/**
     * Set proxy settings
     *
     * The SDK will start using the provided proxy settings as soon as this function returns.
     *
     * @param Proxy settings
     * @see MegaProxy
     */
	public void setProxySettings(MegaProxy proxySettings){
		megaApi.setProxySettings(proxySettings);
	}
	
	/**
     * Try to detect the system's proxy settings
     *
     * Automatic proxy detection is currently supported on Windows only.
     * On other platforms, this fuction will return a MegaProxy object
     * of type MegaProxy::PROXY_NONE
     *
     * @return MegaProxy object with the detected proxy settings
     */
	public MegaProxy getAutoProxySettings(){
		return megaApi.getAutoProxySettings();
	}

	/**
     * Check if the MegaApi object is logged in
     * @return 0 if not logged in, Otherwise, a number >= 0
     */
	public int isLoggedIn()
	{
		return megaApi.isLoggedIn();
	}

	/**
     * Retuns the email of the currently open account
     *
     * If the MegaApi object isn't logged in or the email isn't available,
     * this function returns NULL
     *
     * @return Email of the account
     */
	public String getMyEmail()
	{
		return megaApi.getMyEmail();
	}
		
	/**
     * Set the active log level
     *
     * This function sets the log level of the logging system. If you set a log listener using
     * MegaApi::setLoggerObject, you will receive logs with the same or a lower level than
     * the one passed to this function.
     *
     * @param logLevel Active log level
     *
     * These are the valid values for this parameter:
     * - MegaApi::LOG_LEVEL_FATAL = 0
     * - MegaApi::LOG_LEVEL_ERROR = 1
     * - MegaApi::LOG_LEVEL_WARNING = 2
     * - MegaApi::LOG_LEVEL_INFO = 3
     * - MegaApi::LOG_LEVEL_DEBUG = 4
     * - MegaApi::LOG_LEVEL_MAX = 5
     */
	public static void setLogLevel(int logLevel)
	{
		MegaApi.setLogLevel(logLevel);
	}
	
	/**
     * Set a MegaLogger implementation to receive SDK logs
     *
     * Logs received by this objects depends on the active log level.
     * By default, it is MegaApi::LOG_LEVEL_INFO. You can change it
     * using MegaApi::setLogLevel.
     *
     * @param megaLogger MegaLogger implementation
     */
	public static void setLoggerObject(MegaLoggerInterface megaLogger)
	{
		DelegateMegaLogger newLogger = new DelegateMegaLogger(megaLogger);
		MegaApi.setLoggerObject(newLogger);
		logger = newLogger;
	}
	
	/**
     * Send a log to the logging system
     *
     * This log will be received by the active logger object (MegaApi::setLoggerObject) if
     * the log level is the same or lower than the active log level (MegaApi::setLogLevel)
     *
     * @param logLevel Log level for this message
     * @param message Message for the logging system
     * @param filename Origin of the log message
     * @param line Line of code where this message was generated
     */
	public static void log(int logLevel, String message, String filename, int line)
	{
		MegaApi.log(logLevel, message, filename, line);
	}
	
	/**
     * Send a log to the logging system
     *
     * This log will be received by the active logger object (MegaApi::setLoggerObject) if
     * the log level is the same or lower than the active log level (MegaApi::setLogLevel)
     *
     * @param logLevel Log level for this message
     * @param message Message for the logging system
     * @param filename Origin of the log message
     */
	public static void log(int logLevel, String message, String filename)
	{
		MegaApi.log(logLevel, message, filename);
	}
	
	/**
     * Send a log to the logging system
     *
     * This log will be received by the active logger object (MegaApi::setLoggerObject) if
     * the log level is the same or lower than the active log level (MegaApi::setLogLevel)
     *
     * @param logLevel Log level for this message
     * @param message Message for the logging system
     */
	public static void log(int logLevel, String message)
	{
		MegaApi.log(logLevel, message);
	}

	/**
     * Create a folder in the MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_CREATE_FOLDER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParentHandle - Returns the handle of the parent folder
     * - MegaRequest::getName - Returns the name of the new folder
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getNodeHandle - Handle of the new folder
     *
     * @param name Name of the new folder
     * @param parent Parent folder
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void createFolder(String name, MegaNode parent, MegaRequestListenerInterface listener)
	{
		megaApi.createFolder(name, parent, createDelegateRequestListener(listener));
	}
	
	/**
     * Create a folder in the MEGA account
     *
     * @param name Name of the new folder
     * @param parent Parent folder
     */
	public void createFolder(String name, MegaNode parent)
	{
		megaApi.createFolder(name, parent);
	}

	/**
     * Move a node in the MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_MOVE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node to move
     * - MegaRequest::getParentHandle - Returns the handle of the new parent for the node
     *
     * @param node Node to move
     * @param newParent New parent for the node
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void moveNode(MegaNode node, MegaNode newParent, MegaRequestListenerInterface listener)
	{
		megaApi.moveNode(node, newParent, createDelegateRequestListener(listener));
	}

	/**
     * Move a node in the MEGA account
     *
     * @param node Node to move
     * @param newParent New parent for the node
     */
	public void moveNode(MegaNode node, MegaNode newParent)
	{
		megaApi.moveNode(node, newParent);
	}

	/**
     * Copy a node in the MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_COPY
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node to copy
     * - MegaRequest::getParentHandle - Returns the handle of the new parent for the new node
     * - MegaRequest::getPublicMegaNode - Returns the node to copy (if it is a public node)
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getNodeHandle - Handle of the new node
     *
     * @param node Node to copy
     * @param newParent Parent for the new node
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void copyNode(MegaNode node, MegaNode newParent, MegaRequestListenerInterface listener)
	{
		megaApi.copyNode(node, newParent, createDelegateRequestListener(listener));
	}
	
	/**
     * Copy a node in the MEGA account
     *
     * @param node Node to copy
     * @param newParent Parent for the new node
     */
	public void copyNode(MegaNode node, MegaNode newParent)
	{
		megaApi.copyNode(node, newParent);
	}
	
	/**
     * Copy a node in the MEGA account changing the file name
     *
     * The associated request type with this request is MegaRequest::TYPE_COPY
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node to copy
     * - MegaRequest::getParentHandle - Returns the handle of the new parent for the new node
     * - MegaRequest::getPublicMegaNode - Returns the node to copy
     * - MegaRequest::getName - Returns the name for the new node
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getNodeHandle - Handle of the new node
     *
     * @param node Node to copy
     * @param newParent Parent for the new node
     * @param newName Name for the new node
     *
     * This parameter is only used if the original node is a file and it isn't a public node,
     * otherwise, it's ignored.
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void copyNode(MegaNode node, MegaNode newParent, String newName, MegaRequestListenerInterface listener)
	{
		megaApi.copyNode(node, newParent, newName, createDelegateRequestListener(listener));
	}

	/**
     * Copy a node in the MEGA account changing the file name
     *
     * @param node Node to copy
     * @param newParent Parent for the new node
     * @param newName Name for the new node
     *
     * This parameter is only used if the original node is a file and it isn't a public node,
     * otherwise, it's ignored.
     *
     */
	public void copyNode(MegaNode node, MegaNode newParent, String newName)
	{
		megaApi.copyNode(node, newParent, newName);
	}	

	/**
     * Rename a node in the MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_RENAME
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node to rename
     * - MegaRequest::getName - Returns the new name for the node
     *
     * @param node Node to modify
     * @param newName New name for the node
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void renameNode(MegaNode node, String newName, MegaRequestListenerInterface listener)
	{
		megaApi.renameNode(node, newName, createDelegateRequestListener(listener));
	}

	/**
     * Rename a node in the MEGA account
     *
     * @param node Node to modify
     * @param newName New name for the node
     */
	public void renameNode(MegaNode node, String newName)
	{
		megaApi.renameNode(node, newName);
	}

	/**
     * Remove a node from the MEGA account
     *
     * This function doesn't move the node to the Rubbish Bin, it fully removes the node. To move
     * the node to the Rubbish Bin use MegaApi::moveNode
     *
     * The associated request type with this request is MegaRequest::TYPE_REMOVE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node to remove
     *
     * @param node Node to remove
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void remove(MegaNode node, MegaRequestListenerInterface listener)
	{
		megaApi.remove(node, createDelegateRequestListener(listener));
	}

	/**
     * Remove a node from the MEGA account
     *
     * @param node Node to remove
     */
	public void remove(MegaNode node)
	{
		megaApi.remove(node);
	}
	
	/**
     * Send a node to the Inbox of another MEGA user using a MegaUser
     *
     * The associated request type with this request is MegaRequest::TYPE_COPY
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node to send
     * - MegaRequest::getEmail - Returns the email of the user that receives the node
     *
     * @param node Node to send
     * @param user User that receives the node
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void sendFileToUser(MegaNode node, MegaUser user, MegaRequestListenerInterface listener)
	{
		megaApi.sendFileToUser(node, user, createDelegateRequestListener(listener));
	}

	/**
     * Send a node to the Inbox of another MEGA user using a MegaUser
     *
     * @param node Node to send
     * @param user User that receives the node
     */
	public void sendFileToUser(MegaNode node, MegaUser user)
	{
		megaApi.sendFileToUser(node, user);
	}

	/**
     * Share or stop sharing a folder in MEGA with another user using a MegaUser
     *
     * To share a folder with an user, set the desired access level in the level parameter. If you
     * want to stop sharing a folder use the access level MegaShare::ACCESS_UNKNOWN
     *
     * The associated request type with this request is MegaRequest::TYPE_COPY
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the folder to share
     * - MegaRequest::getEmail - Returns the email of the user that receives the shared folder
     * - MegaRequest::getAccess - Returns the access that is granted to the user
     *
     * @param node The folder to share. It must be a non-root folder
     * @param user User that receives the shared folder
     * @param level Permissions that are granted to the user
     * Valid values for this parameter:
     * - MegaShare::ACCESS_UNKNOWN = -1
     * Stop sharing a folder with this user
     *
     * - MegaShare::ACCESS_READ = 0
     * - MegaShare::ACCESS_READWRITE = 1
     * - MegaShare::ACCESS_FULL = 2
     * - MegaShare::ACCESS_OWNER = 3
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void share(MegaNode node, MegaUser user, int level, MegaRequestListenerInterface listener)
	{
		megaApi.share(node, user, level, createDelegateRequestListener(listener));
	}

	/**
     * Share or stop sharing a folder in MEGA with another user using a MegaUser
     *
     * To share a folder with an user, set the desired access level in the level parameter. If you
     * want to stop sharing a folder use the access level MegaShare::ACCESS_UNKNOWN
     *
     * @param node The folder to share. It must be a non-root folder
     * @param user User that receives the shared folder
     * @param level Permissions that are granted to the user
     * Valid values for this parameter:
     * - MegaShare::ACCESS_UNKNOWN = -1
     * Stop sharing a folder with this user
     *
     * - MegaShare::ACCESS_READ = 0
     * - MegaShare::ACCESS_READWRITE = 1
     * - MegaShare::ACCESS_FULL = 2
     * - MegaShare::ACCESS_OWNER = 3
     *
     */
	public void share(MegaNode node, MegaUser user, int level)
	{
		megaApi.share(node, user, level);
	}
	
	/**
     * Share or stop sharing a folder in MEGA with another user using his email
     *
     * To share a folder with an user, set the desired access level in the level parameter. If you
     * want to stop sharing a folder use the access level MegaShare::ACCESS_UNKNOWN
     *
     * The associated request type with this request is MegaRequest::TYPE_COPY
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the folder to share
     * - MegaRequest::getEmail - Returns the email of the user that receives the shared folder
     * - MegaRequest::getAccess - Returns the access that is granted to the user
     *
     * @param node The folder to share. It must be a non-root folder
     * @param email Email of the user that receives the shared folder. If it doesn't have a MEGA account, the folder will be shared anyway
     * and the user will be invited to register an account.
     * @param level Permissions that are granted to the user
     * Valid values for this parameter:
     * - MegaShare::ACCESS_UNKNOWN = -1
     * Stop sharing a folder with this user
     *
     * - MegaShare::ACCESS_READ = 0
     * - MegaShare::ACCESS_READWRITE = 1
     * - MegaShare::ACCESS_FULL = 2
     * - MegaShare::ACCESS_OWNER = 3
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void share(MegaNode node, String email, int level, MegaRequestListenerInterface listener)
	{
		megaApi.share(node, email, level, createDelegateRequestListener(listener));
	}

	/**
     * Share or stop sharing a folder in MEGA with another user using his email
     *
     * To share a folder with an user, set the desired access level in the level parameter. If you
     * want to stop sharing a folder use the access level MegaShare::ACCESS_UNKNOWN
     *
     * @param node The folder to share. It must be a non-root folder
     * @param email Email of the user that receives the shared folder. If it doesn't have a MEGA account, the folder will be shared anyway
     * and the user will be invited to register an account.
     * @param level Permissions that are granted to the user
     * Valid values for this parameter:
     * - MegaShare::ACCESS_UNKNOWN = -1
     * Stop sharing a folder with this user
     *
     * - MegaShare::ACCESS_READ = 0
     * - MegaShare::ACCESS_READWRITE = 1
     * - MegaShare::ACCESS_FULL = 2
     * - MegaShare::ACCESS_OWNER = 3
     *
     */
	public void share(MegaNode node, String email, int level)
	{
		megaApi.share(node, email, level);
	}

	/**
     * Import a public link to the account
     *
     * The associated request type with this request is MegaRequest::TYPE_IMPORT_LINK
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getLink - Returns the public link to the file
     * - MegaRequest::getParentHandle - Returns the folder that receives the imported file
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getNodeHandle - Handle of the new node in the account
     *
     * @param megaFileLink Public link to a file in MEGA
     * @param parent Parent folder for the imported file
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void importFileLink(String megaFileLink, MegaNode parent, MegaRequestListenerInterface listener)
	{
		megaApi.importFileLink(megaFileLink, parent, createDelegateRequestListener(listener));
	}

	/**
     * Import a public link to the account
     *
     * @param megaFileLink Public link to a file in MEGA
     * @param parent Parent folder for the imported file
     */
	public void importFileLink(String megaFileLink, MegaNode parent)
	{
		megaApi.importFileLink(megaFileLink, parent);
	}

	/**
     * Get a MegaNode from a public link to a file
     *
     * A public node can be imported using MegaApi::copy or downloaded using MegaApi::startDownload
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_PUBLIC_NODE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getLink - Returns the public link to the file
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getPublicMegaNode - Public MegaNode corresponding to the public link
     *
     * @param megaFileLink Public link to a file in MEGA
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void getPublicNode(String megaFileLink, MegaRequestListenerInterface listener)
	{
		megaApi.getPublicNode(megaFileLink, createDelegateRequestListener(listener));
	}

	/**
     * Get a MegaNode from a public link to a file
     *
     * A public node can be imported using MegaApi::copy or downloaded using MegaApi::startDownload
     *
     * @param megaFileLink Public link to a file in MEGA
     */
	public void getPublicNode(String megaFileLink)
	{
		megaApi.getPublicNode(megaFileLink);
	}

	/**
     * Get the thumbnail of a node
     *
     * If the node doesn't have a thumbnail the request fails with the MegaError::API_ENOENT
     * error code
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node
     * - MegaRequest::getFile - Returns the destination path
     * - MegaRequest::getParamType - Returns MegaApi::ATTR_TYPE_THUMBNAIL
     *
     * @param node Node to get the thumbnail
     * @param dstFilePath Destination path for the thumbnail.
     * If this path is a local folder, it must end with a '\' or '/' character and (Base64-encoded handle + "0.jpg")
     * will be used as the file name inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void getThumbnail(MegaNode node, String dstFilePath, MegaRequestListenerInterface listener)
	{
		megaApi.getThumbnail(node, dstFilePath, createDelegateRequestListener(listener));
	}

	/**
     * Get the thumbnail of a node
     *
     * If the node doesn't have a thumbnail the request fails with the MegaError::API_ENOENT
     * error code
     *
     * @param node Node to get the thumbnail
     * @param dstFilePath Destination path for the thumbnail.
     * If this path is a local folder, it must end with a '\' or '/' character and (Base64-encoded handle + "0.jpg")
     * will be used as the file name inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     */
	public void getThumbnail(MegaNode node, String dstFilePath)
	{
		megaApi.getThumbnail(node, dstFilePath);
	}
	
	/**
     * Get the preview of a node
     *
     * If the node doesn't have a preview the request fails with the MegaError::API_ENOENT
     * error code
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node
     * - MegaRequest::getFile - Returns the destination path
     * - MegaRequest::getParamType - Returns MegaApi::ATTR_TYPE_PREVIEW
     *
     * @param node Node to get the preview
     * @param dstFilePath Destination path for the preview.
     * If this path is a local folder, it must end with a '\' or '/' character and (Base64-encoded handle + "1.jpg")
     * will be used as the file name inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void getPreview(MegaNode node, String dstFilePath, MegaRequestListenerInterface listener)
	{
		megaApi.getPreview(node, dstFilePath, createDelegateRequestListener(listener));
	}
	
	/**
     * Get the preview of a node
     *
     * If the node doesn't have a preview the request fails with the MegaError::API_ENOENT
     * error code
     *
     * @param node Node to get the preview
     * @param dstFilePath Destination path for the preview.
     * If this path is a local folder, it must end with a '\' or '/' character and (Base64-encoded handle + "1.jpg")
     * will be used as the file name inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     */
	public void getPreview(MegaNode node, String dstFilePath)
	{
		megaApi.getPreview(node, dstFilePath);
	}
	
	/**
     * Get the avatar of a MegaUser
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getFile - Returns the destination path
     * - MegaRequest::getEmail - Returns the email of the user
     *
     * @param user MegaUser to get the avatar
     * @param dstFilePath Destination path for the avatar. It has to be a path to a file, not to a folder.
     * If this path is a local folder, it must end with a '\' or '/' character and (email + "0.jpg")
     * will be used as the file name inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void getUserAvatar(MegaUser user, String dstFilePath, MegaRequestListenerInterface listener)
	{
		megaApi.getUserAvatar(user, dstFilePath, createDelegateRequestListener(listener));
	}
	
	/**
     * Get the avatar of a MegaUser
     *
     * @param user MegaUser to get the avatar
     * @param dstFilePath Destination path for the avatar. It has to be a path to a file, not to a folder.
     * If this path is a local folder, it must end with a '\' or '/' character and (email + "0.jpg")
     * will be used as the file name inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     */
	public void getUserAvatar(MegaUser user, String dstFilePath)
	{
		megaApi.getUserAvatar(user, dstFilePath);
	}
	
	/**
     * Get an attribute of a MegaUser.
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getText - Returns the value of the attribute
     *
     * @param user MegaUser to get the attribute
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
	public void getUserAttribute(MegaUser user, int type, MegaRequestListenerInterface listener) {
		megaApi.getUserAttribute(user, type, createDelegateRequestListener(listener));
	}

	/**
     * Get an attribute of a MegaUser.
     *
     * @param user MegaUser to get the attribute
     * @param type Attribute type
     *
     * Valid values are:
     *
     * MegaApi::USER_ATTR_FIRSTNAME = 1
     * Get the firstname of the user
     * MegaApi::USER_ATTR_LASTNAME = 2
     * Get the lastname of the user
     */
	public void getUserAttribute(MegaUser user, int type) {
		megaApi.getUserAttribute(user, type);
	}
	
	/**
     * Get an attribute of the current account.
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getText - Returns the value of the attribute
     *
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
	public void getUserAttribute(int type, MegaRequestListenerInterface listener) {
		megaApi.getUserAttribute(type, createDelegateRequestListener(listener));
	}

	/**
     * Get an attribute of the current account.
     *
     * @param type Attribute type
     *
     * Valid values are:
     *
     * MegaApi::USER_ATTR_FIRSTNAME = 1
     * Get the firstname of the user
     * MegaApi::USER_ATTR_LASTNAME = 2
     * Get the lastname of the user
     */
	public void getUserAttribute(int type) {
		megaApi.getUserAttribute(type);
	}
	
	/**
     * Cancel the retrieval of a thumbnail
     *
     * The associated request type with this request is MegaRequest::TYPE_CANCEL_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node
     * - MegaRequest::getParamType - Returns MegaApi::ATTR_TYPE_THUMBNAIL
     *
     * @param node Node to cancel the retrieval of the thumbnail
     * @param listener MegaRequestListenerInterface to track this request
     *
     * @see MegaApi::getThumbnail
     */
	public void cancelGetThumbnail(MegaNode node, MegaRequestListenerInterface listener){
		megaApi.cancelGetThumbnail(node, createDelegateRequestListener(listener));
	}
	
	/**
     * Cancel the retrieval of a thumbnail
     *
     * @param node Node to cancel the retrieval of the thumbnail
     *
     * @see MegaApi::getThumbnail
     */
	public void cancelGetThumbnail(MegaNode node){
		megaApi.cancelGetThumbnail(node);
	}
	
	/**
     * Cancel the retrieval of a preview
     *
     * The associated request type with this request is MegaRequest::TYPE_CANCEL_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node
     * - MegaRequest::getParamType - Returns MegaApi::ATTR_TYPE_PREVIEW
     *
     * @param node Node to cancel the retrieval of the preview
     * @param listener MegaRequestListenerInterface to track this request
     *
     * @see MegaApi::getPreview
     */
	public void cancelGetPreview(MegaNode node, MegaRequestListenerInterface listener){
		megaApi.cancelGetPreview(node, createDelegateRequestListener(listener));
	}
	
	/**
     * Cancel the retrieval of a preview
     * 
     * @param node Node to cancel the retrieval of the preview
     *
     * @see MegaApi::getPreview
     */
	public void cancelGetPreview(MegaNode node){
		megaApi.cancelGetPreview(node);
	}
	
	/**
     * Set the thumbnail of a MegaNode
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node
     * - MegaRequest::getFile - Returns the source path
     * - MegaRequest::getParamType - Returns MegaApi::ATTR_TYPE_THUMBNAIL
     *
     * @param node MegaNode to set the thumbnail
     * @param srcFilePath Source path of the file that will be set as thumbnail
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void setThumbnail(MegaNode node, String srcFilePath, MegaRequestListenerInterface listener)
	{
		megaApi.setThumbnail(node, srcFilePath, createDelegateRequestListener(listener));
	}

	/**
     * Set the thumbnail of a MegaNode
     *
     * @param node MegaNode to set the thumbnail
     * @param srcFilePath Source path of the file that will be set as thumbnail
     */
	public void setThumbnail(MegaNode node, String srcFilePath)
	{
		megaApi.setThumbnail(node, srcFilePath);
	}
	
	/**
     * Set the preview of a MegaNode
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_FILE
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node
     * - MegaRequest::getFile - Returns the source path
     * - MegaRequest::getParamType - Returns MegaApi::ATTR_TYPE_PREVIEW
     *
     * @param node MegaNode to set the preview
     * @param srcFilePath Source path of the file that will be set as preview
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void setPreview(MegaNode node, String srcFilePath, MegaRequestListenerInterface listener)
	{
		megaApi.setPreview(node, srcFilePath, createDelegateRequestListener(listener));
	}
	
	/**
     * Set the preview of a MegaNode
     *
     * @param node MegaNode to set the preview
     * @param srcFilePath Source path of the file that will be set as preview
     */
	public void setPreview(MegaNode node, String srcFilePath)
	{
		megaApi.setPreview(node, srcFilePath);
	}
	
	/**
     * Set the avatar of the MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getFile - Returns the source path
     *
     * @param srcFilePath Source path of the file that will be set as avatar
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void setAvatar(String srcFilePath, MegaRequestListenerInterface listener){
		megaApi.setAvatar(srcFilePath, createDelegateRequestListener(listener));
	}
	
	/**
     * Set the avatar of the MEGA account
     *
     * @param srcFilePath Source path of the file that will be set as avatar
     */
	public void setAvatar(String srcFilePath){
		megaApi.setAvatar(srcFilePath);
	}
	
	/**
     * Set an attribute of the current user
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type
     * - MegaRequest::getFile - Returns the new value for the attribute
     *
     * @param type Attribute type
     *
     * Valid values are:
     *
     * USER_ATTR_FIRSTNAME = 1
     * Change the firstname of the user
     * USER_ATTR_LASTNAME = 2
     * Change the lastname of the user
     *
     * @param value New attribute value
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void setUserAttribute(int type, String value, MegaRequestListenerInterface listener) {
		megaApi.setUserAttribute(type, value, createDelegateRequestListener(listener));
	}
	
	/**
     * Set an attribute of the current user
     *
     * @param type Attribute type
     *
     * Valid values are:
     *
     * USER_ATTR_FIRSTNAME = 1
     * Change the firstname of the user
     * USER_ATTR_LASTNAME = 2
     * Change the lastname of the user
     *
     * @param value New attribute value
     */
	public void setUserAttribute(int type, String value) {
		megaApi.setUserAttribute(type, value);
	}
	
	/**
     * Generate a public link of a file/folder in MEGA
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
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void exportNode(MegaNode node, MegaRequestListenerInterface listener)
	{
		megaApi.exportNode(node, createDelegateRequestListener(listener));
	}

	/**
     * Generate a public link of a file/folder in MEGA
     *
     * @param node MegaNode to get the public link
     */
	public void exportNode(MegaNode node)
	{
		megaApi.exportNode(node);
	}
	
	/**
     * Stop sharing a file/folder
     *
     * The associated request type with this request is MegaRequest::TYPE_EXPORT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the node
     * - MegaRequest::getAccess - Returns false
     *
     * @param node MegaNode to stop sharing
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void disableExport(MegaNode node, MegaRequestListenerInterface listener)
	{
		megaApi.disableExport(node, createDelegateRequestListener(listener));
	}
	
	/**
     * Stop sharing a file/folder
     *
     * @param node MegaNode to stop sharing
     */
	public void disableExport(MegaNode node)
	{
		megaApi.disableExport(node);
	}

	/**
     * Fetch the filesystem in MEGA
     *
     * The MegaApi object must be logged in in an account or a public folder
     * to successfully complete this request.
     *
     * The associated request type with this request is MegaRequest::TYPE_FETCH_NODES
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void fetchNodes(MegaRequestListenerInterface listener)
	{
		megaApi.fetchNodes(createDelegateRequestListener(listener));
	}

	/**
     * Fetch the filesystem in MEGA
     *
     * The MegaApi object must be logged in in an account or a public folder
     * to successfully complete this request.
     */
	public void fetchNodes()
	{
		megaApi.fetchNodes();
	}
	
	/**
     * Get details about the MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_ACCOUNT_DETAILS
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getMegaAccountDetails - Details of the MEGA account
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void getAccountDetails(MegaRequestListenerInterface listener)
	{
		megaApi.getAccountDetails(createDelegateRequestListener(listener));
	}

	/**
     * Get details about the MEGA account
     */
	public void getAccountDetails()
	{
		megaApi.getAccountDetails();
	}

	/**
     * Get details about the MEGA account
     *
     * This function allows to optionally get data about sessions, transactions and purchases related to the account.
     *
     * The associated request type with this request is MegaRequest::TYPE_ACCOUNT_DETAILS
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getMegaAccountDetails - Details of the MEGA account
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void getExtendedAccountDetails(boolean sessions, boolean purchases, boolean transactions, MegaRequestListenerInterface listener) {
		megaApi.getExtendedAccountDetails(sessions, purchases, transactions, createDelegateRequestListener(listener));
	}
	
	/**
     * Get details about the MEGA account
     *
     * This function allows to optionally get data about sessions, transactions and purchases related to the account.
     *
     */
	public void getExtendedAccountDetails(boolean sessions, boolean purchases, boolean transactions) {
		megaApi.getExtendedAccountDetails(sessions, purchases, transactions);
	}
	
	/**
     * Get details about the MEGA account
     *
     * This function allows to optionally get data about sessions and purchases related to the account.
     *
     */
	public void getExtendedAccountDetails(boolean sessions, boolean purchases) {
		megaApi.getExtendedAccountDetails(sessions, purchases);
	}
	
	/**
     * Get details about the MEGA account
     *
     * This function allows to optionally get data about sessions related to the account.
     *
     */
	public void getExtendedAccountDetails(boolean sessions) {
		megaApi.getExtendedAccountDetails(sessions);
	}
	
	/**
     * Get details about the MEGA account
     * 
     */
	public void getExtendedAccountDetails() {
		megaApi.getExtendedAccountDetails();
	}
	
	/**
     * Get the available pricing plans to upgrade a MEGA account
     *
     * You can get a payment URL for any of the pricing plans provided by this function
     * using MegaApi::getPaymentUrl
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_PRICING
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getPricing - MegaPricing object with all pricing plans
     *
     * @param listener MegaRequestListenerInterface to track this request
     *
     * @see MegaApi::getPaymentUrl
     */
	public void getPricing(MegaRequestListenerInterface listener) 
	{
	    megaApi.getPricing(createDelegateRequestListener(listener));
	}

	/**
     * Get the available pricing plans to upgrade a MEGA account
     *
     * You can get a payment URL for any of the pricing plans provided by this function
     * using MegaApi::getPaymentUrl
     *
     * @see MegaApi::getPaymentUrl
     */
	public void getPricing() 
	{
		megaApi.getPricing();
	}

	/**
     * Get the payment id for an upgrade
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_PAYMENT_ID
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the product
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getLink - Payment link
     *
     * @param productHandle Handle of the product (see MegaApi::getPricing)
     * @param listener MegaRequestListenerInterface to track this request
     *
     * @see MegaApi::getPricing
     */
	public void getPaymentId(long productHandle, MegaRequestListenerInterface listener) 
	{
		megaApi.getPaymentId(productHandle, createDelegateRequestListener(listener));
	}

	/**
     * Get the payment URL for an upgrade
     *
     * @param productHandle Handle of the product (see MegaApi::getPricing)
     *
     * @see MegaApi::getPricing
     */
	public void getPaymentId(long productHandle) 
	{
		megaApi.getPaymentId(productHandle);
	}
	
	/**
     * Upgrade an account
     * @param productHandle Product handle to purchase
     *
     * It's possible to get all pricing plans with their product handles using
     * MegaApi::getPricing
     *
     * The associated request type with this request is MegaRequest::TYPE_UPGRADE_ACCOUNT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getNodeHandle - Returns the handle of the product
     * - MegaRequest::getNumber - Returns the payment method
     *
     * @param paymentMethod Payment method
     * Valid values are:
     * - MegaApi::PAYMENT_METHOD_BALANCE = 0
     * Use the account balance for the payment
     *
     * - MegaApi::PAYMENT_METHOD_CREDIT_CARD = 8
     * Complete the payment with your credit card. Use MegaApi::creditCardStore to add
     * a credit card to your account
     *
     * @param listener MegaRequestListener to track this request
     */
	public void upgradeAccount(long productHandle, int paymentMethod, MegaRequestListenerInterface listener) {
		megaApi.upgradeAccount(productHandle, paymentMethod, createDelegateRequestListener(listener));
	}

	/**
     * Upgrade an account
     * @param productHandle Product handle to purchase
     *
     * It's possible to get all pricing plans with their product handles using
     * MegaApi::getPricing
     *
     * @param paymentMethod Payment method
     * Valid values are:
     * - MegaApi::PAYMENT_METHOD_BALANCE = 0
     * Use the account balance for the payment
     *
     * - MegaApi::PAYMENT_METHOD_CREDIT_CARD = 8
     * Complete the payment with your credit card. Use MegaApi::creditCardStore to add
     * a credit card to your account
     */
	public void upgradeAccount(long productHandle, int paymentMethod) {
		megaApi.upgradeAccount(productHandle, paymentMethod);
	}
	
	/**
     * Send the Google Play receipt after a correct purchase of a subscription
     *
     * @param receipt String The complete receipt from Google Play
     * @param listener MegaRequestListenerInterface to track this request
     * 
     */
	public void submitPurchaseReceipt(String receipt, MegaRequestListenerInterface listener) {
		megaApi.submitPurchaseReceipt(receipt, createDelegateRequestListener(listener));
    }

	/**
     * Send the Google Play receipt after a correct purchase of a subscription
     *
     * @param receipt String The complete receipt from Google Play
     * 
     */
	public void submitPurchaseReceipt(String receipt) {
		megaApi.submitPurchaseReceipt(receipt);
    }
	
	/**
     * Store a credit card
     *
     * The associated request type with this request is MegaRequest::TYPE_CREDIT_CARD_STORE
     *
     * @param address1 Billing address
     * @param address2 Second line of the billing address (optional)
     * @param city City of the billing address
     * @param province Province of the billing address
     * @param country Contry of the billing address
     * @param postalcode Postal code of the billing address
     * @param firstname Firstname of the owner of the credit card
     * @param lastname Lastname of the owner of the credit card
     * @param creditcard Credit card number. Only digits, no spaces nor dashes
     * @param expire_month Expire month of the credit card. Must have two digits ("03" for example)
     * @param expire_year Expire year of the credit card. Must have four digits ("2010" for example)
     * @param cv2 Security code of the credit card (3 digits)
     * @param listener MegaRequestListener to track this request
     */
	public void creditCardStore(String address1, String address2, String city, String province, String country, String postalcode, String firstname, String lastname, String creditcard, String expire_month, String expire_year, String cv2, MegaRequestListenerInterface listener) {
	    megaApi.creditCardStore(address1, address2, city, province, country, postalcode, firstname, lastname, creditcard, expire_month, expire_year, cv2, createDelegateRequestListener(listener));
	}

	/**
     * Store a credit card
     *
     * @param address1 Billing address
     * @param address2 Second line of the billing address (optional)
     * @param city City of the billing address
     * @param province Province of the billing address
     * @param country Contry of the billing address
     * @param postalcode Postal code of the billing address
     * @param firstname Firstname of the owner of the credit card
     * @param lastname Lastname of the owner of the credit card
     * @param creditcard Credit card number. Only digits, no spaces nor dashes
     * @param expire_month Expire month of the credit card. Must have two digits ("03" for example)
     * @param expire_year Expire year of the credit card. Must have four digits ("2010" for example)
     * @param cv2 Security code of the credit card (3 digits)
     */
	public void creditCardStore(String address1, String address2, String city, String province, String country, String postalcode, String firstname, String lastname, String creditcard, String expire_month, String expire_year, String cv2) {
		megaApi.creditCardStore(address1, address2, city, province, country, postalcode, firstname, lastname, creditcard, expire_month, expire_year, cv2);
	}

	/**
     * Get the credit card subscriptions of the account
     *
     * The associated request type with this request is MegaRequest::TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getNumber - Number of credit card subscriptions
     *
     * @param listener MegaRequestListener to track this request
     */
	public void creditCardQuerySubscriptions(MegaRequestListenerInterface listener) {
		megaApi.creditCardQuerySubscriptions(createDelegateRequestListener(listener));
	}
	
	/**
     * Get the credit card subscriptions of the account
     *
     */
	public void creditCardQuerySubscriptions() {
		  megaApi.creditCardQuerySubscriptions();
	}
	
	/**
     * Cancel credit card subscriptions if the account
     *
     * The associated request type with this request is MegaRequest::TYPE_CREDIT_CARD_CANCEL_SUBSCRIPTIONS
     *
     * @param reason Reason for the cancellation. It can be NULL.
     * @param listener MegaRequestListener to track this request
     */
	public void creditCardCancelSubscriptions(String reason, MegaRequestListenerInterface listener) {
		megaApi.creditCardCancelSubscriptions(reason, createDelegateRequestListener(listener));
	}

	/**
     * Cancel credit card subscriptions if the account
     * 
     * @param reason Reason for the cancellation. It can be NULL.
     * 
     */
	public void creditCardCancelSubscriptions(String reason) {
		megaApi.creditCardCancelSubscriptions(reason);
	}
	
	/**
     * Get the available payment methods
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_PAYMENT_METHODS
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getNumber - Bitfield with available payment methods
     *
     * To know if a payment method is available, you can do a check like this one:
     * request->getNumber() & (1 << MegaApi::PAYMENT_METHOD_CREDIT_CARD)
     *
     * @param listener MegaRequestListener to track this request
     */
	public void getPaymentMethods(MegaRequestListenerInterface listener) {
		megaApi.getPaymentMethods(createDelegateRequestListener(listener));
	}

	/**
     * Get the available payment methods
     */
	public void getPaymentMethods() {
		megaApi.getPaymentMethods();
	}
	
	/**
     * Export the master key of the account
     *
     * The returned value is a Base64-encoded string
     *
     * With the master key, it's possible to start the recovery of an account when the
     * password is lost:
     * - https://mega.co.nz/#recovery
     *
     * @return Base64-encoded master key
     */
	public String exportMasterKey() 
	{
		return megaApi.exportMasterKey();
	}
	
	/**
     * Change the password of the MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_CHANGE_PW
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getPassword - Returns the old password
     * - MegaRequest::getNewPassword - Returns the new password
     *
     * @param oldPassword Old password
     * @param newPassword New password
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void changePassword(String oldPassword, String newPassword, MegaRequestListenerInterface listener)
	{
		megaApi.changePassword(oldPassword, newPassword, createDelegateRequestListener(listener));
	}

	/**
     * Change the password of the MEGA account
     *
     * @param oldPassword Old password
     * @param newPassword New password
     */
	public void changePassword(String oldPassword, String newPassword)
	{
		megaApi.changePassword(oldPassword, newPassword);
	}
	
	/**
     * Add a new contact to the MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_ADD_CONTACT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the email of the contact
     *
     * @param email Email of the new contact
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void addContact(String email, MegaRequestListenerInterface listener)
	{
		megaApi.addContact(email, createDelegateRequestListener(listener));
	}

	/**
     * Add a new contact to the MEGA account
     *
     * @param email Email of the new contact
     */
	public void addContact(String email)
	{
		megaApi.addContact(email);
	}
	
	/**
     * Remove a contact to the MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_REMOVE_CONTACT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getEmail - Returns the email of the contact
     *
     * @param email Email of the contact
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void removeContact (MegaUser user, MegaRequestListenerInterface listener){
		megaApi.removeContact(user, createDelegateRequestListener(listener));
	}
	
	/**
     * Remove a contact to the MEGA account
     *
     * @param email Email of the contact
     */
	public void removeContact (MegaUser user){
		megaApi.removeContact(user);
	}

	/**
     * Logout of the MEGA account
     *
     * The associated request type with this request is MegaRequest::TYPE_LOGOUT
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void logout(MegaRequestListenerInterface listener)
	{
		megaApi.logout(createDelegateRequestListener(listener));
	}
	
	/**
     * Logout of the MEGA account
     */
	public void logout()
	{
		megaApi.logout();
	}	
	
	/**
     * @brief Logout of the MEGA account without invalidating the session
     *
     * The associated request type with this request is MegaRequest::TYPE_LOGOUT
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void localLogout(MegaRequestListenerInterface listener){
		megaApi.localLogout(createDelegateRequestListener(listener));
	}
	
	/**
     * Logout of the MEGA account without invalidating the session
     *
     */
	public void localLogout(){
		megaApi.localLogout();
	}

	/**
     * Submit feedback about the app
     *
     * The User-Agent is used to identify the app. It can be set in MegaApi::MegaApi
     *
     * The associated request type with this request is MegaRequest::TYPE_REPORT_EVENT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns MegaApi::EVENT_FEEDBACK
     * - MegaRequest::getText - Retuns the comment about the app
     * - MegaRequest::getNumber - Returns the rating for the app
     *
     * @param rating Integer to rate the app. Valid values: from 1 to 5.
     * @param comment Comment about the app
     * @param listener MegaRequestListenerInterface to track this request
     *
     * @deprecated This function is for internal usage of MEGA apps. This feedback
     * is sent to MEGA servers.
     *
     */
	public void submitFeedback(int rating, String comment, MegaRequestListenerInterface listener) 
	{
		megaApi.submitFeedback(rating, comment, createDelegateRequestListener(listener));
	}

	/**
     * Submit feedback about the app
     *
     * The User-Agent is used to identify the app. It can be set in MegaApi::MegaApi
     *
     * @param rating Integer to rate the app. Valid values: from 1 to 5.
     * @param comment Comment about the app
     *
     * @deprecated This function is for internal usage of MEGA apps. This feedback
     * is sent to MEGA servers.
     *
     */
	public void submitFeedback(int rating, String comment) 
	{
		megaApi.submitFeedback(rating, comment);
	}

	/**
     * Send a debug report
     *
     * The User-Agent is used to identify the app. It can be set in MegaApi::MegaApi
     *
     * The associated request type with this request is MegaRequest::TYPE_REPORT_EVENT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns MegaApi::EVENT_DEBUG
     * - MegaRequest::getText - Retuns the debug message
     *
     * @param text Debug message
     * @param listener MegaRequestListenerInterface to track this request
     *
     * @deprecated This function is for internal usage of MEGA apps. This feedback
     * is sent to MEGA servers.
     */
	public void reportDebugEvent(String text, MegaRequestListenerInterface listener) 
	{
		megaApi.reportDebugEvent(text, createDelegateRequestListener(listener));
	}

	/**
     * Send a debug report
     *
     * The User-Agent is used to identify the app. It can be set in MegaApi::MegaApi
     *
     * The associated request type with this request is MegaRequest::TYPE_REPORT_EVENT
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns MegaApi::EVENT_DEBUG
     * - MegaRequest::getText - Retuns the debug message
     *
     * @param text Debug message
     * 
     * @deprecated This function is for internal usage of MEGA apps. This feedback
     * is sent to MEGA servers.
     */
	public void reportDebugEvent(String text) 
	{
		megaApi.reportDebugEvent(text);
	}

	/****************************************************************************************************/
	//TRANSFERS
	/****************************************************************************************************/
	
	/**
     * Upload a file
     * 
     * @param Local path of the file
     * @param Parent node for the file in the MEGA account
     * @param listener MegaTransferListener to track this transfer
     */
	public void startUpload(String localPath, MegaNode parent, MegaTransferListenerInterface listener)
	{
		megaApi.startUpload(localPath, parent, createDelegateTransferListener(listener));
	}

	/**
     * Upload a file
     * 
     * @param Local path of the file
     * @param Parent node for the file in the MEGA account
     */
	public void startUpload(String localPath, MegaNode parent)
	{
		megaApi.startUpload(localPath, parent);
	}
	
	/**
     * Upload a file with a custom modification time
     * 
     * @param localPath Local path of the file
     * @param parent Parent node for the file in the MEGA account
     * @param mtime Custom modification time for the file in MEGA (in seconds since the epoch)
     * @param listener MegaTransferListener to track this transfer
     */
	public void startUpload(String localPath, MegaNode parent, long mtime, MegaTransferListenerInterface listener) {
		megaApi.startUpload(localPath, parent, mtime, createDelegateTransferListener(listener));
	}
	
	/**
     * Upload a file with a custom modification time
     * 
     * @param localPath Local path of the file
     * @param parent Parent node for the file in the MEGA account
     * @param mtime Custom modification time for the file in MEGA (in seconds since the epoch)
     */
	public void startUpload(String localPath, MegaNode parent, long mtime) {
		megaApi.startUpload(localPath, parent, mtime);
	}
	
	/**
     * Upload a file with a custom name
     * 
     * @param localPath Local path of the file
     * @param parent Parent node for the file in the MEGA account
     * @param fileName Custom file name for the file in MEGA
     * @param listener MegaTransferListener to track this transfer
     */
	public void startUpload(String localPath, MegaNode parent, String fileName, MegaTransferListenerInterface listener)
	{
		megaApi.startUpload(localPath, parent, fileName, createDelegateTransferListener(listener));
	}

	/**
     * Upload a file with a custom name
     * 
     * @param localPath Local path of the file
     * @param parent Parent node for the file in the MEGA account
     * @param fileName Custom file name for the file in MEGA
     */
	public void startUpload(String localPath, MegaNode parent, String fileName)
	{
		megaApi.startUpload(localPath, parent, fileName);
	}
	
	/**
     * Upload a file with a custom name and a custom modification time
     * 
     * @param localPath Local path of the file
     * @param parent Parent node for the file in the MEGA account
     * @param fileName Custom file name for the file in MEGA
     * @param mtime Custom modification time for the file in MEGA (in seconds since the epoch)
     * @param listener MegaTransferListener to track this transfer
     */
	public void startUpload(String localPath, MegaNode parent, String fileName, long mtime, MegaTransferListenerInterface listener) {
		megaApi.startUpload(localPath, parent, fileName, mtime, createDelegateTransferListener(listener));
	}
	
	/**
     * Upload a file with a custom name and a custom modification time
     * 
     * @param localPath Local path of the file
     * @param parent Parent node for the file in the MEGA account
     * @param fileName Custom file name for the file in MEGA
     * @param mtime Custom modification time for the file in MEGA (in seconds since the epoch)
     */
	public void startUpload(String localPath, MegaNode parent, String fileName, long mtime) {
		megaApi.startUpload(localPath, parent, fileName, mtime);
	}

	/**
     * Download a file from MEGA
     *  
     * @param node MegaNode that identifies the file
     * @param localPath Destination path for the file
     * If this path is a local folder, it must end with a '\' or '/' character and the file name
     * in MEGA will be used to store a file inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     *
     * @param listener MegaTransferListener to track this transfer
     */
	public void startDownload(MegaNode node, String localPath, MegaTransferListenerInterface listener)
	{
		megaApi.startDownload(node, localPath, createDelegateTransferListener(listener));
	}

	/**
     * Download a file from MEGA
     *  
     * @param node MegaNode that identifies the file
     * @param localPath Destination path for the file
     * If this path is a local folder, it must end with a '\' or '/' character and the file name
     * in MEGA will be used to store a file inside that folder. If the path doesn't finish with
     * one of these characters, the file will be downloaded to a file in that path.
     */
	public void startDownload(MegaNode node, String localPath)
	{
		megaApi.startDownload(node, localPath);
	}
	
	/**
     * Start an streaming download
     *
     * Streaming downloads don't save the downloaded data into a local file. It is provided
     * in MegaTransferListener::onTransferUpdate in a byte buffer. The pointer is returned by
     * MegaTransfer::getLastBytes and the size of the buffer in MegaTransfer::getDeltaSize
     *
     * The same byte array is also provided in the callback MegaTransferListener::onTransferData for
     * compatibility with other programming languages. Only the MegaTransferListener passed to this function
     * will receive MegaTransferListener::onTransferData callbacks. MegaTransferListener objects registered
     * with MegaApi::addTransferListener won't receive them for performance reasons
     *
     * @param node MegaNode that identifies the file (public nodes aren't supported yet)
     * @param startPos First byte to download from the file
     * @param size Size of the data to download
     * @param listener MegaTransferListener to track this transfer
     */
	public void startStreaming(MegaNode node, long startPos, long size, MegaTransferListenerInterface listener)
	{
		megaApi.startStreaming(node, startPos, size, createDelegateTransferListener(listener));
	}
	
	/**
     * Cancel a transfer
     *
     * When a transfer is cancelled, it will finish and will provide the error code
     * MegaError::API_EINCOMPLETE in MegaTransferListener::onTransferFinish and
     * MegaListener::onTransferFinish
     *
     * The associated request type with this request is MegaRequest::TYPE_CANCEL_TRANSFER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getTransferTag - Returns the tag of the cancelled transfer (MegaTransfer::getTag)
     *
     * @param transfer MegaTransfer object that identifies the transfer
     * You can get this object in any MegaTransferListener callback or any MegaListener callback
     * related to transfers.
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void cancelTransfer(MegaTransfer transfer, MegaRequestListenerInterface listener)
	{
		megaApi.cancelTransfer(transfer, createDelegateRequestListener(listener));
	}

	/**
     * Cancel a transfer
     *
     * @param transfer MegaTransfer object that identifies the transfer
     * You can get this object in any MegaTransferListener callback or any MegaListener callback
     * related to transfers.

     */
	public void cancelTransfer(MegaTransfer transfer)
	{
		megaApi.cancelTransfer(transfer);
	}
	
	/**
     * Cancel the transfer with a specific tag
     *
     * When a transfer is cancelled, it will finish and will provide the error code
     * MegaError::API_EINCOMPLETE in MegaTransferListener::onTransferFinish and
     * MegaListener::onTransferFinish
     *
     * The associated request type with this request is MegaRequest::TYPE_CANCEL_TRANSFER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getTransferTag - Returns the tag of the cancelled transfer (MegaTransfer::getTag)
     *
     * @param transferTag tag that identifies the transfer
     * You can get this tag using MegaTransfer::getTag
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void cancelTransferByTag(int transferTag, MegaRequestListenerInterface listener) {
		megaApi.cancelTransferByTag(transferTag, createDelegateRequestListener(listener));
	}
	
	/**
     * Cancel the transfer with a specific tag
     *
     * @param transferTag tag that identifies the transfer
     * You can get this tag using MegaTransfer::getTag
     */
	public void cancelTransferByTag(int transferTag) {
		megaApi.cancelTransferByTag(transferTag);
	}

	/**
     * Cancel all transfers of the same type
     *
     * The associated request type with this request is MegaRequest::TYPE_CANCEL_TRANSFERS
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the first parameter
     *
     * @param type Type of transfers to cancel.
     * Valid values are:
     * - MegaTransfer::TYPE_DOWNLOAD = 0
     * - MegaTransfer::TYPE_UPLOAD = 1
     *
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void cancelTransfers(int direction, MegaRequestListenerInterface listener)
	{
		megaApi.cancelTransfers(direction, createDelegateRequestListener(listener));
	}

	/**
     * Cancel all transfers of the same type
     *
     * @param type Type of transfers to cancel.
     * Valid values are:
     * - MegaTransfer::TYPE_DOWNLOAD = 0
     * - MegaTransfer::TYPE_UPLOAD = 1
     */
	public void cancelTransfers(int direction)
	{
		megaApi.cancelTransfers(direction);
	}

	/**
     * Pause/resume all transfers
     *
     * The associated request type with this request is MegaRequest::TYPE_PAUSE_TRANSFERS
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getFlag - Returns the first parameter
     *
     * @param pause true to pause all transfers / false to resume all transfers
     * @param listener MegaRequestListenerInterface to track this request
     */
	public void pauseTransfers(boolean pause, MegaRequestListenerInterface listener)
	{
		megaApi.pauseTransfers(pause, createDelegateRequestListener(listener));
	}

	/**
     * Pause/resume all transfers
     *
     * @param pause true to pause all transfers / false to resume all transfers
     */
	public void pauseTransfers(boolean pause)
	{
		megaApi.pauseTransfers(pause);
	}

	/**
     * Set the upload speed limit
     *
     * The limit will be applied on the server side when starting a transfer. Thus the limit won't be
     * applied for already started uploads and it's applied per storage server.
     *
     * @param bpslimit -1 to automatically select the limit, 0 for no limit, otherwise the speed limit
     * in bytes per second
     */
	public void setUploadLimit(int bpslimit)
	{
		megaApi.setUploadLimit(bpslimit);
	}
		
	/**
     * Get all active transfers
     * 
     * @return List with all active transfers
     */
	public ArrayList<MegaTransfer> getTransfers()
	{
		return transferListToArray(megaApi.getTransfers());
	}
	
	/**
     * Get the transfer with a transfer tag
     *
     * That tag can be got using MegaTransfer::getTag
     *
     * @param Transfer tag to check
     * @return MegaTransfer object with that tag, or NULL if there isn't any
     * active transfer with it
     *
     */
	public MegaTransfer getTransferByTag(int transferTag) {
		return megaApi.getTransferByTag(transferTag);
	}
	
	/**
	 * Get all active transfers based on the type
	 * 
	 * @param type MegaTransfer.TYPE_DOWNLOAD || MegaTransfer.TYPE_UPLOAD 
	 * 
	 * @return List with all active download or upload transfers
	 */
	public ArrayList<MegaTransfer> getTransfers(int type){
		return transferListToArray(megaApi.getTransfers(type));
	}
	
	/**
     * Force a loop of the SDK thread
     * 
     * @deprecated This function is only here for debugging purposes. It will probably
     * be removed in future updates
     */
	public void update(){
		megaApi.update();
	}
	
	/**
     * Check if the SDK is waiting for the server
     * 
     * @return true if the SDK is waiting for the server to complete a request
     */
    public boolean isWaiting(){
    	return megaApi.isWaiting();
    }
    
    /**
     * Get the number of pending uploads
     *
     * @return Pending uploads
     *
     * @deprecated Function related to statistics will be reviewed in future updates to
     * provide more data and avoid race conditions. They could change or be removed in the current form.
     */
	public int getNumPendingUploads()
	{
		return megaApi.getNumPendingUploads();
	}
	
	/**
     * Get the number of pending downloads
     * 
     * @return Pending downloads
     *
     * @deprecated Function related to statistics will be reviewed in future updates to
     * provide more data and avoid race conditions. They could change or be removed in the current form.
     */
	public int getNumPendingDownloads()
	{
		return megaApi.getNumPendingDownloads();
	}
	
	/**
     * Get the number of queued uploads since the last call to MegaApi::resetTotalUploads
     * 
     * @return Number of queued uploads since the last call to MegaApi::resetTotalUploads
     *
     * @deprecated Function related to statistics will be reviewed in future updates to
     * provide more data and avoid race conditions. They could change or be removed in the current form.
     */
	public int getTotalUploads()
	{
		return megaApi.getTotalUploads();
	}
	
	/**
     * Get the number of queued uploads since the last call to MegaApi::resetTotalDownloads
     * 
     * @return Number of queued uploads since the last call to MegaApi::resetTotalDownloads
     *
     * @deprecated Function related to statistics will be reviewed in future updates. They
     * could change or be removed in the current form.
     */
	public int getTotalDownloads()
	{
		return megaApi.getTotalDownloads();
	}
	
	/**
     * Reset the number of total downloads
     * This function resets the number returned by MegaApi::getTotalDownloads
     *
     * @deprecated Function related to statistics will be reviewed in future updates to
     * provide more data and avoid race conditions. They could change or be removed in the current form.
     *
     */
	public void resetTotalDownloads()
	{
		megaApi.resetTotalDownloads();
	}
	
	/**
     * Reset the number of total uploads
     * This function resets the number returned by MegaApi::getTotalUploads
     *
     * @deprecated Function related to statistics will be reviewed in future updates to
     * provide more data and avoid race conditions. They could change or be removed in the current form.
     */
	public void resetTotalUploads()
	{
		megaApi.resetTotalUploads();
	}
	
	/**
     * Get the total downloaded bytes since the creation of the MegaApi object
     * 
     * @return Total downloaded bytes since the creation of the MegaApi object
     *
     * @deprecated Function related to statistics will be reviewed in future updates to
     * provide more data and avoid race conditions. They could change or be removed in the current form.
     */
	public long getTotalDownloadedBytes(){
		return megaApi.getTotalDownloadedBytes();
	}
	
	/**
     * Get the total uploaded bytes since the creation of the MegaApi object
     * 
     * @return Total uploaded bytes since the creation of the MegaApi object
     *
     * @deprecated Function related to statistics will be reviewed in future updates to
     * provide more data and avoid race conditions. They could change or be removed in the current form.
     *
     */
	public long getTotalUploadedBytes(){
		return megaApi.getTotalUploadedBytes();
	}
	
	
	/**
     * Update the number of pending downloads/uploads
     *
     * This function forces a count of the pending downloads/uploads. It could
     * affect the return value of MegaApi::getNumPendingDownloads and
     * MegaApi::getNumPendingUploads.
     *
     * @deprecated Function related to statistics will be reviewed in future updates to
     * provide more data and avoid race conditions. They could change or be removed in the current form.
     *
     */
	public void updateStats(){
		megaApi.updateStats();
	}
	
	public void startUnbufferedDownload(MegaNode node, long startOffset, long size, OutputStream outputStream, MegaTransferListenerInterface listener)
	{
		DelegateMegaTransferListener delegateListener = new DelegateOutputMegaTransferListener(this, outputStream, listener, true);
		activeTransferListeners.add(delegateListener);
		megaApi.startStreaming(node, startOffset, size, delegateListener);
	}
	
	public void startUnbufferedDownload(MegaNode node, OutputStream outputStream, MegaTransferListenerInterface listener)
	{
		startUnbufferedDownload(node, 0, node.getSize(), outputStream, listener);
	}
	
	/****************************************************************************************************/
	//FILESYSTEM METHODS
	/****************************************************************************************************/
	
	/**
	 * Get the number of child nodes
	 *
	 * If the node doesn't exist in MEGA or isn't a folder,
	 * this function returns 0
	 *
	 * This function doesn't search recursively, only returns the direct child nodes.
	 *
	 * @param parent Parent node
	 * @return Number of child nodes
	 */
	public int getNumChildren(MegaNode parent) 
	{
		return megaApi.getNumChildren(parent);
	}
	
	/**
	 * Get the number of child files of a node
	 *
	 * If the node doesn't exist in MEGA or isn't a folder,
	 * this function returns 0
	 *
	 * This function doesn't search recursively, only returns the direct child files.
	 *
	 * @param parent Parent node
	 * @return Number of child files
	 */
	public int getNumChildFiles(MegaNode parent) 
	{
		return megaApi.getNumChildFiles(parent);
	}

	/**
	 * Get the number of child folders of a node
	 *
	 * If the node doesn't exist in MEGA or isn't a folder,
	 * this function returns 0
	 *
	 * This function doesn't search recursively, only returns the direct child folders.
	 *
	 * @param parent Parent node
	 * @return Number of child folders
	 */
	public int getNumChildFolders(MegaNode parent) 
	{
	  return megaApi.getNumChildFolders(parent);
	}
	
	/**
	 * Get all children of a MegaNode
	 *
	 * If the parent node doesn't exist or it isn't a folder, this function
	 * returns NULL
	 * 
	 * @param parent Parent node
	 * @param order Order for the returned list
	 * Valid values for this parameter are:
	 * - MegaApi::ORDER_NONE = 0
	 * Undefined order
	 *
	 * - MegaApi::ORDER_DEFAULT_ASC = 1
	 * Folders first in alphabetical order, then files in the same order
	 *
	 * - MegaApi::ORDER_DEFAULT_DESC = 2
	 * Files first in reverse alphabetical order, then folders in the same order
	 *
	 * - MegaApi::ORDER_SIZE_ASC = 3
	 * Sort by size, ascending
	 *
	 * - MegaApi::ORDER_SIZE_DESC = 4
	 * Sort by size, descending
	 *
	 * - MegaApi::ORDER_CREATION_ASC = 5
	 * Sort by creation time in MEGA, ascending
	 *
	 * - MegaApi::ORDER_CREATION_DESC = 6
	 * Sort by creation time in MEGA, descending
	 *
	 * - MegaApi::ORDER_MODIFICATION_ASC = 7
	 * Sort by modification time of the original file, ascending
	 *
	 * - MegaApi::ORDER_MODIFICATION_DESC = 8
	 * Sort by modification time of the original file, descending
	 *
	 * - MegaApi::ORDER_ALPHABETICAL_ASC = 9
	 * Sort in alphabetical order, ascending
	 *
	 * - MegaApi::ORDER_ALPHABETICAL_DESC = 10
	 * Sort in alphabetical order, descending
	 *
	 * @return List with all child MegaNode objects
	 */
	public ArrayList<MegaNode> getChildren(MegaNode parent, int order)
	{
		return nodeListToArray(megaApi.getChildren(parent, order));
	}

	/**
	 * Get all children of a MegaNode
	 *
	 * If the parent node doesn't exist or it isn't a folder, this function
	 * returns NULL
	 *
	 * @param parent Parent node
	 * 
	 * @return List with all child MegaNode objects
	 */
	public ArrayList<MegaNode> getChildren(MegaNode parent)
	{
		return nodeListToArray(megaApi.getChildren(parent));
	}
	
	/**
     * Get the current index of the node in the parent folder for a specific sorting order
     *
     * If the node doesn't exist or it doesn't have a parent node (because it's a root node)
     * this function returns -1
     *
     * @param node Node to check
     * @param order Sorting order to use
     * @return Index of the node in its parent folder
     */
	public int getIndex(MegaNode node, int order){
		return megaApi.getIndex(node, order);
	}
	
	/**
     * Get the current index of the node in the parent folder
     *
     * If the node doesn't exist or it doesn't have a parent node (because it's a root node)
     * this function returns -1
     *
     * @param node Node to check
     * 
     * @return Index of the node in its parent folder
     */
	public int getIndex(MegaNode node){
		return megaApi.getIndex(node);
	}
	
	/**
     * Get the child node with the provided name
     *
     * If the node doesn't exist, this function returns NULL
     *
     * @param Parent node
     * @param Name of the node
     * @return The MegaNode that has the selected parent and name
     */
	public MegaNode getChildNode(MegaNode parent, String name)
	{
		return megaApi.getChildNode(parent, name);
	}

	/**
     * Get the parent node of a MegaNode
     *
     * If the node doesn't exist in the account or
     * it is a root node, this function returns NULL
     *
     * @param node MegaNode to get the parent
     * @return The parent of the provided node
     */
	public MegaNode getParentNode(MegaNode node)
	{
		return megaApi.getParentNode(node);
	}

	/**
     * Get the path of a MegaNode
     *
     * If the node doesn't exist, this function returns NULL.
     * You can recoved the node later unsing MegaApi::getNodeByPath
     * except if the path contains names with  '/', '\' or ':' characters.
     *
     * @param node MegaNode for which the path will be returned
     * @return The path of the node
     */
	public String getNodePath(MegaNode node)
	{
		return megaApi.getNodePath(node);
	}

	/**
     * Get the MegaNode in a specific path in the MEGA account
     *
     * The path separator character is '/'
     * The Inbox root node is //in/
     * The Rubbish root node is //bin/
     *
     * Paths with names containing '/', '\' or ':' aren't compatible
     * with this function.
     *
     * @param path Path to check
     * @param n Base node if the path is relative
     * @return The MegaNode object in the path, otherwise NULL
     */
	public MegaNode getNodeByPath(String path, MegaNode baseFolder)
	{
		return megaApi.getNodeByPath(path, baseFolder);
	}

	/**
     * Get the MegaNode in a specific path in the MEGA account
     *
     * The path separator character is '/'
     * The Inbox root node is //in/
     * The Rubbish root node is //bin/
     *
     * Paths with names containing '/', '\' or ':' aren't compatible
     * with this function.
     *
     * @param path Path to check
     * 
     * @return The MegaNode object in the path, otherwise NULL
     */
	public MegaNode getNodeByPath(String path)
	{
		return megaApi.getNodeByPath(path);
	}

	/**
     * Get the MegaNode that has a specific handle
     *
     * You can get the handle of a MegaNode using MegaNode::getHandle. The same handle
     * can be got in a Base64-encoded string using MegaNode::getBase64Handle. Conversions
     * between these formats can be done using MegaApi::base64ToHandle and MegaApi::handleToBase64
     *
     * @param MegaHandler Node handle to check
     * @return MegaNode object with the handle, otherwise NULL
     */
	public MegaNode getNodeByHandle(long handle)
	{
		return megaApi.getNodeByHandle(handle);
	}
	
	/**
     * Get all contacts of this MEGA account
     *
     * @return List of MegaUser object with all contacts of this account
     */
	public ArrayList<MegaUser> getContacts() 
	{
		return userListToArray(megaApi.getContacts());
	}

	/**
     * Get the MegaUser that has a specific email address
     *
     * You can get the email of a MegaUser using MegaUser::getEmail
     *
     * @param email Email address to check
     * @return MegaUser that has the email address, otherwise NULL
     */
	public MegaUser getContact(String email) 
	{
		return megaApi.getContact(email);
	}
	
	/**
     * Get a list with all inbound sharings from one MegaUser
     *
     * @param user MegaUser sharing folders with this account
     * @return List of MegaNode objects that this user is sharing with this account
     */
	public ArrayList<MegaNode> getInShares(MegaUser user)
	{
		return nodeListToArray(megaApi.getInShares(user));
	}

	/**
     * Get a list with all inboud sharings
     *
     * @return List of MegaNode objects that other users are sharing with this account
     */
	public ArrayList<MegaNode> getInShares()
	{
		return nodeListToArray(megaApi.getInShares());
	}
	
	/**
     * Check if a MegaNode is being shared
     *
     * For nodes that are being shared, you can get a a list of MegaShare
     * objects using MegaApi::getOutShares
     *
     * @param node Node to check
     * @return true is the MegaNode is being shared, otherwise false
     */
	public boolean isShared(MegaNode node) 
	{
		return megaApi.isShared(node);
	}
	
	/**
     * Get a list with all active outbound sharings
     *
     * @return List of MegaShare objects
     */
	public ArrayList<MegaShare> getOutShares()
	{
		return shareListToArray(megaApi.getOutShares());
	}
	
	/**
     * Get a list with the active outbound sharings for a MegaNode
     *
     * If the node doesn't exist in the account, this function returns an empty list.
     *
     * @param node MegaNode to check
     * @return List of MegaShare objects
     */
	public ArrayList<MegaShare> getOutShares(MegaNode node)
	{
		return shareListToArray(megaApi.getOutShares(node));		
	}
	
	/**
     * Get the access level of a MegaNode
     * 
     * @param node MegaNode to check
     * @return Access level of the node
     * Valid values are:
     * - MegaShare::ACCESS_OWNER
     * - MegaShare::ACCESS_FULL
     * - MegaShare::ACCESS_READWRITE
     * - MegaShare::ACCESS_READ
     * - MegaShare::ACCESS_UNKNOWN
     */
	public int getAccess(MegaNode node)
	{
		return megaApi.getAccess(node);
	}
	
	/**
     * Get the size of a node tree
     *
     * If the MegaNode is a file, this function returns the size of the file.
     * If it's a folder, this fuction returns the sum of the sizes of all nodes
     * in the node tree.
     *
     * @param node Parent node
     * @return Size of the node tree
     */
	public long getSize(MegaNode node){
		return megaApi.getSize(node);
	}	

	/**
     * Get a Base64-encoded fingerprint for a local file
     *
     * The fingerprint is created taking into account the modification time of the file
     * and file contents. This fingerprint can be used to get a corresponding node in MEGA
     * using MegaApi::getNodeByFingerprint
     *
     * If the file can't be found or can't be opened, this function returns null
     *
     * @param filePath Local file path
     * @return Base64-encoded fingerprint for the file
     */
	public String getFingerprint(String filePath) 
	{
	    return megaApi.getFingerprint(filePath);
	}

	/**
     * Get a Base64-encoded fingerprint for a node
     *          
     * If the node doesn't exist or doesn't have a fingerprint, this function returns null
     *
     * @param node Node for which we want to get the fingerprint
     * @return Base64-encoded fingerprint for the file
     */
	public String getFingerprint(MegaNode node) 
	{
		return megaApi.getFingerprint(node);
	}

	/**
     * Returns a node with the provided fingerprint
     *
     * If there isn't any node in the account with that fingerprint, this function returns null.
     *
     * @param fingerprint Fingerprint to check
     * @return MegaNode object with the provided fingerprint
     */
	public MegaNode getNodeByFingerprint(String fingerprint) 
	{
	    return megaApi.getNodeByFingerprint(fingerprint);
	}

	public MegaNode getNodeByFingerprint(String fingerprint, MegaNode preferredParent) 
	{
	    return megaApi.getNodeByFingerprint(fingerprint, preferredParent);
	}
	
	/**
     * Check if the account already has a node with the provided fingerprint
     *
     * A fingerprint for a local file can be generated using MegaApi::getFingerprint
     *
     * @param fingerprint Fingerprint to check
     * @return true if the account contains a node with the same fingerprint
     */
	public boolean hasFingerprint(String fingerprint) 
	{
		return megaApi.hasFingerprint(fingerprint);
	}
		  
	/**
     * Check if a node has an access level
     *
     * @param node Node to check
     * @param level Access level to check
     * Valid values for this parameter are:
     * - MegaShare::ACCESS_OWNER
     * - MegaShare::ACCESS_FULL
     * - MegaShare::ACCESS_READWRITE
     * - MegaShare::ACCESS_READ
     *
     * @return MegaError object with the result.
     * Valid values for the error code are:
     * - MegaError::API_OK - The node has the required access level
     * - MegaError::API_EACCESS - The node doesn't have the required access level
     * - MegaError::API_ENOENT - The node doesn't exist in the account
     * - MegaError::API_EARGS - Invalid parameters
     */
	public MegaError checkAccess(MegaNode node, int level)
	{
		return megaApi.checkAccess(node, level);
	}

	/**
     * Check if a node can be moved to a target node
     * 
     * @param node Node to check
     * @param target Target for the move operation
     * @return MegaError object with the result:
     * Valid values for the error code are:
     * - MegaError::API_OK - The node can be moved to the target
     * - MegaError::API_EACCESS - The node can't be moved because of permissions problems
     * - MegaError::API_ECIRCULAR - The node can't be moved because that would create a circular linkage
     * - MegaError::API_ENOENT - The node or the target doesn't exist in the account
     * - MegaError::API_EARGS - Invalid parameters
     */
	public MegaError checkMove(MegaNode node, MegaNode target)
	{
		return megaApi.checkMove(node, target);
	}

	/**
     * Returns the root node of the account
     *
     * If you haven't successfully called MegaApi::fetchNodes before,
     * this function returns null
     *
     * @return Root node of the account
     */
	public MegaNode getRootNode()
	{
		return megaApi.getRootNode();
	}

	/**
     * Returns the inbox node of the account
     *
     * If you haven't successfully called MegaApi::fetchNodes before,
     * this function returns null
     *
     * @return Inbox node of the account
     */
	public MegaNode getInboxNode()
	{
		return megaApi.getInboxNode();
	}

	/**
     * Returns the rubbish node of the account
     *
     * If you haven't successfully called MegaApi::fetchNodes before,
     * this function returns null
     *
     * @return Rubbish node of the account
     */
	public MegaNode getRubbishNode()
	{
		return megaApi.getRubbishNode();
	}
	
	/**
     * Search nodes containing a search string in their name
     *
     * The search is case-insensitive.
     *
     * @param node The parent node of the tree to explore
     * @param searchString Search string. The search is case-insensitive
     * @param recursive True if you want to seach recursively in the node tree.
     * False if you want to seach in the children of the node only
     *
     * @return List of nodes that contain the desired string in their name
     */
	public ArrayList<MegaNode> search(MegaNode parent, String searchString, boolean recursive)
	{
		return nodeListToArray(megaApi.search(parent, searchString, recursive));
	}
	
	/**
     * Search nodes containing a search string in their name
     *
     * The search is case-insensitive.
     *
     * @param node The parent node of the tree to explore
     * @param searchString Search string. The search is case-insensitive
     *
     * @return List of nodes that contain the desired string in their name
     */
	public ArrayList<MegaNode> search(MegaNode parent, String searchString)
	{
		return nodeListToArray(megaApi.search(parent, searchString));
	}
	
	/**
     * Process a node tree using a MegaTreeProcessor implementation
     * 
     * @param node The parent node of the tree to explore
     * @param processor MegaTreeProcessor that will receive callbacks for every node in the tree
     * @param recursive True if you want to recursively process the whole node tree.
     * False if you want to process the children of the node only
     *
     * @return True if all nodes were processed. False otherwise (the operation can be
     * cancelled by MegaTreeProcessor::processMegaNode())
     */
	public boolean processMegaTree(MegaNode parent, MegaTreeProcessorInterface processor, boolean recursive)
	{
		DelegateMegaTreeProcessor delegateListener = new DelegateMegaTreeProcessor(this, processor);
		activeMegaTreeProcessors.add(delegateListener);
		boolean result = megaApi.processMegaTree(parent, delegateListener, recursive);
		activeMegaTreeProcessors.remove(delegateListener);
		return result;
	}
	
	/**
     * Process a node tree using a MegaTreeProcessor implementation
     * 
     * @param node The parent node of the tree to explore
     * @param processor MegaTreeProcessor that will receive callbacks for every node in the tree
     *
     * @return True if all nodes were processed. False otherwise (the operation can be
     * cancelled by MegaTreeProcessor::processMegaNode())
     */
	public boolean processMegaTree(MegaNode parent, MegaTreeProcessorInterface processor)
	{
		DelegateMegaTreeProcessor delegateListener = new DelegateMegaTreeProcessor(this, processor);
		activeMegaTreeProcessors.add(delegateListener);
		boolean result = megaApi.processMegaTree(parent, delegateListener);
		activeMegaTreeProcessors.remove(delegateListener);
		return result;
	}
	
	/**
     * Get the SDK version
     *
     * @return SDK version
     */
	public String getVersion() {
		return megaApi.getVersion();
	}

	/**
     * Get the User-Agent header used by the SDK
     *
     * @return User-Agent used by the SDK
     */
	public String getUserAgent() {
		return megaApi.getUserAgent();
	}
	
	public void changeApiUrl(String apiURL, boolean disablepkp) {
		megaApi.changeApiUrl(apiURL, disablepkp);
	}
	
	public void changeApiUrl(String apiURL) {
		megaApi.changeApiUrl(apiURL);
	}

	/**
     * Make a name suitable for a file name in the local filesystem
     *
     * This function escapes (%xx) forbidden characters in the local filesystem if needed.
     * You can revert this operation using MegaApi::localToName
     *
     * @param name Name to convert
     * @return Converted name
     */
	public String nameToLocal(String name) {
		return megaApi.nameToLocal(name);
	}

	/**
     * Unescape a file name escaped with MegaApi::nameToLocal
     *
     * @param name Escaped name to convert
     * @return Converted name
     */
	public String localToName(String localName) {
		return megaApi.localToName(localName);
	}
	
	/**
     * Create a thumbnail for an image
     * 
     * @param imagePath Image path
     * @param dstPath Destination path for the thumbnail (including the file name)
     * @return True if the thumbnail was successfully created, otherwise false.
     */
	public boolean createThumbnail(String imagePath, String dstPath) {
		return megaApi.createThumbnail(imagePath, dstPath);
	}
	
	/**
     * Create a preview for an image
     * 
     * @param imagePath Image path
     * @param dstPath Destination path for the preview (including the file name)
     * @return True if the preview was successfully created, otherwise false.
     */
	public boolean createPreview(String imagePath, String dstPath) {
		return megaApi.createPreview(imagePath, dstPath);
	}
	
	/**
     * Convert a Base64 string to Base32
     *
     * If the input pointer is NULL, this function will return NULL.
     * If the input character array isn't a valid base64 string
     * the effect is undefined
     *
     * @param base64 NULL-terminated Base64 character array
     * @return NULL-terminated Base32 character array
     */
	public static String base64ToBase32(String base64) {
		return MegaApi.base64ToBase32(base64);
	}
	
	/**
     * Convert a Base32 string to Base64
     *
     * If the input pointer is NULL, this function will return NULL.
     * If the input character array isn't a valid base32 string
     * the effect is undefined
     *
     * @param base32 NULL-terminated Base32 character array
     * @return NULL-terminated Base64 character array
     */
	public static String base32ToBase64(String base32) {
		return MegaApi.base32ToBase64(base32);
	}
	
	public static void removeRecursively(String localPath) {
		MegaApi.removeRecursively(localPath);
	}
	
	/****************************************************************************************************/
	//INTERNAL METHODS
	/****************************************************************************************************/
	private MegaRequestListener createDelegateRequestListener(MegaRequestListenerInterface listener)
	{
		DelegateMegaRequestListener delegateListener = new DelegateMegaRequestListener(this, listener, true);
		activeRequestListeners.add(delegateListener);
		return delegateListener;
	}
	
	private MegaRequestListener createDelegateRequestListener(MegaRequestListenerInterface listener, boolean singleListener)
	{
		DelegateMegaRequestListener delegateListener = new DelegateMegaRequestListener(this, listener, singleListener);
		activeRequestListeners.add(delegateListener);
		return delegateListener;
	}
	
	private MegaTransferListener createDelegateTransferListener(MegaTransferListenerInterface listener)
	{
		DelegateMegaTransferListener delegateListener = new DelegateMegaTransferListener(this, listener, true);
		activeTransferListeners.add(delegateListener);
		return delegateListener;
	}
	
	private MegaTransferListener createDelegateTransferListener(MegaTransferListenerInterface listener, boolean singleListener)
	{
		DelegateMegaTransferListener delegateListener = new DelegateMegaTransferListener(this, listener, singleListener);
		activeTransferListeners.add(delegateListener);
		return delegateListener;
	}
	
	private MegaGlobalListener createDelegateGlobalListener(MegaGlobalListenerInterface listener)
	{
		DelegateMegaGlobalListener delegateListener = new DelegateMegaGlobalListener(this, listener);
		activeGlobalListeners.add(delegateListener);
		return delegateListener;
	}
	
	private MegaListener createDelegateMegaListener(MegaListenerInterface listener)
	{
		DelegateMegaListener delegateListener = new DelegateMegaListener(this, listener);
		activeMegaListeners.add(delegateListener);
		return delegateListener;
	}
	
	void privateFreeRequestListener(DelegateMegaRequestListener listener)
	{
		activeRequestListeners.remove(listener);
	}
	
	void privateFreeTransferListener(DelegateMegaTransferListener listener)
	{
		activeTransferListeners.remove(listener);
	}
	
	static ArrayList<MegaNode> nodeListToArray(MegaNodeList nodeList)
	{
		if (nodeList == null)
		{
			return null;
		}
		
		ArrayList<MegaNode> result = new ArrayList<MegaNode>(nodeList.size());
		for(int i=0; i<nodeList.size(); i++)
		{
			result.add(nodeList.get(i).copy());
		}
		
		return result;
	}
	
	static ArrayList<MegaShare> shareListToArray(MegaShareList shareList)
	{
		if (shareList == null)
		{
			return null;
		}
		
		ArrayList<MegaShare> result = new ArrayList<MegaShare>(shareList.size());
		for(int i=0; i<shareList.size(); i++)
		{
			result.add(shareList.get(i).copy());
		}
		
		return result;
	}
	
	static ArrayList<MegaTransfer> transferListToArray(MegaTransferList transferList)
	{
		if (transferList == null)
		{
			return null;
		}
		
		ArrayList<MegaTransfer> result = new ArrayList<MegaTransfer>(transferList.size());
		for(int i=0; i<transferList.size(); i++)
		{
			result.add(transferList.get(i).copy());
		}
		
		return result;
	}
	
	static ArrayList<MegaUser> userListToArray(MegaUserList userList)
	{
		
		if (userList == null)
		{
			return null;
		}
		
		ArrayList<MegaUser> result = new ArrayList<MegaUser>(userList.size());
		for(int i=0; i<userList.size(); i++)
		{
			result.add(userList.get(i).copy());
		}
		
		return result;
	}
}
