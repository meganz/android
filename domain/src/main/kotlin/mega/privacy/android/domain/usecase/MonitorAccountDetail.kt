package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.account.AccountDetail

/**
 * Monitor account detail
 *
 */
fun interface MonitorAccountDetail {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<AccountDetail>
}