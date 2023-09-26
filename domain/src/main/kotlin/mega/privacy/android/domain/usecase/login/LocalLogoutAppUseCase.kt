package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.StopAudioService
import mega.privacy.android.domain.usecase.psa.ClearPsaUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
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
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val stopAudioService: StopAudioService,
    private val photosRepository: PhotosRepository,
    private val albumRepository: AlbumRepository,
    private val clearPsaUseCase: ClearPsaUseCase,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
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
        albumRepository.clearCache()
        photosRepository.clearCache()
        loginRepository.broadcastLogout()
        stopCameraUploadsUseCase(shouldReschedule = false)
        stopAudioService()
        clearPsaUseCase()
    }
}
