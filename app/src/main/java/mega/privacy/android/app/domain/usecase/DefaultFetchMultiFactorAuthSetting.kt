package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.domain.repository.AccountRepository
import nz.mega.sdk.MegaRequestListenerInterface
import javax.inject.Inject

class DefaultFetchMultiFactorAuthSetting @Inject constructor(private val accountRepository: AccountRepository) :
    FetchMultiFactorAuthSetting {
    override fun invoke(): Flow<Boolean> {
        return flow {
            emit(accountRepository.isMultiFactorAuthEnabled())
            emitAll(accountRepository.monitorMultiFactorAuthChanges())
        }
    }
}