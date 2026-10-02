package mega.privacy.android.shared.nodes.namecollision

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.navigation.destination.NameCollisionNavKey
import java.util.UUID

/**
 * Opens the Compose name-collision screen for a batch of collisions.
 *
 * The collisions are parked in [NameCollisionLaunchSourceHolder] under a fresh launch id and only
 * that id travels in the [NameCollisionNavKey], so the serialized back stack never carries the node
 * payloads. Folder uploads resolve their collisions through their own flow, so every launch from here
 * is a plain copy, move or file-upload session.
 *
 * A copy or move reports back through [NameCollisionNavKey.RESULT] (see [NameCollisionResultEffect]);
 * a file upload is handed to the transfer handler by the screen itself and produces no result.
 */
@Stable
class NameCollisionLauncher internal constructor(
    private val launchSourceHolder: NameCollisionLaunchSourceHolder,
    private val navigate: (NavKey) -> Unit,
) {
    fun launch(collisions: List<NameCollision>) {
        val launchId = UUID.randomUUID().toString()
        launchSourceHolder.put(
            launchId,
            NameCollisionArgs.Multiple(collisions = collisions, isFolderUploadContext = false),
        )
        navigate(NameCollisionNavKey(launchId))
    }
}

/**
 * Remembers a [NameCollisionLauncher] bound to [navigate].
 */
@Composable
fun rememberNameCollisionLauncher(navigate: (NavKey) -> Unit): NameCollisionLauncher {
    val context = LocalContext.current
    val currentNavigate by rememberUpdatedState(navigate)
    return remember {
        NameCollisionLauncher(
            launchSourceHolder = context.nameCollisionLaunchSourceHolder,
            navigate = { currentNavigate(it) },
        )
    }
}
