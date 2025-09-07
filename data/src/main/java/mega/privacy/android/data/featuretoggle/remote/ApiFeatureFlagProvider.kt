package mega.privacy.android.data.featuretoggle.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.featureflag.FlagMapper
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ApiFeature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * API feature flag value provider
 *
 */
@OptIn(FlowPreview::class)
internal class ApiFeatureFlagProvider @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
    private val flagMapper: FlagMapper,
    private val appEventGateway: AppEventGateway,
) : FeatureFlagValueProvider {
    override suspend fun isEnabled(feature: Feature): Boolean? =
        withContext(ioDispatcher) {
            if (feature is ApiFeature && feature.checkRemote) {
                appEventGateway.monitorMiscLoaded().filter { it }
                    .timeout(timeOut)
                    .catch {
                        if (it !is TimeoutCancellationException) throw it
                    }
                    .firstOrNull() ?: return@withContext null
                megaApiGateway.getFlag(
                    feature.experimentName, commit = true
                )?.let { megaFlag ->
                    flagMapper(megaFlag).group
                }?.let {
                    feature.mapValue(
                        it
                    )
                }
            } else {
                null
            }
        }

    override val priority = FeatureFlagValuePriority.RemoteToggled

    val timeOut = 10.seconds
}
