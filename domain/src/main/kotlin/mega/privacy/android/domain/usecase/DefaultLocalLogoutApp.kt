package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Default implementation of [CompleteFastLogin].
 *
 * @property loginRepository [LoginRepository].
 */
class DefaultLocalLogoutApp @Inject constructor(
    private val loginRepository: LoginRepository,
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository,
    private val pushesRepository: PushesRepository,
    private val billingRepository: BillingRepository,
    private val stopCameraUpload: StopCameraUpload,
    private val stopAudioService: StopAudioService,
) : LocalLogoutApp {

    override suspend fun invoke(clearPsa: ClearPsa) {
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
        stopCameraUpload()
        stopAudioService()
        clearPsa()
    }
}
