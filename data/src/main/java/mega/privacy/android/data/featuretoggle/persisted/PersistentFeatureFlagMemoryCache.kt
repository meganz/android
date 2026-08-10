package mega.privacy.android.data.featuretoggle.persisted

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.data.gateway.featuretoggle.PersistedFeatureFlagSnapshotGateway
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.qualifier.PersistedFeatures
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache of persisted feature flags. Serves only values actually persisted to disk;
 * returns `null` for anything else so resolution falls through to remote/default providers.
 * (Returning a default here would mask the remote value, since this runs at `Cached` priority.)
 */
@Singleton
internal class PersistentFeatureFlagMemoryCache @Inject constructor(
    @PersistedFeatures private val managedFeatures: Set<@JvmSuppressWildcards Feature>,
    private val persistedFeatureFlagCache: PersistedFeatureFlagCache,
) : PersistedFeatureFlagSnapshotGateway {
    private val mutex = Mutex()
    private val memoryCache = mutableMapOf<String, Boolean>()
    private val requested = mutableSetOf<String>()
    private var loaded = false

    /** Persisted value for [feature], or `null` if unmanaged or not persisted yet. */
    suspend fun enabled(
        feature: Feature,
    ): Boolean? = mutex.withLock {
        if (feature !in managedFeatures) return null
        ensureLoadedLocked()
        val key = keyOf(feature)
        requested += key
        memoryCache[key]
    }

    /** Sparse snapshot of the on-disk store; features absent from disk are omitted. */
    override suspend fun currentSnapshot(): Map<Feature, Boolean> {
        val fileMap = persistedFeatureFlagCache.read()
        return buildMap {
            managedFeatures.forEach { feature ->
                fileMap[keyOf(feature)]?.let { put(feature, it) }
            }
        }
    }

    /** Persist [snapshot] to disk and update memory, skipping keys already read this session. */
    override suspend fun applySnapshot(snapshot: Map<Feature, Boolean>) {
        val keyed = snapshot.mapKeys { (feature, _) -> keyOf(feature) }
        persistedFeatureFlagCache.write(keyed)
        mutex.withLock {
            keyed.forEach { (key, value) ->
                if (key !in requested) memoryCache[key] = value
            }
            loaded = true
        }
    }

    /** Delete the on-disk snapshot and reset in-memory state; next read re-loads from disk. */
    override suspend fun clear() {
        persistedFeatureFlagCache.clear()
        mutex.withLock {
            memoryCache.clear()
            requested.clear()
            loaded = false
        }
    }

    private suspend fun ensureLoadedLocked() {
        if (loaded) return
        val fileMap = persistedFeatureFlagCache.read()
        managedFeatures.forEach { feature ->
            val key = keyOf(feature)
            fileMap[key]?.let { memoryCache[key] = it }
        }
        loaded = true
    }

    private fun keyOf(feature: Feature): String =
        "${feature::class.java.name}#${feature.name}"
}
