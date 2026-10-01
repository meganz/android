package mega.privacy.android.shared.nodes.namecollision.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.indicators.LargeSpinnerIndicator
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.NameCollisionNavKey
import mega.privacy.android.shared.nodes.namecollision.NameCollisionArgs
import mega.privacy.android.shared.nodes.namecollision.NameCollisionLaunchSourceHolder
import mega.privacy.android.shared.nodes.namecollision.NameCollisionViewModel
import mega.privacy.android.shared.nodes.namecollision.view.DuplicateItemsScreen
import mega.privacy.android.shared.nodes.namecollision.view.NameCollisionScreen

/**
 * Registers the Compose name-collision screen ([NameCollisionNavKey]) on the navigation graph.
 *
 * The collision(s) to resolve are looked up from [launchSourceHolder] by the key's launch id, or
 * [NameCollisionArgs.MissingLaunch] when they could not be recovered (for example after process
 * death), which the ViewModel turns into an immediate dismiss. The user's resolution leaves the
 * screen through the ViewModel's events: uploads are forwarded to the transfer handler, a completed
 * copy/move or a resolved folder upload is returned to the caller via [NameCollisionNavKey.RESULT],
 * and anything that closes without a result (a skip, an upload, or a missing launch) is a dismiss.
 */
fun EntryProviderScope<NavKey>.nameCollisionScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
    launchSourceHolder: NameCollisionLaunchSourceHolder,
) {
    entry<NameCollisionNavKey> { key ->
        val viewModel = hiltViewModel<NameCollisionViewModel, NameCollisionViewModel.Factory> { factory ->
            factory.create(
                launchSourceHolder.consume(key.launchId) ?: NameCollisionArgs.MissingLaunch
            )
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val uriHandler = LocalUriHandler.current

        EventEffect(
            event = uiState.uploadEvent,
            onConsumed = viewModel::consumeUploadEvent,
        ) { collidedFiles ->
            transferHandler.setTransferEvent(collidedFiles)
        }

        EventEffect(
            event = uiState.outcome,
            onConsumed = viewModel::consumeOutcome,
        ) { outcome ->
            navigationHandler.returnResult(NameCollisionNavKey.RESULT, outcome)
            navigationHandler.back()
        }

        EventEffect(
            event = uiState.dismissEvent,
            onConsumed = viewModel::consumeDismissEvent,
        ) {
            navigationHandler.back()
        }

        FeatureFlagGate(
            feature = ApiFeatures.NameCollisionRedesign,
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LargeSpinnerIndicator()
                }
            },
            disabled = {
                NameCollisionScreen(
                    uiState = uiState,
                    onReplaceUpdateMerge = viewModel::replaceUpdateOrMerge,
                    onCancel = viewModel::cancel,
                    onRename = viewModel::rename,
                    onLearnMore = { uriHandler.openUri(FILE_VERSIONING_HELP_URL) },
                    onBack = navigationHandler::back,
                )
            },
            enabled = {
                DuplicateItemsScreen(
                    uiState = uiState,
                    onReplaceUpdateMerge = viewModel::replaceUpdateOrMerge,
                    onCancel = viewModel::cancel,
                    onRename = viewModel::rename,
                    onBack = navigationHandler::back,
                )
            },
        )
    }
}

private const val FILE_VERSIONING_HELP_URL =
    "https://help.mega.io/files-folders/restore-delete/file-version-history"
