package mega.privacy.android.app.flags

import kotlinx.coroutines.runBlocking
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaFlag
import mega.privacy.android.domain.entity.featureflag.ApiFeature
import mega.privacy.android.domain.entity.featureflag.MiscLoadedState
import mega.privacy.android.domain.usecase.setting.BroadcastMiscStateUseCase
import nz.mega.sdk.MegaFlag
import javax.inject.Inject

/**
 * Seeds the remote value of an [ApiFeature] (e.g. `ApiFeatures.CustomisableBottomNavigation`)
 * for full-app instrumented tests running on the `:data-test` fake SDK gateways.
 *
 * Production resolution path (what this helper reproduces):
 * 1. `GetFeatureFlagValueUseCase` → `FeatureFlagRepository` picks the highest-priority
 *    non-null provider value; for a [ApiFeature.checkRemote] feature that is
 *    `ApiFeatureFlagProvider` (priority `RemoteToggled`) unless a persisted/runtime override
 *    exists (none do in a fresh Orchestrator-isolated test process).
 * 2. `ApiFeatureFlagProvider` first waits (2s timeout) for
 *    `AccountRepository.monitorMiscState()` to report [MiscLoadedState.FlagsReady]. In
 *    production that state is broadcast by `GlobalOnEventHandler` when the SDK emits
 *    `EVENT_MISC_FLAGS_READY`; the fake gateway never emits it, so this helper broadcasts it
 *    directly through [BroadcastMiscStateUseCase].
 * 3. It then calls `MegaApiGateway.getFlag(feature.experimentName, commit = true)` — the flag
 *    name requested at the gateway boundary is exactly [ApiFeature.experimentName] (for
 *    `CustomisableBottomNavigation` that is `"btnav"`, with no `ff_`/`ab_` prefix) — and maps
 *    the returned `MegaFlag` via `FlagMapper`: `group > 0` → enabled, `group == 0` → disabled.
 * 4. [ApiFeature.singleCheckPerRun] features are cached in-process after the first non-null
 *    resolution; the Android Test Orchestrator's per-test process isolation guarantees each
 *    test starts with an empty cache, so seeding in `@Before` (before the activity launches)
 *    deterministically pins the value for the whole test.
 *
 * Call [seed] before launching any activity so the first (cached) resolution already sees the
 * seeded value.
 */
class RemoteFeatureFlagSeeder @Inject constructor(
    private val fakeMegaApi: FakeMegaApiGateway,
    private val broadcastMiscStateUseCase: BroadcastMiscStateUseCase,
) {

    /**
     * Stubs the fake gateway's `getFlag` response for [feature] and marks the misc flags as
     * ready so `ApiFeatureFlagProvider` resolves [enabled] immediately instead of timing out
     * to the feature's default value.
     */
    fun seed(feature: ApiFeature, enabled: Boolean) {
        fakeMegaApi.stub(
            MegaApiGateway::getFlag,
            matcher = { arguments -> arguments.firstOrNull() == feature.experimentName },
        ) {
            StubMegaFlag(
                type = MegaFlag.FLAG_TYPE_AB_TEST.toLong(),
                group = if (enabled) 1L else 0L,
            )
        }
        runBlocking { broadcastMiscStateUseCase(MiscLoadedState.FlagsReady) }
    }
}
