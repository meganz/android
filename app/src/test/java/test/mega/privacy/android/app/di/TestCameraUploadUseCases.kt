package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.cameraupload.CameraUploadUseCases
import mega.privacy.android.app.utils.wrapper.CameraEnumeratorWrapper
import mega.privacy.android.data.wrapper.ApplicationWrapper
import mega.privacy.android.data.wrapper.CameraUploadsNotificationManagerWrapper
import mega.privacy.android.data.wrapper.CookieEnabledCheckWrapper
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.CreateCameraUploadFolder
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
import mega.privacy.android.domain.usecase.MonitorChargingStoppedState
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.domain.usecase.camerauploads.GetNodeByFingerprintUseCase
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [CameraUploadUseCases::class],
    components = [SingletonComponent::class, ViewModelComponent::class]
)
@Module(includes = [TestGetNodeModule::class])
object TestCameraUploadUseCases {

    @Provides
    fun provideGetNodeByFingerprint() = mock<GetNodeByFingerprintUseCase>()

    @Provides
    fun provideIsChargingRequired() = mock<IsChargingRequired>()

    @Provides
    fun provideSetPrimarySyncHandle() = mock<SetPrimarySyncHandle>()

    @Provides
    fun provideSetSecondarySyncHandle() = mock<SetSecondarySyncHandle>()

    @Provides
    fun provideIsNodeInRubbish() = mock<IsNodeInRubbish>()

    @Provides
    fun provideClearCacheDirectory() = mock<ClearCacheDirectory>()

    @Provides
    fun provideCheckEnableCameraUploadsStatus() = mock<CheckEnableCameraUploadsStatus>()

    @Provides
    fun provideDisableMediaUploadSettings() = mock<DisableMediaUploadSettings>()

    @Provides
    fun provideIsNotEnoughQuota() = mock<IsNotEnoughQuota>()

    @Provides
    fun provideMonitorBatteryInfo() = mock<MonitorBatteryInfo>()

    @Provides
    fun provideMonitorChargingStoppedState() = mock<MonitorChargingStoppedState>()

    @Provides
    fun provideCreateCameraUploadFolder() = mock<CreateCameraUploadFolder>()

    @Provides
    fun provideNotificationHelper() = mock<CameraUploadsNotificationManagerWrapper>()

    @Provides
    fun provideApplicationWrapper() = mock<ApplicationWrapper>()

    @Provides
    fun provideCookieEnabledCheckWrapper() = mock<CookieEnabledCheckWrapper>()

    @Provides
    fun provideCameraEnumeratorWrapper() = mock<CameraEnumeratorWrapper>()
}
