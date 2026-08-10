package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.facade.MegaApiFacade
import mega.privacy.android.data.facade.MegaChatApiFacade
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import javax.inject.Singleton

/**
 * Binds the MEGA SDK gateway interfaces to their real facade implementations.
 *
 * Kept separate from [GatewayModule] and public so instrumented tests can replace the SDK with
 * fakes via `@TestInstallIn(replaces = [SdkGatewayModule::class])` while every other gateway
 * binding stays real.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SdkGatewayModule {

    @Binds
    internal abstract fun bindMegaApiGateway(implementation: MegaApiFacade): MegaApiGateway

    @Binds
    @Singleton
    internal abstract fun bindMegaChatApiGateway(implementation: MegaChatApiFacade): MegaChatApiGateway
}
