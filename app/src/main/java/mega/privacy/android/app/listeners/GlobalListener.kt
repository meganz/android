package mega.privacy.android.app.listeners

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.RingtoneManager
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.google.android.gms.ads.MobileAds
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.fcm.ContactsAdvancedNotificationBuilder
import mega.privacy.android.app.main.ManagerActivity
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
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.account.BroadcastMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.GetNotificationCountUseCase
import mega.privacy.android.domain.usecase.account.GetUserDataUseCase
import mega.privacy.android.domain.usecase.account.SetSecurityUpgradeInAppUseCase
import mega.privacy.android.domain.usecase.billing.GetPaymentMethodUseCase
import mega.privacy.android.domain.usecase.chat.UpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.chat.link.IsRichPreviewsEnabledUseCase
import mega.privacy.android.domain.usecase.chat.link.ShouldShowRichLinkWarningUseCase
import mega.privacy.android.domain.usecase.contact.GetIncomingContactRequestsNotificationListUseCase
import mega.privacy.android.domain.usecase.domainmigration.UpdateDomainNameUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.notifications.BroadcastHomeBadgeCountUseCase
import mega.privacy.android.domain.usecase.pdf.CheckIfShouldDeleteLastPageViewedInPdfUseCase
import mega.privacy.android.domain.usecase.setting.BroadcastMiscLoadedUseCase
import mega.privacy.android.icon.pack.R as IconPackR
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
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Application's Global Listener
 */
