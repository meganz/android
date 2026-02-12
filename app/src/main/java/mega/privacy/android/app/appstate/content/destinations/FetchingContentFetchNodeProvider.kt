package mega.privacy.android.app.appstate.content.destinations

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.content.navigation.FetchNodeProvider
import mega.privacy.android.domain.entity.node.root.RefreshEvent
import javax.inject.Inject

internal class FetchNodeProviderImpl @Inject constructor() : FetchNodeProvider {
    private var isLogInByAccount: Boolean = false

    override fun setLoginByAccount() {
        this.isLogInByAccount = true
    }

    override fun clearLoginByAccount() {
        this.isLogInByAccount = false
    }

    override fun getDestination(session: String, refreshEvent: RefreshEvent?): NavKey {
        return FetchingContentNavKey(session, isLogInByAccount, refreshEvent)
    }


    override fun isFetchNodeDestination(navKey: NavKey): Boolean = navKey is FetchingContentNavKey
}