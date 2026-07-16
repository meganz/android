package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.domain.usecase.apiserver.UpdateApiServerUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppCreateInitialiser
import javax.inject.Inject

/**
 * Applies the persisted API server configuration to the SDK at app create.
 */
class ApiServerInitialiser @Inject constructor(
    private val updateApiServerUseCase: UpdateApiServerUseCase,
) : AppCreateInitialiser {
    override val name = "ApiServerInitialiser"
    override val isCritical = false

    override suspend operator fun invoke() {
        updateApiServerUseCase()
    }
}