@OptIn(FlowPreview::class)
class GlobalListener @Inject constructor(
    private val dbH: Lazy<DatabaseHandler>,
    @ApplicationContext private val appContext: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val storageStateMapper: StorageStateMapper,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val getPaymentMethodUseCase: GetPaymentMethodUseCase,
    private val getPricing: GetPricing,
    private val getNumberOfSubscription: GetNumberOfSubscription,
    private val setSecurityUpgradeInAppUseCase: SetSecurityUpgradeInAppUseCase,
    private val broadcastMyAccountUpdateUseCase: BroadcastMyAccountUpdateUseCase,
    private val getNotificationCountUseCase: GetNotificationCountUseCase,
    private val broadcastHomeBadgeCountUseCase: BroadcastHomeBadgeCountUseCase,
    private val getIncomingContactRequestsNotificationListUseCase: GetIncomingContactRequestsNotificationListUseCase,
    private val updatePushNotificationSettingsUseCase: UpdatePushNotificationSettingsUseCase,
    private val shouldShowRichLinkWarningUseCase: ShouldShowRichLinkWarningUseCase,
    private val isRichPreviewsEnabledUseCase: IsRichPreviewsEnabledUseCase,
    private val broadcastMiscLoadedUseCase: BroadcastMiscLoadedUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val checkIfShouldDeleteLastPageViewedInPdfUseCase: CheckIfShouldDeleteLastPageViewedInPdfUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val updateDomainNameUseCase: UpdateDomainNameUseCase,
) : MegaGlobalListenerInterface {
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val globalSyncUpdates = MutableSharedFlow<Unit>()

    init {
        applicationScope.launch {
            globalSyncUpdates.debounce(1000)
                .catch { Timber.e(it) }
                .collect {
                    runCatching {
                        val notifications = getNotificationCountUseCase(
                            withChatNotifications = false,
                        )
                        broadcastHomeBadgeCountUseCase(notifications)
                    }.getOrElse { Timber.e(it) }
                }
        }
    }

    /**
     * onUsersUpdate
     */
    override fun onUsersUpdate(api: MegaApiJava, users: ArrayList<MegaUser>?) {
        users?.toList()?.forEach { user ->
            val myUserHandle = api.myUserHandle
            val isMyChange =
                myUserHandle != null && myUserHandle == MegaApiJava.userHandleToBase64(user.handle)
            if (user.hasChanged(MegaUser.CHANGE_TYPE_PUSH_SETTINGS.toLong()) && user.isOwnChange == 0) {
                applicationScope.launch {
                    runCatching {
                        updatePushNotificationSettingsUseCase()
                    }.onFailure {
                        Timber.e(it)
                    }
                }
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER.toLong()) && isMyChange) {
                api.getMyChatFilesFolder(GetAttrUserListener(appContext, true))
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_RICH_PREVIEWS.toLong()) && isMyChange) {
                applicationScope.launch {
                    runCatching {
                        shouldShowRichLinkWarningUseCase()
                        isRichPreviewsEnabledUseCase()
                    }.onFailure {
                        Timber.e(it, "Error checking rich link settings")
                    }
                }
                return@forEach
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_RUBBISH_TIME.toLong()) && isMyChange) {
                api.getRubbishBinAutopurgePeriod(GetAttrUserListener(appContext))
                return@forEach
            }
        }
    }

    /**
     * onUserAlertsUpdate
     */
    override fun onUserAlertsUpdate(api: MegaApiJava, userAlerts: ArrayList<MegaUserAlert>?) {
        notifyNotificationCountChange()
    }

    private fun notifyNotificationCountChange() = applicationScope.launch {
        val notificationCount = runCatching {
            getNotificationCountUseCase(
                withChatNotifications = false,
            )
        }
            .getOrNull() ?: 0

        broadcastHomeBadgeCountUseCase(notificationCount)
    }

    /**
     * onNodesUpdate
     */
    override fun onNodesUpdate(api: MegaApiJava, nodeList: ArrayList<MegaNode>?) {
        nodeList?.toList()?.forEach { node ->
            when {
                node.isInShare && node.hasChanged(MegaNode.CHANGE_TYPE_INSHARE.toLong()) -> {
                    showSharedFolderNotification(node)
                }

                node.hasChanged(MegaNode.CHANGE_TYPE_PUBLIC_LINK.toLong()) && node.publicLink != null -> {
                    // when activated share, will show rating if it matches the condition
                    RatingHandlerImpl(appContext).showRatingBaseOnSharing()
                }

                node.hasChanged(MegaNode.CHANGE_TYPE_REMOVED.toLong()) && node.isFile -> {
                    applicationScope.launch {
                        runCatching {
                            checkIfShouldDeleteLastPageViewedInPdfUseCase(
                                nodeHandle = node.handle,
                                fileName = node.name,
                                isOfflineRemoval = false,
                            )
                        }.onFailure { Timber.e(it) }
                    }
                }
            }
        }
    }

    /**
     * onAccountUpdate
     */
    override fun onAccountUpdate(api: MegaApiJava) {
        Timber.d("onAccountUpdate")

        applicationScope.launch {
            runCatching { getUserDataUseCase() }.onFailure { Timber.e(it) }
            runCatching { getPaymentMethodUseCase(true) }.onFailure { Timber.e(it) }
            runCatching { getPricing(true) }.onFailure { Timber.e(it) }
            runCatching {
                dbH.get().resetExtendedAccountDetailsTimestamp()
            }.onFailure { Timber.e(it) }
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
        notifyNotificationCountChange()

        requests.toList().forEach { cr ->
            if (cr.status == MegaContactRequest.STATUS_UNRESOLVED && !cr.isOutgoing) {
                val notificationBuilder: ContactsAdvancedNotificationBuilder =
                    ContactsAdvancedNotificationBuilder.newInstance(
                        appContext,
                        megaApi
                    )
                notificationBuilder.removeAllIncomingContactNotifications()
                applicationScope.launch {
                    val notificationList = getIncomingContactRequestsNotificationListUseCase()
                    notificationBuilder.showIncomingContactRequestNotification(notificationList)
                }
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

                    else -> sendMyAccountUpdateBroadcast(Action.STORAGE_STATE_CHANGED, state)
                }
            }

            MegaEvent.EVENT_ACCOUNT_BLOCKED -> {
                Timber.d("EVENT_ACCOUNT_BLOCKED: %s", event.number)
            }

            MegaEvent.EVENT_BUSINESS_STATUS -> sendBroadcastUpdateAccountDetails()
            MegaEvent.EVENT_MISC_FLAGS_READY -> {
                applicationScope.launch {
                    updateDomainName()
                    broadcastMiscLoadedUseCase()
                }
                getInstance().checkEnabledCookies()
                initialiseAdsIfNeeded()
            }

            MegaEvent.EVENT_RELOADING -> showLoginFetchingNodes()
            MegaEvent.EVENT_UPGRADE_SECURITY -> applicationScope.launch {
                setSecurityUpgradeInAppUseCase(true)
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

    override fun onGlobalSyncStateChanged(api: MegaApiJava) {
        Timber.d("Global sync state changed")
        applicationScope.launch {
            globalSyncUpdates.emit(Unit)
        }
    }

    private fun sendBroadcastUpdateAccountDetails() {
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
                if (n.hasChanged(MegaNode.CHANGE_TYPE_NEW.toLong()))
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
            val channel = NotificationChannel(
                notificationChannelId,
                Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
            val icon: Bitmap? = getBitmapFromVectorDrawable(
                IconPackR.drawable.ic_folder_incoming_medium_solid
            )
            val notificationBuilder: NotificationCompat.Builder =
                NotificationCompat.Builder(appContext, notificationChannelId)
                    .setSmallIcon(IconPackR.drawable.ic_stat_notify)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationContent)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent))
                    .setAutoCancel(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentIntent(pendingIntent)
                    .setColor(ContextCompat.getColor(appContext, R.color.red_600_red_300))
                    .setLargeIcon(icon)
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
            notificationManager.notify(
                Constants.NOTIFICATION_PUSH_CLOUD_DRIVE,
                notificationBuilder.build()
            )
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun getBitmapFromVectorDrawable(
        @DrawableRes drawableId: Int,
    ): Bitmap? {
        val drawable = ContextCompat.getDrawable(appContext, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
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

    /**
     * Initialise ads if needed
     */
    private fun initialiseAdsIfNeeded() {
        applicationScope.launch {
            runCatching {
                val isAdsFeatureEnabled =
                    getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)
                if (isAdsFeatureEnabled) {
                    if (!isMobileAdsInitializeCalled.getAndSet(true)) {
                        Timber.d("Initialising MobileAds")
                        MobileAds.initialize(appContext)
                    }
                }
            }.onFailure {
                Timber.e(it, "MobileAds initialization failed")
            }
        }
    }

    private suspend fun updateDomainName() {
        runCatching { updateDomainNameUseCase() }
            .onFailure { Timber.e(it, "UpdateDomainNameUseCase failed") }
    }
}
