package mega.privacy.android.feature.texteditor.presentation.model

/**
 * One-shot effect from the Compose text editor for actions that require the app layer
 * (Activity, share sheet, chat attach). Consumed via [TextEditorComposeUiState.nodeEffectEvent].
 */
sealed interface TextEditorNodeEffect {

    /**
     * Open get-link flow or remove-link confirmation (legacy text editor manage-link behavior).
     */
    data class ManageLink(val nodeHandle: Long) : TextEditorNodeEffect

    /**
     * System/app share sheet. Use [localPath] non-null for offline/zip-style local file share;
     * otherwise share the cloud node via [resolvedPublicLink] (pre-resolved by the ViewModel).
     */
    data class Share(
        val nodeHandle: Long,
        val localPath: String?,
        val fileName: String?,
        val resolvedPublicLink: String? = null,
    ) : TextEditorNodeEffect

    /**
     * Internal send-to-chat flow for a cloud file node.
     */
    data class SendToChat(val nodeHandle: Long) : TextEditorNodeEffect
}
