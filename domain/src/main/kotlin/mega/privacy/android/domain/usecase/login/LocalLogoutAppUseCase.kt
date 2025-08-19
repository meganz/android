package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.BannerRepository
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.repository.files.PdfRepository
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.StopAudioService
import mega.privacy.android.domain.usecase.account.SetSecurityUpgradeInAppUseCase
import mega.privacy.android.domain.usecase.camerauploads.ClearCameraUploadsRecordUseCase
import mega.privacy.android.domain.usecase.psa.ClearPsaUseCase
import mega.privacy.android.domain.usecase.transfers.filespermission.ClearTransfersPreferencesUseCase
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
    private val clearCameraUploadsRecordUseCase: ClearCameraUploadsRecordUseCase,
    private val stopAudioService: StopAudioService,
    private val photosRepository: PhotosRepository,
    private val albumRepository: AlbumRepository,
    private val clearPsaUseCase: ClearPsaUseCase,
    private val settingsRepository: SettingsRepository,
    private val clearTransfersPreferencesUseCase: ClearTransfersPreferencesUseCase,
    private val setSecurityUpgradeInAppUseCase: SetSecurityUpgradeInAppUseCase,
    private val bannerRepository: BannerRepository,
    private val pdfRepository: PdfRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        pdfRepository.deleteAllLastPageViewedInPdf()
        bannerRepository.clearCache()
        with(transferRepository) {
            cancelTransfers()
            deleteAllActiveTransfers()
            resetPauseTransfers()
        }
        clearTransfersPreferencesUseCase()
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
        settingsRepository.resetSetting()
        loginRepository.broadcastLogout()
        stopCameraUploadsUseCase(CameraUploadsRestartMode.StopAndDisable)
        clearCameraUploadsRecordUseCase(
            listOf(CameraUploadFolderType.Primary, CameraUploadFolderType.Secondary)
        )
        stopAudioService()
        clearPsaUseCase()
        setSecurityUpgradeInAppUseCase(false)
    }
}
