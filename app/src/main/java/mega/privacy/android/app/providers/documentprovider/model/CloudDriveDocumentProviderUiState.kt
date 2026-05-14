package mega.privacy.android.app.providers.documentprovider.model

import androidx.compose.runtime.Stable

/**
 * Session-level state of the Cloud Drive document provider, independent of any specific
 * document or children request. The provider uses this to decide auth prompts, offline
 * banners, and root cursor summary text.
 */
@Stable
sealed interface CloudDriveSessionState {
    /** Before the first credentials / connectivity / root-node read completes. */
    data object Initialising : CloudDriveSessionState

    /** No credentials available; provider should prompt the user to sign in. */
    data object NotLoggedIn : CloudDriveSessionState

    /** App passcode lock is enabled; all reads/writes are blocked. */
    data class PasscodeLockEnabled(val accountName: String) : CloudDriveSessionState

    /** No network connectivity; reads of cached data may still work. */
    data class Offline(val accountName: String) : CloudDriveSessionState

    /** Credentials present but root node has not been resolved yet. */
    data class RootNodeNotLoaded(val accountName: String) : CloudDriveSessionState

    /** Provider is ready to serve documents from the resolved root. */
    data class Ready(
        val accountName: String,
        val rootNodeDocumentId: String,
    ) : CloudDriveSessionState
}

/**
 * Per-id state for `queryDocument`. Driven by the `documentRequestFlow` pipeline and
 * independent of [ChildrenSlot] so the two cannot cancel each other.
 */
@Stable
sealed interface DocumentSlot {
    /** No document has been requested yet. */
    data object Idle : DocumentSlot

    /** Loading the row for [documentId]. */
    data class Loading(val documentId: String) : DocumentSlot

    /** Row for [documentId] resolved. */
    data class Loaded(
        val documentId: String,
        val row: CloudDriveDocumentRow,
    ) : DocumentSlot

    /** [documentId] does not resolve to a node. */
    data class NotFound(val documentId: String) : DocumentSlot
}

/**
 * Per-parent-id state for `queryChildDocuments`. Driven by the `childrenRequestFlow`
 * pipeline and independent of [DocumentSlot].
 */
@Stable
sealed interface ChildrenSlot {
    /** No parent has been requested yet. */
    data object Idle : ChildrenSlot

    /** Loading the children of [parentDocumentId]. */
    data class Loading(val parentDocumentId: String) : ChildrenSlot

    /**
     * Children of [parentDocumentId] resolved. [hasMore] indicates whether the
     * underlying chunked loader is still streaming more pages.
     */
    data class Loaded(
        val parentDocumentId: String,
        val children: List<CloudDriveDocumentRow>,
        val hasMore: Boolean,
    ) : ChildrenSlot

    /** [parentDocumentId] does not resolve to a folder. */
    data class NotFound(val parentDocumentId: String) : ChildrenSlot
}
