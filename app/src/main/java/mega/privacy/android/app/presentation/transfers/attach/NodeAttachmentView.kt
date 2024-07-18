package mega.privacy.android.app.presentation.transfers.attach

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.app.activities.contract.SendToChatActivityContract
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Node attachment view
 *
 */
@Composable
fun NodeAttachmentView(
    viewModel: NodeAttachmentViewModel = hiltViewModel(),
    showMessage: (String, Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val sendToChatLauncher =
        rememberLauncherForActivityResult(
            SendToChatActivityContract()
        ) { result ->
            result?.let { (nodeHandles, chatIds) ->
                if (nodeHandles != null && chatIds != null) {
                    viewModel.attachNodesToChat(
                        nodeHandles.map { NodeId(it) },
                        chatIds
                    )
                }
            }
        }
    LaunchedEffect(uiState.event) {
        val event = uiState.event
        if (event != null) {
            when (event) {
                is NodeAttachmentEvent.SelectChat -> sendToChatLauncher.launch(
                    event.nodeIds.map { it.longValue }.toLongArray()
                )

                is NodeAttachmentEvent.AttachNode -> viewModel.getNodesToAttach(event.nodeIds)
                NodeAttachmentEvent.ShowOverDiskQuotaPaywall -> {
                    val intent = Intent(
                        MegaApplication.getInstance().applicationContext,
                        OverDiskQuotaPaywallActivity::class.java
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }

                is NodeAttachmentEvent.AttachNodeSuccess -> showMessage(
                    context.getString(R.string.sent_as_message),
                    event.chatIds.firstOrNull().takeIf { event.chatIds.size == 1 } ?: -1
                )
            }
            viewModel.markEventHandled()
        }
    }
}

/**
 * Create node attachment view
 *
 * @param activity
 * @param viewModel
 * @param showMessage
 */
fun createNodeAttachmentView(
    activity: Activity,
    viewModel: NodeAttachmentViewModel,
    showMessage: (String, Long) -> Unit,
): View = ComposeView(activity).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        OriginalTempTheme(isDark = isSystemInDarkTheme()) {
            NodeAttachmentView(
                viewModel = viewModel,
                showMessage = showMessage
            )
        }
    }
}