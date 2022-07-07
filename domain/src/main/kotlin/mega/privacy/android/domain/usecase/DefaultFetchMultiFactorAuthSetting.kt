package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default fetch multi factor auth setting
 *
 * @property accountRepository
 */
class DefaultFetchMultiFactorAuthSetting @Inject constructor(private val accountRepository: AccountRepository) :
    FetchMultiFactorAuthSetting {
    override fun invoke(): Flow<Boolean> {
        return flow {
            emit(kotlin.runCatching {
                accountRepository.isMultiFactorAuthEnabled()
            }.getOrDefault(false))
            emitAll(accountRepository.monitorMultiFactorAuthChanges())
        }
    }
}