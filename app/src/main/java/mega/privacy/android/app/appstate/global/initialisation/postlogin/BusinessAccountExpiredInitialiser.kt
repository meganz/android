package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.usecase.IsMasterBusinessAccountUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.business.MonitorBusinessAccountExpiredUseCase
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import mega.privacy.android.navigation.destination.BusinessAccountExpiredDialogNavKey
import timber.log.Timber
import javax.inject.Inject

class BusinessAccountExpiredInitialiser @Inject constructor(
    private val monitorBusinessAccountExpiredUseCase: MonitorBusinessAccountExpiredUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val appDialogsEventQueue: AppDialogsEventQueue,
    private val isMasterBusinessAccountUseCase: IsMasterBusinessAccountUseCase,
) : PostLoginInitialiser(
    action = { _, _ ->
        combine(
            monitorBusinessAccountExpiredUseCase(),
            monitorAccountDetailUseCase().filter { it.levelDetail != null }
        ) { _, accountDetail ->
            accountDetail
        }.catch { Timber.e(it, "Failed to monitor business account expired event") }
            .collectLatest { accountDetail ->
                handleBusinessAccountExpired(
                    isMasterBusinessAccountUseCase = isMasterBusinessAccountUseCase,
                    appDialogsEventQueue = appDialogsEventQueue,
                    accountDetail = accountDetail
                )
            }
    }
)

private suspend fun handleBusinessAccountExpired(
    isMasterBusinessAccountUseCase: IsMasterBusinessAccountUseCase,
    appDialogsEventQueue: AppDialogsEventQueue,
    accountDetail: AccountDetail,
) {
    runCatching {
        val accountType = accountDetail.levelDetail?.accountType
        val isProFlexiAccount = accountType == AccountType.PRO_FLEXI
        val isMasterBusinessAccount = isMasterBusinessAccountUseCase()

        appDialogsEventQueue.emit(
            AppDialogEvent(
                BusinessAccountExpiredDialogNavKey(
                    isProFlexiAccount = isProFlexiAccount,
                    isMasterBusinessAccount = isMasterBusinessAccount
                )
            ),
        )
    }.onFailure { exception ->
        Timber.e(exception, "Failed to get account details for business account expired dialog")
    }
}

