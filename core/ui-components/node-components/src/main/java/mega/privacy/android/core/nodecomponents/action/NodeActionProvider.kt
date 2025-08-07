package mega.privacy.android.core.nodecomponents.action

import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.CoroutineScope

/**
 * Dependency provider for single node action handlers.
 */
data class SingleNodeActionProvider(
    val viewModel: NodeOptionsActionViewModel,
    val coroutineScope: CoroutineScope,
    val versionsLauncher: ActivityResultLauncher<Long>,
    val moveLauncher: ActivityResultLauncher<LongArray>,
    val copyLauncher: ActivityResultLauncher<LongArray>,
    val shareFolderLauncher: ActivityResultLauncher<LongArray>,
    val restoreLauncher: ActivityResultLauncher<LongArray>,
    val sendToChatLauncher: ActivityResultLauncher<LongArray>,
    val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Boolean>,
)

/**
 * Dependency provider for multiple nodes action handlers.
 */
data class MultipleNodesActionProvider(
    val viewModel: NodeOptionsActionViewModel,
    val coroutineScope: CoroutineScope,
    val moveLauncher: ActivityResultLauncher<LongArray>,
    val copyLauncher: ActivityResultLauncher<LongArray>,
    val shareFolderLauncher: ActivityResultLauncher<LongArray>,
    val restoreLauncher: ActivityResultLauncher<LongArray>,
    val sendToChatLauncher: ActivityResultLauncher<LongArray>,
    val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Boolean>,
)