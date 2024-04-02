package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlertsUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.chat.GetNumUnreadChatsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.notifications.GetFeatureNotificationCountUseCase
import mega.privacy.android.domain.usecase.notifications.GetNumUnreadPromoNotificationsUseCase
import javax.inject.Inject

/**
 * Use case for getting the number of notifications the account has pending.
 */
class GetNotificationCountUseCase @Inject constructor(
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val getNumUnreadUserAlertsUseCase: GetNumUnreadUserAlertsUseCase,
    private val getNumUnreadPromoNotificationsUseCase: GetNumUnreadPromoNotificationsUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getIncomingContactRequestsUseCase: GetIncomingContactRequestsUseCase,
    private val getNumUnreadChatsUseCase: GetNumUnreadChatsUseCase,
    private val featureNotifications: Set<@JvmSuppressWildcards GetFeatureNotificationCountUseCase>,
) {

    /**
     * Invoke.
     *
     * @param withChatNotifications True if the count should take into account chat notifications,
     *                              false otherwise.
     */
    suspend operator fun invoke(withChatNotifications: Boolean, promoFeatureFlag: Feature) =
        if (rootNodeExistsUseCase()) {
            val promoNotificationCount = if (getFeatureFlagValueUseCase(promoFeatureFlag)) {
                getNumUnreadPromoNotificationsUseCase()
            } else {
                0
            }
            getNumUnreadUserAlertsUseCase() +
                    getIncomingContactRequestsUseCase().size +
                    promoNotificationCount +
                    featureNotifications.sumOf { it() } +
                    if (withChatNotifications) getNumUnreadChatsUseCase().firstOrNull() ?: 0 else 0
        } else {
            0
        }
}