package mega.privacy.android.data.mapper.notification

import mega.privacy.android.domain.entity.notifications.PromoNotification
import nz.mega.sdk.MegaNotificationList
import javax.inject.Inject

/**
 * Map [MegaNotificationList] to [List<PromoNotification>]
 */
class PromoNotificationListMapper @Inject constructor() {

    /**
     * Convert [MegaNotificationList] to [List<PromoNotification>]
     *
     * @param megaNotificationsList [MegaNotificationList]
     * @return                      [List<PromoNotification>]
     */
    operator fun invoke(megaNotificationsList: MegaNotificationList): List<PromoNotification> =
        buildList {
            for (i in 0 until megaNotificationsList.size()) {
                val megaNotification = megaNotificationsList.get(i)
                add(
                    PromoNotification(
                        promoID = megaNotification.id,
                        title = megaNotification.title,
                        description = megaNotification.description,
                        imageName = megaNotification.imageName,
                        imageURL = megaNotification.imagePath,
                        startTimeStamp = megaNotification.start,
                        endTimeStamp = megaNotification.end,
                        actionName = megaNotification.callToAction1["text"],
                        actionURL = megaNotification.callToAction1["link"],
                    )
                )
            }
        }

}