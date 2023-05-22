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
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.middlelayer.reporter.CrashReporter
import mega.privacy.android.app.middlelayer.reporter.PerformanceReporter
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.login.LoginViewModel.Companion.ACTION_FORCE_RELOAD_ACCOUNT
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.mapper.StorageStateMapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.MyAccountUpdate.Action
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.account.BroadcastMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.GetNotificationCountUseCase
import mega.privacy.android.domain.usecase.account.SetSecurityUpgradeInApp
import mega.privacy.android.domain.usecase.login.BroadcastAccountUpdateUseCase
import mega.privacy.android.domain.usecase.notifications.BroadcastHomeBadgeCountUseCase
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

/**
 * Application's Global Listener
 */
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
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val getPaymentMethod: GetPaymentMethod,
    private val getPricing: GetPricing,
    private val getNumberOfSubscription: GetNumberOfSubscription,
    private val setSecurityUpgradeInApp: SetSecurityUpgradeInApp,
    private val broadcastAccountUpdateUseCase: BroadcastAccountUpdateUseCase,
    private val broadcastMyAccountUpdateUseCase: BroadcastMyAccountUpdateUseCase,
    private val getNotificationCountUseCase: GetNotificationCountUseCase,
    private val broadcastHomeBadgeCountUseCase: BroadcastHomeBadgeCountUseCase,
) : MegaGlobalListenerInterface {

    /**
     * onUsersUpdate
     */
    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {
        users?.toList()?.forEach { user ->
            val myUserHandle = api.myUserHandle
            val isMyChange =
                myUserHandle != null && myUserHandle == MegaApiJava.userHandleToBase64(user.handle)
            if (user.changes == 0 && !isMyChange) {
                LiveEventBus.get(EVENT_USER_VISIBILITY_CHANGE, Long::class.java)
                    .post(user.handle)
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_PUSH_SETTINGS) && user.isOwnChange == 0) {
                pushNotificationSettingManagement.updateMegaPushNotificationSetting()
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER) && isMyChange) {
                api.getMyChatFilesFolder(GetAttrUserListener(appContext, true))
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

    /**
     * onUserAlertsUpdate
     */
    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {
        megaChatNotificationHandler.updateAppBadge()
        notifyNotificationCountChange()
    }

    private fun notifyNotificationCountChange() = applicationScope.launch {
        val notificationCount = runCatching { getNotificationCountUseCase(false) }
            .getOrNull() ?: 0

        broadcastHomeBadgeCountUseCase(notificationCount)
    }

    /**
     * onNodesUpdate
     */
    override fun onNodesUpdate(api: MegaApiJava, nodeList: ArrayList<MegaNode>?) {
        nodeList?.toList()?.forEach { node ->
            if (node.isInShare && node.hasChanged(MegaNode.CHANGE_TYPE_INSHARE)) {
                showSharedFolderNotification(node)
            } else if (node.hasChanged(MegaNode.CHANGE_TYPE_PUBLIC_LINK) && node.publicLink != null) {
                // when activated share, will show rating if it matches the condition
                RatingHandlerImpl(appContext).showRatingBaseOnSharing()
            }
        }
    }

    /**
     * onReloadNeeded
     */
    override fun onReloadNeeded(api: MegaApiJava) {}

    /**
     * onAccountUpdate
     */
    override fun onAccountUpdate(api: MegaApiJava) {
        Timber.d("onAccountUpdate")

        applicationScope.launch {
            runCatching { broadcastAccountUpdateUseCase() }.onFailure { Timber.e(it) }
            runCatching { getPaymentMethod(true) }.onFailure { Timber.e(it) }
            runCatching { getPricing(true) }.onFailure { Timber.e(it) }
            runCatching { dbH.resetExtendedAccountDetailsTimestamp() }.onFailure { Timber.e(it) }
            runCatching { getAccountDetailsUseCase(forceRefresh = true) }.onFailure { Timber.e(it) }
            runCatching { getNumberOfSubscription(true) }.onFailure { Timber.e(it) }
        }
    }

    /**
     * onContactRequestsUpdate
     */
    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?,
    ) {
        if (requests == null) return
        megaChatNotificationHandler.updateAppBadge()
        notifyNotificationCountChange()

        requests.toList().forEach { cr ->
            if (cr.status == MegaContactRequest.STATUS_UNRESOLVED && !cr.isOutgoing) {
                val notificationBuilder: ContactsAdvancedNotificationBuilder =
                    ContactsAdvancedNotificationBuilder.newInstance(
                        appContext,
                        megaApi
                    )
                notificationBuilder.removeAllIncomingContactNotifications()
                notificationBuilder.showIncomingContactRequestNotification()
                Timber.d(
                    "IPC: %s cr.isOutgoing: %s cr.getStatus: %d",
                    cr.sourceEmail,
                    cr.isOutgoing,
                    cr.status
                )
            } else if (cr.status == MegaContactRequest.STATUS_ACCEPTED && cr.isOutgoing) {
                val notificationBuilder: ContactsAdvancedNotificationBuilder =
                    ContactsAdvancedNotificationBuilder.newInstance(
                        appContext,
                        megaApi
                    )
                notificationBuilder.showAcceptanceContactRequestNotification(cr.targetEmail)
                Timber.d(
                    "ACCEPT OPR: %s cr.isOutgoing: %s cr.getStatus: %d",
                    cr.sourceEmail,
                    cr.isOutgoing,
                    cr.status
                )
                RatingHandlerImpl(appContext).showRatingBaseOnContacts()
            }
            if (cr.status == MegaContactRequest.STATUS_ACCEPTED) {
                LiveEventBus.get(EVENT_USER_VISIBILITY_CHANGE, Long::class.java).post(cr.handle)
            }
        }
    }

    /**
     * onEvent
     */
    override fun onEvent(api: MegaApiJava, event: MegaEvent?) {
        if (event == null) return

        Timber.d("Event received: text(${event.text}), type(${event.type}), number(${event.number})")

        when (event.type) {
            MegaEvent.EVENT_STORAGE -> {
                val state = storageStateMapper(event.number.toInt())
                Timber.d("EVENT_STORAGE: $state")
                when (state) {
                    StorageState.Change -> refreshAccountDetail()
                    StorageState.PayWall -> showOverDiskQuotaPaywallWarning()

                    else -> {
                        val intent =
                            Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS).apply {
                                action = Constants.ACTION_STORAGE_STATE_CHANGED
                                putExtra(Constants.EXTRA_STORAGE_STATE, state)
                            }
                        appContext.sendBroadcast(intent)

                        sendMyAccountUpdateBroadcast(Action.STORAGE_STATE_CHANGED, state)
                    }
                }
            }

            MegaEvent.EVENT_ACCOUNT_BLOCKED -> {
                Timber.d("EVENT_ACCOUNT_BLOCKED: %s", event.number)
                appContext.sendBroadcast(
                    Intent(BroadcastConstants.BROADCAST_ACTION_INTENT_EVENT_ACCOUNT_BLOCKED)
                        .putExtra(BroadcastConstants.EVENT_NUMBER, event.number)
                        .putExtra(BroadcastConstants.EVENT_TEXT, event.text)
                )
            }

            MegaEvent.EVENT_BUSINESS_STATUS -> sendBroadcastUpdateAccountDetails()
            MegaEvent.EVENT_MISC_FLAGS_READY -> checkEnabledCookies()
            MegaEvent.EVENT_RELOADING -> showLoginFetchingNodes()
            MegaEvent.EVENT_UPGRADE_SECURITY -> applicationScope.launch {
                setSecurityUpgradeInApp(true)
            }
        }
    }

    /**
     * onSetsUpdate
     */
    override fun onSetsUpdate(api: MegaApiJava, sets: ArrayList<MegaSet>?) {
        Timber.d("Sets Updated")
    }

    /**
     * onSetElementsUpdate
     */
    override fun onSetElementsUpdate(
        api: MegaApiJava,
        elements: ArrayList<MegaSetElement>?,
    ) {
        Timber.d("Set elements updated")
    }

    private fun sendBroadcastUpdateAccountDetails() {
        appContext.sendBroadcast(
            Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
                .putExtra(BroadcastConstants.ACTION_TYPE, Constants.UPDATE_ACCOUNT_DETAILS)
        )

        sendMyAccountUpdateBroadcast(Action.UPDATE_ACCOUNT_DETAILS, null)
    }

    /**
     * Send broadcast to App Event
     */
    private fun sendMyAccountUpdateBroadcast(action: Action, storageState: StorageState?) =
        applicationScope.launch {
            val data = MyAccountUpdate(
                action = action,
                storageState = storageState
            )
            broadcastMyAccountUpdateUseCase(data)
        }

    private fun showSharedFolderNotification(n: MegaNode) {
        Timber.d("showSharedFolderNotification")
        try {
            val sharesIncoming = megaApi.inSharesList ?: return

            val notificationTitle = appContext.getString(
                if (n.hasChanged(MegaNode.CHANGE_TYPE_NEW))
                    R.string.title_incoming_folder_notification
                else R.string.context_permissions_changed
            )

            val userName = sharesIncoming.firstOrNull { it.nodeHandle == n.handle }?.let {
                val user = megaApi.getContact(it.user)
                ContactUtil.getMegaUserNameDB(user) ?: ""
            } ?: ""

            val folderName = if (n.isNodeKeyDecrypted) n.name else notificationTitle

            val source =
                "<b>$folderName</b> " +
                        appContext.getString(R.string.incoming_folder_notification) +
                        " " +
                        Util.toCDATA(userName)
            val notificationContent = HtmlCompat.fromHtml(source, HtmlCompat.FROM_HTML_MODE_LEGACY)
            val notificationChannelId = Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_ID
            val intent: Intent = Intent(appContext, ManagerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setAction(Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION)
            val pendingIntent = PendingIntent.getActivity(
                appContext, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val notificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    notificationChannelId,
                    Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
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
            notificationManager.notify(
                Constants.NOTIFICATION_PUSH_CLOUD_DRIVE,
                notificationBuilder.build()
            )
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
            .subscribe(
                { cookies: Set<CookieType> ->
                    val analyticsCookiesEnabled = cookies.contains(CookieType.ANALYTICS)
                    crashReporter.setEnabled(analyticsCookiesEnabled)
                    performanceReporter.setEnabled(analyticsCookiesEnabled)
                },
                { throwable: Throwable -> Timber.e(throwable) }
            )
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
            runCatching {
                getAccountDetailsUseCase(forceRefresh = true)
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}
