package mega.privacy.android.app.domain.usecase

import android.util.Log
import kotlinx.coroutines.flow.*
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