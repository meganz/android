package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.NotificationsGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.EventMapper
import mega.privacy.android.data.mapper.UserAlertMapper
import mega.privacy.android.data.mapper.meeting.IntegerListMapper
import mega.privacy.android.data.mapper.notification.PromoNotificationListMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.usecase.meeting.FetchNumberOfScheduledMeetingOccurrencesByChat
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeeting
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaPushNotificationSettings
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [NotificationsRepository]
 *
 * @property megaApiGateway
 */
@Singleton
internal class DefaultNotificationsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val userAlertsMapper: UserAlertMapper,
    private val eventMapper: EventMapper,
    private val fetchSchedOccurrencesByChatUseCase: FetchNumberOfScheduledMeetingOccurrencesByChat,
    private val getScheduledMeetingUseCase: GetScheduledMeeting,
    private val localStorageGateway: MegaLocalStorageGateway,
    private val callsPreferencesGateway: CallsPreferencesGateway,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val appEventGateway: AppEventGateway,
    private val notificationsGateway: NotificationsGateway,
    private val promoNotificationListMapper: PromoNotificationListMapper,
    private val integerListMapper: IntegerListMapper,
) : NotificationsRepository, LegacyNotificationRepository {

    private val _pushNotificationSettings =
        MutableStateFlow(megaApiGateway.createInstanceMegaPushNotificationSettings())

    override val pushNotificationSettings: MegaPushNotificationSettings
        get() = _pushNotificationSettings.value

    override fun monitorUserAlerts() = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnUserAlertsUpdate>()
        .mapNotNull { (newUserAlerts) ->
            withContext(dispatcher) {
                val userAlerts = newUserAlerts?.map { userAlert ->
                    userAlertsMapper(
                        megaUserAlert = userAlert,
                        contactProvider = ::provideContact,
                        scheduledMeetingProvider = ::provideScheduledMeeting,
                        scheduledMeetingOccurrProvider = ::provideSchedMeetingOccurrences,
                        nodeProvider = megaApiGateway::getMegaNodeByHandle,
                        rootParentNodeProvider = ::provideRootParentNode,
                        rubbishNodeProvider = megaApiGateway::getRubbishBinNode,
                        rootNodeProvider = megaApiGateway::getRootNode
                    )
                }

                if (!areMeetingInvitationsEnabled()) {
                    userAlerts?.filter { it !is ScheduledMeetingAlert }
                } else {
                    userAlerts
                }
            }
        }.flowOn(dispatcher)

    override suspend fun enableRequestStatusMonitor() = withContext(dispatcher) {
        megaApiGateway.enableRequestStatusMonitor()
    }

    override fun monitorEvent(): Flow<Event> = megaApiGateway.globalUpdates
        .filterIsInstance<GlobalUpdate.OnEvent>()
        .mapNotNull { (event) ->
            event?.let { eventMapper(it) }
        }
        .flowOn(dispatcher)

    override suspend fun getUserAlerts(): List<UserAlert> =
        withContext(dispatcher) {
            val userAlerts = megaApiGateway.getUserAlerts().map { userAlert ->
                userAlertsMapper(
                    megaUserAlert = userAlert,
                    contactProvider = ::provideContact,
                    scheduledMeetingProvider = ::provideScheduledMeeting,
                    scheduledMeetingOccurrProvider = ::provideSchedMeetingOccurrences,
                    nodeProvider = megaApiGateway::getMegaNodeByHandle,
                    rootParentNodeProvider = ::provideRootParentNode,
                    rubbishNodeProvider = megaApiGateway::getRubbishBinNode,
                    rootNodeProvider = megaApiGateway::getRootNode
                )
            }

            if (!areMeetingInvitationsEnabled()) {
                userAlerts.filter { it !is ScheduledMeetingAlert }
            } else {
                userAlerts
            }
        }

    private suspend fun provideEmail(userId: Long): String? =
        getEmailLocally(userId) ?: fetchAndCacheEmail(userId)

    private suspend fun fetchAndCacheEmail(userId: Long): String? =
        suspendCoroutine { continuation ->
            val callback = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(request.email))
                    } else {
                        Timber.e("Error getting user email: ${error.errorString}")
                        continuation.resumeWith(Result.success(null))
                    }
                }
            )
            megaApiGateway.getUserEmail(userId, callback)
        }.also {
            it?.let { localStorageGateway.setNonContactEmail(userId, it) }
        }


    private suspend fun getEmailLocally(userId: Long) =
        localStorageGateway.getNonContactByHandle(userId)?.email

    private suspend fun provideContact(userId: Long, email: String?): Contact {
        val emailAddress = email ?: provideEmail(userId)
        val nickname = localStorageGateway.getContactByEmail(emailAddress)?.nickname
        val visible = isContactVisible(emailAddress)
        val hasPendingRequest = emailAddressFoundInPendingRequests(emailAddress)

        return Contact(
            userId = userId,
            email = emailAddress,
            nickname = nickname,
            isVisible = visible,
            hasPendingRequest = hasPendingRequest
        )
    }

    private suspend fun provideRootParentNode(nodeHandle: Long): MegaNode? =
        withContext(dispatcher) {
            var currentRootParent = megaApiGateway.getMegaNodeByHandle(nodeHandle)
            while (currentRootParent != null) {
                megaApiGateway.getParentNode(currentRootParent)?.let {
                    currentRootParent = it
                } ?: break
            }
            currentRootParent
        }

    private suspend fun provideScheduledMeeting(
        chatId: Long,
        schedId: Long,
    ): ChatScheduledMeeting? = withContext(dispatcher) {
        runCatching { getScheduledMeetingUseCase(chatId, schedId) }.getOrNull()
    }

    private suspend fun provideSchedMeetingOccurrences(
        chatId: Long,
    ): List<ChatScheduledMeetingOccurr>? = withContext(dispatcher) {
        runCatching { fetchSchedOccurrencesByChatUseCase(chatId, 20) }.getOrNull()
    }

    private suspend fun emailAddressFoundInPendingRequests(emailAddress: String?) =
        megaApiGateway.getIncomingContactRequests()?.mapNotNull { it.sourceEmail }
            ?.contains(emailAddress) ?: false

    private suspend fun isContactVisible(emailAddress: String?) = emailAddress?.let {
        megaApiGateway.getContact(it)?.visibility == MegaUser.VISIBILITY_VISIBLE
    } ?: false

    override suspend fun acknowledgeUserAlerts() {
        megaApiGateway.acknowledgeUserAlerts()
    }

    override fun monitorHomeBadgeCount() = appEventGateway.monitorHomeBadgeCount()

    override suspend fun broadcastHomeBadgeCount(badgeCount: Int) = withContext(dispatcher) {
        appEventGateway.broadcastHomeBadgeCount(badgeCount)
    }

    private suspend fun areMeetingInvitationsEnabled(): Boolean =
        callsPreferencesGateway.getCallsMeetingInvitationsPreference().firstOrNull() ==
                CallsMeetingInvitations.Enabled

    override suspend fun isChatDndEnabled(chatId: Long): Boolean = withContext(dispatcher) {
        _pushNotificationSettings.value.isChatDndEnabled(chatId)
    }

    override suspend fun setChatEnabled(chatId: Long, enabled: Boolean) = withContext(dispatcher) {
        val updatedSettings = _pushNotificationSettings.value.apply {
            enableChat(chatId, enabled)
        }

        setPushNotificationSettings(updatedSettings)
    }

    override suspend fun setChatEnabled(chatIdList: List<Long>, enabled: Boolean) =
        withContext(dispatcher) {
            val updatedSettings = _pushNotificationSettings.value.apply {
                chatIdList.forEach { enableChat(it, enabled) }
            }

            setPushNotificationSettings(updatedSettings)
        }

    override suspend fun setChatDoNotDisturb(chatIdList: List<Long>, timestamp: Long) =
        withContext(dispatcher) {
            val updatedSettings = _pushNotificationSettings.value.apply {
                chatIdList.forEach { setChatDnd(it, timestamp) }
            }

            setPushNotificationSettings(updatedSettings)
        }

    override suspend fun isChatDoNotDisturbEnabled(chatId: Long): Boolean =
        withContext(dispatcher) {
            _pushNotificationSettings.value.isChatDndEnabled(chatId)
        }

    override suspend fun setChatDoNotDisturb(chatId: Long, timestamp: Long) =
        withContext(dispatcher) {
            val updatedSettings = _pushNotificationSettings.value.apply {
                setChatDnd(chatId, timestamp)
            }

            setPushNotificationSettings(updatedSettings)
        }

    override suspend fun setChatsEnabled(enabled: Boolean) = withContext(dispatcher) {
        val updatedSettings = _pushNotificationSettings.value.apply {
            enableChats(enabled)
        }

        setPushNotificationSettings(updatedSettings)
    }

    override suspend fun setChatsDoNotDisturb(timestamp: Long) = withContext(dispatcher) {
        val updatedSettings = _pushNotificationSettings.value.apply {
            globalChatsDnd = timestamp
        }

        setPushNotificationSettings(updatedSettings)
    }

    override suspend fun updatePushNotificationSettings() = withContext(dispatcher) {
        val pushNotificationSettings = suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("getPushNotificationSettings") {
                it.megaPushNotificationSettings
            }

            megaApiGateway.getPushNotificationSettings(listener)

        }

        _pushNotificationSettings.value =
            megaApiGateway.copyMegaPushNotificationsSettings(pushNotificationSettings)
                ?: megaApiGateway.createInstanceMegaPushNotificationSettings()


        appEventGateway.broadcastPushNotificationSettings()
    }

    private suspend fun setPushNotificationSettings(settings: MegaPushNotificationSettings) =
        withContext(dispatcher) {
            val pushNotificationSettings = suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("setPushNotificationSettings") {
                    it.megaPushNotificationSettings
                }

                megaApiGateway.setPushNotificationSettings(settings, listener)

            }

            _pushNotificationSettings.value =
                megaApiGateway.copyMegaPushNotificationsSettings(pushNotificationSettings)
                    ?: megaApiGateway.createInstanceMegaPushNotificationSettings()

            appEventGateway.broadcastPushNotificationSettings()
        }

    override suspend fun getEnabledNotifications(): List<Int> =
        withContext(dispatcher) {
            val notificationsIDs = notificationsGateway.getEnabledNotifications()
            notificationsIDs?.let { integerListMapper(it) } ?: emptyList()
        }

    override suspend fun getPromoNotifications(): List<PromoNotification> =
        withContext(dispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        when (error.errorCode) {
                            MegaError.API_OK -> {
                                continuation.resumeWith(
                                    Result.success(
                                        promoNotificationListMapper(
                                            request.megaNotifications
                                        )
                                    )
                                )
                            }

                            MegaError.API_ENOENT -> {
                                continuation.resumeWith(Result.success(emptyList()))
                            }

                            else -> {
                                Timber.e("Error getting promo notifications: ${error.errorString}")
                                continuation.failWithError(error, "getPromoNotifications")
                            }
                        }
                    }
                )
                notificationsGateway.getNotifications(listener)
            }
        }

    override suspend fun setLastReadNotification(notificationId: Long): Unit =
        withContext(dispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("setLastReadNotification") {
                    it.number
                }
                notificationsGateway.setLastReadNotification(notificationId, listener)
            }
        }

    override suspend fun getLastReadNotificationId(): Long =
        withContext(dispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        when (error.errorCode) {
                            MegaError.API_OK -> {
                                continuation.resumeWith(Result.success(request.number))
                            }

                            MegaError.API_ENOENT -> {
                                continuation.resumeWith(Result.success(0))
                            }

                            else -> {
                                Timber.e("Error getting last read notification: ${error.errorString}")
                                continuation.failWithError(error, "getLastReadNotificationId")
                            }
                        }
                    }
                )
                notificationsGateway.getLastReadNotificationId(listener)
            }
        }
}
