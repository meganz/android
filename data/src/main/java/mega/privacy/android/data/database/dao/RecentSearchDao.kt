package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENT_SEARCH
import mega.privacy.android.data.database.entity.RecentSearchEntity

@Dao
internal interface RecentSearchDao {
    @Query("SELECT * FROM $TABLE_RECENT_SEARCH ORDER BY timestamp DESC")
    fun monitorRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecentSearch(entity: RecentSearchEntity)

    @Query("SELECT * FROM $TABLE_RECENT_SEARCH ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentSearch(): RecentSearchEntity?

    @Query("DELETE FROM $TABLE_RECENT_SEARCH")
    suspend fun clearRecentSearches()

    @Query("DELETE FROM $TABLE_RECENT_SEARCH WHERE timestamp = (SELECT MIN(timestamp) FROM ${TABLE_RECENT_SEARCH})")
    suspend fun deleteOldestSearch()

    @Query("SELECT COUNT(*) FROM $TABLE_RECENT_SEARCH")
    suspend fun getSearchCount(): Int

    @Query("DELETE FROM $TABLE_RECENT_SEARCH WHERE searchQuery = :query")
    suspend fun deleteSearchByQuery(query: String)

    /**
     * Stores search query after cleaning up most recent search
     * This prevents storing duplicate recents during slow typing or deletion
     * Only applies prefix cleanup if the most recent search was within RECENT_SEARCH_TIME_WINDOW_MS
     */
    @Transaction
    suspend fun insertRecentSearchWithPrefixCleanup(entity: RecentSearchEntity) {
        val mostRecent = getMostRecentSearch()
        if (mostRecent != null && entity.searchQuery != mostRecent.searchQuery) {
            val timeDifference = entity.timestamp - mostRecent.timestamp

            // Only apply prefix cleanup if the most recent search was very recent, e.g. user is typing
            if (timeDifference in 0..RECENT_SEARCH_TIME_WINDOW_MS) {
                val newQueryLower = entity.searchQuery.lowercase()
                val mostRecentLower = mostRecent.searchQuery.lowercase()

                when {
                    // New query is shorter and is a prefix of most recent - keep the longer one, skip saving
                    mostRecentLower.startsWith(newQueryLower) -> {
                        return // Don't save the shorter query
                    }
                    // New query is longer and contains most recent as prefix - replace shorter with longer
                    newQueryLower.startsWith(mostRecentLower) -> {
                        deleteSearchByQuery(mostRecent.searchQuery)
                    }
                }
            }
        }
        insertOrUpdateRecentSearch(entity)
        if (getSearchCount() > MAX_RECENT_SEARCHES) {
            deleteOldestSearch()
        }
    }

    companion object {
        const val MAX_RECENT_SEARCHES = 10
        const val RECENT_SEARCH_TIME_WINDOW_MS = 5_000L // 5 seconds
    }
}

