package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.domain.entity.account.AccountInactivity
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to monitor the account inactivity status, derived from the last purge event.
 *
 * The inactivity itself (EVENT_LAST_PURGE → [AccountInactivity]) is derived in [AccountRepository].
 * Since that is a one-shot event fired once per session during login / fetch nodes, the latest
 * value is cached here in a [StateFlow] started eagerly on the application scope, so screens that
 * subscribe later (e.g. Cloud Drive) still receive the cached value. The collection is kicked off
 * at app start (see the app start initialiser) to ensure it is active before the event is fired.
 *
 * When the user dismisses the banner, the suppressed purge timestamp held by [AccountRepository]
 * hides it app-wide for the rest of the session, independent of the server acknowledgement.
 */
@Singleton
class MonitorAccountInactivityUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val accountInactivity: StateFlow<AccountInactivity?> by lazy {
        accountRepository.monitorAccountInactivity()
            .combine(accountRepository.monitorSuppressedPurgeTimestamp()) { inactivity, suppressedTs ->
                inactivity.takeUnless { it?.purgeTimestamp == suppressedTs }
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )
    }

    /**
     * Invoke.
     *
     * @return a cached [StateFlow] of [AccountInactivity], or null while no inactive-reason purge
     *         event has been received, or after the banner has been dismissed.
     */
    operator fun invoke(): StateFlow<AccountInactivity?> = accountInactivity
}
