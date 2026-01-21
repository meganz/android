package mega.privacy.android.core.nodecomponents.upload

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import java.io.IOException

@Composable
fun UploadingFiles(
    nameCollisionLauncher: ActivityResultLauncher<ArrayList<NameCollision>>,
    parentNodeId: NodeId,
    uris: List<Uri>,
    pitagTrigger: PitagTrigger,
    onStartUpload: (TransferTriggerEvent) -> Unit,
    viewModel: UploadFileViewModel = hiltViewModel(),
) {
    val megaNavigator = rememberMegaNavigator()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
            snackbarHostState?.showAutoDurationSnackbar(context.getString(R.string.error_not_enough_free_space))
        } else {
            snackbarHostState?.showAutoDurationSnackbar(context.getString(R.string.error_temporary_unavaible))
        }
    }

    LaunchedEffect(uris) {
        if (uris.isNotEmpty()) {
            viewModel.proceedUris(
                uris = uris,
                parentNodeId = parentNodeId,
                pitagTrigger = pitagTrigger,
            )
        }
    }
}

