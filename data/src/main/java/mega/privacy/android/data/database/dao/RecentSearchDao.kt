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
     * This prevents storing duplicate recents during slow typing
     */
    @Transaction
    suspend fun insertRecentSearchWithPrefixCleanup(entity: RecentSearchEntity) {
        val mostRecent = getMostRecentSearch()
        if (mostRecent != null &&
            entity.searchQuery.lowercase().startsWith(mostRecent.searchQuery.lowercase()) &&
            entity.searchQuery != mostRecent.searchQuery
        ) {
            deleteSearchByQuery(mostRecent.searchQuery)
        }
        insertOrUpdateRecentSearch(entity)
        if (getSearchCount() > MAX_RECENT_SEARCHES) {
            deleteOldestSearch()
        }
    }

    companion object {
        const val MAX_RECENT_SEARCHES = 10
    }
}

