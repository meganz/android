package mega.privacy.android.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.data.gateway.DeviceGateway

internal interface Cache<T> {
    fun get(): T?

    fun set(value: T?)

    fun clear()
}

internal interface StateFlowCache<T> : Cache<T> {
    val state: StateFlow<T?>

    suspend fun setAsync(value: T?)
}

internal interface MapCache<K, V> : Cache<Map<K, V>> {
    fun getOrPut(key: K, defaultValue: () -> V): V
    suspend fun getOrPutAsync(key: K, defaultValue: suspend () -> V): V
}

internal class ExpiringCache<T>(
    private val deviceGateway: DeviceGateway,
    private val timeOut: Long,
) : Cache<T> {
    private var curValue: T? = null
    private var expiredTime = 0L

    override fun get(): T? =
        if (deviceGateway.getElapsedRealtime() > expiredTime) null else curValue

    override fun set(value: T?) {
        expiredTime = deviceGateway.getElapsedRealtime() + timeOut
        curValue = value
    }

    override fun clear() {
        curValue = null
    }
}

internal class PermanentCache<T> : Cache<T> {
    private var curValue: T? = null

    override fun get(): T? = curValue

    override fun set(value: T?) {
        curValue = value
    }

    override fun clear() {
        curValue = null
    }
}

internal class InMemoryStateFlowCache<T> : StateFlowCache<T> {
    override val state: StateFlow<T?>
        field: MutableStateFlow<T?> = MutableStateFlow(null)

    override fun get() = state.value

    override fun set(value: T?) {
        state.tryEmit(value)
    }

    override suspend fun setAsync(value: T?) {
        state.emit(value)
    }

    override fun clear() {
        state.tryEmit(null)
    }
}

/**
 * Least Recently Used limited size MapCache implementation.
 */
internal class LruLimitedCache<K, V>(private val maxSize: Int) : MapCache<K, V> {
    private val cache =
        //default LinkedHashMap constructor values except for accessOrder=true to make it Lru
        object : LinkedHashMap<K, V>(16, 0.75f, true) {
            // eldest will be removed if the size is bigger than max size
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) =
                size > maxSize

            /**
             * Same as getOrPut extension but with suspended lambda for default value
             */
            suspend inline fun getOrPutAsync(
                key: K,
                crossinline defaultValue: suspend () -> V,
            ): V {
                val value = get(key)
                return if (value == null) {
                    val answer = defaultValue()
                    put(key, answer)
                    answer
                } else {
                    value
                }
            }
        }

    override suspend fun getOrPutAsync(key: K, defaultValue: suspend () -> V) =
        cache.getOrPutAsync(key, defaultValue)

    override fun getOrPut(key: K, defaultValue: () -> V): V = cache.getOrPut(key, defaultValue)


    override fun get() = cache

    override fun set(value: Map<K, V>?) {
        cache.clear()
        value?.let { cache.putAll(it) }
    }

    override fun clear() {
        cache.clear()
    }
}