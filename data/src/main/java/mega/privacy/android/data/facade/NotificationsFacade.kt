package mega.privacy.android.data.facade

import mega.privacy.android.data.gateway.NotificationsGateway
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaIntegerList
import nz.mega.sdk.MegaRequestListenerInterface
import javax.inject.Inject

/**
 * Notifications facade interface
 * Implements [NotificationsGateway]
 *
 * @property megaApi
 */
internal class NotificationsFacade @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) : NotificationsGateway {

    override fun getNotifications(listener: MegaRequestListenerInterface) {
        megaApi.getNotifications(listener)
    }

    override suspend fun getEnabledNotifications(): MegaIntegerList? = megaApi.enabledNotifications
    override fun setLastReadNotification(
        notificationId: Long,
        listener: MegaRequestListenerInterface,
    ) = megaApi.setLastReadNotification(notificationId, listener)

    override fun getLastReadNotificationId(listener: MegaRequestListenerInterface) {
        return megaApi.getLastReadNotification(listener)
    }
}