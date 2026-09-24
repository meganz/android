package mega.privacy.android.navigation.destination

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Navigation key for the Compose name-collision resolution screen.
 *
 * It carries only a short [launchId]; the collision(s) to resolve are held in
 * `NameCollisionLaunchSourceHolder` (`:shared:nodes`) to keep the back stack small. This key lives in
 * `:navigation` so every planned caller can reach it, including those in modules that cannot depend
 * on `:shared:nodes`.
 *
 * @param launchId Identifier the caller stored the collision payload under before navigating here.
 */
@Serializable
@Parcelize
data class NameCollisionNavKey(val launchId: String) : NavKey, Parcelable {

    companion object {
        /**
         * Result key carrying the resolved [mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult]
         * choices (or the outcome the screen produced) back to the caller.
         */
        const val RESULT = "name_collision_result"
    }
}
