package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.usecase.featureflag.ClearPersistedFeatureFlagsUseCase
import javax.inject.Inject

/**
 * Clear persisted feature flags logout task
 */
class ClearPersistedFeatureFlagsLogoutTask @Inject constructor(
    private val clearPersistedFeatureFlagsUseCase: ClearPersistedFeatureFlagsUseCase,
) : LogoutTask {

    override suspend fun onLogoutSuccess() {
        clearPersistedFeatureFlagsUseCase()
    }
}
