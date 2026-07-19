package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.setting.GetMiscFlagsUseCase
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import javax.inject.Inject

/**
 * Fetches the misc flags at app create when no user is logged in; when logged in, the flags
 * arrive as part of the account data instead.
 */
class MiscFlagsInitialiser @Inject constructor(
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val getMiscFlagsUseCase: GetMiscFlagsUseCase,
) : AsyncAppCreateInitialiser {
    override val name = "MiscFlagsInitialiser"

    override suspend operator fun invoke() {
        val isUserLoggedOut = isUserLoggedInUseCase().not()
        if (isUserLoggedOut) {
            getMiscFlagsUseCase()
        }
    }
}
