package mega.privacy.android.feature.sharelink.presentation

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.toClipEntry
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.ExtraConstant.TYPE_TEXT_PLAIN
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.AlbumGetLinkNavKey
import mega.privacy.android.navigation.destination.GetLinkNavKey
import mega.privacy.android.navigation.destination.LinkSettingsNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.navigation.destination.ShareLinkNavKey
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Registers the revamped Share link screen entry.
 *
 * Gated behind [ApiFeatures.ShareLinkRevamp]: when the flag is disabled the destination
 * removes itself and redirects to the legacy screen for the subject — [GetLinkNavKey], which
 * launches `GetLinkActivity`, for nodes, or [AlbumGetLinkNavKey], which is already Compose, for an
 * album. This mirrors the `FileLinkRevamp` seam.
 *
 * An album only reaches here with the flag on, since the album entry point gates before
 * navigating; the disabled branch covers the flag being turned off mid-session.
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
                    navigationHandler.navigate(
                        key.albumId?.let { AlbumGetLinkNavKey(albumId = it) }
                            ?: GetLinkNavKey(handles = key.handles)
                    )
                }
            }
        ) {
            val viewModel =
                hiltViewModel<ShareLinkViewModel, ShareLinkViewModel.Factory> { factory ->
                    factory.create(ShareLinkViewModel.Args(key.subject()))
                }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val resources = LocalResources.current
            val context = LocalContext.current
            val snackbarQueue = rememberSnackBarQueue()
            val coroutineScope = rememberCoroutineScope()

            ShareLinkScreen(
                uiState = uiState,
                isAlbum = key.albumId != null,
                onSensitiveWarningConfirmed = viewModel::onSensitiveWarningConfirmed,
                onSensitiveWarningDismissed = {
                    viewModel.onSensitiveWarningDismissed()
                    navigationHandler.back()
                },
                onBack = navigationHandler::back,
                onOpenSettings = {
                    navigationHandler.navigate(
                        LinkSettingsNavKey(handles = key.handles, albumId = key.albumId)
                    )
                },
                onShareLink = { linksText ->
                    val data = uiState as? ShareLinkUiState.Data
                    context.shareLinksAsPlainText(
                        text = linksText,
                        subject = data?.takeUnless { it.isMultiNode }?.primary?.name,
                    )
                },
                onCopyLink = {
                    coroutineScope.launch {
                        snackbarQueue.queueMessage(
                            resources.getQuantityString(
                                sharedR.plurals.share_link_copied_snackbar,
                                1,
                            )
                        )
                    }
                },
                onLinksCopied = {
                    val data = uiState as? ShareLinkUiState.Data ?: return@ShareLinkScreen
                    coroutineScope.launch {
                        snackbarQueue.queueMessage(
                            resources.getQuantityString(
                                if (data.hasNewLinks) {
                                    sharedR.plurals.share_link_created_and_copied_snackbar
                                } else {
                                    sharedR.plurals.share_link_copied_snackbar
                                },
                                data.handles.size,
                            )
                        )
                    }
                },
                onCopyKey = {
                    coroutineScope.launch {
                        snackbarQueue.queueMessage(
                            resources.getString(sharedR.string.share_link_key_copied_snackbar)
                        )
                    }
                },
                onCopyPassword = {
                    coroutineScope.launch {
                        snackbarQueue.queueMessage(
                            resources.getString(sharedR.string.share_link_password_copied_snackbar)
                        )
                    }
                },
                onCopyrightAgreed = viewModel::onCopyrightAgreed,
                onCopyrightDisagreed = {
                    viewModel.onCopyrightDisagreed()
                    navigationHandler.back()
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
        val viewModel =
            hiltViewModel<LinkSettingsViewModel, LinkSettingsViewModel.Factory> { factory ->
                factory.create(LinkSettingsViewModel.Args(key.subject()))
            }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val resources = LocalResources.current
        val snackbarQueue = rememberSnackBarQueue()
        val coroutineScope = rememberCoroutineScope()
        val clipboard = LocalClipboard.current
        val uriHandler = LocalUriHandler.current

        EventEffect(event = uiState.savedEvent, onConsumed = viewModel::onSavedEventConsumed) {
            // Queued before navigating: the snackbar queue is activity-scoped, so the confirmation
            // lands on the Share link screen this returns to, which is where the design shows it.
            uiState.savedLink?.let { link ->
                clipboard.setClipEntry(
                    ClipData.newPlainText(COPIED_LINK_LABEL, link).toClipEntry()
                )
                snackbarQueue.queueMessage(
                    resources.getString(sharedR.string.share_link_updated_and_copied_snackbar)
                )
            }
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
            onUpgrade = {
                navigationHandler.navigate(
                    UpgradeAccountNavKey(source = UpgradeAccountSource.UNKNOWN)
                )
            },
        )
    }
}

/**
 * Opens the system share sheet with the given link [text] as plain text.
 *
 * @param subject Optional subject (the node name for a single node) used by share targets that
 * support one, such as email.
 */
private fun Context.shareLinksAsPlainText(text: String, subject: String?) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = TYPE_TEXT_PLAIN
        putExtra(Intent.EXTRA_TEXT, text)
        subject?.let {
            putExtra(Intent.EXTRA_SUBJECT, it)
            putExtra(Intent.EXTRA_TITLE, it)
        }
    }
    val chooser = Intent.createChooser(shareIntent, getString(sharedR.string.general_share)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(chooser)
}


/**
 * The subject the key describes. The nav key carries node handles and an album id as flat,
 * serializable fields; both screens work in terms of the sealed [ShareLinkSubject].
 */
internal fun ShareLinkNavKey.subject(): ShareLinkSubject =
    albumId?.let(ShareLinkSubject::Album) ?: ShareLinkSubject.Nodes(handles)

/** @see ShareLinkNavKey.subject */
internal fun LinkSettingsNavKey.subject(): ShareLinkSubject =
    albumId?.let(ShareLinkSubject::Album) ?: ShareLinkSubject.Nodes(handles)

/** MEGA security help page opened from the "Separate link and key" learn-more link. */
private const val SEPARATE_KEY_LEARN_MORE_URL =
    "https://help.mega.io/security/data-protection/make-links-more-secure"
