package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsMasterBusinessAccountUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import mega.privacy.android.navigation.destination.BusinessExpiredAlertNavKey
import mega.privacy.android.navigation.destination.BusinessGraceDialogNavKey
import timber.log.Timber
import javax.inject.Inject

class CheckBusinessStatusInitialiser @Inject constructor(
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val monitorUpdateUserDataUseCase: MonitorUpdateUserDataUseCase,
    private val isMasterBusinessAccountUseCase: IsMasterBusinessAccountUseCase,
    private val navigationEventQueue: NavigationEventQueue,
    private val appDialogsEventQueue: AppDialogsEventQueue,
) : PostLoginInitialiser(
    action = { _, isFastLogin ->
        if (!isFastLogin) {
            combine(
                monitorAccountDetailUseCase(),
                monitorUpdateUserDataUseCase()
            ) { accountDetail, _ ->
                accountDetail
            }.filter { it.levelDetail != null }
                .take(1)
                .catch { Timber.e(it, "Failed to monitor account detail") }
                .collectLatest { accountDetail ->
                    runCatching {
                        val businessStatus = getBusinessStatusUseCase()
                        Timber.d("Business account status: $businessStatus")
                        handleBusinessStatus(
                            accountDetail = accountDetail,
                            businessStatus = businessStatus,
                            isMasterBusinessAccountUseCase = isMasterBusinessAccountUseCase,
                            navigationEventQueue = navigationEventQueue,
                            appDialogsEventQueue = appDialogsEventQueue,
                        )
                    }.onFailure { exception ->
                        Timber.e(exception, "Failed to handle business status")
                    }
                }
        }
    }
)

private suspend fun handleBusinessStatus(
    accountDetail: AccountDetail,
    businessStatus: BusinessAccountStatus,
    isMasterBusinessAccountUseCase: IsMasterBusinessAccountUseCase,
    navigationEventQueue: NavigationEventQueue,
    appDialogsEventQueue: AppDialogsEventQueue,
) {
    runCatching {
        val accountType = accountDetail.levelDetail?.accountType
        val isBusinessAccount = accountType == AccountType.BUSINESS
        val isProFlexiAccount = accountType == AccountType.PRO_FLEXI

        if (!isBusinessAccount && !isProFlexiAccount) {
            return
        }

        when (businessStatus) {
            BusinessAccountStatus.Expired -> {
                navigationEventQueue.emit(BusinessExpiredAlertNavKey)
            }

            BusinessAccountStatus.GracePeriod -> {
                if (isBusinessAccount) {
                    val isMasterBusinessAccount = isMasterBusinessAccountUseCase()
                    if (isMasterBusinessAccount) {
                        appDialogsEventQueue.emit(
                            AppDialogEvent(BusinessGraceDialogNavKey)
                        )
                    }
                }
            }

            BusinessAccountStatus.Active,
            BusinessAccountStatus.Inactive,
                -> Unit
        }
    }.onFailure { exception ->
        Timber.e(exception, "Failed to handle business status")
    }
}
