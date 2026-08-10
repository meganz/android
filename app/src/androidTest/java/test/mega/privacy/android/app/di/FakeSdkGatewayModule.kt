package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.data.di.SdkGatewayModule
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import javax.inject.Singleton

/**
 * Replaces the real SDK gateway bindings with the WireMock-style fakes from `:data-test` for
 * every instrumented test, so the whole app runs against a programmable in-process SDK.
 *
 * Tests inject [FakeMegaApiGateway] / [FakeMegaChatApiGateway] directly to stub responses,
 * verify calls and emit SDK events; the rest of the app receives the same instances through the
 * gateway interfaces.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SdkGatewayModule::class],
)
object FakeSdkGatewayModule {

    @Provides
    @Singleton
    fun provideFakeMegaApiGateway(): FakeMegaApiGateway = FakeMegaApiGateway()

    @Provides
    @Singleton
    fun provideFakeMegaChatApiGateway(): FakeMegaChatApiGateway = FakeMegaChatApiGateway()

    @Provides
    @Singleton
    fun provideMegaApiGateway(fake: FakeMegaApiGateway): MegaApiGateway = fake

    @Provides
    @Singleton
    fun provideMegaChatApiGateway(fake: FakeMegaChatApiGateway): MegaChatApiGateway = fake
}
