package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Implementation of [ResetQRCode]
 */
class DefaultResetQRCode @Inject constructor(
    private val accountRepository: AccountRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ResetQRCode {
    override suspend fun invoke(): String = withContext(ioDispatcher) {
        accountRepository.createContactLink(renew = true)
    }
}