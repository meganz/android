package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.facade.AssetsFacade
import mega.privacy.android.data.facade.CacheFacade
import mega.privacy.android.data.gateway.AndroidDeviceGateway
import mega.privacy.android.data.gateway.AppInfoGateway
import mega.privacy.android.data.gateway.AssetsGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.DefaultAppInfoGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileCompressionGateway
import mega.privacy.android.data.gateway.ZipFileCompressionGateway
import mega.privacy.android.data.gateway.preferences.AppInfoPreferencesGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.FeatureFlagPreferencesGateway
import mega.privacy.android.data.gateway.preferences.LoggingPreferencesGateway
import mega.privacy.android.data.gateway.preferences.StatisticsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.preferences.AppInfoPreferencesDatastore
import mega.privacy.android.data.preferences.AppPreferencesDatastore
import mega.privacy.android.data.preferences.CallsPreferencesDataStore
import mega.privacy.android.data.preferences.ChatPreferencesDataStore
import mega.privacy.android.data.preferences.FeatureFlagPreferencesDataStore
import mega.privacy.android.data.preferences.LoggingPreferencesDataStore
import mega.privacy.android.data.preferences.StatisticsPreferencesDataStore
import mega.privacy.android.data.preferences.UIPreferencesDatastore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GatewayModule {
    @Binds
    abstract fun bindAssetsGateway(implementation: AssetsFacade): AssetsGateway

    @Binds
    abstract fun bindDeviceGateway(implementation: AndroidDeviceGateway): DeviceGateway

    @Binds
    @Singleton
    abstract fun bindAppInfoGateway(implementation: DefaultAppInfoGateway): AppInfoGateway

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
}