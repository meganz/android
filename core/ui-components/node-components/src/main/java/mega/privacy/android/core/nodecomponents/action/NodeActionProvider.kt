package mega.privacy.android.core.nodecomponents.action

import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.CoroutineScope

/**
 * Dependency provider for single node action handlers.
 */
data class SingleNodeActionProvider(
    override val viewModel: NodeOptionsActionViewModel,
    override val coroutineScope: CoroutineScope,
    override val postMessage: (String) -> Unit,
    override val moveLauncher: ActivityResultLauncher<LongArray>,
    override val copyLauncher: ActivityResultLauncher<LongArray>,
    override val shareFolderLauncher: ActivityResultLauncher<LongArray>,
    override val restoreLauncher: ActivityResultLauncher<LongArray>,
    override val sendToChatLauncher: ActivityResultLauncher<LongArray>,
    override val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Boolean>,
    val versionsLauncher: ActivityResultLauncher<Long>,
) : NodeActionProvider(
    viewModel = viewModel,
    coroutineScope = coroutineScope,
    postMessage = postMessage,
    moveLauncher = moveLauncher,
    copyLauncher = copyLauncher,
    shareFolderLauncher = shareFolderLauncher,
    restoreLauncher = restoreLauncher,
    sendToChatLauncher = sendToChatLauncher,
    hiddenNodesOnboardingLauncher = hiddenNodesOnboardingLauncher
)

/**
 * Dependency provider for multiple nodes action handlers.
 */
data class MultipleNodesActionProvider(
    override val viewModel: NodeOptionsActionViewModel,
    override val coroutineScope: CoroutineScope,
    override val postMessage: (String) -> Unit,
    override val moveLauncher: ActivityResultLauncher<LongArray>,
    override val copyLauncher: ActivityResultLauncher<LongArray>,
    override val shareFolderLauncher: ActivityResultLauncher<LongArray>,
    override val restoreLauncher: ActivityResultLauncher<LongArray>,
    override val sendToChatLauncher: ActivityResultLauncher<LongArray>,
    override val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Boolean>,
) : NodeActionProvider(
    viewModel = viewModel,
    coroutineScope = coroutineScope,
    postMessage = postMessage,
    moveLauncher = moveLauncher,
    copyLauncher = copyLauncher,
    shareFolderLauncher = shareFolderLauncher,
    restoreLauncher = restoreLauncher,
    sendToChatLauncher = sendToChatLauncher,
    hiddenNodesOnboardingLauncher = hiddenNodesOnboardingLauncher,
)

open class NodeActionProvider(
    open val viewModel: NodeOptionsActionViewModel,
    open val coroutineScope: CoroutineScope,
    open val postMessage: (String) -> Unit,
    open val moveLauncher: ActivityResultLauncher<LongArray>,
    open val copyLauncher: ActivityResultLauncher<LongArray>,
    open val shareFolderLauncher: ActivityResultLauncher<LongArray>,
    open val restoreLauncher: ActivityResultLauncher<LongArray>,
    open val sendToChatLauncher: ActivityResultLauncher<LongArray>,
    open val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Boolean>
)