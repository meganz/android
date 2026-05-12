package mega.privacy.android.app.appstate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import dagger.hilt.android.scopes.ActivityScoped
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.deeplinks.ExternalPdfDeepLinkHandler
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOption
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOptionsBottomSheetNavKey
import mega.privacy.android.core.sharedcomponents.parcelable
import mega.privacy.android.core.sharedcomponents.parcelableArrayList
import mega.privacy.android.domain.entity.node.root.RefreshEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.network.GetCurrentConnectivityStateUseCase
import mega.privacy.android.navigation.ACTION_PENDING_DEEP_LINK
import mega.privacy.android.navigation.contract.navOptions
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.DeepLinksDialogNavKey
import mega.privacy.android.navigation.destination.ShareFilesToMegaNavKey
import mega.privacy.android.navigation.destination.ShareTextToMegaNavKey
import mega.privacy.mobile.analytics.event.ShortcutActionChatButtonPressedEvent
import mega.privacy.mobile.analytics.event.ShortcutActionScanDocumentButtonPressedEvent
import mega.privacy.mobile.analytics.event.ShortcutActionUploadButtonPressedEvent
import timber.log.Timber
import javax.inject.Inject

@ActivityScoped
class MegaActivityIntentActionHandler @Inject constructor(
    private val activity: Activity,
    private val navigationEventQueue: NavigationEventQueue,
    private val navigationResultManager: NavigationResultManager,
    private val snackbarEventQueue: SnackbarEventQueue,
    private val getCurrentConnectivityStateUseCase: GetCurrentConnectivityStateUseCase,
    private val externalPdfDeepLinkHandler: ExternalPdfDeepLinkHandler,
) {

    private var pendingDeepLinkUrl: String? = null

    /**
     * Save the pending deep link URL from the current intent before it gets overwritten by a new intent.
     */
    fun savePendingDeepLink(intent: Intent) {
        if (isPendingDeepLinkAction(intent)) {
            pendingDeepLinkUrl = intent.dataString
        }
    }

    /**
     * Persist the pending deep link URL to the outState bundle for config change or process death.
     */
    fun savePendingDeepLinkToBundle(outState: Bundle) {
        pendingDeepLinkUrl?.let { outState.putString(EXTRA_PENDING_DEEP_LINK_URL, it) }
    }

    /**
     * Restore the pending deep link URL from the saved instance state bundle.
     */
    fun restorePendingDeepLinkFromBundle(savedInstanceState: Bundle?) {
        pendingDeepLinkUrl = savedInstanceState?.getString(EXTRA_PENDING_DEEP_LINK_URL)
    }

    /**
     * Re-emit the pending deep link after login completes and root node exists.
     */
    suspend fun handlePendingDeepLinks(intent: Intent, rootNodeExists: Boolean) {
        if (!rootNodeExists) return
        val link = pendingDeepLinkUrl
            ?: intent.dataString?.takeIf { isPendingDeepLinkAction(intent) }
        pendingDeepLinkUrl = null
        if (isPendingDeepLinkAction(intent)) {
            intent.action = null
        }
        link?.let { navigationEventQueue.emit(DeepLinksDialogNavKey(it)) }
    }

    private fun isPendingDeepLinkAction(intent: Intent): Boolean =
        intent.action == ACTION_PENDING_DEEP_LINK
                || intent.action == Constants.ACTION_JOIN_OPEN_CHAT_LINK

    suspend fun handleAction(
        intent: Intent,
        refreshSession: suspend (RefreshEvent) -> Unit,
        launchLegacyPdfViewer: () -> Unit = {},
    ) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                if (externalPdfDeepLinkHandler.consumeExternalActionViewPdfIfApplicable(
                        intent = intent,
                        launchLegacyPdfViewer = launchLegacyPdfViewer,
                        navigateToComposePdfViewer = {
                            navigationEventQueue.emit(navKey = it, navOptions = navOptions {
                                popUpToRoot {
                                    inclusive = true
                                }
                            })
                        },
                    )
                ) {
                    intent.action = null
                    intent.data = null
                }
            }

            Constants.ACTION_REFRESH -> {
                refreshSession(RefreshEvent.ManualRefresh)
                intent.action = null
            }

            Constants.ACTION_REFRESH_API_SERVER -> {
                refreshSession(RefreshEvent.ChangeEnvironment)
                intent.action = null
            }

            MegaActivity.ACTION_DEEP_LINKS -> {
                intent.dataString?.let { data ->
                    navigationEventQueue.emit(DeepLinksDialogNavKey(data))
                    intent.action = null
                    intent.data = null
                }
            }

            RefreshEvent.SdkReload.name -> {
                refreshSession(RefreshEvent.SdkReload)
                intent.action = null
            }

            Intent.ACTION_SEND_MULTIPLE, Intent.ACTION_SEND -> {
                if (intent.action == Intent.ACTION_SEND
                    && intent.type == Constants.TYPE_TEXT_PLAIN
                ) {
                    buildShareTextNavKey(intent)?.let { navKey ->
                        navigationEventQueue.emit(navKey)
                    } ?: Timber.w("Action send text but nothing to share")
                } else {
                    getShareUris(intent)?.let { shareUris ->
                        navigationEventQueue.emit(ShareFilesToMegaNavKey(shareUris))
                    } ?: Timber.w("Action send or send multiple but nothing to share")
                }
                intent.action = null
                intent.removeExtra(Intent.EXTRA_STREAM)
            }

            Constants.ACTION_SHORTCUT_UPLOAD -> {
                Timber.d("Shortcut upload action received")
                Analytics.tracker.trackEvent(ShortcutActionUploadButtonPressedEvent)
                handleIfConnected {
                    navigationResultManager.returnResult(
                        HomeFabOptionsBottomSheetNavKey.KEY,
                        HomeFabOption.UploadFiles
                    )
                }
                intent.action = null
            }

            Constants.ACTION_SHORTCUT_SCAN_DOCUMENT -> {
                Timber.d("Shortcut scan document action received")
                Analytics.tracker.trackEvent(ShortcutActionScanDocumentButtonPressedEvent)
                handleIfConnected {
                    navigationResultManager.returnResult(
                        HomeFabOptionsBottomSheetNavKey.KEY,
                        HomeFabOption.ScanDocument
                    )
                }
                intent.action = null
            }

            Constants.ACTION_SHORTCUT_CHAT -> {
                Timber.d("Shortcut chat action received")
                Analytics.tracker.trackEvent(ShortcutActionChatButtonPressedEvent)
                handleIfConnected {
                    navigationEventQueue.emit(ChatListNavKey())
                }
                intent.action = null
            }
        }
    }

    private fun buildShareTextNavKey(intent: Intent) =
        (intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.clipData?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.text?.toString())?.let { text ->
            ShareTextToMegaNavKey(
                text = text,
                subject = intent.getStringExtra(Intent.EXTRA_SUBJECT),
                email = intent.getStringExtra(Intent.EXTRA_EMAIL),
            )
        }

    private fun getShareUris(intent: Intent) = with(intent) {
        parcelableArrayList<Parcelable>(Intent.EXTRA_STREAM)?.let {
            it.mapNotNull { item -> item as? Uri }.let { uris ->
                Timber.d("Multiple files")
                grantUriPermission(uris)
                uris.map { uri -> UriPath(uri.toString()) }.ifEmpty { null }
            }
        } ?: (parcelable<Parcelable>(Intent.EXTRA_STREAM) as? Uri)
            ?.let { uri ->
                Timber.d("Single file")
                grantUriPermission(listOf(uri))
                listOf(UriPath(uri.toString()))
            }
    }

    private fun grantUriPermission(uris: List<Uri>) {
        uris.forEach { uri ->
            runCatching {
                activity.grantUriPermission(
                    activity.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.onFailure {
                Timber.e(it, "Error granting uri permission")
            }
        }
    }

    private suspend fun handleIfConnected(action: suspend () -> Unit) {
        if (getCurrentConnectivityStateUseCase().connected) {
            action()
        } else {
            snackbarEventQueue.queueMessage(R.string.error_server_connection_problem)
        }
    }

    companion object {
        private const val EXTRA_PENDING_DEEP_LINK_URL = "pendingDeepLinkUrl"
    }
}
