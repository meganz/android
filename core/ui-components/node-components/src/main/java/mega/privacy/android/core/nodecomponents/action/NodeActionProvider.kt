package mega.privacy.android.core.nodecomponents.action

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Dependency provider for single node action handlers.
 */
data class SingleNodeActionProvider(
    override val viewModel: NodeOptionsActionViewModel,
    override val context: Context,
    override val coroutineScope: CoroutineScope,
    override val postMessage: (String) -> Unit,
    override val megaNavigator: MegaNavigator,
    override val navigationHandler: NavigationHandler?,
    override val moveLauncher: ActivityResultLauncher<LongArray>,
    override val copyLauncher: ActivityResultLauncher<LongArray>,
    override val shareFolderLauncher: ActivityResultLauncher<LongArray>,
    override val restoreLauncher: ActivityResultLauncher<ArrayList<NameCollision>>,
    override val sendToChatLauncher: ActivityResultLauncher<LongArray>,
    override val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Boolean>,
    val versionsLauncher: ActivityResultLauncher<Long>,
) : NodeActionProvider(
    viewModel = viewModel,
    context = context,
    coroutineScope = coroutineScope,
    postMessage = postMessage,
    megaNavigator = megaNavigator,
    navigationHandler = navigationHandler,
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
    override val context: Context,
    override val coroutineScope: CoroutineScope,
    override val postMessage: (String) -> Unit,
    override val megaNavigator: MegaNavigator,
    override val navigationHandler: NavigationHandler?,
    override val moveLauncher: ActivityResultLauncher<LongArray>,
    override val copyLauncher: ActivityResultLauncher<LongArray>,
    override val shareFolderLauncher: ActivityResultLauncher<LongArray>,
    override val restoreLauncher: ActivityResultLauncher<ArrayList<NameCollision>>,
    override val sendToChatLauncher: ActivityResultLauncher<LongArray>,
    override val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Boolean>,
) : NodeActionProvider(
    viewModel = viewModel,
    context = context,
    coroutineScope = coroutineScope,
    postMessage = postMessage,
    megaNavigator = megaNavigator,
    navigationHandler = navigationHandler,
    moveLauncher = moveLauncher,
    copyLauncher = copyLauncher,
    shareFolderLauncher = shareFolderLauncher,
    restoreLauncher = restoreLauncher,
    sendToChatLauncher = sendToChatLauncher,
    hiddenNodesOnboardingLauncher = hiddenNodesOnboardingLauncher,
)

open class NodeActionProvider(
    open val viewModel: NodeOptionsActionViewModel,
    open val context: Context,
    open val coroutineScope: CoroutineScope,
    open val postMessage: (String) -> Unit,
    open val megaNavigator: MegaNavigator,
    open val navigationHandler: NavigationHandler?,
    open val moveLauncher: ActivityResultLauncher<LongArray>,
    open val copyLauncher: ActivityResultLauncher<LongArray>,
    open val shareFolderLauncher: ActivityResultLauncher<LongArray>,
    open val restoreLauncher: ActivityResultLauncher<ArrayList<NameCollision>>,
    open val sendToChatLauncher: ActivityResultLauncher<LongArray>,
    open val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Boolean>
)