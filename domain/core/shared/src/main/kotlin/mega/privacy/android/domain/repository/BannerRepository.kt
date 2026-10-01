package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.banner.Banner

/**
 * Banner repository
 */
interface BannerRepository {
    /**
     * Get banners
     */
    suspend fun getBanners(forceRefresh: Boolean): List<Banner>

    /**
     * Dismiss a banner
     *
     * @param bannerId The banner id
     */
    suspend fun dismissBanner(bannerId: Int)

    /**
     * Get the last fetched banner time
     */
    fun getLastFetchedBannerTime(): Long

    /**
     * Clear cache
     */
    fun clearCache()
}