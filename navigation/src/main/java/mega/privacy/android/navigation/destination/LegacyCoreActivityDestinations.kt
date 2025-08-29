package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object OverDiskQuotaPaywallWarning : NavKey

@Serializable
data object MyAccount : NavKey

@Serializable
data class WebSite(val url: String) : NavKey