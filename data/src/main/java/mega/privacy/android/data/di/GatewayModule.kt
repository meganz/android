package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.facade.AdsFacade
import mega.privacy.android.data.facade.AppEventFacade
import mega.privacy.android.data.facade.AssetsFacade
import mega.privacy.android.data.facade.BillingFacade
import mega.privacy.android.data.facade.CacheFolderFacade
import mega.privacy.android.data.facade.CameraUploadsMediaFacade
import mega.privacy.android.data.facade.ClipboardFacade
import mega.privacy.android.data.facade.FileAttributeFacade
import mega.privacy.android.data.facade.FileFacade
import mega.privacy.android.data.facade.FileManagementPreferencesFacade
import mega.privacy.android.data.facade.HttpConnectionFacade
import mega.privacy.android.data.facade.MediaRecorderFacade
import mega.privacy.android.data.facade.MegaApiFacade
import mega.privacy.android.data.facade.MegaApiFolderFacade
import mega.privacy.android.data.facade.MegaChatApiFacade
import mega.privacy.android.data.facade.MegaLocalRoomFacade
import mega.privacy.android.data.facade.MegaLocalStorageFacade
import mega.privacy.android.data.facade.NotificationsFacade
import mega.privacy.android.data.facade.PermissionFacade
import mega.privacy.android.data.facade.SDCardFacade
import mega.privacy.android.data.facade.TelephonyFacade
import mega.privacy.android.data.facade.VerifyPurchaseFacade
import mega.privacy.android.data.facade.VideoCompressionFacade
import mega.privacy.android.data.facade.WorkManagerGatewayImpl
import mega.privacy.android.data.facade.WorkerClassGatewayImpl
import mega.privacy.android.data.facade.chat.ChatStorageFacade
import mega.privacy.android.data.gateway.AdsGateway
import mega.privacy.android.data.gateway.AndroidDeviceGateway
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.AssetsGateway
import mega.privacy.android.data.gateway.BillingGateway
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.CacheGatewayImpl
import mega.privacy.android.data.gateway.CameraUploadsMediaGateway
import mega.privacy.android.data.gateway.ClipboardGateway
import mega.privacy.android.data.gateway.DefaultStreamingGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileAttributeGateway
import mega.privacy.android.data.gateway.FileCompressionGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.HttpConnectionGateway
import mega.privacy.android.data.gateway.MediaRecorderGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.NotificationsGateway
import mega.privacy.android.data.gateway.PermissionGateway
import mega.privacy.android.data.gateway.SDCardGateway
import mega.privacy.android.data.gateway.TelephonyGateway
import mega.privacy.android.data.gateway.TransfersPreferencesGateway
import mega.privacy.android.data.gateway.VerifyPurchaseGateway
import mega.privacy.android.data.gateway.VideoCompressorGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.WorkerClassGateway
import mega.privacy.android.data.gateway.ZipFileCompressionGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.gateway.chat.ChatStorageGateway
import mega.privacy.android.data.gateway.contact.ContactGateway
import mega.privacy.android.data.gateway.contact.ContactGatewayImpl
import mega.privacy.android.data.gateway.preferences.AccountPreferencesGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CameraUploadsSettingsPreferenceGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CredentialsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.EphemeralCredentialsGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.data.gateway.preferences.InAppUpdatePreferencesGateway
import mega.privacy.android.data.gateway.preferences.MediaPlayerPreferencesGateway
import mega.privacy.android.data.gateway.preferences.RequestPhoneNumberPreferencesGateway
import mega.privacy.android.data.gateway.preferences.SlideshowPreferencesGateway
import mega.privacy.android.data.gateway.preferences.StatisticsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.gateway.psa.PsaPreferenceGateway
import mega.privacy.android.data.gateway.security.PasscodeStoreGateway
import mega.privacy.android.data.preferences.AccountPreferencesDataStore
import mega.privacy.android.data.preferences.AppPreferencesDatastore
import mega.privacy.android.data.preferences.CallsPreferencesDataStore
import mega.privacy.android.data.preferences.CameraUploadsSettingsPreferenceDataStore
import mega.privacy.android.data.preferences.ChatPreferencesDataStore
import mega.privacy.android.data.preferences.CredentialsPreferencesDataStore
import mega.privacy.android.data.preferences.EphemeralCredentialsDataStore
import mega.privacy.android.data.preferences.InAppUpdatePreferencesDatastore
import mega.privacy.android.data.preferences.MediaPlayerPreferencesDatastore
import mega.privacy.android.data.preferences.RequestPhoneNumberPreferencesDataStore
import mega.privacy.android.data.preferences.SlideshowPreferencesDataStore
import mega.privacy.android.data.preferences.StatisticsPreferencesDataStore
import mega.privacy.android.data.preferences.TransfersPreferencesDataStore
import mega.privacy.android.data.preferences.UIPreferencesDatastore
import mega.privacy.android.data.preferences.psa.PsaPreferenceDataStore
import mega.privacy.android.data.preferences.security.PasscodeDataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GatewayModule {

    @Binds
    @Singleton
    abstract fun bindRequestPhoneNumberPreferencesGateway(implementation: RequestPhoneNumberPreferencesDataStore): RequestPhoneNumberPreferencesGateway

    @Binds
    @Singleton
    abstract fun bindSlideshowPreferencesGateway(implementation: SlideshowPreferencesDataStore): SlideshowPreferencesGateway

    @Binds
    @Singleton
    abstract fun bindAssetsGateway(implementation: AssetsFacade): AssetsGateway

    @Binds
    @Singleton
    abstract fun bindDeviceGateway(implementation: AndroidDeviceGateway): DeviceGateway

    @Binds
    @Singleton
    abstract fun bindFileAttributeGateway(implementation: FileAttributeFacade): FileAttributeGateway

    @Binds
    @Singleton
    abstract fun bindCameraUploadMediaGateway(implementation: CameraUploadsMediaFacade): CameraUploadsMediaGateway

    @Binds
    @Singleton
    abstract fun bindFileCompressionGateway(implementation: ZipFileCompressionGateway): FileCompressionGateway

    /**
     * Provides CacheGateway implementation
     */
    @Binds
    @Singleton
    abstract fun bindCacheGateway(implementation: CacheGatewayImpl): CacheGateway

    @Binds
    @Singleton
    abstract fun bindChatPreferencesGateway(implementation: ChatPreferencesDataStore): ChatPreferencesGateway

    @Binds
    @Singleton
    abstract fun bindCredentialsGateway(implementation: CredentialsPreferencesDataStore): CredentialsPreferencesGateway

    @Binds
    @Singleton
    abstract fun bindCallsPreferencesGateway(implementation: CallsPreferencesDataStore): CallsPreferencesGateway

    @Binds
    @Singleton
    abstract fun bindAppPreferencesGateway(implementation: AppPreferencesDatastore): AppPreferencesGateway

    @Binds
    @Singleton
    abstract fun bindStatisticsPreferencesGateway(implementation: StatisticsPreferencesDataStore): StatisticsPreferencesGateway

    /**
     * Provide ui preferences gateway implementation
     */
    @Binds
    @Singleton
    abstract fun bindUIPreferencesGateway(implementation: UIPreferencesDatastore): UIPreferencesGateway

    @Binds
    @Singleton
    abstract fun bindAccountPreferencesGateway(implementation: AccountPreferencesDataStore): AccountPreferencesGateway

    @Binds
    @Singleton
    abstract fun MediaPlayerPreferencesGateway(implementation: MediaPlayerPreferencesDatastore): MediaPlayerPreferencesGateway

    @Binds
    abstract fun bindMegaApiWrapper(implementation: MegaApiFacade): MegaApiGateway

    @Binds
    @Singleton
    abstract fun bindMegaChatApiGateway(implementation: MegaChatApiFacade): MegaChatApiGateway

    @Binds
    @Singleton
    abstract fun bindMegaDBHandlerWrapper(implementation: MegaLocalStorageFacade): MegaLocalStorageGateway

    @Binds
    @Singleton
    abstract fun bindFileGateway(implementation: FileFacade): FileGateway

    @Binds
    @Singleton
    abstract fun bindMegaApiFolderGateway(implementation: MegaApiFolderFacade): MegaApiFolderGateway

    /**
     * Provides @CacheFolderGateway
     * @param implementation : @CacheFolderFacade
     * @return CacheFolderGateway : @CacheFolderGateway
     */
    @Binds
    @Singleton
    abstract fun bindCacheFolderGateway(implementation: CacheFolderFacade): CacheFolderGateway

    /**
     * Provide default implementation of [PermissionGateway]
     */
    @Binds
    @Singleton
    abstract fun bindPermissionGateway(implementation: PermissionFacade): PermissionGateway

    /**
     * Provide default implementation of [AppEventGateway]
     */
    @Binds
    @Singleton
    abstract fun bindAppEventGateway(implementation: AppEventFacade): AppEventGateway


    /**
     * Provide default implementation of [TelephonyGateway]
     */
    @Binds
    @Singleton
    abstract fun bindTelephonyGateway(telephonyFacade: TelephonyFacade): TelephonyGateway

    /**
     * Provides [ClipboardGateway] implementation
     */
    @Binds
    @Singleton
    abstract fun bindClipboardGateway(implementation: ClipboardFacade): ClipboardGateway

    @Binds
    @Singleton
    abstract fun bindVerifyPurchaseGateway(implementation: VerifyPurchaseFacade): VerifyPurchaseGateway

    @Binds
    @Singleton
    abstract fun bindBillingFacade(implementation: BillingFacade): BillingGateway

    @Binds
    @Singleton
    abstract fun bindStreamingGateway(implementation: DefaultStreamingGateway): StreamingGateway

    @Binds
    @Singleton
    abstract fun bindFileManagementPreferencesGateway(implementation: FileManagementPreferencesFacade): FileManagementPreferencesGateway

    @Binds
    abstract fun bindVideoCompressorGateway(implementation: VideoCompressionFacade): VideoCompressorGateway

    /**
     * Provides the default implementation to [SDCardGateway]
     *
     * @param implementation [SDCardFacade]
     *
     * @return [SDCardGateway]
     */
    @Binds
    @Singleton
    abstract fun bindSDCardGateway(implementation: SDCardFacade): SDCardGateway

    @Binds
    @Singleton
    abstract fun bindMegaLocalRoomGateway(implementation: MegaLocalRoomFacade): MegaLocalRoomGateway

    @Binds
    @Singleton
    abstract fun bindWorkManagerGateway(implementation: WorkManagerGatewayImpl): WorkManagerGateway

    @Binds
    @Singleton
    abstract fun bindEphemeralCredentialsGateway(implementation: EphemeralCredentialsDataStore): EphemeralCredentialsGateway

    @Binds
    @Singleton
    abstract fun bindPasscodeStoreGateway(implementation: PasscodeDataStore): PasscodeStoreGateway

    @Binds
    @Singleton
    abstract fun bindCameraUploadsSettingsPreferenceGateway(implementation: CameraUploadsSettingsPreferenceDataStore): CameraUploadsSettingsPreferenceGateway

    @Binds
    @Singleton
    abstract fun bindInAppUpdatePreferencesGateway(implementation: InAppUpdatePreferencesDatastore): InAppUpdatePreferencesGateway

    @Binds
    @Singleton
    abstract fun bindAdsGateway(implementation: AdsFacade): AdsGateway

    @Binds
    @Singleton
    abstract fun bindPsaPreferenceGateway(implementation: PsaPreferenceDataStore): PsaPreferenceGateway

    @Binds
    @Singleton
    abstract fun bindHttpConnectionGateway(implementation: HttpConnectionFacade): HttpConnectionGateway

    @Binds
    @Singleton
    abstract fun bindChatStorageGateway(implementation: ChatStorageFacade): ChatStorageGateway

    @Binds
    @Singleton
    abstract fun bindNotificationGateway(implementation: NotificationsFacade): NotificationsGateway

    @Binds
    @Singleton
    abstract fun bindMediaRecorderGateway(implementation: MediaRecorderFacade): MediaRecorderGateway

    @Binds
    @Singleton
    abstract fun bindWorkerClassGateway(implementation: WorkerClassGatewayImpl): WorkerClassGateway

    @Binds
    @Singleton
    abstract fun bindContactGateway(implementation: ContactGatewayImpl): ContactGateway

    @Binds
    @Singleton
    abstract fun bindTransfersPreferencesDataStoreGateway(implementation: TransfersPreferencesDataStore): TransfersPreferencesGateway
}
