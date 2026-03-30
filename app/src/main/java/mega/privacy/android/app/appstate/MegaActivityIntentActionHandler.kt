package mega.privacy.android.app.appstate

import android.content.Intent
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOption
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.root.RefreshEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.DeepLinksDialogNavKey
import mega.privacy.android.navigation.destination.ShareToMegaNavKey
import mega.privacy.mobile.analytics.event.ShortcutActionChatButtonPressedEvent
import mega.privacy.mobile.analytics.event.ShortcutActionScanDocumentButtonPressedEvent
import mega.privacy.mobile.analytics.event.ShortcutActionUploadButtonPressedEvent
import timber.log.Timber
import javax.inject.Inject

class MegaActivityIntentActionHandler @Inject constructor(
    private val navigationEventQueue: NavigationEventQueue,
    private val navigationResultManager: NavigationResultManager,
) {

    suspend fun handleAction(
        intent: Intent,
        refreshSession: suspend (RefreshEvent) -> Unit,
        getShareUris: () -> List<UriPath>?,
    ) {
        when (intent.action) {
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
                getShareUris()?.let { shareUris ->
                    navigationEventQueue.emit(ShareToMegaNavKey(shareUris))
                } ?: Timber.w("Action send multiple but nothing to share")
                intent.action = null
                intent.removeExtra(Intent.EXTRA_STREAM)
            }

            Constants.ACTION_SHORTCUT_UPLOAD -> {
                Timber.d("Shortcut upload action received")
                Analytics.tracker.trackEvent(ShortcutActionUploadButtonPressedEvent)
                navigationResultManager.returnResult(
                    HomeFabOptionsBottomSheetNavKey.KEY,
                    HomeFabOption.UploadFiles
                )
                intent.action = null
            }

            Constants.ACTION_SHORTCUT_SCAN_DOCUMENT -> {
                Timber.d("Shortcut scan document action received")
                Analytics.tracker.trackEvent(ShortcutActionScanDocumentButtonPressedEvent)
                navigationResultManager.returnResult(
                    HomeFabOptionsBottomSheetNavKey.KEY,
                    HomeFabOption.ScanDocument
                )
                intent.action = null
            }

            Constants.ACTION_SHORTCUT_CHAT -> {
                Timber.d("Shortcut chat action received")
                Analytics.tracker.trackEvent(ShortcutActionChatButtonPressedEvent)
                navigationEventQueue.emit(ChatListNavKey())
                intent.action = null
            }
        }
    }
}
