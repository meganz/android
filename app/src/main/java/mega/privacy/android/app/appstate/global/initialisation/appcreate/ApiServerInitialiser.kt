package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.domain.usecase.apiserver.UpdateApiServerUseCase
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import javax.inject.Inject

/**
 * Applies the persisted API server configuration to the SDK at app create.
 */
class ApiServerInitialiser @Inject constructor(
    private val updateApiServerUseCase: UpdateApiServerUseCase,
) : AsyncAppCreateInitialiser {
    override val name = "ApiServerInitialiser"

    override suspend operator fun invoke() {
        updateApiServerUseCase()
    }
}
