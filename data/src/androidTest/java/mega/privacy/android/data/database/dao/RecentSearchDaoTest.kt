package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.dao.RecentSearchDao.Companion.MAX_RECENT_SEARCHES
import mega.privacy.android.data.database.entity.RecentSearchEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentSearchDaoTest {
    private lateinit var recentSearchDao: RecentSearchDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        recentSearchDao = db.recentSearchDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_monitorRecentSearches_returns_empty_list_when_no_searches_exist() = runTest {
        val searches = recentSearchDao.monitorRecentSearches().first()
        assertThat(searches).isEmpty()
    }

    @Test
    fun test_that_monitorRecentSearches_returns_searches_sorted_by_timestamp_descending() =
        runTest {
            val timestamp1 = 1000L
            val timestamp2 = 2000L
            val timestamp3 = 3000L

            recentSearchDao.insertOrUpdateRecentSearch(
                RecentSearchEntity("query1", timestamp1)
            )
            recentSearchDao.insertOrUpdateRecentSearch(
                RecentSearchEntity("query2", timestamp2)
            )
            recentSearchDao.insertOrUpdateRecentSearch(
                RecentSearchEntity("query3", timestamp3)
            )

            val searches = recentSearchDao.monitorRecentSearches().first()
            assertThat(searches).hasSize(3)
            assertThat(searches[0].searchQuery).isEqualTo("query3")
            assertThat(searches[0].timestamp).isEqualTo(timestamp3)
            assertThat(searches[1].searchQuery).isEqualTo("query2")
            assertThat(searches[1].timestamp).isEqualTo(timestamp2)
            assertThat(searches[2].searchQuery).isEqualTo("query1")
            assertThat(searches[2].timestamp).isEqualTo(timestamp1)
        }

    @Test
    fun test_that_insertOrUpdateRecentSearch_inserts_new_search() = runTest {
        val query = "test query"
        val timestamp = 1000L
        val entity = RecentSearchEntity(query, timestamp)

        recentSearchDao.insertOrUpdateRecentSearch(entity)

        val searches = recentSearchDao.monitorRecentSearches().first()
        assertThat(searches).hasSize(1)
        assertThat(searches[0].searchQuery).isEqualTo(query)
        assertThat(searches[0].timestamp).isEqualTo(timestamp)
    }

    @Test
    fun test_that_insertOrUpdateRecentSearch_updates_existing_search_timestamp() = runTest {
        val query = "test query"
        val timestamp1 = 1000L
        val timestamp2 = 2000L

        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity(query, timestamp1)
        )
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity(query, timestamp2)
        )

        val searches = recentSearchDao.monitorRecentSearches().first()
        assertThat(searches).hasSize(1)
        assertThat(searches[0].searchQuery).isEqualTo(query)
        assertThat(searches[0].timestamp).isEqualTo(timestamp2)
    }

    @Test
    fun test_that_clearRecentSearches_removes_all_searches() = runTest {
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query1", 1000L)
        )
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query2", 2000L)
        )

        recentSearchDao.clearRecentSearches()

        val searches = recentSearchDao.monitorRecentSearches().first()
        assertThat(searches).isEmpty()
    }

    @Test
    fun test_that_deleteOldestSearch_removes_oldest_search() = runTest {
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query1", 1000L)
        )
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query2", 2000L)
        )
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query3", 3000L)
        )

        recentSearchDao.deleteOldestSearch()

        val searches = recentSearchDao.monitorRecentSearches().first()
        assertThat(searches).hasSize(2)
        assertThat(searches.find { it.searchQuery == "query1" }).isNull()
        assertThat(searches.find { it.searchQuery == "query2" }).isNotNull()
        assertThat(searches.find { it.searchQuery == "query3" }).isNotNull()
    }

    @Test
    fun test_that_getSearchCount_returns_correct_count() = runTest {
        assertThat(recentSearchDao.getSearchCount()).isEqualTo(0)

        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query1", 1000L)
        )
        assertThat(recentSearchDao.getSearchCount()).isEqualTo(1)

        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query2", 2000L)
        )
        assertThat(recentSearchDao.getSearchCount()).isEqualTo(2)
    }

    @Test
    fun test_that_deleteSearchByQuery_removes_specific_search() = runTest {
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query1", 1000L)
        )
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query2", 2000L)
        )

        recentSearchDao.deleteSearchByQuery("query1")

        val searches = recentSearchDao.monitorRecentSearches().first()
        assertThat(searches).hasSize(1)
        assertThat(searches[0].searchQuery).isEqualTo("query2")
    }

    @Test
    fun test_that_getMostRecentSearch_returns_most_recent_search() = runTest {
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query1", 1000L)
        )
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query2", 2000L)
        )
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("query3", 3000L)
        )

        val mostRecent = recentSearchDao.getMostRecentSearch()
        assertThat(mostRecent).isNotNull()
        assertThat(mostRecent!!.searchQuery).isEqualTo("query3")
        assertThat(mostRecent.timestamp).isEqualTo(3000L)
    }

    @Test
    fun test_that_getMostRecentSearch_returns_null_when_no_searches_exist() = runTest {
        val mostRecent = recentSearchDao.getMostRecentSearch()
        assertThat(mostRecent).isNull()
    }

    @Test
    fun test_that_insertRecentSearchWithPrefixCleanup_removes_most_recent_if_it_is_prefix() =
        runTest {
            recentSearchDao.insertOrUpdateRecentSearch(
                RecentSearchEntity("document", 2000L)
            )

            recentSearchDao.insertOrUpdateRecentSearch(
                RecentSearchEntity("my video", 3000L)
            )

            recentSearchDao.insertRecentSearchWithPrefixCleanup(
                RecentSearchEntity("my video folder", 3000L)
            )

            val searches = recentSearchDao.monitorRecentSearches().first()
            assertThat(searches).hasSize(2)
            assertThat(searches.find { it.searchQuery == "my video" }).isNull()
            assertThat(searches.find { it.searchQuery == "my video folder" }).isNotNull()
            assertThat(searches.find { it.searchQuery == "document" }).isNotNull()
        }

    @Test
    fun test_that_insertRecentSearchWithPrefixCleanup_does_not_remove_most_recent_if_it_is_not_prefix() =
        runTest {
            recentSearchDao.insertOrUpdateRecentSearch(
                RecentSearchEntity("my video", 1000L)
            )
            recentSearchDao.insertOrUpdateRecentSearch(
                RecentSearchEntity("document", 2000L)
            )

            recentSearchDao.insertRecentSearchWithPrefixCleanup(
                RecentSearchEntity("photo", 3000L)
            )

            val searches = recentSearchDao.monitorRecentSearches().first()
            assertThat(searches).hasSize(3)
            assertThat(searches.find { it.searchQuery == "my video" }).isNotNull()
            assertThat(searches.find { it.searchQuery == "document" }).isNotNull()
            assertThat(searches.find { it.searchQuery == "photo" }).isNotNull()
        }

    @Test
    fun test_that_insertRecentSearchWithPrefixCleanup_does_not_remove_if_exact_match() = runTest {
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("my video", 1000L)
        )

        recentSearchDao.insertRecentSearchWithPrefixCleanup(
            RecentSearchEntity("my video", 2000L)
        )

        val searches = recentSearchDao.monitorRecentSearches().first()
        assertThat(searches).hasSize(1)
        assertThat(searches[0].searchQuery).isEqualTo("my video")
        assertThat(searches[0].timestamp).isEqualTo(2000L)
    }

    @Test
    fun test_that_insertRecentSearchWithPrefixCleanup_is_case_insensitive() = runTest {
        recentSearchDao.insertOrUpdateRecentSearch(
            RecentSearchEntity("My Video", 1000L)
        )

        recentSearchDao.insertRecentSearchWithPrefixCleanup(
            RecentSearchEntity("my video folder", 2000L)
        )

        val searches = recentSearchDao.monitorRecentSearches().first()
        assertThat(searches).hasSize(1)
        assertThat(searches[0].searchQuery).isEqualTo("my video folder")
    }

    @Test
    fun test_that_insertRecentSearchWithPrefixCleanup_limits_to_max_searches() = runTest {
        (1..MAX_RECENT_SEARCHES).forEach { index ->
            recentSearchDao.insertOrUpdateRecentSearch(
                RecentSearchEntity("query$index", index.toLong())
            )
        }

        recentSearchDao.insertRecentSearchWithPrefixCleanup(
            RecentSearchEntity("new query", (MAX_RECENT_SEARCHES + 1).toLong())
        )

        val searches = recentSearchDao.monitorRecentSearches().first()
        assertThat(searches).hasSize(MAX_RECENT_SEARCHES)
        assertThat(searches.find { it.searchQuery == "query1" }).isNull()
        assertThat(searches.find { it.searchQuery == "new query" }).isNotNull()
    }
}

