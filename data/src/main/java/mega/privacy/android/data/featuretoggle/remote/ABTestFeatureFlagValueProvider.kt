package mega.privacy.android.data.featuretoggle.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.ABTestFeatureFlagValueMapper
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ABTestFeature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote feature flag value provider
 *
 */
@Singleton
internal class ABTestFeatureFlagValueProvider @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
    private val ABTestFeatureFlagValueMapper: ABTestFeatureFlagValueMapper,
) : FeatureFlagValueProvider {
    override suspend fun isEnabled(feature: Feature): Boolean? =
        withContext(ioDispatcher) {
            if (feature is ABTestFeature) {
                ABTestFeatureFlagValueMapper(
                    megaApiGateway.getABTestValue(
                        feature.name
                    )
                )
            } else {
                null
            }
        }
}