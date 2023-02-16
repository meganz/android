package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.GetAccountDetails
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import javax.inject.Inject

/**
 * Default implementation of [MonitorOtherUsersUpdates]
 *
 * @property getAccountDetails
 * @property accountRepository
 */
internal class DefaultMonitorOtherUsersUpdates @Inject constructor(
    private val getAccountDetails: GetAccountDetails,
    private val accountRepository: AccountRepository,
    private val isUserLoggedIn: IsUserLoggedIn,
) : MonitorOtherUsersUpdates {
    override fun invoke(): Flow<UserUpdate> {
        return flow {
            if (isUserLoggedIn()) {
                val loggedInUser = getAccountDetails(false).userId
                emitAll(
                    accountRepository.monitorUserUpdates()
                        .map { userUpdate -> UserUpdate(userUpdate.changes.filter { it.key != loggedInUser }) }
                )
            }
        }
    }
}