package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Implementation of [ResetContactLink]
 */
class DefaultResetContactLink @Inject constructor(
    private val accountRepository: AccountRepository,
) : ResetContactLink {
    override suspend fun invoke(): String =
        accountRepository.createContactLink(renew = true)
}