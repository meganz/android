package mega.privacy.android.data.featuretoggle.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.RemoteFeatureFlagValueMapper
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote feature flag value provider
 *
 */
@Singleton
internal class RemoteFeatureFlagValueProvider @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
    private val remoteFeatureFlagValueMapper: RemoteFeatureFlagValueMapper,
) : FeatureFlagValueProvider {
    override suspend fun isEnabled(feature: Feature): Boolean =
        withContext(ioDispatcher) {
            remoteFeatureFlagValueMapper(
                megaApiGateway.getABTestValue(
                    feature.name
                )
            )
        }
}