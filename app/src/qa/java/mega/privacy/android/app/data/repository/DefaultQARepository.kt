package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.DistributionGateway
import mega.privacy.android.app.data.gateway.preferences.FeatureFlagPreferencesGateway
import mega.privacy.android.app.data.mapper.BooleanPreferenceMapper
import mega.privacy.android.app.domain.repository.QARepository
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * Default implementation of the [QARepository]
 *
 * @property distributionGateway
 */
class DefaultQARepository @Inject constructor(
    private val distributionGateway: DistributionGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val features: Set<@JvmSuppressWildcards Feature>,
    private val featureFlagPreferencesGateway: FeatureFlagPreferencesGateway,
    private val booleanPreferenceMapper: BooleanPreferenceMapper,
) : QARepository {

    override fun updateApp() = callbackFlow<Progress> {
        distributionGateway.autoUpdateIfAvailable()
            .addOnProgressListener {
                channel.trySend(Progress((it.apkBytesDownloaded / it.apkFileTotalBytes).toFloat()))
            }.addOnFailureListener {
                channel.close(it)
            }
    }

    override suspend fun setFeature(featureName: String, isEnabled: Boolean) =
        withContext(ioDispatcher) {
            featureFlagPreferencesGateway.setFeature(featureName, isEnabled)
        }

    override suspend fun getAllFeatures(): List<Feature> = features.toList()

    override fun monitorLocalFeatureFlags(): Flow<Map<String, Boolean>> {
        return featureFlagPreferencesGateway.getAllFeatures()
            .map(booleanPreferenceMapper)
    }
}