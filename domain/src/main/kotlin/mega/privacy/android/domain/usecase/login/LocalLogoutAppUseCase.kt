package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.ClearPsa
import mega.privacy.android.domain.usecase.StopAudioService
import mega.privacy.android.domain.usecase.workers.StopCameraUploadUseCase
import javax.inject.Inject

/**
 * Use case for logging out of the MEGA account without invalidating the session.
 */
class LocalLogoutAppUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository,
    private val pushesRepository: PushesRepository,
    private val billingRepository: BillingRepository,
    private val stopCameraUploadUseCase: StopCameraUploadUseCase,
    private val stopAudioService: StopAudioService,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(clearPsa: ClearPsa) {
        transferRepository.cancelTransfers()
        with(accountRepository) {
            resetAccountAuth()
            cancelAllNotifications()
            clearAppDataAndCache()
            clearAccountPreferences()
            clearSharedPreferences()
            resetAccountInfo()
        }
        pushesRepository.clearPushToken()
        billingRepository.clearCache()
        loginRepository.broadcastLogout()
        stopCameraUploadUseCase()
        stopAudioService()
        clearPsa()
    }
}
