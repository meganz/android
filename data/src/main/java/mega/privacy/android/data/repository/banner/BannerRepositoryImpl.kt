package mega.privacy.android.data.repository.banner

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.banner.MegaBannerMapper
import mega.privacy.android.domain.entity.banner.Banner
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.BannerRepository
import javax.inject.Inject

internal class BannerRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaBannerMapper: MegaBannerMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val bannerCache: Cache<List<Banner>>,
) : BannerRepository {
    private var lastFetchedTime: Long = 0

    override suspend fun getBanners(forceRefresh: Boolean): List<Banner> =
        withContext(ioDispatcher) {
            if (forceRefresh) {
                getBanners()
            } else {
                bannerCache.get().orEmpty()
            }
        }

    private suspend fun getBanners(): List<Banner> {
        return suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("getBanners") {
                megaBannerMapper(it.megaBannerList)
            }
            megaApiGateway.getBanners(listener)
        }.also {
            lastFetchedTime = System.currentTimeMillis()
            bannerCache.set(it)
        }
    }

    override suspend fun dismissBanner(bannerId: Int) = withContext(ioDispatcher) {
        megaApiGateway.dismissBanner(bannerId)
        bannerCache.set(bannerCache.get().orEmpty().filter { it.id != bannerId })
    }

    override fun getLastFetchedBannerTime(): Long = lastFetchedTime

    override fun clearCache() {
        bannerCache.clear()
        lastFetchedTime = 0
    }
}