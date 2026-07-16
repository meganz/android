package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.GetLinkNavKey
import mega.privacy.android.navigation.destination.LinkSettingsNavKey
import mega.privacy.android.navigation.destination.ShareLinkNavKey
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Registers the revamped Share link screen entry.
 *
 * Gated behind [ApiFeatures.ShareLinkRevamp]: when the flag is disabled the destination
 * removes itself and redirects to the legacy [GetLinkNavKey], which launches
 * `GetLinkActivity`. This mirrors the `FileLinkRevamp` seam.
 */
fun EntryProviderScope<NavKey>.shareLinkScreen(
    navigationHandler: NavigationHandler,
) {
    entry<ShareLinkNavKey> { key ->
        FeatureFlagGate(
            feature = ApiFeatures.ShareLinkRevamp,
            disabled = {
                LaunchedEffect(Unit) {
                    navigationHandler.remove(key)
                    navigationHandler.navigate(GetLinkNavKey(handles = key.handles))
                }
            }
        ) {
            val viewModel = hiltViewModel<ShareLinkViewModel, ShareLinkViewModel.Factory> { factory ->
                factory.create(ShareLinkViewModel.Args(handles = key.handles))
            }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val resources = LocalResources.current
            val snackbarQueue = rememberSnackBarQueue()
            val coroutineScope = rememberCoroutineScope()

            ShareLinkScreen(
                uiState = uiState,
                onBack = navigationHandler::back,
                onOpenSettings = {
                    navigationHandler.navigate(LinkSettingsNavKey(handles = key.handles))
                },
                // The system share sheet is wired in AND-24045.
                onShareLink = {},
                onCopyLink = {
                    val data = uiState as? ShareLinkUiState.Data ?: return@ShareLinkScreen
                    coroutineScope.launch {
                        snackbarQueue.queueMessage(
                            resources.getQuantityString(
                                sharedR.plurals.share_link_created_and_copied_snackbar,
                                data.handles.size,
                            )
                        )
                    }
                },
                onCopyKey = {
                    coroutineScope.launch {
                        snackbarQueue.queueMessage(
                            resources.getString(sharedR.string.album_get_link_copy_key_success_message)
                        )
                    }
                },
            )
        }
    }
}

/**
 * Registers the revamped Link settings editor screen entry. Only reachable from the
 * gear action of [shareLinkScreen], so it inherits the [ApiFeatures.ShareLinkRevamp] gate.
 */
fun EntryProviderScope<NavKey>.linkSettingsScreen(
    navigationHandler: NavigationHandler,
) {
    entry<LinkSettingsNavKey> { key ->
        val viewModel = hiltViewModel<LinkSettingsViewModel, LinkSettingsViewModel.Factory> { factory ->
            factory.create(LinkSettingsViewModel.Args(handles = key.handles))
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val resources = LocalResources.current
        val snackbarQueue = rememberSnackBarQueue()
        val coroutineScope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current

        EventEffect(event = uiState.savedEvent, onConsumed = viewModel::onSavedEventConsumed) {
            navigationHandler.back()
        }
        EventEffect(event = uiState.errorEvent, onConsumed = viewModel::onErrorEventConsumed) {
            coroutineScope.launch {
                snackbarQueue.queueMessage(
                    resources.getString(sharedR.string.general_request_failed_message)
                )
            }
        }

        LinkSettingsScreen(
            uiState = uiState,
            onBack = navigationHandler::back,
            onSeparateKeyEnabled = viewModel::onSeparateKeyEnabled,
            onLearnMore = { uriHandler.openUri(SEPARATE_KEY_LEARN_MORE_URL) },
            onExpiryEnabled = viewModel::onExpiryEnabled,
            onExpiryDateChanged = viewModel::onExpiryDateChanged,
            onPasswordEnabled = viewModel::onPasswordEnabled,
            onPasswordChanged = viewModel::onPasswordChanged,
            onSave = viewModel::onSave,
        )
    }
}

/** MEGA security help page opened from the "Separate link and key" learn-more link. */
private const val SEPARATE_KEY_LEARN_MORE_URL = "https://mega.io/security"
