package mega.privacy.android.app.appstate.content.destinations

import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.navkey.NoNodeNavKey

@Serializable
data class FetchingContentNavKey(val session: String, val isFromLogin: Boolean) : NoNodeNavKey