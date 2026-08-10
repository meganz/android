package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.domain.usecase.featureflag.UpdatePersistedFeatureFlagsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorMiscFlagsReadyEventUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * Persisted feature flags initializer
 *
 * @param updatePersistedFeatureFlagsUseCase
 * @param monitorMiscFlagsReadyEventUseCase
 */
class PersistedFeatureFlagsInitializer @Inject constructor(
    updatePersistedFeatureFlagsUseCase: UpdatePersistedFeatureFlagsUseCase,
    monitorMiscFlagsReadyEventUseCase: MonitorMiscFlagsReadyEventUseCase,
) : AppStartInitialiserAction(
    action = {
        monitorMiscFlagsReadyEventUseCase().collectLatest {
            runCatching {
                updatePersistedFeatureFlagsUseCase()
            }.onFailure { Timber.e(it, "Failed to refresh persisted feature flags") }
        }
    }
)
