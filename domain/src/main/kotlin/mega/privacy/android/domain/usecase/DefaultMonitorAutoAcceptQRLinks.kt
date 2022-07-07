package mega.privacy.android.domain.usecase

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default monitor auto accept QR links us case
 *
 * @property fetchAutoAcceptQRLinks
 * @property getAccountDetails
 * @property accountRepository
 */
class DefaultMonitorAutoAcceptQRLinks @Inject constructor(
    private val fetchAutoAcceptQRLinks: FetchAutoAcceptQRLinks,
    private val getAccountDetails: GetAccountDetails,
    private val accountRepository: AccountRepository,
) : MonitorAutoAcceptQRLinks {
    override fun invoke(): Flow<Boolean> {
        return flow {
            val loggedInUser = getAccountDetails(false).userId

            emit(fetchAutoAcceptQRLinks())

            emitAll(
                accountRepository.monitorUserUpdates()
                    .map { (changes) ->
                        changes
                            .filter { (key, value) ->
                                key == loggedInUser && value.contains(
                                    UserChanges.ContactLinkVerification
                                )
                            }
                    }
                    .filter { it.isNotEmpty() }
                    .map { fetchAutoAcceptQRLinks() }
            )

            awaitCancellation()
        }
    }

}
