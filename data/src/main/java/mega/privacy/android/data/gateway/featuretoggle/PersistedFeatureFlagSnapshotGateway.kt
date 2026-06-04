package mega.privacy.android.data.gateway.featuretoggle

import mega.privacy.android.domain.entity.Feature

/**
 * Persisted feature flag snapshot gateway
 */
internal interface PersistedFeatureFlagSnapshotGateway {

    /**
     * Current snapshot
     */
    suspend fun currentSnapshot(): Map<Feature, Boolean>

    /**
     * Apply snapshot
     *
     * @param snapshot
     */
    suspend fun applySnapshot(snapshot: Map<Feature, Boolean>)
}
