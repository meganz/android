package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed

/**
 * Slideshow preferences gateway
 */
interface SlideshowPreferencesGateway {
    /**
     * Monitor slideshow speed setting from local preference
     * @return flow of latest speed setting
     */
    fun monitorSpeedSetting(userHandle: Long): Flow<SlideshowSpeed?>

    /**
     * Save slideshow speed setting into local preference
     */
    suspend fun saveSpeedSetting(userHandle: Long, speed: SlideshowSpeed)

    /**
     * Monitor slideshow order setting from local preference
     * @return flow of latest order setting
     */
    fun monitorOrderSetting(userHandle: Long): Flow<SlideshowOrder?>

    /**
     * Save slideshow order setting into local preference
     */
    suspend fun saveOrderSetting(userHandle: Long, order: SlideshowOrder)

    /**
     * Monitor slideshow repeat setting from local preference
     * @return flow of latest repeat setting
     */
    fun monitorRepeatSetting(userHandle: Long): Flow<Boolean?>

    /**
     * Save slideshow repeat setting into local preference
     */
    suspend fun saveRepeatSetting(userHandle: Long, isRepeat: Boolean)
}
