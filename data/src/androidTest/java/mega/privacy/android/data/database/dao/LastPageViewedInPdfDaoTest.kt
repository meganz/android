package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.LastPageViewedInPdfEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LastPageViewedInPdfDaoTest {

    private lateinit var underTest: LastPageViewedInPdfDao
    private lateinit var db: MegaDatabase

    private val nodeHandle = 123L

    @Before
    fun createDb() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        underTest = db.lastPageViewedInPdfDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_insertOrUpdateLastPageViewedInPdf_actually_inserts_the_entity() = runTest {
        val newEntity = createEntity()

        underTest.insertOrUpdateLastPageViewedInPdf(newEntity)

        assertThat(underTest.getLastPageViewedInPdfByHandle(nodeHandle)).isEqualTo(newEntity)
    }

    @Test
    fun test_that_insertOrUpdateLastPageViewedInPdf_actually_updates_the_entity() = runTest {
        val newEntity = createEntity()
        val newPage = 3L
        val updatedEntity = newEntity.copy(lastPageViewed = newPage)

        underTest.insertOrUpdateLastPageViewedInPdf(newEntity)
        underTest.insertOrUpdateLastPageViewedInPdf(updatedEntity)

        assertThat(underTest.getLastPageViewedInPdfByHandle(nodeHandle)).isEqualTo(updatedEntity)
    }

    @Test
    fun test_that_deleteLastPageViewedInPdfByHandle_actually_deletes_the_entity() = runTest {
        val newEntity = createEntity()

        underTest.insertOrUpdateLastPageViewedInPdf(newEntity)
        underTest.deleteLastPageViewedInPdfByHandle(nodeHandle)

        assertThat(underTest.getLastPageViewedInPdfByHandle(nodeHandle)).isNull()
    }

    private fun createEntity(
        handle: Long = nodeHandle,
        lastPageViewed: Long = 5,
    ) = LastPageViewedInPdfEntity(
        nodeHandle = handle,
        lastPageViewed = lastPageViewed,
    )

}