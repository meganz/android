package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.domain.usecase.featureflag.UpdatePersistedFeatureFlagsUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * Persisted feature flags initializer
 *
 * @param updatePersistedFeatureFlagsUseCase
 */
class PersistedFeatureFlagsInitializer @Inject constructor(
    updatePersistedFeatureFlagsUseCase: UpdatePersistedFeatureFlagsUseCase,
) : AppStartInitialiserAction(
    action = {
        runCatching { updatePersistedFeatureFlagsUseCase() }
            .onFailure { Timber.e(it, "Failed to refresh persisted feature flags") }
    }
)
