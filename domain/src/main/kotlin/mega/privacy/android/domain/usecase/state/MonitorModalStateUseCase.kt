package mega.privacy.android.domain.usecase.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.state.ModalState
import mega.privacy.android.domain.entity.verification.UnVerified
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.account.RequireTwoFactorAuthenticationUseCase
import mega.privacy.android.domain.usecase.environment.IsFirstLaunchUseCase
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import javax.inject.Inject

/**
 * Monitor modal state use case
 *
 * @property monitorVerificationStatus
 * @property monitorStorageStateEvent
 * @property isFirstLaunchUseCase
 * @property requireTwoFactorAuthenticationUseCase
 */
class MonitorModalStateUseCase @Inject constructor(
    private val monitorVerificationStatus: MonitorVerificationStatus,
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    private val isFirstLaunchUseCase: IsFirstLaunchUseCase,
    private val requireTwoFactorAuthenticationUseCase: RequireTwoFactorAuthenticationUseCase,
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
    ): Flow<ModalState?> {
        return combine(
            monitorVerificationStatus()
                .map { it is UnVerified && it.canRequestOptInVerification },
            monitorStorageStateEvent().map { it.storageState },
            firsLoginState,
            askPermissionState,
            newAccountState,
        ) { canVerify, storageState, newLogin, askPermissions, isNewAccount ->

            val update = determineUpgradeRequiredState(
                storageState,
                newLogin,
                getUpgradeAccount,
                getAccountType,
            )
            val firstLaunch = isFirstLaunchUseCase()
            val freshLogin = newLogin || firstLaunch

            when {
                update != null -> {
                    update
                }
                requireTwoFactorAuthenticationUseCase(
                    newAccount = newLogin,
                    firstLogin = isNewAccount
                ) -> {
                    ModalState.RequestTwoFactorAuthentication
                }
                requiresPhoneVerification(
                    isFreshLogin = freshLogin,
                    shouldRequestPermissions = askPermissions,
                    isNewAccount = isNewAccount,
                    canVerify = canVerify,
                ) -> {
                    ModalState.VerifyPhoneNumber
                }
                firstLaunch || askPermissions -> {
                    ModalState.RequestInitialPermissions
                }
                newLogin -> ModalState.FirstLogin
                else -> {
                    null
                }
            }
        }
    }

    private fun requiresPhoneVerification(
        isFreshLogin: Boolean,
        shouldRequestPermissions: Boolean,
        isNewAccount: Boolean,
        canVerify: Boolean,
    ) = (isFreshLogin || shouldRequestPermissions) && (!isNewAccount && canVerify)

    private fun determineUpgradeRequiredState(
        storageState: StorageState,
        firstLogin: Boolean,
        getUpgradeAccount: () -> Boolean?,
        getAccountType: () -> Int?,
    ): ModalState.UpgradeRequired? {
        val upgradeAccount: Boolean =
            getUpgradeAccount() ?: return null
        val accountType: Int =
            getAccountType() ?: free
        return when {
            upgradeAccount && accountType != free -> {
                ModalState.UpgradeRequired(accountType)
            }
            upgradeAccount && firstLogin && storageState == StorageState.PayWall -> {
                ModalState.UpgradeRequired(null)
            }
            else -> {
                null
            }
        }
    }

    private val free = 0
}