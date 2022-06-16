package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default [GetNumUnreadUserAlerts] implementation.
 *
 * @param accountRepository
 */
class DefaultGetNumUnreadUserAlerts @Inject constructor(
    private val accountRepository: AccountRepository,
) : GetNumUnreadUserAlerts {

    override suspend fun invoke(): Int = accountRepository.getNumUnreadUserAlerts()
}