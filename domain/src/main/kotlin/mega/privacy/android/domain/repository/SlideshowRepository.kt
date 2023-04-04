package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed

/**
 * Slideshow repository
 */
interface SlideshowRepository {
    /**
     * Monitor slideshow order setting from local preference
     * @return flow of latest order setting
     */
    fun monitorOrderSetting(): Flow<SlideshowOrder?>

    /**
     * Save slideshow order setting into local preference
     */
    suspend fun saveOrderSetting(order: SlideshowOrder)

    /**
     * Monitor slideshow speed setting from local preference
     * @return flow of latest speed setting
     */
    fun monitorSpeedSetting(): Flow<SlideshowSpeed?>

    /**
     * Save slideshow speed setting into local preference
     */
    suspend fun saveSpeedSetting(speed: SlideshowSpeed)

    /**
     * Monitor slideshow repeat setting from local preference
     * @return flow of latest repeat setting
     */
    fun monitorRepeatSetting(): Flow<Boolean?>

    /**
     * Save slideshow repeat setting into local preference
     */
    suspend fun saveRepeatSetting(isRepeat: Boolean)
}
