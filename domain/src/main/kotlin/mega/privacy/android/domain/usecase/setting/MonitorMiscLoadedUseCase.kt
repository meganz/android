package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.featureflag.MiscLoadedState
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to monitor when misc data is loaded
 */
class MonitorMiscLoadedUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke the use case
     *
     * @return Flow of Boolean indicating when misc flags are ready (true when FlagsReady)
     */
    operator fun invoke(): Flow<Boolean> = accountRepository.monitorMiscState()
        .filter { it is MiscLoadedState.FlagsReady }
        .map { true }
}