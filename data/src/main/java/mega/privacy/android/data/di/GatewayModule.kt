package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.facade.AssetsFacade
import mega.privacy.android.data.gateway.AssetsGateway

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GatewayModule {
    @Binds
    abstract fun bindAssetsGateway(implementation: AssetsFacade): AssetsGateway
}