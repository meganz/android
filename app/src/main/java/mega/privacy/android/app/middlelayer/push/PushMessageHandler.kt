package mega.privacy.android.app.middlelayer.push

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.app.fcm.IncomingCallService
import mega.privacy.android.app.fcm.KeepAliveService
import mega.privacy.android.app.middlelayer.BuildFlavorHelper.isGMS
import mega.privacy.android.app.utils.ChatUtil
import nz.mega.sdk.*
import nz.mega.sdk.MegaRequest.*
import timber.log.Timber

class PushMessageHandler(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    @MegaApiFolder private val megaApiFolder: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val dbH: DatabaseHandler
) : MegaRequestListenerInterface {

    private var showMessageNotificationAfterPush = false
    private var beep = false

    /**
     * Awake CPU to make sure the following operations can finish.
     *
     * @param launchService Whether launch a foreground service to keep the app alive.
     */
    private fun awakeCpu(launchService: Boolean) {
        Timber.d("wake lock acquire")
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).apply {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:push_message").apply {
                setReferenceCounted(false)
                acquire(AWAKE_CPU_FOR.toLong())
            }

            if (launchService) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                        && (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isBackgroundRestricted
                    ) {
                        // This means the user has enforced background restrictions for the app.
                        // Returning here avoids ForegroundServiceStartNotAllowedException.
                        return
                    }

                    context.startForegroundService(Intent(context, KeepAliveService::class.java))
                } else {
                    context.startService(Intent(context, KeepAliveService::class.java))
                }
            }
        }
    }

    fun handleMessage(message: Message) {
        val messageType = message.type
        Timber.d(
            "Handle message from: ${message.from} , which type is: $messageType. " +
                    "Original priority is ${message.originalPriority}, priority is ${message.priority}"
        )

        if (!message.hasData()) {
            return
        }

        // Check if message contains a data payload.
        Timber.d("Message data payload: ${message.data}")

        val credentials = dbH.credentials

        if (credentials == null) {
            Timber.e("No user credentials, process terminates!")
            return
        }

        when (messageType) {
            TYPE_SHARE_FOLDER, TYPE_CONTACT_REQUEST, TYPE_ACCEPTANCE -> {
                //Leave the flag showMessageNotificationAfterPush as it is
                //If true - wait until connection finish
                //If false, no need to change it
                Timber.d("Flag showMessageNotificationAfterPush: $showMessageNotificationAfterPush")

                val gSession = credentials.session

                if (megaApi.rootNode == null) {
                    Timber.w("RootNode = null")
                    performLoginProcess(gSession)
                } else {
                    Timber.d("Awaiting info on listener")
                    retryPendingConnections()
                }
            }
            TYPE_CALL -> {
                //Leave the flag showMessageNotificationAfterPush as it is
                //If true - wait until connection finish
                //If false, no need to change it
                Timber.d("Flag showMessageNotificationAfterPush: $showMessageNotificationAfterPush")

                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIdle = pm.isDeviceIdleMode

                if (!MegaApplication.getInstance().isActivityVisible && megaApi.rootNode == null || isIdle) {
                    Timber.d("Launch foreground service!")
                    awakeCpu(false)

                    if (isGMS()) {
                        context.startService(Intent(context, IncomingCallService::class.java))
                        return
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // For HMS build flavor, have to startForegroundService.
                        // Android doesn't allow the app to launch background service if the app is not launched by FCM high priority push message.
                        context.startForegroundService(
                            Intent(
                                context,
                                IncomingCallService::class.java
                            )
                        )
                        return
                    }
                }

                val gSession = credentials.session

                if (megaApi.rootNode == null) {
                    Timber.w("RootNode = null")
                    performLoginProcess(gSession)
                } else {
                    Timber.d("RootNode is NOT null - wait CALLDATA:onChatCallUpdate")
                    val ret = megaChatApi.initState
                    Timber.d("result of init ---> $ret")
                    val status = megaChatApi.onlineStatus
                    Timber.d("online status ---> $status")
                    val connectionState = megaChatApi.connectionState
                    Timber.d("connection state ---> $connectionState")
                    retryPendingConnections()
                }
            }
            TYPE_CHAT -> {
                Timber.d("CHAT notification")

                if (MegaApplication.getInstance().isActivityVisible) {
                    Timber.d("App on foreground --> return")
                    retryPendingConnections()
                    return
                }

                beep = Message.NO_BEEP != message.silent
                awakeCpu(beep)
                Timber.d("Notification should beep: $beep")
                showMessageNotificationAfterPush = true
                val gSession = credentials.session

                if (megaApi.rootNode == null) {
                    Timber.w("RootNode = null")
                    performLoginProcess(gSession)
                } else {
                    //Leave the flag showMessageNotificationAfterPush as it is
                    //If true - wait until connection finish
                    //If false, no need to change it
                    Timber.d("Flag showMessageNotificationAfterPush: $showMessageNotificationAfterPush. Call to pushReceived")
                    megaChatApi.pushReceived(beep)
                    beep = false
                }
            }
        }
    }

    /**
     * If the account hasn't logged in, login first.
     *
     * @param gSession Cached session, used to do a fast login.
     */
    private fun performLoginProcess(gSession: String) {
        if (!MegaApplication.isLoggingIn()) {
            /* Two locks and synchronized block prevent background login executes after login process is launched in `LoginFragment`.
                Otherwise the login process in foreground will failed with `-11` and cause logout.*/
            if (allowBackgroundLogin) {
                synchronized(MegaApplication.getInstance()) {
                    if (allowBackgroundLogin) {
                        megaApi.fastLogin(gSession, this)
                    }
                }
            }
            ChatUtil.initMegaChatApi(gSession)
        }
    }

    private fun retryPendingConnections() {
        Timber.d("retryPendingConnections")

        try {
            megaApi.retryPendingConnections()
            megaChatApi.retryPendingConnections(false, null)
        } catch (e: Exception) {
            Timber.e("Exception", e)
        }
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: ${request.requestString}")
        // Avoid duplicate login.
        allowBackgroundLogin = false
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestUpdate: ${request.requestString}")
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish: ${request.requestString}")

        when (request.type) {
            TYPE_LOGIN -> {
                allowBackgroundLogin = true

                if (e.errorCode == MegaError.API_OK) {
                    Timber.d("Fast login OK. Logged in. Setting account auth token for folder links.")
                    megaApiFolder.accountAuth = megaApi.accountAuth
                    Timber.d("Calling fetchNodes from MegaFireBaseMessagingService")
                    megaApi.fetchNodes(this)

                    // Get cookies settings after login.
                    MegaApplication.getInstance().checkEnabledCookies()
                } else {
                    Timber.e("ERROR: ${e.errorString}")
                }
            }
            TYPE_FETCH_NODES -> {
                if (e.errorCode == MegaError.API_OK) {
                    if (showMessageNotificationAfterPush) {
                        showMessageNotificationAfterPush = false
                        Timber.d("Call to pushReceived")
                        megaChatApi.pushReceived(beep)
                        beep = false
                    } else {
                        Timber.d("Login do not started by CHAT message")
                    }
                } else {
                    Timber.d("${request.requestString} failed. Error code: ${e.errorCode}, error string: ${e.errorString}")
                }
            }
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.w("onRequestTemporary: ${request.requestString}")
    }

    /**
     * Generic push message object, used to unify corresponding platform dependent purchase object.
     */
    class Message(
        /**
         * Where is the message from. May be null. Just for log purpose.
         */
        val from: String,
        /**
         * Just for log and debug purpose.
         */
        val originalPriority: Int,
        /**
         * Just for log and debug purpose.
         */
        val priority: Int,
        /**
         * Store couples of info sent from server,
         * but the client only use: silent, type, email.
         */
        val data: Map<String, String>?
    ) {

        /**
         * Check if the data map has info.
         *
         * @return true, has; false, doesn't have.
         */
        fun hasData(): Boolean = data != null && data.isNotEmpty()

        /**
         * Get the message type.
         *
         * @return Message type, or null if data map is empty.
         */
        val type: String?
            get() {
                if (hasData()) {
                    return data!![KEY_TYPE]
                }
                Timber.w("Message type is null!")
                return null
            }

        /**
         * Get the email.
         *
         * @return Email, or null if data map is empty.
         */
        val email: String?
            get() {
                if (hasData()) {
                    return data!![KEY_EMAIL]
                }
                Timber.w("Message email is null!")
                return null
            }

        /**
         * Get if the push message should be silent.
         *
         * @return Message type, or null if data map is empty.
         */
        val silent: String?
            get() {
                if (hasData()) {
                    return data!![KEY_SILENT]
                }
                Timber.w("Message silent is null!")
                return null
            }

        override fun toString(): String {
            return "Message{" +
                    "from='" + from + '\'' +
                    ", data=" + data +
                    ", originalPriority=" + originalPriority +
                    ", priority=" + priority +
                    '}'
        }

        companion object {
            private const val KEY_SILENT = "silent"
            private const val KEY_TYPE = "type"
            private const val KEY_EMAIL = "email"
            const val NO_BEEP = "1"
        }
    }

    companion object {
        private const val AWAKE_CPU_FOR = 60 * 1000
        private const val TYPE_SHARE_FOLDER = "1"
        private const val TYPE_CONTACT_REQUEST = "3"
        private const val TYPE_ACCEPTANCE = "5"
        private const val TYPE_CALL = "4"
        private const val TYPE_CHAT = "2"
        const val PUSH_TOKEN = "PUSH_TOKEN"

        /**
         * Flag for controlling if allows the app to do login in background upon receiving a push message.
         */
        @JvmField
        @Volatile
        var allowBackgroundLogin = true
    }
}