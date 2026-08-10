package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.RecentlyUsedEntity
import mega.privacy.android.data.database.entity.RecentlyUsedTypeEntity
import mega.privacy.android.data.database.entity.TextEditorScrollEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextEditorScrollDaoTest {
    private lateinit var underTest: TextEditorScrollDao
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
            underTest = db.textEditorScrollDao()

            // Seed type table and create parent recently_used entry (FK required)
            db.recentlyUsedTypeDao().insertAll(
                listOf(RecentlyUsedTypeEntity(typeId = 4, name = "TextEditor"))
            )
            db.recentlyUsedDao().insertOrUpdate(
                RecentlyUsedEntity(
                    nodeHandle = 1L,
                    typeId = 4,
                    fileName = "notes.txt",
                    lastAccessedTimestamp = 1000L
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
            val entity = createEntity()

            underTest.insertOrUpdate(entity)

            assertThat(underTest.getByNodeHandle(1L)).isEqualTo(entity)
        }

    @Test
    fun test_that_insertOrUpdate_replaces_existing_entity() =
        runTest {
            underTest.insertOrUpdate(createEntity(cursorPosition = 50))
            val updated = createEntity(cursorPosition = 100)

            underTest.insertOrUpdate(updated)

            assertThat(underTest.getByNodeHandle(1L)?.cursorPosition).isEqualTo(100)
        }

    @Test
    fun test_that_deleteByNodeHandle_removes_entity() =
        runTest {
            underTest.insertOrUpdate(createEntity())

            underTest.deleteByNodeHandle(1L)

            assertThat(underTest.getByNodeHandle(1L)).isNull()
        }

    @Test
    fun test_that_getByNodeHandle_returns_null_when_not_found() =
        runTest {
            assertThat(underTest.getByNodeHandle(999L)).isNull()
        }

    @Test
    fun test_that_cascade_delete_removes_scroll_when_parent_removed() =
        runTest {
            underTest.insertOrUpdate(createEntity())

            db.recentlyUsedDao().deleteByNodeHandle(1L)

            assertThat(underTest.getByNodeHandle(1L)).isNull()
        }

    private fun createEntity(
        nodeHandle: Long = 1L,
        cursorPosition: Int = 50,
        scrollSpot: Float = 0.5f,
    ) = TextEditorScrollEntity(
        nodeHandle = nodeHandle,
        cursorPosition = cursorPosition,
        scrollSpot = scrollSpot
    )
}
