package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to monitor when misc data is loaded
 */
class MonitorMiscLoadedUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Invoke the use case
     *
     * @return Flow of Unit indicating the misc data load status
     */
    operator fun invoke(): Flow<Unit> = accountRepository.monitorMiscLoaded()
}