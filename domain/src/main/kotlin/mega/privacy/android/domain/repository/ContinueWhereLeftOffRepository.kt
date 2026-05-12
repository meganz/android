package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.continuewhereleftoff.TextEditorScroll
import mega.privacy.android.domain.entity.node.SortDirection

/**
 * Repository for continue-where-you-left-off state and the recently-used index.
 *
 * ## Data model
 * Two complementary storage areas:
 *
 * 1. **Recently-used index** — tracks which files were opened and when (`saveRecentlyUsedItem`).
 * 2. **Position store** — tracks how far the user progressed:
 *    - Video/audio/PDF: [savePosition] (position in ms or page number, plus optional total).
 *    - Text editor: [saveTextEditorScroll] (fine-grained cursor + scroll fraction).
 *
 * [monitorContinueWhereLeftOffItems] returns carousel list items from the recently-used index.
 * Position detail is retrieved separately when opening an item.
 */
interface ContinueWhereLeftOffRepository {

    /**
     * Monitor recently used items for the widget carousel.
     * Items are sorted according to the persisted sort preference; the returned flow
     * re-emits whenever the preference changes.
     */
    fun monitorContinueWhereLeftOffItems(limit: Int): Flow<List<ContinueWhereLeftOffItem>>

    /**
     * Monitor the persisted sort preference (field and direction).
     */
    fun monitorSortPreference(): Flow<Pair<ContinueWhereLeftOffSortField, SortDirection>>

    /**
     * Persist the sort preference used by [monitorContinueWhereLeftOffItems].
     */
    suspend fun setSortPreference(
        sortField: ContinueWhereLeftOffSortField,
        sortDirection: SortDirection,
    )

    /**
     * Record that a file was opened or interacted with.
     * Call this on file open; the current timestamp is recorded automatically.
     * To update playback/reading progress call [savePosition] (video/audio/PDF) or
     * [saveTextEditorScroll] (text editor).
     */
    suspend fun saveRecentlyUsedItem(
        nodeHandle: Long,
        type: RecentlyUsedType,
        fileName: String,
    )

    /**
     * Update the recently-used timestamp for [nodeHandle] so the item surfaces
     * at the top of the carousel. Position data (playback progress, page number)
     * is persisted by the respective media/PDF repositories.
     */
    suspend fun savePosition(nodeHandle: Long)

    /**
     * Remove a single item from the recently used index.
     */
    suspend fun removeRecentlyUsedItem(nodeHandle: Long)

    /**
     * Clear all recently used items.
     */
    suspend fun clearAllRecentlyUsedItems()

    /**
     * Save text editor cursor and scroll position.
     */
    suspend fun saveTextEditorScroll(textEditorScroll: TextEditorScroll)

    /**
     * Returns saved text editor state for [nodeHandle], or null if none exists.
     */
    suspend fun getTextEditorScroll(nodeHandle: Long): TextEditorScroll?

    /**
     * Delete text editor scroll state for [nodeHandle].
     */
    suspend fun deleteTextEditorScroll(nodeHandle: Long)
}
