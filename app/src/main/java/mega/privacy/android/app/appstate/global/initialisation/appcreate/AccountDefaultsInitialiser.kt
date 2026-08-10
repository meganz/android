package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import javax.inject.Inject

/**
 * Resets the in-memory account info to its default values at app create.
 */
class AccountDefaultsInitialiser @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
) : AsyncAppCreateInitialiser {
    override val name = "AccountDefaultsInitialiser"

    override suspend operator fun invoke() {
        myAccountInfo.resetDefaults()
    }
}
