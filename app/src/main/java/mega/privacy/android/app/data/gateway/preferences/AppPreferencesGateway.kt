package mega.privacy.android.app.data.gateway.preferences

import kotlinx.coroutines.flow.Flow

/**
 * App preferences gateway - Replaces defaultSharedPreferences
 */
interface AppPreferencesGateway {
    /**
     * Put string
     *
     * @param key
     * @param value
     */
    suspend fun putString(key: String, value: String)

    /**
     * Put string set
     *
     * @param key
     * @param value
     */
    suspend fun putStringSet(key: String, value: MutableSet<String>)

    /**
     * Put int
     *
     * @param key
     * @param value
     */
    suspend fun putInt(key: String, value: Int)

    /**
     * Put long
     *
     * @param key
     * @param value
     */
    suspend fun putLong(key: String, value: Long)

    /**
     * Put float
     *
     * @param key
     * @param value
     */
    suspend fun putFloat(key: String, value: Float)

    /**
     * Put boolean
     *
     * @param key
     * @param value
     */
    suspend fun putBoolean(key: String, value: Boolean)

    /**
     * Monitor string
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorString(key: String, defaultValue: String?): Flow<String?>

    /**
     * Monitor string set
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorStringSet(key: String, defaultValue: MutableSet<String>?): Flow<MutableSet<String>?>

    /**
     * Monitor int
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorInt(key: String, defaultValue: Int): Flow<Int>

    /**
     * Monitor long
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorLong(key: String, defaultValue: Long): Flow<Long>

    /**
     * Monitor float
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorFloat(key: String, defaultValue: Float): Flow<Float>

    /**
     * Monitor boolean
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorBoolean(key: String, defaultValue: Boolean): Flow<Boolean>

}
