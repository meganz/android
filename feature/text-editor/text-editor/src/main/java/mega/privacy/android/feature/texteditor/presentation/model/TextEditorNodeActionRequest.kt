package mega.privacy.android.feature.texteditor.presentation.model

/**
 * Request for a node action (download, get link, share).
 * Passed from the text editor ViewModel to [TextEditorNodeActionHandler], which performs the action
 * (e.g. resolve node, trigger transfer, open get-link flow).
 */
sealed interface TextEditorNodeActionRequest {

    data class Download(val nodeHandle: Long) : TextEditorNodeActionRequest

    data class GetLink(val nodeHandle: Long) : TextEditorNodeActionRequest

    data class Share(val nodeHandle: Long) : TextEditorNodeActionRequest
}
