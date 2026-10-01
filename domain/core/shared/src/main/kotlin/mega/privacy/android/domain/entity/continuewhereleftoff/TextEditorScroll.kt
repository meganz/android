package mega.privacy.android.domain.entity.continuewhereleftoff

/**
 * Persisted text editor cursor and scroll state for a node.
 *
 * The last accessed timestamp is tracked by the recently-used index
 * ([mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository.saveRecentlyUsedItem])
 * and not duplicated here.
 *
 * @property scrollFraction Vertical scroll position as a 0.0–1.0 fraction of the document height.
 */
data class TextEditorScroll(
    val nodeHandle: Long,
    val cursorPosition: Int,
    val scrollFraction: Float,
)
