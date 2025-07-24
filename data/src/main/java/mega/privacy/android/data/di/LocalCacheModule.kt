package mega.privacy.android.data.di

import com.android.billingclient.api.ProductDetails
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.cache.ExpiringCache
import mega.privacy.android.data.cache.PermanentCache
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.qualifier.DisplayPathFromUriCache
import mega.privacy.android.data.qualifier.FileVersionsOption
import mega.privacy.android.data.qualifier.OriginalPathForNodeCache
import mega.privacy.android.data.qualifier.OriginalPathForPendingMessageCache
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.banner.Banner
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.entity.transfer.TransferAppData.RecursiveTransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LocalCacheModule {
    private val PAYMENT_METHODS_CACHE_TIMEOUT_MILLISECONDS = TimeUnit.MINUTES.toMillis(720)
    private val PRICING_CACHE_TIMEOUT_MILLISECONDS = TimeUnit.MINUTES.toMillis(720)

    @Provides
    @Singleton
    internal fun providePaymentMethodCache(deviceGateway: DeviceGateway): Cache<PaymentMethodFlags> =
        ExpiringCache(deviceGateway, PAYMENT_METHODS_CACHE_TIMEOUT_MILLISECONDS)

    @Provides
    @Singleton
    internal fun providePricingCache(deviceGateway: DeviceGateway): Cache<Pricing> =
        ExpiringCache(deviceGateway, PRICING_CACHE_TIMEOUT_MILLISECONDS)

    @Provides
    @Singleton
    internal fun provideNumberOfSubscriptionCache(): Cache<Long> =
        PermanentCache()

    @Provides
    @Singleton
    internal fun provideProductDetailCache(): Cache<List<ProductDetails>> =
        PermanentCache()

    @Provides
    @Singleton
    internal fun provideListMegaSkuCache(): Cache<List<MegaSku>> =
        PermanentCache()

    @Provides
    @Singleton
    internal fun provideActiveSubscription(): Cache<MegaPurchase> =
        PermanentCache()

    @FileVersionsOption
    @Provides
    @Singleton
    internal fun provideFileVersionsOptionCache(): Cache<Boolean> =
        PermanentCache()

    @Provides
    @Singleton
    fun providePsaCache(): Cache<Psa> = PermanentCache()

    @OriginalPathForNodeCache
    @Provides
    @Singleton
    fun provideChatOriginalFileCache(): Cache<Map<NodeId, UriPath>> = PermanentCache()

    @OriginalPathForPendingMessageCache
    @Provides
    @Singleton
    fun provideChatOriginalPathForPendingMessageCache(): Cache<Map<Long, UriPath>> =
        PermanentCache()

    @Provides
    @Singleton
    fun provideBannersCache(): Cache<List<Banner>> = PermanentCache()

    @Provides
    @Singleton
    fun provideParentsAppDataCache(): HashMap<Int, List<RecursiveTransferAppData>> = hashMapOf()

    @Provides
    @Singleton
    @DisplayPathFromUriCache
    fun provideDisplayPathFromUriCache(): HashMap<String, String> = hashMapOf()
}