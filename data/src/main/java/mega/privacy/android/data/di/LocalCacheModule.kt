package mega.privacy.android.data.di

import com.android.billingclient.api.ProductDetails
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.cache.ExpiringCache
import mega.privacy.android.data.cache.InMemoryStateFlowCache
import mega.privacy.android.data.cache.LruLimitedCache
import mega.privacy.android.data.cache.MapCache
import mega.privacy.android.data.cache.PermanentCache
import mega.privacy.android.data.cache.StateFlowCache
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.qualifier.DisplayPathFromUriCache
import mega.privacy.android.data.qualifier.FeatureFlagCache
import mega.privacy.android.data.qualifier.FileVersionsOption
import mega.privacy.android.data.qualifier.OriginalPathForNodeCache
import mega.privacy.android.data.qualifier.OriginalPathForPendingMessageCache
import mega.privacy.android.data.qualifier.ParentNodeCache
import mega.privacy.android.data.qualifier.TransferPathCache
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.banner.Banner
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.payment.UpgradeSource
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import nz.mega.sdk.MegaNode
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LocalCacheModule {
    private const val MAX_TRANSFER_CACHE_SIZE = 1000

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
    fun providePsaCache(): StateFlowCache<Psa> = InMemoryStateFlowCache()

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
    fun provideUpgradeSourceCache(): Cache<UpgradeSource> = PermanentCache()

    @Provides
    @Singleton
    @DisplayPathFromUriCache
    fun provideDisplayPathFromUriCache(): MapCache<String, String> =
        LruLimitedCache(MAX_TRANSFER_CACHE_SIZE)

    @ParentNodeCache
    @Provides
    @Singleton
    fun provideParentNodeCache(): MapCache<Long, MegaNode?> =
        LruLimitedCache(MAX_TRANSFER_CACHE_SIZE)

    @TransferPathCache
    @Provides
    @Singleton
    fun provideTransferPathCache(): MapCache<Pair<Long, TransferType>, String> =
        LruLimitedCache(MAX_TRANSFER_CACHE_SIZE)

    @FeatureFlagCache
    @Provides
    @Singleton
    fun provideFeatureFlagCache(): HashMap<Feature, Boolean?> = hashMapOf()
}