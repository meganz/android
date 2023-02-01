package mega.privacy.android.domain.usecase

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default implementation of [MonitorUserUpdates]
 *
 * @property getAccountDetails
 * @property accountRepository
 * @constructor Create empty Default monitor user updates
 */
@OptIn(FlowPreview::class)
internal class DefaultMonitorUserUpdates @Inject constructor(
    private val getAccountDetails: GetAccountDetails,
    private val accountRepository: AccountRepository,
    private val isUserLoggedIn: IsUserLoggedIn,
) : MonitorUserUpdates {
    override fun invoke(): Flow<UserChanges> {
        return flow {
            if (isUserLoggedIn()) {
                val loggedInUser = getAccountDetails(false).userId
                emitAll(
                    accountRepository.monitorUserUpdates()
                        .flatMapConcat { it.changes.entries.asFlow() }
                        .filter { it.key == loggedInUser }
                        .flatMapConcat { it.value.asFlow() }
                )
            }
        }
    }
}