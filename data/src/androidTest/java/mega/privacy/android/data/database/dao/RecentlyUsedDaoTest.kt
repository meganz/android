package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.RecentlyUsedEntity
import mega.privacy.android.data.database.entity.RecentlyUsedTypeEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentlyUsedDaoTest {
    private lateinit var underTest: RecentlyUsedDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            db =
                Room
                    .inMemoryDatabaseBuilder(
                        context,
                        MegaDatabase::class.java
                    ).build()
            underTest = db.recentlyUsedDao()

            // Seed type table
            db.recentlyUsedTypeDao().insertAll(
                listOf(
                    RecentlyUsedTypeEntity(typeId = 1, name = "PDF"),
                    RecentlyUsedTypeEntity(typeId = 2, name = "Video"),
                    RecentlyUsedTypeEntity(typeId = 3, name = "Audio"),
                    RecentlyUsedTypeEntity(typeId = 4, name = "TextEditor")
                )
            )
        }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_insertOrUpdate_inserts_entity() =
        runTest {
            val entity = createEntity(nodeHandle = 1L)

            underTest.insertOrUpdate(entity)

            assertThat(underTest.getByNodeHandle(1L)).isEqualTo(entity)
        }

    @Test
    fun test_that_insertOrUpdate_replaces_existing_entity() =
        runTest {
            underTest.insertOrUpdate(createEntity(nodeHandle = 1L, fileName = "old.pdf"))
            val updated = createEntity(nodeHandle = 1L, fileName = "new.pdf")

            underTest.insertOrUpdate(updated)

            assertThat(underTest.getByNodeHandle(1L)?.fileName).isEqualTo("new.pdf")
        }

    @Test
    fun test_that_deleteByNodeHandle_removes_entity() =
        runTest {
            underTest.insertOrUpdate(createEntity(nodeHandle = 1L))

            underTest.deleteByNodeHandle(1L)

            assertThat(underTest.getByNodeHandle(1L)).isNull()
        }

    @Test
    fun test_that_deleteAll_removes_all_entities() =
        runTest {
            underTest.insertOrUpdate(createEntity(nodeHandle = 1L))
            underTest.insertOrUpdate(createEntity(nodeHandle = 2L))

            underTest.deleteAll()

            assertThat(underTest.getCount()).isEqualTo(0)
        }

    @Test
    fun test_that_insertAndPrune_keeps_max_items() =
        runTest {
            repeat(5) { i ->
                underTest.insertOrUpdate(
                    createEntity(
                        nodeHandle = i.toLong(),
                        lastAccessedTimestamp = i.toLong()
                    )
                )
            }

            underTest.insertAndPrune(
                createEntity(nodeHandle = 99L, lastAccessedTimestamp = 99L),
                maxItems = 3
            )

            assertThat(underTest.getCount()).isEqualTo(3)
        }

    @Test
    fun test_that_monitorItems_returns_items_ordered_by_timestamp_desc() =
        runTest {
            underTest.insertOrUpdate(createEntity(nodeHandle = 1L, lastAccessedTimestamp = 100L))
            underTest.insertOrUpdate(createEntity(nodeHandle = 2L, lastAccessedTimestamp = 300L))
            underTest.insertOrUpdate(createEntity(nodeHandle = 3L, lastAccessedTimestamp = 200L))

            val items = underTest.monitorItems(
                buildQuery(orderBy = "last_accessed_timestamp DESC", limit = 10)
            ).first()

            assertThat(items.map { it.nodeHandle }).isEqualTo(listOf(2L, 3L, 1L))
        }

    @Test
    fun test_that_monitorItems_respects_limit() =
        runTest {
            repeat(5) { i ->
                underTest.insertOrUpdate(
                    createEntity(nodeHandle = i.toLong(), lastAccessedTimestamp = i.toLong())
                )
            }

            val items = underTest.monitorItems(
                buildQuery(orderBy = "last_accessed_timestamp DESC", limit = 2)
            ).first()

            assertThat(items).hasSize(2)
        }

    @Test
    fun test_that_monitorItems_returns_items_ordered_by_name_ascending() =
        runTest {
            underTest.insertOrUpdate(createEntity(nodeHandle = 1L, fileName = "Charlie.pdf"))
            underTest.insertOrUpdate(createEntity(nodeHandle = 2L, fileName = "alpha.mp3"))
            underTest.insertOrUpdate(createEntity(nodeHandle = 3L, fileName = "Bravo.mp4"))

            val items = underTest.monitorItems(
                buildQuery(orderBy = "file_name COLLATE NOCASE ASC", limit = 10)
            ).first()

            assertThat(items.map { it.nodeHandle }).isEqualTo(listOf(2L, 3L, 1L))
        }

    @Test
    fun test_that_monitorItems_returns_items_ordered_by_name_descending() =
        runTest {
            underTest.insertOrUpdate(createEntity(nodeHandle = 1L, fileName = "Charlie.pdf"))
            underTest.insertOrUpdate(createEntity(nodeHandle = 2L, fileName = "alpha.mp3"))
            underTest.insertOrUpdate(createEntity(nodeHandle = 3L, fileName = "Bravo.mp4"))

            val items = underTest.monitorItems(
                buildQuery(orderBy = "file_name COLLATE NOCASE DESC", limit = 10)
            ).first()

            assertThat(items.map { it.nodeHandle }).isEqualTo(listOf(1L, 3L, 2L))
        }

    @Test
    fun test_that_monitorItems_returns_items_ordered_by_timestamp_ascending() =
        runTest {
            underTest.insertOrUpdate(createEntity(nodeHandle = 1L, lastAccessedTimestamp = 100L))
            underTest.insertOrUpdate(createEntity(nodeHandle = 2L, lastAccessedTimestamp = 300L))
            underTest.insertOrUpdate(createEntity(nodeHandle = 3L, lastAccessedTimestamp = 200L))

            val items = underTest.monitorItems(
                buildQuery(orderBy = "last_accessed_timestamp ASC", limit = 10)
            ).first()

            assertThat(items.map { it.nodeHandle }).isEqualTo(listOf(1L, 3L, 2L))
        }

    @Test
    fun test_that_getByNodeHandle_returns_null_when_not_found() =
        runTest {
            assertThat(underTest.getByNodeHandle(999L)).isNull()
        }

    private fun createEntity(
        nodeHandle: Long = 1L,
        typeId: Int = 1,
        fileName: String = "test.pdf",
        lastAccessedTimestamp: Long = System.currentTimeMillis(),
    ) = RecentlyUsedEntity(
        nodeHandle = nodeHandle,
        typeId = typeId,
        fileName = fileName,
        lastAccessedTimestamp = lastAccessedTimestamp
    )

    private fun buildQuery(orderBy: String, limit: Int) = SimpleSQLiteQuery(
        "SELECT * FROM recently_used ORDER BY $orderBy LIMIT ?",
        arrayOf(limit)
    )
}
