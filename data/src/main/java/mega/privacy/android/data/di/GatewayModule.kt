package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.facade.AppEventFacade
import mega.privacy.android.data.facade.AssetsFacade
import mega.privacy.android.data.facade.BillingFacade
import mega.privacy.android.data.facade.CacheFacade
import mega.privacy.android.data.facade.CacheFolderFacade
import mega.privacy.android.data.facade.CameraUploadMediaFacade
import mega.privacy.android.data.facade.ClipboardFacade
import mega.privacy.android.data.facade.DeviceEventFacade
import mega.privacy.android.data.facade.FileAttributeFacade
import mega.privacy.android.data.facade.FileFacade
import mega.privacy.android.data.facade.FileManagementPreferencesFacade
import mega.privacy.android.data.facade.MegaApiFacade
import mega.privacy.android.data.facade.MegaApiFolderFacade
import mega.privacy.android.data.facade.MegaChatApiFacade
import mega.privacy.android.data.facade.MegaLocalRoomFacade
import mega.privacy.android.data.facade.MegaLocalStorageFacade
import mega.privacy.android.data.facade.PermissionFacade
import mega.privacy.android.data.facade.SDCardFacade
import mega.privacy.android.data.facade.TelephonyFacade
import mega.privacy.android.data.facade.VerifyPurchaseFacade
import mega.privacy.android.data.facade.VideoCompressionFacade
import mega.privacy.android.data.facade.WorkManagerFacade
import mega.privacy.android.data.gateway.AndroidDeviceGateway
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.AppInfoGateway
import mega.privacy.android.data.gateway.AssetsGateway
import mega.privacy.android.data.gateway.BillingGateway
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.CameraUploadMediaGateway
import mega.privacy.android.data.gateway.ClipboardGateway
import mega.privacy.android.data.gateway.DefaultAppInfoGateway
import mega.privacy.android.data.gateway.DefaultStreamingGateway
import mega.privacy.android.data.gateway.DeviceEventGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileAttributeGateway
import mega.privacy.android.data.gateway.FileCompressionGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.PermissionGateway
import mega.privacy.android.data.gateway.SDCardGateway
import mega.privacy.android.data.gateway.TelephonyGateway
import mega.privacy.android.data.gateway.VerifyPurchaseGateway
import mega.privacy.android.data.gateway.VideoCompressorGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.ZipFileCompressionGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.gateway.preferences.AccountPreferencesGateway
import mega.privacy.android.data.gateway.preferences.AppInfoPreferencesGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CameraTimestampsPreferenceGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.FeatureFlagPreferencesGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.data.gateway.preferences.LoggingPreferencesGateway
import mega.privacy.android.data.gateway.preferences.RequestPhoneNumberPreferencesGateway
import mega.privacy.android.data.gateway.preferences.SlideshowPreferencesGateway
import mega.privacy.android.data.gateway.preferences.StatisticsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.preferences.AccountPreferencesDataStore
import mega.privacy.android.data.preferences.AppInfoPreferencesDatastore
import mega.privacy.android.data.preferences.AppPreferencesDatastore
import mega.privacy.android.data.preferences.CallsPreferencesDataStore
import mega.privacy.android.data.preferences.CameraTimestampsPreferenceDataStore
import mega.privacy.android.data.preferences.ChatPreferencesDataStore
import mega.privacy.android.data.preferences.FeatureFlagPreferencesDataStore
import mega.privacy.android.data.preferences.LoggingPreferencesDataStore
import mega.privacy.android.data.preferences.RequestPhoneNumberPreferencesDataStore
import mega.privacy.android.data.preferences.SlideshowPreferencesDataStore
import mega.privacy.android.data.preferences.StatisticsPreferencesDataStore
import mega.privacy.android.data.preferences.UIPreferencesDatastore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GatewayModule {

    @Binds
    abstract fun bindRequestPhoneNumberPreferencesGateway(implementation: RequestPhoneNumberPreferencesDataStore): RequestPhoneNumberPreferencesGateway

    @Binds
    abstract fun bindSlideshowPreferencesGateway(implementation: SlideshowPreferencesDataStore): SlideshowPreferencesGateway

    @Binds
    abstract fun bindAssetsGateway(implementation: AssetsFacade): AssetsGateway

    @Binds
    abstract fun bindDeviceGateway(implementation: AndroidDeviceGateway): DeviceGateway

    @Binds
    @Singleton
    abstract fun bindAppInfoGateway(implementation: DefaultAppInfoGateway): AppInfoGateway

    @Binds
    abstract fun bindFileAttributeGateway(implementation: FileAttributeFacade): FileAttributeGateway

    @Binds
    abstract fun bindCameraUploadMediaGateway(implementation: CameraUploadMediaFacade): CameraUploadMediaGateway

    @Binds
    abstract fun bindFileCompressionGateway(implementation: ZipFileCompressionGateway): FileCompressionGateway

    /**
     * Provides CacheGateway implementation
     */
    @Binds
    abstract fun bindCacheGateway(implementation: CacheFacade): CacheGateway

    @Binds
    abstract fun bindChatPreferencesGateway(implementation: ChatPreferencesDataStore): ChatPreferencesGateway

    @Binds
    abstract fun bindCallsPreferencesGateway(implementation: CallsPreferencesDataStore): CallsPreferencesGateway

    @Binds
    abstract fun bindLoggingPreferencesGateway(implementation: LoggingPreferencesDataStore): LoggingPreferencesGateway

    @Binds
    abstract fun bindAppPreferencesGateway(implementation: AppPreferencesDatastore): AppPreferencesGateway

    @Binds
    abstract fun bindAppInfoPreferencesGateway(implementation: AppInfoPreferencesDatastore): AppInfoPreferencesGateway

    @Binds
    abstract fun bindFeatureFlagPreferencesGateway(implementation: FeatureFlagPreferencesDataStore): FeatureFlagPreferencesGateway

    @Binds
    abstract fun bindStatisticsPreferencesGateway(implementation: StatisticsPreferencesDataStore): StatisticsPreferencesGateway

    /**
     * Provide ui preferences gateway implementation
     */
    @Binds
    abstract fun bindUIPreferencesGateway(implementation: UIPreferencesDatastore): UIPreferencesGateway

    @Binds
    abstract fun bindAccountPreferencesGateway(implementation: AccountPreferencesDataStore): AccountPreferencesGateway

    @Binds
    abstract fun bindMegaApiWrapper(implementation: MegaApiFacade): MegaApiGateway

    @Binds
    abstract fun bindMegaChatApiGateway(implementation: MegaChatApiFacade): MegaChatApiGateway

    @Binds
    @Singleton
    abstract fun bindMegaDBHandlerWrapper(implementation: MegaLocalStorageFacade): MegaLocalStorageGateway

    @Binds
    abstract fun bindFileGateway(implementation: FileFacade): FileGateway

    /**
     * Provide camera timestamps preference gateway implementation
     */
    @Binds
    abstract fun bindCameraTimestampsPreferenceGateway(implementation: CameraTimestampsPreferenceDataStore): CameraTimestampsPreferenceGateway

    @Binds
    abstract fun bindMegaApiFolderGateway(implementation: MegaApiFolderFacade): MegaApiFolderGateway

    /**
     * Provides @CacheFolderGateway
     * @param implementation : @CacheFolderFacade
     * @return CacheFolderGateway : @CacheFolderGateway
     */
    @Binds
    abstract fun bindCacheFolderGateway(implementation: CacheFolderFacade): CacheFolderGateway

    /**
     * Provide default implementation of [PermissionGateway]
     */
    @Binds
    abstract fun bindPermissionGateway(implementation: PermissionFacade): PermissionGateway

    /**
     * Provide default implementation of [AppEventGateway]
     */
    @Binds
    @Singleton
    abstract fun bindAppEventGateway(implementation: AppEventFacade): AppEventGateway

    /**
     * Provide default implementation of [DeviceEventGateway]
     */
    @Binds
    @Singleton
    abstract fun bindDeviceEventGateway(implementation: DeviceEventFacade): DeviceEventGateway


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
    abstract fun bindClipboardGateway(implementation: ClipboardFacade): ClipboardGateway

    @Binds
    @Singleton
    abstract fun bindVerifyPurchaseGateway(implementation: VerifyPurchaseFacade): VerifyPurchaseGateway

    @Binds
    @Singleton
    abstract fun bindBillingFacade(implementation: BillingFacade): BillingGateway

    @Binds
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
    abstract fun bindSDCardGateway(implementation: SDCardFacade): SDCardGateway

    @Binds
    @Singleton
    abstract fun bindMegaLocalRoomGateway(implementation: MegaLocalRoomFacade): MegaLocalRoomGateway

    @Binds
    @Singleton
    abstract fun bindWorkManagerGateway(implementation: WorkManagerFacade): WorkManagerGateway
}
