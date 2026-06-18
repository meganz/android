package mega.privacy.android.core.nodecomponents.action

import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Wraps [delegate] so the Download action can surface the "Downloading" snackbar.
 *
 * Full-screen viewers (PDF viewer, file link, etc.) have no transfers widget, so a
 * [DownloadMenuAction] must be triggered with `withStartMessage = true` — otherwise the user
 * gets no feedback that the download started. The shared `DownloadActionClickHandler` always
 * passes `withStartMessage = false`, so these screens intercept Download via this wrapper and
 * trigger it themselves through [onDownload]. Every other action is forwarded unchanged to
 * [delegate].
 */
fun buildDownloadAwareActionHandler(
    delegate: SingleNodeActionHandler,
    onDownload: (TypedNode) -> Unit,
): SingleNodeActionHandler = SingleNodeActionHandler { action, node ->
    if (action is DownloadMenuAction) {
        onDownload(node)
    } else {
        delegate(action, node)
    }
}
