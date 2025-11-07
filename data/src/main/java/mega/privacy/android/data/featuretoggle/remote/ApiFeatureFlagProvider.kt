package mega.privacy.android.data.featuretoggle.remote

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.featureflag.FlagMapper
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ApiFeature
import mega.privacy.android.domain.entity.featureflag.MiscLoadedState
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
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
    private val accountRepository: AccountRepository,
) : FeatureFlagValueProvider {
    override suspend fun isEnabled(feature: Feature): Boolean? =
        withContext(ioDispatcher) {
            if (feature is ApiFeature && feature.checkRemote) {
                // Fail-safe: Call getUserData if not called already. This will trigger EVENT_MISC_FLAGS_READY event
                accountRepository.getCurrentMiscState()
                    .takeIf { it is MiscLoadedState.NotLoaded }
                    ?.let { accountRepository.getUserData() }

                // Wait for flags to be loaded
                accountRepository.monitorMiscState()
                    .filter { it is MiscLoadedState.FlagsReady }
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
