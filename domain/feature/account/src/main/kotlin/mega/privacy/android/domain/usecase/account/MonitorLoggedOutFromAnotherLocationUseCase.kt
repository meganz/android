package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for monitoring the state indicating if the user session has been logged out from another location.
 */
class MonitorLoggedOutFromAnotherLocationUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke.
     *
     * @return Flow of Boolean indicating the session state.
     */
    operator fun invoke(): Flow<Boolean> =
        accountRepository.monitorLoggedOutFromAnotherLocation()
}