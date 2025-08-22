package mega.privacy.android.domain.usecase.node.hiddennode

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import javax.inject.Inject

/**
 * Monitor if hidden nodes feature is enabled (eligible) based on account details.
 * Hidden nodes are only available for paid accounts that are not expired business accounts.
 *
 * @return [Flow<Boolean>] true if hidden nodes feature is enabled, false otherwise
 */
class MonitorHiddenNodesEnabledUseCase @Inject constructor(
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) {
    /**
     * Invoke
     *
     * @return [Flow<Boolean>] true if hidden nodes feature is enabled, false otherwise
     */
    operator fun invoke(): Flow<Boolean> = monitorAccountDetailUseCase().map { accountDetail ->
        val accountType = accountDetail.levelDetail?.accountType
        if (accountType?.isPaid == true) {
            if (accountType.isBusinessAccount) {
                // For business accounts, check if they're active or in grace period
                val businessStatus = runCatching { getBusinessStatusUseCase() }.getOrNull()
                businessStatus == BusinessAccountStatus.Active || businessStatus == BusinessAccountStatus.GracePeriod
            } else {
                // For non-business paid accounts, always enable
                true
            }
        } else {
            // For free accounts, always disable
            false
        }
    }
}
