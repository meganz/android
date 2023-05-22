package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.usecase.GetNumUnreadUserAlertsUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.chat.GetNumUnreadChatsUseCase
import javax.inject.Inject

/**
 * Use case for getting the number of notifications the account has pending.
 */
class GetNotificationCountUseCase @Inject constructor(
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val getNumUnreadUserAlertsUseCase: GetNumUnreadUserAlertsUseCase,
    private val getIncomingContactRequestsUseCase: GetIncomingContactRequestsUseCase,
    private val getNumUnreadChatsUseCase: GetNumUnreadChatsUseCase,
) {

    /**
     * Invoke.
     *
     * @param withChatNotifications True if the count should take into account chat notifications,
     *                              false otherwise.
     */
    suspend operator fun invoke(withChatNotifications: Boolean) = if (rootNodeExistsUseCase()) {
        getNumUnreadUserAlertsUseCase() +
                getIncomingContactRequestsUseCase().size +
                if (withChatNotifications) getNumUnreadChatsUseCase() else 0
    } else {
        0
    }
}