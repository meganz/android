package mega.privacy.android.navigation.destination

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

/**
 * Navigation key for share to mega screen
 */
@Serializable
data class ShareToMegaNavKey(val shareUris: List<UriPath>?) : NoSessionNavKey.Mandatory