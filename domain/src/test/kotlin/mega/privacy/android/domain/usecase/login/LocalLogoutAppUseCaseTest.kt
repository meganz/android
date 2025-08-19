package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalLogoutAppUseCaseTest {

    private lateinit var underTest: LocalLogoutAppUseCase

    private val loginRepository = mock<LoginRepository>()
    private val accountRepository = mock<AccountRepository>()
    private val transferRepository = mock<TransferRepository>()
    private val pushesRepository = mock<PushesRepository>()
    private val billingRepository = mock<BillingRepository>()
    private val stopCameraUploadsUseCase = mock<StopCameraUploadsUseCase>()
    private val stopAudioService = mock<StopAudioService>()
    private val photosRepository = mock<PhotosRepository>()
    private val albumRepository = mock<AlbumRepository>()
    private val clearPsaUseCase = mock<ClearPsaUseCase>()
    private val settingsRepository = mock<SettingsRepository>()
    private val clearCameraUploadsRecordUseCase = mock<ClearCameraUploadsRecordUseCase>()
    private val clearTransfersPreferencesUseCase = mock<ClearTransfersPreferencesUseCase>()
    private val setSecurityUpgradeInAppUseCase = mock<SetSecurityUpgradeInAppUseCase>()
    private val bannerRepository = mock<BannerRepository>()
    private val pdfRepository = mock<PdfRepository>()

    @BeforeAll
    fun setUp() {
        underTest = LocalLogoutAppUseCase(
            loginRepository = loginRepository,
            accountRepository = accountRepository,
            transferRepository = transferRepository,
            pushesRepository = pushesRepository,
            billingRepository = billingRepository,
            photosRepository = photosRepository,
            albumRepository = albumRepository,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
            stopAudioService = stopAudioService,
            clearPsaUseCase = clearPsaUseCase,
            settingsRepository = settingsRepository,
            clearCameraUploadsRecordUseCase = clearCameraUploadsRecordUseCase,
            clearTransfersPreferencesUseCase = clearTransfersPreferencesUseCase,
            setSecurityUpgradeInAppUseCase = setSecurityUpgradeInAppUseCase,
            bannerRepository = bannerRepository,
            pdfRepository = pdfRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            loginRepository,
            accountRepository,
            transferRepository,
            pushesRepository,
            billingRepository,
            stopCameraUploadsUseCase,
            stopAudioService,
            clearPsaUseCase,
            clearCameraUploadsRecordUseCase,
            clearTransfersPreferencesUseCase,
            setSecurityUpgradeInAppUseCase,
            bannerRepository,
            pdfRepository,
        )
    }

    @Test
    fun `test that all required functionalities are invoked`() = runTest {
        underTest.invoke()

        verify(transferRepository).cancelTransfers()
        verify(transferRepository).deleteAllActiveTransfers()
        verify(transferRepository).resetPauseTransfers()
        verify(clearTransfersPreferencesUseCase).invoke()
        verify(accountRepository).resetAccountAuth()
        verify(accountRepository).cancelAllNotifications()
        verify(accountRepository).clearAppDataAndCache()
        verify(accountRepository).clearAccountPreferences()
        verify(accountRepository).clearSharedPreferences()
        verify(accountRepository).resetAccountInfo()
        verify(pushesRepository).clearPushToken()
        verify(billingRepository).clearCache()
        verify(loginRepository).broadcastLogout()
        verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.StopAndDisable)
        verify(stopAudioService).invoke()
        verify(clearPsaUseCase).invoke()
        verify(settingsRepository).resetSetting()
        verify(clearCameraUploadsRecordUseCase)
            .invoke(listOf(CameraUploadFolderType.Primary, CameraUploadFolderType.Secondary))
        verify(setSecurityUpgradeInAppUseCase).invoke(false)
        verify(bannerRepository).clearCache()
        verify(pdfRepository).deleteAllLastPageViewedInPdf()
    }
}
