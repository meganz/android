package mega.privacy.android.data.featuretoggle.persisted

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.data.gateway.featuretoggle.PersistedFeatureFlagSnapshotGateway
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.featuretoggle.qualifier.DefaultFeatureFlagProviders
import mega.privacy.android.domain.featuretoggle.qualifier.PersistedFeatures
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent feature flag memory cache
 *
 * @property defaultProviders
 * @property managedFeatures
 * @property persistedFeatureFlagCache
 */
@Singleton
internal class PersistentFeatureFlagMemoryCache @Inject constructor(
    @DefaultFeatureFlagProviders private val defaultProviders: Set<@JvmSuppressWildcards FeatureFlagValueProvider>,
    @PersistedFeatures private val managedFeatures: Set<@JvmSuppressWildcards Feature>,
    private val persistedFeatureFlagCache: PersistedFeatureFlagCache,
) : PersistedFeatureFlagSnapshotGateway {
    private val mutex = Mutex()
    private val memoryCache = mutableMapOf<String, Boolean>()
    private val requested = mutableSetOf<String>()
    private var loaded = false

    /**
     * Returns the currently effective value for [feature], lazily seeding all [managedFeatures]
     * from disk + defaults on the first call. Marks [feature] as read for session consistency.
     * @param feature
     */
    suspend fun enabled(
        feature: Feature,
    ): Boolean? = mutex.withLock {
        if (feature !in managedFeatures) return null
        ensureLoadedLocked()
        val key = keyOf(feature)
        requested += key
        memoryCache[key]
    }

    /**
     * Sparse snapshot of the on-disk store, keyed by feature. Entries absent from disk are
     * omitted so callers can distinguish "not yet persisted" from "persisted as `false`".
     */
    override suspend fun currentSnapshot(): Map<Feature, Boolean> {
        val fileMap = persistedFeatureFlagCache.read()
        return buildMap {
            managedFeatures.forEach { feature ->
                fileMap[keyOf(feature)]?.let { put(feature, it) }
            }
        }
    }

    /**
     * Replace the on-disk snapshot with [snapshot] and update in-memory values — but only for
     * keys that have not yet been read this session.
     * @param snapshot
     */
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

    private suspend fun ensureLoadedLocked() {
        if (loaded) return
        val fileMap = persistedFeatureFlagCache.read()
        managedFeatures.forEach { feature ->
            val key = keyOf(feature)
            val default = defaultProviders.firstNotNullOfOrNull { it.isEnabled(feature) } ?: false
            memoryCache[key] = fileMap[key] ?: default
        }
        loaded = true
    }

    /**
     * Key of
     *
     * @param feature
     */
    private fun keyOf(feature: Feature): String =
        "${feature::class.java.name}#${feature.name}"
}
