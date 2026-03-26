package mega.privacy.android.core.nodecomponents.upload

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.nodes.R as NodesR
import java.io.IOException

@Composable
fun UploadingFiles(
    nameCollisionLauncher: ActivityResultLauncher<ArrayList<NameCollision>>,
    parentNodeId: NodeId,
    urisEvent: StateEventWithContent<List<Uri>>,
    onUrisConsumed: () -> Unit,
    pitagTrigger: PitagTrigger,
    onStartUpload: (TransferTriggerEvent) -> Unit,
    viewModel: UploadFileViewModel = hiltViewModel(),
) {
    val megaNavigator = rememberMegaNavigator()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resource = LocalResources.current
    val snackbarHostState = LocalSnackBarHostState.current

    EventEffect(
        uiState.nameCollisionEvent,
        onConsumed = viewModel::onConsumeNameCollisionEvent
    ) {
        nameCollisionLauncher.launch(ArrayList(it))
    }

    EventEffect(
        uiState.overQuotaEvent,
        onConsumed = viewModel::onConsumeOverQuotaEvent
    ) {
        megaNavigator.openOverDiskQuotaPaywallWarning(context)
    }

    EventEffect(
        uiState.startUploadEvent,
        onConsumed = viewModel::onConsumeStartUploadEvent
    ) {
        onStartUpload(it)
    }

    EventEffect(
        uiState.uploadErrorEvent,
        onConsumed = viewModel::onConsumeUploadErrorEvent
    ) { error ->
        if (error is IOException) {
            snackbarHostState?.showAutoDurationSnackbar(resource.getString(NodesR.string.error_not_enough_free_space))
        } else {
            snackbarHostState?.showAutoDurationSnackbar(resource.getString(NodesR.string.error_temporary_unavaible))
        }
    }

    EventEffect(
        urisEvent,
        onConsumed = onUrisConsumed,
    ) { uris ->
        viewModel.proceedUris(
            uris = uris,
            parentNodeId = parentNodeId,
            pitagTrigger = pitagTrigger,
        )
    }
}

@Stable
class UploadUrisEventState internal constructor(
    initialEvent: StateEventWithContent<List<Uri>>,
) {
    var event: StateEventWithContent<List<Uri>> by mutableStateOf(initialEvent)
        private set

    fun trigger(uris: List<Uri>) {
        event = triggered(uris)
    }

    fun consume() {
        event = consumed()
    }
}

@Composable
fun rememberUploadUrisEventState(): UploadUrisEventState {
    return remember { UploadUrisEventState(consumed()) }
}

