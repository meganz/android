package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.facade.AccountInfoFacade
import mega.privacy.android.app.data.facade.AccountInfoWrapper
import mega.privacy.android.app.data.facade.CacheFolderFacade
import mega.privacy.android.app.data.facade.MegaApiFacade
import mega.privacy.android.app.data.facade.MegaApiFolderFacade
import mega.privacy.android.app.data.facade.MegaChatApiFacade
import mega.privacy.android.app.data.facade.MegaLocalStorageFacade
import mega.privacy.android.app.data.gateway.AndroidDeviceGateway
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.data.gateway.DeviceGateway
import mega.privacy.android.app.data.gateway.FileCompressionGateway
import mega.privacy.android.app.data.gateway.ZipFileCompressionGateway
import mega.privacy.android.app.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.app.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.app.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.app.data.gateway.preferences.FeatureFlagPreferencesGateway
import mega.privacy.android.app.data.gateway.preferences.LoggingPreferencesGateway
import mega.privacy.android.app.data.gateway.preferences.StatisticsPreferencesGateway
import mega.privacy.android.app.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.app.data.preferences.AppPreferencesDatastore
import mega.privacy.android.app.data.preferences.CallsPreferencesDataStore
import mega.privacy.android.app.data.preferences.ChatPreferencesDataStore
import mega.privacy.android.app.data.preferences.FeatureFlagPreferencesDataStore
import mega.privacy.android.app.data.preferences.LoggingPreferencesDataStore
import mega.privacy.android.app.data.preferences.StatisticsPreferencesDataStore
import mega.privacy.android.app.data.preferences.UIPreferencesDatastore
import mega.privacy.android.app.di.mediaplayer.AudioPlayer
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.facade.MediaPlayerFacade
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.meeting.facade.RTCAudioManagerFacade
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import javax.inject.Singleton

/**
 * Gateway module
 *
 * Registers bindings for gateway dependencies used by the repository classes.
 *
 * Facades and wrappers used by the repositories will also be provided here until they are replaced
 * with repository code or gateways.
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GatewayModule {

    @Binds
    abstract fun bindAccountInfoWrapper(implementation: AccountInfoFacade): AccountInfoWrapper

    @Binds
    abstract fun bindMegaApiWrapper(implementation: MegaApiFacade): MegaApiGateway

    @Binds
    abstract fun bindMegaApiFolderGateway(implementation: MegaApiFolderFacade): MegaApiFolderGateway

    @Binds
    abstract fun bindMegaChatApiGateway(implementation: MegaChatApiFacade): MegaChatApiGateway

    @Binds
    abstract fun bindMegaDBHandlerWrapper(implementation: MegaLocalStorageFacade): MegaLocalStorageGateway

    @Binds
    abstract fun bindChatPreferencesGateway(implementation: ChatPreferencesDataStore): ChatPreferencesGateway

    @Binds
    abstract fun bindCallsPreferencesGateway(implementation: CallsPreferencesDataStore): CallsPreferencesGateway

    @Binds
    abstract fun bindLoggingPreferencesGateway(implementation: LoggingPreferencesDataStore): LoggingPreferencesGateway

    @Binds
    abstract fun bindAppPreferencesGateway(implementation: AppPreferencesDatastore): AppPreferencesGateway

    @Binds
    abstract fun bindFeatureFlagPreferencesGateway(implementation: FeatureFlagPreferencesDataStore): FeatureFlagPreferencesGateway

    @Binds
    abstract fun bindStatisticsPreferencesGateway(implementation: StatisticsPreferencesDataStore): StatisticsPreferencesGateway

    @Binds
    abstract fun bindDeviceGateway(implementation: AndroidDeviceGateway): DeviceGateway

    @Binds
    abstract fun bindFileCompressionGateway(implementation: ZipFileCompressionGateway): FileCompressionGateway

    /**
     * Provides @CacheFolderGateway
     * @param implementation : @CacheFolderFacade
     * @return CacheFolderGateway : @CacheFolderGateway
     */
    @Binds
    abstract fun bindCacheFolderGateway(implementation: CacheFolderFacade): CacheFolderGateway

    /**
     * Provide MediaPlayerGateway implementation
     */
    @AudioPlayer
    @Binds
    @Singleton
    abstract fun bindsAudioPlayerGateway(@AudioPlayer mediaPlayerFacade: MediaPlayerFacade): MediaPlayerGateway

    /**
     * Provide MediaPlayerGateway implementation
     */
    @VideoPlayer
    @Binds
    @Singleton
    abstract fun bindsVideoPlayerGateway(@VideoPlayer mediaPlayerFacade: MediaPlayerFacade): MediaPlayerGateway


    /**
     * Provide ui preferences gateway implementation
     */
    @Binds
    abstract fun bindUIPreferencesGateway(implementation: UIPreferencesDatastore): UIPreferencesGateway

    @Binds
    @Singleton
    abstract fun bindRTCAudioManagerGateway(implementation: RTCAudioManagerFacade): RTCAudioManagerGateway
}