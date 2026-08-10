package mega.privacy.android.data.facade

import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.data.gateway.RemoteConfigGateway
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class FirebaseRemoteConfigFacade @Inject constructor() : RemoteConfigGateway {

    private val remoteConfig: FirebaseRemoteConfig
        get() = FirebaseRemoteConfig.getInstance()

    override suspend fun setMinimumFetchInterval(intervalInSeconds: Long) {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(intervalInSeconds)
            .build()
        remoteConfig.setConfigSettingsAsync(settings).await()
    }

    override suspend fun fetchAndActivate(): Boolean =
        remoteConfig.fetchAndActivate().await()

    override fun getBoolean(key: String): Boolean? =
        getRemoteValue(key)?.let { runCatching { it.asBoolean() }.getOrNull() }

    override fun getString(key: String): String? =
        getRemoteValue(key)?.asString()

    override fun getLong(key: String): Long? =
        getRemoteValue(key)?.let { runCatching { it.asLong() }.getOrNull() }

    private fun getRemoteValue(key: String): FirebaseRemoteConfigValue? =
        remoteConfig.getValue(key)
            .takeIf { it.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE }

    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                val exception = task.exception
                when {
                    exception != null -> continuation.resumeWithException(exception)
                    task.isCanceled -> continuation.cancel()
                    else -> continuation.resume(task.result)
                }
            }
        }
}
