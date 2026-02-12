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