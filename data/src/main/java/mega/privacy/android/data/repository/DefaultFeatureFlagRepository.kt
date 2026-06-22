package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.featuretoggle.PersistedFeatureFlagSnapshotGateway
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.featuretoggle.qualifier.PersistedFeatures
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FeatureFlagRepository
import javax.inject.Inject


/**
 * Default feature flag repository
 *
 * @property ioDispatcher
 * @property featureFlagValueProviderSet
 * @property persistedFeatureFlagSnapshotGateway Read/apply API over the persisted feature flag
 *  snapshot. Hides the concrete persisted provider so the repository never references it
 *  directly — which is what breaks the cycle with `Set<FeatureFlagValueProvider>`.
 * @property managedFeatures Features whose values are persisted; iterated when refreshing the
 *  snapshot.
 */
internal class DefaultFeatureFlagRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val featureFlagValueProviderSet: Set<@JvmSuppressWildcards FeatureFlagValueProvider>,
    private val persistedFeatureFlagSnapshotGateway: PersistedFeatureFlagSnapshotGateway,
    @PersistedFeatures private val managedFeatures: Set<@JvmSuppressWildcards Feature>,
) : FeatureFlagRepository {

    override suspend fun getFeatureValue(feature: Feature) =
        getFeatureValue(feature, FeatureFlagValuePriority.entries.toSet())

    override suspend fun getFeatureValue(
        feature: Feature,
        priorities: Set<FeatureFlagValuePriority>,
    ) = withContext(ioDispatcher) {
        val sorted = featureFlagValueProviderSet
            .filter { priorities.contains(it.priority) }
            .sortedWith(
                compareByDescending<FeatureFlagValueProvider> { it.priority }
                    .thenBy { it::class.qualifiedName }
            )
        sorted.firstNotNullOfOrNull { it.isEnabled(feature) }
    }

    override suspend fun getCurrentPersistedSnapshot() =
        withContext(ioDispatcher) { persistedFeatureFlagSnapshotGateway.currentSnapshot() }

    override suspend fun applySnapshot(newSnapshot: Map<Feature, Boolean>) {
        withContext(ioDispatcher) { persistedFeatureFlagSnapshotGateway.applySnapshot(newSnapshot) }
    }

    override suspend fun clearPersistedSnapshot() {
        withContext(ioDispatcher) { persistedFeatureFlagSnapshotGateway.clear() }
    }
}
