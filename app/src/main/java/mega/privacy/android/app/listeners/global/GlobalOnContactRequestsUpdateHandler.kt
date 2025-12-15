package mega.privacy.android.app.listeners.global

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.fcm.ContactsAdvancedNotificationBuilder
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.contact.GetIncomingContactRequestsNotificationListUseCase
import mega.privacy.android.domain.usecase.notifications.NotifyNotificationCountChangeUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaContactRequest
import timber.log.Timber
import javax.inject.Inject

class GlobalOnContactRequestsUpdateHandler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val notifyNotificationCountChangeUseCase: NotifyNotificationCountChangeUseCase,
    private val getIncomingContactRequestsNotificationListUseCase: GetIncomingContactRequestsNotificationListUseCase,
) {
    operator fun invoke(requests: ArrayList<MegaContactRequest>?) {
        if (requests == null) return
        applicationScope.launch {
            notifyNotificationCountChangeUseCase()
        }

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
}