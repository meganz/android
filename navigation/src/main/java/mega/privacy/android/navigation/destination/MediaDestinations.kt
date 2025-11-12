package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object MediaMainNavKey : NavKey

@Serializable
data class AlbumContentNavKey(val id: Long?, val type: String?) : NavKey