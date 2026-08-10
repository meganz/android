package mega.privacy.android.data.featuretoggle.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.RemoteConfigGateway
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.FirebaseABTestFeature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Remote Config feature flag value provider
 *
 * Returns null for features without a remotely fetched value so lower priority
 * providers keep supplying the default.
 */
@Singleton
internal class FirebaseFeatureFlagValueProvider @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val remoteConfigGateway: RemoteConfigGateway,
) : FeatureFlagValueProvider {
    override suspend fun isEnabled(feature: Feature): Boolean? =
        withContext(ioDispatcher) {
            if (feature is FirebaseABTestFeature) {
                remoteConfigGateway.getBoolean(feature.remoteConfigKey)
            } else {
                null
            }
        }

    override val priority = FeatureFlagValuePriority.RemoteToggled
}
