package mega.privacy.android.domain.usecase.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.state.ModalState
import mega.privacy.android.domain.entity.verification.UnVerified
import mega.privacy.android.domain.repository.BusinessRepository
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.RequireTwoFactorAuthenticationUseCase
import mega.privacy.android.domain.usecase.environment.IsFirstLaunchUseCase
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import javax.inject.Inject

/**
 * Monitor modal state use case
 *
 * @property monitorVerificationStatus
 * @property monitorStorageStateEventUseCase
 * @property isFirstLaunchUseCase
 * @property requireTwoFactorAuthenticationUseCase
 */
class MonitorModalStateUseCase @Inject constructor(
    private val monitorVerificationStatus: MonitorVerificationStatus,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val isFirstLaunchUseCase: IsFirstLaunchUseCase,
    private val requireTwoFactorAuthenticationUseCase: RequireTwoFactorAuthenticationUseCase,
    private val businessRepository: BusinessRepository,
) {
    /**
     * Invoke
     *
     * @param firsLoginState
     * @param askPermissionState
     * @param newAccountState
     * @param getUpgradeAccount
     * @param getAccountType
     *
     * @return modal state based on inputs or null if no state
     */
    operator fun invoke(
        firsLoginState: StateFlow<Boolean>,
        askPermissionState: StateFlow<Boolean>,
        newAccountState: StateFlow<Boolean>,
        getUpgradeAccount: () -> Boolean?,
        getAccountType: () -> Int?,
    ): Flow<ModalState> {
        return flow {
            val newLogin = firsLoginState.value
            val firstLaunch = isFirstLaunchUseCase()
            val isNewAccount = newAccountState.value
            val askPermissions = askPermissionState.value
            val freshLogin = newLogin || firstLaunch

            checkBusinessStatus(newLogin)
            checkUpgradeRequiredStatus(newLogin, getUpgradeAccount, getAccountType)
            checkTwoFactorAuthenticationsStatus(
                newLogin,
                isNewAccount,
            )
            checkPhoneVerificationStatus(freshLogin, askPermissions, isNewAccount)
            checkInitialPermissionStatus(firstLaunch, askPermissions)

            if (newLogin) {
                emit(ModalState.FirstLogin)
            }
        }

    }

    private suspend fun FlowCollector<ModalState>.checkBusinessStatus(
        newLogin: Boolean,
    ) {
        val businessAccountStatus = businessRepository.getBusinessStatus()
        if (newLogin && businessAccountStatus == BusinessAccountStatus.Expired) {
            emit(ModalState.ExpiredBusinessAccount)
        }

        if (newLogin && businessAccountStatus == BusinessAccountStatus.GracePeriod && businessRepository.isMasterBusinessAccount()) {
            emit(ModalState.ExpiredBusinessAccountGracePeriod)
        }
    }

    private suspend fun FlowCollector<ModalState>.checkInitialPermissionStatus(
        firstLaunch: Boolean,
        askPermissions: Boolean,
    ) {
        if (firstLaunch || askPermissions) {
            emit(ModalState.RequestInitialPermissions)
        }
    }

    private suspend fun FlowCollector<ModalState>.checkUpgradeRequiredStatus(
        newLogin: Boolean,
        getUpgradeAccount: () -> Boolean?,
        getAccountType: () -> Int?,
    ) {
        val storageState = monitorStorageStateEventUseCase().value.storageState
        val upgradeAccount: Boolean =
            getUpgradeAccount() ?: return
        val accountType: Int =
            getAccountType() ?: free
        when {
            upgradeAccount && accountType != free -> {
                emit(ModalState.UpgradeRequired(accountType))
            }
            upgradeAccount && newLogin && storageState == StorageState.PayWall -> {
                emit(ModalState.UpgradeRequired(null))
            }
        }
    }

    private suspend fun FlowCollector<ModalState>.checkPhoneVerificationStatus(
        freshLogin: Boolean,
        askPermissions: Boolean,
        isNewAccount: Boolean,
    ) {
        val canVerify = monitorVerificationStatus()
            .map { it is UnVerified && it.canRequestOptInVerification }.first()
        if (requiresPhoneVerification(
                isFreshLogin = freshLogin,
                shouldRequestPermissions = askPermissions,
                isNewAccount = isNewAccount,
                canVerify = canVerify,
            )
        ) {
            emit(ModalState.VerifyPhoneNumber)
        }
    }

    private suspend fun FlowCollector<ModalState>.checkTwoFactorAuthenticationsStatus(
        newLogin: Boolean,
        isNewAccount: Boolean,
    ) {
        if (requireTwoFactorAuthenticationUseCase(
                newAccount = newLogin,
                firstLogin = isNewAccount
            )
        ) {
            emit(ModalState.RequestTwoFactorAuthentication)
        }
    }

    private fun requiresPhoneVerification(
        isFreshLogin: Boolean,
        shouldRequestPermissions: Boolean,
        isNewAccount: Boolean,
        canVerify: Boolean,
    ) = (isFreshLogin || shouldRequestPermissions) && (!isNewAccount && canVerify)


    private val free = 0
}