package mega.privacy.android.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.cache.ExpiringCache
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LocalCacheModule {
    private const val PAYMENT_METHODS_CACHE_TIMEOUT_MILLISECONDS = 720 * 60 * 1000L

    @Provides
    @Singleton
    internal fun providePaymentMethodCache(deviceGateway: DeviceGateway): Cache<PaymentMethodFlags> =
        ExpiringCache(deviceGateway, PAYMENT_METHODS_CACHE_TIMEOUT_MILLISECONDS)
}