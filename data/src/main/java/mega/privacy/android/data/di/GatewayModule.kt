package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.facade.AssetsFacade
import mega.privacy.android.data.facade.CacheFacade
import mega.privacy.android.data.facade.FileAttributeFacade
import mega.privacy.android.data.gateway.AndroidDeviceGateway
import mega.privacy.android.data.gateway.AppInfoGateway
import mega.privacy.android.data.gateway.AssetsGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.DefaultAppInfoGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileAttributeGateway
import mega.privacy.android.data.gateway.FileCompressionGateway
import mega.privacy.android.data.gateway.ZipFileCompressionGateway
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
    abstract fun bindFileAttributeGateway(implementation: FileAttributeFacade): FileAttributeGateway

    @Binds
    abstract fun bindFileCompressionGateway(implementation: ZipFileCompressionGateway): FileCompressionGateway

    /**
     * Provides CacheGateway implementation
     */
    @Binds
    abstract fun bindCacheGateway(implementation: CacheFacade): CacheGateway
}