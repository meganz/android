package mega.privacy.android.app.listeners

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.components.PushNotificationSettingManagement
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_MEETING_AVATAR_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_USER_VISIBILITY_CHANGE
import mega.privacy.android.app.fcm.ContactsAdvancedNotificationBuilder
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.GetCookieSettingsUseCase
import mega.privacy.android.app.globalmanagement.MegaChatNotificationHandler
import mega.privacy.android.app.main.LoginActivity
import mega.privacy.android.app.main.LoginActivity.Companion.ACTION_FORCE_RELOAD_ACCOUNT
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.middlelayer.reporter.CrashReporter
import mega.privacy.android.app.middlelayer.reporter.PerformanceReporter
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.mapper.StorageStateMapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetAccountDetails
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.GetPricing
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import javax.inject.Inject

class GlobalListener @Inject constructor(
    private val dbH: DatabaseHandler,
    private val megaChatNotificationHandler: MegaChatNotificationHandler,
    private val pushNotificationSettingManagement: PushNotificationSettingManagement,
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val crashReporter: CrashReporter,
    private val performanceReporter: PerformanceReporter,
    @ApplicationContext private val appContext: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val storageStateMapper: StorageStateMapper,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getAccountDetails: GetAccountDetails,
    private val getPaymentMethod: GetPaymentMethod,
    private val getPricing: GetPricing,
    private val getNumberOfSubscription: GetNumberOfSubscription,
) : MegaGlobalListenerInterface {

    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser?>?) {

        users?.filterNotNull()?.forEach { user ->
            val myUserHandle = api.myUserHandle
            val isMyChange =
                myUserHandle != null && myUserHandle == MegaApiJava.userHandleToBase64(user.handle)
            if (user.changes == 0 && !isMyChange) {
                LiveEventBus.get(EVENT_USER_VISIBILITY_CHANGE, Long::class.java)
                    .post(user.handle)
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_PUSH_SETTINGS) && isMyChange) {
                pushNotificationSettingManagement.updateMegaPushNotificationSetting()
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER) && isMyChange) {
                api.getMyChatFilesFolder(GetAttrUserListener(appContext, true))
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_CAMERA_UPLOADS_FOLDER) && isMyChange) {
                //user has change CU attribute, need to update local ones
                Timber.d("Get CameraUpload attribute when change on other client.")
                api.getUserAttribute(MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER,
                    GetCameraUploadAttributeListener(appContext))
                return@forEach
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_RICH_PREVIEWS) && isMyChange) {
                api.shouldShowRichLinkWarning(GetAttrUserListener(appContext))
                api.isRichPreviewsEnabled(GetAttrUserListener(appContext))
                return@forEach
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_RUBBISH_TIME) && isMyChange) {
                api.getRubbishBinAutopurgePeriod(GetAttrUserListener(appContext))
                return@forEach
            }

            // Receive the avatar change, send the event
            if (user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR) && user.isOwnChange == 0) {
                LiveEventBus.get(EVENT_MEETING_AVATAR_CHANGE, Long::class.java)
                    .post(user.handle)
            }
        }
    }

    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert?>?) {
        megaChatNotificationHandler.updateAppBadge()
        notifyNotificationCountChange(api)
    }

    private fun notifyNotificationCountChange(api: MegaApiJava) {
        val incomingContactRequests = api.incomingContactRequests
        LiveEventBus.get(Constants.EVENT_NOTIFICATION_COUNT_CHANGE, Int::class.java).post(
            api.numUnreadUserAlerts + (incomingContactRequests?.size ?: 0))
    }

    override fun onNodesUpdate(api: MegaApiJava, nodeList: ArrayList<MegaNode?>?) {
        nodeList?.filterNotNull()?.forEach { node ->
            if (node.isInShare && node.hasChanged(MegaNode.CHANGE_TYPE_INSHARE)) {
                showSharedFolderNotification(node)
            } else if (node.hasChanged(MegaNode.CHANGE_TYPE_PUBLIC_LINK) && node.publicLink != null) {
                // when activated share, will show rating if it matches the condition
                RatingHandlerImpl(appContext).showRatingBaseOnSharing()
            }
        }
    }

    override fun onReloadNeeded(api: MegaApiJava) {}

    override fun onAccountUpdate(api: MegaApiJava) {
        Timber.d("onAccountUpdate")
        val intent = Intent(BroadcastConstants.BROADCAST_ACTION_INTENT_ON_ACCOUNT_UPDATE).apply {
            action = BroadcastConstants.ACTION_ON_ACCOUNT_UPDATE
        }
        appContext.sendBroadcast(intent)
        applicationScope.launch {
            getPaymentMethod(true)
            getPricing(true)
            dbH.resetExtendedAccountDetailsTimestamp()
            getAccountDetails(forceRefresh = true)
            getNumberOfSubscription(true)
        }
    }

    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest?>?,
    ) {
        if (requests == null) return
        megaChatNotificationHandler.updateAppBadge()
        notifyNotificationCountChange(api)

        requests.filterNotNull().forEach { cr ->
            if (cr.status == MegaContactRequest.STATUS_UNRESOLVED && !cr.isOutgoing) {
                val notificationBuilder: ContactsAdvancedNotificationBuilder =
                    ContactsAdvancedNotificationBuilder.newInstance(
                        appContext,
                        megaApi)
                notificationBuilder.removeAllIncomingContactNotifications()
                notificationBuilder.showIncomingContactRequestNotification()
                Timber.d("IPC: %s cr.isOutgoing: %s cr.getStatus: %d",
                    cr.sourceEmail,
                    cr.isOutgoing,
                    cr.status)
            } else if (cr.status == MegaContactRequest.STATUS_ACCEPTED && cr.isOutgoing) {
                val notificationBuilder: ContactsAdvancedNotificationBuilder =
                    ContactsAdvancedNotificationBuilder.newInstance(
                        appContext,
                        megaApi)
                notificationBuilder.showAcceptanceContactRequestNotification(cr.targetEmail)
                Timber.d("ACCEPT OPR: %s cr.isOutgoing: %s cr.getStatus: %d",
                    cr.sourceEmail,
                    cr.isOutgoing,
                    cr.status)
                RatingHandlerImpl(appContext).showRatingBaseOnContacts()
            }
            if (cr.status == MegaContactRequest.STATUS_ACCEPTED) {
                LiveEventBus.get(EVENT_USER_VISIBILITY_CHANGE, Long::class.java).post(cr.handle)
            }
        }
    }

    override fun onEvent(api: MegaApiJava, event: MegaEvent) {
        Timber.d("Event received: text(${event.text}), type(${event.type}), number(${event.number})")

        when (event.type) {
            MegaEvent.EVENT_STORAGE -> {
                val state = storageStateMapper(event.number.toInt())
                Timber.d("EVENT_STORAGE: $state")
                when (state) {
                    StorageState.Change -> {
                        refreshAccountDetail()
                    }
                    StorageState.PayWall -> {
                        showOverDiskQuotaPaywallWarning()
                    }
                    else -> {
                        val intent =
                            Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS).apply {
                                action = Constants.ACTION_STORAGE_STATE_CHANGED
                                putExtra(Constants.EXTRA_STORAGE_STATE, state)
                            }
                        appContext.sendBroadcast(intent)
                    }
                }
            }
            MegaEvent.EVENT_ACCOUNT_BLOCKED -> {
                Timber.d("EVENT_ACCOUNT_BLOCKED: %s", event.number)
                appContext.sendBroadcast(Intent(BroadcastConstants.BROADCAST_ACTION_INTENT_EVENT_ACCOUNT_BLOCKED)
                    .putExtra(BroadcastConstants.EVENT_NUMBER, event.number)
                    .putExtra(BroadcastConstants.EVENT_TEXT, event.text))
            }
            MegaEvent.EVENT_BUSINESS_STATUS -> sendBroadcastUpdateAccountDetails()
            MegaEvent.EVENT_MISC_FLAGS_READY -> checkEnabledCookies()
            MegaEvent.EVENT_RELOADING -> showLoginFetchingNodes()
        }
    }

    override fun onSetsUpdate(api: MegaApiJava?, sets: ArrayList<MegaSet>?) {
        Timber.d("Sets Updated")
    }

    override fun onSetElementsUpdate(
        api: MegaApiJava?,
        elements: ArrayList<MegaSetElement>?,
    ) {
        Timber.d("Set elements updated")
    }

    private fun sendBroadcastUpdateAccountDetails() {
        appContext.sendBroadcast(Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
            .putExtra(BroadcastConstants.ACTION_TYPE, Constants.UPDATE_ACCOUNT_DETAILS))
    }

    private fun showSharedFolderNotification(n: MegaNode) {
        Timber.d("showSharedFolderNotification")
        try {
            val sharesIncoming = megaApi.inSharesList ?: return
            var name: String? = ""

            for (mS in sharesIncoming) {
                if (mS.nodeHandle == n.handle) {
                    val user = megaApi.getContact(mS.user)
                    name = ContactUtil.getMegaUserNameDB(user) ?: ""
                }
            }
            val source =
                "<b>" + n.name + "</b> " + appContext.getString(R.string.incoming_folder_notification) + " " + Util.toCDATA(
                    name)
            val notificationContent = HtmlCompat.fromHtml(source, HtmlCompat.FROM_HTML_MODE_LEGACY)
            val notificationChannelId = Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_ID
            val intent: Intent = Intent(appContext, ManagerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setAction(Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION)
            val pendingIntent = PendingIntent.getActivity(appContext, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
            val notificationTitle: String =
                appContext.getString(if (n.hasChanged(MegaNode.CHANGE_TYPE_NEW)) R.string.title_incoming_folder_notification else R.string.context_permissions_changed)
            val notificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(notificationChannelId,
                    Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME,
                    NotificationManager.IMPORTANCE_HIGH)
                channel.setShowBadge(true)
                notificationManager.createNotificationChannel(channel)
            }
            val d: Drawable = appContext.resources
                .getDrawable(R.drawable.ic_folder_incoming, appContext.theme)
            val notificationBuilder: NotificationCompat.Builder =
                NotificationCompat.Builder(appContext, notificationChannelId)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationContent)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent))
                    .setAutoCancel(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentIntent(pendingIntent)
                    .setColor(ContextCompat.getColor(appContext, R.color.red_600_red_300))
                    .setLargeIcon((d as BitmapDrawable).bitmap)
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
            notificationManager.notify(Constants.NOTIFICATION_PUSH_CLOUD_DRIVE,
                notificationBuilder.build())
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * Check current enabled cookies and set the corresponding flags to true/false
     */
    private fun checkEnabledCookies() {
        getCookieSettingsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { cookies: Set<CookieType?>?, throwable: Throwable? ->
                if (throwable == null) {
                    cookies?.let {
                        val analyticsCookiesEnabled = cookies.contains(CookieType.ANALYTICS)
                        crashReporter.setEnabled(analyticsCookiesEnabled)
                        performanceReporter.setEnabled(analyticsCookiesEnabled)
                    }
                }
            }
    }

    /**
     * A force reload account has been received. A fetch nodes is in progress and the
     * Login screen should be shown.
     */
    private fun showLoginFetchingNodes() {
        appContext.startActivity(Intent(appContext, LoginActivity::class.java).apply {
            putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
            action = ACTION_FORCE_RELOAD_ACCOUNT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun refreshAccountDetail() {
        applicationScope.launch {
            getAccountDetails(forceRefresh = true)
        }
    }
}