package mega.privacy.android.data.featuretoggle.persisted

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import timber.log.Timber
import javax.inject.Inject

/**
 * Persisted feature flag cache
 *
 * @property ioDispatcher
 * @property cacheGateway
 */
internal class PersistedFeatureFlagCache @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheGateway: CacheGateway,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Read the persisted feature flag map from disk. Returns an empty map if the file does not
     * yet exist or cannot be parsed.
     */
    suspend fun read(): Map<String, Boolean> = withContext(ioDispatcher) {
        runCatching {
            val file = cacheGateway.getCacheFile(CACHE_FOLDER, CACHE_FILE)
                ?: return@runCatching emptyMap()
            if (!file.exists()) return@runCatching emptyMap<String, Boolean>()
            json.decodeFromString<Map<String, Boolean>>(file.readText())
        }.onFailure {
            Timber.w(it, "Failed to read persisted feature flags")
        }.getOrDefault(emptyMap())
    }

    /**
     * Overwrite the persisted feature flag map on disk. The call is silently dropped if the
     * cache file cannot be created.
     *
     * @param map The full set of feature-flag key/value pairs to persist.
     */
    suspend fun write(map: Map<String, Boolean>) {
        withContext(ioDispatcher) {
            runCatching {
                val file = cacheGateway.getCacheFile(CACHE_FOLDER, CACHE_FILE)
                    ?: return@runCatching
                file.writeText(json.encodeToString(map))
            }.onFailure {
                Timber.w(it, "Failed to write persisted feature flags")
            }
        }
    }

    /**
     * Delete the persisted feature flag file from disk. The call is silently dropped if the
     * file cannot be located or removed.
     */
    suspend fun clear() {
        withContext(ioDispatcher) {
            runCatching {
                val file = cacheGateway.getCacheFile(CACHE_FOLDER, CACHE_FILE)
                    ?: return@runCatching
                if (file.exists()) file.delete()
            }.onFailure {
                Timber.w(it, "Failed to clear persisted feature flags")
            }
        }
    }

    private companion object {
        const val CACHE_FOLDER = "featureflags"
        const val CACHE_FILE = "persisted_feature_flags.json"
    }
}