package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow

interface StatisticsPreferencesGateway {
    /**
     * Get the click count
     *
     * @return Total number of clicks.
     */
    fun getClickCount(): Flow<Int>

    /**
     * Set the click count
     *
     * @param count
     */
    suspend fun setClickCount(count: Int)

    /**
     * Get the click count of a [mediaHandle] folder
     *
     * @return Total number of clicks for folder with [mediaHandle]
     */
    fun getClickCountFolder(mediaHandle: Long): Flow<Int>

    /**
     * Set the click count of [mediaHandle] folder
     *
     * @param count
     * @param mediaHandle
     */
    suspend fun setClickCountFolder(count: Int, mediaHandle: Long)
}