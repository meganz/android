package mega.privacy.android.data.cache

import mega.privacy.android.data.gateway.DeviceGateway

internal interface Cache<T> {
    fun get(): T?

    fun set(value: T?)
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
}

internal class PermanentCache<T> : Cache<T> {
    private var curValue: T? = null

    override fun get(): T? = curValue

    override fun set(value: T?) {
        curValue = value
    }
}